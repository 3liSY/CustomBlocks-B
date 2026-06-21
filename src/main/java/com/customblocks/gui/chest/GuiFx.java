/**
 * GuiFx.java
 *
 * Sound feedback for the server-side chest GUIs (Royal Directive §2 — every interaction
 * answers back). Palette recycled from the old mod's FeedbackHelper (proven on 1.21.1):
 * amethyst chime for clicks, XP orb for success, note-block bass for danger.
 * BLOCK_NOTE_BLOCK_* are RegistryEntry<SoundEvent> and need .value() (NFR-12); every
 * other constant here is a bare SoundEvent.
 *
 * Depends on: vanilla SoundEvents only
 * Called by:  gui/chest menus (click handlers)
 */
package com.customblocks.gui.chest;

import com.customblocks.core.ParticleFx;
import com.customblocks.core.SoundFx;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;

public final class GuiFx {

    private GuiFx() {} // static-only

    /** A menu just opened from a command — quiet page turn. */
    public static void open(ServerPlayerEntity p) {
        p.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 0.7f, 1.0f);
    }

    /** A tile was clicked / a choice cycled — light chime + glyphs (gated by the gui category). */
    public static void click(ServerPlayerEntity p) {
        SoundFx.play(p, "gui");
        ParticleFx.play(p, "gui");
    }

    /** A picker choice landed — lower chime + sparkle (gated by the selection category). */
    public static void select(ServerPlayerEntity p) {
        SoundFx.play(p, "selection");
        ParticleFx.play(p, "selection");
    }

    /** A non-destructive action was kicked off — XP orb blip. */
    public static void apply(ServerPlayerEntity p) {
        p.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.05f);
    }

    /** A destructive action was kicked off — deep bass warning. */
    public static void danger(ServerPlayerEntity p) {
        p.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0f, 0.55f);
    }

    /** Click on a disabled tile — short low bass "nope". */
    public static void deny(ServerPlayerEntity p) {
        p.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.8f, 0.8f);
    }
}
