/**
 * TimerDisplayVisual.java — Group 31 (BuzzerGame) items 1-2 (physical stand display + live resize).
 *
 * The world-side visual for a timer stand: it spawns and drives three vanilla display entities, all
 * server-authoritative (no client mod code needed) so they show for every player:
 *   - one ITEM_DISPLAY rendering the {@code customblocks:timer_display} stand model (the placed block
 *     itself is invisible — see {@link TimerDisplayBlock});
 *   - two TEXT_DISPLAY digit panels, one facing the placed direction (player side) and one the opposite
 *     (camera side), so both faces are readable.
 * Everything is driven by a single {@code scale} multiplier (item 2 resize): the stand grows from its
 * base on the floor and the digit panels track the screen, so one number resizes the whole thing (and the
 * block's hitbox — see the block). All display state is written through entity NBT
 * ({@link net.minecraft.entity.Entity#readNbt}) because 1.21.1 exposes no public typed setters.
 *
 * Every geometry number is a tunable constant up top — dialled in on the first in-game look.
 *
 * Depends on: EntityType.ITEM_DISPLAY / TEXT_DISPLAY, DisplayEntity, the timer_display item model
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
    /** Green glow for the running/idle digits (matches the approved mockup). */
    public static final int DIGIT_COLOR = 0x3BFF78;
    /** Dimmer green for the idle "0.00" resting state. */
    public static final int IDLE_DIGIT_COLOR = 0x2F8A50;

    /** The custom 7-segment LED bitmap font (item 3). White glyphs → tinted by the digit colour. */
    private static final Identifier LED_FONT = Identifier.of(CustomBlocksMod.MOD_ID, "led");

    public static final float SCALE_MIN = 0.4f;
    public static final float SCALE_MAX = 3.0f;
    public static final float SCALE_STEP = 0.15f;   // one resize-tool click
    public static final float SCALE_SMALL = 0.6f;
    public static final float SCALE_MEDIUM = 1.0f;  // the default / item-1 size
    public static final float SCALE_LARGE = 1.8f;

    // ------------------------------------------------------------------ tunable look constants
    private static final float STAND_BASE_SCALE = 1.0f;   // model size at scale=1 (≈ 1 block)
    private static final String ITEM_DISPLAY_MODE = "none";
    private static final float STAND_YAW_OFFSET = 0f;     // flip to 180 if the stand faces backwards

    private static final float TEXT_SCALE = 0.34f;        // digit height at scale=1
    private static final double TEXT_UP = 0.24;           // block-center → screen-center (of the base cube)
    private static final double TEXT_FORWARD = 0.30;      // block-center → just in front of the screen face
    private static final float TEXT_YAW_OFFSET = 0f;      // flip to 180 if the digits face inward
    private static final int LINE_WIDTH = 400;

    /** Handles to a stand's three spawned entities, persisted by the block entity. */
    public record Handles(@Nullable UUID stand, @Nullable UUID front, @Nullable UUID back) {
        public boolean complete() { return stand != null && front != null && back != null; }
    }

    /** Clamp any requested scale into the allowed range. */
    public static float clampScale(float scale) {
        return Math.max(SCALE_MIN, Math.min(SCALE_MAX, scale));
    }

    /** The rendered size multiplier (base model size × the user scale) — used by the block's hitbox. */
    public static float renderedScale(float scale) {
        return STAND_BASE_SCALE * scale;
    }

    // ------------------------------------------------------------------ spawn

    /** Spawn the stand model + both digit panels at the given scale/angle; returns their UUIDs to persist. */
    public static Handles spawnAll(ServerWorld world, BlockPos pos, float yaw, String digits,
                                   int color, float scale, boolean led) {
        DisplayEntity.ItemDisplayEntity stand = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
        stand.readNbt(standNbt(pos, yaw, scale));
        world.spawnEntity(stand);
        UUID front = spawnText(world, pos, yaw, false, digits, color, scale, led);
        UUID back = spawnText(world, pos, yaw, true, digits, color, scale, led);
        return new Handles(stand.getUuid(), front, back);
    }

    private static UUID spawnText(ServerWorld world, BlockPos pos, float yaw, boolean back,
                                  String digits, int color, float scale, boolean led) {
        DisplayEntity.TextDisplayEntity e = new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world);
        e.readNbt(textNbt(world, pos, yaw, back, digits, color, scale, led));
        world.spawnEntity(e);
        return e.getUuid();
    }

    // ------------------------------------------------------------------ update / despawn

    /** Re-push the stand's model transform (position + scale + angle) — call when scale or yaw changed. */
    public static void updateStand(ServerWorld world, @Nullable UUID id, BlockPos pos, float yaw, float scale) {
        if (id == null || !(world.getEntity(id) instanceof DisplayEntity.ItemDisplayEntity e)) return;
        NbtCompound n = standNbt(pos, yaw, scale);
        n.putUuid("UUID", id);
        e.readNbt(n);
    }

    /** Re-push the digit text (and its scaled/angled transform) to both faces. */
    public static void updateDigits(ServerWorld world, Handles h, BlockPos pos, float yaw,
                                    String digits, int color, float scale, boolean led) {
        pushText(world, h.front(), pos, yaw, false, digits, color, scale, led);
        pushText(world, h.back(), pos, yaw, true, digits, color, scale, led);
    }

    private static void pushText(ServerWorld world, @Nullable UUID id, BlockPos pos, float yaw,
                                 boolean back, String digits, int color, float scale, boolean led) {
        if (id == null || !(world.getEntity(id) instanceof DisplayEntity.TextDisplayEntity e)) return;
        NbtCompound n = textNbt(world, pos, yaw, back, digits, color, scale, led);
        n.putUuid("UUID", id); // keep identity stable across the re-read
        e.readNbt(n);
    }

    /** Remove every spawned entity for a stand (on break, or before a clean respawn). */
    public static void despawn(ServerWorld world, Handles h) {
        discard(world, h.stand());
        discard(world, h.front());
        discard(world, h.back());
    }

    private static void discard(ServerWorld world, @Nullable UUID id) {
        if (id != null && world.getEntity(id) instanceof Entity e) e.discard();
    }

    // ------------------------------------------------------------------ nbt builders

    private static NbtCompound standNbt(BlockPos pos, float yaw, float scale) {
        float eff = STAND_BASE_SCALE * scale;
        // Pivot from the floor: the model is centre-anchored, so lift it by half its rendered height.
        Vec3d p = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5 * eff, pos.getZ() + 0.5);
        NbtCompound n = baseNbt(p, yaw + STAND_YAW_OFFSET);
        n.put("transformation", transform(eff, eff, eff));
        NbtCompound item = new NbtCompound();
        item.putString("id", CustomBlocksMod.MOD_ID + ":timer_display");
        item.putInt("count", 1);
        n.put("item", item);
        n.putString("item_display", ITEM_DISPLAY_MODE);
        return n;
    }

    private static NbtCompound textNbt(ServerWorld world, BlockPos pos, float yaw, boolean back,
                                       String digits, int color, float scale, boolean led) {
        float eff = STAND_BASE_SCALE * scale;
        float faceYaw = back ? yaw + 180f : yaw;             // the camera side reads from the opposite angle
        double[] f = forward(faceYaw);                        // unit (dx,dz) the screen face points along
        double fwd = TEXT_FORWARD * eff;
        Vec3d p = new Vec3d(pos.getX() + 0.5, pos.getY() + (0.5 + TEXT_UP) * eff, pos.getZ() + 0.5)
                .add(f[0] * fwd, 0, f[1] * fwd);
        NbtCompound n = baseNbt(p, faceYaw + TEXT_YAW_OFFSET);
        float glyph = TEXT_SCALE * scale;
        n.put("transformation", transform(glyph, glyph, glyph));
        // LED style: the custom 7-segment bitmap font (no bold — bold would double-print the glyphs).
        // Plain-text style: the vanilla font, bold, with a drop-shadow.
        Style style = led
                ? Style.EMPTY.withColor(TextColor.fromRgb(color)).withFont(LED_FONT)
                : Style.EMPTY.withColor(TextColor.fromRgb(color)).withBold(true);
        Text t = Text.literal(digits).setStyle(style);
        n.putString("text", Text.Serialization.toJsonString(t, world.getRegistryManager()));
        n.putInt("line_width", LINE_WIDTH);
        n.putByte("text_opacity", (byte) -1); // 255 = fully opaque
        n.putInt("background", 0);            // transparent (no box behind the digits)
        n.putBoolean("default_background", false);
        n.putBoolean("shadow", !led);         // shadow reads well on plain text; muddies the LED segments
        n.putBoolean("see_through", false);
        n.putString("alignment", "center");
        return n;
    }

    /** Common display fields: position, yaw, fixed billboard, no interpolation, full-bright glow. */
    private static NbtCompound baseNbt(Vec3d pos, float yaw) {
        NbtCompound n = new NbtCompound();
        NbtList posList = new NbtList();
        posList.add(NbtDouble.of(pos.x));
        posList.add(NbtDouble.of(pos.y));
        posList.add(NbtDouble.of(pos.z));
        n.put("Pos", posList);
        NbtList rot = new NbtList();
        rot.add(NbtFloat.of(yaw));
        rot.add(NbtFloat.of(0f));
        n.put("Rotation", rot);
        n.putString("billboard", "fixed");
        n.putInt("interpolation_duration", 0);
        n.putInt("teleport_duration", 0);
        n.putFloat("view_range", 1.0f);
        NbtCompound brightness = new NbtCompound(); // fullbright so digits glow in any light (filming)
        brightness.putInt("block", 15);
        brightness.putInt("sky", 15);
        n.put("brightness", brightness);
        return n;
    }

    private static NbtCompound transform(float sx, float sy, float sz) {
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
    private static double[] forward(float yaw) {
        double r = Math.toRadians(yaw);
        return new double[]{ -Math.sin(r), Math.cos(r) };
    }
}
