/**
 * GuiCommands.java
 *
 * Responsibility: /cb gui [block <id>] — open the in-game GUI by sending an
 * OpenGuiPayload to the requesting player's client.
 * /cb edithud — toggle HUD visibility.
 * Stays under 400 lines (§9.3).
 *
 * Depends on: OpenGuiPayload, GuiMode, ServerPlayNetworking, Chat
 * Called by: CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.GuiMode;
import com.customblocks.network.payloads.OpenGuiPayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class GuiCommands {

    private GuiCommands() {}

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // /cb gui — main menu
        var gui = CommandManager.literal("gui");
        gui.executes(ctx -> openGui(ctx, GuiMode.MAIN_MENU, ""));

        // /cb gui block <id> — block editor
        gui.then(CommandManager.literal("block")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> openGui(ctx, GuiMode.BLOCK_EDITOR,
                                StringArgumentType.getString(ctx, "id")))));

        // /cb gui config — config screen
        gui.then(CommandManager.literal("config")
                .executes(ctx -> openGui(ctx, GuiMode.CONFIG, "")));

        // /cb gui macros — macro list screen
        gui.then(CommandManager.literal("macros")
                .executes(ctx -> openGui(ctx, GuiMode.MACRO_LIST, "")));

        // /cb gui arabic — arabic browser screen
        gui.then(CommandManager.literal("arabic")
                .executes(ctx -> openGui(ctx, GuiMode.ARABIC_BROWSER, "")));

        root.then(gui);

        // /cb edithud — toggle HUD visibility
        root.then(CommandManager.literal("edithud").executes(GuiCommands::editHud));

        // /cb admingui — alias for main menu (admin context)
        root.then(CommandManager.literal("admingui")
                .executes(ctx -> openGui(ctx, GuiMode.MAIN_MENU, "")));
    }

    private static int openGui(CommandContext<ServerCommandSource> ctx,
                                GuiMode mode, String data) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();

        // For BLOCK_EDITOR: validate the id before opening
        if (mode == GuiMode.BLOCK_EDITOR && !data.isEmpty()) {
            if (SlotManager.getById(data) == null) {
                Chat.error(src, "No block '§e" + data + "§r'.");
                return 0;
            }
        }

        ServerPlayNetworking.send(player, new OpenGuiPayload(mode.id, data));
        return 1;
    }

    private static int editHud(CommandContext<ServerCommandSource> ctx) {
        CustomBlocksConfig.hudEnabled = !CustomBlocksConfig.hudEnabled;
        CustomBlocksConfig.save();
        String state = CustomBlocksConfig.hudEnabled ? "§aenabled" : "§cdisabled";
        Chat.success(ctx.getSource(), "HUD overlay " + state + "§r.");
        return 1;
    }
}
