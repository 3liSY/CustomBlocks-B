/**
 * ConfigCommands.java
 *
 * Responsibility: In-game config tweaks via chat (NOT a GUI). Currently `/cb config undomode`
 * cycles the undo scope between server-wide and per-player; `/cb config` shows the current
 * value. Registered into the /cb tree by CommandRegistrar. Stays under 400 lines (§9.3).
 *
 * Switching undo scope clears the existing history (the old stacks live in a different
 * keyspace), so the change is predictable rather than leaving half-orphaned entries.
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.UndoManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Locale;

public final class ConfigCommands {

    private ConfigCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("config")
                .executes(ConfigCommands::show)
                .then(CommandManager.literal("undomode")
                        .executes(ConfigCommands::cycleUndoMode)
                        .then(CommandManager.argument("value", StringArgumentType.word())
                                .suggests((c, b) -> {
                                    b.suggest("serverwide");
                                    b.suggest("perplayer");
                                    return b.buildFuture();
                                })
                                .executes(ctx -> setUndoMode(ctx, StringArgumentType.getString(ctx, "value"))))));
    }

    private static int show(CommandContext<ServerCommandSource> ctx) {
        Chat.info(ctx.getSource(), "Undo mode: §f" + label(CustomBlocksConfig.undoMode)
                + " §8(/cb config undomode to switch)");
        return 1;
    }

    private static int cycleUndoMode(CommandContext<ServerCommandSource> ctx) {
        String next = "global".equals(CustomBlocksConfig.undoMode) ? "per_player" : "global";
        return apply(ctx, next);
    }

    private static int setUndoMode(CommandContext<ServerCommandSource> ctx, String raw) {
        String mode;
        switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "serverwide": case "server": case "global": case "shared":
                mode = "global"; break;
            case "perplayer": case "per_player": case "player": case "isolated":
                mode = "per_player"; break;
            default:
                Chat.error(ctx.getSource(), "Use: serverwide or perplayer");
                return 0;
        }
        return apply(ctx, mode);
    }

    private static int apply(CommandContext<ServerCommandSource> ctx, String mode) {
        CustomBlocksConfig.undoMode = mode;
        CustomBlocksConfig.save();
        UndoManager.clearAll(); // switching scope invalidates the old stacks
        Chat.success(ctx.getSource(), "Undo mode → " + label(mode) + " §7(history cleared)");
        return 1;
    }

    private static String label(String mode) {
        return "per_player".equals(mode) ? "per-player" : "server-wide";
    }
}
