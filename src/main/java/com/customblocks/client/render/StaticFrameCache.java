/**
 * StaticFrameCache.java — Group 14 / Phase 1c, G05-§H cache budget. CLIENT-ONLY.
 *
 * The STATIC half of {@link AnimFrameCache}. Holds, per off-atlas static slot, ONE off-atlas GL texture
 * of the slot's full-resolution {@code slot_N.png}, uploaded with mipmaps OFF (nearest filter) so the
 * placed block stays CRISP instead of being muffled/downscaled by Minecraft's shared block atlas. Unlike
 * AnimFrameCache there is no frame strip and no per-frame upload — the image is read once and reused.
 *
 * Off-atlas gate (so we NEVER double-draw an atlas block): a slot is rendered off-atlas only when the pack
 * marked its block model INVISIBLE — i.e. {@code models/block/slot_N.json} has no {@code "parent"} key
 * (see ServerPackGenerator.invisibleBlockModelJson). Every atlas model (cube_all / per-face cube / shape /
 * cross / empty) sets a parent, so this cleanly separates off-atlas-static blocks from atlas ones and from
 * non-full / per-face blocks (which keep the atlas). Multi-frame strips belong to AnimFrameCache and are
 * rejected here. Everything is read from the client's PACK resources — no server sync needed, so it works
 * on a dedicated server too (same as AnimFrameCache). Cleared on every resource reload / pack rebuild.
 *
 * G05-§H budget: browsing a big pack could otherwise upload a full-res off-atlas texture for every distinct
 * static slot ever seen and never let one go until a reload. Each cache (world + icon) is now bounded by an
 * ENTRY count and a decoded-BYTE ceiling ({@link #MAX_ENTRIES}/{@link #MAX_BYTES}), kept in access order so
 * the LEAST-recently-used slot is evicted first when a new build pushes it over — the evicted texture is
 * destroyed (GL + backing NativeImage freed) and simply rebuilt from the pack when it next comes on screen,
 * exactly like AnimFrameCache's idle pool. {@link #stats()} exposes the live counters for diagnostics (§H8).
 *
 * Depends on: MinecraftClient (resource + texture managers), the pack (slot_N.png + slot_N.json model).
 * Called by:  AnimSlotBER (placed render), SlotItemRenderer (hand/inventory), ResourcePackGenerator +
 *             ClientPackReceiver (clear on reload).
 *
 * See: docs/adr/ADR-008 (Path B, off-atlas) + GROUP_14 Phase 1c + Group_05 §H.
 */
package com.customblocks.client.render;

