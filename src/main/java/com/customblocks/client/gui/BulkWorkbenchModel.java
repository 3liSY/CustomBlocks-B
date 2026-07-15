/**
 * BulkWorkbenchModel.java — Group 07 §G07-3 (Bulk Workbench Screen). CLIENT-ONLY.
 *
 * Pure read/derive logic for the Workbench: which blocks a filter + search leaves visible, and what the
 * live "old → new" preview of an op would be. No drawing, no networking, no state — the Screen owns those.
 *
 * The filter semantics here MIRROR the server's core/BulkScope EXACTLY (all · category: · id: · name:
 * (+ trailing * = starts-with) · favorite:yes|no · locked:yes|no), because the expression this class builds
 * is the one shipped to the server when the player escalates a selection to "all N matches". If the two ever
 * disagree, the preview lies about what Apply will touch.
 *
 * Likewise {@link #preview} mirrors each handler's real skip rules rather than assuming a uniform one:
 * Edit / Rename / Move / Re-ID / Delete skip locked blocks; Duplicate, Export and the flag ops do NOT
 * (duplicating and exporting only READ the original, and lock/favorite are flags in their own right).
 * Re-ID additionally reproduces the handler's no-op / invalid-id / collision skips.
 *
 * Depends on: ClientSlotCache, BulkOpSpec.
 * Called by: BulkWorkbenchScreen, BulkWorkbenchView, BulkOpsView.
 */
package com.customblocks.client.gui;

import com.customblocks.client.ClientSlotCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
final class BulkWorkbenchModel {

    private BulkWorkbenchModel() {} // static-only

