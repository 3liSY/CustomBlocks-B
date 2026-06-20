/**
 * AnimFrameCache.java — Group 14 / Phase 1b. CLIENT-ONLY.
 *
 * Holds, per animated slot, ONE off-atlas GL texture of the slot's full-resolution vertical frame-strip
 * (the same {@code slot_N.png} the pack ships) plus its playback order/timing. The placed block is drawn
 * by {@link AnimSlotBER} straight from this texture with mipmaps OFF — so it stays crisp instead of being
 * muffled by Minecraft's block atlas (which mipmaps + downscales tall strips). This is exactly how vanilla
 * animates a texture (one strip, shift the V coordinate per frame), but bypassing the atlas.
 *
 * Why off-atlas fixes the muffle: the baked strip PNG is crisp; the atlas degrades it (mipmaps + atlas
 * overflow from tall strips). Reading the RAW pack file and uploading it as its own NativeImageBackedTexture
 * (nearest filter, no mipmap) shows the crisp pixels. Inventory/hand still use the atlas (fine at icon size).
 *
 * Everything is read from the PACK (strip dimensions + {@code slot_N.png.mcmeta}) — no server sync needed.
 * The cache is cleared on every resource reload (re-skin / pack rebuild) so a changed strip re-reads fresh.
 *
 * Depends on: MinecraftClient (resource manager + texture manager), the pack (slot_N.png + .mcmeta).
 * Called by:  AnimSlotBER (render), ResourcePackGenerator (clear on reload).
 */
package com.customblocks.client.render;

import com.customblocks.CustomBlocksMod;
import com.google.gson.JsonArray;
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
public final class AnimFrameCache {

    private AnimFrameCache() {} // static-only

    private static final String MOD_ID = CustomBlocksMod.MOD_ID;
    /** Ticks-per-frame when a strip has no mcmeta timing at all (matches AnimData.FALLBACK_TICKS). */
    private static final int FALLBACK_TICKS = 2;

    /** One animated slot's GL texture + its playback sequence (strip index + ticks per displayed step). */
    public static final class Slot {
        public final Identifier textureId;
        public final int frameCount;   // rows in the strip (height / width)
        private final int[] order;     // strip index shown at each playback step
        private final int[] times;     // ticks each playback step is held
        private final int totalTicks;

        private Slot(Identifier textureId, int frameCount, int[] order, int[] times) {
            this.textureId = textureId;
            this.frameCount = frameCount;
            this.order = order;
            this.times = times;
            int sum = 0;
            for (int t : times) sum += Math.max(1, t);
            this.totalTicks = Math.max(1, sum);
        }

        /** The strip row to display right now (world time + partial tick, wrapped over the loop). */
        public int currentStripIndex(long worldTime, float tickDelta) {
            if (order.length == 0) return 0;
            double pos = ((worldTime % totalTicks) + tickDelta) % totalTicks;
            if (pos < 0) pos += totalTicks;
            double acc = 0;
            for (int i = 0; i < order.length; i++) {
                acc += Math.max(1, times[i]);
                if (pos < acc) return order[i];
            }
            return order[order.length - 1];
        }
    }

    private static final Map<Integer, Slot> CACHE = new HashMap<>();
    /** Slots we've decided are NOT animated (or failed to read) — so we don't retry every frame. */
    private static final Set<Integer> NOT_ANIMATED = new HashSet<>();

    /** Cached Slot for an animated block, or null if the slot is static / unreadable. */
    public static Slot get(int slotIndex) {
        if (slotIndex < 0) return null;
        Slot s = CACHE.get(slotIndex);
        if (s != null) return s;
        if (NOT_ANIMATED.contains(slotIndex)) return null;
        s = build(slotIndex);
        if (s == null) { NOT_ANIMATED.add(slotIndex); return null; }
        CACHE.put(slotIndex, s);
        return s;
    }

    private static Slot build(int n) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ResourceManager rm = mc.getResourceManager();
        Identifier stripPath = Identifier.of(MOD_ID, "textures/block/slot_" + n + ".png");
        Optional<Resource> res = rm.getResource(stripPath);
        if (res.isEmpty()) return null;

        NativeImage img;
        try (InputStream in = res.get().getInputStream()) {
            img = NativeImage.read(in);
        } catch (Exception e) {
            return null;
        }
        int w = img.getWidth(), h = img.getHeight();
        int frames = (w > 0) ? h / w : 0;
        if (frames <= 1) { img.close(); return null; } // single frame → static block, atlas handles it

        Identifier texId = Identifier.of(MOD_ID, "anim_slot_dyn_" + n);
        NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
        tex.setFilter(false, false); // nearest, NO mipmap — the crisp, un-muffled path
        mc.getTextureManager().registerTexture(texId, tex);

        int[][] pb = readPlayback(rm, n, frames);
        return new Slot(texId, frames, pb[0], pb[1]);
    }

    /**
     * Read the slot's {@code .mcmeta} animation block into {order[], times[]} (the displayed strip index +
     * ticks at each step — loop/bounce/reverse are already baked into the frames array by the pack builder).
     * Falls back to a plain forward 0..frames-1 at the default frametime if the mcmeta is missing/odd.
     */
    private static int[][] readPlayback(ResourceManager rm, int n, int frames) {
        try {
            Identifier metaPath = Identifier.of(MOD_ID, "textures/block/slot_" + n + ".png.mcmeta");
            Optional<Resource> res = rm.getResource(metaPath);
            if (res.isPresent()) {
                String json;
                try (InputStream in = res.get().getInputStream()) {
                    json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                JsonObject anim = root.getAsJsonObject("animation");
                if (anim != null) {
                    int base = anim.has("frametime") ? anim.get("frametime").getAsInt() : FALLBACK_TICKS;
                    JsonArray fr = anim.has("frames") ? anim.getAsJsonArray("frames") : null;
                    if (fr != null && fr.size() > 0) {
                        int[] order = new int[fr.size()];
                        int[] times = new int[fr.size()];
                        for (int i = 0; i < fr.size(); i++) {
                            JsonObject f = fr.get(i).getAsJsonObject();
                            int idx = f.has("index") ? f.get("index").getAsInt() : i;
                            order[i] = Math.max(0, Math.min(frames - 1, idx));
                            times[i] = f.has("time") ? Math.max(1, f.get("time").getAsInt()) : Math.max(1, base);
                        }
                        return new int[][]{order, times};
                    }
                    return sequential(frames, Math.max(1, base));
                }
            }
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Anim mcmeta read failed for slot {}: {}", n, e.toString());
        }
        return sequential(frames, FALLBACK_TICKS);
    }

    private static int[][] sequential(int frames, int ticks) {
        int[] order = new int[frames];
        int[] times = new int[frames];
        for (int i = 0; i < frames; i++) { order[i] = i; times[i] = ticks; }
        return new int[][]{order, times};
    }

    /** Drop every cached texture (on resource reload / re-skin) so the next render re-reads fresh strips. */
    public static void clear() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            for (Slot s : CACHE.values()) {
                try { mc.getTextureManager().destroyTexture(s.textureId); } catch (Exception ignored) {}
            }
        }
        CACHE.clear();
        NOT_ANIMATED.clear();
    }
}
