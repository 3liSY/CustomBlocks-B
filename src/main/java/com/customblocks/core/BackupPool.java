/**
 * BackupPool.java — Group 09 (P2, 2026-07-23): content-addressed storage so backups stop duplicating
 * unchanged bytes. Every file a snapshot would copy is hashed (SHA-256) and its bytes are stored ONCE
 * under backups/_pool/&lt;sha&gt;; each backup then records only a path→sha list (see BackupManager
 * format v3). Textures rarely change, so a second auto-backup that re-references the same 140 MB of
 * texture blobs costs only its small manifest — letting the owner keep far more restore points on disk.
 *
 * RELIABILITY: a blob is written to &lt;sha&gt;.tmp then atomically renamed, and writing a blob that
 * already exists is a no-op, so concurrent/duplicate saves are safe. The pool is pure content — deleting
 * a backup never deletes a blob another backup still references; {@link #gc(Set)} sweeps only orphans.
 * The pool dir name starts with "_" so BackupManager.list() skips it (it is not a backup).
 *
 * Depends on: CustomBlocksMod (LOGGER). Called by: BackupManager (save/restore/delete/zip).
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksMod;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

public final class BackupPool {

    private BackupPool() {} // static-only

    /** Content-blob store, a sibling of the named backup folders. Leading "_" keeps it out of list(). */
    public static final Path POOL_DIR = BackupManager.BACKUPS_DIR.resolve("_pool");

    /** The pool folder name, so BackupManager can skip it when listing backups. */
    public static final String POOL_NAME = "_pool";

    /** SHA-256 of a byte array as lowercase hex. */
    public static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return hex(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e); // never on a standard JRE
        }
    }

    /** Streaming SHA-256 of a file (doesn't load the whole file into memory). */
    public static String sha256(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[1 << 16];
            try (var in = Files.newInputStream(file)) {
                int n;
                while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            }
            return hex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** The pool path a given hash lives at (may or may not exist). */
    public static Path blob(String sha) {
        return POOL_DIR.resolve(sha);
    }

    /** True if a blob for {@code sha} is already stored. */
    public static boolean has(String sha) {
        return Files.isRegularFile(blob(sha));
    }

    /**
     * Hash {@code file} and, if its bytes aren't stored yet, copy them into the pool (atomically).
     * Returns the sha. Existing blobs are left untouched — identical content is stored only once.
     */
    public static String put(Path file) throws IOException {
        String sha = sha256(file);
        Path dest = blob(sha);
        if (Files.isRegularFile(dest)) return sha; // already pooled — dedup hit
        Files.createDirectories(POOL_DIR);
        Path tmp = POOL_DIR.resolve(sha + ".tmp");
        Files.copy(file, tmp, StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.move(tmp, dest, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }
        return sha;
    }

    /** Copy a pooled blob back out to {@code dest} (parent dirs created, existing file replaced). */
    public static void rehydrate(String sha, Path dest) throws IOException {
        Path src = blob(sha);
        if (!Files.isRegularFile(src)) throw new IOException("Pool is missing blob " + sha + " for " + dest);
        if (dest.getParent() != null) Files.createDirectories(dest.getParent());
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    /** On-disk byte size of a blob, or 0 if absent/unreadable. */
    public static long blobSize(String sha) {
        try { return Files.size(blob(sha)); } catch (IOException e) { return 0L; }
    }

    /**
     * Delete every pool blob NOT in {@code referenced} (orphans left after a backup was deleted). Returns
     * how many blobs were removed. Never touches a referenced blob, so surviving backups stay whole.
     */
    public static synchronized int gc(Set<String> referenced) {
        if (!Files.isDirectory(POOL_DIR)) return 0;
        int removed = 0;
        try (Stream<Path> s = Files.list(POOL_DIR)) {
            for (Path p : s.filter(Files::isRegularFile).toList()) {
                String name = p.getFileName().toString();
                if (name.endsWith(".tmp")) { try { Files.deleteIfExists(p); } catch (IOException ignored) {} continue; }
                if (!referenced.contains(name)) {
                    try { if (Files.deleteIfExists(p)) removed++; } catch (IOException ignored) {}
                }
            }
        } catch (IOException e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Backup pool GC failed to scan _pool", e);
        }
        return removed;
    }

    /** Total bytes currently held in the pool (deduped on-disk footprint of all backups' file content). */
    public static long totalSize() {
        if (!Files.isDirectory(POOL_DIR)) return 0L;
        try (Stream<Path> walk = Files.walk(POOL_DIR)) {
            return walk.filter(Files::isRegularFile).mapToLong(p -> {
                try { return Files.size(p); } catch (IOException e) { return 0L; }
            }).sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(Character.forDigit((x >> 4) & 0xF, 16)).append(Character.forDigit(x & 0xF, 16));
        return sb.toString();
    }

    // (reserved) recursive-delete helper kept close to the pool if a future op needs it.
    @SuppressWarnings("unused")
    private static void deleteRecursively(Path p) {
        if (!Files.exists(p)) return;
        try (Stream<Path> walk = Files.walk(p)) {
            walk.sorted(Comparator.reverseOrder()).forEach(x -> {
                try { Files.deleteIfExists(x); } catch (IOException ignored) {}
            });
        } catch (IOException ignored) {}
    }
}
