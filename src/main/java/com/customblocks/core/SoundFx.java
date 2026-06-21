/**
 * SoundFx.java
 *
 * Server-side event sounds for the mod's feedback categories (Group 16, slice 5 — the sound half
 * of the merged Feedback FX). Parallel to {@link ParticleFx}: each of the six
 * {@link com.customblocks.CustomBlocksConfig#FX_CATEGORIES} maps to one short sound played to the
 * acting player. {@link #play} respects the per-category toggle (config soundsEnabled_<category>);
 * {@link #preview} ignores it (used by the board so a disabled category can still be sampled).
 *
 * Note-block sounds are RegistryEntry<SoundEvent> and need .value() (NFR-12 / verifySound gate).
 *
 * Depends on: CustomBlocksConfig (toggle map), vanilla SoundEvents
 * Called by:  Chat (success/error), GuiFx (gui/selection), SoundCommands (preview)
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public final class SoundFx {

    private SoundFx() {} // static-only

    /** Play {@code category}'s sound for {@code p}, but only if that category's sound is enabled. */
    public static void play(ServerPlayerEntity p, String category) {
        if (p == null || !CustomBlocksConfig.soundsOn(category)) return;
        emit(p, category);
    }

    /** Play {@code category}'s sound for {@code p} regardless of its toggle (board sampling). */
    public static void preview(ServerPlayerEntity p, String category) {
        if (p == null) return;
        emit(p, category);
    }

    private static void emit(ServerPlayerEntity p, String category) {
        switch (category) {
            case "success"       -> at(p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.0f);
            case "error"         -> at(p, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0f, 0.7f);
            case "gui"           -> at(p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.25f);
            case "selection"     -> at(p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.0f);
            case "bulk_complete" -> at(p, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.8f, 1.0f);
            case "achievement"   -> at(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
            default -> { /* unknown category — no-op */ }
        }
    }

    private static void at(ServerPlayerEntity p, SoundEvent sound, float volume, float pitch) {
        p.playSound(sound, volume, pitch);
    }
}
