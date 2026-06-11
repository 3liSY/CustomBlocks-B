/**
 * ColorReplacer.java
 *
 * Responsibility: Targeted pixel colour operations for the colour-variant system (Group 06 /
 * M3 hex). swapColor replaces pixels near one RGB with another — a direct per-pixel swap with
 * NO flood-fill, so design pixels away from the old colour are never touched (the old
 * project's safe batch-recolour path). tint repaints base item art (Squares / Triangles) to a
 * configured hex by scaling the target colour with each pixel's brightness, so outlines and
 * shading survive.
 *
 * Depends on: javax.imageio only.
 * Called by:  core/ColorVariantService.recolorVariants (swapColor),
 *             network/ServerPackGenerator (tint).
 */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public final class ColorReplacer {

    private ColorReplacer() {} // static-only

    /**
     * Replace every pixel within {@code tol} per-channel distance of {@code oldRgb} with
     * {@code newRgb}. Alpha is preserved; all other pixels pass through untouched.
     */
    public static byte[] swapColor(byte[] png, int oldRgb, int newRgb, int tol) throws Exception {
        BufferedImage img = read(png);
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int or = oldRgb >> 16 & 0xFF, og = oldRgb >> 8 & 0xFF, ob = oldRgb & 0xFF;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int r = argb >> 16 & 0xFF, g = argb >> 8 & 0xFF, b = argb & 0xFF;
                if (Math.abs(r - or) <= tol && Math.abs(g - og) <= tol && Math.abs(b - ob) <= tol) {
                    argb = (argb & 0xFF000000) | (newRgb & 0xFFFFFF);
                }
                out.setRGB(x, y, argb);
            }
        }
        return write(out);
    }

    /**
     * Re-tint item art to {@code rgb} by HSB transfer: every pixel takes the target's hue,
     * its saturation becomes target-sat × pixel-sat, and its brightness is scaled by the
     * target's brightness — so the art's highlight, fill and outline keep their contrast
     * instead of flattening (the old max-channel scaling washed the highlight out).
     */
    public static byte[] tint(byte[] png, int rgb) throws Exception {
        BufferedImage img = read(png);
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float[] t = java.awt.Color.RGBtoHSB(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, null);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                float[] p = java.awt.Color.RGBtoHSB(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, null);
                int tinted = java.awt.Color.HSBtoRGB(t[0], t[1] * p[1], p[2] * t[2]);
                out.setRGB(x, y, (argb & 0xFF000000) | (tinted & 0xFFFFFF));
            }
        }
        return write(out);
    }

    private static BufferedImage read(byte[] png) throws Exception {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        if (img == null) throw new Exception("Could not read that texture.");
        return img;
    }

    private static byte[] write(BufferedImage img) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }
}
