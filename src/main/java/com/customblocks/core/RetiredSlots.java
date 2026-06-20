/**
 * RetiredSlots.java
 *
 * Responsibility: Track slot INDICES that have been permanently retired (Group 13 / Build B:
 * the old static Arabic letter blocks). A retired index is:
 *   - reserved from auto-reuse (nextFreeSlotIndex prefers fresh slots; it only reuses a retired
 *     index as a last resort, and drops it from this set when it does), and
 *   - the target of the placed-copy air cleanup (any placed slot_N at a retired index becomes air).
 * Persisted to config/customblocks/retired_slots.json via atomic write (NFR-13) so the cleanup
 * survives restarts. Numbers are never added here.
 *
 * Depends on: (none -- standalone)
 * Called by:  SlotManager (reserve + reuse), ArabicLetterRetirement (record + air cleanup)
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

public final class RetiredSlots {

    private static final String FILE = "config/customblocks/retired_slots.json";
    private static final Gson GSON = new Gson();
    private static final Set<Integer> RETIRED = new HashSet<>();

    static { load(); }

    private RetiredSlots() {}

    public static synchronized boolean contains(int index) {
        return RETIRED.contains(index);
    }

    public static synchronized boolean isEmpty() {
        return RETIRED.isEmpty();
    }

    public static synchronized int count() {
        return RETIRED.size();
    }

    /** Record a batch of retired indices and persist once. */
    public static synchronized void addAll(Collection<Integer> indices) {
        if (RETIRED.addAll(indices)) save();
    }

    /** Drop one index (it has been reused for a new block). Persists only if it changed. */
    public static synchronized void remove(int index) {
        if (RETIRED.remove(index)) save();
    }

    /** Snapshot of all retired indices, sorted ascending. */
    public static synchronized List<Integer> all() {
        List<Integer> out = new ArrayList<>(RETIRED);
        Collections.sort(out);
        return out;
    }

    // -------------------------------------------------------------------------

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject o = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null || !o.has("retired")) return;
            for (var e : o.getAsJsonArray("retired")) RETIRED.add(e.getAsInt());
        } catch (Exception ignored) {}
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject o = new JsonObject();
            JsonArray arr = new JsonArray();
            for (int i : all()) arr.add(i);
            o.add("retired", arr);
            Path tmp = file.resolveSibling("retired_slots.json.tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
