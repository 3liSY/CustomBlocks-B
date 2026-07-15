/**
 * AnimFrameCache.java — Group 14 / ADR-014 Step 3 slice 1 (per-frame upload + pool). CLIENT-ONLY.
 *
 * Holds, per animated slot, the slot's full-resolution frame GRID in RAM (the {@code slot_N.png} the pack
 * ships) plus a SMALL per-slot GL texture sized to exactly ONE cell. Each render the cell for the current
 * playback frame is blitted out of the RAM grid into that one-cell texture and re-uploaded ({@link
 * AnimSlotBER} then draws the whole cell, UV 0..1). So the GPU only ever holds ONE frame per on-screen
 * slot instead of the entire grid — VRAM is bounded by what's on screen (ADR-014 Step 3 §3): a long clip no
 * longer parks a multi-MB grid texture on the GPU for its whole life, and the half-texel cell inset the
 * grid-sampling needed is gone (a one-cell texture has no neighbour to bleed in from).
 *
 * Pooling: a slot not drawn for {@link #EVICT_MS} is disposed (RAM grid closed, GL texture destroyed) and
 * rebuilt from the pack when it next comes on screen — so off-screen animated blocks hold neither RAM nor
 * VRAM. The sweep is throttled ({@link #SWEEP_INTERVAL_MS}) so it costs almost nothing per frame.
 *
 * Mipmaps stay OFF here (linear, no mipmap) exactly as Step 2 — the distance-shimmer mipmap fix is Step 3
 * slice 2 and needs a custom render layer (Minecraft's entity render layer re-disables the texture filter on
 * every draw, so a flag flip here would do nothing). This slice is the foundation that makes that safe.
 *
 * Layout / timing / sidecar reading are unchanged from Step 2: a grid sidecar ({@code slot_N.grid.json})
 * wins, else a legacy vertical strip + {@code slot_N.png.mcmeta} (cols = 1) is still read; per-frame times
 * are MILLISECONDS played on the {@link AnimClock} wall-clock. Everything is read from the PACK (image +
 * sidecar) — no server sync — so it works on a dedicated server too. Cleared on every resource reload.
 *
 * Depends on: MinecraftClient (resource + texture managers), the pack (slot_N.png + .grid.json/.mcmeta), AnimClock.
 * Called by:  AnimSlotBER (render), SlotItemRenderer (icon), ResourcePackGenerator (clear on reload).
 */
package com.customblocks.client.render;

