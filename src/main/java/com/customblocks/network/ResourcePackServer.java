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
    private static final AtomicInteger pendingBuilds = new AtomicInteger(0);

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

    /** Rebuild the pack ZIP on a background thread, then prompt all players to reload. */
    public static void updatePack() {
        if (pendingBuilds.incrementAndGet() > 1) {
            pendingBuilds.decrementAndGet(); // a build is already queued; it will use latest data
            return;
        }
        PACK_BUILDER.submit(() -> {
            try {
                ServerPackGenerator.generate(PACK_FILE);
                if (PACK_FILE.exists()) {
                    currentPackFile = PACK_FILE;
                    currentHash = sha1(PACK_FILE);
                    MinecraftServer s = serverInstance;
                    if (s != null) s.execute(ResourcePackServer::sendToAll);
                }
            } catch (Exception e) {
                CustomBlocksMod.LOGGER.error("[CustomBlocks] Pack rebuild failed", e);
            } finally {
                pendingBuilds.decrementAndGet();
            }
        });
    }

    public static void sendToAll() {
        MinecraftServer s = serverInstance;
        if (s == null) return;
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) sendToPlayer(p);
    }

    public static void sendToPlayer(ServerPlayerEntity player) {
        String hash = currentHash;
        if (hash == null || hash.isEmpty() || activePort < 0) return;
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
