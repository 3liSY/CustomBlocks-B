/**
 * TrashManager.java — Group 09, Slice 4 (deleted-block trash store).
 *
 * When a custom block is deleted, {@link SlotManager#delete} hands its snapshot here and we keep a
 * copy under config/customblocks/trash/&lt;entryId&gt;/ so it can be browsed and restored later:
 *   • entry.json   — the block's fields (id, name, glow, hardness, sound, collision, category, shape)
 *                    plus the deleted timestamp and a "pinned" flag;
 *   • texture.png  — the baked texture bytes (if any);
 *   • source.png   — the original source image (if any), so a restored block can be re-rendered.
 *
 * RELIABILITY (mirrors BackupManager): each entry is built in a &lt;entryId&gt;.tmp dir then atomically
 * renamed, so a crash mid-write can only leave a stray .tmp (ignored by list()). Capture is BEST-EFFORT
 * and never throws — a failed trash write must not break the delete it is shadowing. This class only
 * reads live data; it never writes the live slot files.
 *
 * Pruning is LAZY: list() drops unpinned entries older than {@code trashRetentionDays} (0 = keep
 * forever). Pinned entries are never auto-pruned. Restore itself is orchestrated by TrashCommands
 * (it recreates the block via the tested SlotManager.create + setters, then rebuilds the pack).
 *
 * Depends on: CustomBlocksConfig, SlotData, IncidentRecorder, CustomBlocksMod (LOGGER), Gson.
 * Called by:  SlotManager (capture), TrashCommands / TrashMenu (list / get / pin / purge / bytes).
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksConfig;
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

public final class TrashManager {

    private TrashManager() {} // static-only

    public static final Path TRASH_DIR = Path.of("config/customblocks/trash");

    private static final Pattern UNSAFE = Pattern.compile("[^A-Za-z0-9_-]");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter HUMAN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String ENTRY = "entry.json";
    private static final String TEXTURE = "texture.png";
    private static final String SOURCE = "source.png";

    /** One trashed block, as shown in the trash browser and used to restore it. */
    public record TrashEntry(String entryId, String customId, String displayName,
                             int glow, float hardness, String soundType, boolean noCollision,
                             String category, String shape,
                             long deletedEpochMs, String deletedHuman, boolean pinned, boolean hasTexture) {}

    // ── Capture (called from SlotManager.delete; BEST-EFFORT — never throws) ───
    public static synchronized void capture(SlotData d, byte[] texture, byte[] source) {
        if (d == null) return;
        try {
            Files.createDirectories(TRASH_DIR);
            String entryId = uniqueEntryId(d.customId());
            Path tmp = TRASH_DIR.resolve(entryId + ".tmp");
            deleteRecursively(tmp);
            Files.createDirectories(tmp);

            JsonObject o = new JsonObject();
            o.addProperty("entryId", entryId);
            o.addProperty("customId", d.customId());
            o.addProperty("displayName", d.displayName());
            o.addProperty("glow", d.glow());
            o.addProperty("hardness", d.hardness());
            o.addProperty("soundType", d.soundType());
            o.addProperty("noCollision", d.noCollision());
            o.addProperty("category", d.category());
            o.addProperty("shape", d.shape());
            o.addProperty("deleted", System.currentTimeMillis());
            o.addProperty("deletedHuman", LocalDateTime.now().format(HUMAN));
            o.addProperty("pinned", false);
            Files.writeString(tmp.resolve(ENTRY), GSON.toJson(o), StandardCharsets.UTF_8);

            if (texture != null && texture.length > 0) Files.write(tmp.resolve(TEXTURE), texture);
            if (source != null && source.length > 0)  Files.write(tmp.resolve(SOURCE), source);

            Path target = TRASH_DIR.resolve(entryId);
            deleteRecursively(target);
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target);
            }
        } catch (Exception e) {
            // A failed trash capture must NOT break the delete — just log it.
            IncidentRecorder.record("Trash capture failed for \"" + d.customId() + "\"", e);
        }
    }

    /** A filesystem-safe, unique entry id: "&lt;sanitizedId&gt;-&lt;stamp&gt;" (suffixed if it collides). */
    private static String uniqueEntryId(String customId) {
        String safe = UNSAFE.matcher(customId == null ? "block" : customId).replaceAll("_");
        if (safe.isBlank()) safe = "block";
        if (safe.length() > 40) safe = safe.substring(0, 40);
        String base = safe + "-" + LocalDateTime.now().format(STAMP);
        String id = base;
        int n = 2;
        while (Files.exists(TRASH_DIR.resolve(id)) || Files.exists(TRASH_DIR.resolve(id + ".tmp"))) {
            id = base + "-" + n++;
        }
        return id;
    }

    // ── Browse / query ────────────────────────────────────────────────────────
    /** All trashed blocks, newest first, AFTER lazily pruning expired unpinned entries. */
    public static synchronized List<TrashEntry> list() {
        pruneExpired(CustomBlocksConfig.trashRetentionDays);
        List<TrashEntry> out = new ArrayList<>();
        if (!Files.isDirectory(TRASH_DIR)) return out;
        try (Stream<Path> s = Files.list(TRASH_DIR)) {
            s.filter(Files::isDirectory)
             .filter(p -> !p.getFileName().toString().endsWith(".tmp"))
             .forEach(p -> { TrashEntry e = read(p); if (e != null) out.add(e); });
        } catch (IOException e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Failed to list trash", e);
        }
        out.sort(Comparator.comparingLong(TrashEntry::deletedEpochMs).reversed());
        return out;
    }

    public static synchronized TrashEntry get(String entryId) {
        if (!isValidEntryId(entryId)) return null;
        Path dir = TRASH_DIR.resolve(entryId);
        return Files.isDirectory(dir) ? read(dir) : null;
    }

    private static TrashEntry read(Path dir) {
        String entryId = dir.getFileName().toString();
        Path manifest = dir.resolve(ENTRY);
        if (!Files.isRegularFile(manifest)) return null;
        try {
            JsonObject o = GSON.fromJson(Files.readString(manifest, StandardCharsets.UTF_8), JsonObject.class);
            String customId = str(o, "customId", entryId);
            String name = str(o, "displayName", customId);
            int glow = o.has("glow") ? o.get("glow").getAsInt() : 0;
            float hardness = o.has("hardness") ? o.get("hardness").getAsFloat() : SlotData.DEFAULT_HARDNESS;
            String sound = str(o, "soundType", SlotData.DEFAULT_SOUND);
            boolean noCol = o.has("noCollision") && o.get("noCollision").getAsBoolean();
            String cat = str(o, "category", SlotData.DEFAULT_CATEGORY);
            String shape = str(o, "shape", SlotData.DEFAULT_SHAPE);
            long deleted = o.has("deleted") ? o.get("deleted").getAsLong() : 0L;
            String human = str(o, "deletedHuman", "");
            boolean pinned = o.has("pinned") && o.get("pinned").getAsBoolean();
            boolean hasTex = Files.isRegularFile(dir.resolve(TEXTURE));
            return new TrashEntry(entryId, customId, name, glow, hardness, sound, noCol, cat, shape,
                    deleted, human, pinned, hasTex);
        } catch (Exception e) {
            return null;
        }
    }

    /** The trashed block's baked texture bytes, or null. */
    public static synchronized byte[] textureBytes(String entryId) { return bytes(entryId, TEXTURE); }
    /** The trashed block's original source-image bytes, or null. */
    public static synchronized byte[] sourceBytes(String entryId)  { return bytes(entryId, SOURCE); }

    private static byte[] bytes(String entryId, String file) {
        if (!isValidEntryId(entryId)) return null;
        Path p = TRASH_DIR.resolve(entryId).resolve(file);
        if (!Files.isRegularFile(p)) return null;
        try { return Files.readAllBytes(p); } catch (IOException e) { return null; }
    }

    // ── Mutate (pin / purge / prune) ──────────────────────────────────────────
    /** Set the pinned flag (pinned entries are never auto-pruned). Returns false if the entry is gone. */
    public static synchronized boolean setPinned(String entryId, boolean pinned) {
        TrashEntry e = get(entryId);
        if (e == null) return false;
        Path manifest = TRASH_DIR.resolve(entryId).resolve(ENTRY);
        try {
            JsonObject o = GSON.fromJson(Files.readString(manifest, StandardCharsets.UTF_8), JsonObject.class);
            o.addProperty("pinned", pinned);
            Path tmp = TRASH_DIR.resolve(entryId).resolve(ENTRY + ".tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, manifest, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException ex) {
            IncidentRecorder.record("Trash pin update failed for \"" + entryId + "\"", ex);
            return false;
        }
    }

    /** Permanently delete a trash entry. Returns false if it didn't exist. Never touches live data. */
    public static synchronized boolean purge(String entryId) {
        if (!isValidEntryId(entryId)) return false;
        Path dir = TRASH_DIR.resolve(entryId);
        if (!Files.isDirectory(dir)) return false;
        deleteRecursively(dir);
        return true;
    }

    /** Delete unpinned entries older than {@code retentionDays} days. retentionDays ≤ 0 = keep forever. */
    public static synchronized int pruneExpired(int retentionDays) {
        if (retentionDays <= 0) return 0;
        long cutoff = System.currentTimeMillis() - (long) retentionDays * 86_400_000L;
        if (!Files.isDirectory(TRASH_DIR)) return 0;
        List<String> doomed = new ArrayList<>();
        try (Stream<Path> s = Files.list(TRASH_DIR)) {
            s.filter(Files::isDirectory)
             .filter(p -> !p.getFileName().toString().endsWith(".tmp"))
             .forEach(p -> {
                 TrashEntry e = read(p);
                 if (e != null && !e.pinned() && e.deletedEpochMs() > 0 && e.deletedEpochMs() < cutoff) {
                     doomed.add(e.entryId());
                 }
             });
        } catch (IOException e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Failed to scan trash for pruning", e);
            return 0;
        }
        int removed = 0;
        for (String id : doomed) { deleteRecursively(TRASH_DIR.resolve(id)); removed++; }
        if (removed > 0) {
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Pruned {} expired trash entr(ies) (> {} day(s) old).",
                    removed, retentionDays);
        }
        return removed;
    }

    private static boolean isValidEntryId(String entryId) {
        // Folder names we created; reject anything with path separators / traversal.
        return entryId != null && !entryId.isBlank()
                && !entryId.contains("/") && !entryId.contains("\\") && !entryId.contains("..");
    }

    private static String str(JsonObject o, String k, String d) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : d;
    }

    private static void deleteRecursively(Path p) {
        if (!Files.exists(p)) return;
        try (Stream<Path> walk = Files.walk(p)) {
            walk.sorted(Comparator.reverseOrder()).forEach(x -> {
                try { Files.deleteIfExists(x); } catch (IOException ignored) {}
            });
        } catch (IOException ignored) {}
    }
}
