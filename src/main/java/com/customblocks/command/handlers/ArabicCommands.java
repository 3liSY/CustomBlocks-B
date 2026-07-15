/**
 * ArabicCommands.java
 *
 * Responsibility: /cb arabic subcommands — letter (give the real letter slot block), word (opens
 * the all-in-GUI maker), list (marked browser GUI). The old `text` subcommand was removed (Area
 * 2b, lives in `word`); `import` was removed at G13-25 CP5 (the static art blocks are retired —
 * real Arabic slot blocks are pre-baked at boot instead). Stays under 400 lines (§9.3).
 * The maker flow itself lives in ArabicMaker; this file only wires the commands.
 *
 * Depends on: ArabicArt (catalog), ArabicGlyphs, ArabicMaker, ArabicSlotBootstrap (slot ids),
 *             SlotManager, GuiRouter/Nav, Chat
 * Called by: CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.arabic.ArabicArt;
import com.customblocks.arabic.ArabicGlyphs;
import com.customblocks.arabic.ArabicJoining;
import com.customblocks.arabic.ArabicMaker;
import com.customblocks.arabic.ArabicSlotBootstrap;
import com.customblocks.block.SlotBlock;
import com.customblocks.command.Chat;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Locale;

public final class ArabicCommands {

    private ArabicCommands() {}

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // Bare /cb arabic opens the Arabic Studio hub (Pass 5).
        var arabic = CommandManager.literal("arabic").executes(ArabicCommands::openHub);

        // (`import` was removed at G13-25 CP5 — static art blocks are retired; the real Arabic
        //  slot blocks are guaranteed at boot by ArabicSlotBootstrap instead.)

        // /cb arabic letter <name> [color] [count] — give the REAL auto-joining letter slot block
        // (default black, count 1). Place these right-to-left and they auto-shape (isolated/
        // initial/medial/final) from their neighbours by swapping between the pre-baked form slots.
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

    /**
     * /cb arabic letter <name> [color] [count] — give the REAL isolated letter slot block (G13-25:
     * one pre-baked slot per letter x form x colour; black is the base id, colours are "_<colour>"
     * variants). Placed right-to-left they auto-shape by swapping between the sibling form slots.
     */
    private static int giveLetter(CommandContext<ServerCommandSource> ctx, String name, String color, int count)
            throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String col = color.toLowerCase(Locale.ROOT);
        if (!ArabicArt.isColor(col)) {
            Chat.error(src, "Unknown color \"" + CbFmt.VALUE + color + CbFmt.RESET + "\". Use: black, red, green, yellow.");
            return 0;
        }
        String base = name.toLowerCase(Locale.ROOT);
        if (ArabicGlyphs.charForName(base).isEmpty()) {
            Chat.error(src, "Unknown letter \"" + CbFmt.VALUE + name + CbFmt.RESET + "\". Valid: " + ArabicArt.letterNames());
            return 0;
        }
        String id = ArabicSlotBootstrap.slotId(base, ArabicJoining.ISOLATED)
                + ("black".equals(col) ? "" : "_" + col);
        SlotData d = SlotManager.getById(id);
        SlotBlock.SlotItem item = (d == null) ? null : SlotManager.itemAt(d.index());
        if (item == null) {
            Chat.error(src, "Block \"" + CbFmt.VALUE + id + CbFmt.RESET + "\" is missing — it is created at boot; check the log.");
            return 0;
        }
        ServerPlayerEntity player = src.getPlayerOrThrow();
        player.getInventory().insertStack(new ItemStack(item, count));
        Chat.success(src, "Gave " + CbFmt.VALUE + count + CbFmt.RESET + " " + CbFmt.VALUE + base + CbFmt.RESET + " ("
                + col + "). Place right-to-left — they auto-join.");
        return 1;
    }

    /** /cb arabic word <id> <name> — validate, then hand off to the GUI maker (Area 2b/2c). */
    private static int openWord(CommandContext<ServerCommandSource> ctx, String id, String name) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) { Chat.error(src, "Run this in-game — it opens an anvil to type the text."); return 0; }
        if (SlotManager.hasId(id)) { Chat.error(src, "Id \"" + CbFmt.VALUE + id + CbFmt.RESET + "\" is taken. Pick another."); return 0; }
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

    /** Console fallback: count the REAL Arabic slot blocks (SlotData carries ArabicMeta). */
    private static int listArt(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        long present = 0;
        for (SlotData d : SlotManager.assignedSlots()) if (d.isArabic()) present++;
        final long fp = present;
        Chat.raw(src, CbFmt.VALUE + "Arabic blocks: " + CbFmt.BODY + fp
                + " present " + CbFmt.DIM + "(letters x4 forms + numbers, per colour)");
        return 1;
    }
}
