/**
 * BackupManager.java — Group 09, Slice 1 (backup core: save + list).
 *
 * A point-in-time backup is a folder under config/customblocks/backups/<name>/ holding a verbatim
 * copy of the live data: slots.json, config.json, the textures/ dir and the sources/ dir, plus a
 * manifest.json (timestamp + block count). Snapshots are path-agnostic — they copy whatever exists
 * now and (later, Slice 2) restore it exactly — so this slice does NOT depend on the risky data-path
 * normalization (Slice 6).
 *
 * RELIABILITY: this class is READ-ONLY with respect to live data — it never writes into the live
 * config/customblocks files, only reads them. Each backup is built in a sibling <name>.tmp dir and
 * then atomically renamed into place, so a crash mid-copy can only ever leave a stray .tmp (ignored
 * by list()), never a half-written named backup. The caller flushes SlotManager to disk first.
 *
 * Depends on: CustomBlocksMod (LOGGER), Gson. Heavy file I/O — callers run save() off the server thread.
 * Called by:  BackupCommands (save/list). Restore/panic/delete arrive in Slice 2.
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class BackupManager {

    private BackupManager() {} // static-only

    public static final Path BACKUPS_DIR = Path.of("config/customblocks/backups");

    /** The live data this snapshots (READ-ONLY here — never written by this class). */
    private static final Path   LIVE_DIR   = Path.of("config/customblocks");
    private static final String[] LIVE_FILES = {"slots.json", "config.json"};
    private static final String[] LIVE_DIRS  = {"textures", "sources"};

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,48}");
    private static final DateTimeFormatter STAMP  = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter HUMAN  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** One backup's metadata as shown by /cb backup list. blocks == -1 means "unknown". */
    public record BackupInfo(String name, long createdEpochMs, String created, int blocks, boolean auto) {}

    public static boolean isValidName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    /** An auto-generated, filesystem-safe name like "backup-20260613-191500". */
    public static String timestampName(String prefix) {
        return prefix + "-" + LocalDateTime.now().format(STAMP);
    }

    public static boolean exists(String name) {
        return isValidName(name) && Files.isDirectory(BACKUPS_DIR.resolve(name));
    }

    /**
     * Snapshot the live data into backups/&lt;name&gt;/. Build in &lt;name&gt;.tmp, then atomically
     * rename. Call SlotManager.saveAll() on the server thread BEFORE this so slots.json is current.
     * Heavy file I/O — call OFF the server thread.
     */
    public static synchronized void save(String name, int blocks, boolean auto) throws IOException {
        if (!isValidName(name)) throw new IOException("Invalid backup name: " + name);
        Path target = BACKUPS_DIR.resolve(name);
        if (Files.exists(target)) throw new IOException("Backup already exists: " + name);
        Files.createDirectories(BACKUPS_DIR);

        Path tmp = BACKUPS_DIR.resolve(name + ".tmp");
        deleteRecursively(tmp); // clear any leftover from a previous failed run
        Files.createDirectories(tmp);
        try {
            for (String f : LIVE_FILES) {
                Path src = LIVE_DIR.resolve(f);
                if (Files.isRegularFile(src)) {
                    Files.copy(src, tmp.resolve(f), StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
            for (String d : LIVE_DIRS) {
                Path src = LIVE_DIR.resolve(d);
                if (Files.isDirectory(src)) copyDir(src, tmp.resolve(d));
            }
            writeManifest(tmp, name, blocks, auto);
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target); // same-volume rename still leaves no half-written named dir
            }
        } catch (IOException e) {
            deleteRecursively(tmp); // never leave junk behind
            throw e;
        }
    }

    /** All backups, newest first. Skips *.tmp (in-progress/failed) dirs. */
    public static List<BackupInfo> list() {
        List<BackupInfo> out = new ArrayList<>();
        if (!Files.isDirectory(BACKUPS_DIR)) return out;
        try (Stream<Path> s = Files.list(BACKUPS_DIR)) {
            s.filter(Files::isDirectory)
             .filter(p -> !p.getFileName().toString().endsWith(".tmp"))
             .forEach(p -> out.add(read(p)));
        } catch (IOException e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Failed to list backups", e);
        }
        out.sort(Comparator.comparingLong(BackupInfo::createdEpochMs).reversed());
        return out;
    }

    private static BackupInfo read(Path dir) {
        String name = dir.getFileName().toString();
        Path manifest = dir.resolve("manifest.json");
        if (Files.isRegularFile(manifest)) {
            try {
                JsonObject o = GSON.fromJson(Files.readString(manifest, StandardCharsets.UTF_8), JsonObject.class);
                long created = o.has("created") ? o.get("created").getAsLong() : 0L;
                int blocks   = o.has("blocks") ? o.get("blocks").getAsInt() : -1;
                boolean auto = o.has("auto") && o.get("auto").getAsBoolean();
                String when  = o.has("createdHuman") ? o.get("createdHuman").getAsString() : "";
                return new BackupInfo(name, created, when, blocks, auto);
            } catch (Exception ignored) { /* fall through to folder-derived */ }
        }
        long mtime = 0L;
        try { mtime = Files.getLastModifiedTime(dir).toMillis(); } catch (IOException ignored) {}
        return new BackupInfo(name, mtime, "", -1, name.startsWith("auto-"));
    }

    /** True if {@code name} is a usable backup: the folder exists and its slots.json parses. */
    public static boolean isValidBackup(String name) {
        if (!exists(name)) return false;
        Path slots = BACKUPS_DIR.resolve(name).resolve("slots.json");
        if (!Files.isRegularFile(slots)) return false;
        try {
            GSON.fromJson(Files.readString(slots, StandardCharsets.UTF_8), JsonObject.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Name of the newest backup, or null if there are none. */
    public static String latestName() {
        List<BackupInfo> all = list();
        return all.isEmpty() ? null : all.get(0).name();
    }

    /** Delete a backup folder. Returns false if it didn't exist. Never touches live data. */
    public static synchronized boolean delete(String name) {
        if (!exists(name)) return false;
        deleteRecursively(BACKUPS_DIR.resolve(name));
        return true;
    }

    /**
     * Keep the {@code keep} newest auto-backups (folders named "auto-…") and delete the rest. Only
     * "auto-" backups are touched — manual saves and "pre-restore-…" safety copies are never pruned.
     * Returns how many were removed. Called after each auto-backup (Slice 3).
     */
    public static synchronized int pruneAuto(int keep) {
        int k = Math.max(0, keep);
        List<BackupInfo> autos = new ArrayList<>();
        for (BackupInfo b : list()) { // list() is already newest-first
            if (b.name().startsWith("auto-")) autos.add(b);
        }
        int removed = 0;
        for (int i = k; i < autos.size(); i++) {
            if (delete(autos.get(i).name())) removed++;
        }
        return removed;
    }

    /**
     * Restore live data from backups/&lt;name&gt;/ with a SAFE SWAP:
     *   1. verify the chosen backup parses (else abort, live untouched);
     *   2. MOVE the current live files into a fresh "pre-restore-&lt;stamp&gt;" backup — a fast rename
     *      that both clears the live slots AND leaves the old state as a recoverable snapshot;
     *   3. COPY the chosen backup's files into the live location.
     * Returns the safety backup's name. On a copy failure it best-effort rolls the safety copy back
     * into place, then rethrows. The CALLER must pause the pack first and, afterwards, reload config +
     * SlotManager and rebuild the pack. Run on the server thread (no concurrent edits).
     */
    public static synchronized String restore(String name, int currentBlocks) throws IOException {
        if (!isValidBackup(name)) throw new IOException("Backup \"" + name + "\" is missing or unreadable.");
        Path backupDir = BACKUPS_DIR.resolve(name);

        String safety = timestampName("pre-restore");
        Path safetyDir = BACKUPS_DIR.resolve(safety);
        Files.createDirectories(safetyDir);
        for (String f : LIVE_FILES) moveIfExists(LIVE_DIR.resolve(f), safetyDir.resolve(f));
        for (String d : LIVE_DIRS)  moveIfExists(LIVE_DIR.resolve(d), safetyDir.resolve(d));
        writeManifest(safetyDir, safety, currentBlocks, true);

        try {
            for (String f : LIVE_FILES) {
                Path src = backupDir.resolve(f);
                if (Files.isRegularFile(src)) {
                    Files.copy(src, LIVE_DIR.resolve(f),
                            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
            for (String d : LIVE_DIRS) {
                Path src = backupDir.resolve(d);
                if (Files.isDirectory(src)) copyDir(src, LIVE_DIR.resolve(d));
            }
        } catch (IOException e) {
            rollback(safetyDir); // put the old state back so a failed restore can't lose data
            throw e;
        }
        return safety;
    }

    /** Move src→dest if src exists (atomic rename where supported). */
    private static void moveIfExists(Path src, Path dest) throws IOException {
        if (!Files.exists(src)) return;
        if (dest.getParent() != null) Files.createDirectories(dest.getParent());
        try {
            Files.move(src, dest, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(src, dest);
        }
    }

    /** Best-effort: move the safety copy's items back into the live location after a failed restore. */
    private static void rollback(Path safetyDir) {
        try {
            for (String f : LIVE_FILES) {
                Path s = safetyDir.resolve(f);
                if (Files.isRegularFile(s)) { Files.deleteIfExists(LIVE_DIR.resolve(f)); moveIfExists(s, LIVE_DIR.resolve(f)); }
            }
            for (String d : LIVE_DIRS) {
                Path s = safetyDir.resolve(d);
                if (Files.isDirectory(s)) { deleteRecursively(LIVE_DIR.resolve(d)); moveIfExists(s, LIVE_DIR.resolve(d)); }
            }
        } catch (IOException ignored) { /* live may be partial; the safety backup folder still holds it */ }
    }

    private static void writeManifest(Path dir, String name, int blocks, boolean auto) throws IOException {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("created", System.currentTimeMillis());
        o.addProperty("createdHuman", LocalDateTime.now().format(HUMAN));
        o.addProperty("blocks", blocks);
        o.addProperty("auto", auto);
        Files.writeString(dir.resolve("manifest.json"), GSON.toJson(o), StandardCharsets.UTF_8);
    }

    /** Recursively copy src/* into dest (dest created if absent). */
    private static void copyDir(Path src, Path dest) throws IOException {
        Files.createDirectories(dest);
        try (Stream<Path> walk = Files.walk(src)) {
            for (Path p : walk.toList()) {
                Path d = dest.resolve(src.relativize(p).toString());
                if (Files.isDirectory(p)) Files.createDirectories(d);
                else Files.copy(p, d, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /** Best-effort recursive delete (used only on our own backups/*.tmp scratch dirs). */
    private static void deleteRecursively(Path p) {
        if (!Files.exists(p)) return;
        try (Stream<Path> walk = Files.walk(p)) {
            walk.sorted(Comparator.reverseOrder()).forEach(x -> {
                try { Files.deleteIfExists(x); } catch (IOException ignored) {}
            });
        } catch (IOException ignored) {}
    }
}
