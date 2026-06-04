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

import java.util.Collection;
import java.util.Map;
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
                    .strength(1.5f, 6.0f);
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
        String name = (displayName == null || displayName.isBlank()) ? customId : displayName;
        SlotData d = new SlotData(idx, customId, name);
        BY_SLOT.put(d.slotKey(), d);
        BY_ID.put(customId, d);
        saveAll();
        return d;
    }

    /** Free a slot. Returns the removed data, or null if no such id. */
    public static synchronized SlotData delete(String customId) {
        SlotData d = BY_ID.remove(customId);
        if (d != null) {
            BY_SLOT.remove(d.slotKey());
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

    /** Copy a block into a new free slot under {@code newId}. Returns the new data, or null. */
    public static synchronized SlotData dupe(String customId, String newId) {
        SlotData src = BY_ID.get(customId);
        if (src == null || newId == null || newId.isBlank() || BY_ID.containsKey(newId)) return null;
        return create(newId, src.displayName());
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

    public static SlotData getBySlot(String slotKey) { return BY_SLOT.get(slotKey); }
    public static SlotData getById(String customId) { return BY_ID.get(customId); }
    public static boolean hasId(String customId) { return BY_ID.containsKey(customId); }
    public static Collection<SlotData> assignedSlots() { return BY_ID.values(); }
}
