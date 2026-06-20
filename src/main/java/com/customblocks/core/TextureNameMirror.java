/**
 * TextureNameMirror.java
 *
 * Responsibility: The SOLE owner of config/customblocks/textures_names/ — an optional, human-readable,
 * WRITE-ONLY copy of each block's texture named by the block's display name (e.g. "Neptune Red.png")
 * instead of slot_N.png. The mod writes this folder; it NEVER reads it back. slot_N.png stays the one
 * true texture, keyed by slot index, so block identity, models, the resource pack, and every placed
 * block are completely untouched. If this folder breaks or is deleted, blocks are unaffected and no
 * pack rebuild happens (Group 26 Part C).
 *
 * Best-effort: every public method is gated on CustomBlocksConfig.mirrorNamedTextures and swallows its
 * own errors, so a mirror failure can NEVER break a block create, retexture, paint, rename, or delete.
 * All hooks fire from the single texture/identity writers (TextureStore, SlotManager), so no edit path
 * can slip past the mirror (ADR-004).
 *
 * A manifest (config/customblocks/data/mirror_index.json, slot -> filenames it owns) lets renames and
 * deletes clean up the OLD files deterministically, so the folder never accumulates orphans.
 *
 * Depends on: CustomBlocksConfig, SlotManager, SlotData, TextureStore, CustomBlocksMod (LOGGER)
 * Called by:  TextureStore (save / saveFace / deleteFace / delete), SlotManager (rename / restore),
 *             MirrorCommands (on / off / rebuild / status).
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.CustomBlocksMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class TextureNameMirror {

    private static final String DIR = "config/customblocks/textures_names";
    private static final String INDEX_DIR = "config/customblocks/data";
    private static final String INDEX_FILE = "mirror_index.json";

    /** One coarse lock — all folder + manifest mutations serialise (texture saves can be off-thread). */
    private static final Object LOCK = new Object();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private TextureNameMirror() {} // static-only

    private static boolean enabled() { return CustomBlocksConfig.mirrorNamedTextures; }

    // ── Hooks (flag-gated, best-effort) ───────────────────────────────────────

    /** (Re)write the named PNG(s) for one slot from its current name + bytes. No-op when the flag is off. */
    public static void syncSlot(int index) {
        if (!enabled()) return;
        synchronized (LOCK) {
            try { doSyncSlot(index); }
            catch (Exception e) { swallow("sync slot " + index, e); }
        }
    }

    /** Delete the named PNG(s) a slot owned. No-op when the flag is off. */
    public static void removeSlot(int index) {
        if (!enabled()) return;
        synchronized (LOCK) {
            try { doRemoveSlot(index); }
            catch (Exception e) { swallow("remove slot " + index, e); }
        }
    }

    /**
     * Wipe the folder and regenerate every named PNG from the live blocks (the one source of truth).
     * Used by `/cb config mirrornames on` (backfill) and `rebuild`; runs regardless of the flag so the
     * `on` command can build the folder right after flipping it. Returns the number of files written.
     */
    public static int rebuildAll() {
        synchronized (LOCK) {
            try { return doRebuildAll(); }
            catch (Exception e) { swallow("rebuild", e); return 0; }
        }
    }

    // ── Status helpers (read-only, for MirrorCommands) ────────────────────────

    /** How many PNG files currently sit in the mirror folder. */
    public static int fileCount() {
        Path dir = Path.of(DIR);
        if (!Files.isDirectory(dir)) return 0;
        try (Stream<Path> s = Files.list(dir)) {
            return (int) s.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")).count();
        } catch (Exception e) {
            return 0;
        }
    }

    /** Absolute path of the mirror folder (for the status message). */
    public static String folderPath() {
        return Path.of(DIR).toAbsolutePath().toString();
    }

    // ── Internals (all run under LOCK) ────────────────────────────────────────

    private static void doSyncSlot(int index) throws Exception {
        SlotData d = SlotManager.getBySlot("slot_" + index);
        Map<Integer, List<String>> manifest = loadManifest();
        deleteOwned(manifest.get(index)); // drop this slot's old files first (covers rename / retexture)

        // A block with no SlotData or no base texture has nothing to mirror — leave it out.
        if (d == null || TextureStore.load(index) == null) {
            manifest.remove(index);
            saveManifest(manifest);
            return;
        }
        String fileBase = uniqueBaseForSingle(index, d);
        List<String> written = writeFilesFor(index, fileBase);
        if (written.isEmpty()) manifest.remove(index);
        else manifest.put(index, written);
        saveManifest(manifest);
    }

    private static void doRemoveSlot(int index) throws Exception {
        Map<Integer, List<String>> manifest = loadManifest();
        deleteOwned(manifest.get(index));
        manifest.remove(index);
        saveManifest(manifest);
    }

    private static int doRebuildAll() throws Exception {
        wipeFolder();
        Path dir = Path.of(DIR);
        Files.createDirectories(dir);

        List<SlotData> slots = new ArrayList<>(SlotManager.assignedSlots());
        slots.sort(Comparator.comparingInt(SlotData::index));

        Map<Integer, List<String>> manifest = new TreeMap<>();
        Set<String> usedBaseNames = new HashSet<>();
        int files = 0;
        for (SlotData d : slots) {
            int index = d.index();
            if (TextureStore.load(index) == null) continue; // no texture → nothing to browse
            String sane = sanitize(d.displayName(), index);
            // Lowest slot index keeps the bare name; later collisions get a "(slot N)" suffix.
            String fileBase = usedBaseNames.contains(sane) ? sane + " (slot " + index + ")" : sane;
            usedBaseNames.add(sane);
            List<String> written = writeFilesFor(index, fileBase);
            if (!written.isEmpty()) {
                manifest.put(index, written);
                files += written.size();
            }
        }
        saveManifest(manifest);
        return files;
    }

    /** Write the base PNG (+ any face overrides) for a slot under {@code fileBase}. Returns filenames written. */
    private static List<String> writeFilesFor(int index, String fileBase) throws Exception {
        List<String> written = new ArrayList<>();
        byte[] base = TextureStore.load(index);
        if (base == null) return written;
        Path dir = Path.of(DIR);
        Files.createDirectories(dir);

        String baseFile = fileBase + ".png";
        writeAtomic(dir.resolve(baseFile), base);
        written.add(baseFile);

        for (String face : TextureStore.FACES) {
            byte[] faceBytes = TextureStore.loadFace(index, face);
            if (faceBytes != null) {
                String faceFile = fileBase + " (" + face + ").png";
                writeAtomic(dir.resolve(faceFile), faceBytes);
                written.add(faceFile);
            }
        }
        return written;
    }

    /** For a single sync: the sanitized name, with a "(slot N)" suffix if a LOWER-index slot shares it. */
    private static String uniqueBaseForSingle(int index, SlotData d) {
        String sane = sanitize(d.displayName(), index);
        for (SlotData other : SlotManager.assignedSlots()) {
            if (other.index() < index && sanitize(other.displayName(), other.index()).equals(sane)) {
                return sane + " (slot " + index + ")";
            }
        }
        return sane;
    }

    private static void deleteOwned(List<String> filenames) {
        if (filenames == null) return;
        Path dir = Path.of(DIR);
        for (String name : filenames) {
            try { Files.deleteIfExists(dir.resolve(name)); }
            catch (Exception ignored) { /* best-effort */ }
        }
    }

    private static void wipeFolder() {
        Path dir = Path.of(DIR);
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
             .forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
        } catch (Exception ignored) { /* best-effort */ }
    }

    // ── Filename safety ───────────────────────────────────────────────────────

    /** Map a display name to a Windows-safe file base. Empty/invalid → "slot_N" so it never collides. */
    private static String sanitize(String name, int index) {
        if (name == null) return "slot_" + index;
        StringBuilder b = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            // Forbidden on Windows: < > : " / \ | ? *  and control chars.
            if (c < 0x20 || c == '<' || c == '>' || c == ':' || c == '"'
                    || c == '/' || c == '\\' || c == '|' || c == '?' || c == '*') {
                b.append('_');
            } else {
                b.append(c);
            }
        }
        // Windows strips trailing spaces/dots from file names — trim them so the name is stable.
        String out = b.toString().strip();
        while (out.endsWith(".")) out = out.substring(0, out.length() - 1).strip();
        return out.isEmpty() ? "slot_" + index : out;
    }

    // ── Manifest IO (config/customblocks/data/mirror_index.json) ───────────────

    private static Map<Integer, List<String>> loadManifest() {
        Map<Integer, List<String>> map = new TreeMap<>();
        Path file = Path.of(INDEX_DIR, INDEX_FILE);
        if (!Files.exists(file)) return map;
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                int idx;
                try { idx = Integer.parseInt(e.getKey()); }
                catch (NumberFormatException nfe) { continue; }
                List<String> names = new ArrayList<>();
                for (JsonElement el : e.getValue().getAsJsonArray()) names.add(el.getAsString());
                map.put(idx, names);
            }
        } catch (Exception e) {
            swallow("read manifest", e); // start clean; rebuild fixes any drift
        }
        return map;
    }

    private static void saveManifest(Map<Integer, List<String>> map) {
        Path dir = Path.of(INDEX_DIR);
        try {
            Files.createDirectories(dir);
            JsonObject root = new JsonObject();
            for (Map.Entry<Integer, List<String>> e : map.entrySet()) {
                JsonArray arr = new JsonArray();
                for (String name : e.getValue()) arr.add(name);
                root.add(String.valueOf(e.getKey()), arr);
            }
            writeAtomic(dir.resolve(INDEX_FILE), GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            swallow("save manifest", e);
        }
    }

    // ── Shared file primitives ────────────────────────────────────────────────

    private static void writeAtomic(Path file, byte[] bytes) throws Exception {
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        Files.write(tmp, bytes);
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void swallow(String what, Exception e) {
        CustomBlocksMod.LOGGER.warn("[CustomBlocks] Named-texture mirror: {} failed (ignored, blocks unaffected): {}",
                what, e.toString());
    }
}
