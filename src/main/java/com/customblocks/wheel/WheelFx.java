/**
 * WheelFx.java - Group 34 (Wheel of Fortune) v2 item E (sound + particles).
 *
 * Every noise and particle the wheel makes, kept out of {@link WheelBlockEntity} so the spin state machine
 * stays readable. Two moments, both design-locked 2026-07-23:
 *   - DURING the spin: a whir loop under peg-clacks that SPACE OUT as the arrow decelerates. The caller
 *     passes a 0..1 speed factor taken from the same easing curve that drives the arrow, so the audio can
 *     never drift out of sync with what the eye sees.
 *   - ON the landing: a four-layer payoff (bell ding + fanfare + firework + level-up) over a firework ring
 *     drawn around the win popup.
 *
 * Depends on: SoundEvents / ParticleTypes, WheelRing (ring geometry for the particle burst)
 * Called by:  WheelBlockEntity (spin start, per-tick spin audio, landing)
 */
package com.customblocks.wheel;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class WheelFx {

    private WheelFx() {} // static-only

    /** Radius the celebration ring bursts at — inside the wedge band, around the popup. */
    private static final double BURST_R = 4.0;
    /** Points on that ring. */
    private static final int BURST_POINTS = 32;

    // ------------------------------------------------------------------ spin audio

    /** The clunk of the wheel being let go. */
    public static void spinStart(ServerWorld world, Vec3d at) {
        world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 1.0f, 0.7f);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 0.8f, 0.6f);
    }

    /**
     * The whir under the spin. {@code speed} is 0..1 (1 = full tilt at the start): it rides the pitch and the
     * volume down with the arrow, so the machine audibly runs out of momentum.
     */
    public static void whir(ServerWorld world, Vec3d at, double speed) {
        float pitch = (float) (0.5 + 1.1 * speed);
        float volume = (float) (0.25 + 0.45 * speed);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_MINECART_RIDING, SoundCategory.BLOCKS, volume, pitch);
    }

    /** One peg-clack. The CALLER decides when a peg is crossed, so the clacks space out on their own. */
    public static void clack(ServerWorld world, Vec3d at, double speed) {
        float pitch = (float) (1.1 + 0.5 * speed);
        float volume = (float) (0.5 + 0.4 * speed);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON,
                SoundCategory.BLOCKS, volume, pitch);
    }

    // ------------------------------------------------------------------ landing payoff

    /** The four-layer win audio plus a firework ring around the popup (items D + E). */
    public static void win(ServerWorld world, BlockPos anchor, float faceYaw) {
        Vec3d at = WheelRing.center(anchor);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_BELL_USE, SoundCategory.BLOCKS, 1.0f, 1.2f);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST,
                SoundCategory.BLOCKS, 1.0f, 1.1f);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 0.9f, 1.3f);

        for (int i = 0; i < BURST_POINTS; i++) {
            Vec3d p = WheelRing.point(anchor, faceYaw, BURST_R, i * 360.0 / BURST_POINTS, WheelDisplayVisual.DEPTH_ICON);
            world.spawnParticles(ParticleTypes.FIREWORK, p.x, p.y, p.z, 4, 0.12, 0.12, 0.12, 0.06);
            world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.03);
        }
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y, at.z, 40, 1.2, 1.2, 1.2, 0.02);
        world.spawnParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 30, 0.6, 0.6, 0.6, 0.12);
    }
}
