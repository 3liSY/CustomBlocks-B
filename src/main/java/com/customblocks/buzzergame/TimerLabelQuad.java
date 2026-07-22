/**
 * TimerLabelQuad.java — Group 31 (BuzzerGame) item E / I1 (rev 2026-07-19).
 *
 * The two fixed Arabic screen words الهدف (target) / النتيجة (result) as flat GREEN ITEM_DISPLAY quads glued to
 * the timer screen, one per line per face. This REPLACED the old {@code customblocks:timer_label} bitmap-font
 * label: a font glyph fought MC's bidi reorder AND its 256px font-atlas page AND an invisible PUA-codepoint
 * match between the java + the font json — any one silently breaking rendered a tofu box, which regressed four
 * times. An ITEM_DISPLAY always renders its model, so the words can never tofu again.
 *
 * Each quad reuses {@link TimerDisplayVisual}'s screen glue (out along the true tilted face-normal, up along the
 * tilted local-up) and adds a push along the screen's horizontal RIGHT axis so the word sits in the right half
 * of the glass, its LEFT edge landing on the number's centre divider (the LED number is right-aligned to that
 * same divider — see TimerDisplayVisual.numberText). The target word hides (scale 0) while no target is armed.
 *
 * Depends on: TimerDisplayVisual (shared glue geometry + constants), the timer_label_* item models
 * Called by:  TimerDisplayVisual (spawnAll / updateText / despawn)
 */
package com.customblocks.buzzergame;

import com.customblocks.CustomBlocksMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class TimerLabelQuad {

    private TimerLabelQuad() {} // static-only

    private static final String LABEL_TARGET_ITEM = CustomBlocksMod.MOD_ID + ":timer_label_target"; // الهدف
    private static final String LABEL_RESULT_ITEM = CustomBlocksMod.MOD_ID + ":timer_label_result"; // النتيجة

    // The quad's edge (blocks) at scale 1 before the per-line factor; sized so the word (~0.55 of the 256²
    // texture height) reads a touch under the LED digits and the target-line word + number both fit the
    // ~0.625-block-wide glass. LABEL_INK_HALF = half the word's ink width as a fraction of the quad (word is
    // ~0.95 of the texture, centred), so pushing the quad right by that lands the word's LEFT edge on the divider.
    private static final float  LABEL_BASE     = 0.20f;
    private static final double LABEL_INK_HALF = 0.475;
    private static final double LABEL_GAP      = 0.02;      // small gap (blocks at scale 1) after the colon

    /** Spawn one Arabic-word quad; returns its UUID to persist. */
    public static UUID spawn(ServerWorld world, BlockPos pos, TimerDisplayVisual.Render r, boolean back, boolean targetLine) {
        DisplayEntity.ItemDisplayEntity e = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
        e.readNbt(nbt(pos, r, back, targetLine));
        world.spawnEntity(e);
        return e.getUuid();
    }

    /** Re-fit an existing Arabic-word quad to the current render (position + size + shown/hidden). */
    public static void push(ServerWorld world, @Nullable UUID id, BlockPos pos, TimerDisplayVisual.Render r, boolean back, boolean targetLine) {
        if (id == null || !(world.getEntity(id) instanceof DisplayEntity.ItemDisplayEntity e)) return;
        NbtCompound n = nbt(pos, r, back, targetLine);
        n.putUuid("UUID", id);
        e.readNbt(n);
    }

    private static NbtCompound nbt(BlockPos pos, TimerDisplayVisual.Render r, boolean back, boolean targetLine) {
        float eff = TimerDisplayVisual.STAND_BASE_SCALE * r.wholeScale();
        float sScreen = eff * r.screenScale();
        float faceYaw = back ? r.yaw() + 180f : r.yaw();
        float pitch = back ? -TimerDisplayVisual.SCREEN_PITCH : TimerDisplayVisual.SCREEN_PITCH;

        double screenBaseY = TimerDisplayVisual.partPosY(pos, r, TimerPart.SCREEN)
                + (TimerDisplayVisual.SCREEN_B0 - 0.5) * sScreen;
        double faceCenterY = screenBaseY + TimerDisplayVisual.SCREEN_FACE_CENTER * sScreen;

        double[] f = TimerDisplayVisual.forward(faceYaw);
        double pr = Math.toRadians(pitch);
        double cx = Math.cos(pr), sx = Math.sin(pr);
        double nX = f[0] * cx, nY = -sx, nZ = f[1] * cx;      // outward face-normal (with +Y at −22.5°)
        double uX = f[0] * sx, uY = cx, uZ = f[1] * sx;       // tilted local-up
        double rX = f[1], rZ = -f[0];                          // screen-local RIGHT (horizontal, viewer's right)

        float effText = Math.min(r.textScale(), TimerDisplayVisual.TEXT_SCALE_ONSCREEN_MAX);
        float lineFactor = targetLine ? TimerDisplayVisual.TARGET_LINE_FACTOR : TimerDisplayVisual.RESULT_LINE_FACTOR;
        float quad = LABEL_BASE * sScreen * effText * lineFactor; // the flat model is 1 block per unit scale
        double fwd = TimerDisplayVisual.TEXT_FORWARD * sScreen;
        double up = (targetLine ? TimerDisplayVisual.TARGET_LINE_UP : TimerDisplayVisual.RESULT_LINE_UP) * sScreen;
        double right = LABEL_INK_HALF * quad + LABEL_GAP * sScreen; // word LEFT edge lands just right of the divider

        Vec3d p = new Vec3d(pos.getX() + 0.5, faceCenterY, pos.getZ() + 0.5)
                .add(nX * fwd, nY * fwd, nZ * fwd)   // sit on the glass
                .add(uX * up, uY * up, uZ * up)      // match the number line's height
                .add(rX * right, 0, rZ * right);     // shift into the right half of the glass

        NbtCompound n = TimerDisplayVisual.baseNbt(p, faceYaw, pitch);
        // Hide the target word when nothing is armed (its number line is blank too) — scale 0 = invisible.
        boolean shown = !(targetLine && !r.hasTarget());
        float s = shown ? quad : 0f;
        n.put("transformation", TimerDisplayVisual.transform(s, s, s));
        NbtCompound item = new NbtCompound();
        item.putString("id", targetLine ? LABEL_TARGET_ITEM : LABEL_RESULT_ITEM);
        item.putInt("count", 1);
        n.put("item", item);
        n.putString("item_display", TimerDisplayVisual.ITEM_DISPLAY_MODE);
        return n;
    }
}
