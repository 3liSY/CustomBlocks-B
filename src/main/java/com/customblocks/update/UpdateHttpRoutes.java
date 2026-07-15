/**
 * UpdateHttpRoutes.java
 *
 * Responsibility: Register the two §K auto-update HTTP endpoints on the mod's existing embedded
 * server (GROUP_20 §CS3, AU1). Split out of {@link com.customblocks.network.ResourcePackServer} so
 * that class stays under the 500-line no-monolith limit (§9.3):
 *   • GET /version         → {@link ServerJarInfo#versionJson(String)} — {version, sha256, downloadUrl}.
 *   • GET /download/latest → the server's own customblocks-*.jar bytes (the single download source).
 *
 * The download URL is supplied lazily (a getter on ResourcePackServer that knows the live host+port),
 * so this class carries no dependency back on ResourcePackServer's internals.
 *
 * Depends on: com.sun.net.httpserver, ServerJarInfo
 * Called by: ResourcePackServer.start()
 */
package com.customblocks.update;

import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Supplier;

public final class UpdateHttpRoutes {

    private UpdateHttpRoutes() {} // static-only

    /** Wire /version + /download/latest onto {@code server}. {@code downloadUrl} is resolved per request. */
    public static void register(HttpServer server, Supplier<String> downloadUrl) {
        // GET /version → metadata JSON for the client's version check (AU3).
        server.createContext("/version", exchange -> {
            byte[] bytes = ServerJarInfo.versionJson(downloadUrl.get()).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });
        // GET /download/latest → the server's own customblocks-*.jar bytes (AU1: server is the source).
        server.createContext("/download/latest", exchange -> {
            File f = ServerJarInfo.available() ? ServerJarInfo.jarFile().toFile() : null;
            if (f == null || !f.exists()) {
                byte[] msg = "no update jar available".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, msg.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(msg); }
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/java-archive");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + f.getName() + "\"");
            exchange.sendResponseHeaders(200, f.length());
            try (OutputStream os = exchange.getResponseBody()) { Files.copy(f.toPath(), os); }
        });
    }
}
