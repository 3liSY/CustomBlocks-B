/**
 * RoundBroadcast.java — Group 31 (BuzzerGame) — trimmed to the solo stopwatch (2026-07-18).
 *
 * Solo has no action-bar timer, no 3-2-1-GO titles, and no host-run reveal (all removed / parked — the
 * physical timer stand is the only readout). What remains is the "near the game" player warning used when a
 * linked buzzer/stand is broken or the host logs off, anchored at a block position so two games in the same
 * world don't bleed into each other. The countdown/GO/reveal broadcast code was dropped with the multiplayer
 * pass (see the deferred H scope).
 *
 * Depends on: Chat
 * Called by:  BuzzerSessionManager.onHostDisconnect (logout warn), BuzzerBlock / TimerDisplayBlock (break warn)
 */
package com.customblocks.buzzergame;

import com.customblocks.command.Chat;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RoundBroadcast {

    private RoundBroadcast() {} // static-only

    /** How far from the anchor counts as "near the game" for warnings. */
    private static final double RADIUS = 48.0;
    private static final double RADIUS_SQ = RADIUS * RADIUS;

    /** Warn everyone near a point (e.g. a linked buzzer was broken mid-setup). */
    public static void warnNear(ServerWorld world, BlockPos anchor, String message) {
        for (ServerPlayerEntity p : nearby(world, anchor)) {
            Chat.toPlayerUnbranded(p, Text.literal("[BuzzerGame] " + message).formatted(Formatting.YELLOW));
        }
    }

    /** Warn everyone near ANY of the given points, each player once (host logout auto-unlink cleanup). */
    public static void warnNearAny(ServerWorld world, List<BlockPos> anchors, String message) {
        Set<ServerPlayerEntity> near = new LinkedHashSet<>();
        for (BlockPos anchor : anchors) near.addAll(nearby(world, anchor));
        Text line = Text.literal("[BuzzerGame] " + message).formatted(Formatting.YELLOW);
        for (ServerPlayerEntity p : near) Chat.toPlayerUnbranded(p, line);
    }

    private static List<ServerPlayerEntity> nearby(ServerWorld world, BlockPos anchor) {
        Vec3d center = Vec3d.ofCenter(anchor);
        List<ServerPlayerEntity> out = new ArrayList<>();
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (p.getPos().squaredDistanceTo(center) <= RADIUS_SQ) out.add(p);
        }
        return out;
    }
}
