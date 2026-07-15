/**
 * BulkNlParser.java — Group 07 §G07-4 (natural-language command bar). CLIENT-ONLY.
 *
 * A deterministic, heuristic parser turning a plain-English instruction into a bulk op + a filter — no AI
 * dependency, so it's instant and testable. It does NOT run anything: it produces an {@link Nl} that the Screen
 * drops into its existing Console state (op + single-condition filter + value), so the normal live preview and
 * the Execute confirm-modal take over — the NL bar only expresses intent, exactly like typing the op/filter by
 * hand. Unknown phrasings return an {@link Nl} carrying an {@code error} the bar shows instead of running.
 *
 * Examples it understands:
 *   "delete all locked"            → Delete · locked:yes
 *   "set glow to 10 on red blocks" → Edit glow=10 · category:red
 *   "lock every favorite"          → Lock · favorite:yes
 *   "move all to stone"            → Move category=stone · all
 *   "duplicate category red"       → Duplicate · category:red
 *
 * Depends on: BulkOpSpec (op codes + value lists). Called by: BulkWorkbenchScreen (doNlParse → applyNl).
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
final class BulkNlParser {

    private BulkNlParser() {} // static-only

    private static final Pattern INT = Pattern.compile("-?\\d+");

    /** A parsed instruction. {@code error != null} means "show this, don't run". Otherwise apply the fields. */
    record Nl(int opIndex, String property, String textMode, String value,
              String filterKind, String filterValue, boolean filterNeg, String desc, String error) {
        static Nl err(String msg) { return new Nl(-1, "", "prefix", "", "all", "", false, "", msg); }
        boolean ok() { return error == null; }
    }

    static Nl parse(String raw) {
        String s = raw == null ? "" : raw.toLowerCase(Locale.ROOT).trim();
        if (s.isBlank()) return Nl.err("Type an instruction — e.g. \"delete all locked\" or \"set glow 10 on red\".");

        int op;
        String property = "", textMode = "prefix", value = "";

        if (has(s, "delete", "remove")) { op = BulkOpSpec.OP_DELETE; }
        else if (has(s, "duplicate", "clone") || (s.contains("copy") && !s.contains("_copy"))) { op = BulkOpSpec.OP_DUPLICATE; }
        else if (s.contains("export")) { op = BulkOpSpec.OP_EXPORT; value = firstOf(s, BulkOpSpec.EXPORT_FORMATS, "json"); }
        else if (s.contains("unlock")) { op = BulkOpSpec.OP_LOCK; value = "unlock"; }
        else if (s.contains("lock")) { op = BulkOpSpec.OP_LOCK; value = "lock"; }
        else if (has(s, "unfavorite", "unstar")) { op = BulkOpSpec.OP_FAVORITE; value = "unfavorite"; }
        else if (has(s, "favorite", "favourite", "star")) { op = BulkOpSpec.OP_FAVORITE; value = "favorite"; }
        else if (has(s, "glow", "light", "luminance")) {
            op = BulkOpSpec.OP_PROPERTY; property = "glow";
            Integer n = firstInt(s);
            if (n == null) return Nl.err("Glow needs a number — e.g. \"set glow 10 on all\".");
            value = String.valueOf(Math.max(0, Math.min(15, n)));
        } else if (has(s, "hardness", "strength")) {
            op = BulkOpSpec.OP_PROPERTY; property = "hardness";
            value = firstOf(s, BulkOpSpec.HARDNESS_VALUES, null);
            if (value == null) { Integer n = firstInt(s); value = n == null ? "stone" : String.valueOf(n); }
        } else if (s.contains("sound")) {
            op = BulkOpSpec.OP_PROPERTY; property = "sound";
            value = firstOf(s, BulkOpSpec.valuesForProperty("sound"), null);
            if (value == null) return Nl.err("Which sound? e.g. \"set sound wood on all\".");
        } else if (has(s, "passable", "walk through", "walkthrough", "no collision", "noclip")) {
            op = BulkOpSpec.OP_PROPERTY; property = "collision"; value = "passable";
        } else if (has(s, "solid", "collision", "collide")) {
            op = BulkOpSpec.OP_PROPERTY; property = "collision"; value = "solid";
        } else if (s.contains("prefix")) {
            op = BulkOpSpec.OP_RENAME; textMode = "prefix"; value = after(s, "prefix");
            if (value.isBlank()) return Nl.err("Prefix with what? e.g. \"prefix red_ on all\".");
        } else if (s.contains("suffix")) {
            op = BulkOpSpec.OP_RENAME; textMode = "suffix"; value = after(s, "suffix");
            if (value.isBlank()) return Nl.err("Suffix with what? e.g. \"suffix _v2 on all\".");
        } else if (has(s, "category", "move")) {
            op = BulkOpSpec.OP_CATEGORY; value = after(s, "to");
            if (value.isBlank()) value = after(s, "category");
            if (value.isBlank()) return Nl.err("Which category? e.g. \"move all to red\".");
        } else {
            return Nl.err("Couldn't parse. Try: delete · glow N · lock · favorite · move to <cat> · duplicate · export <fmt> · prefix <x>.");
        }

        // ── Filter (single condition) ──
        String fk = "all", fv = "";
        boolean found = false;
        if (has(s, "unlocked", "not locked")) { fk = "locked"; fv = "no"; found = true; }
        else if (s.contains("locked")) { fk = "locked"; fv = "yes"; found = true; }
        else if (has(s, "favorited", "favourited", "starred", "favorites", "favourites")) { fk = "favorite"; fv = "yes"; found = true; }
        else {
            String cat = category(s, op == BulkOpSpec.OP_CATEGORY ? value : null);
            String nm = after(s, "named"), id = after(s, "prefixed");
            if (nm.isBlank()) nm = after(s, "called");
            if (cat != null) { fk = "category"; fv = cat; found = true; }
            else if (!nm.isBlank()) { fk = "name"; fv = nm; found = true; }
            else if (!id.isBlank()) { fk = "id"; fv = id; found = true; }
            else if (has(s, "all", "every", "everything", "each")) { found = true; } // explicit "all"
        }

        // Safety: a destructive delete must name its target — never default to "all" silently.
        if (op == BulkOpSpec.OP_DELETE && !found)
            return Nl.err("For delete, say which blocks — e.g. \"delete all\", \"delete locked\", \"delete category red\".");

        String desc = descOf(op, property, value, fk, fv);
        return new Nl(op, property, textMode, value, fk, fv, false, desc, null);
    }

    // ── helpers ────────────────────────────────────────────────────────────────
    private static boolean has(String s, String... needles) {
        for (String n : needles) if (s.contains(n)) return true;
        return false;
    }

    private static Integer firstInt(String s) {
        Matcher m = INT.matcher(s);
        return m.find() ? Integer.parseInt(m.group()) : null;
    }

    /** First option from {@code opts} that appears as a word in {@code s}, or {@code def}. */
    private static String firstOf(String s, String[] opts, String def) {
        for (String o : opts) if (s.matches(".*\\b" + Pattern.quote(o.toLowerCase(Locale.ROOT)) + "\\b.*")) return o;
        return def;
    }

    /** The single token right after keyword {@code kw} ("" if none). Strips a trailing period/comma. */
    private static String after(String s, String kw) {
        int i = s.indexOf(kw);
        if (i < 0) return "";
        String rest = s.substring(i + kw.length()).trim();
        if (rest.isEmpty()) return "";
        String tok = rest.split("\\s+")[0];
        return tok.replaceAll("[.,;]$", "");
    }

    /** A category filter: "category X", "in the X category", or "X blocks" — but not the move target {@code exclude}. */
    private static String category(String s, String exclude) {
        String c = after(s, "category");
        if (c.isBlank()) {
            Matcher m = Pattern.compile("(\\w+)\\s+blocks?\\b").matcher(s);
            if (m.find()) c = m.group(1);
        }
        if (c.isBlank() || c.equals("all") || c.equals("every") || (exclude != null && c.equalsIgnoreCase(exclude))) return null;
        return c;
    }

    private static String descOf(int op, String property, String value, String fk, String fv) {
        String opPart = switch (op) {
            case BulkOpSpec.OP_PROPERTY -> "Edit " + property + "=" + value;
            case BulkOpSpec.OP_CATEGORY -> "Move → category " + value;
            case BulkOpSpec.OP_RENAME   -> "Rename";
            case BulkOpSpec.OP_LOCK     -> value.equals("unlock") ? "Unlock" : "Lock";
            case BulkOpSpec.OP_FAVORITE -> value.equals("unfavorite") ? "Unfavorite" : "Favorite";
            case BulkOpSpec.OP_EXPORT   -> "Export " + value;
            case BulkOpSpec.OP_DUPLICATE -> "Duplicate";
            case BulkOpSpec.OP_DELETE   -> "Delete";
            default -> "Apply";
        };
        String filterPart = fk.equals("all") ? "all" : fk + ":" + fv;
        return opPart + " · " + filterPart;
    }
}
