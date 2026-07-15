/**
 * FreedSlots.java
 *
 * Responsibility: Track slot INDICES freed by a NORMAL delete (the red Deleter tool or
 * `/cb delete`), so {@link SlotManager#nextFreeSlotIndex} does not hand a just-freed index
 * straight back to the next create. A placed copy of the deleted block still wears its
 * {@code slot_N} body in the world; reusing that index immediately would make the lingering
 * placement inherit the new block's skin / name / HUD (the G06-2 / G06-3 / G05-2 corruption).
 *
 * A freed index is:
 *   - reserved from auto-reuse (nextFreeSlotIndex prefers pristine, never-assigned slots; it
 *     reuses a freed index only as a last resort, and drops it from this set when it does — the
 *     create path then clears its stale texture), and
 *   - NEVER air-cleaned. This is the crucial difference from {@link RetiredSlots}: that set is
 *     the target of the Arabic placed-copy air-swap (ArabicLetterRetirement), which turns a
 *     placed slot_N into AIR. The developer wants a deleted block's placement to STAY in the
 *     world as a broken/empty block, not vanish — so the normal-delete reuse guard must live in
 *     its own set that no air-clean ever reads.
 *
 * Persisted to config/customblocks/freed_slots.json via atomic write (NFR-13) so the reservation
 * survives a restart — otherwise a create after a restart could reuse the index and corrupt the
 * lingering placement.
 *
 * Depends on: (none -- standalone)
 * Called by:  SlotManager (reserve on delete + reuse-as-last-resort in nextFreeSlotIndex)
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FreedSlots {

    private static final String FILE = "config/customblocks/freed_slots.json";
    private static final Gson GSON = new Gson();
    private static final Set<Integer> FREED = new HashSet<>();

    static { load(); }

    private FreedSlots() {}

    public static synchronized boolean contains(int index) {
        return FREED.contains(index);
    }

    public static synchronized boolean isEmpty() {
        return FREED.isEmpty();
    }

    public static synchronized int count() {
        return FREED.size();
    }

    /** Reserve a freed index from instant reuse and persist. No-op if already present. */
    public static synchronized void add(int index) {
        if (FREED.add(index)) save();
    }

    /** Drop one index (it has been reused for a new block, or its placement is gone). */
    public static synchronized void remove(int index) {
        if (FREED.remove(index)) save();
    }

    /** Snapshot of all freed indices, sorted ascending. */
    public static synchronized List<Integer> all() {
        List<Integer> out = new ArrayList<>(FREED);
        Collections.sort(out);
        return out;
    }

    // -------------------------------------------------------------------------

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject o = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null || !o.has("freed")) return;
            for (var e : o.getAsJsonArray("freed")) FREED.add(e.getAsInt());
        } catch (Exception ignored) {}
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject o = new JsonObject();
            JsonArray arr = new JsonArray();
            for (int i : all()) arr.add(i);
            o.add("freed", arr);
            Path tmp = file.resolveSibling("freed_slots.json.tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
