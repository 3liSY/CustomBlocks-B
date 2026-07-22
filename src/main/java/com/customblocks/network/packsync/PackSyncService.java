/**
 * PackSyncService.java
 *
 * Responsibility: Server side of the dedicated-server pack sync (Group 05). Holds an immutable
 * {@link PackManifest} snapshot PER RESOLUTION (§F: 128 / 256 / full); on a modded client's join (or after
 * a block change) it opens a SESSION for that player at their server-selected resolution
 * ({@link com.customblocks.core.LowResPlayers}), sends the matching variant's manifest, and streams the
 * files the client asks for as credit-paced PackFilePayload chunks, ending each round with PackDonePayload.
 *
 * §G — bounded, resumable, verified:
 *   • SESSION ID — every stream is tagged with a monotonic id; a superseded transfer (resolution change or
 *     new generation mid-sync) is cancelled by id, and stale requests/acks/applies are ignored (§G5/§G12).
 *   • CREDIT — each session starts with {@link PackSyncProtocol#INITIAL_CREDIT} send credit; the server
 *     spends credit as it sends and the client returns it via PackAckPayload as it writes, so outstanding
 *     bytes stay bounded and the stream self-paces to the client under latency/loss (§G1/§G4/§G15).
 *   • RETRY ROUNDS — after Done the session stays open (IDLE); a client that hit a SHA-256 mismatch may
 *     re-request just the failed files in the same session (§G7).
 *   • APPLIED — the server treats a generation as applied only when the client returns
 *     PackAppliedPayload(ok) after its reload, never on the file stream finishing (§G10/§G11).
 *   • BOUNDS — the request list is inflated through the bounded manifest parser and only safe,
 *     manifest-present paths are queued (§G8/§G9).
 *
 * Each resolution's artifact is built ONCE per published generation and shared across every player on that
 * resolution (§F16). Building runs off the main thread; a player whose variant isn't built yet waits in
 * AWAITING and is synced the moment its build lands.
 *
 * Only used on a DEDICATED server for MODDED clients (canSend check). The integrated host keeps its
 * untouched RegenPackPayload local regen; vanilla clients keep the full HTTP pack.
 *
 * All session state is touched on the server main thread only (network receivers hop via server.execute);
 * manifest capture runs off-thread, like the existing zip rebuild.
 *
 * Depends on: PackManifest, PackSyncProtocol, LowResPlayers, ServerPlayNetworking, the Pack* payloads.
 * Called by:  CustomBlocksMod (JOIN -> beginSync, tick -> tick, disconnect -> forget), PayloadRegistrar
 *             receivers (onRequest / onAck / onApplied), ResourcePackServer.rebuild (-> refresh),
 *             LowResCommands (-> resync).
 */
package com.customblocks.network.packsync;

