/**
 * CategorySuggest.java — tab-completion for the /cb category tree (Group 11).
 *
 * Split out of CategoryCommands under the 400-line handler gate (§9.3). One rule runs through every
 * suggester here: a position offers ONE kind of thing at a time, and only what still prefixes what
 * is typed.
 *
 * That rule is the fix for TG11 A6. Modes used to be Brigadier literals sitting beside the greedy
 * name argument, and Brigadier merges every branch's suggestions into a single alphabetical list, so
 * `/cb category filter ` answered with `alphabetically, Arabic Numbers, arabic_letters, food,
 * newest, none, oldest, Uncategorized` — orders and categories in one soup, with nothing marking
 * which was which. Now the verb takes one greedy argument (see CategoryMemberCommands.modeFirst) and
 * these methods decide what that position means.
 *
 * Depends on: CategoryMembershipStore, CategoryMetadataStore, CategoryService, CbFmt
 * Called by:  CategoryCommands, CategoryMemberCommands
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.core.CategoryMembershipStore;
import com.customblocks.core.CategoryMetadataStore;
import com.customblocks.core.CategoryService;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class CategorySuggest {

    private CategorySuggest() {} // static-only

    /** The required direction word in `combine <a> into <b>` and `rename <old> into <new>`. */
    static final String CONNECTOR = " into ";

    /**
     * Suggest EXISTING category names, by their typed display name.
     *
     * Reads the metadata records plus the keys actually in use, not SlotManager's block scan: a
     * category with 0 blocks is still real (G11) and has to tab-complete, or an empty category
     * would be untypeable the moment its last block left.
     *
     * The suggestion TEXT is the bare name — never pre-quoted (G11 2026-07-26). Offering
     * {@code "Arabic Numbers"} was half of TG11 A2/A10: accepting it typed the quotes straight into
     * a greedy argument, which then never matched anything. The block count rides along as the
     * suggestion's TOOLTIP instead, so an empty category is still spottable in the list without a
     * single character of it landing in the command line. {@code Uncategorized} is offered too, and
     * its tooltip says it is the system floor (C16).
     */
    static CompletableFuture<Suggestions> categories(CommandContext<ServerCommandSource> ctx,
                                                     SuggestionsBuilder b) {
        String typed = CategoryMembershipStore.unquote(b.getRemaining()).toLowerCase(Locale.ROOT);
        java.util.Set<String> keys = new java.util.TreeSet<>(CategoryMetadataStore.knownCategories());
        keys.addAll(CategoryMembershipStore.keysInUse());
        keys.add(CategoryMembershipStore.UNCATEGORIZED);
        for (String k : keys) {
            String name = CategoryMetadataStore.getDisplayName(k);
            if (!name.toLowerCase(Locale.ROOT).startsWith(typed) && !k.startsWith(typed)) continue;
            int count = CategoryMembershipStore.blocksIn(k).size();
            String tip = count + (count == 1 ? " block" : " blocks");
            if (CategoryMembershipStore.isUncategorized(k)) {
                tip = CbFmt.DIM + CbFmt.ITALIC + "system floor · " + tip;
            }
            b.suggest(name, Text.literal(tip));
        }
        return b.buildFuture();
    }

    /**
     * A `<verb> [mode] <category…>` position: mode words OR category names, never both (TG11 A6).
     *
     * While what is typed can still become a mode word, the position is a mode; the moment it
     * cannot, it is a name. An empty box offers the short mode list, which is also what teaches the
     * shape of the command.
     */
    static CompletableFuture<Suggestions> modeOrCategory(SuggestionsBuilder b, List<String> modes) {
        String typed = b.getRemaining().toLowerCase(Locale.ROOT);
        boolean modePossible = false;
        for (String m : modes) if (m.startsWith(typed)) { modePossible = true; break; }
        if (!modePossible) return categories(null, b);
        for (String m : modes) if (m.startsWith(typed)) b.suggest(m, Text.literal(CbFmt.DIM + "order"));
        return b.buildFuture();
    }

    /**
     * `combine <a> into <b>`: category names before the connector, then the connector itself once a
     * real name is typed, then category names again after it — one kind at a time, as above.
     */
    static CompletableFuture<Suggestions> combine(CommandContext<ServerCommandSource> ctx,
                                                  SuggestionsBuilder b) {
        String remaining = b.getRemaining();
        int at = connectorAt(remaining);
        if (at >= 0) { // past the connector — complete the TARGET name from just after it
            return categories(ctx, b.createOffset(b.getStart() + at + CONNECTOR.length()));
        }
        String typed = CategoryMembershipStore.unquote(remaining).trim();
        if (!typed.isEmpty() && CategoryMetadataStore.keyExists(CategoryMembershipStore.key(typed))) {
            b.suggest(typed + CONNECTOR.stripTrailing(), Text.literal(CbFmt.DIM + "…into which category?"));
            return b.buildFuture();
        }
        return categories(ctx, b);
    }

    /**
     * `rename <old> into <new>`: the same one-kind-at-a-time walk as {@link #combine}, except that
     * NOTHING is suggested past the connector — the new name is a name that does not exist yet, so
     * offering the existing categories there would only invite a collision.
     */
    static CompletableFuture<Suggestions> rename(CommandContext<ServerCommandSource> ctx,
                                                 SuggestionsBuilder b) {
        String remaining = b.getRemaining();
        if (connectorAt(remaining) >= 0) return b.buildFuture(); // past `into` — free text, no list
        String typed = CategoryMembershipStore.unquote(remaining).trim();
        if (!typed.isEmpty() && CategoryMetadataStore.keyExists(CategoryMembershipStore.key(typed))) {
            b.suggest(typed + CONNECTOR.stripTrailing(), Text.literal(CbFmt.DIM + "…into what new name?"));
            return b.buildFuture();
        }
        return categories(ctx, b);
    }

    /** Index of the ` into ` connector in a combine/rename spec, or -1 when it was not typed. */
    static int connectorAt(String spec) {
        return spec.toLowerCase(Locale.ROOT).indexOf(CONNECTOR);
    }

    /**
     * Suggest a fixed word set, prefix-filtered on what is already typed.
     *
     * {@code b.suggest()} adds unconditionally — a suggester that skips the prefix check keeps
     * offering its words after unrelated text, which is exactly what TG11 A1 caught on {@code none}.
     */
    static CompletableFuture<Suggestions> words(SuggestionsBuilder b, List<String> words) {
        String typed = b.getRemaining().toLowerCase(Locale.ROOT);
        for (String w : words) if (w.startsWith(typed)) b.suggest(w);
        return b.buildFuture();
    }

    /** The category colour words a player may pick. */
    static CompletableFuture<Suggestions> colors(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder b) {
        String typed = b.getRemaining().toLowerCase(Locale.ROOT);
        for (String c : CategoryService.colorWords()) if (c.startsWith(typed)) b.suggest(c);
        return b.buildFuture();
    }
}
