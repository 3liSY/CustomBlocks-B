/**
 * LocalTexturePreview.java — GROUP 27 UI kit. Shared preview-grid loader. CLIENT-SIDE ONLY.
 *
 * The reliable texture source for the cube screens (fixes F3 "preview unavailable"): instead of
 * re-downloading a block's source URL over HTTP (which 403s / dies on expired links), read the block's
 * ALREADY-baked {@code slot_N.png} straight from the client resource pack — the same file StaticFrameCache
 * renders in-world — and downsample it to a {@link PreviewCube} grid. No network, works on dedicated
 * servers, always loads when the block exists locally. Callers fall back to the URL fetch only when this
 * returns null (block not synced to this client yet).
 *
 * §F3 GIF fix: an animated slot's {@code slot_N.png} is NOT a tall vertical strip — {@link AnimFrameCache}
 * bakes it as a roughly-SQUARE grid ({@code cols = ceil(sqrt(frameCount))}, see
 * {@link com.customblocks.image.AnimationDecoder#gridCols}), so an {@code h > w} guess never catches it and
 * the old code downsampled the whole multi-frame grid as if it were one frame (the garbled GIF preview).
 * This reads the same {@code slot_N.grid.json} sidecar AnimFrameCache reads and crops cell 0, falling back
 * to the legacy vertical-strip mcmeta case, then plain static.
 *
 * Depends on: ClientSlotCache (id→slot), MinecraftClient resource manager, ImageIO, Gson, PreviewCube.
 * Called by: RecolorSliderScreen, ShapeEditorScreen (cube fetch).
 */
package com.customblocks.client.gui;

import com.customblocks.client.ClientSlotCache;
import com.customblocks.image.AnimationDecoder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class LocalTexturePreview {

    private LocalTexturePreview() {} // static-only

    private static final String MOD_ID = "customblocks";

    /** Preview grid for a block from its baked pack texture, or null if not available locally. */
    public static int[] load(String blockId) {
        if (blockId == null || blockId.isBlank()) return null;
        Integer idx = ClientSlotCache.indexForId(blockId);
        if (idx == null) return null;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return null;
        ResourceManager rm = mc.getResourceManager();
        Identifier tex = Identifier.of(MOD_ID, "textures/block/slot_" + idx + ".png");
        Optional<Resource> res = rm.getResource(tex);
        if (res.isEmpty()) return null;
        try (InputStream in = res.get().getInputStream()) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) return null;
            int w = img.getWidth(), h = img.getHeight();
            if (w <= 0 || h <= 0) return null;
            img = cropFrame0(rm, idx, img, w, h);
            return PreviewCube.downsample(img);
        } catch (Exception e) {
            return null;
        }
    }

    /** Crop frame 0 out of an animated grid/strip, or return {@code img} unchanged for a static texture. */
    private static BufferedImage cropFrame0(ResourceManager rm, int idx, BufferedImage img, int w, int h) {
        JsonObject grid = readGridSidecar(rm, idx);
        if (grid != null) {
            int count = grid.has("count") ? grid.get("count").getAsInt() : 0;
            int cols = grid.has("cols") ? grid.get("cols").getAsInt() : AnimationDecoder.gridCols(count);
            if (cols >= 1) {
                int cell = w / cols;
                if (cell > 0 && cell <= w && cell <= h) return img.getSubimage(0, 0, cell, cell);
            }
            return img;
        }
        if (h > w) return img.getSubimage(0, 0, w, w); // legacy vertical-strip mcmeta → frame 0
        return img; // static texture
    }

    /** Read {@code slot_N.grid.json} (same sidecar AnimFrameCache reads), or null if absent/unreadable. */
    private static JsonObject readGridSidecar(ResourceManager rm, int idx) {
        try {
            Identifier path = Identifier.of(MOD_ID, "textures/block/slot_" + idx + ".grid.json");
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
}
