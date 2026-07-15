/**
 * BulkSession.java
 *
 * Per-player state for the one chest flow that still hands blocks to a bulk operation: Group 12's Export
 * Dashboard "Bulk Choose" tile. It sets {@link #listPickForExport}, opens the Bulk Workbench in "pick" mode,
 * and {@code BulkNet.PICK_DONE} reads the flag back to decide whether an incoming pick is legitimate.
 *
 * Everything else this class used to hold — the op, the filter, the property/value, the rename and export
 * choices, the whole two-step selection state — belonged to the chest Bulk Hub → Select → Action → Confirm
 * menus, which Group 07 §G07-3 deleted. The Bulk Workbench Screen keeps that state client-side and ships it
 * with each BulkActionPayload, so the server no longer needs a session for it at all.
 *
 * Nothing here mutates blocks.
 *
 * Called by: ExportDashboardMenu (sets the flag), BulkNet (reads + clears it).
 */
package com.customblocks.gui.chest;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BulkSession {

    private static final Map<UUID, BulkSession> SESSIONS = new ConcurrentHashMap<>();

    /** True while the block browser is being used to hand-pick blocks for the Export Dashboard. */
    public boolean listPickForExport = false;

    public static BulkSession get(UUID player) {
        return SESSIONS.computeIfAbsent(player, k -> new BulkSession());
    }
}
