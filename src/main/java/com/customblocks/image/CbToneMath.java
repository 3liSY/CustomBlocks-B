/**
 * CbToneMath.java — Group 27 §G27.7 §C3 tone tools. Pure image/colour maths; no Minecraft types.
 *
 * Responsibility: the four extra recolour "tune" tools layered on top of the HSL shift — temperature
 * (warm/cool), contrast, a split brightness curve (lift shadows / lower highlights), and one-tap filters
 * (grayscale / sepia / invert / posterize). Every transform is a per-pixel point operation, so the same
 * {@link #applyRgb} drives both the live cube preview (one colour at a time) and the server bake
 * ({@link #apply} over a whole PNG) — preview and committed result stay identical.
 *
 * Slider ranges (raw UI values): temp/contrast in [-100,100] (0 = unchanged), shadowLift/highlightDrop in
 * [0,100] (0 = off), filter 0 none · 1 gray · 2 sepia · 3 invert · 4 posterize.
 *
 * Depends on: javax.imageio only.
 * Called by: client/gui/RecolorSliderScreen (preview) + core/ColorToolService.applyRecolor (bake).
 */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public final class CbToneMath {

    private CbToneMath() {} // static-only

    public static final int F_NONE = 0, F_GRAY = 1, F_SEPIA = 2, F_INVERT = 3, F_POSTERIZE = 4;

    /** True when every tool is at its neutral setting (lets callers skip the pass entirely). */
    public static boolean isIdentity(double temp, double contrast, double shadowLift, double highlightDrop, int filter) {
        return temp == 0 && contrast == 0 && shadowLift == 0 && highlightDrop == 0 && filter == F_NONE;
    }

    /** Apply all four tools to a single 0xRRGGBB colour, in order: temperature → contrast → curve → filter. */
    public static int applyRgb(int rgb, double temp, double contrast, double shadowLift, double highlightDrop, int filter) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;

        // Temperature: warm (+) pushes red up / blue down; cool (−) the reverse.
        if (temp != 0) {
            double t = temp / 100.0 * 40.0;
            r = clamp8((int) Math.round(r + t));
            b = clamp8((int) Math.round(b - t));
        }
        // Contrast: scale each channel around mid-grey.
        if (contrast != 0) {
            double f = 1.0 + contrast / 100.0; // [-100,100] → factor [0,2]
            r = clamp8((int) Math.round((r - 128) * f + 128));
            g = clamp8((int) Math.round((g - 128) * f + 128));
            b = clamp8((int) Math.round((b - 128) * f + 128));
        }
        // Brightness curve: lift darks, drop brights (per channel, weighted by how dark/bright it is).
        if (shadowLift != 0 || highlightDrop != 0) {
            double lift = shadowLift / 100.0, drop = highlightDrop / 100.0;
            r = curve(r, lift, drop); g = curve(g, lift, drop); b = curve(b, lift, drop);
        }
        // One-tap filter.
        switch (filter) {
            case F_GRAY -> { int l = luma(r, g, b); r = g = b = l; }
            case F_SEPIA -> {
                int nr = clamp8((int) Math.round(0.393 * r + 0.769 * g + 0.189 * b));
                int ng = clamp8((int) Math.round(0.349 * r + 0.686 * g + 0.168 * b));
                int nb = clamp8((int) Math.round(0.272 * r + 0.534 * g + 0.131 * b));
                r = nr; g = ng; b = nb;
            }
            case F_INVERT -> { r = 255 - r; g = 255 - g; b = 255 - b; }
            case F_POSTERIZE -> { r = posterize(r); g = posterize(g); b = posterize(b); }
            default -> { /* F_NONE */ }
        }
        return (r << 16) | (g << 8) | b;
    }

    /** Apply all four tools to every visible pixel of {@code png}; transparent padding is left untouched. */
    public static byte[] apply(byte[] png, double temp, double contrast, double shadowLift, double highlightDrop, int filter) throws Exception {
        if (isIdentity(temp, contrast, shadowLift, highlightDrop, filter)) return png;
        BufferedImage img = read(png);
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int a = argb >>> 24;
                if (a == 0) { out.setRGB(x, y, argb); continue; }
                int rgb = applyRgb(argb & 0xFFFFFF, temp, contrast, shadowLift, highlightDrop, filter);
                out.setRGB(x, y, (a << 24) | rgb);
            }
        }
        return write(out);
    }

    private static int curve(int c, double lift, double drop) {
        double v = c;
        v += lift * 255.0 * (1.0 - c / 255.0);   // raise darks
        v -= drop * 255.0 * (c / 255.0);          // lower brights
        return clamp8((int) Math.round(v));
    }

    private static int posterize(int c) {
        int levels = 4;
        return clamp8((int) Math.round(Math.round(c / 255.0 * (levels - 1)) / (double) (levels - 1) * 255.0));
    }

    private static int luma(int r, int g, int b) {
        return clamp8((int) Math.round(0.299 * r + 0.587 * g + 0.114 * b));
    }

    private static int clamp8(int v) { return v < 0 ? 0 : Math.min(v, 255); }

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
