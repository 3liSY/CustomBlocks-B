/**
 * SlotManager.java
 *
 * Responsibility: Single source of truth for all slot assignments (design rule #2,
 * FR-01-4). Pre-registers the fixed pool of SlotBlocks at startup, tracks which slots
 * are assigned to which SlotData, and routes all persistence through SlotDataStore.
 *
 * Phase 2 (persistence + CRUD): create/delete/rename/dupe + load/save. The undo engine
 * (Phase 6) and texture sync (Phase 4) hook in here later.
 *
 * Depends on: SlotBlock, SlotData, SlotDataStore, CustomBlocksMod (MOD_ID, LOGGER)
 * Called by:  CustomBlocksMod.onInitialize(); command handlers; later GUI + items.
 *
 * ADR Reference: ADR-001 (pre-registration instead of dynamic registry).
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksMod;
import com.customblocks.block.SlotBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public final class SlotManager {

    private SlotManager() {} // static-only

    private static SlotBlock[] blocks;
    private static SlotBlock.SlotItem[] items;
    private static int maxSlots;

    /** Assigned slot data keyed by slot key ("slot_N"). */
    private static final Map<String, SlotData> BY_SLOT = new ConcurrentHashMap<>();
    /** Assigned slot data keyed by custom id. */
    private static final Map<String, SlotData> BY_ID = new ConcurrentHashMap<>();

    // ── Registration (Phase 1) ───────────────────────────────────────────────
    /** Pre-register the fixed pool of {@code max} SlotBlocks + their BlockItems. */
    public static void registerAll(int max) {
        maxSlots = max;
        blocks = new SlotBlock[max];
        items = new SlotBlock.SlotItem[max];
        for (int i = 0; i < max; i++) {
            AbstractBlock.Settings settings = AbstractBlock.Settings.create()
                    .strength(1.5f, 6.0f)
                    // nonOpaque so the game respects each block's real (possibly partial) shape for
                    // face culling — without it every slot is treated as a full opaque cube, so a
                    // slab/pillar/etc. wrongly culls its neighbours' faces and you see through the
                    // world behind it (the G08 "x-ray" bug). Also lets cut-out (transparent-background)
                    // textures show through correctly.
                    .nonOpaque()
                    // Luminance is baked per state at construction, so read the LIGHT
                    // property — each of the 16 states bakes to its own correct value.
                    .luminance(state -> state.get(SlotBlock.LIGHT));
            SlotBlock block = new SlotBlock(i, settings);
            Identifier id = Identifier.of(CustomBlocksMod.MOD_ID, "slot_" + i);
            SlotBlock.SlotItem item = new SlotBlock.SlotItem(block, new Item.Settings());
            Registry.register(Registries.BLOCK, id, block);
            Registry.register(Registries.ITEM, id, item);
            blocks[i] = block;
            items[i] = item;
        }
    }

    // ── Assignment / CRUD (Phase 2/3) ────────────────────────────────────────
    /** Claim the next free slot for a new block. Returns null if id taken or pool full. */
    public static synchronized SlotData create(String customId, String displayName) {
        if (customId == null || customId.isBlank()) return null;
        if (BY_ID.containsKey(customId)) return null;
        int idx = nextFreeSlotIndex();
        if (idx < 0) return null;
        TextureStore.delete(idx); // clear any stale texture from a previously freed slot
        String name = (displayName == null || displayName.isBlank()) ? customId : displayName;
        SlotData d = new SlotData(idx, customId, name);
        BY_SLOT.put(d.slotKey(), d);
        BY_ID.put(customId, d);
        saveAll();
        return d;
    }

    /** Free a slot. Returns the removed data, or null if no such id. The block is copied into the
     *  trash (best-effort) BEFORE its texture is removed, so it can be browsed/restored later. */
    public static synchronized SlotData delete(String customId) {
        SlotData d = BY_ID.remove(customId);
        if (d != null) {
            BY_SLOT.remove(d.slotKey());
            // Snapshot into the trash first, while the texture/source still exist on disk.
            TrashManager.capture(d, TextureStore.load(d.index()), TextureStore.loadSource(d.index()));
            TextureStore.delete(d.index());
            saveAll();
        }
        return d;
    }

    /** Change a block's display name (immutably). Returns the new data, or null. */
    public static synchronized SlotData rename(String customId, String newDisplayName) {
        SlotData d = BY_ID.get(customId);
        if (d == null) return null;
        SlotData updated = d.withDisplayName(newDisplayName);
        BY_ID.put(customId, updated);
        BY_SLOT.put(updated.slotKey(), updated);
        saveAll();
        return updated;
    }

    /**
     * Change a block's custom id from {@code oldId} to {@code newId}, keeping its slot INDEX
     * (so the baked texture, keyed by index, does NOT move and already-placed blocks — which are
     * the registry {@code slot_N} blocks, not the custom id — are unaffected; no pack rebuild
     * needed). Migrates every id-keyed reference (locks, favorites, notes, drafts) so nothing
     * dangles; the category rides on the SlotData itself. Returns the renamed data, or null if
     * oldId is missing, or newId is blank / unchanged / already taken.
     *
     * Records NO undo — the caller (ReIdCommands, or HistoryCommands when reversing) owns that.
     * Because the migration is a pure move, reId is its own inverse: reId(new, old) reverses it.
     */
    public static synchronized SlotData reId(String oldId, String newId) {
        if (oldId == null || newId == null) return null;
        newId = newId.trim();
        if (newId.isBlank() || newId.equals(oldId)) return null;
        SlotData old = BY_ID.get(oldId);
        if (old == null || BY_ID.containsKey(newId)) return null;
        SlotData updated = old.withCustomId(newId);
        BY_ID.remove(oldId);
        BY_ID.put(newId, updated);
        BY_SLOT.put(updated.slotKey(), updated); // same slot key, new value (drops the old-id snapshot)
        // Migrate every id-keyed reference so none points at the now-gone old id.
        LockManager.renameId(oldId, newId);
        FavoritesManager.renameId(oldId, newId);
        BlockNotesManager.renameId(oldId, newId);
        DraftManager.renameId(oldId, newId);
        BlockToleranceStore.renameId(oldId, newId);
        saveAll();
        return updated;
    }

    /** Set a block's light emission (0..15, clamped). Returns the new data, or null. */
    public static synchronized SlotData setGlow(String customId, int level) {
        SlotData d = BY_ID.get(customId);
        if (d == null) return null;
        SlotData updated = d.withGlow(level);
        BY_ID.put(customId, updated);
        BY_SLOT.put(updated.slotKey(), updated);
        saveAll();
        return updated;
    }

    /** Set a block's break hardness (negative = unbreakable, 0 = instant). Returns new data or null. */
    public static synchronized SlotData setHardness(String customId, float hardness) {
        SlotData d = BY_ID.get(customId);
        if (d == null) return null;
        SlotData updated = d.withHardness(hardness);
        BY_ID.put(customId, updated);
        BY_SLOT.put(updated.slotKey(), updated);
        saveAll();
        return updated;
    }

    /** Set a block's break/step/place sound group (see SlotBlock.getSoundGroup). Returns new data or null. */
    public static synchronized SlotData setSoundType(String customId, String soundType) {
        SlotData d = BY_ID.get(customId);
        if (d == null) return null;
        SlotData updated = d.withSoundType(soundType);
        BY_ID.put(customId, updated);
        BY_SLOT.put(updated.slotKey(), updated);
        saveAll();
        return updated;
    }

    /** Toggle a block's collision (true = passable/walk-through). Returns new data or null. */
    public static synchronized SlotData setNoCollision(String customId, boolean noCollision) {
        SlotData d = BY_ID.get(customId);
        if (d == null) return null;
        SlotData updated = d.withNoCollision(noCollision);
        BY_ID.put(customId, updated);
        BY_SLOT.put(updated.slotKey(), updated);
        saveAll();
        return updated;
    }

    /** Assign a block to a category ("" = uncategorized). Returns new data or null. */
    public static synchronized SlotData setCategory(String customId, String category) {
        SlotData d = BY_ID.get(customId);
        if (d == null) return null;
        SlotData updated = d.withCategory(category);
        BY_ID.put(customId, updated);
        BY_SLOT.put(updated.slotKey(), updated);
        saveAll();
        return updated;
    }

    /** Set a block's shape (see BlockShapes; null/blank → full). Returns new data or null.
     *  The caller rebuilds the pack — the model changes, unlike glow/sound which are live. */
    public static synchronized SlotData setShape(String customId, String shape) {
        SlotData d = BY_ID.get(customId);
        if (d == null) return null;
        SlotData updated = d.withShape(shape);
        BY_ID.put(customId, updated);
        BY_SLOT.put(updated.slotKey(), updated);
        saveAll();
        return updated;
    }

    /** Copy a block into a new free slot under {@code newId}. Returns the new data, or null. */
    /**
     * Duplicate a block into a new id: clones the display name, every attribute (glow / hardness /
     * sound / collision / category) AND the baked texture + stored source image, so the copy is
     * visually identical. Returns the new block's latest snapshot, or null if the source is missing,
     * the new id is taken/blank, or no slot is free. Caller rebuilds the pack to show the texture.
     */
    public static synchronized SlotData dupe(String customId, String newId) {
        SlotData src = BY_ID.get(customId);
        if (src == null || newId == null || newId.isBlank() || BY_ID.containsKey(newId)) return null;
        SlotData created = create(newId, src.displayName());
        if (created == null) return null; // no free slot
        // Clone attributes onto the new block.
        setGlow(newId, src.glow());
        setHardness(newId, src.hardness());
        setSoundType(newId, src.soundType());
        setNoCollision(newId, src.noCollision());
        setCategory(newId, src.category());
        // Clone the baked texture and the original source image (for retexture-all re-renders).
        byte[] tex = TextureStore.load(src.index());
        if (tex != null) TextureStore.save(created.index(), tex);
        byte[] source = TextureStore.loadSource(src.index());
        if (source != null) TextureStore.saveSource(created.index(), source);
        return BY_ID.get(newId);
    }

    // ── Undo/redo support (non-recording state restore — Phase 6) ─────────────
    /**
     * Put a snapshot straight back into the live maps + persist. Does NOT record undo —
     * only UndoManager-driven restores call this, so undo never re-triggers itself.
     */
    public static synchronized void restoreSnapshot(SlotData d) {
        if (d == null) return;
        BY_ID.put(d.customId(), d);
        BY_SLOT.put(d.slotKey(), d);
        saveAll();
    }

    /** Remove a block (slot data + texture) without recording undo (undo/redo internal). */
    public static synchronized void removeSilently(String customId) {
        SlotData d = BY_ID.remove(customId);
        if (d != null) {
            BY_SLOT.remove(d.slotKey());
            TextureStore.delete(d.index());
            saveAll();
        }
    }

    private static int nextFreeSlotIndex() {
        for (int i = 0; i < maxSlots; i++) {
            if (!BY_SLOT.containsKey("slot_" + i)) return i;
        }
        return -1;
    }

    // ── Persistence (routed through SlotDataStore — design rule #4) ───────────
    public static void saveAll() {
        SlotDataStore.save(BY_ID.values());
    }

    public static synchronized void loadAll() {
        for (SlotData d : SlotDataStore.load()) {
            if (d.index() < 0 || d.index() >= maxSlots) {
                CustomBlocksMod.LOGGER.warn("[CustomBlocks] Skipping '{}' — slot index {} is outside maxSlots {}.",
                        d.customId(), d.index(), maxSlots);
                continue;
            }
            BY_SLOT.put(d.slotKey(), d);
            BY_ID.put(d.customId(), d);
        }
    }

    /** Re-read all slot data from disk (for /cb reload). */
    public static synchronized void reload() {
        BY_SLOT.clear();
        BY_ID.clear();
        loadAll();
    }

    // ── Lookups ──────────────────────────────────────────────────────────────
    public static int getMaxSlots() { return maxSlots; }
    public static int registeredCount() { return blocks == null ? 0 : blocks.length; }
    public static int usedSlots() { return BY_SLOT.size(); }

    public static SlotBlock blockAt(int index) {
        if (blocks == null || index < 0 || index >= blocks.length) return null;
        return blocks[index];
    }

    public static SlotBlock.SlotItem itemAt(int index) {
        if (items == null || index < 0 || index >= items.length) return null;
        return items[index];
    }

    /** Live light level for a slot index (0 if unassigned). Read by the block's luminance fn. */
    public static int glowFor(int index) {
        SlotData d = BY_SLOT.get("slot_" + index);
        return d == null ? 0 : d.glow();
    }

    /** Live shape name for a slot index ("full" if unassigned). Read by the block's shape overrides. */
    public static String shapeFor(int index) {
        SlotData d = BY_SLOT.get("slot_" + index);
        return d == null ? SlotData.DEFAULT_SHAPE : d.shape();
    }

    public static SlotData getBySlot(String slotKey) { return BY_SLOT.get(slotKey); }
    public static SlotData getById(String customId) { return BY_ID.get(customId); }
    public static boolean hasId(String customId) { return BY_ID.containsKey(customId); }
    public static Collection<SlotData> assignedSlots() { return BY_ID.values(); }

    // ── Search / categories (Phase 8/9 — pure reads over the live maps) ───────
    /** All distinct non-empty categories currently in use, sorted alphabetically. */
    public static Set<String> categories() {
        Set<String> out = new TreeSet<>();
        for (SlotData d : BY_ID.values()) {
            if (!d.category().isEmpty()) out.add(d.category());
        }
        return out;
    }

    /** Blocks in a given category (case-insensitive exact match). */
    public static List<SlotData> byCategory(String category) {
        String c = category.trim().toLowerCase(Locale.ROOT);
        List<SlotData> out = new ArrayList<>();
        for (SlotData d : BY_ID.values()) {
            if (d.category().equalsIgnoreCase(c)) out.add(d);
        }
        return out;
    }

    /** Blocks whose id, display name, or category contains the query (case-insensitive). */
    public static List<SlotData> search(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<SlotData> out = new ArrayList<>();
        if (q.isEmpty()) return out;
        for (SlotData d : BY_ID.values()) {
            if (d.customId().toLowerCase(Locale.ROOT).contains(q)
                    || d.displayName().toLowerCase(Locale.ROOT).contains(q)
                    || d.category().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(d);
            }
        }
        return out;
    }
}
