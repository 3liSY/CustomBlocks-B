/**
 * CieDe2000.java
 *
 * Responsibility: The CIEDE2000 colour-difference formula (ΔE00), replacing the plain Euclidean
 * CIE76 distance everywhere a perceptual difference is compared (G10 §H, locked 2026-07-25).
 * CIE76 overstates distance in saturated blues and understates it in near-neutrals, so one ΔE
 * budget cannot mean the same thing across the palette; CIEDE2000 is uniform enough that it can.
 *
 * Implementation follows Sharma, Wu & Dalal, "The CIEDE2000 Color-Difference Formula:
 * Implementation Notes, Supplementary Test Data, and Mathematical Observations" (CRA 2005) —
 * including the two published pitfalls their test vectors exist to catch: the hue angles must be
 * computed with atan2 on the G-scaled a' (not raw a), and the mean hue h̄' needs the ±360°
 * wraparound cases handled exactly (pairs 9-16 of the test set break a naive average).
 *
 * Validated against all 34 published test pairs by tools/render_preview/CieDe2000Check.java —
 * run it after ANY edit to this file.
 *
 * Depends on: nothing (pure maths).
 * Called by:  image/BackgroundRemover (background ΔE).
 */
package com.customblocks.image;

public final class CieDe2000 {

    private CieDe2000() {} // static-only

    /** 25⁷, the constant under both chroma-rotation roots. */
    private static final double POW25_7 = 6_103_515_625.0;

    /**
     * ΔE00 between two CIE-LAB colours, each given as {L*, a*, b*}. Parametric weights
     * kL = kC = kH = 1 (the reference viewing condition, and the only ones the mod uses).
     */
    public static double of(double[] lab1, double[] lab2) {
        double l1 = lab1[0], a1 = lab1[1], b1 = lab1[2];
        double l2 = lab2[0], a2 = lab2[1], b2 = lab2[2];

        // Step 1 — chroma mean of the RAW a,b pairs, then the G scaling that de-saturates a*.
        double c1 = Math.hypot(a1, b1);
        double c2 = Math.hypot(a2, b2);
        double cBar = (c1 + c2) / 2.0;
        double cBar7 = Math.pow(cBar, 7);
        double g = 0.5 * (1.0 - Math.sqrt(cBar7 / (cBar7 + POW25_7)));

        double a1p = (1.0 + g) * a1;
        double a2p = (1.0 + g) * a2;
        double c1p = Math.hypot(a1p, b1);
        double c2p = Math.hypot(a2p, b2);

        // Hue angles in degrees, [0, 360). A colour with no chroma has hue 0 by convention.
        double h1p = hueDeg(b1, a1p);
        double h2p = hueDeg(b2, a2p);

        // Step 2 — the differences.
        double dLp = l2 - l1;
        double dCp = c2p - c1p;

        double dhp; // Δh' in degrees, the wraparound-corrected hue difference
        if (c1p * c2p == 0.0) {
            dhp = 0.0;
        } else {
            dhp = h2p - h1p;
            if (dhp > 180.0) dhp -= 360.0;
            else if (dhp < -180.0) dhp += 360.0;
        }
        double dHp = 2.0 * Math.sqrt(c1p * c2p) * Math.sin(Math.toRadians(dhp) / 2.0);

        // Step 3 — the means, with the mean hue's own wraparound rules.
        double lBarP = (l1 + l2) / 2.0;
        double cBarP = (c1p + c2p) / 2.0;

        double hBarP;
        if (c1p * c2p == 0.0) {
            hBarP = h1p + h2p; // one side hueless → the other side's hue (their sum, per the paper)
        } else {
            double sum = h1p + h2p;
            if (Math.abs(h1p - h2p) <= 180.0) hBarP = sum / 2.0;
            else if (sum < 360.0) hBarP = (sum + 360.0) / 2.0;
            else hBarP = (sum - 360.0) / 2.0;
        }

        double t = 1.0
                - 0.17 * Math.cos(Math.toRadians(hBarP - 30.0))
                + 0.24 * Math.cos(Math.toRadians(2.0 * hBarP))
                + 0.32 * Math.cos(Math.toRadians(3.0 * hBarP + 6.0))
                - 0.20 * Math.cos(Math.toRadians(4.0 * hBarP - 63.0));

        double dTheta = 30.0 * Math.exp(-Math.pow((hBarP - 275.0) / 25.0, 2));
        double cBarP7 = Math.pow(cBarP, 7);
        double rc = 2.0 * Math.sqrt(cBarP7 / (cBarP7 + POW25_7));
        double rt = -Math.sin(Math.toRadians(2.0 * dTheta)) * rc;

        double lm50 = (lBarP - 50.0) * (lBarP - 50.0);
        double sl = 1.0 + 0.015 * lm50 / Math.sqrt(20.0 + lm50);
        double sc = 1.0 + 0.045 * cBarP;
        double sh = 1.0 + 0.015 * cBarP * t;

        double fL = dLp / sl;
        double fC = dCp / sc;
        double fH = dHp / sh;
        return Math.sqrt(fL * fL + fC * fC + fH * fH + rt * fC * fH);
    }

    /** atan2 hue in degrees mapped to [0, 360); 0 when the colour has no chroma (a' = b = 0). */
    private static double hueDeg(double b, double ap) {
        if (b == 0.0 && ap == 0.0) return 0.0;
        double h = Math.toDegrees(Math.atan2(b, ap));
        return h < 0.0 ? h + 360.0 : h;
    }
}
