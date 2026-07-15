/**
 * BulkScope.java
 *
 * Responsibility: resolve a bulk-operation id expression to the matching SlotData list.
 * Pure read over SlotManager — no mutation, no server types.
 *
 * Supported expressions (Group 07, trimmed to id-shaped forms only — §G07 D-cleanup 2026-07-13):
 *   all / blank        — every assigned block (callers force a confirm for "all")
 *   &lt;id1&gt; &lt;id2&gt; ...     — explicit id list, plain spaces (Group 04 §A7, owner 2026-07-14 — the form you type)
 *   &lt;id1&gt;,&lt;id2&gt;,...     — explicit id list, commas
 *   "id1" "id2" ...    — quoted explicit id list (§G07-4 A5a; ids with awkward characters stay intact)
 *   &lt;id&gt;               — one block by exact id
 *
 * The old category:/id:/name:/favorite:/locked: filter language and the AND/OR/NOT boolean combinator were
 * removed — block picking for bulk ops now lives on the Hub screen (Blocks List ticks) or the NL bar; chat
 * commands only take an explicit id set.
 *
 * An unknown token is SKIPPED, not an error — a list that matches nothing comes back empty, and the callers
 * all report "no blocks matched". That is what makes a dead `category:red` fail loudly instead of silently
 * selecting something else.
 *
 * Depends on: SlotData, SlotManager
 * Called by:  command/handlers/BulkCommands
 */
package com.customblocks.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class BulkScope {

    private BulkScope() {} // static-only

    /** One "quoted" token — backs the /cb bulklock "id1" "id2" … multi-id form. */
    private static final java.util.regex.Pattern QUOTED = java.util.regex.Pattern.compile("\"([^\"]*)\"");

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

        // Quoted explicit id list — /cb bulklock "id1" "id2" … (§G07-4 A5a). Each "…" is one id; unquoted
        // scraps between quotes are ignored, so a stray space can't silently widen or narrow the batch.
        if (raw.indexOf('"') >= 0) {
            List<SlotData> out = new ArrayList<>();
            java.util.regex.Matcher m = QUOTED.matcher(raw);
            while (m.find()) {
                SlotData d = SlotManager.getById(m.group(1).trim());
                if (d != null) out.add(d);
            }
            return out;
        }

        // Comma-separated explicit id list. Checked before the space form so "id1, id2" (comma AND space)
        // splits on the comma and the stray spaces get trimmed off, rather than producing an empty token.
        if (raw.contains(",")) {
            List<SlotData> out = new ArrayList<>();
            for (String token : raw.split(",")) {
                SlotData d = SlotManager.getById(token.trim());
                if (d != null) out.add(d);
            }
            return out;
        }

        // Space-separated explicit id list — /cb bulkdelete id1 id2 id3 (Group 04 §A7, owner 2026-07-14).
        // Every bulk command takes its scope as a greedy string, so the whole list arrives here as one
        // expression and this is the only place that needs to understand it.
        if (raw.matches(".*\\s.*")) {
            List<SlotData> out = new ArrayList<>();
            for (String token : raw.split("\\s+")) {
                SlotData d = SlotManager.getById(token);
                if (d != null) out.add(d);
            }
            return out;
        }

        // Single exact id.
        SlotData single = SlotManager.getById(raw);
        return single != null ? new ArrayList<>(List.of(single)) : List.of();
    }

}
