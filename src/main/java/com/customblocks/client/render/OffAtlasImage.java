/**
 * OffAtlasImage.java — Group 14 / Phase 1c Step 2. CLIENT-ONLY.
 *
 * Off-atlas blocks are drawn straight from their NativeImage with an alpha-tested cutout layer, so any
 * transparent pixel in the baked texture shows the WORLD through the block. The old atlas cube was solid,
 * so its transparent areas read as a black backdrop. A baked slot texture has transparent pixels by design
 * — aspect-ratio letterbox padding (ImageProcessor) plus any transparent area of the source image — so off
 * atlas those go see-through. Owner wants them BLACK by default.
 *
 * This flattens a NativeImage onto an opaque black backdrop in place: each pixel's RGB is scaled by its own
 * alpha and the alpha is forced to 255. A pixel that was already opaque (a baked background colour, an
 * opaque photo) is left exactly as it was, so the bg-colour feature still shows its colour; only genuinely
 * transparent / semi-transparent pixels darken toward black. Crispness is untouched (no resize/filter).
 *
 * Depends on: NativeImage (RGBA, packed ABGR).
 * Called by:  StaticFrameCache.build, AnimFrameCache.build (before upload).
 */
package com.customblocks.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;

@Environment(EnvType.CLIENT)
public final class OffAtlasImage {

    private OffAtlasImage() {} // static-only

    /**
     * Composite {@code img} over opaque black in place: out = src * (a/255), alpha = 255. Opaque pixels are
     * unchanged. NativeImage packs colour as ABGR (0xAABBGGRR).
     */
    public static void compositeOverBlack(NativeImage img) {
        if (img == null) return;
        int w = img.getWidth(), h = img.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = img.getColor(x, y);
                int a = (c >> 24) & 0xFF;
                if (a == 255) continue; // already opaque — keep the baked colour exactly
                int b = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int r = c & 0xFF;
                r = r * a / 255;
                g = g * a / 255;
                b = b * a / 255;
                img.setColor(x, y, (0xFF << 24) | (b << 16) | (g << 8) | r);
            }
        }
    }
}
