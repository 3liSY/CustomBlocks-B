/**
 * PackManifest.java
 *
 * Responsibility: Capture ONE full snapshot of the resource pack as (path -> bytes) by running the single
 * source of truth, {@link ServerPackGenerator#emit}, exactly once; derive a (path -> sha256 + size)
 * fingerprint from it; and serve the bytes of any requested path back to the sync service. Also hashes an
 * on-disk loose pack folder so the client can diff what it already has against the server's fingerprints
 * and request only the missing/changed files (Group 05 dedicated sync; §G integrity/bounds hardening).
 *
 * Wire form of the manifest is GZIP of "path\tsha256\tsize\n" lines (§G): the sha lets the client verify
 * each received file, the size lets it bounds-check the transfer and preflight disk space. Parsing runs
 * through {@link PackSyncProtocol#gunzipBounded} and the file-count / path-length caps so a hostile or
 * corrupt manifest can never OOM or zip-bomb the receiver.
 *
 * Immutable once captured: a sync session holds one snapshot so every file it serves is internally
 * consistent (no mid-stream drift if a block changes during a large first sync). The next change captures
 * a fresh manifest.
 *
 * Depends on: ServerPackGenerator (emit), PackSyncProtocol (bounds + bounded inflate).
 * Called by:  PackSyncService (server snapshot + per-file bytes), ClientPackReceiver (client diff/verify).
 */
package com.customblocks.network.packsync;

import com.customblocks.CustomBlocksConfig;
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
import java.util.zip.GZIPOutputStream;

public final class PackManifest {

    /** Suffix of an in-progress client download — never hashed as a real pack file (§G resume/cleanup). */
    public static final String PART_SUFFIX = ".part";

    private final Map<String, byte[]> files;   // pack-relative path -> file bytes (insertion order)
    private final Map<String, String> hashes;  // pack-relative path -> sha256 hex
    private final long totalBytes;

    private PackManifest(Map<String, byte[]> files, Map<String, String> hashes, long totalBytes) {
        this.files = files;
        this.hashes = hashes;
        this.totalBytes = totalBytes;
    }

    /** One manifest row the client needs to verify + size a file: its expected sha256 and byte length. */
    public record Entry(String sha, long size) {}

    /**
     * Run {@link ServerPackGenerator#emit} once and snapshot every emitted file. Paths are normalised to
     * forward slashes so server (this) and client (folder hash) agree exactly. Call OFF the main server
     * thread (it reads SlotManager/TextureStore, like the existing pack rebuild already does).
     */
    public static PackManifest capture() throws Exception {
        return capture(CustomBlocksConfig.textureSize);
    }