    /** Same id charset the server enforces (BulkReidCommands.VALID_ID). */
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9_.+-]+");

    /** The filter kinds offered by the Bulk tab's [filter ▾] control, in cycle order. */
    static final String[] FILTER_KINDS = {"all", "category", "favorite", "locked", "name", "id"};

    /** One row of the live preview: {@code old → new}, or a skip with a reason. */
    record PreviewRow(String id, String before, String after, String skipReason) {
        boolean skipped() { return skipReason != null; }
    }

    /** The Bulk tab's view filter. {@code kind} is one of {@link #FILTER_KINDS}; {@code value} may be blank. */
    record Filter(String kind, String value) {

        static Filter all() { return new Filter("all", ""); }

        /** True when this kind needs a typed value before it can select anything. */
        boolean needsValue() {
            return switch (kind) {
                case "category", "name", "id" -> true;
                default -> false;
            };
        }

        /** True when the filter is complete enough to narrow the list. */
        boolean active() { return !needsValue() || !value.isBlank(); }

        /** The BulkScope expression this filter sends to the server when the selection is escalated. */
        String expr() {
            return switch (kind) {
                case "category" -> "category:" + value.trim();
                case "favorite" -> "favorite:yes";
                case "locked"   -> "locked:yes";
                case "name"     -> "name:" + value.trim();
                case "id"       -> "id:" + value.trim();
                default         -> "all";
            };
        }

        /** Plain-words label for the confirm bar ("all 47 in category:stone"). */
        String label() {
            return switch (kind) {
                case "category" -> "category:" + value.trim();
                case "favorite" -> "favorited";
                case "locked"   -> "locked";
                case "name"     -> "name:" + value.trim();
                case "id"       -> "id:" + value.trim();
                default         -> "all blocks";
            };
        }
    }

    // ── multi-condition filter builder (§G07-4 AND/OR/NOT combinators) ─────────
    // Mirrors core/BulkScope's boolean eval EXACTLY (NOT > AND > OR, quoted values), so the Console's live
    // preview and the escalated server resolve agree on what Execute touches.

    /** Kinds that need a typed value before they narrow anything (favorite/locked carry a yes/no instead). */
    static boolean kindNeedsValue(String kind) {
        return switch (kind) { case "category", "name", "id" -> true; default -> false; };
    }

    /** One condition row in the builder. {@code negate} = NOT; "all" and blank value-kinds are inactive. */
    record Cond(String kind, String value, boolean negate) {
        boolean active() {
            return switch (kind) {
                case "all" -> false;
                case "category", "name", "id" -> !value.isBlank();
                default -> true; // favorite / locked always narrow
            };
        }
        /** The BulkScope token this condition contributes to the sent expression. */
        String token() {
            String body = switch (kind) {
                case "category" -> "category:" + quote(value);
                case "id"       -> "id:" + quote(value);
                case "name"     -> "name:" + quote(value);
                case "favorite" -> "favorite:" + (value.equalsIgnoreCase("no") ? "no" : "yes");
                case "locked"   -> "locked:" + (value.equalsIgnoreCase("no") ? "no" : "yes");
                default         -> "all";
            };
            return negate ? "NOT " + body : body;
        }
        private static String quote(String v) {
            return v != null && v.contains(" ") ? "\"" + v + "\"" : (v == null ? "" : v);
        }
    }

    private static boolean matchesCond(ClientSlotCache.Entry e, Cond c, Set<String> locked, Set<String> fav) {
        if (!c.active()) return true; // inactive condition passes through, so it never narrows on its own
        boolean v = switch (c.kind()) {
            case "category" -> e.category() != null && e.category().equalsIgnoreCase(c.value().trim());
            case "id"       -> e.id().toLowerCase(Locale.ROOT).startsWith(c.value().trim().toLowerCase(Locale.ROOT));
            case "name"     -> {
                String pat = c.value().trim().toLowerCase(Locale.ROOT);
                boolean wildcard = pat.endsWith("*");
                String term = wildcard ? pat.substring(0, pat.length() - 1) : pat;
                String name = e.name().toLowerCase(Locale.ROOT);
                yield wildcard ? name.startsWith(term) : name.contains(term);
            }
            case "favorite" -> fav.contains(e.id()) == !c.value().equalsIgnoreCase("no");
            case "locked"   -> locked.contains(e.id()) == !c.value().equalsIgnoreCase("no");
            default         -> true;
        };
        return c.negate() ? !v : v;
    }

    /** Every block the builder selects — the escalation target. {@code conns} holds the connector before each
     *  condition after the first (size = conds − 1). */
    static List<ClientSlotCache.Entry> matchingBool(List<Cond> conds, List<String> conns,
                                                    Set<String> locked, Set<String> fav) {
        List<ClientSlotCache.Entry> out = new ArrayList<>();
        for (ClientSlotCache.Entry e : ClientSlotCache.entries()) if (evalConds(e, conds, conns, locked, fav)) out.add(e);
        out.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return out;
    }

    private static boolean evalConds(ClientSlotCache.Entry e, List<Cond> conds, List<String> conns,
                                     Set<String> locked, Set<String> fav) {
        boolean result = false, group = true;
        for (int i = 0; i < conds.size(); i++) {
            boolean tv = matchesCond(e, conds.get(i), locked, fav);
            if (i == 0) { group = tv; continue; }
            String conn = i - 1 < conns.size() ? conns.get(i - 1) : "AND";
            if ("OR".equalsIgnoreCase(conn)) { result = result || group; group = tv; }
            else group = group && tv;
        }
        return result || group;
    }

    /** The BulkScope expression the builder sends on escalate (what the client just evaluated locally). */
    static String exprOf(List<Cond> conds, List<String> conns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < conds.size(); i++) {
            if (i > 0) sb.append(' ').append(i - 1 < conns.size() ? conns.get(i - 1) : "AND").append(' ');
            sb.append(conds.get(i).token());
        }
        return sb.toString();
    }

    /** True when at least one condition actually narrows — so escalation and the label mean something. */
    static boolean anyActive(List<Cond> conds) {
        for (Cond c : conds) if (c.active()) return true;
        return false;
    }

    // ── selection ────────────────────────────────────────────────────────────

    /** Every block the filter selects — the escalation target. Mirrors BulkScope.resolve. */
    static List<ClientSlotCache.Entry> matching(Filter f, Set<String> locked, Set<String> fav) {
        List<ClientSlotCache.Entry> out = new ArrayList<>();
        if (!f.active()) return out;
        for (ClientSlotCache.Entry e : ClientSlotCache.entries()) if (matches(e, f, locked, fav)) out.add(e);
        out.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return out;
    }

    /** The rows actually drawn: the filter's matches, further narrowed by the search box (name or id). */
    static List<ClientSlotCache.Entry> narrow(String search, List<ClientSlotCache.Entry> matches) {
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return matches;
        List<ClientSlotCache.Entry> out = new ArrayList<>();
        for (ClientSlotCache.Entry e : matches) {
            if (e.name().toLowerCase(Locale.ROOT).contains(q) || e.id().toLowerCase(Locale.ROOT).contains(q)) out.add(e);
        }
        return out;
    }

    /** Convenience for the Browse tab, which has no filter control — one resolve, one narrow. */
    static List<ClientSlotCache.Entry> visible(String search, Filter f, Set<String> locked, Set<String> fav) {
        return narrow(search, matching(f, locked, fav));
    }

    private static boolean matches(ClientSlotCache.Entry e, Filter f, Set<String> locked, Set<String> fav) {
        String value = f.value().trim();
        return switch (f.kind()) {
            case "category" -> e.category() != null && e.category().equalsIgnoreCase(value);
            case "id"       -> e.id().toLowerCase(Locale.ROOT).startsWith(value.toLowerCase(Locale.ROOT));
            case "name"     -> {
                String pattern = value.toLowerCase(Locale.ROOT);
                boolean wildcard = pattern.endsWith("*");
                String term = wildcard ? pattern.substring(0, pattern.length() - 1) : pattern;
                String name = e.name().toLowerCase(Locale.ROOT);
                yield wildcard ? name.startsWith(term) : name.contains(term);
            }
            case "favorite" -> fav.contains(e.id());
            case "locked"   -> locked.contains(e.id());
            default         -> true;
        };
    }

    /** Look up one synced entry by id, or null. */
    static ClientSlotCache.Entry byId(String id) {
        for (ClientSlotCache.Entry e : ClientSlotCache.entries()) if (e.id().equals(id)) return e;
        return null;
    }

    // ── preview ──────────────────────────────────────────────────────────────

    /**
     * The exact {@code old → new} the server would produce, in the order the handler walks the batch.
     * {@code ids} is the resolved scope (ticked ids, or every match after an escalate).
     */
    static List<PreviewRow> preview(int op, String p1, String p2, String p3,
                                    List<String> ids, Set<String> locked, Set<String> fav) {
        List<PreviewRow> rows = new ArrayList<>();
        // Re-ID needs the full id space to detect collisions the way the handler does.
        Set<String> taken = new HashSet<>();
        if (op == BulkOpSpec.OP_REID) for (ClientSlotCache.Entry e : ClientSlotCache.entries()) taken.add(e.id());

        for (String id : ids) {
            ClientSlotCache.Entry e = byId(id);
            if (e == null) continue;
            if (BulkOpSpec.skipsLocked(op) && locked.contains(id)) {
                rows.add(new PreviewRow(id, "", "", "locked"));
                continue;
            }
            switch (op) {
                case BulkOpSpec.OP_PROPERTY -> rows.add(propertyRow(e, p1, p2));
                case BulkOpSpec.OP_RENAME -> {
                    String after = transform(e.name(), p1, p2, p3);
                    rows.add(after.equals(e.name())
                            ? new PreviewRow(id, e.name(), "", "unchanged")
                            : new PreviewRow(id, e.name(), after, null));
                }
                case BulkOpSpec.OP_CATEGORY -> {
                    String target = normalizeCategory(p1);
                    rows.add(new PreviewRow(id, blank(e.category()), blank(target), null));
                }
                case BulkOpSpec.OP_REID -> rows.add(reidRow(id, p1, p2, p3, taken));
                case BulkOpSpec.OP_DUPLICATE -> rows.add(new PreviewRow(id, id, id + "_copy", null));
                case BulkOpSpec.OP_DELETE -> rows.add(new PreviewRow(id, e.name(), "deleted", null));
                case BulkOpSpec.OP_RECOLOR -> rows.add(new PreviewRow(id, "original", "hue " + p1 + "°", null));
                case BulkOpSpec.OP_LOCK, BulkOpSpec.OP_FAVORITE -> rows.add(flagRow(id, p1, locked, fav));
                default -> { } // Export writes a file — no per-block old→new to show (BulkOpSpec.hasPreview)
            }
        }
        return rows;
    }

    private static PreviewRow propertyRow(ClientSlotCache.Entry e, String prop, String value) {
        String before = switch (prop) {
            case "glow"      -> String.valueOf(e.glow());
            case "hardness"  -> trimFloat(e.hardness());
            case "sound"     -> e.sound();
            case "collision" -> e.passable() ? "passable" : "solid";
            default          -> "?";
        };
        return new PreviewRow(e.id(), before, displayValue(prop, value), null);
    }

    /**
     * Render a raw pane value the way BulkValues.parse's {@code display} will: "unbreakable" → "-1",
     * glow clamped to 0-15. Without this the preview reads "1.5 → stone" instead of "1.5 → 1.5".
     */
    static String displayValue(String prop, String value) {
        String raw = value == null ? "" : value.trim();
        switch (prop) {
            case "glow" -> {
                try { return String.valueOf(Math.max(0, Math.min(15, Integer.parseInt(raw)))); }
                catch (NumberFormatException e) { return raw; }
            }
            case "hardness" -> {
                Float h = parseHardness(raw);
                return h == null ? raw : trimFloat(Math.max(-1.0f, Math.min(100.0f, h)));
            }
            case "collision" -> {
                return switch (raw.toLowerCase(Locale.ROOT)) {
                    case "passable", "through", "walkthrough", "walk", "none",
                         "off", "no", "false", "ghost", "decor" -> "passable";
                    default -> "solid";
                };
            }
            default -> { return raw.toLowerCase(Locale.ROOT); }
        }
    }

    /** Reproduces BulkReidCommands' per-block skips so the preview never promises a rename it can't do. */
    private static PreviewRow reidRow(String oldId, String mode, String a, String b, Set<String> taken) {
        String newId = transform(oldId, mode, a, b);
        if (newId.equals(oldId)) return new PreviewRow(oldId, oldId, "", "unchanged");
        if (!VALID_ID.matcher(newId).matches()) return new PreviewRow(oldId, oldId, newId, "invalid id");
        if (taken.contains(newId)) return new PreviewRow(oldId, oldId, newId, "id taken");
        taken.remove(oldId);
        taken.add(newId);
        return new PreviewRow(oldId, oldId, newId, null);
    }

    private static PreviewRow flagRow(String id, String flagOp, Set<String> locked, Set<String> fav) {
        boolean on = switch (flagOp) {
            case "lock", "unlock" -> locked.contains(id);
            default               -> fav.contains(id);
        };
        boolean want = flagOp.equals("lock") || flagOp.equals("favorite");
        String state = switch (flagOp) {
            case "lock", "unlock" -> on ? "locked" : "unlocked";
            default               -> on ? "favorite" : "not favorite";
        };
        if (on == want) return new PreviewRow(id, state, "", "already " + state);
        String after = switch (flagOp) {
            case "lock"   -> "locked";
            case "unlock" -> "unlocked";
            case "favorite" -> "favorite";
            default       -> "not favorite";
        };
        return new PreviewRow(id, state, after, null);
    }

    // ── Re-ID by pattern template (§G27.22b) ──────────────────────────────────
    // The new Bulk Actions Re-ID is a single TEMPLATE that becomes each block's id, with {n}/{nn} auto-numbering
    // (planet_{n} → planet_1, planet_2…). Locked blocks are shown-but-skipped and don't consume a number; the
    // rest number 1..k in the given order. Reproduces the server's per-block invalid/taken/clash/no-op skips so
    // the preview never promises a re-id the handler can't do.

    /** Substitute {n} (1,2,3…) and {nn} (01,02…) in a template. */
    static String numTokens(String template, int n) {
        if (template == null) return "";
        return template.replace("{nn}", String.format("%02d", n)).replace("{n}", String.valueOf(n));
    }

    /** The rows the §G27.22b Re-ID would produce, numbered over {@code ids} (locked skipped, not numbered). */
    static List<PreviewRow> reidPreview(List<String> ids, String template, Set<String> locked) {
        List<PreviewRow> rows = new ArrayList<>();
        Set<String> taken = new HashSet<>();
        for (ClientSlotCache.Entry e : ClientSlotCache.entries()) taken.add(e.id());
        boolean blank = template == null || template.isBlank();
        int n = 0;
        for (String id : ids) {
            if (byId(id) == null) continue;
            if (locked.contains(id)) { rows.add(new PreviewRow(id, id, "", "locked")); continue; }
            n++;
            String newId = blank ? id : numTokens(template, n);
            if (newId.equals(id)) { rows.add(new PreviewRow(id, id, "", "unchanged")); continue; }
            if (!VALID_ID.matcher(newId).matches()) { rows.add(new PreviewRow(id, id, newId, "invalid id")); continue; }
            if (taken.contains(newId)) { rows.add(new PreviewRow(id, id, newId, "id taken")); continue; }
            taken.remove(id);
            taken.add(newId);
            rows.add(new PreviewRow(id, id, newId, null));
        }
        return rows;
    }

    /** prefix / suffix / replace — the one transform bulkrename and bulkreid share (B3 live example uses it). */
    static String transform(String source, String mode, String a, String b) {
        return switch (mode) {
            case "prefix"  -> a + source;
            case "suffix"  -> source + a;
            case "replace" -> source.replace(a, b);
            default        -> source;
        };
    }

    /** Mirrors BulkCategoryCommands.normalize: none/clear/uncategorized → "", else lowercase. */
    static String normalizeCategory(String raw) {
        String c = raw == null ? "" : raw.trim();
        if (c.equalsIgnoreCase("none") || c.equalsIgnoreCase("clear") || c.equalsIgnoreCase("uncategorized")) return "";
        return c.toLowerCase(Locale.ROOT);
    }

    /** Mirrors BulkValues.parseHardness: a number, or a friendly keyword. Null when unrecognized. */
    private static Float parseHardness(String raw) {
        switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "unbreakable": case "unbreak": case "indestructible": case "bedrock": return -1.0f;
            case "instant": case "instabreak": case "instant-break": case "soft":      return 0.0f;
            case "stone": case "default": case "normal":                               return 1.5f;
            default:
                try { return Float.parseFloat(raw.trim()); }
                catch (NumberFormatException e) { return null; }
        }
    }

    private static String blank(String s) { return s == null || s.isEmpty() ? "(none)" : s; }

    /** Drop a trailing ".0" so 50.0 reads as "50" but 1.5 stays "1.5" (matches BulkValues.trim). */
    static String trimFloat(float v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
    }
}
