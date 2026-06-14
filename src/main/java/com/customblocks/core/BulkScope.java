/**
 * BulkScope.java
 *
 * Responsibility: resolve a bulk-operation filter expression to the matching SlotData list.
 * Pure read over SlotManager / LockManager / FavoritesManager — no mutation, no server types.
 *
 * Ported and upgraded from the old mod's BulkScope: adapted to the immutable SlotData record
 * and the new SlotManager API, and extended with the id:&lt;prefix&gt; filter the old resolver
 * lacked (the old one only matched an exact id or a comma-list of ids).
 *
 * Supported expressions (Group 07):
 *   all / blank        — every assigned block (callers force a confirm for "all")
 *   category:&lt;name&gt;     — blocks in that category (case-insensitive exact)
 *   id:&lt;prefix&gt;        — blocks whose id starts with the prefix
 *   name:&lt;text&gt;        — display name contains text  (name:&lt;text&gt;* = starts-with)
 *   favorite:yes|no    — favorited / not, for the given player (null player → empty)
 *   locked:yes|no      — locked / unlocked
 *   &lt;id1&gt;,&lt;id2&gt;,...     — explicit id list
 *   &lt;id&gt;               — one block by exact id
 *
 * Depends on: SlotData, SlotManager, LockManager, FavoritesManager
 * Called by:  command/handlers/BulkCommands
 */
package com.customblocks.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class BulkScope {

    private BulkScope() {} // static-only

    /** True when the expression selects everything — callers always confirm "all". */
    public static boolean isAll(String expr) {
        return expr == null || expr.isBlank() || "all".equalsIgnoreCase(expr.trim());
    }

    /**
     * Resolve the filter to matching blocks. Never null; an empty list means "no match".
     *
     * @param expr       the filter string (see class doc)
     * @param playerUuid player whose favorites are consulted (null → favorite filter yields empty)
     */
    public static List<SlotData> resolve(String expr, UUID playerUuid) {
        if (isAll(expr)) return new ArrayList<>(SlotManager.assignedSlots());
        String raw = expr.trim();
        String low = raw.toLowerCase(Locale.ROOT);

        if (low.startsWith("category:")) {
            return SlotManager.byCategory(raw.substring("category:".length()).trim());
        }
        if (low.startsWith("id:")) {
            String prefix = low.substring("id:".length()).trim();
            List<SlotData> out = new ArrayList<>();
            for (SlotData d : SlotManager.assignedSlots()) {
                if (d.customId().toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(d);
            }
            return out;
        }
        if (low.startsWith("name:")) {
            String pattern = raw.substring("name:".length()).trim().toLowerCase(Locale.ROOT);
            boolean wildcard = pattern.endsWith("*");
            String term = wildcard ? pattern.substring(0, pattern.length() - 1) : pattern;
            List<SlotData> out = new ArrayList<>();
            for (SlotData d : SlotManager.assignedSlots()) {
                String name = d.displayName().toLowerCase(Locale.ROOT);
                if (wildcard ? name.startsWith(term) : name.contains(term)) out.add(d);
            }
            return out;
        }
        if (low.startsWith("favorite:")) {
            if (playerUuid == null) return List.of();
            boolean want = "yes".equals(low.substring("favorite:".length()).trim());
            List<SlotData> out = new ArrayList<>();
            for (SlotData d : SlotManager.assignedSlots()) {
                if (FavoritesManager.isFavorite(playerUuid, d.customId()) == want) out.add(d);
            }
            return out;
        }
        if (low.startsWith("locked:")) {
            boolean want = "yes".equals(low.substring("locked:".length()).trim());
            List<SlotData> out = new ArrayList<>();
            for (SlotData d : SlotManager.assignedSlots()) {
                if (LockManager.isLocked(d.customId()) == want) out.add(d);
            }
            return out;
        }

        // Comma-separated explicit id list.
        if (raw.contains(",")) {
            List<SlotData> out = new ArrayList<>();
            for (String token : raw.split(",")) {
                SlotData d = SlotManager.getById(token.trim());
                if (d != null) out.add(d);
            }
            return out;
        }

        // Single exact id.
        SlotData single = SlotManager.getById(raw);
        return single != null ? new ArrayList<>(List.of(single)) : List.of();
    }
}
