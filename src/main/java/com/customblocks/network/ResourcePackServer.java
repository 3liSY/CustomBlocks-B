/**
 * ResourcePackServer.java
 *
 * Responsibility: Run a tiny embedded HTTP server that serves ONE endpoint
 * (/pack.zip), rebuild that ZIP on demand (background thread), and prompt all online
 * players to (re)load it via a vanilla ResourcePackSend packet.
 *
 * Single texture path: the HTTP pack is sent to EVERY client (vanilla and modded). We do
 * NOT run a second client-side generator, which is what caused the old "PACK2" conflicts.
 *
 * Depends on: ServerPackGenerator, CustomBlocksConfig
 * Called by:  CustomBlocksMod (start/stop/setServer), the retexture command + player join.
 */
package com.customblocks.network;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.CustomBlocksMod;
import com.customblocks.network.payloads.RegenPackPayload;
import com.sun.net.httpserver.HttpServer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ResourcePackServer {

    private static final File PACK_FILE = new File("config/customblocks", "customblocks_pack.zip");

    private static HttpServer server;
    private static volatile File currentPackFile;
    private static volatile String currentHash;
    private static volatile int activePort = -1;
    private static MinecraftServer serverInstance;

    private static final ExecutorService PACK_BUILDER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "CustomBlocks-PackBuilder");
        t.setDaemon(true);
        return t;
    });

    // Group 05: debounce window — rapid texture changes collapse into one rebuild.
    private static final long DEBOUNCE_MS = 500;
    private static final ScheduledExecutorService DEBOUNCER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "CustomBlocks-PackDebounce");
        t.setDaemon(true);
        return t;
    });
    /** true while a rebuild is already scheduled within the debounce window. */
    private static final AtomicBoolean rebuildScheduled = new AtomicBoolean(false);

    // Group 05 B3 (SINGLEPLAYER-ONLY coalescing) ─────────────────────────────
    // Two /cb image commands fired within ~1s each download their image FIRST (1–6s) and only
    // THEN rebuild, so their rebuilds land seconds apart — past the 500ms debounce — giving TWO
    // silent reloads. A time-window can't fix a gap as long as a download; instead we COUNT the
    // in-flight image downloads and HOLD the integrated-host reload until the last one commits,
    // so the burst collapses into ONE reload.
    //
    // MP-SAFETY: this counter is READ in exactly one place — the RegenPackPayload branch of
    // sendToPlayer — which the dedicated server returns BEFORE reaching (isDedicated()). rebuild()
    // and updatePack() are untouched, so dedicated pack-sync timing is identical. The B3 redo rule.
    private static final AtomicInteger imageOpsInFlight = new AtomicInteger(0);
    /** A rebuild wanted to reload the integrated host but was HELD mid-burst; a reload is owed. */
    private static volatile boolean hostReloadHeld = false;

    /**
     * A /cb image download started — hold the singleplayer reload until it (and its peers) finish.
     * Returns a SINGLE-FIRE release: run it once (in a finally, AFTER this op's updatePack) to drop
     * the count no matter which path the op exits by — the CAS makes extra calls harmless.
     */
    public static Runnable beginImageOp() {
        imageOpsInFlight.incrementAndGet();
        AtomicBoolean done = new AtomicBoolean(false);
        return () -> { if (done.compareAndSet(false, true)) endImageOp(); };
    }

    /**
     * A /cb image download's work finished — call in a finally AFTER its {@code updatePack()} so the
     * count only drops once this op has committed. When the last op leaves and a reload was held back
     * during the burst, the last op's own rebuild (now that the count is 0) delivers the single reload.
     * The safety flush below covers the one case where no rebuild follows — e.g. the last op FAILED to
     * download, so nothing scheduled a rebuild — by pushing the current pack once (idempotent: the
     * LAST_SENT_PACK guard dedups if a real rebuild also sends). No-op on dedicated (sendToPlayer
     * returns before the held branch there).
     */
    public static void endImageOp() {
        int n = imageOpsInFlight.updateAndGet(v -> v > 0 ? v - 1 : 0); // clamp; never negative
        if (n == 0 && hostReloadHeld && !rebuildScheduled.get()) {
            hostReloadHeld = false;
            MinecraftServer s = serverInstance;
            if (s != null) s.execute(ResourcePackServer::sendToAll);
        }
    }

    // Group 02: pause auto-regeneration and suppress auto re-sends until explicitly resumed.
    private static volatile boolean paused = false;
    private static volatile boolean suppressed = false;

    // Deferred reload safety (recycled old-project behaviour): a pack push would yank the
    // player out of any open CustomBlocks menu/prompt, so pushes to such players are HELD
    // here and delivered when their GUI closes (CbChestHandler/AnvilPrompt call onGuiClosed).
    private static final java.util.Set<UUID> PENDING_SENDS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // The pack id last successfully sent to each player. A fresh server start fires TWO send
    // paths at the joining player (SERVER_STARTED rebuild → sendToAll, and the JOIN handler →
    // sendToPlayer); both carry the same pack hash, which the client shows as TWO prompts. We
    // record the id we sent and skip a repeat of the SAME id, so identical sends are idempotent
    // (one prompt). Cleared on disconnect (see forget()) so a genuine rejoin re-prompts once.
    private static final java.util.Map<UUID, UUID> LAST_SENT_PACK = new java.util.concurrent.ConcurrentHashMap<>();

    // Players who joined BEFORE the world's first pack build finished. On a fresh single-player
    // world the rebuild is async (~500ms+) but the JOIN fires almost immediately, so the join's
    // pack push finds currentHash == null and can deliver nothing. We remember those players here
    // and push to them the moment the first build completes — the join never loses the race again.
    private static final java.util.Set<UUID> AWAITING_FIRST_PACK = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private ResourcePackServer() {} // static-only

    public static void setServer(MinecraftServer s) { serverInstance = s; }
    public static int activePort() { return activePort; }
    public static String getHash() { return currentHash; }
    /** The currently-served pack file (size + last-modified), or null before the first build. */
    public static java.io.File getPackFile() { return currentPackFile; }
    /** True while a rebuild is scheduled within the debounce window (IT Chest "rebuilding"). */
    public static boolean isRebuilding() { return rebuildScheduled.get(); }

    public static void start() {
        if (server != null) server.stop(0);
        // These fields are static and survive a single-player world reload within the same client
        // JVM. If we don't clear them, a fresh JOIN sends the PREVIOUS world's stale pack hash (then
        // the new rebuild sends a different one → two prompts), and a leftover send-record can block
        // the real join send entirely (→ no prompt, and later edits never push). Start each world
        // load from a clean slate: the world's own rebuild + first send is the single source of truth.
        currentHash = null;
        currentPackFile = null;
        LAST_SENT_PACK.clear();
        PENDING_SENDS.clear();
        AWAITING_FIRST_PACK.clear();
        int base = CustomBlocksConfig.httpPort;
        int[] ports = { base, 8124, 8081, 24454, 3000 };
        for (int p : ports) {
            try {
                server = HttpServer.create(new InetSocketAddress(p), 0);
                activePort = p;
                break;
            } catch (Exception e) {
                CustomBlocksMod.LOGGER.warn("[CustomBlocks] HTTP port {} unavailable, trying next...", p);
            }
        }
        if (server == null || activePort < 0) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Could not open an HTTP port — textures will not load.");
            return;
        }
        server.createContext("/pack.zip", exchange -> {
            File f = currentPackFile;
            if (f == null || !f.exists()) {
                byte[] msg = "warming up".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, msg.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(msg); }
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.sendResponseHeaders(200, f.length());
            try (OutputStream os = exchange.getResponseBody()) { Files.copy(f.toPath(), os); }
        });
        // Serves per-block export JSONs: GET /export/<id>  (written by /cb export <id> download)
        server.createContext("/export/", exchange -> {
            String raw = exchange.getRequestURI().getPath().substring("/export/".length());
            String id  = raw.replaceAll("\\.json$", "").replaceAll("[^a-zA-Z0-9_\\-]", ""); // sanitise — no path traversal
            File f = new File("config/customblocks/cloud_exports", id + ".json");
            if (id.isEmpty() || !f.exists()) {
                byte[] msg = ("not found: " + id).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, msg.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(msg); }
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + id + ".json\"");
            exchange.sendResponseHeaders(200, f.length());
            try (OutputStream os = exchange.getResponseBody()) { Files.copy(f.toPath(), os); }
        });
        // Serves an exported block texture as a downloadable PNG: GET /png/<id>
        // (written by /cb exportpng <id> to config/customblocks/cloud_exports/<id>.png — the
        // clickable [download] link in chat points here so the file opens straight in a browser).
        server.createContext("/png/", exchange -> {
            String raw = exchange.getRequestURI().getPath().substring("/png/".length());
            String id  = raw.replaceAll("\\.png$", "").replaceAll("[^a-zA-Z0-9_\\-]", ""); // no traversal
            File f = new File("config/customblocks/cloud_exports", id + ".png");
            if (id.isEmpty() || !f.exists()) {
                byte[] msg = ("not found: " + id).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, msg.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(msg); }
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + id + ".png\"");
            exchange.sendResponseHeaders(200, f.length());
            try (OutputStream os = exchange.getResponseBody()) { Files.copy(f.toPath(), os); }
        });
        // Serves a block's CURRENT baked texture straight from TextureStore: GET /tex/<id>
        // (always available, no export step). The live-recolour slider fetches this to preview.
        server.createContext("/tex/", exchange -> {
            String raw = exchange.getRequestURI().getPath().substring("/tex/".length());
            String id  = raw.replaceAll("\\.png$", "").replaceAll("[^a-zA-Z0-9_\\-]", "");
            com.customblocks.core.SlotData d = id.isEmpty() ? null : com.customblocks.core.SlotManager.getById(id);
            byte[] tex = d == null ? null : com.customblocks.core.TextureStore.load(d.index());
            if (tex == null || tex.length == 0) {
                byte[] msg = ("not found: " + id).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, msg.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(msg); }
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, tex.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(tex); }
        });
        // Serves a bundle ZIP for direct browser download: GET /zip/<name>.zip
        // (Export All and Export Category write into cloud_exports/; the chat [download] link points here.)
        server.createContext("/zip/", exchange -> {
            String raw = exchange.getRequestURI().getPath().substring("/zip/".length());
            String name = raw.replaceAll("[^a-zA-Z0-9_.\\-]", ""); // no path traversal — slashes stripped
            File f = new File("config/customblocks/cloud_exports", name);
            if (name.isEmpty() || !name.endsWith(".zip") || !f.exists()) {
                byte[] msg = ("not found: " + name).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, msg.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(msg); }
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + name + "\"");
            exchange.sendResponseHeaders(200, f.length());
            try (OutputStream os = exchange.getResponseBody()) { Files.copy(f.toPath(), os); }
        });
        // Serves the diagnostic report as a downloadable text file: GET /report/diag_report.txt
        // (written by /cb report + the IT Chest "Generate Report" button to config/customblocks/data).
        server.createContext("/report/", exchange -> {
            File f = new File("config/customblocks/data", "diag_report.txt");
            if (!f.exists()) {
                byte[] msg = "no report generated yet".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, msg.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(msg); }
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"diag_report.txt\"");
            exchange.sendResponseHeaders(200, f.length());
            try (OutputStream os = exchange.getResponseBody()) { Files.copy(f.toPath(), os); }
        });
        // Group 20 §K — auto-update. Hash this server's own jar once, then expose /version + /download.
        com.customblocks.update.ServerJarInfo.init();
        com.customblocks.update.UpdateHttpRoutes.register(server, ResourcePackServer::getDownloadUrl);
        server.setExecutor(null);
        server.start();
        CustomBlocksMod.LOGGER.info("[CustomBlocks] Resource-pack HTTP server live on port {}", activePort);
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public static String getPackUrl() {
        int port = activePort > 0 ? activePort : CustomBlocksConfig.httpPort;
        return "http://" + CustomBlocksConfig.httpHost + ":" + port + "/pack.zip";
    }

    public static String getExportUrl(String id) {
        int port = activePort > 0 ? activePort : CustomBlocksConfig.httpPort;
        return "http://" + CustomBlocksConfig.httpHost + ":" + port + "/export/" + id;
    }

    /** Group 20 §K: the URL a client hits to download this server's jar (goes in /version + the join packet). */
    public static String getDownloadUrl() {
        int port = activePort > 0 ? activePort : CustomBlocksConfig.httpPort;
        return "http://" + CustomBlocksConfig.httpHost + ":" + port + "/download/latest";
    }

    /** Public URL for an exported PNG (the chat [download] link target). */
    public static String getPngUrl(String id) {
        int port = activePort > 0 ? activePort : CustomBlocksConfig.httpPort;
        return "http://" + CustomBlocksConfig.httpHost + ":" + port + "/png/" + id + ".png";
    }

    /** Public URL for the generated diagnostic report (the chat [download] link target). */
    public static String getReportUrl() {
        int port = activePort > 0 ? activePort : CustomBlocksConfig.httpPort;
        return "http://" + CustomBlocksConfig.httpHost + ":" + port + "/report/diag_report.txt";
    }

    /** Public URL for a block's current baked texture (used by the live-recolour preview). */
    public static String getTexUrl(String id) {
        int port = activePort > 0 ? activePort : CustomBlocksConfig.httpPort;
        return "http://" + CustomBlocksConfig.httpHost + ":" + port + "/tex/" + id + ".png";
    }

    /** Public URL for a bundle ZIP in cloud_exports/ (the chat [download] link target). */
    public static String getZipUrl(String fileName) {
        int port = activePort > 0 ? activePort : CustomBlocksConfig.httpPort;
        return "http://" + CustomBlocksConfig.httpHost + ":" + port + "/zip/" + fileName;
    }

    // ── Group 02: resource-pack regeneration controls ───────────────────────

    /** Pause automatic pack regeneration (edits won't rebuild the pack until resume()). */
    public static void pause() { paused = true; }

    /** Resume automatic regeneration and rebuild once immediately. */
    public static void resume() { paused = false; updatePack(); }

    public static boolean isPaused() { return paused; }

    /** Re-send the current pack to every online player; returns the player count. */
    public static int syncToAll() {
        MinecraftServer s = serverInstance;
        if (s == null) return 0;
        int n = s.getPlayerManager().getPlayerList().size();
        sendToAll();
        return n;
    }

    /** Clear the suppression flag and re-send the pack to all players. */
    public static void unsuppress() {
        suppressed = false;
        sendToAll();
    }

    /**
     * Request a pack rebuild. Debounced (~500ms): several rapid texture changes collapse
     * into a single rebuild that picks up the latest data, preventing pack thrash during
     * bulk operations (Group 05). The actual build runs on the single PACK_BUILDER thread.
     */
    public static void updatePack() {
        if (paused) return;
        // Only the first caller in a window schedules the rebuild; later calls ride along.
        if (rebuildScheduled.compareAndSet(false, true)) {
            DEBOUNCER.schedule(() -> {
                rebuildScheduled.set(false);
                if (!paused) rebuild();
            }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        }
    }

    /** Build the pack ZIP on the builder thread, then prompt all players to reload. */
    private static void rebuild() {
        PACK_BUILDER.submit(() -> {
            try {
                CustomBlocksMod.LOGGER.info("[CustomBlocks] Rebuilding resource pack…");
                ServerPackGenerator.generate(PACK_FILE);
                if (PACK_FILE.exists()) {
                    currentPackFile = PACK_FILE;
                    currentHash = sha1(PACK_FILE);
                    MinecraftServer s = serverInstance;
                    if (s != null) {
                        s.execute(ResourcePackServer::sendToAll);
                        // Dedicated server: re-capture the manifest and push it to modded clients so
                        // each pulls just the changed files (no-op on the integrated host).
                        s.execute(() -> com.customblocks.network.packsync.PackSyncService.refresh(s));
                    }
                }
            } catch (Exception e) {
                CustomBlocksMod.LOGGER.error("[CustomBlocks] Pack rebuild failed", e);
                // Major-error routing (Group 04): pack failures land in the incidents log too.
                com.customblocks.core.IncidentRecorder.record("Resource pack rebuild failed", e);
            }
        });
    }

    public static void sendToAll() {
        MinecraftServer s = serverInstance;
        if (s == null) return;
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) sendToPlayer(p);
    }

    public static void sendToPlayer(ServerPlayerEntity player) {
        if (suppressed) return;
        String hash = currentHash;
        if (hash == null || hash.isEmpty()) {
            // Pack not built yet — fresh world, the rebuild is still running. Remember this
            // player so the build's completion (rebuild → sendToAll) delivers to them; the join
            // must not silently drop the pack just because it arrived a few hundred ms early.
            AWAITING_FIRST_PACK.add(player.getUuid());
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Join before pack ready — {} queued for first build.",
                    player.getName().getString());
            return;
        }
        if (activePort < 0) return;
        PENDING_SENDS.remove(player.getUuid());
        if (hasBlockingGui(player)) {
            // Don't interrupt an open CustomBlocks menu — deliver when it closes.
            PENDING_SENDS.add(player.getUuid());
            return;
        }
        UUID id = UUID.nameUUIDFromBytes(hash.getBytes(StandardCharsets.UTF_8));
        // Already sent this exact pack to this player — skip so a second send path can't
        // produce a duplicate prompt. A new pack (different hash → different id) still sends.
        if (id.equals(LAST_SENT_PACK.get(player.getUuid()))) return;

        // A MODDED client (the host, modded friends) can decode our channels — it ignores the HTTP
        // push and instead generates the pack locally on this signal (Group 05 fix). Skip the self
        // HTTP push for them; vanilla friends still get the real download below.
        if (ServerPlayNetworking.canSend(player, RegenPackPayload.ID)) {
            // On a DEDICATED server the modded client's OWN local slot data is stale, so a local
            // regen renders server-created blocks magenta (Group 05 remote bug). PackSyncService
            // streams the real files from the server instead — it's driven by the JOIN handler
            // (beginSync) and rebuild (refresh), so just skip the regen push here. The integrated
            // host (singleplayer / LAN) shares the JVM, so its local regen below stays correct.
            if (serverInstance != null && serverInstance.isDedicated()) return;
            // Group 05 B3: mid-burst, an image download is still in flight → HOLD this reload so the
            // rapid ops collapse into one. We deliberately do NOT record LAST_SENT_PACK here, so the
            // eventual real send (last op's rebuild, or endImageOp's flush) still goes through.
            if (imageOpsInFlight.get() > 0) { hostReloadHeld = true; return; }
            try {
                ServerPlayNetworking.send(player, new RegenPackPayload(hash));
                LAST_SENT_PACK.put(player.getUuid(), id);
                hostReloadHeld = false; // a reload just went out — nothing owed
                AWAITING_FIRST_PACK.remove(player.getUuid());
                CustomBlocksMod.LOGGER.info("[CustomBlocks] Signaled modded client {} to regen pack locally (hash {}).",
                        player.getName().getString(), hash);
            } catch (Exception e) {
                CustomBlocksMod.LOGGER.warn("[CustomBlocks] Failed to signal modded client {}", player.getName().getString());
            }
            return;
        }

        try {
            ResourcePackSendS2CPacket packet = new ResourcePackSendS2CPacket(
                    id, getPackUrl(), hash, false,
                    Optional.of(Text.literal("CustomBlocks textures")));
            player.networkHandler.sendPacket(packet);
            LAST_SENT_PACK.put(player.getUuid(), id);
            AWAITING_FIRST_PACK.remove(player.getUuid());
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Sent resource pack to {} (id {}).",
                    player.getName().getString(), id);
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Failed to send pack to {}", player.getName().getString());
        }
    }

    /** Forget a player's send history (call on disconnect) so a later rejoin re-prompts once. */
    public static void forget(UUID uuid) {
        LAST_SENT_PACK.remove(uuid);
        PENDING_SENDS.remove(uuid);
        AWAITING_FIRST_PACK.remove(uuid);
    }

    /** True while the player is inside a CustomBlocks chest menu or anvil prompt. */
    private static boolean hasBlockingGui(ServerPlayerEntity player) {
        if (player.currentScreenHandler == player.playerScreenHandler) return false;
        if (player.currentScreenHandler instanceof com.customblocks.gui.chest.CbChestHandler h) {
            return !h.isDisposed();
        }
        return com.customblocks.gui.chest.AnvilPrompt.isPrompt(player.currentScreenHandler);
    }

    /**
     * A CustomBlocks GUI just closed for this player — if a pack push was held back, deliver
     * it shortly. The small delay lets menu→menu navigation (close + reopen in one action)
     * settle first; if another of our screens is open by then, the push keeps waiting.
     */
    public static void onGuiClosed(ServerPlayerEntity player) {
        if (!PENDING_SENDS.contains(player.getUuid())) return;
        MinecraftServer s = serverInstance;
        if (s == null) return;
        DEBOUNCER.schedule(() -> s.execute(() -> {
            if (player.isDisconnected() || hasBlockingGui(player)) return;
            if (PENDING_SENDS.remove(player.getUuid())) sendToPlayer(player);
        }), 300, TimeUnit.MILLISECONDS);
    }

    private static String sha1(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (InputStream is = Files.newInputStream(f.toPath())) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
