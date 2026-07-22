/**
 * LowResCommands.java — GROUP 05 §F (server-authoritative /cblowres). SERVER-SIDE.
 *
 * Responsibility: the "/cblowres <128|256|512|off> [player]" command that chooses a player's pack
 * resolution ON THE SERVER, then forces an immediate resync of that player's dedicated pack stream.
 *
 *   /cblowres                 — show your current selection.
 *   /cblowres <size>          — set YOUR OWN resolution (any player, no operator access; §F1).
 *   /cblowres <size> <player> — set ANOTHER player's resolution (operator level 2 only; §F2/§F3).
 *                               Online → applied now; offline → stored as pending (§F5).
 *
 * Its own root literal (NOT a /cb subcommand): the old client-side /cblowres owned this root; now the
 * server does. Registered from CommandRegistrar alongside the /cb tree.
 *
 * Depends on: LowResPlayers (authority + persistence), PackSyncService (resync), Chat.
 * Called by:  CommandRegistrar.register().
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.LowResPlayers;
import com.customblocks.network.packsync.PackSyncService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class LowResCommands {

    private LowResCommands() {} // static-only

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("cblowres")
                .executes(ctx -> status(ctx.getSource()))
                .then(CommandManager.argument("size", StringArgumentType.word())
                        .suggests((c, b) -> { b.suggest("128"); b.suggest("256"); b.suggest("512"); b.suggest("off"); return b.buildFuture(); })
                        .executes(ctx -> apply(ctx, StringArgumentType.getString(ctx, "size"), null))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .suggests((c, b) -> {
                                    MinecraftServer s = c.getSource().getServer();
                                    if (s != null) for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) b.suggest(p.getGameProfile().getName());
                                    return b.buildFuture();
                                })
                                .executes(ctx -> apply(ctx, StringArgumentType.getString(ctx, "size"),
                                        StringArgumentType.getString(ctx, "player"))))));
    }

    /** No-arg: report the sender's own current selection. */
    private static int status(ServerCommandSource src) {
        ServerPlayerEntity self = src.getPlayer();
        if (self == null) {
            Chat.info(src, "Usage: /cblowres <128|256|512|off> <player> — from console you must name a player.");
            return 0;
        }
        Chat.info(src, "Your CustomBlocks resolution is " + LowResPlayers.label(LowResPlayers.sizeFor(self.getUuid()))
                + ". Change it with /cblowres 128 | 256 | off.");
        return 1;
    }

    private static int apply(CommandContext<ServerCommandSource> ctx, String sizeToken, String targetName) {
        ServerCommandSource src = ctx.getSource();
        int size = LowResPlayers.parseSize(sizeToken);
        if (size < 0) { // §F18 — unknown/malformed size makes no change
            Chat.error(src, "Pick a resolution: 128, 256, or 512/off. \"" + sizeToken + "\" isn't one.");
            return 0;
        }
        return targetName == null ? applySelf(src, size) : applyOther(src, size, targetName);
    }

    /** §F1 — set the sender's own resolution (no operator gate). Console has no self, so it must name a player (§F4). */
    private static int applySelf(ServerCommandSource src, int size) {
        ServerPlayerEntity self = src.getPlayer();
        if (self == null) {
            Chat.error(src, "The console has no player to set — use /cblowres " + size + " <player>.");
            return 0;
        }
        LowResPlayers.setUuid(self.getUuid(), self.getGameProfile().getName(), size);
        PackSyncService.resync(self);
        Chat.success(src, "Your CustomBlocks resolution is now " + LowResPlayers.label(size)
                + (size >= LowResPlayers.FULL ? " — restoring the full pack." : " — re-syncing the smaller pack, no rejoin needed."));
        return 1;
    }

    /** §F2/§F3/§F5 — set another player's resolution; operator level 2 only, online applied now, offline pending. */
    private static int applyOther(ServerCommandSource src, int size, String targetName) {
        if (!src.hasPermissionLevel(2)) { // §F3 — a non-operator cannot change anyone else
            Chat.error(src, "Only operators can change another player's resolution.");
            return 0;
        }
        MinecraftServer server = src.getServer();
        ServerPlayerEntity target = server == null ? null : server.getPlayerManager().getPlayer(targetName);
        if (target != null) {
            LowResPlayers.setUuid(target.getUuid(), target.getGameProfile().getName(), size);
            PackSyncService.resync(target); // §F2 — cancels any older session and starts one immediate sync
            Chat.success(src, target.getGameProfile().getName() + "'s CustomBlocks resolution is now "
                    + LowResPlayers.label(size) + " — re-syncing now.");
            Chat.info(target.getCommandSource(), "An operator set your CustomBlocks resolution to " + LowResPlayers.label(size) + ".");
        } else { // §F5 — offline: remember by name, bind on their next join
            LowResPlayers.setPending(targetName, size);
            Chat.success(src, "Saved: \"" + targetName + "\" will use " + LowResPlayers.label(size)
                    + " the next time they join (they are offline now).");
        }
        return 1;
    }
}
