/**
 * MarkerTombstones.java — G06-14 slice 5 (Empty tombstones a deleted block's markers).
 *
 * A persisted set of customIds that were EMPTIED from the trash (permanently deleted). When a block is
 * emptied, its remaining "Deleted: <name>" markers must become generic, un-healable "(Deleted)" markers
 * — so a later same-name {@code /cb create} can't accidentally revive them ({@link MarkerResolver}'s
 * auto-heal rule: heal a marker only when a LIVE block of the same customId exists AND the id is not
 * tombstoned here). Loaded markers are stripped immediately on Empty; far/unloaded ones are stripped the
 * moment their chunk loads (the sweep consults this set), which is why it must survive a restart.
 *
 * Append-only in practice (one entry per emptied block); tiny. Persisted to
 * config/customblocks/marker_tombstones.json via atomic write (NFR-13), mirroring {@link DeletedSlots}.
 *
 * Depends on: (none — standalone)
 * Called by:  TrashCommands (add on Empty), DeletedPlacementSweeper (contains during heal/tombstone)
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

public final class MarkerTombstones {

    private static final String FILE = "config/customblocks/marker_tombstones.json";
    private static final Gson GSON = new Gson();
    /** Lower-cased customIds of emptied blocks (match is case-insensitive, like SlotManager id resolve). */
    private static final Set<String> EMPTIED = new HashSet<>();

    static { load(); }

    private MarkerTombstones() {} // static-only

    /** Tombstone a customId (its markers may never heal again). Persists. No-op if blank/already present. */
    public static synchronized void add(String customId) {
        if (customId == null || customId.isBlank()) return;
        if (EMPTIED.add(customId.toLowerCase())) save();
    }

    /** Un-tombstone a customId — a restore brought a live block of this id back; its markers may heal again. */
    public static synchronized void remove(String customId) {
        if (customId == null || customId.isBlank()) return;
        if (EMPTIED.remove(customId.toLowerCase())) save();
    }

    /** True when this customId was emptied — its markers must stay generic and never heal. */
    public static synchronized boolean contains(String customId) {
        return customId != null && EMPTIED.contains(customId.toLowerCase());
    }

    public static synchronized boolean isEmpty() { return EMPTIED.isEmpty(); }

    // -------------------------------------------------------------------------

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject o = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null || !o.has("emptied")) return;
            for (var e : o.getAsJsonArray("emptied")) EMPTIED.add(e.getAsString().toLowerCase());
        } catch (Exception ignored) {}
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject o = new JsonObject();
            JsonArray arr = new JsonArray();
            for (String s : EMPTIED) arr.add(s);
            o.add("emptied", arr);
            Path tmp = file.resolveSibling("marker_tombstones.json.tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
