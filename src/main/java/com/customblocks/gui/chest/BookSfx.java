/**
 * BookSfx.java
 *
 * Responsibility: The Settings Book's sound design (Group 21, spec section 4) -- a distinct, short
 * sound per action: toggle on (high) / off (low), a soft click per number step, fold open / close,
 * a page-turn when opening a sub-menu or flipping pages, and a happy ding on a saved text edit. Every
 * sound respects the per-category "gui" sound toggle (so turning GUI sounds off silences the book).
 *
 * Uses only plain SoundEvents (no note-block entries) so the soundGate gate (NFR-12) is satisfied
 * without any .value() calls.
 *
 * Depends on: CustomBlocksConfig (gui sound toggle), vanilla SoundEvents.
 * Called by:  ConfigApply, StepperMenu, SettingsBookMenu, FieldSlots.
 */
package com.customblocks.gui.chest;

import com.customblocks.CustomBlocksConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public final class BookSfx {

    private BookSfx() {} // static-only

    private static final String CHANNEL = "gui"; // respects config soundsEnabled_gui

    /** Boolean flipped: a bright chime when turning on, a lower one when turning off. */
    public static void toggle(ServerPlayerEntity p, boolean on) {
        play(p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, on ? 1.45f : 0.8f);
    }

    /** A soft click for an enum cycle or a single number step. */
    public static void step(ServerPlayerEntity p) {
        play(p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.1f);
    }

    /** A drawer folding open or closed. */
    public static void fold(ServerPlayerEntity p, boolean opening) {
        play(p, opening ? SoundEvents.BLOCK_BARREL_OPEN : SoundEvents.BLOCK_BARREL_CLOSE, 0.6f, 1.3f);
    }

    /** Opening a sub-menu or flipping the page. */
    public static void turn(ServerPlayerEntity p) {
        play(p, SoundEvents.ITEM_BOOK_PAGE_TURN, 0.8f, 1.0f);
    }

    /** A typed value was saved. */
    public static void save(ServerPlayerEntity p) {
        play(p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.6f);
    }

    private static void play(ServerPlayerEntity p, SoundEvent sound, float volume, float pitch) {
        if (p == null || !CustomBlocksConfig.soundsOn(CHANNEL)) return;
        p.playSound(sound, volume, pitch);
    }
}
