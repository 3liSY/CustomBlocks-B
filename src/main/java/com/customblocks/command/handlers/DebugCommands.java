/**
 * DebugCommands.java — hidden developer commands (not in /cb help; listed in HelpTopics.INTERNAL).
 *
 * /cb debug kick <cause>  — §G04-5. Force-fires the unified kick screen for a cause so H1–H9 (layout,
 *                           colours, Technical Details, Copy Report) are testable without reproducing a
 *                           real crash. The screen is client-side, so this only asks the client to open
 *                           it (KickPreviewPayload) — no real disconnect happens.
 *
 * OP-only (permission level 2). Causes are hard-listed here (registry / resourcepack / generic) rather
 * than read from the client-only CbKick, so this server-side handler carries no client dependency.
 *
 * Depends on: Chat, KickPreviewPayload. Registered by: CommandRegistrar.
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.network.payloads.KickPreviewPayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public final class DebugCommands {

    private DebugCommands() {} // static-only

    /** The kick causes /cb debug kick accepts — mirrors CbKick.forCause on the client. */
    private static final List<String> CAUSES = List.of("registry", "resourcepack", "generic");

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("debug")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("kick")
                        .then(CommandManager.argument("cause", StringArgumentType.word())
                                .suggests((c, b) -> { CAUSES.forEach(b::suggest); return b.buildFuture(); })
                                .executes(ctx -> kick(ctx, StringArgumentType.getString(ctx, "cause"))))));
    }

    private static int kick(CommandContext<ServerCommandSource> ctx, String cause) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) {
            Chat.error(src, "Run /cb debug kick as a player — the kick screen opens on your client.");
            return 0;
        }
        ServerPlayNetworking.send(p, new KickPreviewPayload(cause));
        Chat.info(src, "Previewing the \"" + cause + "\" kick screen. Press Back to return to the game.");
        return 1;
    }
}
