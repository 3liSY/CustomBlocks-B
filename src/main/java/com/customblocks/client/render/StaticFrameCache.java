/**
 * StaticFrameCache.java — Group 14 / Phase 1c. CLIENT-ONLY.
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
 * Depends on: MinecraftClient (resource + texture managers), the pack (slot_N.png + slot_N.json model).
 * Called by:  AnimSlotBER (placed render), SlotItemRenderer (hand/inventory), ResourcePackGenerator +
 *             ClientPackReceiver (clear on reload).
 *
 * See: docs/adr/ADR-008 (Path B, off-atlas) + GROUP_14 Phase 1c.
 */
package com.customblocks.client.render;

import com.customblocks.CustomBlocksMod;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class StaticFrameCache {

    private StaticFrameCache() {} // static-only

    private static final String MOD_ID = CustomBlocksMod.MOD_ID;

    /** slot → uploaded off-atlas texture id. Built once, then reused. */
    private static final Map<Integer, Identifier> CACHE = new HashMap<>();
    /** Slots we've decided are NOT off-atlas (atlas-rendered / animated / unreadable) — don't retry. */
    private static final Set<Integer> NOT_OFFATLAS = new HashSet<>();
    /** slot → uploaded texture id for the Group 30 icon fallback (see {@link #getIconFallback}). Separate
     *  from CACHE/NOT_OFFATLAS above so it never affects the world-render off-atlas gate. */
    private static final Map<Integer, Identifier> ICON_CACHE = new HashMap<>();
    private static final Set<Integer> ICON_NOT_READABLE = new HashSet<>();

    /** Off-atlas texture id for a STATIC off-atlas slot, or null when the slot is atlas-rendered/animated. */
    public static Identifier get(int slotIndex) {
        if (slotIndex < 0) return null;
        Identifier cached = CACHE.get(slotIndex);
        if (cached != null) return cached;
        if (NOT_OFFATLAS.contains(slotIndex)) return null;
        Identifier id = build(slotIndex);
        if (id == null) { NOT_OFFATLAS.add(slotIndex); return null; }
        CACHE.put(slotIndex, id);
        return id;
    }

    private static Identifier build(int n) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ResourceManager rm = mc.getResourceManager();
        // Only render off-atlas when the pack marked this slot's model invisible (no "parent"). A normal
        // atlas block (cube_all / per-face / shape / cross) keeps its model and is left to the atlas.
        if (!isOffAtlasModel(rm, n)) return null;
        // An animated GRID is roughly square (h/w ≈ 1) so the strip-ratio check below won't catch it — its
        // grid sidecar marks it as AnimFrameCache's, so bail and never draw it as a single static texture.
        if (rm.getResource(Identifier.of(MOD_ID, "textures/block/slot_" + n + ".grid.json")).isPresent()) return null;

        Identifier texPath = Identifier.of(MOD_ID, "textures/block/slot_" + n + ".png");
        Optional<Resource> res = rm.getResource(texPath);
        if (res.isEmpty()) return null;

        NativeImage img;
        try (InputStream in = res.get().getInputStream()) {
            img = NativeImage.read(in);
        } catch (Exception e) {
            return null;
        }
        int w = img.getWidth(), h = img.getHeight();
        if (w <= 0 || h <= 0) { img.close(); return null; }
        if (h / w > 1) { img.close(); return null; } // multi-frame strip → animated (AnimFrameCache owns it)

        // Step 2: flatten transparent/letterbox pixels onto black (off-atlas has no solid cube behind it).
        // Skipped in transparent mode (/cb config transparent) so those pixels stay see-through.
        if (!OffAtlasBgState.isTransparent()) OffAtlasImage.compositeOverBlack(img);

        Identifier texId = Identifier.of(MOD_ID, "static_slot_dyn_" + n);
        NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
        tex.setFilter(true, false); // linear/smooth, NO mipmap — the locked decision (full-res, no atlas pre-shrink)
        mc.getTextureManager().registerTexture(texId, tex);
        return texId;
    }

    /** True when models/block/slot_N.json is the invisible off-atlas marker (no {@code "parent"} key). */
    private static boolean isOffAtlasModel(ResourceManager rm, int n) {
        try {
            Identifier modelPath = Identifier.of(MOD_ID, "models/block/slot_" + n + ".json");
            Optional<Resource> res = rm.getResource(modelPath);
            if (res.isEmpty()) return false;
            String json;
            try (InputStream in = res.get().getInputStream()) {
                json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return !root.has("parent"); // invisible model omits "parent"; every atlas model sets one
        } catch (Exception e) {
            return false;
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
        Identifier cached = ICON_CACHE.get(slotIndex);
        if (cached != null) return cached;
        if (ICON_NOT_READABLE.contains(slotIndex)) return null;
        Identifier id = buildIconFallback(slotIndex);
        if (id == null) { ICON_NOT_READABLE.add(slotIndex); return null; }
        ICON_CACHE.put(slotIndex, id);
        return id;
    }

    private static Identifier buildIconFallback(int n) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ResourceManager rm = mc.getResourceManager();
        if (rm.getResource(Identifier.of(MOD_ID, "textures/block/slot_" + n + ".grid.json")).isPresent()) return null;

        Identifier texPath = Identifier.of(MOD_ID, "textures/block/slot_" + n + ".png");
        Optional<Resource> res = rm.getResource(texPath);
        if (res.isEmpty()) return null;

        NativeImage img;
        try (InputStream in = res.get().getInputStream()) {
            img = NativeImage.read(in);
        } catch (Exception e) {
            return null;
        }
        int w = img.getWidth(), h = img.getHeight();
        if (w <= 0 || h <= 0) { img.close(); return null; }
        if (h / w > 1) { img.close(); return null; } // multi-frame strip — not this slot's job

        if (!OffAtlasBgState.isTransparent()) OffAtlasImage.compositeOverBlack(img);

        Identifier texId = Identifier.of(MOD_ID, "static_slot_icon_" + n);
        NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
        tex.setFilter(true, false);
        mc.getTextureManager().registerTexture(texId, tex);
        return texId;
    }

    /** Drop every cached texture (on resource reload / re-skin) so the next render re-reads fresh. */
    public static void clear() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            for (Identifier id : CACHE.values()) {
                try { mc.getTextureManager().destroyTexture(id); } catch (Exception ignored) {}
            }
            for (Identifier id : ICON_CACHE.values()) {
                try { mc.getTextureManager().destroyTexture(id); } catch (Exception ignored) {}
            }
        }
        CACHE.clear();
        NOT_OFFATLAS.clear();
        ICON_CACHE.clear();
        ICON_NOT_READABLE.clear();
    }
}
