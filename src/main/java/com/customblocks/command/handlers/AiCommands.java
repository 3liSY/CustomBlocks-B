/**
 * AiCommands.java — Group 15 (AI Texture Generation).
 *
 * Registers /cb ai [prompt], which opens the Block Creation Studio on its AI tab:
 *   - /cb ai                  → opens on the AI tab, empty (no request fired).
 *   - /cb ai glowing crystal  → opens on the AI tab with the prompt pre-filled; the first generation
 *                               runs automatically (the client panel's debounce fires shortly after open).
 *
 * The prompt rides in OpenGuiPayload's data field as "ai:<prompt>" so the existing CREATE_STUDIO open
 * path carries it to the client (CustomBlocksClient parses the prefix). No new payload needed.
 *
 * Depends on: OpenGuiPayload, GuiMode, Chat. Called by: CommandRegistrar.
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.gui.GuiMode;
import com.customblocks.network.payloads.OpenGuiPayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class AiCommands {

    private AiCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("ai")
                .executes(ctx -> open(ctx.getSource(), ""))
                .then(CommandManager.argument("prompt", StringArgumentType.greedyString())
                        .executes(ctx -> open(ctx.getSource(), StringArgumentType.getString(ctx, "prompt")))));
    }

    private static int open(ServerCommandSource src, String prompt) {
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            Chat.error(src, "Run /cb ai as a player to open the Block Creation Studio's AI tab.");
            return 0;
        }
        ServerPlayNetworking.send(player,
                new OpenGuiPayload(GuiMode.CREATE_STUDIO.id, "ai:" + (prompt == null ? "" : prompt.trim())));
        return 1;
    }
}