import com.customblocks.CustomBlocksMod;
import com.customblocks.image.AnimationDecoder;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class AnimFrameCache {

    private AnimFrameCache() {} // static-only

    private static final String MOD_ID = CustomBlocksMod.MOD_ID;
    /** Real milliseconds per game tick — converts a legacy tick time into the ms clock. */
    private static final int TICK_MS = 50;
    /** Milliseconds-per-frame when a clip has no sidecar timing at all (matches AnimData.FALLBACK_MS). */
    private static final int FALLBACK_MS = 100;
    /** A slot not drawn for this long is pooled out (RAM grid + GL texture freed), rebuilt when re-seen. */
    private static final long EVICT_MS = 8_000;
    /** Don't sweep the pool more than this often — keeps get() effectively free per frame. */
    private static final long SWEEP_INTERVAL_MS = 2_000;

    /**
     * One animated slot: its full grid in RAM, a one-cell GL texture re-uploaded per frame, plus the
     * playback order/timing (frame index + MILLISECONDS per step).
     */
    public static final class Slot {
        private final Identifier textureId;
        private final NativeImageBackedTexture frameTex; // one-cell GPU texture, re-uploaded each frame change
        private final NativeImage grid;                  // full frame grid kept in RAM, blitted from
        private final int count;       // total frames in the grid
        private final int cols;        // grid columns (1 = legacy vertical strip)
        private final int cell;        // cell size in px (square)
        private final int imgW, imgH;  // grid texture dimensions
        private final int[] order;     // frame index shown at each playback step
        private final int[] times;     // MILLISECONDS each playback step is held
        private final long totalMs;
        private int lastUploadedFrame = -1; // which frame the one-cell texture currently holds
        private long lastSeenRealMs;        // wall-clock of the last draw — drives pool eviction

        private Slot(Identifier textureId, NativeImageBackedTexture frameTex, NativeImage grid,
                     int count, int cols, int cell, int imgW, int imgH, int[] order, int[] times) {
            this.textureId = textureId;
            this.frameTex = frameTex;
            this.grid = grid;
            this.count = count;
            this.cols = Math.max(1, cols);
            this.cell = Math.max(1, cell);
            this.imgW = Math.max(1, imgW);
            this.imgH = Math.max(1, imgH);
            this.order = order;
            this.times = times;
            long sum = 0;
            for (int t : times) sum += Math.max(1, t);
            this.totalMs = Math.max(1, sum);
            this.lastSeenRealMs = System.currentTimeMillis();
        }

        /** The frame index to display right now, given the millisecond animation clock, wrapped over the loop. */
        private int currentFrame(long nowMs) {
            if (order.length == 0) return 0;
            long pos = nowMs % totalMs;
            if (pos < 0) pos += totalMs;
            long acc = 0;
            for (int i = 0; i < order.length; i++) {
                acc += Math.max(1, times[i]);
                if (pos < acc) return order[i];
            }
            return order[order.length - 1];
        }

        /**
         * Make the one-cell GPU texture show the current playback frame, then return its id to bind (drawn
         * full, UV 0..1). Blits + re-uploads only when the displayed frame actually changes, so several
         * blocks of this slot in one render pass upload at most once. Marks the slot on-screen so the pool
         * sweep keeps it resident. Runs on the render thread (GL context present).
         */
        public Identifier prepare(long nowMs) {
            this.lastSeenRealMs = System.currentTimeMillis();
            int f = currentFrame(nowMs);
            if (f != lastUploadedFrame) {
                blitCell(f);
                frameTex.upload();
                lastUploadedFrame = f;
            }
            return textureId;
        }

        /** Copy the grid cell for {@code frame} into the one-cell image (source coords clamped for safety). */
        private void blitCell(int frame) {
            int fIdx = Math.max(0, Math.min(count - 1, frame));
            int srcX = (fIdx % cols) * cell, srcY = (fIdx / cols) * cell;
            NativeImage dst = frameTex.getImage();
            if (dst == null) return;
            for (int yy = 0; yy < cell; yy++) {
                int sy = Math.min(srcY + yy, imgH - 1);
                for (int xx = 0; xx < cell; xx++) {
                    dst.setColor(xx, yy, grid.getColor(Math.min(srcX + xx, imgW - 1), sy));
                }
            }
        }

        /** Free this slot's GL texture and RAM grid. Client thread only (texture manager / NativeImage). */
        private void dispose() {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) {
                try { mc.getTextureManager().destroyTexture(textureId); } catch (Exception ignored) {}
            }
            try { grid.close(); } catch (Exception ignored) {}
        }
    }

    private static final Map<Integer, Slot> CACHE = new HashMap<>();
    /** Slots we've decided are NOT animated (or failed to read) — so we don't retry every frame. */
    private static final Set<Integer> NOT_ANIMATED = new HashSet<>();
    private static long lastSweepMs = 0;

    /** Cached Slot for an animated block, or null if the slot is static / unreadable. */
    public static Slot get(int slotIndex) {
        if (slotIndex < 0) return null;
        sweepIfDue();
        Slot s = CACHE.get(slotIndex);
        if (s != null) return s;
        if (NOT_ANIMATED.contains(slotIndex)) return null;
        s = build(slotIndex);
        if (s == null) { NOT_ANIMATED.add(slotIndex); return null; }
        CACHE.put(slotIndex, s);
        return s;
    }

    /** Pool out slots not drawn for {@link #EVICT_MS}; throttled to {@link #SWEEP_INTERVAL_MS}. */
    private static void sweepIfDue() {
        long now = System.currentTimeMillis();
        if (now - lastSweepMs < SWEEP_INTERVAL_MS) return;
        lastSweepMs = now;
        Iterator<Map.Entry<Integer, Slot>> it = CACHE.entrySet().iterator();
        while (it.hasNext()) {
            Slot s = it.next().getValue();
            if (now - s.lastSeenRealMs > EVICT_MS) { s.dispose(); it.remove(); }
        }
    }

    private static Slot build(int n) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ResourceManager rm = mc.getResourceManager();
        Identifier imgPath = Identifier.of(MOD_ID, "textures/block/slot_" + n + ".png");
        Optional<Resource> res = rm.getResource(imgPath);
        if (res.isEmpty()) return null;

        NativeImage img;
        try (InputStream in = res.get().getInputStream()) {
            img = NativeImage.read(in);
        } catch (Exception e) {
            return null;
        }
        int w = img.getWidth(), h = img.getHeight();
        if (w <= 0 || h <= 0) { img.close(); return null; }

        // Layout: a grid sidecar (new) wins; else a legacy vertical strip (slot_N.png.mcmeta, cols = 1).
        int count, cols;
        int[][] pb;
        JsonObject grid = readGridSidecar(rm, n);
        if (grid != null) {
            count = grid.has("count") ? grid.get("count").getAsInt() : 0;
            cols = grid.has("cols") ? grid.get("cols").getAsInt() : AnimationDecoder.gridCols(count);
            // "timebase":"ms" → times are real milliseconds; absent (pre-ADR-014 sidecar) → ticks, ×50 to ms.
            boolean msUnit = grid.has("timebase") && "ms".equals(grid.get("timebase").getAsString());
            pb = readFrames(grid, count, msUnit);
        } else {
            count = h / w;                 // vertical strip: one column, N rows
            cols = 1;
            pb = readMcmeta(rm, n, count);
        }
        if (count <= 1 || cols < 1) { img.close(); return null; } // single frame → static (atlas handles it)
        int cell = w / cols;
        if (cell <= 0) { img.close(); return null; }

        // Flatten transparent pixels (whole image) onto black — same as the static path. Skipped in
        // transparent mode (/cb config transparent) so those pixels stay see-through.
        if (!OffAtlasBgState.isTransparent()) OffAtlasImage.compositeOverBlack(img);

        // One-cell GPU texture, re-uploaded per frame from the RAM grid (img). Linear, NO mipmap (slice 1;
        // mipmaps = slice 2). The grid (img) is kept in RAM by the Slot — NOT closed here, NOT uploaded whole.
        Identifier texId = Identifier.of(MOD_ID, "anim_slot_dyn_" + n);
        NativeImageBackedTexture frameTex = new NativeImageBackedTexture(new NativeImage(cell, cell, false));
        frameTex.setFilter(true, false); // linear/smooth, NO mipmap — full-res, no atlas pre-shrink
        mc.getTextureManager().registerTexture(texId, frameTex);

        return new Slot(texId, frameTex, img, count, cols, cell, w, h, pb[0], pb[1]);
    }

    /** Read {@code slot_N.grid.json} (the new grid layout + playback), or null if absent/unreadable. */
    private static JsonObject readGridSidecar(ResourceManager rm, int n) {
        try {
            Identifier path = Identifier.of(MOD_ID, "textures/block/slot_" + n + ".grid.json");
            Optional<Resource> res = rm.getResource(path);
            if (res.isEmpty()) return null;
            String json;
            try (InputStream in = res.get().getInputStream()) {
                json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse the {frames:[{index,time}]} playback from a sidecar object into {order[], times[]}, with times
     * returned in MILLISECONDS. {@code msUnit} = the sidecar's times are already ms (grid {@code timebase:ms});
     * false = they are ticks (legacy grid / vanilla mcmeta) and get ×50 to ms.
     */
    private static int[][] readFrames(JsonObject root, int count, boolean msUnit) {
        int mul = msUnit ? 1 : TICK_MS;
        int base = root.has("frametime") ? Math.max(1, root.get("frametime").getAsInt()) * mul : FALLBACK_MS;
        JsonArray fr = root.has("frames") ? root.getAsJsonArray("frames") : null;
        if (fr == null || fr.size() == 0) return sequential(count, base);
        int[] order = new int[fr.size()];
        int[] times = new int[fr.size()];
        for (int i = 0; i < fr.size(); i++) {
            JsonObject f = fr.get(i).getAsJsonObject();
            int idx = f.has("index") ? f.get("index").getAsInt() : i;
            order[i] = Math.max(0, Math.min(count - 1, idx));
            times[i] = f.has("time") ? Math.max(1, f.get("time").getAsInt()) * mul : base;
        }
        return new int[][]{order, times};
    }

    /** Legacy: read the vanilla {@code slot_N.png.mcmeta} animation block (vertical-strip blocks; ticks → ms). */
    private static int[][] readMcmeta(ResourceManager rm, int n, int frames) {
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
                if (anim != null) return readFrames(anim, frames, false); // mcmeta times are ticks
            }
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Anim mcmeta read failed for slot {}: {}", n, e.toString());
        }
        return sequential(frames, FALLBACK_MS);
    }

    private static int[][] sequential(int frames, int ms) {
        int n = Math.max(1, frames);
        int[] order = new int[n];
        int[] times = new int[n];
        for (int i = 0; i < n; i++) { order[i] = i; times[i] = Math.max(1, ms); }
        return new int[][]{order, times};
    }

    /** Drop every cached slot (on resource reload / re-skin) so the next render re-reads fresh grids. */
    public static void clear() {
        for (Slot s : CACHE.values()) s.dispose();
        CACHE.clear();
        NOT_ANIMATED.clear();
    }
}
