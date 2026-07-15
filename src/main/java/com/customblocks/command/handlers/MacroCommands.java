/**
 * MacroCommands.java
 *
 * Responsibility: /cb macro subcommands — record, add, stop, cancel, play, list, delete.
 * Stays under 400 lines (§9.3).
 *
 * Depends on: MacroManager, Chat
 * Called by: CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.core.WidgetSync;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.MacroManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public final class MacroCommands {

    private MacroCommands() {}

    private static final SuggestionProvider<ServerCommandSource> MACRO_NAMES =
            (ctx, builder) -> {
                MacroManager.listNames().forEach(builder::suggest);
                return builder.buildFuture();
            };

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        var macro = CommandManager.literal("macro");

        macro.then(CommandManager.literal("record")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(ctx -> record(ctx, StringArgumentType.getString(ctx, "name")))));

        macro.then(CommandManager.literal("add")
                .then(CommandManager.argument("cmd", StringArgumentType.greedyString())
                        .executes(ctx -> add(ctx, StringArgumentType.getString(ctx, "cmd")))));

        macro.then(CommandManager.literal("stop")  .executes(MacroCommands::stop));
        macro.then(CommandManager.literal("cancel").executes(MacroCommands::cancel));

        macro.then(CommandManager.literal("play")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(MACRO_NAMES)
                        .executes(ctx -> play(ctx, StringArgumentType.getString(ctx, "name")))));

        macro.then(CommandManager.literal("list").executes(MacroCommands::list));

        macro.then(CommandManager.literal("delete")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(MACRO_NAMES)
                        .executes(ctx -> delete(ctx, StringArgumentType.getString(ctx, "name")))));

        root.then(macro);
    }

    private static int record(CommandContext<ServerCommandSource> ctx, String name)
            throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        if (MacroManager.isRecording(p.getUuid())) {
            Chat.error(ctx.getSource(), "Already recording. Use /cb macro stop or cancel first.");
            return 0;
        }
        MacroManager.startRecording(p.getUuid(), name);
        WidgetSync.push(p);   // G03: light the recording banner
        Chat.success(ctx.getSource(),
                "Recording macro \"" + CbFmt.VALUE + name + CbFmt.RESET + "\". Add steps with /cb macro add <cmd>. Finish with /cb macro stop.");
        return 1;
    }

    private static int add(CommandContext<ServerCommandSource> ctx, String cmd)
            throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        if (!MacroManager.isRecording(p.getUuid())) {
            Chat.error(ctx.getSource(), "Not recording. Start with /cb macro record <name>.");
            return 0;
        }
        String full = cmd.startsWith("/") ? cmd : "/" + cmd;
        MacroManager.addCommand(p.getUuid(), full);
        Chat.info(ctx.getSource(), "Added: " + CbFmt.DIM + full);
        return 1;
    }

    private static int stop(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        String name = MacroManager.stopRecording(p.getUuid());
        WidgetSync.push(p);   // G03: clear the recording banner
        if (name == null) { Chat.error(ctx.getSource(), "Not recording anything."); return 0; }
        Chat.success(ctx.getSource(), "Macro \"" + CbFmt.VALUE + name + CbFmt.RESET + "\" saved. Play with /cb macro play " + name);
        return 1;
    }

    private static int cancel(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        MacroManager.cancelRecording(p.getUuid());
        Chat.info(ctx.getSource(), "Recording cancelled.");
        return 1;
    }

    private static int play(CommandContext<ServerCommandSource> ctx, String name) {
        ServerCommandSource src = ctx.getSource();
        List<String> commands = MacroManager.load(name);
        if (commands == null) { Chat.error(src, "No macro named \"" + CbFmt.VALUE + name + CbFmt.RESET + "\"."); return 0; }
        if (commands.isEmpty()) { Chat.info(src, "Macro \"" + CbFmt.VALUE + name + CbFmt.RESET + "\" is empty."); return 1; }
        Chat.info(src, "Playing \"" + CbFmt.VALUE + name + CbFmt.RESET + "\" (" + commands.size() + " step(s))...");
        int ok = 0;
        for (String c : commands) {
            String line = c.startsWith("/") ? c.substring(1) : c;
            try { src.getServer().getCommandManager().getDispatcher().execute(line, src); ok++; }
            catch (Exception e) { Chat.error(src, "Error at step: " + CbFmt.BAD + c); }
        }
        Chat.success(src, "Done: " + CbFmt.VALUE + ok + "/" + commands.size() + CbFmt.RESET + " succeeded.");
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        List<String> names = MacroManager.listNames();
        if (names.isEmpty()) {
            Chat.info(src, "No macros saved. Create one with /cb macro record <name>.");
            return 1;
        }
        Chat.raw(src, CbFmt.VALUE + names.size() + " macro(s):");
        for (String n : names) {
            MutableText line = Text.literal(CbFmt.DIM + " - " + CbFmt.BODY + n + " ")
                    .append(Chat.runButton("[play]",   "/cb macro play "   + n, "Play macro "   + n))
                    .append(Text.literal(" "))
                    .append(Chat.runButton("[delete]", "/cb macro delete " + n, "Delete macro " + n));
            Chat.raw(src, line);
        }
        return 1;
    }

    private static int delete(CommandContext<ServerCommandSource> ctx, String name) {
        if (!MacroManager.exists(name)) {
            Chat.error(ctx.getSource(), "No macro named \"" + CbFmt.VALUE + name + CbFmt.RESET + "\".");
            return 0;
        }
        MacroManager.delete(name);
        Chat.success(ctx.getSource(), "Deleted macro \"" + CbFmt.VALUE + name + CbFmt.RESET + "\".");
        return 1;
    }

}
