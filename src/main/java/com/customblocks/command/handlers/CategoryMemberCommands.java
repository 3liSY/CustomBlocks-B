/**
 * CategoryMemberCommands.java — the multi-membership half of /cb category (Group 11 §B).
 *
 * Split out of CategoryCommands so both stay under the 400-line handler gate (§9.3); it hangs its
 * sub-verbs onto the SAME `category` literal node, so `/cb category …` is still one command tree.
 *
 *   create <name>                — an empty category (rejects a key collision)
 *   set    <blockId> <category>  — ADD one membership
 *   remove <blockId> <category>  — remove one membership (last one → Uncategorized)
 *   delete <name>                — ask in chat, then drop the category (undoable)
 *   keep / wipe <token>          — the two answers to that question (button-only)
 *   filter [mode] <name>         — the blocks inside a category, ordered
 *   info   [list] <name>         — summary, optionally naming every block
 *
 * A category name may contain spaces ("Arabic Letters") and is NEVER quoted (G11 2026-07-26), so
 * every name argument is a greedy string in LAST position and any mode word comes before it. The
 * old shape — a quotable name followed by a trailing mode word — is what made every multi-word
 * category unreachable (TG11 A2/A10).
 *
 * Depends on: CategoryService, CategoryDeletePrompt, CategorySuggest, BulkConfirm, Chat
 * Called by:  CategoryCommands.register
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.CategoryReport;
import com.customblocks.core.CategoryService;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

public final class CategoryMemberCommands {

    private CategoryMemberCommands() {} // static-only

    private static String str(CommandContext<ServerCommandSource> c, String n) {
        return StringArgumentType.getString(c, n);
    }

    /** Hang the membership sub-verbs onto the shared `category` node. */
    public static void register(LiteralArgumentBuilder<ServerCommandSource> cat) {
        cat.then(CommandManager.literal("create")
                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> CategoryCommands.report(ctx.getSource(),
                                CategoryService.create(str(ctx, "name"))))))

           .then(CommandManager.literal("set")
                .then(CommandManager.argument("block", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .then(CommandManager.argument("cat", StringArgumentType.greedyString())
                                .suggests(CategorySuggest::categories)
                                .executes(ctx -> CategoryCommands.report(ctx.getSource(),
                                        CategoryService.addMembership(BulkConfirm.actor(ctx.getSource()),
                                                str(ctx, "block"), str(ctx, "cat")))))))

           .then(CommandManager.literal("remove")
                .then(CommandManager.argument("block", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .then(CommandManager.argument("cat", StringArgumentType.greedyString())
                                .suggests(CategorySuggest::categories)
                                .executes(ctx -> CategoryCommands.report(ctx.getSource(),
                                        CategoryService.removeMembership(BulkConfirm.actor(ctx.getSource()),
                                                str(ctx, "block"), str(ctx, "cat")))))))

           // `delete <name>` has ONE form and asks in chat (G11 2026-07-26). With nothing exclusive
           // to the category it just drops it and keeps every block; otherwise it posts the two-button
           // question. The old trailing `category` / `exclusive` / `move <target>` mode words are gone
           // for good — a trailing word is exactly what an unquoted greedy name cannot survive.
           .then(CommandManager.literal("delete")
                .then(catArg("cat")
                        .executes(ctx -> CategoryDeletePrompt.delete(ctx.getSource(), str(ctx, "cat")))))

           // The two answers to that question. Both take the prompt's single-use token, never a
           // category name, so neither is a destructive command anyone can type at a category.
           .then(CommandManager.literal("keep")
                .then(CommandManager.argument("token", StringArgumentType.word())
                        .executes(ctx -> CategoryDeletePrompt.keep(ctx.getSource(), str(ctx, "token")))))

           .then(CommandManager.literal("wipe")
                .then(CommandManager.argument("token", StringArgumentType.word())
                        .executes(ctx -> CategoryDeletePrompt.wipe(ctx.getSource(), str(ctx, "token")))))

           .then(modeFirst("filter", CategoryService.blockModes(),
                   (src, mode, name) -> filterBlocks(src, name, mode)))

           .then(modeFirst("info", List.of("list"),
                   (src, mode, name) -> info(src, name, "list".equals(mode))));
    }

    /** A greedy, tab-completing category-name argument — always the LAST argument of its branch. */
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<ServerCommandSource, String> catArg(String name) {
        return CommandManager.argument(name, StringArgumentType.greedyString())
                .suggests(CategorySuggest::categories);
    }

    /** What a verb does once its mode word ("" = none) and category name are known. */
    private interface Verb { int run(ServerCommandSource src, String mode, String category); }

    /**
     * Build {@code <verb> [mode] <category…>} — the shape every G11 category verb takes since
     * 2026-07-26: the mode word LEADS, the name is greedy and LAST, so a multi-word name needs no
     * quotes to stay distinguishable from a mode.
     *
     * ONE greedy argument, with the leading mode word split off here (TG11 A6, 2026-07-27). The
     * modes used to be real Brigadier literals sitting beside the name argument, and Brigadier offers
     * every branch at a shared position: the suggestion list came back as one alphabetical soup of
     * mode words AND category names, so `alphabetically` sat between `Arabic Numbers` and `food`.
     * Parsing the word ourselves is what lets {@link CategorySuggest#modeOrCategory} show one
     * kind at a time. Both spellings still run: {@code filter newest Arabic Numbers} and the bare
     * {@code filter Arabic Numbers}.
     */
    private static LiteralArgumentBuilder<ServerCommandSource> modeFirst(String verb, List<String> modes, Verb body) {
        return CommandManager.literal(verb)
                .then(CommandManager.argument("rest", StringArgumentType.greedyString())
                        .suggests((c, b) -> CategorySuggest.modeOrCategory(b, modes))
                        .executes(ctx -> {
                            String rest = str(ctx, "rest").trim();
                            String mode = leadingMode(rest, modes);
                            if (!mode.isEmpty()) rest = rest.substring(mode.length()).trim();
                            if (rest.isEmpty()) {
                                Chat.error(ctx.getSource(), "Name a category — /cb category " + verb
                                        + (mode.isEmpty() ? "" : " " + mode) + " <category>.");
                                return 0;
                            }
                            return body.run(ctx.getSource(), mode, rest);
                        }));
    }

    /** The mode word {@code rest} starts with (whole word only), or "" when it starts with a name. */
    private static String leadingMode(String rest, List<String> modes) {
        int sp = rest.indexOf(' ');
        String first = sp < 0 ? rest : rest.substring(0, sp);
        for (String m : modes) if (m.equalsIgnoreCase(first)) return first;
        return "";
    }

    // ── info ────────────────────────────────────────────────────────────────

    private static int info(ServerCommandSource src, String category, boolean listBlocks) {
        // Through CategoryChat so a category named in a listing is the same clickable, hoverable,
        // correctly-coloured chip it is in every other [CB] line (G11 D3/D4) — a listing was the one
        // place a name stayed dead text.
        for (String line : CategoryReport.info(category, listBlocks)) Chat.raw(src, CategoryChat.decorate(line));
        return 1;
    }

    // ── filter (blocks inside one category) ─────────────────────────────────

    private static int filterBlocks(ServerCommandSource src, String category, String mode) {
        List<String> lines = CategoryReport.filterBlocks(category, mode);
        if (lines == null) {
            Chat.error(src, "Unknown order \"" + mode + "\". Inside a category you can use: "
                    + String.join(", ", CategoryService.blockModes()) + ".");
            return 0;
        }
        for (String line : lines) Chat.raw(src, CategoryChat.decorate(line));
        return 1;
    }

}
