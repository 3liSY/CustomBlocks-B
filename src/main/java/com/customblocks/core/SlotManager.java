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
import java.util.function.UnaryOperator;

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
                    // dynamicBounds: shape is normally CACHED at registration (="full") so SlotBlock.get*Shape
                    // was never re-read → pillar/thin/stairs pushed the player (G08). Read shape LIVE instead.
                    .dynamicBounds()
                    // Luminance is baked per state at construction, so read the LIGHT
                    // property — each of the 16 states bakes to its own correct value.
                    .luminance(state -> state.get(SlotBlock.LIGHT));
            // Every index is a plain SlotBlock — G13-25 data-only design: an Arabic letter/number is
            // a normal slot flagged by ArabicMeta on its SlotData, never a subclass (SlotPools header).
            SlotBlock block = SlotPools.blockFor(i, settings);
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
        return create(customId, displayName, true);
    }

    /**
     * Batch create: claim a free slot WITHOUT persisting (caller calls {@link #saveAll()} once
     * after the whole batch), optionally placing it straight into a category. Used by bulk
     * imports (e.g. the 224 bundled Arabic blocks) so we don't rewrite slots.json per block.
     */
    public static synchronized SlotData createNoSave(String customId, String displayName, String category) {
        SlotData d = create(customId, displayName, false);
        if (d == null) return null;
        SlotData withCat = d.withCategory(category);
        BY_ID.put(customId, withCat);
        BY_SLOT.put(withCat.slotKey(), withCat);
        CategoryMembershipStore.replaceAll(customId, category); // G11: born into the membership model too
        return withCat;
    }

    /** G13-25: as {@link #createNoSave}, but stamping the slot's ArabicMeta so it is born a
     *  letter/number. Caller (ArabicSlotBootstrap) calls {@link #saveAll()} once after the batch. */
    public static synchronized SlotData createArabicNoSave(String customId, String displayName,
                                                           String category, ArabicMeta arabic) {
        SlotData d = createNoSave(customId, displayName, category);
        if (d == null) return null;
        SlotData withMeta = d.withArabic(arabic);
        BY_ID.put(withMeta.customId(), withMeta);
        BY_SLOT.put(withMeta.slotKey(), withMeta);
        return withMeta;
    }

    private static SlotData create(String customId, String displayName, boolean persist) {
        if (customId == null || customId.isBlank()) return null;
        if (BY_ID.containsKey(customId)) return null;
        // Allocation policy lives in SlotPools (full-pool scan; G06-2/G06-3 reuse rules).
        int idx = SlotPools.nextFreeNormalIndex(maxSlots, i -> BY_SLOT.containsKey("slot_" + i));
        if (idx < 0) return null;
        TextureStore.delete(idx); // clear any stale texture from a previously freed slot
        String name = (displayName == null || displayName.isBlank()) ? customId : displayName;
        name = NameCase.titleCase(name); // global rule: capitalize the first letter of every word
        SlotData d = new SlotData(idx, customId, name);
        BY_SLOT.put(d.slotKey(), d);
        BY_ID.put(customId, d);
        // D6 GUARD: do NOT MarkerTombstones.remove here — only a deliberate restore un-tombstones (TrashCommands.doRestore); on create it revives emptied markers → regresses D6.
        if (persist) saveAll();
        return d;
    }

    /** Free a slot. Returns the removed data, or null if no such id. The block is copied into the
     *  trash (best-effort) BEFORE its texture is removed, so it can be browsed/restored later. */
    public static synchronized SlotData delete(String customId) {
        SlotData d = BY_ID.remove(customId);
        if (d != null) {
            BY_SLOT.remove(d.slotKey());
            // Snapshot into the trash first, while the texture/source still exist on disk.
            TrashManager.capture(d, TextureStore.load(d.index()), TextureStore.loadSource(d.index()),
                    TextureStore.loadUrl(d.index()));
            TextureStore.delete(d.index());
            // G06-14 (recycle-bin): RESERVE the freed index while in trash — never reused (nextFreeSlotIndex
            // skips DeletedSlots), so a placed copy still wearing slot_N can't inherit a future block's skin;
            // DeletedPlacementSweeper turns each into a "Deleted: <name>" marker. Released on Restore
            // (restoreSnapshot reuses this index) or Empty (TrashCommands frees it).
            DeletedSlots.add(d.index());
            saveAll();
        }
        return d;
    }

    /** Change a block's display name (immutably). Returns the new data, or null. */
    public static synchronized SlotData rename(String customId, String newDisplayName) {
        SlotData d = getById(customId); // case-insensitive resolve; key by the STORED id (G25-family fix)
        if (d == null) return null;
        SlotData updated = d.withDisplayName(NameCase.titleCase(newDisplayName));
        BY_ID.put(d.customId(), updated);
        BY_SLOT.put(updated.slotKey(), updated);
        saveAll();
        TextureNameMirror.syncSlot(updated.index()); // Part C — name changed, no byte write: re-mirror (flag-gated)
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
        LockManager.renameId(oldId, newId); FavoritesManager.renameId(oldId, newId);
        BlockNotesManager.renameId(oldId, newId);
        DraftManager.renameId(oldId, newId);
        BlockToleranceStore.renameId(oldId, newId);
        GuessModeStore.renameId(oldId, newId); // Group 30 — keep a disguise reference valid across reid
        CategoryDisplayBlockManager.renameId(oldId, newId); // G07 dangling-ref audit — a category's display block must follow its id
        CategoryMembershipStore.renameBlockId(oldId, newId); // G11 — memberships are id-keyed, so they move too
        saveAll();
        return updated;
    }

    /**
     * The ONE setter rail every attribute setter below runs on: resolve the id case-insensitively, apply
     * the change to the immutable snapshot, re-key both maps under the STORED id (the G25-family fix, so
     * /cb setshape works whatever case you type), persist, and hand back the new data — or null when no
     * such block exists. The whole read-modify-write holds this class's monitor, so a setter is atomic.
     *
     * These were eight byte-identical copies differing only in their withX call; one rail means they
     * cannot drift apart (an id-keying or save fix has a single place to land).
     */
    private static synchronized SlotData mutate(String customId, UnaryOperator<SlotData> change) {
        SlotData d = getById(customId);
        if (d == null) return null;
        SlotData updated = change.apply(d);
        BY_ID.put(d.customId(), updated);
        BY_SLOT.put(updated.slotKey(), updated);
        saveAll();
        return updated;
    }

    /** Set a block's light emission (0..15, clamped). Returns the new data, or null. */
    public static SlotData setGlow(String customId, int level) { return mutate(customId, d -> d.withGlow(level)); }

    /** Set a block's break hardness (negative = unbreakable, 0 = instant). Returns new data or null. */
    public static SlotData setHardness(String customId, float hardness) { return mutate(customId, d -> d.withHardness(hardness)); }

    /** Set a block's break/step/place sound group (see SlotBlock.getSoundGroup). Returns new data or null. */
    public static SlotData setSoundType(String customId, String soundType) { return mutate(customId, d -> d.withSoundType(soundType)); }

    /** Toggle a block's collision (true = passable/walk-through). Returns new data or null. */
    public static SlotData setNoCollision(String customId, boolean noCollision) { return mutate(customId, d -> d.withNoCollision(noCollision)); }

    /**
     * Assign a block to a category ("" = uncategorized), REPLACING whatever it was in.
     *
     * G11: real membership lives in {@link CategoryMembershipStore} now, so this writes both — the
     * legacy one-word field and a one-category membership set. That keeps every caller that still
     * speaks the old single-category language (studio create, template apply, ZIP/vault import,
     * trash restore, the Arabic bootstrap) landing correctly in the new model without each one
     * having to be reworked. Command paths that want to ADD a membership call the membership store
     * directly instead of this.
     */
    public static SlotData setCategory(String customId, String category) {
        SlotData d = mutate(customId, x -> x.withCategory(category));
        if (d != null) CategoryMembershipStore.replaceAll(d.customId(), category);
        return d;
    }

    /**
     * Stamp the legacy display-only category field WITHOUT touching membership.
     *
     * The one caller is {@link CategoryMembershipStore}'s legacy shadow, which derives this value
     * FROM the membership set — routing it through {@link #setCategory} would immediately overwrite
     * the set it was derived from. Not a general-purpose setter; nothing else should call it.
     */
    static SlotData setCategoryShadow(String customId, String category) {
        return mutate(customId, d -> d.withCategory(category));
    }

    /** G10 §C: set a block's stored background ("black" / "transparent" / "#RRGGBB"). Returns new data
     *  or null. This records the CHOICE only — the caller re-bakes the texture (BackgroundService) and
     *  rebuilds the pack, because the pixels change, not just the metadata. */
    public static SlotData setBackground(String customId, String background) { return mutate(customId, d -> d.withBackground(background)); }

    /** Set a block's shape (see BlockShapes; null/blank → full). Returns new data or null.
     *  The caller rebuilds the pack — the model changes, unlike glow/sound which are live. */
    public static SlotData setShape(String customId, String shape) { return mutate(customId, d -> d.withShape(shape)); }

    /** Set a block's animation state (AnimData.NONE = make it static again). Returns new data or null.
     *  The caller rebuilds the pack — the .mcmeta + model change, like setShape. */
    public static SlotData setAnim(String customId, AnimData anim) { return mutate(customId, d -> d.withAnim(anim)); }

    /** Copy a block into a new free slot under {@code newId}. Returns the new data, or null. */
    /**
     * Duplicate a block into a new id: clones the display name, every attribute (glow / hardness /
     * sound / collision / category) AND the baked texture + stored source image, so the copy is
     * visually identical. Returns the new block's latest snapshot, or null if the source is missing,
     * the new id is taken/blank, or no slot is free. Caller rebuilds the pack to show the texture.
     */
    public static synchronized SlotData dupe(String customId, String newId) {
        SlotData src = getById(customId); // case-insensitive source lookup (G25-family fix)
        if (src == null || newId == null || newId.isBlank() || BY_ID.containsKey(newId)) return null;
        SlotData created = create(newId, src.displayName());
        if (created == null) return null; // no free slot
        // Clone attributes onto the new block.
        setGlow(newId, src.glow());
        setHardness(newId, src.hardness());
        setSoundType(newId, src.soundType());
        setNoCollision(newId, src.noCollision());
        setCategory(newId, src.category());
        if (src.isAnimated()) setAnim(newId, src.anim()); // clone animation so the copy animates too (Group 14)
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
        // Undo of a delete brings this block back at its old index → un-retire it so nextFreeSlotIndex
        // can use it again and the placement sweeper stops turning its (new) copies into (Removed).
        // Copies already swapped to (Removed) before the undo do not return (positions aren't tracked).
        DeletedSlots.remove(d.index());
        saveAll();
        TextureNameMirror.syncSlot(d.index()); // Part C — undo/redo may restore a different name (flag-gated)
    }

    public static synchronized void removeSilently(String customId) { removeSilently(customId, false); }
    public static synchronized void removeSilentlyKeepTexture(String customId) { removeSilently(customId, true); } // keeps PNG/source for undo-of-create → /cb redo
    private static synchronized void removeSilently(String customId, boolean keepTexture) {
        SlotData d = BY_ID.remove(customId);
        if (d == null) return;
        BY_SLOT.remove(d.slotKey());
        if (!keepTexture) TextureStore.delete(d.index());
        saveAll();
    }

    /**
     * Group 13 / Build B: permanently retire a batch of ids in one pass -- free each slot, delete
     * its texture, and record the freed index in {@link RetiredSlots} (so placed copies get
     * air-cleaned and the index is not reused while an old placement may linger). Saves once.
     * Returns the freed slot indices. Ids that do not exist are skipped (idempotent).
     */
    public static synchronized List<Integer> retireSlots(Collection<String> customIds) {
        List<Integer> freed = new ArrayList<>();
        for (String id : customIds) {
            SlotData d = BY_ID.remove(id);
            if (d == null) continue;
            BY_SLOT.remove(d.slotKey());
            TextureStore.delete(d.index());
            freed.add(d.index());
        }
        if (!freed.isEmpty()) {
            RetiredSlots.addAll(freed);
            saveAll();
        }
        return freed;
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

    /**
     * Group 26 FIX A — one-time, idempotent re-derive of every block's display name through
     * {@link NameCase#titleCase}. Blocks persisted before the underscore->space fix kept names like
     * "Alef_Black"; re-running the canonical rule cleans them to "Alef Black". Only persists when
     * something actually changed, so this is a no-op on every later boot (clean names re-derive to
     * themselves). Mutates slot state + saves through SlotDataStore (design rules #3, #4).
     * Returns how many names were cleaned. Call once right after {@link #loadAll()}.
     */
    public static synchronized int migrateDisplayNames() {
        int changed = 0;
        for (SlotData d : new ArrayList<>(BY_ID.values())) {
            String clean = NameCase.titleCase(d.displayName());
            if (!clean.equals(d.displayName())) {
                SlotData updated = d.withDisplayName(clean); // same index + id, so slotKey is unchanged
                BY_ID.put(updated.customId(), updated);
                BY_SLOT.put(updated.slotKey(), updated);
                changed++;
            }
        }
        if (changed > 0) saveAll();
        return changed;
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

    /** All registered slot blocks (Group 14 Phase 1b — used to build the shared anim_slot BlockEntityType).
     *  Returns the live backing array; callers must not mutate it. Null before registerAll(). */
    public static SlotBlock[] allBlocks() { return blocks; }

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

    /** Live animation state for a slot index (AnimData.NONE if unassigned/static). Read by the pack builder. */
    public static AnimData animFor(int index) {
        SlotData d = BY_SLOT.get("slot_" + index);
        return d == null ? AnimData.NONE : d.anim();
    }

    public static SlotData getBySlot(String slotKey) { return BY_SLOT.get(slotKey); }

    /**
     * Resolve a block by custom id. Exact match first (fast path — byte-identical to the old behavior
     * for every existing caller, so no regression); only when that misses does it fall back to a
     * case-insensitive scan (Group 26 FIX B) so `/cb give te` and `/cb give Te` both resolve the same
     * block. Tie-break on duplicate case-insensitive matches = lowest slot index (deterministic).
     * Returns null if nothing matches.
     */
    public static SlotData getById(String customId) {
        if (customId == null) return null;
        SlotData exact = BY_ID.get(customId);
        return exact != null ? exact : findByIdIgnoreCase(customId);
    }

    /** True if {@link #getById} would resolve this id — exact first, then case-insensitive fallback. */
    public static boolean hasId(String customId) {
        if (customId == null) return false;
        return BY_ID.containsKey(customId) || findByIdIgnoreCase(customId) != null;
    }

    /** Case-insensitive id lookup over BY_ID; on multiple matches returns the lowest slot index. */
    private static SlotData findByIdIgnoreCase(String customId) {
        SlotData best = null;
        for (SlotData d : BY_ID.values()) {
            if (d.customId().equalsIgnoreCase(customId) && (best == null || d.index() < best.index())) {
                best = d;
            }
        }
        return best;
    }

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
