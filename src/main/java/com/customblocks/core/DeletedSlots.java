/**
 * DeletedSlots.java
 *
 * Responsibility: Track slot INDICES freed by a normal delete (the red Deleter tool or `/cb delete`).
 * A deleted index is **permanently retired** — {@link SlotManager#nextFreeSlotIndex} never reuses it
 * (not even as a last resort), and {@link com.customblocks.block.DeletedPlacementSweeper} swaps any
 * placed copy of that index to the shared {@code (Removed)} block. Together that guarantees a deleted
 * block's leftover placement can never inherit an old OR a future block's identity (G06-2 / G06-3 /
 * G05-2, "improved Option 2" 2026-06-26).
 *
 * This replaces the old {@link FreedSlots} reuse-as-last-resort guard, whose contents are migrated in
 * once on boot. It is distinct from {@link RetiredSlots} (the Arabic air-clean set, which turns
 * placements into AIR and may be reused) — a deleted block's placement becomes a visible (Removed)
 * block, never air, and its index is never reused.
 *
 * Persisted to config/customblocks/deleted_slots.json via atomic write (NFR-13) so both the no-reuse
 * guard and the placement sweep survive a restart.
 *
 * Depends on: (none -- standalone)
 * Called by:  SlotManager (record on delete + skip in nextFreeSlotIndex + un-retire on undo),
 *             DeletedPlacementSweeper (read for the placed-copy swap), CustomBlocksMod (boot migration)
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DeletedSlots {

    private static final String FILE = "config/customblocks/deleted_slots.json";
    private static final Gson GSON = new Gson();
    private static final Set<Integer> DELETED = new HashSet<>();

    static { load(); }

    private DeletedSlots() {}

    public static synchronized boolean contains(int index) {
        return DELETED.contains(index);
    }

    public static synchronized boolean isEmpty() {
        return DELETED.isEmpty();
    }

    public static synchronized int count() {
        return DELETED.size();
    }

    /** Retire a freed index permanently and persist. No-op if already present. */
    public static synchronized void add(int index) {
        if (DELETED.add(index)) save();
    }

    /** Retire a batch and persist once (boot migration from {@link FreedSlots}). */
    public static synchronized void addAll(Collection<Integer> indices) {
        if (DELETED.addAll(indices)) save();
    }

    /** Un-retire one index — undo of a delete restored the block, so its index is live again. */
    public static synchronized void remove(int index) {
        if (DELETED.remove(index)) save();
    }

    /** Snapshot of all deleted indices, sorted ascending. */
    public static synchronized List<Integer> all() {
        List<Integer> out = new ArrayList<>(DELETED);
        Collections.sort(out);
        return out;
    }

    // -------------------------------------------------------------------------

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject o = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null || !o.has("deleted")) return;
            for (var e : o.getAsJsonArray("deleted")) DELETED.add(e.getAsInt());
        } catch (Exception ignored) {}
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject o = new JsonObject();
            JsonArray arr = new JsonArray();
            for (int i : all()) arr.add(i);
            o.add("deleted", arr);
            Path tmp = file.resolveSibling("deleted_slots.json.tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