import com.customblocks.CustomBlocksMod;
import com.customblocks.core.TextureStore;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class StaticFrameCache {

    private StaticFrameCache() {} // static-only

    private static final String MOD_ID = CustomBlocksMod.MOD_ID;

    /** Max resident textures per cache (world / icon). A new build past this evicts the least-recently-used. */
    private static final int MAX_ENTRIES = 96;
    /** Max resident decoded bytes per cache. At 512px = 1 MiB/texture this binds ~alongside the entry cap. */
    private static final long MAX_BYTES = 96L * 1024 * 1024;
    /** Don't log an eviction readout more than this often. */
    private static final long LOG_THROTTLE_MS = 5_000;

    /** One resident off-atlas texture: its bind id and decoded (native) byte footprint. The GL texture is
     *  owned by the texture manager under {@code id}; {@code destroyTexture(id)} frees it + its NativeImage. */
    private static final class Entry {
        final Identifier id;
        final long bytes;
        Entry(Identifier id, long bytes) { this.id = id; this.bytes = bytes; }
    }

    /** slot → world off-atlas texture, access-ordered so eldest == least-recently-used (G05-§H LRU). */
    private static final Map<Integer, Entry> CACHE = new LinkedHashMap<>(16, 0.75f, true);
    /** slot → Group 30 icon-fallback texture, same LRU discipline as {@link #CACHE}. */
    private static final Map<Integer, Entry> ICON_CACHE = new LinkedHashMap<>(16, 0.75f, true);
    /** Slots we've decided are NOT off-atlas (atlas-rendered / animated / unreadable) — don't retry. */
    private static final Set<Integer> NOT_OFFATLAS = new HashSet<>();
    private static final Set<Integer> ICON_NOT_READABLE = new HashSet<>();
    /** slot → does the pack carry any per-face texture ({@code slot_N_<face>.png}), i.e. is the slot PAINTED.
     *  Polled per item per frame by the shaped-icon path ({@link ShapedItemIcon}), so it is cached here and
     *  invalidated with the texture caches on every resource reload (a newly painted/cleared slot re-reads
     *  after the pack rebuild). Kept beside the other pack-derived per-slot flags for one clear discipline. */
    private static final Map<Integer, Boolean> PER_FACE = new HashMap<>();

    private static long worldBytes = 0;
    private static long iconBytes = 0;
    private static long lastLogMs = 0;

    /**
     * Outcome of a build: an {@link Entry} to cache, or none with a flag saying whether the "no off-atlas
     * texture" verdict is STABLE (present + parsed pack data, e.g. an atlas model or a multi-frame strip →
     * safe to remember) or TRANSIENT (a required model/png was absent or unreadable → the pack is still being
     * (re)written; must NOT be cached, or a false negative pins a wrong icon until a reload). Mirrors
     * AnimFrameCache.Built — same anti-poison discipline.
     */
    private static final class Res {
        static final Res TRANSIENT = new Res(null, false);
        static final Res STABLE = new Res(null, true);
        final Entry entry;
        final boolean stableNegative;
        private Res(Entry entry, boolean stableNegative) { this.entry = entry; this.stableNegative = stableNegative; }
        static Res ok(Entry entry) { return new Res(entry, false); }
    }

    /** Off-atlas texture id for a STATIC off-atlas slot, or null when the slot is atlas-rendered/animated. */
    public static Identifier get(int slotIndex) {
        if (slotIndex < 0) return null;
        Entry cached = CACHE.get(slotIndex); // access-order get() marks this slot most-recently-used
        if (cached != null) return cached.id;
        if (NOT_OFFATLAS.contains(slotIndex)) return null;
        Res r = build(slotIndex);
        if (r.entry == null) { if (r.stableNegative) NOT_OFFATLAS.add(slotIndex); return null; }
        CACHE.put(slotIndex, r.entry);
        worldBytes += r.entry.bytes;
        enforce(CACHE, true);
        return r.entry.id;
    }

    private static Res build(int n) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return Res.TRANSIENT;                    // teardown — never cache a negative
        ResourceManager rm = mc.getResourceManager();
        // Only render off-atlas when the pack marked this slot's model invisible (no "parent"). A normal
        // atlas block (cube_all / per-face / shape / cross) keeps its model and is left to the atlas.
        Optional<Resource> modelRes = rm.getResource(Identifier.of(MOD_ID, "models/block/slot_" + n + ".json"));
        if (modelRes.isEmpty()) return Res.TRANSIENT;            // model not written yet (mid-regen) — retry
        Boolean offAtlas = isOffAtlasModel(modelRes.get());
        if (offAtlas == null) return Res.TRANSIENT;              // model unreadable/partial — retry
        if (!offAtlas) return Res.STABLE;                        // has "parent" → atlas-rendered, a stable fact
        // An animated GRID is roughly square (h/w ≈ 1) so the strip-ratio check below won't catch it — its
        // grid sidecar marks it as AnimFrameCache's, so bail and never draw it as a single static texture.
        if (rm.getResource(Identifier.of(MOD_ID, "textures/block/slot_" + n + ".grid.json")).isPresent()) return Res.STABLE;

        Identifier texPath = Identifier.of(MOD_ID, "textures/block/slot_" + n + ".png");
        Optional<Resource> res = rm.getResource(texPath);
        if (res.isEmpty()) return Res.TRANSIENT;                 // png not written yet (mid-regen) — retry

        NativeImage img;
        try (InputStream in = res.get().getInputStream()) {
            img = NativeImage.read(in);
        } catch (Exception e) {
            return Res.TRANSIENT;                                // partial/locked file mid-write — retry
        }
        int w = img.getWidth(), h = img.getHeight();
        if (w <= 0 || h <= 0) { img.close(); return Res.TRANSIENT; } // degenerate decode — treat as mid-write
        if (h / w > 1) { img.close(); return Res.STABLE; } // multi-frame strip → animated (AnimFrameCache owns it)

        // Step 2: flatten transparent/letterbox pixels onto black (off-atlas has no solid cube behind it).
        // Skipped in transparent mode (/cb config transparent) so those pixels stay see-through.
        if (!OffAtlasBgState.isTransparent()) OffAtlasImage.compositeOverBlack(img);

        Identifier texId = Identifier.of(MOD_ID, "static_slot_dyn_" + n);
        NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
        tex.setFilter(true, false); // linear/smooth, NO mipmap — the locked decision (full-res, no atlas pre-shrink)
        mc.getTextureManager().registerTexture(texId, tex);
        return Res.ok(new Entry(texId, decodedBytes(w, h)));
    }

    /**
     * Parse an already-fetched {@code models/block/slot_N.json}: TRUE = invisible off-atlas marker (no
     * {@code "parent"}), FALSE = a normal atlas model (has {@code "parent"}), NULL = unreadable/partial JSON
     * (mid-write — the caller treats this as transient, not a stable verdict).
     */
    private static Boolean isOffAtlasModel(Resource modelRes) {
        try {
            String json;
            try (InputStream in = modelRes.getInputStream()) {
                json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return !root.has("parent"); // invisible model omits "parent"; every atlas model sets one
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Group 30 (Guess Mode) icon fallback: the real {@code slot_N.png} cube for a slot whose ITEM model is
     * {@code builtin/entity} but whose WORLD block model is still a plain atlas {@code cube_all} (the common
     * static single-texture block — see ServerPackGenerator). Unlike {@link #get}, this skips the
     * {@link #isOffAtlasModel} gate entirely: it doesn't matter that the world model still has a "parent",
     * because this texture is only ever used to draw the ITEM icon (hand/inventory/frame/dropped), which no
     * longer has its own baked atlas icon once the item model is builtin/entity. Visually identical to the
     * cube_all icon it replaces. Rejects a multi-frame strip (animated slots have their own item path already).
     * Separate cache from {@link #get} so this never marks a slot NOT_OFFATLAS for world-render purposes.
     */
    public static Identifier getIconFallback(int slotIndex) {
        if (slotIndex < 0) return null;
        Entry cached = ICON_CACHE.get(slotIndex);
        if (cached != null) return cached.id;
        if (ICON_NOT_READABLE.contains(slotIndex)) return null;
        Res r = buildIconFallback(slotIndex);
        if (r.entry == null) { if (r.stableNegative) ICON_NOT_READABLE.add(slotIndex); return null; }
        ICON_CACHE.put(slotIndex, r.entry);
        iconBytes += r.entry.bytes;
        enforce(ICON_CACHE, false);
        return r.entry.id;
    }

    private static Res buildIconFallback(int n) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return Res.TRANSIENT;                    // teardown — never cache a negative
        ResourceManager rm = mc.getResourceManager();
        // A grid slot is animated (AnimFrameCache draws its icon); the plain fallback never applies. Stable.
        if (rm.getResource(Identifier.of(MOD_ID, "textures/block/slot_" + n + ".grid.json")).isPresent()) return Res.STABLE;

        Identifier texPath = Identifier.of(MOD_ID, "textures/block/slot_" + n + ".png");
        Optional<Resource> res = rm.getResource(texPath);
        if (res.isEmpty()) return Res.TRANSIENT;                 // png not written yet (mid-regen) — retry

        NativeImage img;
        try (InputStream in = res.get().getInputStream()) {
            img = NativeImage.read(in);
        } catch (Exception e) {
            return Res.TRANSIENT;                                // partial/locked file mid-write — retry
        }
        int w = img.getWidth(), h = img.getHeight();
        if (w <= 0 || h <= 0) { img.close(); return Res.TRANSIENT; } // degenerate decode — treat as mid-write
        if (h / w > 1) { img.close(); return Res.STABLE; } // multi-frame strip — animated, not this slot's job

        if (!OffAtlasBgState.isTransparent()) OffAtlasImage.compositeOverBlack(img);

        Identifier texId = Identifier.of(MOD_ID, "static_slot_icon_" + n);
        NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
        tex.setFilter(true, false);
        mc.getTextureManager().registerTexture(texId, tex);
        return Res.ok(new Entry(texId, decodedBytes(w, h)));
    }

    /**
     * True when the pack carries any per-face texture ({@code slot_N_<face>.png}) for this slot — i.e. the
     * slot is PAINTED. A painted (or rotated) shaped slot keeps its atlas cube icon (the §B trade: its
     * per-face art can't be reproduced from the single base texture the shaped-icon path draws with), so
     * {@link ShapedItemIcon} uses this to skip those. Cached because it is polled per item, per frame; the
     * cache clears with the texture caches on every resource reload, so a slot that gains/loses face art
     * after a pack rebuild is re-read. Not cached while the client is tearing down (mc == null) so a
     * transient miss can't stick as a false negative.
     */
    public static boolean hasPerFace(int slotIndex) {
        if (slotIndex < 0) return false;
        Boolean cached = PER_FACE.get(slotIndex);
        if (cached != null) return cached;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return false; // teardown — don't cache; re-read once the client is back
        ResourceManager rm = mc.getResourceManager();
        boolean has = false;
        for (String face : TextureStore.FACES) {
            if (rm.getResource(Identifier.of(MOD_ID, "textures/block/slot_" + slotIndex + "_" + face + ".png")).isPresent()) {
                has = true;
                break;
            }
        }
        PER_FACE.put(slotIndex, has);
        return has;
    }

    /** RGBA decoded footprint of a w×h image (native memory held by the NativeImage). */
    private static long decodedBytes(int w, int h) {
        return (long) w * h * 4L;
    }

    /**
     * Evict the least-recently-used entries (access-order eldest) until the cache is back within BOTH the
     * entry and byte ceilings. Each eviction destroys the GL texture (which closes its backing NativeImage,
     * freeing the native bytes) — the slot is simply rebuilt from the pack when next drawn.
     */
    private static void enforce(Map<Integer, Entry> cache, boolean world) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean evicted = false;
        Iterator<Map.Entry<Integer, Entry>> it = cache.entrySet().iterator();
        while (it.hasNext() && cache.size() > 1
                && (cache.size() > MAX_ENTRIES || (world ? worldBytes : iconBytes) > MAX_BYTES)) {
            Entry e = it.next().getValue();
            if (mc != null) {
                try { mc.getTextureManager().destroyTexture(e.id); } catch (Exception ignored) {}
            }
            if (world) worldBytes -= e.bytes; else iconBytes -= e.bytes;
            it.remove();
            evicted = true;
        }
        if (evicted) logStatsThrottled();
    }

    /** Throttled one-line readout of both caches — puts the G05 §H counters into the log for review (§H8). */
    private static void logStatsThrottled() {
        long now = System.currentTimeMillis();
        if (now - lastLogMs < LOG_THROTTLE_MS) return;
        lastLogMs = now;
        CustomBlocksMod.LOGGER.info("[CustomBlocks] G05 static cache: {}", stats());
    }

    /** Live counters for diagnostics: world/icon entry counts and MiB against their ceilings. */
    public static String stats() {
        return String.format("world %d/%d entries %.1f/%d MiB, icon %d/%d entries %.1f/%d MiB",
                CACHE.size(), MAX_ENTRIES, worldBytes / (1024.0 * 1024), MAX_BYTES / (1024 * 1024),
                ICON_CACHE.size(), MAX_ENTRIES, iconBytes / (1024.0 * 1024), MAX_BYTES / (1024 * 1024));
    }

    /** Drop every cached texture (on resource reload / re-skin) so the next render re-reads fresh. */
    public static void clear() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            for (Entry e : CACHE.values()) {
                try { mc.getTextureManager().destroyTexture(e.id); } catch (Exception ignored) {}
            }
            for (Entry e : ICON_CACHE.values()) {
                try { mc.getTextureManager().destroyTexture(e.id); } catch (Exception ignored) {}
            }
        }
        CACHE.clear();
        ICON_CACHE.clear();
        NOT_OFFATLAS.clear();
        ICON_NOT_READABLE.clear();
        PER_FACE.clear();
        worldBytes = 0;
        iconBytes = 0;
    }
}