    /**
     * Group 05 §F — capture the pack at a specific texture resolution ({@code size}). Every emitted PNG is
     * bounded to {@code size} on a side; models/blockstates/mcmeta are resolution-independent, so two
     * resolutions' manifests differ only where a texture actually shrank. {@code size} equal to the
     * configured full {@link CustomBlocksConfig#textureSize} reproduces the pre-§F snapshot.
     */
    public static PackManifest capture(int size) throws Exception {
        LinkedHashMap<String, byte[]> files = new LinkedHashMap<>();
        ServerPackGenerator.emit((path, data) -> files.put(norm(path), data), size);
        LinkedHashMap<String, String> hashes = new LinkedHashMap<>();
        long total = 0;
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            hashes.put(e.getKey(), hashHex(e.getValue()));
            total += e.getValue().length;
        }
        return new PackManifest(files, hashes, total);
    }

    /** Unmodifiable path -> sha256 map. */
    public Map<String, String> hashes() { return Collections.unmodifiableMap(hashes); }

    /** Bytes for one path, or null if the path isn't in this snapshot. */
    public byte[] bytes(String path) { return files.get(norm(path)); }

    /** Declared byte length of one path, or -1 if absent (server side, for per-file bound checks). */
    public long size(String path) {
        byte[] b = files.get(norm(path));
        return b == null ? -1 : b.length;
    }

    public int fileCount() { return files.size(); }

    public long totalBytes() { return totalBytes; }

    /**
     * Hash an on-disk loose pack folder into (pack-relative path -> sha256), so the client can diff its
     * existing {@code resourcepacks/CustomBlocks} against the server's manifest and request only what
     * differs. Missing folder -> empty map (client requests everything). {@code .part} temp files are
     * skipped — they are unverified in-progress downloads, not real pack content (§G). Forward-slash paths
     * to match {@link #capture}.
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
            } else if (!f.getName().endsWith(PART_SUFFIX)) {
                try {
                    String rel = norm(base.toPath().relativize(f.toPath()).toString());
                    out.put(rel, hashHex(Files.readAllBytes(f.toPath())));
                } catch (Exception ignored) {
                    // Unreadable file -> omit it; the diff treats it as missing and re-fetches.
                }
            }
        }
    }

    /**
     * One stable fingerprint for the WHOLE pack: sha256 over every "path\tsha256" line in sorted order
     * (size is content-derived, so it is deliberately excluded — identical bytes give an identical id).
     * The client records this after applying a sync so a rejoin with nothing changed is a no-op.
     */
    public String aggregateHash() { return aggregateHash(hashes); }

    /** Same fingerprint computed from any path -> sha map (the client uses this on its target). */
    public static String aggregateHash(Map<String, String> hashes) {
        TreeMap<String, String> sorted = new TreeMap<>(hashes);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            sb.append(e.getKey()).append('\t').append(e.getValue()).append('\n');
        }
        return hashHex(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Public sha256 hex of a byte[] — the client uses it to verify a received file. */
    public static String sha256Hex(byte[] data) { return hashHex(data); }

    // ── Transport framing (identical on server + client) ─────────────────────

    /** GZIP this manifest as "path\tsha256\tsize\n" lines (the PackManifestPayload body). */
    public byte[] gzipManifestLines() {
        List<String> lines = new ArrayList<>(hashes.size());
        for (Map.Entry<String, String> e : hashes.entrySet()) {
            long sz = size(e.getKey());
            lines.add(e.getKey() + '\t' + e.getValue() + '\t' + sz);
        }
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

    /**
     * Inverse of {@link #gzipLines}: bounded GZIP blob -> the lines (blank lines dropped). Routed through
     * {@link PackSyncProtocol#gunzipBounded} (input + inflate-size cap) and capped at
     * {@link PackSyncProtocol#MAX_FILES} lines, so a hostile blob is rejected before large allocation (§G8).
     * Throws {@link IllegalStateException} on any limit breach; callers catch it and reject the message.
     */
    public static List<String> gunzipLines(byte[] gz) {
        List<String> out = new ArrayList<>();
        if (gz == null || gz.length == 0) return out;
        byte[] raw = PackSyncProtocol.gunzipBounded(gz, PackSyncProtocol.MAX_INFLATED_BYTES);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(raw), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                if (out.size() >= PackSyncProtocol.MAX_FILES)
                    throw new IllegalStateException("manifest exceeds " + PackSyncProtocol.MAX_FILES + " files");
                out.add(line);
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("GUNZIP failed", e);
        }
        return out;
    }

    /** Parse a gunzipped manifest blob into path -> sha (first two columns; size ignored). */
    public static Map<String, String> parseManifest(byte[] gz) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, Entry> e : parseEntries(gz).entrySet()) out.put(e.getKey(), e.getValue().sha());
        return out;
    }

    /**
     * Parse a gunzipped "path\tsha256\tsize" manifest blob into path -> {@link Entry}. Rows with an unsafe
     * path (§G9), an over-length path, a malformed/negative size, or a size over
     * {@link PackSyncProtocol#MAX_FILE_BYTES} are dropped (the client simply won't request them). Insertion
     * order preserved.
     */
    public static Map<String, Entry> parseEntries(byte[] gz) {
        LinkedHashMap<String, Entry> out = new LinkedHashMap<>();
        for (String line : gunzipLines(gz)) {
            int t1 = line.indexOf('\t');
            if (t1 <= 0) continue;
            int t2 = line.indexOf('\t', t1 + 1);
            String path = line.substring(0, t1);
            String sha = t2 > t1 ? line.substring(t1 + 1, t2) : line.substring(t1 + 1);
            if (sha.isEmpty() || !PackSyncProtocol.safeRelPath(path)) continue;
            long size = -1;
            if (t2 > t1) { try { size = Long.parseLong(line.substring(t2 + 1).trim()); } catch (Exception ignored) {} }
            if (size < 0 || size > PackSyncProtocol.MAX_FILE_BYTES) continue;
            out.put(path, new Entry(sha, size));
        }
        return out;
    }

    private static String norm(String path) { return path.replace('\\', '/'); }

    static String hashHex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return hex(md.digest(data));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** Stream-hash a file with SHA-256 (the client verifies a committed {@code .part} without buffering it). */
    public static String sha256File(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(f.toPath())) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
        }
        return hex(md.digest());
    }

    private static String hex(byte[] digest) {
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) sb.append(Character.forDigit((b >> 4) & 0xF, 16))
                                .append(Character.forDigit(b & 0xF, 16));
        return sb.toString();
    }
}
