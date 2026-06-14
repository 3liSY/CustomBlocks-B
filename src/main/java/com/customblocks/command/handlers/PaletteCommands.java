/**
 * PaletteCommands.java — Group 10 (per-player colour palettes).
 *
 *   /cb palette                  — open the Palette GUI.
 *   /cb palette add <colour>     — add a colour (name or hex) to your working set.
 *   /cb palette clear            — empty your working set.
 *   /cb palette save <name>      — save the working set as a named palette.
 *   /cb palette load <name>      — load a saved palette into the working set.
 *   /cb palette list             — list your saved palettes + current working set.
 *   /cb palette delete <name>    — delete a saved palette.
 *
 * State lives in PlayerPaletteManager (persisted). Colours resolve through ColorLibrary (name or
 * hex), so "red" and "#FF0000" both work. Stays under the 400-line handler gate (§9.3).
 *
 * Depends on: PlayerPaletteManager, ColorLibrary, GuiRouter/Nav, Chat.
 * Called by:  CommandRegistrar.
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.ColorLibrary;
import com.customblocks.core.PlayerPaletteManager;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public final class PaletteCommands {

    private PaletteCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("palette")
                .executes(PaletteCommands::open)
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("colour", StringArgumentType.greedyString())
                                .executes(ctx -> add(ctx, StringArgumentType.getString(ctx, "colour")))))
                .then(CommandManager.literal("clear").executes(PaletteCommands::clear))
                .then(CommandManager.literal("list").executes(PaletteCommands::list))
                .then(CommandManager.literal("save")
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> save(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(CommandManager.literal("load")
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> load(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(CommandManager.literal("delete")
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> delete(ctx, StringArgumentType.getString(ctx, "name"))))));
    }

    private static ServerPlayerEntity player(CommandContext<ServerCommandSource> ctx) {
        return ctx.getSource().getEntity() instanceof ServerPlayerEntity p ? p : null;
    }

    private static int open(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) { Chat.error(ctx.getSource(), "Only a player can open the palette."); return 0; }
        GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.PALETTE_LIST));
        return 1;
    }

    private static int add(CommandContext<ServerCommandSource> ctx, String colour) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) { Chat.error(ctx.getSource(), "Only a player has a palette."); return 0; }
        String hex = ColorLibrary.resolve(colour);
        if (hex == null) {
            Chat.error(ctx.getSource(), "Couldn't read the colour \"" + colour + "\". Use a name (red) or hex (#FF0000).");
            return 0;
        }
        if (PlayerPaletteManager.workingAdd(p.getUuid(), hex)) {
            Chat.success(ctx.getSource(), "Added §f" + hex + "§r to your working palette ("
                    + PlayerPaletteManager.working(p.getUuid()).size() + "/" + PlayerPaletteManager.WORKING_MAX + ").");
        } else {
            Chat.info(ctx.getSource(), "§f" + hex + "§r is already in your working palette (or it's full).");
        }
        return 1;
    }

    private static int clear(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) { Chat.error(ctx.getSource(), "Only a player has a palette."); return 0; }
        PlayerPaletteManager.workingClear(p.getUuid());
        Chat.success(ctx.getSource(), "Cleared your working palette.");
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) { Chat.error(ctx.getSource(), "Only a player has a palette."); return 0; }
        List<String> work = PlayerPaletteManager.working(p.getUuid());
        List<String> names = PlayerPaletteManager.names(p.getUuid());
        Chat.info(ctx.getSource(), "§6Working palette §7(" + work.size() + "): §f"
                + (work.isEmpty() ? "§8empty" : String.join(" ", work)));
        if (names.isEmpty()) {
            Chat.info(ctx.getSource(), "§7No saved palettes yet — §f/cb palette save <name>§7.");
        } else {
            Chat.info(ctx.getSource(), "§6Saved palettes §7(" + names.size() + "): §f" + String.join(", ", names));
        }
        return 1;
    }

    private static int save(CommandContext<ServerCommandSource> ctx, String name) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) { Chat.error(ctx.getSource(), "Only a player has a palette."); return 0; }
        String n = name.trim();
        if (n.isEmpty()) { Chat.error(ctx.getSource(), "Give the palette a name."); return 0; }
        if (PlayerPaletteManager.save(p.getUuid(), n)) {
            Chat.success(ctx.getSource(), "Saved palette §f" + n + "§r ("
                    + PlayerPaletteManager.working(p.getUuid()).size() + " colours).");
        } else {
            Chat.error(ctx.getSource(), "Your working palette is empty — add colours first (/cb palette add <colour>).");
        }
        return 1;
    }

    private static int load(CommandContext<ServerCommandSource> ctx, String name) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) { Chat.error(ctx.getSource(), "Only a player has a palette."); return 0; }
        String n = name.trim();
        if (PlayerPaletteManager.load(p.getUuid(), n)) {
            Chat.success(ctx.getSource(), "Loaded palette §f" + n + "§r ("
                    + PlayerPaletteManager.working(p.getUuid()).size() + " colours) into your working set.");
        } else {
            Chat.error(ctx.getSource(), "No palette called \"" + n + "\". See /cb palette list.");
        }
        return 1;
    }

    private static int delete(CommandContext<ServerCommandSource> ctx, String name) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) { Chat.error(ctx.getSource(), "Only a player has a palette."); return 0; }
        String n = name.trim();
        if (PlayerPaletteManager.delete(p.getUuid(), n)) {
            Chat.success(ctx.getSource(), "Deleted palette §f" + n + "§r.");
        } else {
            Chat.error(ctx.getSource(), "No palette called \"" + n + "\". See /cb palette list.");
        }
        return 1;
    }
}
