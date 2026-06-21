/**
 * FeedbackFx.java
 *
 * Tiny convenience for firing BOTH channels of a merged Feedback FX category at once (Group 16,
 * slice 5). {@link ParticleFx#play} + {@link SoundFx#play} are each per-category toggle-gated, so
 * this just calls both — callers don't repeat the pair. Use it from event sites that aren't already
 * routed through Chat.success / GuiFx (those fire their own categories inline). The {@code fire}
 * overload pulls the acting player out of a command source for the common command-handler case.
 *
 * Depends on: ParticleFx, SoundFx
 * Called by:  BulkCommands (bulk_complete) and other category event sites
 */
package com.customblocks.core;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class FeedbackFx {

    private FeedbackFx() {} // static-only

    /** Fire {@code category}'s particle + sound for {@code p} (each gated by its own toggle). */
    public static void play(ServerPlayerEntity p, String category) {
        ParticleFx.play(p, category);
        SoundFx.play(p, category);
    }

    /** Same, resolving the acting player from a command source; no-op for console/non-players. */
    public static void fire(ServerCommandSource src, String category) {
        if (src != null && src.getEntity() instanceof ServerPlayerEntity p) play(p, category);
    }
}
