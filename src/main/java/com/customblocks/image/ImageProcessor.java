/**
 * ImageProcessor.java
 *
 * Responsibility: Decode arbitrary image bytes and produce a square PNG suitable for a
 * block texture — aspect ratio preserved, centered, with transparent padding, scaled
 * with bicubic interpolation (no nearest-neighbour pixelation).
 *
 * Phase 4: core resize. BackgroundRemover (3 modes) + ColorReplacer come next.
 *
 * Called by: the retexture command.
 */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public final class ImageProcessor {

    private ImageProcessor() {} // static-only

    /**
     * Produce a {@code size}x{@code size} ARGB PNG from arbitrary image bytes.
     * {@code size} should be a power of two (16/32/64/128) for clean mipmapping.
     */
    public static byte[] toBlockPng(byte[] input, int size) throws Exception {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(input));
        if (src == null) {
            throw new Exception("Could not read that image (unsupported format? WebP is not supported yet).");
        }
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int sw = src.getWidth();
        int sh = src.getHeight();
        double scale = Math.min((double) size / sw, (double) size / sh);
        int dw = Math.max(1, (int) Math.round(sw * scale));
        int dh = Math.max(1, (int) Math.round(sh * scale));
        int dx = (size - dw) / 2;
        int dy = (size - dh) / 2;
        g.drawImage(src, dx, dy, dw, dh, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(out, "PNG", baos);
        return baos.toByteArray();
    }
}
