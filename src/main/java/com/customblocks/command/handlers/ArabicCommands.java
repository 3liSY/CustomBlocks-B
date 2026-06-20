/**
 * ArabicCommands.java
 *
 * Responsibility: /cb arabic subcommands — import (224 bundled art blocks), letter (give),
 * word (opens the all-in-GUI maker), list (marked browser GUI). The old `text` subcommand was
 * removed entirely (Area 2b) — its function now lives in `word`. Stays under 400 lines (§9.3).
 * The maker flow itself lives in ArabicMaker; this file only wires the commands.
 *
 * Depends on: ArabicArt, ArabicBlockRegistry, ArabicMaker, SlotManager, GuiRouter/Nav, Chat
 * Called by: CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.arabic.ArabicArt;
import com.customblocks.arabic.ArabicBlockRegistry;
import com.customblocks.arabic.ArabicGlyphs;
import com.customblocks.arabic.ArabicMaker;
import com.customblocks.block.ArabicLetterBlock;
import com.customblocks.command.Chat;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.Optional;

public final class ArabicCommands {

    private ArabicCommands() {}

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // Bare /cb arabic opens the Arabic Studio hub (Pass 5).
        var arabic = CommandManager.literal("arabic").executes(ArabicCommands::openHub);

        // /cb arabic import — create the 224 bundled art blocks (idempotent), rebuild pack once
        arabic.then(CommandManager.literal("import")
                .executes(ArabicCommands::importArt));

        // /cb arabic letter <name> [color] [count] — give the auto-joining letter block (default
        // black, count 1). Place these right-to-left and they auto-shape (isolated/initial/medial/
        // final) from their neighbours. The old static-bundled letters were retired, so this single
        // name now drives the one auto-join system (the separate `join` subcommand was a dupe, removed).
        arabic.then(CommandManager.literal("letter")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests((c, b) -> {
                            String typed = b.getRemaining().toLowerCase(Locale.ROOT);
                            for (ArabicArt.Glyph g : ArabicArt.ALL) {
                                if (g.group() == ArabicArt.Group.LETTER && g.idBase().startsWith(typed))
                                    b.suggest(g.idBase());
                            }
                            return b.buildFuture();
                        })
                        .executes(ctx -> giveLetter(ctx, StringArgumentType.getString(ctx, "name"), "black", 1))
                        .then(CommandManager.argument("color", StringArgumentType.word())
                                .suggests((c, b) -> {
                                    String typed = b.getRemaining().toLowerCase(Locale.ROOT);
                                    for (String col : ArabicArt.COLORS) if (col.startsWith(typed)) b.suggest(col);
                                    return b.buildFuture();
                                })
                                .executes(ctx -> giveLetter(ctx,
                                        StringArgumentType.getString(ctx, "name"),
                                        StringArgumentType.getString(ctx, "color"), 1))
                                .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> giveLetter(ctx,
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "color"),
                                                IntegerArgumentType.getInteger(ctx, "count")))))));

        // /cb arabic word <id> <name> — opens the all-in-GUI maker: anvil for the text (Brigadier
        // can't take non-ASCII), then the Single-block/Color-Studio or Place-letters flow. (Area 2b/2c)
        arabic.then(CommandManager.literal("word")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> openWord(ctx,
                                        StringArgumentType.getString(ctx, "id"),
                                        StringArgumentType.getString(ctx, "name"))))));

        // /cb arabic text was REMOVED entirely (Area 2b) — its function lives in `word`.

        // /cb arabic list — the marked, browsable block list (Area 2a).
        arabic.then(CommandManager.literal("list").executes(ArabicCommands::openList));

        root.then(arabic);
    }

    private static int importArt(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Chat.info(src, "Importing bundled Arabic art blocks (56 glyphs x 4 colors)...");
        ArabicBlockRegistry.ImportResult r = ArabicBlockRegistry.importArt(true);
        if (r.created() > 0) Chat.success(src, "Created §e" + r.created() + "§r block(s).");
        if (r.skipped() > 0) Chat.info(src,    "Skipped §e" + r.skipped() + "§r (already exist).");
        if (r.failed()  > 0) Chat.error(src,   "Failed §c"  + r.failed()  + "§r — check log.");
        if (r.created() == 0 && r.failed() == 0) Chat.info(src, "All Arabic art blocks already present.");
        return 1;
    }

    /**
     * /cb arabic letter <name> [color] [count] — give auto-joining letter blocks. Place them
     * right-to-left and they auto-shape (isolated/initial/medial/final) from their neighbours.
     * (Replaces both the old static-bundled `letter` giver and the duplicate `join` subcommand.)
     */
    private static int giveLetter(CommandContext<ServerCommandSource> ctx, String name, String color, int count)
            throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String col = color.toLowerCase(Locale.ROOT);
        if (!ArabicArt.isColor(col)) {
            Chat.error(src, "Unknown color '§e" + color + "§r'. Use: black, red, green, yellow.");
            return 0;
        }
        Optional<Character> ch = ArabicGlyphs.charForName(name.toLowerCase(Locale.ROOT));
        if (ch.isEmpty()) {
            Chat.error(src, "Unknown letter '§e" + name + "§r'. Valid: " + ArabicArt.letterNames());
            return 0;
        }
        ServerPlayerEntity player = src.getPlayerOrThrow();
        player.getInventory().insertStack(ArabicLetterBlock.stackFor(ch.get(), col, -1, count));
        Chat.success(src, "Gave §e" + count + "§r §e" + name.toLowerCase(Locale.ROOT) + "§r ("
                + col + "). Place right-to-left — they auto-join.");
        return 1;
    }

    /** /cb arabic word <id> <name> — validate, then hand off to the GUI maker (Area 2b/2c). */
    private static int openWord(CommandContext<ServerCommandSource> ctx, String id, String name) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) { Chat.error(src, "Run this in-game — it opens an anvil to type the text."); return 0; }
        if (SlotManager.hasId(id)) { Chat.error(src, "Id '§e" + id + "§r' is taken. Pick another."); return 0; }
        ArabicMaker.startFromCommand(player, id, name);
        return 1;
    }

    /** Open the Arabic Studio hub for a player; console falls back to the text count. (Pass 5) */
    private static int openHub(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return listArt(ctx);
        GuiRouter.openFresh(player, Nav.MenuKey.of(Nav.Dest.ARABIC));
        return 1;
    }

    /** Open the marked, browsable block list; console falls back to the text count. (Area 2a) */
    private static int openList(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return listArt(ctx);
        GuiRouter.openFresh(player, Nav.MenuKey.of(Nav.Dest.ARABIC_LIST));
        return 1;
    }

    private static int listArt(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        long present = 0;
        for (ArabicArt.Glyph g : ArabicArt.ALL)
            for (String c : ArabicArt.COLORS)
                if (SlotManager.getById(ArabicArt.blockId(g, c)) != null) present++;
        long total = (long) ArabicArt.ALL.size() * ArabicArt.COLORS.length;
        final long fp = present;
        src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§eArabic art: §f" + fp + "/" + total
                + " blocks present §7(" + ArabicArt.letterNames().split(",").length
                + " letters + numbers, x4 colors)"), false);
        return 1;
    }
}
