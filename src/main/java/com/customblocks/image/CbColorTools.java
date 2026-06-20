/**
 * CbColorTools.java — Group 27 §G27.7 §B3 smart-colour maths. Pure image/colour maths; no Minecraft types.
 *
 * Responsibility: the "smart colour" helpers behind the floating colour panel — harmony suggestions
 * (complementary / triad / analogous), a tint-and-shade strip, and a WCAG contrast readout used by the
 * letter-vs-background contrast guard. Hue rotations delegate to {@link ColorMath#hslShiftRgb} and the
 * tint/shade steps to {@link ColorMath#labLerp} so this file stays a thin, well-tested layer.
 *
 * Depends on: ColorMath only.
 * Called by:  client/gui/panel/CbColorPanel (harmony strip, tints, contrast guard).
 */
package com.customblocks.image;

public final class CbColorTools {

    private CbColorTools() {} // static-only

    public static final int WHITE = 0xFFFFFF, BLACK = 0x000000;

    // ── Harmony (hue rotations around the colour wheel) ────────────────────────

    /** The opposite colour on the wheel (hue +180°). */
    public static int complementary(int rgb) {
        return ColorMath.hslShiftRgb(rgb & 0xFFFFFF, 180, 1.0, 1.0);
    }

    /** The two triad partners (hue ±120°). */
    public static int[] triad(int rgb) {
        int c = rgb & 0xFFFFFF;
        return new int[]{ ColorMath.hslShiftRgb(c, 120, 1.0, 1.0), ColorMath.hslShiftRgb(c, 240, 1.0, 1.0) };
    }

    /** The two neighbouring colours (hue ±30°). */
    public static int[] analogous(int rgb) {
        int c = rgb & 0xFFFFFF;
        return new int[]{ ColorMath.hslShiftRgb(c, -30, 1.0, 1.0), ColorMath.hslShiftRgb(c, 30, 1.0, 1.0) };
    }

    /**
     * A perceptually even shade→tint strip of {@code steps} colours centred on {@code rgb}: the darker
     * half blends toward black, the lighter half toward white. {@code steps} should be odd so the base
     * colour sits in the middle; the strongest blend is {@code maxBlend} (0..1) at the ends.
     */
    public static int[] tintShadeStrip(int rgb, int steps, double maxBlend) {
        int c = rgb & 0xFFFFFF;
        int[] out = new int[steps];
        int mid = steps / 2;
        for (int i = 0; i < steps; i++) {
            if (i == mid) { out[i] = c; continue; }
            if (i < mid) {
                double t = maxBlend * (mid - i) / (double) mid;   // toward black
                out[i] = ColorMath.labLerp(c, BLACK, t);
            } else {
                double t = maxBlend * (i - mid) / (double) (steps - 1 - mid); // toward white
                out[i] = ColorMath.labLerp(c, WHITE, t);
            }
        }
        return out;
    }

    // ── WCAG contrast guard (letter vs background readability) ──────────────────

    /** Relative luminance of a 0xRRGGBB colour per WCAG 2.x (linearised sRGB). */
    public static double luminance(int rgb) {
        double r = lin(((rgb >> 16) & 0xFF) / 255.0);
        double g = lin(((rgb >> 8) & 0xFF) / 255.0);
        double b = lin((rgb & 0xFF) / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    /** Contrast ratio between two colours, 1.0 (identical) .. 21.0 (black vs white). */
    public static double contrastRatio(int rgbA, int rgbB) {
        double la = luminance(rgbA & 0xFFFFFF), lb = luminance(rgbB & 0xFFFFFF);
        double hi = Math.max(la, lb), lo = Math.min(la, lb);
        return (hi + 0.05) / (lo + 0.05);
    }

    /**
     * A short Minecraft-formatted readout for the contrast guard. Thresholds follow the WCAG bands:
     * &lt;3.0 reads as too low to tell apart from a distance, 3.0–4.5 is borderline, ≥4.5 is clear.
     * Glyphs are ASCII ("OK"/"LOW") to stay mojibake-safe in-game.
     */
    public static String contrastLabel(double ratio) {
        String n = String.format(java.util.Locale.ROOT, "%.1f:1", ratio);
        if (ratio < 3.0)  return "§ccontrast LOW §7(" + n + ")";
        if (ratio < 4.5)  return "§econtrast okay §7(" + n + ")";
        return "§acontrast good §7(" + n + ")";
    }

    /** True when the pair is too low-contrast to read comfortably (drives the guard warning). */
    public static boolean lowContrast(int rgbA, int rgbB) {
        return contrastRatio(rgbA, rgbB) < 3.0;
    }

    private static double lin(double c) { // sRGB → linear (matches ColorMath)
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
