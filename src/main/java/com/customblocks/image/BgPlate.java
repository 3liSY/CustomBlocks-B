/**
 * BgPlate.java — the CLEAN PLATE: what the background would look like behind the subject (G10 §HA4,
 * 2026-07-28).
 *
 * <p>Every colour test in §H so far measures against ONE key. That is exact when the backdrop is one
 * tone and wrong the moment it is a fade: on the letter-O source the backdrop runs from
 * {@code 15,116,186} at the top-left to {@code 31,133,197} at the bottom-right, 6.5 ΔE00 apart — wider
 * than rung 2's JND and wider than the mop-up's 2×JND bar. Leftover ringing near the darker corner is
 * therefore a dead match for the backdrop AROUND IT while measuring far from the single key, so no
 * global bar can take it without also opening the bar everywhere else, which is the subject-eating
 * trade §H exists to avoid.
 *
 * <p>The clean plate is compositing's standard answer to exactly this (uneven backdrops, spill,
 * shadows): estimate the backdrop per pixel, then key each pixel against the backdrop AT ITS OWN SPOT.
 * A fade costs nothing to describe that way — locally it is flat everywhere.
 *
 * <h2>How the estimate is made</h2>
 *
 * PUSH-PULL pyramid, the classic hole-filler for scattered samples. Known background pixels are
 * samples; the subject is the hole. PUSH averages colour and coverage down halving levels until the
 * whole frame is covered; PULL walks back up, keeping each level's own samples and taking the coarser
 * level's estimate only where coverage is missing. The result is smooth by construction — it can only
 * interpolate what the surrounding backdrop actually is — and it is not a fitted surface: the three
 * fits already measured and rejected (bilinear 35 %, neighbour-relative flood 75 %, running local
 * colour 74.9 %) all imposed a SHAPE on the backdrop. This imposes none; a fade, a vignette and a soft
 * corner glow are all reproduced because each is read straight off its own neighbourhood.
 *
 * <p>Averaging is done in LINEAR LIGHT, for the same reason every other §H blend moved there: light
 * adds linearly and sRGB bytes do not, so averaging encoded bytes would bias the estimate toward the
 * darker samples and put a systematic error into the very colour the edge solve divides by. Callers
 * that need to MEASURE against the estimate convert it with {@link #labOf}; callers that need to
 * SUBTRACT it — the edge solve — use the linear triple directly, with no round trip.
 *
 * Depends on: LinearBlend.toLinear, BackgroundRemover.rgbToLab.
 * Called by:  image/BgMopUp, image/BgRungUnmix.
 */
package com.customblocks.image;

final class BgPlate {

    private BgPlate() {} // static-only

    /** Coarsest level: stop when a further halving would drop a side below this. */
    private static final int MIN_SIDE = 2;

    /**
     * A pixel's backdrop estimate in LINEAR LIGHT, or {@code null} when the picture carries no
     * background sample at all — which the callers already exclude.
     *
     * <p>Layout is {@code [y * w + x][0..2]} = linear R, G, B.
     *
     * @param isBg the accepted background mask — its pixels are the samples
     */
    static double[][] build(int[] px, boolean[][] isBg, int w, int h) {
        double[] l = new double[w * h], a = new double[w * h], b = new double[w * h];
        double[] cov = new double[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!isBg[x][y]) continue;
                int i = y * w + x;
                int p = px[i];
                l[i] = LinearBlend.toLinear(p >> 16);
                a[i] = LinearBlend.toLinear(p >> 8);
                b[i] = LinearBlend.toLinear(p);
                cov[i] = 1.0;
            }
        }

        double[][] filled = pushPull(l, a, b, cov, w, h);
        if (filled == null) return null;

        double[][] plate = new double[w * h][];
        for (int i = 0; i < w * h; i++) {
            plate[i] = new double[]{filled[0][i], filled[1][i], filled[2][i]};
        }
        return plate;
    }

    /** A plate estimate as Lab, for the colour distances every other §H test is measured in. */
    static double[] labOf(double[] linear) {
        return BackgroundRemover.rgbToLab(0xFF000000
                | (encode(linear[0]) << 16) | (encode(linear[1]) << 8) | encode(linear[2]));
    }

    private static int encode(double lin) {
        double c = lin <= 0.0031308 ? lin * 12.92 : 1.055 * Math.pow(lin, 1.0 / 2.4) - 0.055;
        int v = (int) Math.round(c * 255.0);
        return v < 0 ? 0 : Math.min(255, v);
    }

    /**
     * One recursion of the pyramid. Returns the three channels with every hole filled, or {@code null}
     * when this level holds no sample at all — which can only happen with an empty mask.
     */
    private static double[][] pushPull(double[] l, double[] a, double[] b, double[] cov,
                                       int w, int h) {
        boolean anySample = false, anyHole = false;
        for (double c : cov) {
            if (c > 0) anySample = true; else anyHole = true;
            if (anySample && anyHole) break;
        }
        if (!anySample) return null;
        if (!anyHole) return new double[][]{l, a, b};

        // ── PUSH: halve, averaging only the covered children ──────────────────────────────────
        int cw = Math.max(1, w / 2), ch = Math.max(1, h / 2);
        if (cw < MIN_SIDE && ch < MIN_SIDE) return flat(l, a, b, cov, w, h);

        double[] cl = new double[cw * ch], ca = new double[cw * ch], cb = new double[cw * ch];
        double[] cc = new double[cw * ch];
        for (int y = 0; y < ch; y++) {
            for (int x = 0; x < cw; x++) {
                double sl = 0, sa = 0, sb = 0, sc = 0;
                for (int dy = 0; dy < 2; dy++) {
                    for (int dx = 0; dx < 2; dx++) {
                        int sx = x * 2 + dx, sy = y * 2 + dy;
                        if (sx >= w || sy >= h) continue;
                        int i = sy * w + sx;
                        double c = cov[i];
                        if (c <= 0) continue;
                        sl += l[i] * c; sa += a[i] * c; sb += b[i] * c; sc += c;
                    }
                }
                int j = y * cw + x;
                if (sc > 0) { cl[j] = sl / sc; ca[j] = sa / sc; cb[j] = sb / sc; cc[j] = 1.0; }
            }
        }

        double[][] coarse = pushPull(cl, ca, cb, cc, cw, ch);
        if (coarse == null) return flat(l, a, b, cov, w, h);

        // ── PULL: keep this level's own samples, borrow the coarse estimate only in the holes ──
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (cov[i] > 0) continue;
                int j = Math.min(ch - 1, y / 2) * cw + Math.min(cw - 1, x / 2);
                l[i] = coarse[0][j]; a[i] = coarse[1][j]; b[i] = coarse[2][j];
            }
        }
        return new double[][]{l, a, b};
    }

    /** Degenerate fallback: one mean for the whole level. Reached only on a 1-pixel-wide pyramid top. */
    private static double[][] flat(double[] l, double[] a, double[] b, double[] cov, int w, int h) {
        double sl = 0, sa = 0, sb = 0, n = 0;
        for (int i = 0; i < w * h; i++) {
            if (cov[i] <= 0) continue;
            sl += l[i]; sa += a[i]; sb += b[i]; n++;
        }
        if (n == 0) return null;
        double ml = sl / n, ma = sa / n, mb = sb / n;
        for (int i = 0; i < w * h; i++) {
            if (cov[i] > 0) continue;
            l[i] = ml; a[i] = ma; b[i] = mb;
        }
        return new double[][]{l, a, b};
    }
}
