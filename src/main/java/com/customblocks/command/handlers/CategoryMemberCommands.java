/**
 * CategoryMemberCommands.java — the multi-membership half of /cb category (Group 11 §B).
 *
 * Split out of CategoryCommands so both stay under the 400-line handler gate (§9.3); it hangs its
 * sub-verbs onto the SAME `category` literal node, so `/cb category …` is still one command tree.
 *
 *   create <name>                       — an empty category (rejects a key collision)
 *   set    <blockId> <category>         — ADD one membership
 *   remove <blockId> <category>         — remove one membership (last one → Uncategorized)
 *   delete <name> [category|exclusive|move <target>]  — the three delete modes
 *   filter <name> [mode]                — the blocks inside a category, ordered
 *   info   <name> [list]                — summary, optionally naming every block
 *
 * A category name may contain spaces ("Arabic Letters"), so any name argument with something
 * AFTER it is a quotable string rather than the greedy string used for trailing names — otherwise
 * the greedy match would swallow the mode word.
 *
 * Depends on: CategoryService, CategoryMembershipStore, CategoryMetadataStore, BulkConfirm, BulkChat, Chat
 * Called by:  CategoryCommands.register
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.CategoryService;
import com.customblocks.core.SlotData;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
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
                                .suggests(CategoryCommands::suggestCategories)
                                .executes(ctx -> CategoryCommands.report(ctx.getSource(),
                                        CategoryService.addMembership(BulkConfirm.actor(ctx.getSource()),
                                                str(ctx, "block"), str(ctx, "cat")))))))

           .then(CommandManager.literal("remove")
                .then(CommandManager.argument("block", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .then(CommandManager.argument("cat", StringArgumentType.greedyString())
                                .suggests(CategoryCommands::suggestCategories)
                                .executes(ctx -> CategoryCommands.report(ctx.getSource(),
                                        CategoryService.removeMembership(BulkConfirm.actor(ctx.getSource()),
                                                str(ctx, "block"), str(ctx, "cat")))))))

           // Bare `delete <name>` keeps today's meaning — strip the category, keep every block
           // (TG11 A3). The two modes that can destroy or move blocks must be asked for by name.
           .then(CommandManager.literal("delete")
                .then(CommandManager.argument("cat", StringArgumentType.string())
                        .suggests(CategoryCommands::suggestCategories)
                        .executes(ctx -> CategoryCommands.report(ctx.getSource(),
                                CategoryService.deleteCategoryOnly(str(ctx, "cat"))))
                        .then(CommandManager.literal("category")
                                .executes(ctx -> CategoryCommands.report(ctx.getSource(),
                                        CategoryService.deleteCategoryOnly(str(ctx, "cat")))))
                        .then(CommandManager.literal("exclusive")
                                .executes(ctx -> deleteExclusive(ctx.getSource(), str(ctx, "cat"))))
                        .then(CommandManager.literal("move")
                                .then(CommandManager.argument("target", StringArgumentType.greedyString())
                                        .suggests(CategoryCommands::suggestCategories)
                                        .executes(ctx -> CategoryCommands.report(ctx.getSource(),
                                                CategoryService.deleteMoveFirst(str(ctx, "cat"), str(ctx, "target"))))))))

           .then(CommandManager.literal("filter")
                .then(CommandManager.argument("cat", StringArgumentType.string())
                        .suggests(CategoryCommands::suggestCategories)
                        .executes(ctx -> filterBlocks(ctx.getSource(), str(ctx, "cat"), ""))
                        .then(CommandManager.argument("mode", StringArgumentType.word())
                                .suggests((c, b) -> {
                                    for (String m : CategoryService.blockModes()) b.suggest(m);
                                    return b.buildFuture();
                                })
                                .executes(ctx -> filterBlocks(ctx.getSource(), str(ctx, "cat"), str(ctx, "mode"))))))

           .then(CommandManager.literal("info")
                .then(CommandManager.argument("cat", StringArgumentType.string())
                        .suggests(CategoryCommands::suggestCategories)
                        .executes(ctx -> info(ctx.getSource(), str(ctx, "cat"), false))
                        .then(CommandManager.literal("list")
                                .executes(ctx -> info(ctx.getSource(), str(ctx, "cat"), true)))));
    }

    // ── info ────────────────────────────────────────────────────────────────

    private static int info(ServerCommandSource src, String category, boolean listBlocks) {
        for (String line : CategoryService.info(category, listBlocks)) Chat.raw(src, Text.literal(line));
        return 1;
    }

    // ── filter (blocks inside one category) ─────────────────────────────────

    private static int filterBlocks(ServerCommandSource src, String category, String mode) {
        List<String> lines = CategoryService.filterBlocks(category, mode);
        if (lines == null) {
            Chat.error(src, "Unknown order \"" + mode + "\". Inside a category you can use: "
                    + String.join(", ", CategoryService.blockModes()) + ".");
            return 0;
        }
        for (String line : lines) Chat.raw(src, Text.literal(line));
        return 1;
    }

    // ── delete, exclusive-blocks mode (the one path that destroys blocks) ───

    /**
     * Mode 2 always goes behind /cb confirm, however few blocks it would take — unlike a bulk op
     * there is no threshold here, because the player named a CATEGORY and the blocks that die are
     * whatever happened to have no second membership. That set isn't visible from the command, so
     * it gets shown and held every time (G11 Locked Decisions: only behind /cb confirm, as one
     * undoable batch).
     */
    private static int deleteExclusive(ServerCommandSource src, String category) {
        List<SlotData> doomed = CategoryService.exclusiveBlocksIn(category);
        if (doomed.isEmpty()) {
            Chat.info(src, "No block is ONLY in that category — nothing would be deleted. "
                    + "Use /cb category delete " + category + " to just drop the category.");
            return 0;
        }
        List<String> ids = new ArrayList<>();
        for (SlotData d : doomed) ids.add(d.customId());

        BulkConfirm.request(src,
                () -> CategoryCommands.report(src, CategoryService.deleteExclusiveBlocks(
                        src.getServer(), BulkConfirm.actor(src), category)),
                "delete " + ids.size() + " block(s) exclusive to \"" + category + "\"");

        String hoverList = CbFmt.BAD + "Will DELETE " + ids.size() + " block(s):\n" + CbFmt.BODY
                + BulkChat.columns(ids);
        BulkChat.confirm(src, CbFmt.BAD + CbFmt.BOLD + "Delete ", CbFmt.BAD + " only in \"" + category + "\"? ",
                ids.size(), hoverList,
                CbFmt.DANGER + CbFmt.BOLD + "[✔ DELETE]", CbFmt.OK + CbFmt.BOLD + "[✖ Keep]");
        return 1;
    }
}
