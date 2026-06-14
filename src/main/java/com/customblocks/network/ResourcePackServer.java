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
import com.sun.net.httpserver.HttpServer;
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

    // Group 02: pause auto-regeneration and suppress auto re-sends until explicitly resumed.
    private static volatile boolean paused = false;
    private static volatile boolean suppressed = false;

    // Deferred reload safety (recycled old-project behaviour): a pack push would yank the
    // player out of any open CustomBlocks menu/prompt, so pushes to such players are HELD
    // here and delivered when their GUI closes (CbChestHandler/AnvilPrompt call onGuiClosed).
    private static final java.util.Set<UUID> PENDING_SENDS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private ResourcePackServer() {} // static-only

    public static void setServer(MinecraftServer s) { serverInstance = s; }
    public static int activePort() { return activePort; }
    public static String getHash() { return currentHash; }

    public static void start() {
        if (server != null) server.stop(0);
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
            String id  = raw.replaceAll("[^a-zA-Z0-9_\\-]", ""); // sanitise — no path traversal
            File f = new File("config/customblocks/exports", id + ".json");
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

    /** Public URL for an exported PNG (the chat [download] link target). */
    public static String getPngUrl(String id) {
        int port = activePort > 0 ? activePort : CustomBlocksConfig.httpPort;
        return "http://" + CustomBlocksConfig.httpHost + ":" + port + "/png/" + id + ".png";
    }

    /** Public URL for a block's current baked texture (used by the live-recolour preview). */
    public static String getTexUrl(String id) {
        int port = activePort > 0 ? activePort : CustomBlocksConfig.httpPort;
        return "http://" + CustomBlocksConfig.httpHost + ":" + port + "/tex/" + id + ".png";
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
                    if (s != null) s.execute(ResourcePackServer::sendToAll);
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
        if (hash == null || hash.isEmpty() || activePort < 0) return;
        PENDING_SENDS.remove(player.getUuid());
        if (hasBlockingGui(player)) {
            // Don't interrupt an open CustomBlocks menu — deliver when it closes.
            PENDING_SENDS.add(player.getUuid());
            return;
        }
        UUID id = UUID.nameUUIDFromBytes(hash.getBytes(StandardCharsets.UTF_8));
        try {
            ResourcePackSendS2CPacket packet = new ResourcePackSendS2CPacket(
                    id, getPackUrl(), hash, false,
                    Optional.of(Text.literal("CustomBlocks textures")));
            player.networkHandler.sendPacket(packet);
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Failed to send pack to {}", player.getName().getString());
        }
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
