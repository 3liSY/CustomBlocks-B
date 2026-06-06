/**
 * CommandRegistrar.java
 *
 * Responsibility: Build and register the /customblock command tree (alias /cb), then
 * delegate each subcommand group to a focused handler in command/handlers/.
 *
 * Design rule #7: NO command monolith. Each handler stays under 400 lines (§9.3).
 * LANG1 is avoided by construction — we use a clean Brigadier literal tree, so there
 * is no stray "unknown_cb_tail" argument to leak into the action bar.
 *
 * Depends on: CreationCommands, AttributeCommands, UtilityCommands (handlers)
 * Called by:  CustomBlocksMod.onInitialize()
 */
package com.customblocks.command;

import com.customblocks.command.handlers.ArabicCommands;
import com.customblocks.command.handlers.AttributeCommands;
import com.customblocks.command.handlers.CloudCommands;
import com.customblocks.command.handlers.ConfigCommands;
import com.customblocks.command.handlers.CreationCommands;
import com.customblocks.command.handlers.DiagnosticsCommands;
import com.customblocks.command.handlers.GuiCommands;
import com.customblocks.command.handlers.HistoryCommands;
import com.customblocks.command.handlers.MacroCommands;
import com.customblocks.command.handlers.ManagementCommands;
import com.customblocks.command.handlers.TemplateCommands;
import com.customblocks.command.handlers.UtilityCommands;
import com.customblocks.command.handlers.VideoCommands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public final class CommandRegistrar {

    private CommandRegistrar() {} // static-only

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> {
            LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("customblock");
            CreationCommands.register(root);
            AttributeCommands.register(root);
            HistoryCommands.register(root);
            ConfigCommands.register(root);
            ManagementCommands.register(root);
            TemplateCommands.register(root);
            UtilityCommands.register(root);
            MacroCommands.register(root);
            ArabicCommands.register(root);
            DiagnosticsCommands.register(root);
            CloudCommands.register(root);
            GuiCommands.register(root);
            VideoCommands.register(root);
            LiteralCommandNode<ServerCommandSource> node = dispatcher.register(root);
            // /cb alias → redirects to the same tree.
            dispatcher.register(CommandManager.literal("cb").redirect(node));
        });
    }
}
