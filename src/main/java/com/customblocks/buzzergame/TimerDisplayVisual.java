/**
 * TimerDisplayVisual.java — Group 31 (BuzzerGame) items E + F (Arabic screen + per-part resize).
 *
 * The world-side visual for a timer stand: fourteen server-authoritative display entities (no client mod), so
 * they show for every player:
 *   - three ITEM_DISPLAY parts (base / leg / screen), each its own {@code customblocks:timer_part_*} model.
 *     They are sliced from the original stand at absolute coordinates, so at scale 1 they reassemble into the
 *     exact one-piece stand. Each part scales independently (item F); a lower part growing lifts the parts
 *     above it via {@link #partPosY}. The placed block itself is invisible (see {@link TimerDisplayBlock}).
 *   - four TEXT_DISPLAY number lines (a target line + a larger result line, on the player face and the camera
 *     face), right-aligned so the LED number's blinking colon sits on the screen's centre divider. Numbers use
 *     the {@code customblocks:led} bitmap font (latin digits — bidi-safe).
 *   - four ITEM_DISPLAY Arabic-word quads (target/result × front/back): the two fixed words الهدف / النتيجة as
 *     flat GREEN {@code customblocks:timer_label_*} models glued to the glass just right of the divider. This
 *     replaced the old bitmap-font label (I1 rev 2026-07-19): a font glyph fought MC bidi + the 256px atlas
 *     page + an invisible PUA-codepoint match and tofu'd four times; an ITEM_DISPLAY always renders its model.
 *
 * All display state is written through entity NBT ({@link net.minecraft.entity.Entity#readNbt}) because
 * 1.21.1 exposes no public typed setters. Every geometry number is a tunable constant up top — the on-screen
 * text offsets + screen pitch are expected to want 1–2 in-game tuning rounds.
 *
 * Depends on: EntityType.ITEM_DISPLAY / TEXT_DISPLAY, DisplayEntity, the timer_part_* item models
 * Called by:  TimerDisplayBlockEntity (spawn on place, update each tick, despawn on break)
 */
package com.customblocks.buzzergame;

