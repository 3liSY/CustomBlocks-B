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
            throw new Exception("Could not read that image — the data isn't a recognized image "
                    + "(supported: PNG, JPG, GIF, WebP). The link may point to a web page, not a direct image.");
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

    /**
     * Composite a PNG over an opaque {@code bgArgb} background of the same size: every transparent or
     * semi-transparent pixel takes (some of) the background colour. Used by the Block Creation Studio's
     * "background colour" so a logo with transparent areas sits on a solid backdrop instead of replacing
     * the image. {@code bgArgb} should be fully opaque (0xFFrrggbb).
     */
    public static byte[] fillBackground(byte[] png, int bgArgb) throws Exception {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(png));
        if (src == null) throw new Exception("Could not read the texture to apply a background colour.");
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setColor(new java.awt.Color(0xFF000000 | (bgArgb & 0xFFFFFF), true));
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(out, "PNG", baos);
        return baos.toByteArray();
    }
}
