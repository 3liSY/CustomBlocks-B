/**
 * ServerJarInfo.java
 *
 * Responsibility: Server-side source of truth for §K auto-update (GROUP_20 §CS3). On boot it
 * finds the running mod's own jar in the server's `mods/` folder, SHA-256's it, and caches
 * {version, sha256, jarFile}. The two HTTP contexts in {@link com.customblocks.network.ResourcePackServer}
 * (/version, /download/latest) read from here — the server IS the single download source (AU1);
 * no Cloudflare worker, no R2.
 *
 * Version comes from Fabric metadata (authoritative for what's actually running). The jar FILE is
 * located by scanning `mods/` for `customblocks-*.jar` — matches the owner's mental model
 * ("take it from the server's mods folder"). In a dev run (no packaged jar) the scan finds nothing
 * and {@link #available()} stays false, so /download 404s cleanly — auto-update is an MP-only,
 * real-jar feature.
 *
 * Depends on: FabricLoader (version + gamedir), Gson (JSON), CustomBlocksMod.MOD_ID
 * Called by:  ResourcePackServer.start() (init), the /version + /download contexts (getters)
 */
package com.customblocks.update;

import com.customblocks.CustomBlocksMod;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public final class ServerJarInfo {

    private static volatile String version = null;   // always set once init() runs (from metadata)
    private static volatile String sha256  = null;   // null until the jar is found + hashed
    private static volatile Path   jarFile = null;   // null in dev (no packaged jar in mods/)
    private static volatile boolean initialized = false;

    private ServerJarInfo() {} // static-only

    /**
     * Locate + hash the running jar. Idempotent and cheap to call again (guards on {@link #initialized}).
     * Safe on the integrated (singleplayer) host too — it just serves that client's own jar, which no
     * one downloads from, so it is harmless.
     */
    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        // Version — always available from Fabric metadata, even in dev.
        version = FabricLoader.getInstance()
                .getModContainer(CustomBlocksMod.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse(null);
        if (version == null) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] auto-update: no mod metadata — auto-update version unavailable.");
            return;
        }

        // Jar file — scan the server's own mods/ folder for customblocks-*.jar.
        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        Path found = findJar(modsDir);
        if (found == null) {
            CustomBlocksMod.LOGGER.info(
                    "[CustomBlocks] auto-update: no packaged customblocks jar in {} (dev run?) — /download disabled, version still reported.",
                    modsDir);
            return;
        }
        try {
            jarFile = found;
            sha256  = sha256Hex(found);
            CustomBlocksMod.LOGGER.info("[CustomBlocks] auto-update: serving auto-update jar {} (v{}, sha256 {}…)",
                    found.getFileName(), version, sha256.substring(0, Math.min(12, sha256.length())));
        } catch (Exception e) {
            jarFile = null;
            sha256  = null;
            CustomBlocksMod.LOGGER.error("[CustomBlocks] auto-update: failed to hash jar {} — /download disabled.", found, e);
        }
    }

    /** First `customblocks*.jar` (ignoring any `.disabled` swap leftovers) in the mods folder, or null. */
    private static Path findJar(Path modsDir) {
        if (modsDir == null || !Files.isDirectory(modsDir)) return null;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(modsDir, "customblocks*.jar")) {
            for (Path p : ds) {
                if (Files.isRegularFile(p)) return p;
            }
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] auto-update: could not scan {} for the jar", modsDir, e);
        }
        return null;
    }

    /** True once a real jar has been located AND hashed — the only state in which /download may serve bytes. */
    public static boolean available() { return jarFile != null && sha256 != null; }

    public static String version() { return version; }
    public static String sha256()  { return sha256;  }
    public static Path   jarFile() { return jarFile; }

    /**
     * The JSON body for GET /version. {@code downloadUrl} is passed in by the HTTP context (it knows the
     * live host + port). {@code sha256}/{@code downloadUrl} are null when {@link #available()} is false so
     * a dev/no-jar server still answers /version honestly (client sees no download and falls back to a toast).
     */
    public static String versionJson(String downloadUrl) {
        JsonObject o = new JsonObject();
        o.addProperty("version", version);
        if (available()) {
            o.addProperty("sha256", sha256);
            o.addProperty("downloadUrl", downloadUrl);
        } else {
            o.add("sha256", null);
            o.add("downloadUrl", null);
        }
        return o.toString();
    }

    private static String sha256Hex(Path p) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(p)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
