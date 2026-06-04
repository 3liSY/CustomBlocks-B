/**
 * UtilityCommands.java
 *
 * Responsibility: Non-mutating / utility subcommands — list, give, reload, export.
 * Registered into the /cb tree by CommandRegistrar. Stays under 400 lines (§9.3).
 *
 * `list` ends with clickable [.json] / [.txt] buttons that run `/cb export`, which
 * writes the block list to config/customblocks/exports/ and offers a copy-path button.
 */
package com.customblocks.command.handlers;

import com.customblocks.block.SlotBlock;
import com.customblocks.core.BlockExporter;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.nio.file.Path;
import java.util.Collection;

public final class UtilityCommands {

    private UtilityCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("list").executes(UtilityCommands::list));

        root.then(CommandManager.literal("give")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .executes(ctx -> give(ctx, StringArgumentType.getString(ctx, "id")))));

        root.then(CommandManager.literal("reload")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(UtilityCommands::reload));

        root.then(CommandManager.literal("export")
                .executes(ctx -> export(ctx, "json")) // bare "/cb export" defaults to json
                .then(CommandManager.literal("json").executes(ctx -> export(ctx, "json")))
                .then(CommandManager.literal("txt").executes(ctx -> export(ctx, "txt"))));
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Collection<SlotData> all = SlotManager.assignedSlots();
        if (all.isEmpty()) {
            src.sendFeedback(() -> Text.literal("§7No custom blocks yet. Make one with §f/cb create <id>§7."), false);
            return 1;
        }
        src.sendFeedback(() -> Text.literal("§e" + all.size() + " custom block(s):"), false);
        for (SlotData d : all) {
            src.sendFeedback(() -> Text.literal(
                    "§7 - §f" + d.customId() + " §7(slot " + d.index() + ", \"" + d.displayName() + "\")"), false);
        }
        // Clickable export options.
        MutableText exportLine = Text.literal("§7Export list: ")
                .append(runButton("[.json]", "/cb export json", "Export all blocks to a .json file"))
                .append(Text.literal(" "))
                .append(runButton("[.txt]", "/cb export txt", "Export all blocks to a .txt file"));
        src.sendFeedback(() -> exportLine, false);
        return 1;
    }

    private static int give(CommandContext<ServerCommandSource> ctx, String id) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.getById(id);
        if (d == null) {
            src.sendError(Text.literal("No block with id '" + id + "'."));
            return 0;
        }
        SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
        if (item == null) {
            src.sendError(Text.literal("Slot item missing for '" + id + "'."));
            return 0;
        }
        ServerPlayerEntity player = src.getPlayerOrThrow();
        player.getInventory().insertStack(new ItemStack(item));
        src.sendFeedback(() -> Text.literal("§aGave §f" + id + "§a."), false);
        return 1;
    }

    private static int reload(CommandContext<ServerCommandSource> ctx) {
        SlotManager.reload();
        ctx.getSource().sendFeedback(
                () -> Text.literal("§aReloaded §f" + SlotManager.usedSlots() + "§a block(s) from disk."), false);
        return 1;
    }

    private static int export(CommandContext<ServerCommandSource> ctx, String format) {
        ServerCommandSource src = ctx.getSource();
        Collection<SlotData> all = SlotManager.assignedSlots();
        if (all.isEmpty()) {
            src.sendFeedback(() -> Text.literal("§7Nothing to export yet — make a block with §f/cb create <id>§7."), false);
            return 1;
        }
        Path file = BlockExporter.exportAll(format, all);
        if (file == null) {
            src.sendError(Text.literal("Export failed (unsupported format '" + format + "' or write error)."));
            return 0;
        }
        String abs = file.toAbsolutePath().toString();
        MutableText msg = Text.literal("§aExported §f" + all.size() + "§a block(s) → §f" + file + " ")
                .append(copyButton(abs));
        src.sendFeedback(() -> msg, false);
        return 1;
    }

    // ── Clickable chat helpers ───────────────────────────────────────────────

    private static MutableText runButton(String label, String command, String hover) {
        return Text.literal(label).styled(s -> s
                .withColor(Formatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }

    private static MutableText copyButton(String path) {
        return Text.literal("[copy path]").styled(s -> s
                .withColor(Formatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, path))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Copy the file path to clipboard"))));
    }
}
