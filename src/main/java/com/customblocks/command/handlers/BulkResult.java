/**
 * BulkResult.java — Group 07 §G07-4 (X3 dual feedback).
 *
 * A per-player "last bulk result" line. The apply cores already print their outcome to chat; X3 asks that the
 * SAME outcome also show as an in-screen, dismissable modal on the Bulk Operations Hub (a player looking at the
 * Screen shouldn't have to glance at chat to learn "No names changed — 1 locked"). Each core records its plain
 * result here; {@link BulkSnapshot#build} consumes it ONCE into the refresh snapshot, so only the acting player
 * (whose op just produced it) sees the modal, and it shows a single time. The chat line stays as the log.
 *
 * Recorded by:  BulkCommands / BulkCategoryCommands / BulkDuplicateCommands / BulkReidCommands /
 *               BulkFlagCommands / BulkExportCommands (apply cores).
 * Consumed by:  BulkSnapshot.build (relay into the Hub); cleared on an explicit Hub open (no stale modal).
 */
package com.customblocks.command.handlers;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BulkResult {

    private BulkResult() {} // static-only

    private static final Map<UUID, String> PENDING = new ConcurrentHashMap<>();

    /** Record the actor's latest bulk result (plain text, no § codes needed — the client colours the modal). */
    public static void record(ServerCommandSource src, String plain) {
        if (src == null || plain == null || plain.isBlank()) return;
        if (src.getEntity() instanceof ServerPlayerEntity p) PENDING.put(p.getUuid(), plain);
    }

    /** Take + clear the player's pending result (null if none). Called once when a snapshot is built. */
    public static String consume(UUID player) {
        return player == null ? null : PENDING.remove(player);
    }

    /** Drop any pending result (called on an explicit Hub open so a stale chat-path result can't pop a modal). */
    public static void clear(UUID player) {
        if (player != null) PENDING.remove(player);
    }
}
