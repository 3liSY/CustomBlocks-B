/**
 * CategoryMembershipStore.java — multi-category membership (Group 11 §B).
 *
 * The category a block is in stopped being a word ON the block. A block now holds a SET of
 * category keys, and this store owns that set. {@link SlotData#category()} survives as a
 * legacy display field only — see "the legacy shadow" below.
 *
 * Model (G11 Locked Decisions, 2026-07-25):
 *   - A block may hold several memberships and NONE of them is a stored "main" — nothing here
 *     records a priority, and callers must not infer one from set order.
 *   - Every block always holds at least one membership. {@code "uncategorized"} is a built-in
 *     key that is the floor: removing a block's last real category drops it there instead of
 *     leaving it with nothing, and the key itself can never be deleted.
 *
 * The legacy shadow (owner decision, 2026-07-25): every surface that has NOT been reworked yet
 * (the HUD, the Arabic chest menus, exports, blueprint lore, the Bulk Workbench, the G27
 * Category Hub) still reads the one-word {@link SlotData#category()}. So each mutation here
 * writes a DERIVED value back onto the block: the alphabetically-first real membership, by its
 * typed display name, or "" when the block sits in {@code Uncategorized} alone. It is a display
 * mirror, not a main category — it is computed, never chosen, and this store never reads it back
 * as truth. It goes away when G27 reworks those surfaces onto {@link #of}.
 *
 * Persists to config/customblocks/data/category_membership.json via atomic write (NFR-13), the
 * same shape CategoryMetadataStore uses.
 *
 * Depends on: SlotManager (legacy shadow + live-id filter), CategoryMetadataStore (display names)
 * Called by:  CategoryService, CategoryCommands, AttributeCommands, BulkCategoryCommands,
 *             SlotManager (legacy dual-write), CustomBlocksMod (first-load conversion)
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class CategoryMembershipStore {

    private static final String FILE = "config/customblocks/data/category_membership.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** The built-in floor category. Always exists, never deletable, never removable as a last membership. */
    public static final String UNCATEGORIZED = "uncategorized";

    /** blockId → its category keys. LinkedHashSet keeps join order stable across saves (no priority implied). */
    private static final Map<String, LinkedHashSet<String>> DATA = new HashMap<>();

    static { load(); }

    private CategoryMembershipStore() {} // static-only

    /**
     * Normalize a typed category name to its matching key: lower-case, trimmed, and every run of
     * spaces / hyphens / underscores folded to one space.
     *
     * The fold is what makes "arabic letters" resolve "Arabic Letters" (G11 Locked Decisions) AND
     * what makes "arabic-letters" a key COLLISION with it rather than a silent second category
     * (TG11 A7). Both stores key off this one method so a name can never match in one and miss in
     * the other.
     */
    public static String key(String category) {
        if (category == null) return "";
        return category.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", " ").trim();
    }

    /** True for the built-in floor key. */
    public static boolean isUncategorized(String category) {
        return UNCATEGORIZED.equals(key(category));
    }

    // ── Reads ────────────────────────────────────────────────────────────────

    /**
     * Every category key {@code blockId} belongs to. A block with no entry reads as
     * {@code {"uncategorized"}} — never an empty set, so no caller has to handle "no category".
     */
    public static synchronized Set<String> of(String blockId) {
        LinkedHashSet<String> set = DATA.get(blockId);
        if (set == null || set.isEmpty()) return new LinkedHashSet<>(List.of(UNCATEGORIZED));
        return new LinkedHashSet<>(set);
    }

    /** True when {@code blockId} holds {@code category} (any membership — there is no main). */
    public static synchronized boolean has(String blockId, String category) {
        return of(blockId).contains(key(category));
    }

    /**
     * Every LIVE block id holding {@code category}, as {@link SlotData}, sorted by slot index.
     *
     * Entries for ids that no longer exist are skipped rather than deleted: a trashed block keeps
     * its memberships so /cb trash restore is lossless, and a stale row can never surface as a
     * phantom block in give/info/filter.
     */
    public static synchronized List<SlotData> blocksIn(String category) {
        String k = key(category);
        List<SlotData> out = new ArrayList<>();
        if (k.isEmpty()) return out;
        if (UNCATEGORIZED.equals(k)) {
            // The floor is implicit: a block with no row at all is uncategorized too.
            for (SlotData d : SlotManager.assignedSlots()) {
                if (of(d.customId()).contains(UNCATEGORIZED)) out.add(d);
            }
        } else {
            for (var e : DATA.entrySet()) {
                if (!e.getValue().contains(k)) continue;
                SlotData d = SlotManager.getById(e.getKey());
                if (d != null) out.add(d);
            }
        }
        out.sort(java.util.Comparator.comparingInt(SlotData::index));
        return out;
    }

    /** Live blocks whose ONLY membership is {@code category} — the exclusive-blocks delete mode. */
    public static synchronized List<SlotData> exclusiveBlocksIn(String category) {
        String k = key(category);
        List<SlotData> out = new ArrayList<>();
        for (SlotData d : blocksIn(k)) {
            if (of(d.customId()).size() == 1) out.add(d);
        }
        return out;
    }

    /** Every category key that at least one live block belongs to (excluding the floor). */
    public static synchronized Set<String> keysInUse() {
        Set<String> out = new TreeSet<>();
        for (var e : DATA.entrySet()) {
            if (SlotManager.getById(e.getKey()) == null) continue;
            for (String k : e.getValue()) if (!UNCATEGORIZED.equals(k)) out.add(k);
        }
        return out;
    }

    // ── Mutations ────────────────────────────────────────────────────────────
    //
    // LOCK ORDER (do not break this): a mutator holds THIS class's monitor only while it edits the
    // map and saves, and calls shadow() AFTER releasing it. shadow() writes through SlotManager,
    // which takes its own monitor — and SlotManager.createNoSave already runs the other way round,
    // holding SlotManager's monitor while it calls replaceAll() here. Shadowing inside the lock
    // would close that cycle into a deadlock. Never move a shadow() call into a synchronized block.

    /**
     * Add one membership. A block sitting only in {@code Uncategorized} leaves the floor as it
     * gains a real category — a block is never in a real category AND the floor at once.
     * Returns false when nothing changed (blank key, or already a member).
     */
    public static boolean add(String blockId, String category) {
        String k = key(category);
        if (blockId == null || k.isEmpty()) return false;
        boolean changed;
        synchronized (CategoryMembershipStore.class) {
            LinkedHashSet<String> set = DATA.computeIfAbsent(blockId, b -> new LinkedHashSet<>());
            if (set.isEmpty()) set.add(UNCATEGORIZED); // materialize the implicit floor before editing
            if (!UNCATEGORIZED.equals(k)) set.remove(UNCATEGORIZED);
            changed = set.add(k);
            if (changed) save();
        }
        if (changed) shadow(blockId);
        return changed;
    }

    /**
     * Remove one membership. Dropping the last real category leaves the block in
     * {@code Uncategorized} rather than empty (TG11 A2). Refuses to remove the floor itself —
     * it is always the last resort, so there is nothing below it to fall to.
     * Returns false when nothing changed.
     */
    public static boolean remove(String blockId, String category) {
        String k = key(category);
        if (blockId == null || k.isEmpty() || UNCATEGORIZED.equals(k)) return false;
        synchronized (CategoryMembershipStore.class) {
            LinkedHashSet<String> set = DATA.get(blockId);
            if (set == null || !set.remove(k)) return false;
            if (set.isEmpty()) set.add(UNCATEGORIZED);
            save();
        }
        shadow(blockId);
        return true;
    }

    /**
     * Replace a block's whole membership set with one category ("" / "uncategorized" → the floor).
     * This is the LEGACY write path: {@link SlotManager#setCategory} routes through it so every
     * not-yet-reworked caller (studio create, template apply, ZIP/vault import, Arabic bootstrap)
     * still lands in the new model with its old replace semantics intact.
     */
    public static synchronized void replaceAll(String blockId, String category) {
        if (blockId == null) return;
        String k = key(category);
        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.add(k.isEmpty() ? UNCATEGORIZED : k);
        DATA.put(blockId, set);
        save();
        // No shadow() here — the caller is SlotManager.setCategory, which is already writing the
        // legacy field itself. Shadowing back would be a redundant second save of the same value.
    }

    /** Move every block's {@code oldKey} membership to {@code newKey} (category rename). */
    public static void renameKey(String oldKey, String newKey) {
        String from = key(oldKey), to = key(newKey);
        if (from.isEmpty() || to.isEmpty() || from.equals(to) || UNCATEGORIZED.equals(from)) return;
        List<String> touched = new ArrayList<>();
        synchronized (CategoryMembershipStore.class) {
            for (var e : DATA.entrySet()) {
                LinkedHashSet<String> set = e.getValue();
                if (!set.remove(from)) continue;
                set.add(to);
                if (set.isEmpty()) set.add(UNCATEGORIZED);
                touched.add(e.getKey());
            }
            if (touched.isEmpty()) return;
            save();
        }
        for (String id : touched) shadow(id);
    }

    /**
     * Union {@code fromKey} into {@code toKey}: every block in the source gains the target and
     * loses the source. A block already in both just dedups — it is a Set (TG11 A6).
     * Returns how many blocks were touched.
     */
    public static int mergeInto(String fromKey, String toKey) {
        String from = key(fromKey), to = key(toKey);
        if (from.isEmpty() || to.isEmpty() || from.equals(to) || UNCATEGORIZED.equals(from)) return 0;
        List<String> touched = new ArrayList<>();
        synchronized (CategoryMembershipStore.class) {
            for (var e : DATA.entrySet()) {
                LinkedHashSet<String> set = e.getValue();
                if (!set.remove(from)) continue;
                if (!UNCATEGORIZED.equals(to)) set.remove(UNCATEGORIZED);
                set.add(to);
                if (set.isEmpty()) set.add(UNCATEGORIZED);
                touched.add(e.getKey());
            }
            if (touched.isEmpty()) return 0;
            save();
        }
        for (String id : touched) shadow(id);
        return touched.size();
    }

    /**
     * Strip {@code category} from every block that holds it, leaving other memberships alone —
     * the "category only" delete mode (TG11 A3). Blocks with nothing else left fall to the floor.
     * Returns how many blocks were touched.
     */
    public static int deleteKeyEverywhere(String category) {
        String k = key(category);
        if (k.isEmpty() || UNCATEGORIZED.equals(k)) return 0; // the floor is never deletable
        List<String> touched = new ArrayList<>();
        synchronized (CategoryMembershipStore.class) {
            for (var e : DATA.entrySet()) {
                LinkedHashSet<String> set = e.getValue();
                if (!set.remove(k)) continue;
                if (set.isEmpty()) set.add(UNCATEGORIZED);
                touched.add(e.getKey());
            }
            if (touched.isEmpty()) return 0;
            save();
        }
        for (String id : touched) shadow(id);
        return touched.size();
    }

    /**
     * Put a block's membership set back to exactly {@code keys} — the undo/redo restore path for
     * {@link UndoManager.Kind#CATEGORY}. An empty or all-blank set falls to the floor, so an undo
     * can never leave a block belonging to nothing.
     */
    public static void restoreSet(String blockId, Collection<String> keys) {
        if (blockId == null) return;
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (keys != null) for (String k : keys) {
            String n = key(k);
            if (!n.isEmpty()) set.add(n);
        }
        if (set.size() > 1) set.remove(UNCATEGORIZED); // the floor never coexists with a real category
        if (set.isEmpty()) set.add(UNCATEGORIZED);
        synchronized (CategoryMembershipStore.class) {
            DATA.put(blockId, set);
            save();
        }
        shadow(blockId);
    }

    /** Follow a block through /cb reid so its memberships don't dangle on the old id. */
    public static synchronized void renameBlockId(String oldId, String newId) {
        LinkedHashSet<String> set = DATA.remove(oldId);
        if (set == null) return;
        DATA.put(newId, set);
        save();
    }

    // ── The legacy shadow ────────────────────────────────────────────────────

    /**
     * Recompute the derived {@link SlotData#category()} display value for one block: the
     * alphabetically-first real membership shown by its TYPED display name (so "Arabic Letters"
     * keeps its capitals on the old screens), or "" when the block sits in the floor alone.
     *
     * Deliberately NOT a main category — nobody picks it, nothing stores a priority, and no read
     * path in this class consults it. Delete this method and its call sites once G27 moves those
     * surfaces onto {@link #of}.
     */
    private static void shadow(String blockId) {
        SlotData d = SlotManager.getById(blockId);
        if (d == null) return; // trashed block: its memberships are kept, but there's nothing to stamp
        String first = "";
        for (String k : new TreeSet<>(of(blockId))) {
            if (UNCATEGORIZED.equals(k)) continue;
            first = CategoryMetadataStore.getDisplayName(k);
            break;
        }
        if (!first.equals(d.category())) SlotManager.setCategoryShadow(blockId, first);
    }

    // ── First-load conversion (G11: no migration phase, just don't lose stray words) ──

    /**
     * Turn any leftover legacy category word into real membership + metadata records, ONCE.
     *
     * No backup, no confirm, no report: the owner confirmed no category data is in real use, so
     * the old gated migration was dropped (G11 Locked Decisions, 2026-07-25). It still runs so a
     * stray assignment — the 224 bundled Arabic blocks, for one — is converted rather than lost.
     * A no-op on every boot after the first, detected by the store file already existing.
     *
     * Call right after {@link SlotManager#loadAll()}, once the blocks are in memory.
     */
    public static synchronized void runFirstLoadConversionIfNeeded() {
        if (Files.exists(Path.of(FILE))) return;
        int blocks = 0;
        Set<String> made = new TreeSet<>();
        for (SlotData d : SlotManager.assignedSlots()) {
            String typed = d.category();
            if (typed == null || typed.isBlank()) continue;
            String k = key(typed);
            DATA.computeIfAbsent(d.customId(), b -> new LinkedHashSet<>()).add(k);
            CategoryMetadataStore.create(typed); // stores the name AS TYPED, so casing survives
            made.add(k);
            blocks++;
        }
        save(); // always write, even with nothing found — this file IS the "already ran" marker
        com.customblocks.CustomBlocksMod.LOGGER.info(
                "[CustomBlocks] Category first-load conversion: {} block(s) into {} category record(s){}",
                blocks, made.size(), made.isEmpty() ? " (nothing to convert)" : " " + made);
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject root = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) return;
            for (var e : root.entrySet()) {
                try {
                    LinkedHashSet<String> set = new LinkedHashSet<>();
                    for (JsonElement el : e.getValue().getAsJsonArray()) {
                        String k = key(el.getAsString());
                        if (!k.isEmpty()) set.add(k);
                    }
                    if (set.isEmpty()) set.add(UNCATEGORIZED);
                    DATA.put(e.getKey(), set);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private static void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            for (var e : DATA.entrySet()) {
                Collection<String> set = e.getValue();
                // A block sitting only in the floor is the default — don't spend a row on it.
                if (set.isEmpty() || (set.size() == 1 && set.contains(UNCATEGORIZED))) continue;
                JsonArray arr = new JsonArray();
                for (String k : set) arr.add(k);
                root.add(e.getKey(), arr);
            }
            Path tmp = file.resolveSibling("category_membership.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
