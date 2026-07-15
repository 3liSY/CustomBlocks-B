/**
 * CbUiSounds.java — Group 27 §G27.8.A editor UI sounds. CLIENT-SIDE ONLY.
 *
 * Responsibility: the tiny shared sound cues for CB screens — button click, selection tick and the
 * amethyst chime on Apply/Save/Create. Gated by the master sound toggle + volume in
 * {@link CbScreenPrefs} (§G27.8.B Settings). Vanilla SoundEvents only; all bare SoundEvent here
 * (only BLOCK_NOTE_BLOCK_* would need .value(), NFR-12 — none used).
 *
 * Depends on: MinecraftClient sound manager, CbScreenPrefs.
 * Called by: CbActionBar, CbSettingsOverlay, the Group 27 frame screens.
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

@Environment(EnvType.CLIENT)
public final class CbUiSounds {

    private CbUiSounds() {}

    /** Button press. */
    public static void click() { play(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f); }

    /** Small selection change (swatch, chip, toggle). */
    public static void tick()  { play(SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, 1.6f); }

    /** Apply / Save / Create success chime. */
    public static void chime() { play(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f); }

    private static void play(SoundEvent sound, float pitch) {
        CbScreenPrefs p = CbScreenPrefs.get();
        if (!p.soundOn || p.soundVolume <= 0) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        mc.getSoundManager().play(
                PositionedSoundInstance.master(sound, pitch, p.soundVolume / 100f));
    }
}
