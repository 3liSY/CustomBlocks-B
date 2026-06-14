/**
 * ColorMath.java
 *
 * Responsibility: The colour maths behind Group 10 Slice 2 — dress (solid-colour overlay),
 * gradient (average colour of a texture + perceptual CIE-Lab interpolation between two
 * colours), and the solid swatch PNG those gradient steps are baked into. Pure image/maths;
 * no Minecraft or server types.
 *
 * CIE-Lab (D65) is used for interpolation instead of raw RGB so the in-between colours read
 * as evenly spaced to the eye (Group 10 Decision §M — replaces the old YCbCr distance).
 *
 * Group 10 (full) also adds HSL transforms (hue/saturation/lightness shift) that power the
 * Colour Variants panel and the live recolour slider — one general {@link #hslShift} plus a
 * single-colour {@link #hslShiftRgb} for swatch previews.
 *
 * Depends on: javax.imageio only.
 * Called by:  command/handlers/ColorImageCommands (dress, gradient), core/ColorToolService
 *             (variants + live recolour), client/gui/RecolorSliderScreen (client preview).
 */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public final class ColorMath {

    private ColorMath() {} // static-only

    /**
     * Blend a solid {@code rgb} over every visible pixel of {@code png} at {@code intensity}
     * (0 = unchanged, 1 = fully the colour). Each channel is a linear mix; alpha is preserved
     * and fully-transparent pixels are left transparent, so only the artwork is tinted.
     */
    public static byte[] dress(byte[] png, int rgb, double intensity) throws Exception {
        BufferedImage img = read(png);
        double k = Math.max(0.0, Math.min(1.0, intensity));
        int cr = rgb >> 16 & 0xFF, cg = rgb >> 8 & 0xFF, cb = rgb & 0xFF;
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int a = argb >>> 24;
                if (a == 0) { out.setRGB(x, y, argb); continue; } // keep padding transparent
                int r = argb >> 16 & 0xFF, g = argb >> 8 & 0xFF, b = argb & 0xFF;
                int nr = (int) Math.round(r * (1 - k) + cr * k);
                int ng = (int) Math.round(g * (1 - k) + cg * k);
                int nb = (int) Math.round(b * (1 - k) + cb * k);
                out.setRGB(x, y, (a << 24) | (nr << 16) | (ng << 8) | nb);
            }
        }
        return write(out);
    }

    /**
     * The alpha-weighted average colour of a texture as 0xRRGGBB — its single representative
     * colour for gradient endpoints. Fully-transparent images fall back to mid-grey.
     */
    public static int averageColor(byte[] png) throws Exception {
        BufferedImage img = read(png);
        double sr = 0, sg = 0, sb = 0, sa = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int a = argb >>> 24;
                if (a == 0) continue;
                sr += (argb >> 16 & 0xFF) * (double) a;
                sg += (argb >> 8 & 0xFF) * (double) a;
                sb += (argb & 0xFF) * (double) a;
                sa += a;
            }
        }
        if (sa <= 0) return 0x808080;
        int r = clamp8((int) Math.round(sr / sa));
        int g = clamp8((int) Math.round(sg / sa));
        int b = clamp8((int) Math.round(sb / sa));
        return (r << 16) | (g << 8) | b;
    }

    /** Perceptual interpolation between two 0xRRGGBB colours at {@code t} in [0,1], via CIE-Lab. */
    public static int labLerp(int rgbA, int rgbB, double t) {
        double[] la = rgbToLab(rgbA);
        double[] lb = rgbToLab(rgbB);
        double[] m = {
                la[0] + (lb[0] - la[0]) * t,
                la[1] + (lb[1] - la[1]) * t,
                la[2] + (lb[2] - la[2]) * t
        };
        return labToRgb(m);
    }

    /** A {@code size}×{@code size} fully-opaque PNG filled with {@code rgb}. */
    public static byte[] solidPng(int rgb, int size) throws Exception {
        int argb = 0xFF000000 | (rgb & 0xFFFFFF);
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) out.setRGB(x, y, argb);
        }
        return write(out);
    }

    // ── HSL transforms — power the Colour Variants panel + the live recolour slider ─────────────

    /**
     * Shift every visible pixel of {@code png} in HSL space: rotate hue by {@code hueDeg} degrees,
     * scale saturation by {@code satFactor} and lightness by {@code lightFactor} (1.0 = unchanged).
     * Alpha is preserved and fully-transparent padding is left untouched, so only the artwork moves.
     * This is one general operation; the named variants below are just preset arguments to it.
     */
    public static byte[] hslShift(byte[] png, double hueDeg, double satFactor, double lightFactor) throws Exception {
        BufferedImage img = read(png);
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int a = argb >>> 24;
                if (a == 0) { out.setRGB(x, y, argb); continue; } // keep padding transparent
                out.setRGB(x, y, (a << 24) | (hslShiftRgb(argb & 0xFFFFFF, hueDeg, satFactor, lightFactor)));
            }
        }
        return write(out);
    }

    /** The same HSL shift applied to a single 0xRRGGBB colour (used for swatch previews / lore). */
    public static int hslShiftRgb(int rgb, double hueDeg, double satFactor, double lightFactor) {
        double[] hsl = rgbToHsl(rgb);
        double h = ((hsl[0] + hueDeg / 360.0) % 1.0 + 1.0) % 1.0;
        double s = clamp01(hsl[1] * satFactor);
        double l = clamp01(hsl[2] * lightFactor);
        return hslToRgb(h, s, l);
    }

    private static double[] rgbToHsl(int rgb) {
        double r = (rgb >> 16 & 0xFF) / 255.0, g = (rgb >> 8 & 0xFF) / 255.0, b = (rgb & 0xFF) / 255.0;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        double l = (max + min) / 2.0, h = 0, s = 0;
        double d = max - min;
        if (d > 1e-9) {
            s = l > 0.5 ? d / (2.0 - max - min) : d / (max + min);
            if (max == r)      h = (g - b) / d + (g < b ? 6 : 0);
            else if (max == g) h = (b - r) / d + 2;
            else               h = (r - g) / d + 4;
            h /= 6.0;
        }
        return new double[]{h, s, l};
    }

    private static int hslToRgb(double h, double s, double l) {
        double r, g, b;
        if (s <= 1e-9) {
            r = g = b = l; // achromatic — grey stays grey under a hue shift
        } else {
            double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            double p = 2 * l - q;
            r = hue2rgb(p, q, h + 1.0 / 3.0);
            g = hue2rgb(p, q, h);
            b = hue2rgb(p, q, h - 1.0 / 3.0);
        }
        return (clamp8((int) Math.round(r * 255)) << 16)
                | (clamp8((int) Math.round(g * 255)) << 8)
                | clamp8((int) Math.round(b * 255));
    }

    private static double hue2rgb(double p, double q, double t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1.0 / 6.0) return p + (q - p) * 6 * t;
        if (t < 1.0 / 2.0) return q;
        if (t < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - t) * 6;
        return p;
    }

    private static double clamp01(double v) { return v < 0 ? 0 : Math.min(1.0, v); }

    // ── sRGB ⇄ CIE-Lab (D65 reference white) ──────────────────────────────────

    private static final double XN = 0.95047, YN = 1.0, ZN = 1.08883;

    private static double[] rgbToLab(int rgb) {
        double r = lin((rgb >> 16 & 0xFF) / 255.0);
        double g = lin((rgb >> 8 & 0xFF) / 255.0);
        double b = lin((rgb & 0xFF) / 255.0);
        double x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / XN;
        double y = (r * 0.2126 + g * 0.7152 + b * 0.0722) / YN;
        double z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / ZN;
        double fx = fwd(x), fy = fwd(y), fz = fwd(z);
        return new double[]{116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)};
    }

    private static int labToRgb(double[] lab) {
        double fy = (lab[0] + 16) / 116;
        double fx = fy + lab[1] / 500;
        double fz = fy - lab[2] / 200;
        double x = inv(fx) * XN, y = inv(fy) * YN, z = inv(fz) * ZN;
        double r = x * 3.2406 + y * -1.5372 + z * -0.4986;
        double g = x * -0.9689 + y * 1.8758 + z * 0.0415;
        double b = x * 0.0557 + y * -0.2040 + z * 1.0570;
        return (srgb8(r) << 16) | (srgb8(g) << 8) | srgb8(b);
    }

    private static double lin(double c) { // sRGB → linear
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static int srgb8(double c) { // linear → sRGB byte
        c = c <= 0.0031308 ? 12.92 * c : 1.055 * Math.pow(c, 1 / 2.4) - 0.055;
        return clamp8((int) Math.round(c * 255));
    }

    private static double fwd(double t) { // XYZ → Lab f()
        return t > 0.008856 ? Math.cbrt(t) : 7.787 * t + 16.0 / 116.0;
    }

    private static double inv(double t) { // Lab → XYZ inverse of f()
        double t3 = t * t * t;
        return t3 > 0.008856 ? t3 : (t - 16.0 / 116.0) / 7.787;
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
