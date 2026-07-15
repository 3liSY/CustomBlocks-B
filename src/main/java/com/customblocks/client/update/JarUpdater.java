/**
 * JarUpdater.java
 *
 * Responsibility: Client-side download + integrity + swap for §K auto-update (GROUP_20 §CS3, AU3/AU4).
 * Given the server's download URL + expected SHA-256, it: (1) downloads the jar to a staging file
 * under config/customblocks/updates/, (2) verifies the SHA-256 BEFORE touching mods/ (mismatch →
 * abort, staging deleted, mods/ untouched — K6), (3) swaps: renames the client's current
 * customblocks*.jar to `.disabled` (Fabric ignores it) and drops the new jar in as
 * customblocks-<version>.jar. Fabric loads the new one on the next JVM start.
 *
 * All heavy work runs on a single daemon thread; the UpdateScreen polls {@link #phase()} +
 * {@link #progress()} to draw its bar. CLIENT-SIDE ONLY.
 *
 * Depends on: FabricLoader (gamedir), java.net.HttpURLConnection, MessageDigest
 * Called by: UpdateScreen (start + poll), UpdateController (older-client-with-download path)
 */
package com.customblocks.client.update;

import com.customblocks.CustomBlocksMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;

@Environment(EnvType.CLIENT)
public final class JarUpdater {

    public enum Phase { IDLE, DOWNLOADING, VERIFYING, SWAPPING, DONE, FAILED }

    private static volatile Phase phase = Phase.IDLE;
    private static volatile long bytesRead = 0;
    private static volatile long contentLength = -1; // -1 = unknown (no Content-Length header)
    private static volatile String error = null;
    private static volatile Thread worker = null;

    private JarUpdater() {} // static-only

    public static Phase phase()  { return phase; }
    public static String error() { return error; }
    public static boolean isRunning() { return phase == Phase.DOWNLOADING || phase == Phase.VERIFYING || phase == Phase.SWAPPING; }
    public static boolean isDone()    { return phase == Phase.DONE; }
    public static boolean isFailed()  { return phase == Phase.FAILED; }

    /** 0.0–1.0 download fraction, or -1 when the total size is unknown (draw an indeterminate bar). */
    public static float progress() {
        if (contentLength <= 0) return -1f;
        return Math.min(1f, (float) bytesRead / (float) contentLength);
    }

    /** Reset to IDLE so a fresh session can run again (call on disconnect / screen re-open). */
    public static synchronized void reset() {
        if (isRunning()) return; // never yank a live download
        phase = Phase.IDLE;
        bytesRead = 0;
        contentLength = -1;
        error = null;
    }

    /**
     * Kick off download → verify → swap on a background thread. Idempotent while a run is live or
     * already finished — call once from the UpdateScreen. {@code serverVersion} names the new jar file.
     */
    public static synchronized void start(String downloadUrl, String expectedSha256, String serverVersion) {
        if (isRunning() || isDone()) return;
        error = null;
        bytesRead = 0;
        contentLength = -1;
        phase = Phase.DOWNLOADING;
        worker = new Thread(() -> run(downloadUrl, expectedSha256, serverVersion), "CustomBlocks-JarUpdater");
        worker.setDaemon(true);
        worker.start();
    }

    private static void run(String downloadUrl, String expectedSha256, String serverVersion) {
        Path staging = null;
        try {
            Path updatesDir = FabricLoader.getInstance().getGameDir().resolve("config/customblocks/updates");
            Files.createDirectories(updatesDir);
            staging = updatesDir.resolve("customblocks-" + safe(serverVersion) + ".jar.part");

            // ── 1. download ──────────────────────────────────────────────────
            HttpURLConnection conn = (HttpURLConnection) URI.create(downloadUrl).toURL().openConnection();
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(30_000);
            conn.setRequestProperty("User-Agent", "CustomBlocks-AutoUpdate");
            int code = conn.getResponseCode();
            if (code != 200) { fail("server returned HTTP " + code + " for the jar"); return; }
            contentLength = conn.getContentLengthLong();
            try (InputStream in = conn.getInputStream(); OutputStream out = Files.newOutputStream(staging)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    bytesRead += n;
                }
            }

            // ── 2. verify (BEFORE touching mods/) ────────────────────────────
            phase = Phase.VERIFYING;
            String actual = sha256Hex(staging);
            if (expectedSha256 == null || expectedSha256.isBlank() || !actual.equalsIgnoreCase(expectedSha256)) {
                Files.deleteIfExists(staging); // never keep a corrupt jar
                fail("hash mismatch — download corrupt, your mod was left untouched");
                return;
            }

            // ── 3. swap ──────────────────────────────────────────────────────
            phase = Phase.SWAPPING;
            Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
            Files.createDirectories(modsDir);
            disableCurrentJars(modsDir);
            Path target = modsDir.resolve("customblocks-" + safe(serverVersion) + ".jar");
            Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING);

            phase = Phase.DONE;
            CustomBlocksMod.LOGGER.info("[CustomBlocks] §K: staged update jar {} — restart to apply.", target.getFileName());
        } catch (Exception e) {
            try { if (staging != null) Files.deleteIfExists(staging); } catch (Exception ignored) {}
            fail(e.getClass().getSimpleName() + ": " + e.getMessage());
            CustomBlocksMod.LOGGER.error("[CustomBlocks] §K: auto-update failed", e);
        }
    }

    /** Rename every live customblocks*.jar (that isn't already .disabled) to <name>.disabled (AU4). */
    private static void disableCurrentJars(Path modsDir) throws Exception {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(modsDir, "customblocks*.jar")) {
            for (Path p : ds) {
                if (!Files.isRegularFile(p)) continue;
                Path disabled = p.resolveSibling(p.getFileName().toString() + ".disabled");
                Files.move(p, disabled, StandardCopyOption.REPLACE_EXISTING);
                CustomBlocksMod.LOGGER.info("[CustomBlocks] §K: disabled old jar {}", p.getFileName());
            }
        }
    }

    private static void fail(String msg) {
        error = msg;
        phase = Phase.FAILED;
    }

    /** Strip anything that isn't safe in a filename so a crafted version string can't path-traverse. */
    private static String safe(String v) {
        String s = v == null ? "unknown" : v.replaceAll("[^A-Za-z0-9._-]", "");
        return s.isEmpty() ? "unknown" : s;
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
