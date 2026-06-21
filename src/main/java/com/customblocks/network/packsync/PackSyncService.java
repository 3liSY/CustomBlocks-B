/**
 * PackSyncService.java
 *
 * Responsibility: Server side of the dedicated-server pack sync (Group 05, remote/dedicated fix).
 * Holds the current immutable {@link PackManifest} snapshot; on a modded client's join (or after a
 * block change) sends that client the manifest; when the client replies with a PackRequestPayload
 * (the files it lacks) queues them and streams them as PackFilePayload chunks, capped per player
 * per tick so a large first sync never lags the connection; finishes each stream with
 * PackDonePayload. The client only ever WRITES files — server stays the single source of truth.
 *
 * Only used on a DEDICATED server for MODDED clients (canSend check). The integrated host keeps
 * its untouched RegenPackPayload local regen; vanilla clients keep the HTTP pack.
 *
 * All session state is touched on the server main thread only (network receivers hop via
 * server.execute); the manifest capture runs off-thread, like the existing zip rebuild.
 *
 * Depends on: PackManifest, ServerPlayNetworking, the four Pack* payloads.
 * Called by:  CustomBlocksMod (JOIN -> beginSync, tick -> tick, request receiver -> onRequest,
 *             disconnect -> forget) and ResourcePackServer.rebuild (-> refresh on change).
 */
package com.customblocks.network.packsync;

