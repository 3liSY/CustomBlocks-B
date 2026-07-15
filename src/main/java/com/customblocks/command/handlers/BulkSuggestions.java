/**
 * BulkSuggestions.java
 *
 * Responsibility: Tab-completion for the bulk commands' greedy-string args (Group 07).
 * Greedy strings swallow the whole tail, so Brigadier can't suggest per-token on its
 * own — these providers re-tokenize what's typed, offset the suggestion builder to the
 * current token, and suggest filters / properties / values that match it.
 *
 * Token layouts covered:
 *   FILTER_ONLY   — <ids…>                        (bulkdelete, bulklock, bulkfavorite, …)
 *   PROPERTY_ARGS — <ids…> <property> <value>     (bulkproperty)
 *   RENAME_ARGS   — <ids…> prefix|suffix|replace  (bulkrename; text after the mode is free)
 *
 * Scope suggestions mirror exactly what BulkScope.resolve accepts (all / id / space-separated id
 * list / comma id list / quoted id list — completion continues after the last comma).
 *
 * Depends on: SlotManager, SlotBlock (sound types), BulkScope syntax
 * Called by:  BulkCommands, BulkFlagCommands
 */
package com.customblocks.command.handlers;

import com.customblocks.block.BlockShapes;
import com.customblocks.block.SlotBlock;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Locale;

public final class BulkSuggestions {

    private BulkSuggestions() {} // static-only

    private static final String[] PROPERTIES = {"glow", "hardness", "sound", "collision"};

    // Group 04 §A7 — the scope is an id LIST now, so it can span any number of tokens. Every provider below
    // therefore keeps offering block ids at EVERY token, not just the first, and offers the next argument
    // (property / shape / category / mode / format) alongside them: at token 2 of `bulkshape a b slab`, both
    // "another id" and "the shape" are legal, so both are suggested. Only "all" is first-token-only — it is
    // the whole scope by itself and means nothing in the middle of a list.

    /** &lt;ids…&gt; — the whole arg is one scope expression. */
    public static final SuggestionProvider<ServerCommandSource> FILTER_ONLY = (ctx, builder) -> {
        Cursor cur = Cursor.of(builder);
        suggestScope(cur);
        return cur.b.buildFuture();
    };

    /** &lt;ids…&gt; &lt;property&gt; &lt;value&gt; — property/value parsed from the END, so after the
     *  first id we offer property names beside the remaining ids, and after a property its values. */
    public static final SuggestionProvider<ServerCommandSource> PROPERTY_ARGS = (ctx, builder) -> {
        Cursor cur = Cursor.of(builder);
        if (isProperty(cur.previous)) {
            suggestValues(cur);
        } else {
            suggestScope(cur);
            if (cur.tokenIndex > 0) suggestMatching(cur, PROPERTIES);
        }
        return cur.b.buildFuture();
    };

    /** &lt;ids…&gt; prefix|suffix|replace &lt;text…&gt; — free text after the mode ends the id list. */
    public static final SuggestionProvider<ServerCommandSource> RENAME_ARGS = (ctx, builder) -> {
        Cursor cur = Cursor.of(builder);
        if (!cur.sawMode()) { // past the mode keyword the rest is free text — nothing to suggest
            suggestScope(cur);
            if (cur.tokenIndex > 0) suggestMatching(cur, "prefix", "suffix", "replace");
        }
        return cur.b.buildFuture();
    };

    /** &lt;ids…&gt; &lt;category&gt; — beside the remaining ids, offer existing categories + "none". */
    public static final SuggestionProvider<ServerCommandSource> CATEGORY_ARGS = (ctx, builder) -> {
        Cursor cur = Cursor.of(builder);
        suggestScope(cur);
        if (cur.tokenIndex > 0) {
            cur.add("none");
            for (String c : SlotManager.categories()) cur.add(c);
        }
        return cur.b.buildFuture();
    };

    /** &lt;ids…&gt; &lt;shape&gt; — beside the remaining ids, offer the shape names (bulkshape, G08 §E). */
    public static final SuggestionProvider<ServerCommandSource> SHAPE_ARGS = (ctx, builder) -> {
        Cursor cur = Cursor.of(builder);
        suggestScope(cur);
        if (cur.tokenIndex > 0) suggestMatching(cur, BlockShapes.names());
        return cur.b.buildFuture();
    };

    /** &lt;setting&gt; &lt;value…&gt; — /cb setall: setting names first, then values for that setting (G07). */
    public static final SuggestionProvider<ServerCommandSource> SETALL_ARGS = (ctx, builder) -> {
        Cursor cur = Cursor.of(builder);
        if (cur.tokenIndex == 0) {
            suggestMatching(cur, "glow", "hardness", "sound", "collision", "category", "shape");
        } else if (cur.tokenIndex == 1) {
            switch (cur.previous) {
                case "glow", "light", "hardness", "sound", "collision" -> suggestValues(cur);
                case "shape" -> suggestMatching(cur, BlockShapes.names());
                case "category" -> { cur.add("none"); for (String c : SlotManager.categories()) cur.add(c); }
                default -> { /* unknown setting — nothing to offer */ }
            }
        }
        return cur.b.buildFuture();
    };