import com.customblocks.CustomBlocksMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class TimerDisplayVisual {

    private TimerDisplayVisual() {} // static-only

    // ------------------------------------------------------------------ colours + resize range
    /** Default screen text colour (design lock: brand green #15FF00). One colour for ALL text. */
    public static final int DEFAULT_TEXT_COLOR = 0x15FF00;

    /** The custom 7-segment LED bitmap font used for the numbers (the only text on the screen now). */
    private static final Identifier LED_FONT = Identifier.of(CustomBlocksMod.MOD_ID, "led");

    /** I1 (rev 2026-07-19 — the regression-ender): the two fixed Arabic words are no longer TEXT; they render as
     *  flat GREEN ITEM_DISPLAY quads in {@link TimerLabelQuad}, so they can never tofu on bidi / the 256px atlas
     *  page / a PUA-codepoint mismatch again. The shared screen-glue members below are package-private for it. */

    // Per-part + whole resize range and step (2026-07-19 lock): fine ×0.1 step, wide ×0.1–5.0 bounds.
    public static final float SCALE_MIN = 0.1f;
    public static final float SCALE_MAX = 5.0f;
    public static final float SCALE_STEP = 0.1f;    // one resize-tool click (whole or part)
    public static final float SCALE_SMALL = 0.6f;
    public static final float SCALE_MEDIUM = 1.0f;  // the "medium" size preset (NOT the spawn default)
    public static final float SCALE_LARGE = 1.8f;

    /** Text-size (textsize command) clamp — design lock: the 30× arg is accepted + stored… */
    public static final float TEXT_SCALE_MIN = 0.2f;
    public static final float TEXT_SCALE_MAX = 30.0f;
    /** …but the ON-SCREEN glyph is capped to this multiple of the (owner-confirmed-OK) default so digits
     *  never overflow the small glass — the "auto-cap to the glass" lock. Expressed relative to the default
     *  glyph, which already fits, so any value here ≥1 grows text into the remaining glass room, then clamps. */
    public static final float TEXT_SCALE_ONSCREEN_MAX = 1.8f;

    // ------------------------------------------------------------------ tunable geometry (dial in-game)
    static final float STAND_BASE_SCALE = 1.0f;
    static final String ITEM_DISPLAY_MODE = "none";
    private static final float STAND_YAW_OFFSET = 0f;

    // Part vertical bands in block units (from the original model: base 0–2.5px, leg 2.5–9px, screen 8–16px).
    private static final double BASE_B0 = 0.0 / 16.0,  BASE_H  = 2.5 / 16.0;
    private static final double LEG_B0  = 2.5 / 16.0,  LEG_H   = 6.5 / 16.0;
    static final double SCREEN_B0 = 8.0 / 16.0;

    // Text glue (all block units at scale 1; TUNABLE — expect a couple in-game rounds).
    static final double SCREEN_FACE_CENTER = 0.234; // screen-face centre above the screen part base
    static final double TARGET_LINE_UP  = 0.135;    // target line above the face centre
    static final double RESULT_LINE_UP  = -0.115;   // result line below the face centre
    // I2 fix (2026-07-19, third pass): full 3D tilt-correct glue. The glass front sits at z+0.482 (model
    // screen_front, tilted −22.5° about x). Earlier passes offset the text in XZ only and separated the two
    // lines along world-Y, so on the tilted face the text floated off. textNbt() now pushes out along the TRUE
    // face-normal (which has a +Y component at −22.5°) and separates the lines along the tilted local-up axis.
    // TEXT_FORWARD is the small push out along that normal so the glyphs sit right on the glass.
    static final double TEXT_FORWARD    = 0.02;     // out along the true face normal (sit on the glass)
    static final float  SCREEN_PITCH    = -22.5f;   // front = −22.5° (back flips to +22.5), matches the model tilt
    private static final float  TEXT_BASE       = 0.26f;    // target-line glyph height at scale 1
    static final float  RESULT_LINE_FACTOR = 1.5f;  // النتيجة line is larger
    static final float  TARGET_LINE_FACTOR = 1.0f;
    private static final int    LINE_WIDTH = 800;


    private static final String PART_BASE_ITEM   = CustomBlocksMod.MOD_ID + ":timer_part_base";
    private static final String PART_LEG_ITEM    = CustomBlocksMod.MOD_ID + ":timer_part_leg";
    private static final String PART_SCREEN_ITEM = CustomBlocksMod.MOD_ID + ":timer_part_screen";

    /** The full render state pushed to the entities each update. LED is the only digit style now (the plain-
     *  text toggle was dropped, 2026-07-19); {@code blink} drives the ~1 Hz colon (Step 4). */
    public record Render(float yaw,
                         float wholeScale, float baseScale, float legScale, float screenScale,
                         boolean hasTarget, String targetNum, String resultNum,
                         int color, float textScale, boolean blink) {}

    /** Handles to a stand's spawned entities, persisted by the block entity: 3 ITEM_DISPLAY parts + 4
     *  TEXT_DISPLAY number lines + 4 ITEM_DISPLAY Arabic-word quads (front/back × target/result, I1 rev) + 3
     *  INTERACTION hitboxes (base/leg/screen, I4/I5). */
    public record Handles(@Nullable UUID base, @Nullable UUID leg, @Nullable UUID screen,
                          @Nullable UUID frontTarget, @Nullable UUID frontResult,
                          @Nullable UUID backTarget, @Nullable UUID backResult,
                          @Nullable UUID frontLabelTarget, @Nullable UUID frontLabelResult,
                          @Nullable UUID backLabelTarget, @Nullable UUID backLabelResult,
                          @Nullable UUID hitBase, @Nullable UUID hitLeg, @Nullable UUID hitScreen) {
        public boolean complete() {
            return base != null && leg != null && screen != null
                    && frontTarget != null && frontResult != null && backTarget != null && backResult != null
                    && frontLabelTarget != null && frontLabelResult != null
                    && backLabelTarget != null && backLabelResult != null
                    && hitBase != null && hitLeg != null && hitScreen != null;
        }
    }

    /** Clamp a whole/part scale into the allowed range. */
    public static float clampScale(float scale) {
        return Math.max(SCALE_MIN, Math.min(SCALE_MAX, scale));
    }

    /** Clamp a text scale (textsize) into its allowed range. */
    public static float clampTextScale(float scale) {
        return Math.max(TEXT_SCALE_MIN, Math.min(TEXT_SCALE_MAX, scale));
    }

    /** Approx rendered height multiplier for the block hitbox (whole scale × the tallest stack). */
    public static float renderedScale(float wholeScale) {
        return STAND_BASE_SCALE * wholeScale;
    }

    /**
     * Cell-relative px heights of the rendered stand's part bands: {@code {baseTop, legTop, screenBottom,
     * screenTop}}. Mirrors the vertical stacking in {@link #partPosY} (each part anchored at its model base,
     * lower parts lifting the ones above), tracking whole + per-part scale. Shared by the block hitbox
     * ({@link TimerDisplayBlock#shapeFor}) and the wand's Resize-mode part picker (I4) so the highlight box
     * and the click-to-select bands can never drift apart from the visible stand or each other.
     */
    public static double[] partBandsPx(float wholeScale, float baseScale, float legScale, float screenScale) {
        double eff = renderedScale(wholeScale);
        double sb = eff * baseScale, sl = eff * legScale, ss = eff * screenScale;
        double baseTop = 2.5 * sb;                       // base 0–2.5px, grows up from the floor
        double legTop = baseTop + 6.5 * sl;              // leg 2.5–9px, sits on the base top
        double scLift = 2.5 * (sb - eff) + 6.5 * (sl - eff); // base + leg growth lifts the screen (see partPosY)
        double screenBottom = 8.0 * eff + scLift;        // screen 8–16px, lifted
        double screenTop = screenBottom + 8.0 * ss;
        return new double[]{ baseTop, legTop, screenBottom, screenTop };
    }

    // ------------------------------------------------------------------ spawn

    /** Spawn all fourteen entities (3 parts + 4 number lines + 4 word quads + 3 hitboxes); returns the UUIDs. */
    public static Handles spawnAll(ServerWorld world, BlockPos pos, Render r) {
        UUID base = spawnPart(world, pos, r, TimerPart.BASE, PART_BASE_ITEM);
        UUID leg = spawnPart(world, pos, r, TimerPart.LEG, PART_LEG_ITEM);
        UUID screen = spawnPart(world, pos, r, TimerPart.SCREEN, PART_SCREEN_ITEM);
        UUID ft = spawnText(world, pos, r, false, true);
        UUID fr = spawnText(world, pos, r, false, false);
        UUID bt = spawnText(world, pos, r, true, true);
        UUID br = spawnText(world, pos, r, true, false);
        UUID flt = TimerLabelQuad.spawn(world, pos, r, false, true);
        UUID flr = TimerLabelQuad.spawn(world, pos, r, false, false);
        UUID blt = TimerLabelQuad.spawn(world, pos, r, true, true);
        UUID blr = TimerLabelQuad.spawn(world, pos, r, true, false);
        UUID hb = TimerHitbox.spawn(world, pos, r, TimerPart.BASE);
        UUID hl = TimerHitbox.spawn(world, pos, r, TimerPart.LEG);
        UUID hs = TimerHitbox.spawn(world, pos, r, TimerPart.SCREEN);
        return new Handles(base, leg, screen, ft, fr, bt, br, flt, flr, blt, blr, hb, hl, hs);
    }

    private static UUID spawnPart(ServerWorld world, BlockPos pos, Render r, TimerPart part, String itemId) {
        DisplayEntity.ItemDisplayEntity e = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
        e.readNbt(partNbt(pos, r, part, itemId));
        world.spawnEntity(e);
        return e.getUuid();
    }

    private static UUID spawnText(ServerWorld world, BlockPos pos, Render r, boolean back, boolean targetLine) {
        DisplayEntity.TextDisplayEntity e = new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world);
        e.readNbt(textNbt(world, pos, r, back, targetLine));
        world.spawnEntity(e);
        return e.getUuid();
    }

    // ------------------------------------------------------------------ update / despawn

    /** Re-push all three stand parts (position + scale + angle) and re-fit their INTERACTION hitboxes. */
    public static void updateStand(ServerWorld world, Handles h, BlockPos pos, Render r) {
        pushPart(world, h.base(), pos, r, TimerPart.BASE, PART_BASE_ITEM);
        pushPart(world, h.leg(), pos, r, TimerPart.LEG, PART_LEG_ITEM);
        pushPart(world, h.screen(), pos, r, TimerPart.SCREEN, PART_SCREEN_ITEM);
        TimerHitbox.push(world, h.hitBase(), pos, r, TimerPart.BASE);
        TimerHitbox.push(world, h.hitLeg(), pos, r, TimerPart.LEG);
        TimerHitbox.push(world, h.hitScreen(), pos, r, TimerPart.SCREEN);
    }

    /** Re-push all four number lines AND the four Arabic-word quads (text/model + scaled/angled transform) to
     *  both faces. Called on any text OR geometry change, so the word quads re-fit the screen as it moves/scales
     *  and hide/show with the target line (idle → target word gone). */
    public static void updateText(ServerWorld world, Handles h, BlockPos pos, Render r) {
        pushText(world, h.frontTarget(), pos, r, false, true);
        pushText(world, h.frontResult(), pos, r, false, false);
        pushText(world, h.backTarget(), pos, r, true, true);
        pushText(world, h.backResult(), pos, r, true, false);
        TimerLabelQuad.push(world, h.frontLabelTarget(), pos, r, false, true);
        TimerLabelQuad.push(world, h.frontLabelResult(), pos, r, false, false);
        TimerLabelQuad.push(world, h.backLabelTarget(), pos, r, true, true);
        TimerLabelQuad.push(world, h.backLabelResult(), pos, r, true, false);
    }

    private static void pushPart(ServerWorld world, @Nullable UUID id, BlockPos pos, Render r, TimerPart part, String itemId) {
        if (id == null || !(world.getEntity(id) instanceof DisplayEntity.ItemDisplayEntity e)) return;
        NbtCompound n = partNbt(pos, r, part, itemId);
        n.putUuid("UUID", id);
        e.readNbt(n);
    }

    private static void pushText(ServerWorld world, @Nullable UUID id, BlockPos pos, Render r, boolean back, boolean targetLine) {
        if (id == null || !(world.getEntity(id) instanceof DisplayEntity.TextDisplayEntity e)) return;
        NbtCompound n = textNbt(world, pos, r, back, targetLine);
        n.putUuid("UUID", id);
        e.readNbt(n);
    }

    /** Remove every spawned entity for a stand (on break, or before a clean respawn). */
    public static void despawn(ServerWorld world, Handles h) {
        discard(world, h.base());
        discard(world, h.leg());
        discard(world, h.screen());
        discard(world, h.frontTarget());
        discard(world, h.frontResult());
        discard(world, h.backTarget());
        discard(world, h.backResult());
        discard(world, h.frontLabelTarget());
        discard(world, h.frontLabelResult());
        discard(world, h.backLabelTarget());
        discard(world, h.backLabelResult());
        discard(world, h.hitBase());
        discard(world, h.hitLeg());
        discard(world, h.hitScreen());
    }

    private static void discard(ServerWorld world, @Nullable UUID id) {
        if (id != null && world.getEntity(id) instanceof Entity e) e.discard();
    }

    // ------------------------------------------------------------------ stacking geometry

    /** The world Y a part's ITEM_DISPLAY sits at, keeping its base anchored + lifting for parts below. */
    static double partPosY(BlockPos pos, Render r, TimerPart part) {
        float eff = STAND_BASE_SCALE * r.wholeScale();
        double b0, sP, lift;
        switch (part) {
            case BASE -> { b0 = BASE_B0; sP = r.baseScale(); lift = 0.0; }
            case LEG -> {
                b0 = LEG_B0; sP = r.legScale();
                lift = BASE_H * eff * (r.baseScale() - 1.0);
            }
            default -> { // SCREEN
                b0 = SCREEN_B0; sP = r.screenScale();
                lift = BASE_H * eff * (r.baseScale() - 1.0) + LEG_H * eff * (r.legScale() - 1.0);
            }
        }
        // PartPos.y = blockY + b0*eff + lift + (0.5 - b0)*eff*sP  (identity at sP=1, lift=0 → blockY + 0.5*eff)
        return pos.getY() + b0 * eff + lift + (0.5 - b0) * eff * sP;
    }

    private static float partScale(Render r, TimerPart part) {
        float eff = STAND_BASE_SCALE * r.wholeScale();
        return switch (part) {
            case BASE -> eff * r.baseScale();
            case LEG -> eff * r.legScale();
            default -> eff * r.screenScale();
        };
    }

    // ------------------------------------------------------------------ nbt builders

    private static NbtCompound partNbt(BlockPos pos, Render r, TimerPart part, String itemId) {
        float sc = partScale(r, part);
        Vec3d p = new Vec3d(pos.getX() + 0.5, partPosY(pos, r, part), pos.getZ() + 0.5);
        NbtCompound n = baseNbt(p, r.yaw() + STAND_YAW_OFFSET, 0f);
        n.put("transformation", transform(sc, sc, sc));
        NbtCompound item = new NbtCompound();
        item.putString("id", itemId);
        item.putInt("count", 1);
        n.put("item", item);
        n.putString("item_display", ITEM_DISPLAY_MODE);
        return n;
    }

    private static NbtCompound textNbt(ServerWorld world, BlockPos pos, Render r, boolean back, boolean targetLine) {
        float eff = STAND_BASE_SCALE * r.wholeScale();
        float sScreen = eff * r.screenScale();
        float faceYaw = back ? r.yaw() + 180f : r.yaw();
        float pitch = back ? -SCREEN_PITCH : SCREEN_PITCH;

        // Anchor on the screen face, tracking the screen part's rendered base + its scale.
        double screenBaseY = partPosY(pos, r, TimerPart.SCREEN) + (SCREEN_B0 - 0.5) * sScreen;
        double faceCenterY = screenBaseY + SCREEN_FACE_CENTER * sScreen;

        // I2 (3D tilt-correct glue): the glass is tilted `pitch` about the horizontal axis, so its outward
        // normal has a VERTICAL component and its "up" runs along the tilted face — not world-Y. Build both
        // world vectors from the horizontal facing (f) and the pitch, then place the line by pushing out along
        // the true normal (sit on the glass) and separating the two lines along the tilted local-up.
        double[] f = forward(faceYaw);
        double pr = Math.toRadians(pitch);
        double cx = Math.cos(pr), sx = Math.sin(pr);
        // outward face-normal: horizontal shrunk by cos(pitch), vertical = -sin(pitch) (front pitch −22.5 → +Y).
        double nX = f[0] * cx, nY = -sx, nZ = f[1] * cx;
        // tilted local-up (in the vertical plane, perpendicular to the normal): mostly +Y, leans by the tilt.
        double uX = f[0] * sx, uY = cx, uZ = f[1] * sx;
        double fwd = TEXT_FORWARD * sScreen;
        double up = (targetLine ? TARGET_LINE_UP : RESULT_LINE_UP) * sScreen;
        Vec3d p = new Vec3d(pos.getX() + 0.5, faceCenterY, pos.getZ() + 0.5)
                .add(nX * fwd, nY * fwd, nZ * fwd)   // out along the true face-normal (with Y) → sits on the glass
                .add(uX * up, uY * up, uZ * up);      // separate the two lines along the tilted up-axis

        NbtCompound n = baseNbt(p, faceYaw, pitch);
        float effText = Math.min(r.textScale(), TEXT_SCALE_ONSCREEN_MAX); // auto-cap to the glass (30× arg still stored)
        float glyph = TEXT_BASE * sScreen * effText * (targetLine ? TARGET_LINE_FACTOR : RESULT_LINE_FACTOR);
        n.put("transformation", transform(glyph, glyph, glyph));

        Text t = numberText(r, targetLine);
        n.putString("text", Text.Serialization.toJsonString(t, world.getRegistryManager()));
        n.putInt("line_width", LINE_WIDTH);
        n.putByte("text_opacity", (byte) -1);
        n.putInt("background", 0);              // transparent (no box behind the text)
        n.putBoolean("default_background", false);
        n.putBoolean("shadow", false);          // LED only now — no shadow (it muddies the 7-segment glyphs)
        n.putBoolean("see_through", false);
        // RIGHT-aligned (I1 rev): the number's right edge (after its blinking colon) is pinned at screen centre,
        // so it grows LEFTWARD as the seconds gain a digit (never drifting/colliding — the old centre-aligned
        // line shifted the colon) and leaves the whole right half of the glass for the Arabic-word quad.
        n.putString("alignment", "right");
        return n;
    }


    /** Build one screen NUMBER line — LED digits + a blinking colon, e.g. {@code 5.00 : } — right-aligned so its
     *  colon sits at screen centre and the Arabic-word quad ({@link TimerLabelQuad}) glues just to its right (RTL layout,
     *  design lock). Only latin digits + a neutral colon here (bidi-safe → MC never reorders them); the Arabic
     *  word is an image quad now, never a font glyph, so it can never tofu again (I1 rev 2026-07-19). */
    private static Text numberText(Render r, boolean targetLine) {
        // Idle (no target armed): the target line is blank so the screen shows a single resting result line.
        if (targetLine && !r.hasTarget()) return Text.empty();
        String num = targetLine ? r.targetNum() : r.resultNum();
        TextColor color = TextColor.fromRgb(r.color());
        Style numStyle = Style.EMPTY.withColor(color).withFont(LED_FONT);
        // The colon blinks ~1 Hz (Step 4): bright textcolor on the tick, a dim shade of it off — same char so
        // the number never jitters horizontally.
        TextColor colonColor = TextColor.fromRgb(r.blink() ? r.color() : dimShade(r.color()));
        Style colonStyle = Style.EMPTY.withColor(colonColor);
        MutableText line = Text.empty();
        line.append(Text.literal(num).setStyle(numStyle));       // LED number — left of centre
        line.append(Text.literal(" : ").setStyle(colonStyle));   // blinking colon — the divider (word quad sits right)
        return line;
    }

    /** A dim shade of an RGB (each channel × ~0.15) — the "off" state of the blinking colon and, baked into the
     *  LED font, the faint ghost-8 unlit segments both read as this dim version of the current textcolor. */
    private static int dimShade(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        return ((int) (r * 0.15) << 16) | ((int) (g * 0.15) << 8) | (int) (b * 0.15);
    }

    /** Common display fields: position, yaw+pitch, fixed billboard, no interpolation, full-bright glow. */
    static NbtCompound baseNbt(Vec3d pos, float yaw, float pitch) {
        NbtCompound n = new NbtCompound();
        NbtList posList = new NbtList();
        posList.add(NbtDouble.of(pos.x));
        posList.add(NbtDouble.of(pos.y));
        posList.add(NbtDouble.of(pos.z));
        n.put("Pos", posList);
        NbtList rot = new NbtList();
        rot.add(NbtFloat.of(yaw));
        rot.add(NbtFloat.of(pitch));
        n.put("Rotation", rot);
        n.putString("billboard", "fixed");
        // I6 (place flicker): the transform is seeded into this same compound pre-spawn (see partNbt/textNbt),
        // and both durations are 0 so the client snaps straight to the final pose — no interpolate-from-default
        // over a frame. start_interpolation=0 commits that pose at tick 0 rather than leaving it un-triggered.
        n.putInt("interpolation_duration", 0);
        n.putInt("teleport_duration", 0);
        n.putInt("start_interpolation", 0);
        n.putFloat("view_range", 1.0f);
        NbtCompound brightness = new NbtCompound(); // fullbright so digits glow in any light (filming)
        brightness.putInt("block", 15);
        brightness.putInt("sky", 15);
        n.put("brightness", brightness);
        return n;
    }

    static NbtCompound transform(float sx, float sy, float sz) {
        NbtCompound t = new NbtCompound();
        t.put("translation", floats(0f, 0f, 0f));
        t.put("scale", floats(sx, sy, sz));
        t.put("left_rotation", floats(0f, 0f, 0f, 1f));
        t.put("right_rotation", floats(0f, 0f, 0f, 1f));
        return t;
    }

    private static NbtList floats(float... values) {
        NbtList list = new NbtList();
        for (float v : values) list.add(NbtFloat.of(v));
        return list;
    }

    /**
     * Unit (dx, dz) the front of a display at this yaw points along. Matches {@code Direction.asRotation()}
     * so the cardinal angles still line up exactly: yaw 0 → +Z, 90 → -X, 180 → -Z, 270 → +X.
     */
    static double[] forward(float yaw) {
        double rad = Math.toRadians(yaw);
        return new double[]{ -Math.sin(rad), Math.cos(rad) };
    }
}
