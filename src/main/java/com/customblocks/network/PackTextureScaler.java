/**
 * PackTextureScaler.java — GROUP 05 §F (pack texture resolution clamp/scale). SERVER-SIDE.
 *
 * Responsibility: bound one pack PNG to a target resolution as {@link ServerPackGenerator#emit} writes
 * it. Static block/face textures are clamped to a {@code max}-square; an animated frame GRID (or legacy
 * vertical strip) is scaled uniformly so each FRAME — not just the whole sheet — stays within {@code max}
 * while its grid layout and playback survive. Passing the configured full texture size reproduces the
 * pre-§F output (only a legacy oversized store file is rescued); a smaller {@code max} produces the
 * 128/256 variant. Split out of ServerPackGenerator to keep that file under the §9.3 size gate.
 *
 * Every method is failure-tolerant: on any decode/encode problem it logs and returns the original bytes
 * so one bad texture can never abort a pack build.
 *
 * Depends on: ImageProcessor (square clamp + IHDR read), ImageResampler (Lanczos resize),
 *             AnimationDecoder (grid columns), javax.imageio.
 * Called by:  ServerPackGenerator.emit.
 */
package com.customblocks.network;

import com.customblocks.CustomBlocksMod;
import com.customblocks.image.AnimationDecoder;
import com.customblocks.image.ImageProcessor;
import com.customblocks.image.ImageResampler;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public final class PackTextureScaler {

    private PackTextureScaler() {} // static-only

    /**
     * Downscale {@code png} to a {@code max}-square texture when it exceeds that size on either side;
     * otherwise return it untouched (the common case — one cheap IHDR read, no decode). At the full
     * texture size this only rescues a legacy/oversized store file; at a smaller {@code max} it produces
     * the 128/256 variant. Never throws.
     */
    public static byte[] clampStatic(byte[] png, int max) {
        int w = ImageProcessor.pngWidth(png), h = ImageProcessor.pngHeight(png);
        if (w <= max && h <= max) return png; // within budget (or unreadable → leave it) — no work
        try {
            byte[] fixed = ImageProcessor.toBlockPng(png, max);
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Downscaled block texture {}x{} -> {}px on emit.", w, h, max);
            return fixed;
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Could not downscale oversized texture ({}x{}); shipping as-is.", w, h, e);
            return png;
        }
    }

    /**
     * Scale an animated frame GRID (or legacy vertical strip) uniformly so each FRAME is at most
     * {@code max} on a side, preserving the grid layout (columns unchanged) so the off-atlas renderer
     * still derives every cell's UV from cols + image width. The per-frame side is {@code width / cols};
     * only when that exceeds {@code max} is the whole sheet resampled by the same ratio, keeping aspect
     * and playback intact (§F12). Never throws.
     */
    public static byte[] clampAnimatedGrid(byte[] png, int max, int frameCount) {
        try {
            int cols = stripCols(png, frameCount);
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(png));
            if (src == null || cols < 1) return png; // unreadable → leave it
            int w = src.getWidth(), h = src.getHeight();
            int frameSide = w / cols;
            if (frameSide <= max) return png; // each frame already within budget
            double s = (double) max / frameSide;
            int dw = Math.max(cols, (int) Math.round(w * s)); // keep at least 1px per column
            int dh = Math.max(1, (int) Math.round(h * s));
            BufferedImage out = ImageResampler.resize(toArgb(src), dw, dh);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(out, "png", bos);
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Scaled animated grid {}x{} (frame {}px, cols {}) -> {}px/frame on emit.",
                    w, h, frameSide, cols, max);
            return bos.toByteArray();
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Could not scale animated grid to {}px; shipping full-size.", max, e);
            return png;
        }
    }

    /**
     * Grid columns for a stored animated texture: {@code 1} = a LEGACY vertical strip (one column, N rows),
     * {@code >=2} = a packed grid. Detected from the PNG's own IHDR dimensions: a vertical strip is exactly
     * {@code frameCount}x as tall as it is wide; anything else is a grid (cols = ceil(sqrt(count)), matching
     * the decoder).
     */
    public static int stripCols(byte[] png, int frameCount) {
        if (frameCount <= 1) return 1;
        int w = pngDim(png, 16), h = pngDim(png, 20);   // IHDR: width @16, height @20 (big-endian)
        if (w <= 0 || h <= 0) return 1;                  // unreadable dims → treat as legacy (safe)
        if ((long) w * frameCount == h) return 1;        // exact N-tall single column → legacy vertical strip
        return AnimationDecoder.gridCols(frameCount);
    }

    /** Read a big-endian 4-byte int from a PNG at {@code off} (16 = width, 20 = height in the IHDR chunk). */
    private static int pngDim(byte[] b, int off) {
        if (b == null || b.length < off + 4) return -1;
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16)
                | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
    }

    /** Force TYPE_INT_ARGB so an indexed/gray PNG resamples correctly (ImageResampler reads via getRGB). */
    private static BufferedImage toArgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) return src;
        BufferedImage argb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return argb;
    }
}
