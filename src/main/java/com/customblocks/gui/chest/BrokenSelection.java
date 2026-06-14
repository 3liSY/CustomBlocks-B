/**
 * BrokenSelection.java — per-player "ticked broken blocks" set for the /cb showbrokenblocks GUI (Slice 5).
 *
 * A SEPARATE store from ListSelection (block id) and BackupSelection (backup name), keyed on the customIds
 * of broken blocks, so the three selections never collide. Used by BrokenBlocksMenu (tick / select-all /
 * clear / fix-selected) and BrokenConfirmMenu (bulk delete). Stale ids are harmless — SlotManager skips
 * ids that no longer resolve.
 *
 * Called by: BrokenBlocksMenu, BrokenConfirmMenu.
 */
package com.customblocks.gui.chest;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BrokenSelection {

    private BrokenSelection() {} // static-only

    private static final Map<UUID, Set<String>> SELECTIONS = new ConcurrentHashMap<>();

    private static Set<String> set(UUID u) {
        return SELECTIONS.computeIfAbsent(u, k -> new LinkedHashSet<>());
    }

    public static boolean toggle(UUID u, String id) {
        Set<String> s = set(u);
        if (s.remove(id)) return false;
        s.add(id);
        return true;
    }

    public static boolean has(UUID u, String id) { return set(u).contains(id); }
    public static int size(UUID u) { return set(u).size(); }
    public static void addAll(UUID u, Collection<String> ids) { set(u).addAll(ids); }
    public static Set<String> ids(UUID u) { return new LinkedHashSet<>(set(u)); }
    public static void clear(UUID u) { SELECTIONS.remove(u); }
}
