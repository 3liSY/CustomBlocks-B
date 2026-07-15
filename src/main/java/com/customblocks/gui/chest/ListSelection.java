/**
 * ListSelection.java — per-player "ticked blocks" set for Group 12's Export Dashboard hand-off.
 *
 * A plain id set (LinkedHashSet → selection order preserved). Nothing here mutates blocks; the dashboard's
 * "selection" phase reads {@link #joined} as a BulkScope explicit-id list and hands the actual work to the
 * tested bulk commands. Stale ids (a block deleted after being ticked) are harmless — BulkScope simply skips
 * ids that no longer resolve.
 *
 * The chest BlockListMenu that used to fill this is gone (Group 07 §G07-3); the Bulk Workbench Screen ticks
 * blocks client-side and posts the chosen ids once, via BulkNet's PICK_DONE.
 *
 * Called by: BulkNet (PICK_DONE writes it), ExportDashboardMenu (reads it).
 */
package com.customblocks.gui.chest;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ListSelection {

    private ListSelection() {} // static-only

    private static final Map<UUID, Set<String>> SELECTIONS = new ConcurrentHashMap<>();

    private static Set<String> set(UUID u) {
        return SELECTIONS.computeIfAbsent(u, k -> new LinkedHashSet<>());
    }

    /** Toggle an id; returns true if it is now selected, false if it was removed. */
    public static boolean toggle(UUID u, String id) {
        Set<String> s = set(u);
        if (s.remove(id)) return false;
        s.add(id);
        return true;
    }

    public static boolean has(UUID u, String id) { return set(u).contains(id); }

    public static int size(UUID u) { return set(u).size(); }

    public static void addAll(UUID u, Collection<String> ids) { set(u).addAll(ids); }

    /** Forget a player's selection (also safe to call on disconnect). */
    public static void clear(UUID u) { SELECTIONS.remove(u); }

    /** The selected ids as a comma-separated list (BulkScope's explicit-id-list syntax). */
    public static String joined(UUID u) { return String.join(",", set(u)); }
}
