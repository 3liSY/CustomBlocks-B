/**
 * BackupSelection.java — per-player "ticked backups" set for the /cb backup GUI (Group 09, Slice 2 GUI).
 *
 * Mirrors {@link ListSelection} but is a SEPARATE store keyed on backup NAMES, so a player's block
 * selection and backup selection never collide. Used by BackupMenu (tick / select-all / clear) and
 * BackupConfirmMenu (bulk delete). Nothing here touches files — it only remembers chosen names; the
 * actual delete is done by BackupManager. Stale names (a backup deleted after being ticked) are
 * harmless — BackupManager.delete simply returns false for them.
 *
 * Called by: BackupMenu, BackupConfirmMenu.
 */
package com.customblocks.gui.chest;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BackupSelection {

    private BackupSelection() {} // static-only

    private static final Map<UUID, Set<String>> SELECTIONS = new ConcurrentHashMap<>();

    private static Set<String> set(UUID u) {
        return SELECTIONS.computeIfAbsent(u, k -> new LinkedHashSet<>());
    }

    /** Toggle a backup name; returns true if it is now selected, false if it was removed. */
    public static boolean toggle(UUID u, String name) {
        Set<String> s = set(u);
        if (s.remove(name)) return false;
        s.add(name);
        return true;
    }

    public static boolean has(UUID u, String name) { return set(u).contains(name); }

    public static int size(UUID u) { return set(u).size(); }

    public static void addAll(UUID u, Collection<String> names) { set(u).addAll(names); }

    /** A copy of the selected names (selection order preserved). */
    public static Set<String> names(UUID u) { return new LinkedHashSet<>(set(u)); }

    /** Forget a player's selection (also safe to call on disconnect). */
    public static void clear(UUID u) { SELECTIONS.remove(u); }
}
