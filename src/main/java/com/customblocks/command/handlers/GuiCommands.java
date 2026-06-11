/**
 * GuiCommands.java
 *
 * Responsibility: /cb edithud — open the drag-to-reposition HUD editor by sending an
 * OpenGuiPayload (mode = HUD_EDITOR) to the requesting player's client. HUD on/off
 * toggling now lives in `/cb config hud` (see ConfigCommands), so this command is no
 * longer a toggle stub. Stays under 400 lines (§9.3).
 *
 * Depends on: OpenGuiPayload, GuiMode, ServerPlayNetworking, Chat, SlotManager
 * Called by: CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.GuiMode;
import com.customblocks.network.payloads.OpenGuiPayload;
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
        // /cb gui, /cb admingui and the chest menus are handled by ChestGuiCommands.
        // /cb edithud — open the drag-to-reposition HUD editor (client-side overlay).
        root.then(CommandManager.literal("edithud").executes(GuiCommands::editHud));
    }

    private static int openGui(CommandContext<ServerCommandSource> ctx,
                                GuiMode mode, String data) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();

        // For BLOCK_EDITOR: validate the id before opening.
        if (mode == GuiMode.BLOCK_EDITOR && !data.isEmpty() && SlotManager.getById(data) == null) {
            Chat.error(src, "There's no block called \"" + data + "\". Check /cb list for the right id.");
            return 0;
        }

        ServerPlayNetworking.send(player, new OpenGuiPayload(mode.id, data));
        return 1;
    }

    private static int editHud(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        // The editor is a client-side overlay; tell the player's client to open it.
        return openGui(ctx, GuiMode.HUD_EDITOR, "");
    }
}
