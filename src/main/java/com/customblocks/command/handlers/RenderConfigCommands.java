/**
 * RenderConfigCommands.java — Group 14 / Phase 1c Step 2b.
 *
 * Rendering-related /cb config subcommands, split out of ConfigCommands to keep that file under the
 * 400-line command-handler limit (§9.3). Currently: `/cb config transparent [toggle|on|off]` — the
 * off-atlas block background mode (off = solid black default, on = see-through). The value is server
 * config (persisted) and pushed to every client via TransparentBgPayload so it applies live.
 *
 * Depends on: CustomBlocksConfig, Chat, TransparentBgPayload
 * Called by:  ConfigCommands.register (merges this into the /cb config tree)
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.network.payloads.TransparentBgPayload;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class RenderConfigCommands {

    private RenderConfigCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // /cb config transparent [toggle|on|off] — off-atlas block background: off (default) = solid black,
        // on = see-through. Pushed to clients so it applies live.
        root.then(CommandManager.literal("config")
                .then(CommandManager.literal("transparent")
                        .executes(RenderConfigCommands::status)
                        .then(CommandManager.literal("toggle").executes(ctx -> set(ctx, !CustomBlocksConfig.transparentBackground)))
                        .then(CommandManager.literal("on").executes(ctx -> set(ctx, true)))
                        .then(CommandManager.literal("off").executes(ctx -> set(ctx, false)))));
    }

    private static int status(CommandContext<ServerCommandSource> ctx) {
        Chat.info(ctx.getSource(), "Block background: " + (CustomBlocksConfig.transparentBackground ? "§btransparent" : "§ablack")
                + " §8(/cb config transparent toggle)");
        return 1;
    }

    private static int set(CommandContext<ServerCommandSource> ctx, boolean transparent) {
        CustomBlocksConfig.transparentBackground = transparent;
        CustomBlocksConfig.save();
        // Push to every online client so off-atlas blocks rebuild in the new mode immediately.
        MinecraftServer s = ctx.getSource().getServer();
        if (s != null) {
            for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(p, new TransparentBgPayload(transparent));
            }
        }
        Chat.success(ctx.getSource(), transparent
                ? "Block backgrounds → transparent (see-through where the image has no pixels)."
                : "Block backgrounds → black (solid backdrop).");
        return 1;
    }
}
