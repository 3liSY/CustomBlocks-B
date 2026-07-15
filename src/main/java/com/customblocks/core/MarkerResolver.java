/**
 * MarkerResolver.java — G06-14 slice 2.
 *
 * Remembers, per deleted slot index, the deleted block's id + human name, so the
 * {@link com.customblocks.block.DeletedPlacementSweeper} can stamp each new Deleted marker with the
 * right "Deleted: <name>" label — even for far copies whose chunk loads minutes later, or after a
 * restart. The marker's identity lives in its BlockEntity once placed; this map is the source the
 * sweeper reads at placement time.
 *
 * Persisted to config/customblocks/marker_names.json via atomic write (NFR-13) so far / unloaded
 * copies still get a correct label after a restart. Slice 4 (Trash restore) will extend this with
 * heal-by-name + on-load matching; slice 2 only needs put / read / forget.
 *
 * Depends on: (none — standalone, mirrors DeletedSlots' storage idiom)
 * Called by:  DeletionService (put on delete), DeletedPlacementSweeper (idFor/nameFor at swap),
 *             HistoryCommands (forget on undo of a delete)
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public final class MarkerResolver {

    private static final String FILE = "config/customblocks/marker_names.json";
    private static final Gson GSON = new Gson();

    /** index → the deleted block's id + display name. */
    private record Entry(String id, String name) {}

    private static final Map<Integer, Entry> BY_INDEX = new HashMap<>();

    static { load(); }

    private MarkerResolver() {} // static-only

    /** Remember a deleted index's id + name so its markers can be labelled. Persists. */
    public static synchronized void put(int index, String id, String name) {
        BY_INDEX.put(index, new Entry(id == null ? "" : id, name == null ? "" : name));
        save();
    }

    /** The deleted block's id for {@code index}, or "" if unknown (→ a non-healing generic marker). */
    public static synchronized String idFor(int index) {
        Entry e = BY_INDEX.get(index);
        return e == null ? "" : e.id();
    }

    /** The deleted block's display name for {@code index}, or "" (→ a generic "(Deleted)" label). */
    public static synchronized String nameFor(int index) {
        Entry e = BY_INDEX.get(index);
        return e == null ? "" : e.name();
    }

    /** Drop an index — undo of a delete brought the block back, so it's no longer deleted. Persists. */
    public static synchronized void forget(int index) {
        if (BY_INDEX.remove(index) != null) save();
    }

    // -------------------------------------------------------------------------

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject root = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null || !root.has("markers")) return;
            JsonObject markers = root.getAsJsonObject("markers");
            for (String key : markers.keySet()) {
                JsonObject e = markers.getAsJsonObject(key);
                String id = e.has("id") ? e.get("id").getAsString() : "";
                String name = e.has("name") ? e.get("name").getAsString() : "";
                BY_INDEX.put(Integer.parseInt(key), new Entry(id, name));
            }
        } catch (Exception ignored) {}
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject markers = new JsonObject();
            for (Map.Entry<Integer, Entry> en : BY_INDEX.entrySet()) {
                JsonObject e = new JsonObject();
                e.addProperty("id", en.getValue().id());
                e.addProperty("name", en.getValue().name());
                markers.add(String.valueOf(en.getKey()), e);
            }
            JsonObject root = new JsonObject();
            root.add("markers", markers);
            Path tmp = file.resolveSibling("marker_names.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
