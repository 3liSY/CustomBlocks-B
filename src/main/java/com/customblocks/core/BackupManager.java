/**
 * BackupManager.java — Group 09 backup core (pooled, deduped snapshots).
 *
 * A backup is a folder under config/customblocks/backups/&lt;name&gt;/ holding a manifest.json that lists
 * every captured file by content hash (sha) rather than a verbatim copy. The bytes live once in the
 * shared _pool/ (see {@link BackupPool}); many backups that share an unchanged texture reference the same
 * blob, so a second snapshot after a small edit costs almost nothing. The manifest also records the block
 * count, {@link Kind} (manual / auto / safety), reason, note, total size, and a top-level contents list.
 *
 * RELIABILITY: this class is READ-ONLY with respect to live data — it only reads config/customblocks and
 * writes into backups/. Each backup is built in a sibling &lt;name&gt;.tmp dir and atomically renamed into
 * place, so a crash mid-copy can only leave a stray .tmp (ignored by {@link #list()}), never a half-written
 * named backup. Callers flush SlotManager to disk first.
 *
 * No-monolith split (2026-07-23, §9.3): restore lives in {@link BackupRestore}, retention/prune in
 * {@link BackupRetention}, integrity/boot-guard in {@link BackupIntegrity}, portable ZIP in
 * {@link BackupArchive}, and list presentation in {@link BackupView}. This class owns save, list, delete,
 * naming, and the manifest/pool primitives those helpers build on.
 *
 * Depends on: CustomBlocksMod (LOGGER), CustomBlocksConfig (owner timezone), CbPaths, BackupPool, Gson.
 * Heavy file I/O — callers run save() off the server thread.
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.CustomBlocksMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class BackupManager {

    private BackupManager() {} // static-only

    public static final Path BACKUPS_DIR = Path.of("config/customblocks/backups");

    /** The live data this snapshots (READ-ONLY here — never written by this class). */
    static final Path LIVE_DIR = CbPaths.ROOT;

    static final String MANIFEST = "manifest.json";

    /** Current on-disk manifest version. FORMAT_POOLED (3) means files are stored by sha in _pool/. */
    private static final int FORMAT_VERSION = 3;
    static final int FORMAT_POOLED = 3;

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,48}");

    /** Prefixes reserved for system-generated names so a player can't hand-type a backup that impersonates
     *  an automatic or safety copy (e.g. faking a "pre-restore…" undo point). Applies to USER-typed names
     *  only — names the mod generates itself skip this check (see BackupCommands.validateNewName). */
    private static final String[] RESERVED_PREFIXES = {"manual_", "auto_", "safety_", "auto-", "pre-restore", "pre-"};

    /** Filesystem-safe timestamp for generated names, e.g. "2026-07-23_04-09-44" (in the owner's zone). */
    static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter HUMAN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Infer the kind from a name's prefix (for older backups whose manifest predates the "kind" field). */
    static Kind kindFromName(String name) {
        if (name.startsWith("auto-") || name.startsWith("auto_")) return Kind.AUTO;
        if (name.startsWith("pre-") || name.startsWith("safety_")) return Kind.SAFETY;
        return Kind.MANUAL;
    }

    public static boolean isValidName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    /** True if {@code name} starts with a system-reserved prefix (see {@link #RESERVED_PREFIXES}). */
    public static boolean isReservedName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ENGLISH);
        for (String p : RESERVED_PREFIXES) {
            if (lower.startsWith(p)) return true;
        }
        return false;
    }

    /** A system-generated, sortable name like "auto_2026-07-23_04-09-44" (kind-prefixed, owner-zone stamp). */
    public static String generatedName(Kind kind) {
        return kind.id() + "_" + LocalDateTime.now(CustomBlocksConfig.OWNER_ZONE).format(STAMP);
    }

    /** A system-generated name for a given prefix, e.g. "pre-setall_2026-07-23_04-09-44" (owner-zone stamp). */
    public static String timestampName(String prefix) {
        return prefix + "_" + LocalDateTime.now(CustomBlocksConfig.OWNER_ZONE).format(STAMP);
    }

    public static boolean exists(String name) {
        return isValidName(name) && Files.isDirectory(BACKUPS_DIR.resolve(name));
    }

    /**
     * Snapshot the live data into backups/&lt;name&gt;/. Every regular file under the live tree (minus the
     * excluded dirs — see {@link CbPaths#isExcludedFromBackup}) is pooled by content hash and recorded in
     * the manifest. Build in &lt;name&gt;.tmp, then atomically rename. Call SlotManager.saveAll() on the
     * server thread BEFORE this so slots.json is current. Heavy file I/O — call OFF the server thread.
     */
    public static synchronized void save(String name, int blocks, Kind kind, String reason, String note) throws IOException {
        if (kind == null) kind = Kind.MANUAL;
        if (!isValidName(name)) throw new IOException("Invalid backup name: " + name);
        Path target = BACKUPS_DIR.resolve(name);
        if (Files.exists(target)) throw new IOException("Backup already exists: " + name);
        Files.createDirectories(BACKUPS_DIR);

        Path tmp = BACKUPS_DIR.resolve(name + ".tmp");
        deleteRecursively(tmp); // clear any leftover from a previous failed run
        Files.createDirectories(tmp);
        try {
            List<FileRef> files = new ArrayList<>();
            for (String entry : liveEntries()) {
                Path src = LIVE_DIR.resolve(entry);
                if (Files.isDirectory(src)) {
                    try (Stream<Path> walk = Files.walk(src)) {
                        for (Path f : walk.filter(Files::isRegularFile).toList()) {
                            files.add(poolFile(f));
                        }
                    }
                } else if (Files.isRegularFile(src)) {
                    files.add(poolFile(src));
                }
            }
            writeManifest(tmp, name, blocks, kind, reason, note, files);
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

    public static List<BackupInfo> list() {
        List<BackupInfo> out = new ArrayList<>();
        if (!Files.isDirectory(BACKUPS_DIR)) return out;
        try (Stream<Path> s = Files.list(BACKUPS_DIR)) {
            s.filter(Files::isDirectory)
             .filter(p -> {
                 String n = p.getFileName().toString();
                 return !n.endsWith(".tmp") && !n.equals("_pool");
             })
             .forEach(p -> out.add(read(p)));
        } catch (IOException e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Failed to list backups", e);
        }
        out.sort(Comparator.comparingLong(BackupInfo::createdEpochMs).reversed());
        return out;
    }

    private static BackupInfo read(Path dir) {
        String name = dir.getFileName().toString();
        Path manifest = dir.resolve(MANIFEST);
        if (Files.isRegularFile(manifest)) {
            try {
                JsonObject o = GSON.fromJson(Files.readString(manifest, StandardCharsets.UTF_8), JsonObject.class);
                long created = o.has("created") ? o.get("created").getAsLong() : 0L;
                int blocks = o.has("blocks") ? o.get("blocks").getAsInt() : -1;
                Kind kind = Kind.fromId(o.has("kind") && !o.get("kind").isJsonNull() ? o.get("kind").getAsString() : null);
                if (kind == null) {
                    kind = kindFromName(name);
                    if (kind == Kind.MANUAL && o.has("auto") && o.get("auto").getAsBoolean()) kind = Kind.AUTO;
                }
                String reason = o.has("reason") && !o.get("reason").isJsonNull() ? o.get("reason").getAsString() : "";
                String note = o.has("note") && !o.get("note").isJsonNull() ? o.get("note").getAsString() : "";
                boolean prot = o.has("protected") && o.get("protected").getAsBoolean();
                String when = o.has("createdHuman") ? o.get("createdHuman").getAsString() : "";
                long size = o.has("totalSize") && !o.get("totalSize").isJsonNull() ? o.get("totalSize").getAsLong() : folderSize(dir);
                return new BackupInfo(name, created, when, blocks, kind, reason, note, prot, size);
            } catch (Exception ignored) {
                // fall through to the mtime-based fallback below
            }
        }
        long mtime = 0L;
        try {
            mtime = Files.getLastModifiedTime(dir).toMillis();
        } catch (IOException ignored) {
        }
        return new BackupInfo(name, mtime, "", -1, kindFromName(name), "", "", false, folderSize(dir));
    }

    /** True if the backup exists and its slots.json is present and parses (a cheap health gate). */
    public static boolean isValidBackup(String name) {
        if (!exists(name)) return false;
        Path dir = BACKUPS_DIR.resolve(name);
        try {
            JsonObject m = readManifest(dir);
            byte[] slots;
            if (m != null && formatOf(m) >= FORMAT_POOLED) {
                String sha = null;
                for (FileRef fr : fileRefs(m)) {
                    if (fr.path().equals("slots.json")) { sha = fr.sha(); break; }
                }
                if (sha == null || !BackupPool.has(sha)) return false;
                slots = Files.readAllBytes(BackupPool.blob(sha));
            } else {
                Path p = dir.resolve("slots.json");
                if (!Files.isRegularFile(p)) return false;
                slots = Files.readAllBytes(p);
            }
            GSON.fromJson(new String(slots, StandardCharsets.UTF_8), JsonObject.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String latestName() {
        List<BackupInfo> all = list();
        return all.isEmpty() ? null : all.get(0).name();
    }

    public static synchronized boolean delete(String name) {
        if (!deleteFolder(name)) return false;
        gcPool(); // drop pool blobs no surviving backup references
        return true;
    }

    static synchronized boolean deleteFolder(String name) {
        if (!exists(name)) return false;
        deleteRecursively(BACKUPS_DIR.resolve(name));
        return true;
    }

    static synchronized void gcPool() {
        BackupPool.gc(allReferencedShas());
    }

    /** Top-level live entries to snapshot (excludes backups/, updates/, the pack zip — see CbPaths). */
    static List<String> liveEntries() throws IOException {
        if (!Files.isDirectory(LIVE_DIR)) return new ArrayList<>();
        try (Stream<Path> s = Files.list(LIVE_DIR)) {
            List<String> out = new ArrayList<>();
            s.map(p -> p.getFileName().toString())
             .filter(n -> !CbPaths.isExcludedFromBackup(n))
             .forEach(out::add);
            out.sort(String::compareTo);
            return out;
        }
    }

    /** Distinct top-level names a backup captured (pooled: derived from file paths; legacy: the folder list). */
    static List<String> topLevelEntries(Path backupDir) throws IOException {
        JsonObject m = readManifest(backupDir);
        if (m != null && formatOf(m) >= FORMAT_POOLED) {
            TreeSet<String> tops = new TreeSet<>();
            for (FileRef fr : fileRefs(m)) {
                int slash = fr.path().indexOf('/');
                tops.add(slash < 0 ? fr.path() : fr.path().substring(0, slash));
            }
            return new ArrayList<>(tops);
        }
        return backupDataEntries(backupDir);
    }

    /** Legacy (pre-pool) data entries: the files actually sitting in the backup folder, minus the manifest. */
    static List<String> backupDataEntries(Path backupDir) throws IOException {
        try (Stream<Path> s = Files.list(backupDir)) {
            List<String> out = new ArrayList<>();
            s.map(p -> p.getFileName().toString())
             .filter(n -> !n.equals(MANIFEST) && !n.endsWith(".tmp"))
             .forEach(out::add);
            out.sort(String::compareTo);
            return out;
        }
    }

    static void moveIfExists(Path src, Path dest) throws IOException {
        if (!Files.exists(src)) return;
        if (dest.getParent() != null) Files.createDirectories(dest.getParent());
        try {
            Files.move(src, dest, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(src, dest);
        }
    }

    /** Put a live file's bytes into the shared pool and return its manifest reference (relative path, sha, size). */
    static FileRef poolFile(Path liveFile) throws IOException {
        String rel = LIVE_DIR.relativize(liveFile).toString().replace('\\', '/');
        String sha = BackupPool.put(liveFile);
        long size = Files.size(liveFile);
        return new FileRef(rel, sha, size);
    }

    static void writeManifest(Path dir, String name, int blocks, Kind kind, String reason, String note,
                              List<FileRef> files) throws IOException {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("created", System.currentTimeMillis());
        o.addProperty("createdHuman", LocalDateTime.now(CustomBlocksConfig.OWNER_ZONE).format(HUMAN));
        o.addProperty("blocks", blocks);
        o.addProperty("kind", (kind == null ? Kind.MANUAL : kind).id());
        o.addProperty("auto", kind == Kind.AUTO);
        o.addProperty("reason", reason == null ? "" : reason);
        o.addProperty("note", note == null ? "" : note);
        o.addProperty("formatVersion", FORMAT_VERSION);
        o.addProperty("protected", false);

        JsonArray fileArr = new JsonArray();
        LinkedHashSet<String> topLevel = new LinkedHashSet<>();
        long total = 0L;
        if (files != null) {
            for (FileRef fr : files) {
                JsonObject f = new JsonObject();
                f.addProperty("path", fr.path());
                f.addProperty("sha", fr.sha());
                f.addProperty("size", fr.size());
                fileArr.add(f);
                total += fr.size();
                int slash = fr.path().indexOf('/');
                topLevel.add(slash < 0 ? fr.path() : fr.path().substring(0, slash));
            }
        }
        o.add("files", fileArr);
        o.addProperty("totalSize", total);

        JsonArray items = new JsonArray();
        for (String c : topLevel) items.add(c);
        o.add("contents", items);

        Files.writeString(dir.resolve(MANIFEST), GSON.toJson(o), StandardCharsets.UTF_8);
    }

    /** Write an older (format 2, non-pooled) manifest — kept for tools that still emit the flat layout. */
    static void writeLegacyManifest(Path dir, String name, int blocks, Kind kind, String reason,
                                    List<String> contents) throws IOException {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("created", System.currentTimeMillis());
        o.addProperty("createdHuman", LocalDateTime.now(CustomBlocksConfig.OWNER_ZONE).format(HUMAN));
        o.addProperty("blocks", blocks);
        o.addProperty("kind", (kind == null ? Kind.MANUAL : kind).id());
        o.addProperty("auto", kind == Kind.AUTO);
        o.addProperty("reason", reason == null ? "" : reason);
        o.addProperty("note", "");
        o.addProperty("formatVersion", 2);
        o.addProperty("protected", false);
        JsonArray items = new JsonArray();
        if (contents != null) {
            for (String c : contents) items.add(c);
        }
        o.add("contents", items);
        Files.writeString(dir.resolve(MANIFEST), GSON.toJson(o), StandardCharsets.UTF_8);
    }

    static JsonObject readManifest(Path dir) {
        Path manifest = dir.resolve(MANIFEST);
        if (!Files.isRegularFile(manifest)) return null;
        try {
            return GSON.fromJson(Files.readString(manifest, StandardCharsets.UTF_8), JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }

    static int formatOf(JsonObject m) {
        return (m != null && m.has("formatVersion") && !m.get("formatVersion").isJsonNull())
                ? m.get("formatVersion").getAsInt() : 1;
    }

    static List<FileRef> fileRefs(JsonObject m) {
        List<FileRef> out = new ArrayList<>();
        if (m == null || !m.has("files") || !m.get("files").isJsonArray()) return out;
        for (JsonElement el : m.getAsJsonArray("files")) {
            if (!el.isJsonObject()) continue;
            JsonObject f = el.getAsJsonObject();
            String path = f.has("path") ? f.get("path").getAsString() : null;
            String sha = f.has("sha") ? f.get("sha").getAsString() : null;
            long size = f.has("size") ? f.get("size").getAsLong() : 0L;
            if (path != null && sha != null) out.add(new FileRef(path, sha, size));
        }
        return out;
    }

    /** Every pool sha referenced by any surviving backup — the keep-set for {@link #gcPool()}. */
    private static Set<String> allReferencedShas() {
        Set<String> refs = new HashSet<>();
        if (!Files.isDirectory(BACKUPS_DIR)) return refs;
        try (Stream<Path> s = Files.list(BACKUPS_DIR)) {
            for (Path dir : s.filter(Files::isDirectory).toList()) {
                String n = dir.getFileName().toString();
                if (n.equals("_pool") || n.endsWith(".tmp")) continue;
                JsonObject m = readManifest(dir);
                if (m == null) continue;
                for (FileRef fr : fileRefs(m)) refs.add(fr.sha());
            }
        } catch (IOException e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Failed to scan backups for pool references", e);
        }
        return refs;
    }

    static long folderSize(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile).mapToLong(p -> {
                try {
                    return Files.size(p);
                } catch (IOException e) {
                    return 0L;
                }
            }).sum();
        } catch (IOException e) {
            return -1L;
        }
    }

    static void copyDir(Path src, Path dest) throws IOException {
        Files.createDirectories(dest);
        try (Stream<Path> walk = Files.walk(src)) {
            for (Path p : walk.toList()) {
                Path d = dest.resolve(src.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(d);
                } else {
                    Files.copy(p, d, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    static void deleteRecursively(Path p) {
        if (!Files.exists(p)) return;
        try (Stream<Path> walk = Files.walk(p)) {
            walk.sorted(Comparator.reverseOrder()).forEach(x -> {
                try {
                    Files.deleteIfExists(x);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    /** What made a backup: a player's manual save, the scheduler, or a pre-op safety copy. */
    public enum Kind {
        MANUAL, AUTO, SAFETY;

        public String id() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        public static Kind fromId(String s) {
            if (s != null) {
                for (Kind k : values()) {
                    if (k.id().equals(s)) return k;
                }
            }
            return null;
        }
    }

    /** One file inside a backup: its path relative to the live root, its content hash, and its byte size. */
    record FileRef(String path, String sha, long size) {}

    /**
     * One backup's metadata as shown by /cb backup list and the Backup Screen. blocks == -1 means
     * "unknown". {@code protectedFromPrune} backups are kept even past the keep count. {@code sizeBytes}
     * is the pooled total (-1 if it couldn't be measured).
     */
    public record BackupInfo(String name, long createdEpochMs, String created, int blocks, Kind kind,
                             String reason, String note, boolean protectedFromPrune, long sizeBytes) {
        public boolean auto() {
            return kind == Kind.AUTO;
        }
    }
}
