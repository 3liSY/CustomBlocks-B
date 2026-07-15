/**
 * BulkOpText.java — Group 07 (Bulk Operations Hub). CLIENT-ONLY.
 *
 * Pure text derivation for the Console tab, split out of {@link BulkWorkbenchScreen} to keep it under the
 * ≤500-line cap (§9.3). Every method reads the Screen's current intent through its package-private getters and
 * returns a display string — no state, no drawing, no mutation. Two related surfaces share the same colour
 * language (B3): added text in §e, replaced-away text in §c, so the control strip's live example and each
 * list row's inline "old → new" read as one.
 *
 * Depends on: BulkWorkbenchScreen (getters), BulkWorkbenchModel, BulkOpSpec, ClientSlotCache.
 * Called by:  BulkOpsView (example + summary), BulkWorkbenchView (inline preview).
 */
package com.customblocks.client.gui;

import com.customblocks.client.ClientSlotCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class BulkOpText {

    private BulkOpText() {} // static-only

    /** The one-line "what Execute will do" the confirm modal leads with. */
    static String summary(BulkWorkbenchScreen s) {
        return switch (s.opIndex()) {
            case BulkOpSpec.OP_PROPERTY  -> s.property() + " → " + BulkWorkbenchModel.displayValue(s.property(), s.value());
            case BulkOpSpec.OP_RENAME    -> renameWhat(s, "name");
            case BulkOpSpec.OP_REID      -> "re-id from pattern \"" + s.textA() + "\"";
            case BulkOpSpec.OP_CATEGORY  -> BulkWorkbenchModel.normalizeCategory(s.textA()).isEmpty()
                    ? "clear the category" : "to category \"" + BulkWorkbenchModel.normalizeCategory(s.textA()) + "\"";
            case BulkOpSpec.OP_DUPLICATE -> "each block → <id>_copy";
            case BulkOpSpec.OP_EXPORT    -> "format " + s.exportFormat();
            case BulkOpSpec.OP_LOCK      -> s.lockMode();
            case BulkOpSpec.OP_FAVORITE  -> s.favMode();
            case BulkOpSpec.OP_RECOLOR   -> "hue-shift " + s.recolorHue() + "° (edge mode)";
            default                      -> "one /cb undo restores them";
        };
    }

    private static String renameWhat(BulkWorkbenchScreen s, String noun) {
        return switch (s.textMode()) {
            case "prefix" -> "add " + noun + "-prefix \"" + s.textA() + "\"";
            case "suffix" -> "add " + noun + "-suffix \"" + s.textA() + "\"";
            default       -> "replace \"" + s.textA() + "\" → \"" + s.textB() + "\" in " + noun + "s";
        };
    }

    /**
     * A live, colour-highlighted "before → after" example for the Rename/Re-ID controls (B3 polish). Uses a real
     * targeted block when there is one, else a placeholder, and highlights the change the SAME way each list row
     * does. For {@code replace} it also flags §8(no match) when the find-text isn't in the sample. "" otherwise.
     */
    static String example(BulkWorkbenchScreen s) {
        // §G27.22b Re-ID is a pattern TEMPLATE with {n} numbering — its own example shape.
        if (s.opIndex() == BulkOpSpec.OP_REID) {
            String sampleId = "block_id";
            for (String sid : s.scopeIds()) { if (BulkWorkbenchModel.byId(sid) != null) { sampleId = sid; break; } }
            String t = s.textA();
            return t.isBlank() ? "§7e.g. §f" + sampleId + " §8→ §7…"
                    : "§7e.g. §f" + sampleId + " §8→ §a" + BulkWorkbenchModel.numTokens(t, 1);
        }
        if (s.opIndex() != BulkOpSpec.OP_RENAME) return "";
        String mode = s.textMode(), a = s.textA(), b = s.textB();
        String sample = null;
        for (String sid : s.scopeIds()) {
            ClientSlotCache.Entry e = BulkWorkbenchModel.byId(sid);
            if (e != null) { sample = e.name(); break; }
        }
        if (sample == null) sample = "Block Name";
        if (a.isBlank() && !"replace".equals(mode)) return "§7e.g. §f" + sample + " §8→ §7…";
        String after = BulkWorkbenchModel.transform(sample, mode, a, b);
        String beforeC, afterC;
        switch (mode) {
            case "prefix" -> { beforeC = "§f" + sample;              afterC = "§e" + a + "§f" + sample; }
            case "suffix" -> { beforeC = "§f" + sample;              afterC = "§f" + sample + "§e" + a; }
            default       -> { beforeC = hi(sample, a, "§c", "§f");  afterC = hi(after, b, "§e", "§f"); }
        }
        String tail = ("replace".equals(mode) && !a.isEmpty() && !sample.contains(a)) ? "  §8(no match)" : "";
        return "§7e.g. " + beforeC + " §8→ " + afterC + tail;
    }

    /**
     * The inline "old → new" a targeted row shows on the Console tab (Dir-2 "Result"). For Rename/Re-ID it
     * match-highlights the change (B3). Returns null when the row is not in scope, a §c reason when it would skip.
     */
    static String inlinePreview(BulkWorkbenchScreen s, String id) {
        BulkWorkbenchModel.PreviewRow p = s.previewFor(id);
        if (p == null) return null;
        if (p.skipped()) return "§c" + p.skipReason();
        if (s.opIndex() == BulkOpSpec.OP_RENAME || s.opIndex() == BulkOpSpec.OP_REID) {
            String a = s.textA(), b = s.textB();
            return switch (s.textMode()) {
                case "prefix" -> "§8" + p.before() + " §7→ §e" + a + "§f" + p.before();
                case "suffix" -> "§8" + p.before() + " §7→ §f" + p.before() + "§e" + a;
                default       -> hi(p.before(), a, "§c", "§8") + " §7→ " + hi(p.after(), b, "§e", "§f");
            };
        }
        return "§8" + p.before() + " §7→ §f" + p.after();
    }

    /** {change, skip(locked), problem(no-op/invalid/taken/clash)} for the impact line (3e), for both the
     *  preview ops and the Re-ID editor. */
    static int[] impact(BulkWorkbenchScreen s) {
        int change = 0, skip = 0, problem = 0;
        for (BulkWorkbenchModel.PreviewRow r : s.preview()) {
            if (!r.skipped()) change++;
            else if ("locked".equals(r.skipReason())) skip++;
            else problem++;                            // taken / invalid / clash / unchanged
        }
        return new int[]{change, skip, problem};
    }

    /** Wrap every occurrence of {@code needle} in {@code hay} with {@code code}, returning to {@code base} after. */
    private static String hi(String hay, String needle, String code, String base) {
        if (needle == null || needle.isEmpty() || !hay.contains(needle)) return base + hay;
        return base + hay.replace(needle, code + needle + base);
    }
}