import com.customblocks.CustomBlocksMod;
import com.customblocks.core.LowResPlayers;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public final class PackSyncService {

    private static final ExecutorService CAPTURE = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "CustomBlocks-PackManifest");
        t.setDaemon(true);
        return t;
    });

    // ── Per-generation, per-resolution manifest cache (built once per generation, shared) ──────
    private static volatile long generation = 1;
    private static final Map<Integer, PackManifest> MANIFESTS = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> MANIFEST_GEN = new ConcurrentHashMap<>();
    private static final Set<Integer> BUILDING = ConcurrentHashMap.newKeySet();

    /** Assigns a fresh id to every opened session so a superseded transfer is distinguishable (§G5). */
    private static final AtomicLong SESSION_SEQ = new AtomicLong();

    // Server-thread-only state:
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    /** Players waiting for their resolution's manifest to finish building: uuid -> resolution (px). */
    private static final Map<UUID, Integer> AWAITING = new HashMap<>();

    // ── §I compatibility handshake ────────────────────────────────────────────────
    /** uuid -> the pack-sync protocol version the client declared (absent until its hello arrives). */
    private static final Map<UUID, Integer> CLIENT_PROTO = new HashMap<>();
    /** uuid -> the client's human build string (for a distinguishable incompatibility log, §I3). */
    private static final Map<UUID, String> CLIENT_BUILD = new HashMap<>();
    /** uuid -> deadline (ms) by which a channel-capable client must have said a compatible hello (§I2). */
    private static final Map<UUID, Long> COMPAT_DEADLINE = new HashMap<>();
    /** Grace window for the client's hello to arrive before we assume it is too old to sync. */
    private static final long COMPAT_GRACE_MS = 4000;

    private PackSyncService() {} // static-only

    private enum State { AWAIT_REQUEST, STREAMING, IDLE }

    /** One in-flight sync for one player: the snapshot it serves + send queue + cursor + flow credit. */
    private static final class Session {
        final long id;
        final PackManifest manifest;
        final int size;
        final Deque<String> queue = new ArrayDeque<>();
        State state = State.AWAIT_REQUEST;
        /** Bytes the server may still put on the wire now; spent on send, replenished by client acks. */
        long credit = PackSyncProtocol.INITIAL_CREDIT;
        String curPath;
        byte[] curData;
        int curOffset;
        int curIndex;
        int curCount;
        // §I6 per-session diagnostics:
        final long startMs = System.currentTimeMillis();
        int filesRequested;   // total paths queued across all rounds
        long bytesStreamed;   // total bytes put on the wire
        int rounds;           // request→stream→done cycles (>1 means a retry happened)
        Session(long id, PackManifest m, int size) { this.id = id; this.manifest = m; this.size = size; }
    }

    /** A modded client on a dedicated server that registered our channel — CAN receive a stream at all. */
    private static boolean channelEligible(ServerPlayerEntity player) {
        MinecraftServer s = player.getServer();
        return s != null && s.isDedicated() && ServerPlayNetworking.canSend(player, PackManifestPayload.ID);
    }

    /** Channel-capable AND confirmed to speak our exact sync protocol (§I1) — safe to stream to. */
    private static boolean eligible(ServerPlayerEntity player) {
        return channelEligible(player)
                && CLIENT_PROTO.getOrDefault(player.getUuid(), -1) == PackSyncProtocol.PROTOCOL_VERSION;
    }

    /** This server's human build string for the handshake log (mod version + protocol, §I3). */
    private static String serverBuild() {
        String v = com.customblocks.update.ServerJarInfo.version();
        return (v == null ? "unknown" : v) + "/p" + PackSyncProtocol.PROTOCOL_VERSION;
    }

    /**
     * Called from the JOIN handler for every player. Vanilla / integrated host are ignored (the HTTP path
     * serves them). A channel-capable modded client is given a grace window to send its compatibility hello;
     * the hello (or its absence past the deadline) decides whether we stream or send a friendly "update"
     * message. beginSync is invoked too but no-ops until a compatible hello is recorded.
     */
    public static void onJoinAttempt(ServerPlayerEntity player) {
        if (!channelEligible(player)) return;
        UUID id = player.getUuid();
        if (CLIENT_PROTO.getOrDefault(id, -1) != PackSyncProtocol.PROTOCOL_VERSION)
            COMPAT_DEADLINE.put(id, System.currentTimeMillis() + COMPAT_GRACE_MS);
        beginSync(player); // real sync only if a compatible hello already arrived; else a harmless no-op
    }

    /** Client declared its pack-sync protocol — accept and sync, or reject clearly with both builds (§I1-3). */
    public static void onHello(ServerPlayerEntity player, int protocol, String build) {
        UUID id = player.getUuid();
        CLIENT_PROTO.put(id, protocol);
        CLIENT_BUILD.put(id, build == null ? "unknown" : build);
        COMPAT_DEADLINE.remove(id);
        if (protocol == PackSyncProtocol.PROTOCOL_VERSION) {
            CustomBlocksMod.LOGGER.info("[CustomBlocks] {} pack-sync handshake OK (client {}, server {}).",
                    player.getName().getString(), CLIENT_BUILD.get(id), serverBuild());
            beginSync(player);
        } else {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] {} pack-sync INCOMPATIBLE — client {} vs server {}; not streaming.",
                    player.getName().getString(), CLIENT_BUILD.get(id), serverBuild());
            sendIncompat(player, CLIENT_BUILD.get(id));
        }
    }

    /** Tell a player their build can't receive this server's textures — no reload, no silent magenta (§I2). */
    private static void sendIncompat(ServerPlayerEntity player, String clientBuild) {
        try {
            com.customblocks.command.Chat.toPlayer(player, net.minecraft.text.Text.literal(
                    "[CustomBlocks] Your mod build (" + clientBuild + ") can't sync this server's custom textures "
                            + "(server " + serverBuild() + "). Update CustomBlocks to see custom blocks.")
                    .formatted(net.minecraft.util.Formatting.YELLOW));
        } catch (Exception ignored) { /* player may already be gone */ }
    }

    /** Past the grace window a channel-capable client that never sent a compatible hello is treated as old. */
    private static void sweepCompatDeadlines(MinecraftServer server) {
        if (COMPAT_DEADLINE.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> e : new ArrayList<>(COMPAT_DEADLINE.entrySet())) {
            if (now < e.getValue()) continue;
            COMPAT_DEADLINE.remove(e.getKey());
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.getKey());
            if (p == null || !channelEligible(p)) continue;
            if (CLIENT_PROTO.getOrDefault(e.getKey(), -1) == PackSyncProtocol.PROTOCOL_VERSION) continue; // became OK
            String build = CLIENT_BUILD.getOrDefault(e.getKey(), "pre-§G (no handshake)");
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] {} sent no pack-sync handshake in {} ms — client {} vs server {}; treating as incompatible.",
                    p.getName().getString(), COMPAT_GRACE_MS, build, serverBuild());
            sendIncompat(p, build);
        }
    }

    /** The cached manifest for {@code size} if it is current, else null. */
    private static PackManifest readyManifest(int size) {
        PackManifest m = MANIFESTS.get(size);
        Long g = MANIFEST_GEN.get(size);
        return (m != null && g != null && g == generation) ? m : null;
    }

    /**
     * A pack change happened: bump the generation (invalidating every cached variant) and re-sync every
     * eligible player, which lazily rebuilds only the resolutions actually in use. Call on the main thread.
     */
    public static void refresh(MinecraftServer server) {
        if (server == null || !server.isDedicated()) return;
        generation++;
        MANIFESTS.clear();
        MANIFEST_GEN.clear();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (eligible(p)) beginSync(p);
        }
    }

    /**
     * Open a fresh session for one player at THEIR selected resolution and send that variant's manifest.
     * Any prior session for the player is dropped first (its id is now stale). If the variant isn't built
     * yet, remember the player and kick off the build; its completion syncs them. No-op for vanilla/host.
     */
    public static void beginSync(ServerPlayerEntity player) {
        if (!eligible(player)) return;
        UUID id = player.getUuid();
        SESSIONS.remove(id); // supersede any in-flight session — its id can no longer apply
        int size = LowResPlayers.sizeFor(id);
        PackManifest m = readyManifest(size);
        if (m == null) {
            AWAITING.put(id, size);
            build(player.getServer(), size);
            return;
        }
        AWAITING.remove(id);
        long sid = SESSION_SEQ.incrementAndGet();
        Session session = new Session(sid, m, size);
        SESSIONS.put(id, session);
        try {
            ServerPlayNetworking.send(player, new PackManifestPayload(sid, m.gzipManifestLines()));
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Sent {}px pack manifest to {} (session {}, {} files, {} KB, hash {}).",
                    size, player.getName().getString(), sid, m.fileCount(), m.totalBytes() / 1024, m.aggregateHash());
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Failed to send pack manifest to {}", player.getName().getString());
            SESSIONS.remove(id);
        }
    }

    /**
     * A resolution changed for one player (via /cblowres): cancel any in-flight session so its queued
     * chunks can't apply (they carry the old id), then start a fresh sync at the new selection. Main thread.
     */
    public static void resync(ServerPlayerEntity player) {
        if (player == null) return;
        forget(player.getUuid());
        beginSync(player);
    }

    /** Build the {@code size} variant off-thread (coalesced), then wake every player awaiting it. */
    private static void build(MinecraftServer server, int size) {
        if (server == null) return;
        final long gen = generation;
        if (!BUILDING.add(size)) return; // a build for this variant is already in flight
        CAPTURE.submit(() -> {
            try {
                PackManifest m = PackManifest.capture(size);
                MANIFESTS.put(size, m);
                MANIFEST_GEN.put(size, gen);
                CustomBlocksMod.LOGGER.info("[CustomBlocks] Pack manifest ready {}px ({} files, {} KB, hash {}, gen {}).",
                        size, m.fileCount(), m.totalBytes() / 1024, m.aggregateHash(), gen);
            } catch (Exception e) {
                CustomBlocksMod.LOGGER.error("[CustomBlocks] Pack manifest capture failed at {}px", size, e);
            } finally {
                BUILDING.remove(size);
                server.execute(() -> wakeAwaiting(server, size));
            }
        });
    }

    /** Re-run beginSync for every player waiting on {@code size} (re-checks readiness; stale gens rebuild). */
    private static void wakeAwaiting(MinecraftServer server, int size) {
        for (Map.Entry<UUID, Integer> e : new ArrayList<>(AWAITING.entrySet())) {
            if (e.getValue() != size) continue;
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.getKey());
            if (p == null) { AWAITING.remove(e.getKey()); continue; } // left while we built
            beginSync(p);
        }
    }

    /** Client asked for the files it lacks (or the retry files) — queue exactly those in the served snapshot. */
    public static void onRequest(ServerPlayerEntity player, long session, byte[] gzPaths) {
        UUID id = player.getUuid();
        Session s = SESSIONS.get(id);
        if (s == null || s.id != session) {
            CustomBlocksMod.LOGGER.debug("[CustomBlocks] Ignoring pack request from {} for stale/unknown session {}.",
                    player.getName().getString(), session);
            return;
        }
        if (s.state == State.STREAMING) return; // a request while already streaming is unexpected — ignore
        List<String> wanted;
        try {
            wanted = PackManifest.gunzipLines(gzPaths); // bounded inflate + file-count cap (§G8)
        } catch (Exception bad) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Rejected malformed pack request from {}: {}",
                    player.getName().getString(), bad.toString());
            return;
        }
        s.queue.clear();
        s.curData = null;
        int kept = 0;
        for (String path : wanted) {
            if (PackSyncProtocol.safeRelPath(path) && s.manifest.bytes(path) != null) { s.queue.add(path); kept++; }
        }
        s.filesRequested += kept;
        s.rounds++;
        CustomBlocksMod.LOGGER.info("[CustomBlocks] {} requested {} pack file(s); queued {} (session {}, {}px, round {}).",
                player.getName().getString(), wanted.size(), kept, s.id, s.size, s.rounds);
        if (s.queue.isEmpty()) sendDone(player, s); // nothing to send — end the round
        else s.state = State.STREAMING;
    }

    /** Client wrote {@code bytes} it received — return that much send credit (capped at the window). */
    public static void onAck(ServerPlayerEntity player, long session, long bytes) {
        Session s = SESSIONS.get(player.getUuid());
        if (s == null || s.id != session || bytes <= 0) return;
        s.credit = Math.min(PackSyncProtocol.INITIAL_CREDIT, s.credit + bytes);
    }

    /** Client finished its reload — record the terminal result for this session, then close it (§G10/§G11). */
    public static void onApplied(ServerPlayerEntity player, long session, String hash, boolean ok) {
        Session s = SESSIONS.get(player.getUuid());
        if (s == null || s.id != session) return;
        // §I6 — one auditable per-session summary (player/uuid, resolution, files, bytes, rounds, elapsed, result).
        long elapsed = System.currentTimeMillis() - s.startMs;
        CustomBlocksMod.LOGGER.info("[CustomBlocks] Pack session {} {} — {} ({}) {}px: {} files, {} KB, {} round(s), {} ms.",
                s.id, ok ? "APPLIED" : "FAILED (previous pack kept)", player.getName().getString(),
                player.getUuid(), s.size, s.filesRequested, s.bytesStreamed / 1024, s.rounds, elapsed);
        SESSIONS.remove(player.getUuid()); // round over either way; a rejoin/refresh opens a new session
    }

    /** Stream queued chunks to every streaming player, capped by credit and per-tick budget. */
    public static void tick(MinecraftServer server) {
        sweepCompatDeadlines(server); // §I2 — notify a client that never handshook
        if (SESSIONS.isEmpty()) return;
        for (ServerPlayerEntity player : new ArrayList<>(server.getPlayerManager().getPlayerList())) {
            Session s = SESSIONS.get(player.getUuid());
            if (s != null && s.state == State.STREAMING) pump(player, s);
        }
    }

    private static void pump(ServerPlayerEntity player, Session s) {
        long sent = 0;
        while (sent < PackSyncProtocol.BUDGET_PER_TICK) {
            if (s.curData == null) {
                String path = s.queue.poll();
                if (path == null) { sendDone(player, s); return; } // round drained
                byte[] data = s.manifest.bytes(path);
                if (data == null) continue; // vanished from the snapshot — skip
                s.curPath = path;
                s.curData = data;
                s.curOffset = 0;
                s.curIndex = 0;
                s.curCount = Math.max(1, (data.length + PackSyncProtocol.CHUNK_BYTES - 1) / PackSyncProtocol.CHUNK_BYTES);
            }
            int len = Math.min(PackSyncProtocol.CHUNK_BYTES, s.curData.length - s.curOffset);
            if (s.credit < len && len > 0) return;   // window full — wait for the client's ack (§G1/§G4)
            byte[] chunk = Arrays.copyOfRange(s.curData, s.curOffset, s.curOffset + len);
            try {
                ServerPlayNetworking.send(player, new PackFilePayload(s.id, s.curPath, s.curIndex, s.curCount, chunk));
            } catch (Exception e) {
                CustomBlocksMod.LOGGER.warn("[CustomBlocks] Pack file send to {} failed; aborting session {}.",
                        player.getName().getString(), s.id);
                SESSIONS.remove(player.getUuid());
                return;
            }
            s.credit -= len;
            s.curOffset += len;
            s.curIndex++;
            s.bytesStreamed += len;
            sent += Math.max(len, 1); // count empty files as progress so they can't spin the loop
            if (s.curOffset >= s.curData.length) s.curData = null; // file complete
        }
    }

    /** End one round: tell the client every requested byte was sent; keep the session open for retry/apply. */
    private static void sendDone(ServerPlayerEntity player, Session s) {
        s.state = State.IDLE;
        s.curData = null;
        try {
            ServerPlayNetworking.send(player, new PackDonePayload(s.id, s.manifest.aggregateHash()));
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Pack stream to {} sent (session {}, {}px, hash {}); awaiting apply.",
                    player.getName().getString(), s.id, s.size, s.manifest.aggregateHash());
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Pack done-signal to {} failed.", player.getName().getString());
            SESSIONS.remove(player.getUuid());
        }
    }

    /** Drop a player's in-flight session + awaiting flag (before a forced resync — KEEPS the handshake). */
    public static void forget(UUID uuid) {
        SESSIONS.remove(uuid);
        AWAITING.remove(uuid);
    }

    /** Full cleanup on disconnect: the session and the handshake record, so a rejoin re-handshakes fresh. */
    public static void onDisconnect(UUID uuid) {
        forget(uuid);
        COMPAT_DEADLINE.remove(uuid);
        CLIENT_PROTO.remove(uuid);
        CLIENT_BUILD.remove(uuid);
    }
}
