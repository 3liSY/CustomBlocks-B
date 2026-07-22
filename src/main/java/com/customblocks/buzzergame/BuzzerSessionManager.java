/**
 * BuzzerSessionManager.java — Group 31 (BuzzerGame) item 1 (wand-owned session rebuild, 2026-07-18).
 *
 * The in-memory home for every host's {@link BuzzerSession}. Replaces the scrapped panel block as the
 * session anchor: a session is created when a host first uses the wand, keyed by the host's UUID, and
 * lives only in memory — it is NOT persisted and is destroyed when the host logs off (design lock).
 * Multiple hosts each own an independent session; there is no shared/global session.
 *
 * Server-thread only (block onUse, commands via server.execute, the end-of-tick ticker, and the
 * disconnect hook all run on the server thread), so plain HashMaps are safe — no synchronization.
 *
 * Responsibilities:
 *   - own the {hostId → session} and {sessionId → session} maps + the getOrCreate/get/bySession lookups
 *     linked buzzers / stands use to resolve their session.
 *   - drive every live session's clock once per server tick and hand each beat to {@link RoundBroadcast}
 *     to show players near the host (the old panel block used to do this; there is no block now).
 *   - on host logout (or a safety sweep of an offline host): auto-unlink every linked buzzer / stand and
 *     warn remaining players near them — the same cleanup shape as breaking a linked block, now also
 *     fired by disconnect.
 *
 * Depends on: BuzzerSession, BuzzerBlockEntity, TimerDisplayBlockEntity, RoundBroadcast, RoundBeat
 * Called by:  CustomBlocksMod (END_SERVER_TICK ticker + DISCONNECT cleanup), BuzzerGameWand (getOrCreate),
 *             BuzzerBlock / TimerDisplay* (bySession), BuzzerGameCommands (get)
 */
package com.customblocks.buzzergame;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BuzzerSessionManager {

    private BuzzerSessionManager() {} // static-only

    private static final Map<UUID, BuzzerSession> BY_HOST = new HashMap<>();
    private static final Map<UUID, BuzzerSession> BY_SESSION = new HashMap<>();

    /** The host's session, creating a fresh one if they don't have one yet (first wand use). */
    public static BuzzerSession getOrCreate(ServerPlayerEntity host) {
        UUID hostId = host.getUuid();
        BuzzerSession s = BY_HOST.get(hostId);
        if (s == null) {
            s = new BuzzerSession(hostId);
            BY_HOST.put(hostId, s);
            BY_SESSION.put(s.sessionId(), s);
        }
        return s;
    }

    /** The host's current session, or null if they have none. */
    public static @Nullable BuzzerSession get(UUID hostId) {
        return BY_HOST.get(hostId);
    }

    /** Resolve a linked buzzer/stand's stored session id to its live session (null = host gone / reset). */
    public static @Nullable BuzzerSession bySession(@Nullable UUID sessionId) {
        return sessionId == null ? null : BY_SESSION.get(sessionId);
    }

    // ------------------------------------------------------------------ per-tick clock + broadcast

    /**
     * Advance every live session's clock once per server tick; end any whose host has gone offline. Solo:
     * the only per-tick work is ticking the RUNNING stopwatch clock — there is no action-bar/title broadcast
     * anymore (design lock 2026-07-18, the physical screen is the only readout). The timer stand's own block
     * tick reads the current time off the session each tick, so the screen tracks the clock independently.
     */
    public static void tickAll(MinecraftServer server) {
        if (BY_HOST.isEmpty()) return;
        List<UUID> offline = null;
        for (BuzzerSession session : new ArrayList<>(BY_HOST.values())) {
            ServerPlayerEntity host = server.getPlayerManager().getPlayer(session.hostId());
            if (host == null) {
                // Safety net — DISCONNECT normally ends the session first; this catches any that slipped through.
                if (offline == null) offline = new ArrayList<>();
                offline.add(session.hostId());
                continue;
            }
            if (session.isBroadcasting()) {
                session.tick(); // solo: advance the RUNNING stopwatch clock (no broadcast)
            }
        }
        if (offline != null) {
            for (UUID hostId : offline) forget(hostId);
        }
    }

    // ------------------------------------------------------------------ logout / cleanup

    /** Host logged off: unlink every linked buzzer/stand and warn remaining players, then drop the session. */
    public static void onHostDisconnect(ServerPlayerEntity host) {
        BuzzerSession session = BY_HOST.get(host.getUuid());
        if (session == null) return;
        ServerWorld world = host.getServerWorld();
        List<BlockPos> anchors = unlinkAll(world, session);
        if (!anchors.isEmpty()) {
            RoundBroadcast.warnNearAny(world, anchors, "The host left — this buzzer game was unlinked.");
        }
        forget(host.getUuid());
    }

    /** Clear the BE-side link of every buzzer/stand in this session (loaded chunks); return their positions. */
    private static List<BlockPos> unlinkAll(ServerWorld world, BuzzerSession session) {
        List<BlockPos> positions = new ArrayList<>();
        for (BlockPos pos : session.linkedPositions()) {
            if (pos == null) continue;
            positions.add(pos);
            if (world.getBlockEntity(pos) instanceof BuzzerBlockEntity buzzer
                    && session.sessionId().equals(buzzer.getSessionId())) {
                buzzer.clearLink();
            } else if (world.getBlockEntity(pos) instanceof TimerDisplayBlockEntity display
                    && session.sessionId().equals(display.getSessionId())) {
                display.clearLink();
            }
        }
        return positions;
    }

    /** Drop a session from both maps (no cleanup/warn). */
    private static void forget(UUID hostId) {
        BuzzerSession s = BY_HOST.remove(hostId);
        if (s != null) BY_SESSION.remove(s.sessionId());
    }
}