    /** &lt;filter&gt; [json|txt|csv|md|html|yaml|png] — after the filter, offer the export formats. */
    public static final SuggestionProvider<ServerCommandSource> EXPORT_ARGS = (ctx, builder) -> {
        Cursor cur = Cursor.of(builder);
        suggestScope(cur);
        if (cur.tokenIndex > 0) suggestMatching(cur, "json", "txt", "csv", "md", "html", "yaml", "png");
        return cur.b.buildFuture();
    };

    // ── shared pieces ──────────────────────────────────────────────────────────

    /** Everything BulkScope.resolve understands, concretized from live data. */
    private static void suggestScope(Cursor cur) {
        // Inside a comma list ("a,b,") — keep completing block ids after the last comma.
        int comma = cur.current.lastIndexOf(',');
        if (comma >= 0) {
            Cursor after = cur.offsetInto(comma + 1);
            for (SlotData d : SlotManager.assignedSlots()) after.add(d.customId());
            return;
        }
        if (cur.tokenIndex == 0) cur.add("all"); // whole-scope word — meaningless mid-list
        for (SlotData d : SlotManager.assignedSlots()) cur.add(d.customId());
    }

    /** Values for the property in the previous token — mirrors BulkValues.parse. */
    private static void suggestValues(Cursor cur) {
        switch (cur.previous) {
            case "glow", "light" -> suggestMatching(cur, "0", "5", "10", "15");
            case "hardness"      -> suggestMatching(cur, "unbreakable", "instant", "stone", "5", "20", "50");
            case "sound"         -> suggestMatching(cur, SlotBlock.SOUND_TYPES);
            case "collision"     -> suggestMatching(cur, "solid", "passable");
            default -> { /* unknown property — nothing sensible to offer */ }
        }
    }

    private static boolean isProperty(String token) {
        return switch (token) {
            case "glow", "light", "hardness", "sound", "collision" -> true;
            default -> false;
        };
    }

    private static void suggestMatching(Cursor cur, String... options) {
        for (String o : options) cur.add(o);
    }

    /**
     * The current token under the cursor inside a greedy string: which token it is,
     * the previous token (for context), and a SuggestionsBuilder offset to the token's
     * start so accepted suggestions replace only that token.
     */
    private static final class Cursor {
        final SuggestionsBuilder b;
        final int tokenIndex;
        final String current;   // lowercase, what's typed of the token so far
        final String previous;  // lowercase previous token ("" for the first)
        final String[] done;    // lowercase COMPLETE tokens before the cursor

        private Cursor(SuggestionsBuilder b, int tokenIndex, String current, String previous, String[] done) {
            this.b = b;
            this.tokenIndex = tokenIndex;
            this.current = current;
            this.previous = previous;
            this.done = done;
        }

        static Cursor of(SuggestionsBuilder builder) {
            String remaining = builder.getRemaining();
            // Token starts after the last whitespace; everything before is complete tokens.
            int tokenStart = lastWhitespace(remaining) + 1;
            String head = remaining.substring(0, tokenStart);
            String[] done = head.trim().isEmpty()
                    ? new String[0] : head.trim().toLowerCase(Locale.ROOT).split("\\s+");
            String prev = done.length == 0 ? "" : done[done.length - 1];
            SuggestionsBuilder offset = tokenStart == 0
                    ? builder : builder.createOffset(builder.getStart() + tokenStart);
            String current = remaining.substring(tokenStart).toLowerCase(Locale.ROOT);
            return new Cursor(offset, done.length, current, prev, done);
        }

        /**
         * True once a prefix/suffix/replace keyword has been typed — the id list has ended and the rest of
         * the line is free text (mirrors BulkCommands.modeIndex, which parses the same grammar server-side).
         */
        boolean sawMode() {
            for (String t : done) {
                if (t.equals("prefix") || t.equals("suffix") || t.equals("replace")) return true;
            }
            return false;
        }

        /** A nested cursor inside the current token (used to complete after a comma). */
        Cursor offsetInto(int at) {
            return new Cursor(b.createOffset(b.getStart() + at), tokenIndex,
                    current.substring(at), previous, done);
        }

        /** Suggest only when it prefix-matches what's typed (mirrors BlockSuggestions). */
        void add(String suggestion) {
            if (suggestion.toLowerCase(Locale.ROOT).startsWith(current)) b.suggest(suggestion);
        }

        private static int lastWhitespace(String s) {
            for (int i = s.length() - 1; i >= 0; i--) {
                if (Character.isWhitespace(s.charAt(i))) return i;
            }
            return -1;
        }
    }
}
