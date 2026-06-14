/**
 * BulkSuggestions.java
 *
 * Responsibility: Tab-completion for the bulk commands' greedy-string args (Group 07).
 * Greedy strings swallow the whole tail, so Brigadier can't suggest per-token on its
 * own — these providers re-tokenize what's typed, offset the suggestion builder to the
 * current token, and suggest filters / properties / values that match it.
 *
 * Token layouts covered:
 *   FILTER_ONLY   — <filter>                       (bulkdelete, bulklock, bulkfavorite, …)
 *   PROPERTY_ARGS — <filter> <property> <value>    (bulkproperty)
 *   RENAME_ARGS   — <filter> prefix|suffix|replace (bulkrename; text after the mode is free)
 *
 * Filter suggestions mirror exactly what BulkScope.resolve accepts, including comma id
 * lists (completion continues after the last comma).
 *
 * Depends on: SlotManager, SlotBlock (sound types), BulkScope syntax
 * Called by:  BulkCommands, BulkFlagCommands
 */
package com.customblocks.command.handlers;

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

    /** &lt;filter&gt; — the whole arg is one filter expression. */
    public static final SuggestionProvider<ServerCommandSource> FILTER_ONLY = (ctx, builder) -> {
        Cursor cur = Cursor.of(builder);
        if (cur.tokenIndex == 0) suggestFilter(cur);
        return cur.b.buildFuture();
    };

    /** &lt;filter&gt; &lt;property&gt; &lt;value&gt; — property/value parsed from the END, so after the
     *  filter we offer property names, and after a recognized property its values. */
    public static final SuggestionProvider<ServerCommandSource> PROPERTY_ARGS = (ctx, builder) -> {
        Cursor cur = Cursor.of(builder);
        if (cur.tokenIndex == 0) {
            suggestFilter(cur);
        } else if (isProperty(cur.previous)) {
            suggestValues(cur);
        } else {
            suggestMatching(cur, PROPERTIES);
        }
        return cur.b.buildFuture();
    };

    /** &lt;filter&gt; prefix|suffix|replace &lt;text…&gt; — free text after the mode. */
    public static final SuggestionProvider<ServerCommandSource> RENAME_ARGS = (ctx, builder) -> {
        Cursor cur = Cursor.of(builder);
        if (cur.tokenIndex == 0) suggestFilter(cur);
        else if (cur.tokenIndex == 1) suggestMatching(cur, "prefix", "suffix", "replace");
        return cur.b.buildFuture();
    };

    /** &lt;filter&gt; &lt;category&gt; — after the filter, offer existing categories + "none". */
    public static final SuggestionProvider<ServerCommandSource> CATEGORY_ARGS = (ctx, builder) -> {
        Cursor cur = Cursor.of(builder);
        if (cur.tokenIndex == 0) {
            suggestFilter(cur);
        } else if (cur.tokenIndex == 1) {
            cur.add("none");
            for (String c : SlotManager.categories()) cur.add(c);
        }
        return cur.b.buildFuture();
    };

    /** &lt;filter&gt; [json|txt|csv|md|html|yaml|png] — after the filter, offer the export formats. */
    public static final SuggestionProvider<ServerCommandSource> EXPORT_ARGS = (ctx, builder) -> {
        Cursor cur = Cursor.of(builder);
        if (cur.tokenIndex == 0) suggestFilter(cur);
        else if (cur.tokenIndex == 1) suggestMatching(cur, "json", "txt", "csv", "md", "html", "yaml", "png");
        return cur.b.buildFuture();
    };

    // ── shared pieces ──────────────────────────────────────────────────────────

    /** Everything BulkScope.resolve understands, concretized from live data. */
    private static void suggestFilter(Cursor cur) {
        // Inside a comma list ("a,b,") — keep completing block ids after the last comma.
        int comma = cur.current.lastIndexOf(',');
        if (comma >= 0) {
            Cursor after = cur.offsetInto(comma + 1);
            for (SlotData d : SlotManager.assignedSlots()) after.add(d.customId());
            return;
        }
        cur.add("all");
        cur.add("locked:yes"); cur.add("locked:no");
        cur.add("favorite:yes"); cur.add("favorite:no");
        cur.add("name:"); cur.add("id:");
        for (String c : SlotManager.categories()) cur.add("category:" + c);
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

        private Cursor(SuggestionsBuilder b, int tokenIndex, String current, String previous) {
            this.b = b;
            this.tokenIndex = tokenIndex;
            this.current = current;
            this.previous = previous;
        }

        static Cursor of(SuggestionsBuilder builder) {
            String remaining = builder.getRemaining();
            // Token starts after the last whitespace; everything before is complete tokens.
            int tokenStart = lastWhitespace(remaining) + 1;
            String head = remaining.substring(0, tokenStart);
            String[] done = head.trim().isEmpty() ? new String[0] : head.trim().split("\\s+");
            String prev = done.length == 0 ? "" : done[done.length - 1].toLowerCase(Locale.ROOT);
            SuggestionsBuilder offset = tokenStart == 0
                    ? builder : builder.createOffset(builder.getStart() + tokenStart);
            String current = remaining.substring(tokenStart).toLowerCase(Locale.ROOT);
            return new Cursor(offset, done.length, current, prev);
        }

        /** A nested cursor inside the current token (used to complete after a comma). */
        Cursor offsetInto(int at) {
            return new Cursor(b.createOffset(b.getStart() + at), tokenIndex,
                    current.substring(at), previous);
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
