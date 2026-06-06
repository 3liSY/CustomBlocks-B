/**
 * CloudCommands.java
 *
 * Responsibility: /cb vault and /cb discord subcommands. Phase 14 stubs that tell
 * the user how to configure each feature rather than silently doing nothing.
 * Stays under 400 lines (§9.3).
 *
 * Depends on: CloudVaultClient, DiscordWebhook, Chat
 * Called by: CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.cloud.CloudVaultClient;
import com.customblocks.command.Chat;
import com.customblocks.discord.DiscordWebhook;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public final class CloudCommands {

    private CloudCommands() {}

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // /cb vault [upload <id> | download <code>]
        var vault = CommandManager.literal("vault");
        vault.executes(ctx -> vaultInfo(ctx));
        vault.then(CommandManager.literal("upload")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> vaultUpload(ctx,
                                StringArgumentType.getString(ctx, "id")))));
        vault.then(CommandManager.literal("download")
                .then(CommandManager.argument("code", StringArgumentType.word())
                        .executes(ctx -> vaultDownload(ctx,
                                StringArgumentType.getString(ctx, "code")))));
        root.then(vault);

        // /cb discord [test | status]
        var discord = CommandManager.literal("discord");
        discord.executes(ctx -> discordStatus(ctx));
        discord.then(CommandManager.literal("test").executes(ctx -> discordTest(ctx)));
        root.then(discord);
    }

    private static int vaultInfo(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!CloudVaultClient.isConfigured()) {
            Chat.info(src, "Block Vault is not configured. Set §evaultEndpoint§r in config.json to enable.");
        } else {
            Chat.info(src, "Vault endpoint configured. Use §e/cb vault upload <id>§r or §e/cb vault download <code>§r.");
        }
        return 1;
    }

    private static int vaultUpload(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        if (!CloudVaultClient.isConfigured()) {
            Chat.error(src, "Block Vault not configured — set vaultEndpoint in config.json.");
            return 0;
        }
        Chat.info(src, "Vault upload coming in Phase 14 — set up the Cloudflare Worker first.");
        return 1;
    }

    private static int vaultDownload(CommandContext<ServerCommandSource> ctx, String code) {
        ServerCommandSource src = ctx.getSource();
        if (!CloudVaultClient.isConfigured()) {
            Chat.error(src, "Block Vault not configured — set vaultEndpoint in config.json.");
            return 0;
        }
        Chat.info(src, "Vault download coming in Phase 14. Stay tuned!");
        return 1;
    }

    private static int discordStatus(CommandContext<ServerCommandSource> ctx) {
        if (DiscordWebhook.isConfigured())
            Chat.success(ctx.getSource(), "Discord webhook configured. Use /cb discord test to verify.");
        else
            Chat.info(ctx.getSource(), "Discord not configured. Set §ediscordWebhookUrl§r in config.json.");
        return 1;
    }

    private static int discordTest(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!DiscordWebhook.isConfigured()) { Chat.error(src, "Discord not configured."); return 0; }
        DiscordWebhook.post("[CustomBlocks] Test message from " + src.getName() + " — Discord active!");
        Chat.success(src, "Test message sent to Discord.");
        return 1;
    }
}
