/**
 * PackManifest.java
 *
 * Responsibility: Capture ONE full snapshot of the resource pack as (path -> bytes) by running
 * the single source of truth, {@link ServerPackGenerator#emit}, exactly once; derive a
 * (path -> sha1) fingerprint map from it; and serve the bytes of any requested path back to the
 * sync service. Also hashes an on-disk loose pack folder so the client can diff what it already
 * has against the server's fingerprints and request only the missing/changed files (Group 05,
 * remote/dedicated fix).
 *
 * Immutable once captured: a sync session holds one snapshot so every file it serves is internally
 * consistent (no mid-stream drift if a block changes during a large first sync). The next change
 * captures a fresh manifest.
 *
 * Depends on: ServerPackGenerator (emit)
 * Called by:  PackSyncService (server snapshot + per-file bytes), ClientPackReceiver via the
 *             folder-hash helper (client diff).
 */
package com.customblocks.network.packsync;

import com.customblocks.network.ServerPackGenerator;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class PackManifest {

    private final Map<String, byte[]> files;   // pack-relative path -> file bytes (insertion order)
    private final Map<String, String> hashes;  // pack-relative path -> sha1 hex
    private final long totalBytes;

    private PackManifest(Map<String, byte[]> files, Map<String, String> hashes, long totalBytes) {
        this.files = files;
        this.hashes = hashes;
        this.totalBytes = totalBytes;
    }

    /**
     * Run {@link ServerPackGenerator#emit} once and snapshot every emitted file. Paths are
     * normalised to forward slashes so server (this) and client (folder hash) agree exactly.
     * Call OFF the main server thread (it reads SlotManager/TextureStore, like the existing
     * pack rebuild already does on its builder thread).
     */
    public static PackManifest capture() throws Exception {
        LinkedHashMap<String, byte[]> files = new LinkedHashMap<>();
        ServerPackGenerator.emit((path, data) -> files.put(norm(path), data));
        LinkedHashMap<String, String> hashes = new LinkedHashMap<>();
        long total = 0;
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            hashes.put(e.getKey(), sha1(e.getValue()));
            total += e.getValue().length;
        }
        return new PackManifest(files, hashes, total);
    }

    /** Unmodifiable path -> sha1 map (what the manifest payload ships to the client). */
    public Map<String, String> hashes() { return Collections.unmodifiableMap(hashes); }

    /** Bytes for one path, or null if the path isn't in this snapshot. */
    public byte[] bytes(String path) { return files.get(norm(path)); }

    public int fileCount() { return files.size(); }

    public long totalBytes() { return totalBytes; }

    /**
     * Hash an on-disk loose pack folder into (pack-relative path -> sha1), so the client can diff
     * its existing {@code resourcepacks/CustomBlocks} against the server's manifest and request
     * only what differs. Missing folder -> empty map (client requests everything). Forward-slash
     * paths to match {@link #capture}.
     */
    public static Map<String, String> hashFolder(File root) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (root == null || !root.isDirectory()) return out;
        hashWalk(root, root, out);
        return out;
    }

    private static void hashWalk(File base, File dir, Map<String, String> out) {
        File[] kids = dir.listFiles();
        if (kids == null) return;
        for (File f : kids) {
            if (f.isDirectory()) {
                hashWalk(base, f, out);
            } else {
                try {
                    String rel = norm(base.toPath().relativize(f.toPath()).toString());
                    out.put(rel, sha1(Files.readAllBytes(f.toPath())));
                } catch (Exception ignored) {
                    // Unreadable file -> omit it; the diff treats it as missing and re-fetches.
                }
            }
        }
    }

    /**
     * One stable fingerprint for the WHOLE pack: sha1 over every "path\tsha1" line in sorted order.
     * The client records this after applying a sync so a rejoin with nothing changed is a no-op.
     */
    public String aggregateHash() { return aggregateHash(hashes); }

    /** Same fingerprint computed from any path -> sha1 map (the client uses this on its target). */
    public static String aggregateHash(Map<String, String> hashes) {
        TreeMap<String, String> sorted = new TreeMap<>(hashes);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            sb.append(e.getKey()).append('\t').append(e.getValue()).append('\n');
        }
        return sha1(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Public sha1 hex of a byte[] — the client uses it to update its applied-pack cache. */
    public static String sha1Hex(byte[] data) { return sha1(data); }

    // ── Transport framing (identical on server + client) ─────────────────────

    /** GZIP this manifest as "path\tsha1\n" lines (the PackManifestPayload body). */
    public byte[] gzipHashLines() {
        List<String> lines = new ArrayList<>(hashes.size());
        for (Map.Entry<String, String> e : hashes.entrySet()) lines.add(e.getKey() + '\t' + e.getValue());
        return gzipLines(lines);
    }

    /** GZIP a list of plain lines (used for the client's request path list). */
    public static byte[] gzipLines(List<String> lines) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(bos)) {
                for (String line : lines) {
                    gz.write(line.getBytes(StandardCharsets.UTF_8));
                    gz.write('\n');
                }
            }
            return bos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("GZIP failed", e);
        }
    }

    /** Inverse of {@link #gzipLines}: GZIP blob -> the lines (blank lines dropped). */
    public static List<String> gunzipLines(byte[] gz) {
        List<String> out = new ArrayList<>();
        if (gz == null || gz.length == 0) return out;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new ByteArrayInputStream(gz)), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.isEmpty()) out.add(line);
            }
        } catch (Exception e) {
            throw new IllegalStateException("GUNZIP failed", e);
        }
        return out;
    }

    /** Parse a gunzipped "path\tsha1" manifest blob back into a path -> sha1 map. */
    public static Map<String, String> parseManifest(byte[] gz) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (String line : gunzipLines(gz)) {
            int tab = line.indexOf('\t');
            if (tab > 0 && tab < line.length() - 1) out.put(line.substring(0, tab), line.substring(tab + 1));
        }
        return out;
    }

    private static String norm(String path) { return path.replace('\\', '/'); }

    static String sha1(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(Character.forDigit((b >> 4) & 0xF, 16))
                                    .append(Character.forDigit(b & 0xF, 16));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-1 unavailable", e);
        }
    }

    /** Stream-hash a file (kept for callers that prefer not to buffer whole files). */
    static String sha1(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (InputStream in = Files.newInputStream(f.toPath())) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) sb.append(Character.forDigit((b >> 4) & 0xF, 16))
                                .append(Character.forDigit(b & 0xF, 16));
        return sb.toString();
    }
}
