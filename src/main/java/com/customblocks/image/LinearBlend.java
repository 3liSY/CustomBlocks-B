/**
 * LinearBlend.java
 *
 * Responsibility: compositing a pixel onto an opaque fill in LINEAR LIGHT (G10 §H, locked
 * 2026-07-25). sRGB bytes are transfer-encoded; blending them directly loses light energy toward
 * the darker operand, which against a black fill draws a dark rim along every anti-aliased edge —
 * the artifact the old RING_/peel compensations were built to chase. The correct blend is:
 * decode sRGB → mix → re-encode. Alpha is a coverage fraction and is already linear; it is used
 * as the mix weight and NEVER passed through the transfer curve.
 *
 * Depends on: nothing (pure maths).
 * Called by:  image/BackgroundRemover (Stage 3 composite, snap flatten),
 *             image/ImageProcessor.fillBackground (the old Graphics2D gamma-space fill).
 */
package com.customblocks.image;

public final class LinearBlend {

    private LinearBlend() {} // static-only

    /** sRGB byte → linear-light value, precomputed for all 256 codes (the decode is the hot path). */
    private static final double[] TO_LINEAR = new double[256];

    static {
        for (int i = 0; i < 256; i++) {
            double c = i / 255.0;
            TO_LINEAR[i] = c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }
    }

    /** sRGB byte → linear light. Package-private for rung 5, which must solve the mixing equation
     *  {@code P = aF + (1-a)B} in the space where that mixing physically happened. */
    static double toLinear(int code) {
        return TO_LINEAR[code & 0xFF];
    }

    /**
     * Composite {@code srcArgb} over the opaque {@code fillRgb} (0xRRGGBB) and return the opaque
     * result. Fully transparent returns the fill; fully opaque returns the source unchanged —
     * only genuinely mixed pixels pay for the transfer round-trip.
     */
    public static int over(int srcArgb, int fillRgb) {
        int a = (srcArgb >>> 24) & 0xFF;
        if (a == 255) return srcArgb | 0xFF000000;
        if (a == 0) return 0xFF000000 | (fillRgb & 0xFFFFFF);
        double w = a / 255.0; // coverage — linear by definition, not gamma-encoded
        int r = mix((srcArgb >> 16) & 0xFF, (fillRgb >> 16) & 0xFF, w);
        int g = mix((srcArgb >> 8) & 0xFF, (fillRgb >> 8) & 0xFF, w);
        int b = mix(srcArgb & 0xFF, fillRgb & 0xFF, w);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** One channel: decode both sides, mix by coverage, re-encode. */
    private static int mix(int src, int fill, double w) {
        double lin = TO_LINEAR[src] * w + TO_LINEAR[fill] * (1.0 - w);
        return encode(lin);
    }

    /** Linear-light value → sRGB byte (inverse transfer, rounded, clamped). */
    private static int encode(double lin) {
        double c = lin <= 0.0031308 ? lin * 12.92 : 1.055 * Math.pow(lin, 1.0 / 2.4) - 0.055;
        int v = (int) Math.round(c * 255.0);
        return v < 0 ? 0 : Math.min(255, v);
    }
}