import com.customblocks.CustomBlocksMod;
import com.customblocks.network.payloads.PackDonePayload;
import com.customblocks.network.payloads.PackFilePayload;
import com.customblocks.network.payloads.PackManifestPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PackSyncService {

    /** Bytes per chunk in one PackFilePayload (well under the custom-payload cap). */
    private static final int CHUNK_BYTES = 128 * 1024;
    /** Max bytes streamed to ONE player in ONE server tick (throttle — keeps the connection smooth). */
    private static final int BUDGET_PER_TICK = 256 * 1024;

    private static final ExecutorService CAPTURE = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "CustomBlocks-PackManifest");
        t.setDaemon(true);
        return t;
    });

    /** The immutable snapshot every active sync serves from. Replaced on change (debounced upstream). */
    private static volatile PackManifest current;
    /** Guards against overlapping captures; a change during a capture re-runs once it finishes. */
    private static final AtomicBoolean capturing = new AtomicBoolean(false);
    private static volatile boolean captureAgain = false;

    // Server-thread-only state:
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    /** Players who joined before the first manifest existed — synced the moment one is captured. */
    private static final Set<UUID> AWAITING = new HashSet<>();

    private PackSyncService() {} // static-only

    /** One in-flight sync for one player: the snapshot it serves + its send queue + chunk cursor. */
    private static final class Session {
        final PackManifest manifest;
        final Deque<String> queue = new ArrayDeque<>();
        boolean awaitingRequest = true;   // true until the client's request arrives
        String curPath;
        byte[] curData;
        int curOffset;
        int curIndex;
        int curCount;
        Session(PackManifest m) { this.manifest = m; }
    }

    /** A modded client on a dedicated server is the only one that uses file sync. */
    private static boolean eligible(ServerPlayerEntity player) {
        MinecraftServer s = player.getServer();
        return s != null && s.isDedicated() && ServerPlayNetworking.canSend(player, PackManifestPayload.ID);
    }

    /**
     * (Re)capture the manifest off-thread, then push it to every eligible online player so each
     * re-diffs and pulls only what changed. Call after the pack data changes (debounced upstream).
     */
    public static void refresh(MinecraftServer server) {
        if (server == null || !server.isDedicated()) return;
        if (!capturing.compareAndSet(false, true)) { captureAgain = true; return; }
        CAPTURE.submit(() -> {
            try {
                PackManifest m = PackManifest.capture();
                current = m;
                CustomBlocksMod.LOGGER.info("[CustomBlocks] Pack manifest ready ({} files, {} KB, hash {}).",
                        m.fileCount(), m.totalBytes() / 1024, m.aggregateHash());
                server.execute(() -> pushManifestToAll(server));
            } catch (Exception e) {
                CustomBlocksMod.LOGGER.error("[CustomBlocks] Pack manifest capture failed", e);
            } finally {
                capturing.set(false);
                if (captureAgain) { captureAgain = false; refresh(server); }
            }
        });
    }

    private static void pushManifestToAll(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (eligible(p)) beginSync(p);
        }
    }

    /**
     * Send one player the current manifest and open a fresh session. If no manifest exists yet
     * (very first join on a fresh world) remember the player and trigger a capture; the capture's
     * completion syncs them. No-op for vanilla / integrated host.
     */
    public static void beginSync(ServerPlayerEntity player) {
        if (!eligible(player)) return;
        PackManifest m = current;
        if (m == null) {
            AWAITING.add(player.getUuid());
            refresh(player.getServer());
            return;
        }
        AWAITING.remove(player.getUuid());
        SESSIONS.put(player.getUuid(), new Session(m));
        try {
            ServerPlayNetworking.send(player, new PackManifestPayload(m.gzipHashLines()));
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Sent pack manifest to {} ({} files).",
                    player.getName().getString(), m.fileCount());
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Failed to send pack manifest to {}", player.getName().getString());
            SESSIONS.remove(player.getUuid());
        }
    }

    /** Client asked for the files it lacks — queue exactly those (that exist in the snapshot). */
    public static void onRequest(ServerPlayerEntity player, byte[] gzPaths) {
        Session session = SESSIONS.get(player.getUuid());
        if (session == null) {
            // Request outside a known session (e.g. a refresh replaced it) — serve from current.
            PackManifest m = current;
            if (m == null) return;
            session = new Session(m);
            SESSIONS.put(player.getUuid(), session);
        }
        session.awaitingRequest = false;
        session.queue.clear();
        List<String> wanted = PackManifest.gunzipLines(gzPaths);
        int kept = 0;
        for (String path : wanted) {
            if (session.manifest.bytes(path) != null) { session.queue.add(path); kept++; }
        }
        CustomBlocksMod.LOGGER.info("[CustomBlocks] {} requested {} pack file(s); queued {}.",
                player.getName().getString(), wanted.size(), kept);
        if (session.queue.isEmpty()) finish(player, session); // already current
    }

    /** Stream queued chunks to every player mid-sync, capped per player per tick. */
    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) return;
        for (ServerPlayerEntity player : new ArrayList<>(server.getPlayerManager().getPlayerList())) {
            Session session = SESSIONS.get(player.getUuid());
            if (session == null || session.awaitingRequest) continue;
            pump(player, session);
        }
    }

    private static void pump(ServerPlayerEntity player, Session session) {
        long sent = 0;
        while (sent < BUDGET_PER_TICK) {
            if (session.curData == null) {
                String path = session.queue.poll();
                if (path == null) { finish(player, session); return; }
                byte[] data = session.manifest.bytes(path);
                if (data == null) continue; // vanished from the snapshot — skip
                session.curPath = path;
                session.curData = data;
                session.curOffset = 0;
                session.curIndex = 0;
                session.curCount = Math.max(1, (data.length + CHUNK_BYTES - 1) / CHUNK_BYTES);
            }
            int len = Math.min(CHUNK_BYTES, session.curData.length - session.curOffset);
            byte[] chunk = Arrays.copyOfRange(session.curData, session.curOffset, session.curOffset + len);
            try {
                ServerPlayNetworking.send(player, new PackFilePayload(
                        session.curPath, session.curIndex, session.curCount, chunk));
            } catch (Exception e) {
                CustomBlocksMod.LOGGER.warn("[CustomBlocks] Pack file send to {} failed; aborting sync.",
                        player.getName().getString());
                SESSIONS.remove(player.getUuid());
                return;
            }
            session.curOffset += len;
            session.curIndex++;
            sent += Math.max(len, 1); // count empty files as progress so they can't spin the loop
            if (session.curOffset >= session.curData.length) session.curData = null; // file complete
        }
    }

    private static void finish(ServerPlayerEntity player, Session session) {
        SESSIONS.remove(player.getUuid());
        try {
            ServerPlayNetworking.send(player, new PackDonePayload(session.manifest.aggregateHash()));
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Pack sync to {} complete (hash {}).",
                    player.getName().getString(), session.manifest.aggregateHash());
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Pack done-signal to {} failed.", player.getName().getString());
        }
    }

    /** Drop a player's session + awaiting flag (call on disconnect). */
    public static void forget(UUID uuid) {
        SESSIONS.remove(uuid);
        AWAITING.remove(uuid);
    }
}
