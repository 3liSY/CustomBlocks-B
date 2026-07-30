/**
 * BgVeil.java — "is this pixel the background seen through a translucent white veil?" (G10 §HA4).
 *
 * <p>A stock-photo watermark is not painted content: it is white laid over the picture at a low
 * opacity. Compositing white over a colour has an exact signature — the result moves along the
 * straight line from that colour toward white in LINEAR light, which raises lightness, lowers chroma,
 * and leaves hue where it was. Measured on the letter-O baseline, the veiled pixels sit at
 * {@code ΔL +3.7, ΔC -5.8, Δhue -6.4°} — 6.3 ΔE00 from the key, far outside rung 2's JND, which is why
 * they survived every colour match and baked as the blue blobs and swirls ringing the glyph.
 *
 * <p>So this is NOT a looser tolerance. The JND bar is unchanged; what changes is WHAT the bar is
 * measured against — the key itself, or the key mixed with white by some fraction. A pixel qualifies
 * only when it lands on that line, which a colour that merely happens to be nearby does not.
 *
 * <p>Two guards keep it from eating real content, and both are structural rather than tuned:
 *
 * <ul>
 *   <li>{@link #MAX_MIX} — the veil may not be more white than background. Pure white IS on the line
 *       (mix 1.0), so without a cap the white letter O, the white stripes and every white subject on a
 *       coloured background would qualify. The baseline watermark mixes at roughly 0.10-0.13, so the
 *       cap sits between the two by a wide margin and is not balanced on an edge.
 *   <li>{@link #MIN_KEY_CHROMA} — the key must be a COLOUR. Every grey lies on the line from black to
 *       white, so against a black or grey background the test degenerates into "is this pixel grey",
 *       which is the chrome logo's entire silver bevel. A near-neutral key disables the test outright.
 * </ul>
 *
 * Depends on: BackgroundRemover.rgbToLab, CieDe2000.
 * Called by:  image/BgRungKey (the region walk and its pocket set).
 */
package com.customblocks.image;

final class BgVeil {

    private BgVeil() {} // static-only

    /** The most white a veil may be before it stops being a veil and becomes white content. */
    private static final double MAX_MIX = 0.35;

    /** Below this Lab chroma the key is a neutral, the white line runs through every grey, and the
     *  test cannot distinguish a veil from silver — so it is switched off entirely. */
    private static final double MIN_KEY_CHROMA = 10.0;

    /** Whether the veil test can run against this key at all. Cheap; call it once per walk. */
    static boolean applies(int key) {
        double[] lab = BackgroundRemover.rgbToLab(key);
        return Math.hypot(lab[1], lab[2]) >= MIN_KEY_CHROMA;
    }

    /**
     * Whether {@code argb} is {@code key} under a translucent white veil, judged at the same
     * {@code tol} ΔE00 the caller uses for the key itself.
     *
     * <p>The projection runs in linear light because that is where compositing actually happens;
     * projecting in gamma-encoded sRGB would bend the line and let colours off it slip through near
     * the dark end.
     */
    static boolean veiled(int argb, int key, double tol) {
        double[] p = linear(argb), k = linear(key);

        // Project p onto the segment k → white, in linear light.
        double num = 0, den = 0;
        for (int i = 0; i < 3; i++) {
            double axis = 1.0 - k[i];
            num += (p[i] - k[i]) * axis;
            den += axis * axis;
        }
        if (den <= 1e-9) return false;            // key is already white; nothing to mix toward
        double mix = num / den;
        if (mix <= 0 || mix > MAX_MIX) return false;

        int rebuilt = 0xFF000000;
        for (int i = 0; i < 3; i++) {
            double lin = k[i] + mix * (1.0 - k[i]);
            int c = (int) Math.round(255.0 * srgb(Math.min(1.0, Math.max(0.0, lin))));
            rebuilt |= Math.min(255, Math.max(0, c)) << (16 - 8 * i);
        }
        double[] a = BackgroundRemover.rgbToLab(argb), b = BackgroundRemover.rgbToLab(rebuilt);
        return CieDe2000.of(a, b) <= tol;
    }

    private static double[] linear(int argb) {
        double[] out = new double[3];
        for (int i = 0; i < 3; i++) {
            double c = ((argb >> (16 - 8 * i)) & 0xFF) / 255.0;
            out[i] = (c <= 0.04045) ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }
        return out;
    }

    private static double srgb(double lin) {
        return (lin <= 0.0031308) ? lin * 12.92 : 1.055 * Math.pow(lin, 1.0 / 2.4) - 0.055;
    }
}
