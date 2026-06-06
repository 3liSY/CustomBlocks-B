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
        Chat.success(ctx.getSource(),
                "Recording macro '§e" + name + "§r'. Add steps with /cb macro add <cmd>. Finish with /cb macro stop.");
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
        Chat.info(ctx.getSource(), "Added: §7" + full);
        return 1;
    }

    private static int stop(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        String name = MacroManager.stopRecording(p.getUuid());
        if (name == null) { Chat.error(ctx.getSource(), "Not recording anything."); return 0; }
        Chat.success(ctx.getSource(), "Macro '§e" + name + "§r' saved. Play with /cb macro play " + name);
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
        if (commands == null) { Chat.error(src, "No macro named '§e" + name + "§r'."); return 0; }
        if (commands.isEmpty()) { Chat.info(src, "Macro '§e" + name + "§r' is empty."); return 1; }
        Chat.info(src, "Playing '§e" + name + "§r' (" + commands.size() + " step(s))...");
        int ok = 0;
        for (String c : commands) {
            String line = c.startsWith("/") ? c.substring(1) : c;
            try { src.getServer().getCommandManager().getDispatcher().execute(line, src); ok++; }
            catch (Exception e) { Chat.error(src, "Error at step: §c" + c); }
        }
        Chat.success(src, "Done: §e" + ok + "/" + commands.size() + "§r succeeded.");
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        List<String> names = MacroManager.listNames();
        if (names.isEmpty()) {
            Chat.info(src, "No macros saved. Create one with /cb macro record <name>.");
            return 1;
        }
        src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§e" + names.size() + " macro(s):"), false);
        for (String n : names) {
            MutableText line = Text.literal("§7 - §f" + n + " ")
                    .append(btn("[play]",   "/cb macro play "   + n, "Play macro "   + n))
                    .append(Text.literal(" "))
                    .append(btn("[delete]", "/cb macro delete " + n, "Delete macro " + n));
            src.sendFeedback(() -> line, false);
        }
        return 1;
    }

    private static int delete(CommandContext<ServerCommandSource> ctx, String name) {
        if (!MacroManager.exists(name)) {
            Chat.error(ctx.getSource(), "No macro named '§e" + name + "§r'.");
            return 0;
        }
        MacroManager.delete(name);
        Chat.success(ctx.getSource(), "Deleted macro '§e" + name + "§r'.");
        return 1;
    }

    private static MutableText btn(String label, String cmd, String hover) {
        return Text.literal(label).styled(s -> s
                .withColor(Formatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }
}
