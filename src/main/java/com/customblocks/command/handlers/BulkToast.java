/**
 * BulkToast.java — Group 07 §G07-4 (in-screen undo/redo toast).
 *
 * A per-player "last undo/redo" one-liner. When a player fires the Hub's Undo/Redo (or a jump-back-N from the
 * history panel), the server records the outcome here ("Undid Delete (12)" / "Redid Re-ID (3)"); {@link
 * BulkSnapshot#build} consumes it ONCE into the refresh snapshot, so only the acting player sees the toast and
 * it shows a single time. Distinct from {@link BulkResult} (a dismissable MODAL for a bulk-op outcome) — this is
 * a lightweight bottom-right toast that auto-fades, with no chat line of its own.
 *
 * Recorded by:  BulkNet (UNDO / REDO handling).
 * Consumed by:  BulkSnapshot.build (relay into the Hub); cleared on an explicit Hub open (no stale toast).
 */
package com.customblocks.command.handlers;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BulkToast {

    private BulkToast() {} // static-only

    private static final Map<UUID, String> PENDING = new ConcurrentHashMap<>();

    /** Record the actor's latest undo/redo toast (plain text; the client colours it). */
    public static void record(ServerPlayerEntity player, String plain) {
        if (player == null || plain == null || plain.isBlank()) return;
        PENDING.put(player.getUuid(), plain);
    }

    /** Take + clear the player's pending toast (null if none). Called once when a snapshot is built. */
    public static String consume(UUID player) {
        return player == null ? null : PENDING.remove(player);
    }

    /** Drop any pending toast (called on an explicit Hub open so a stale toast can't pop). */
    public static void clear(UUID player) {
        if (player != null) PENDING.remove(player);
    }
}
