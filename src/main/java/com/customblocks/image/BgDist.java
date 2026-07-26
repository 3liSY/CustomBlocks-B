/**
 * BgDist.java — one bake's "ΔE00 to the background colour" oracle, memoised by exact ARGB
 * (alpha folded out — it plays no part in colour distance). A bake asks about the same colours
 * thousands of times: graphics sources repeat a small palette across millions of pixels, so
 * memoising the exact answer cuts the CIEDE2000 count by orders of magnitude with zero change to
 * any result. The cap only bounds heap on photographic sources with millions of distinct colours —
 * past it, distances are computed directly; outputs are identical with any cap value.
 *
 * Depends on: CieDe2000, BackgroundRemover.rgbToLab (the project's one sRGB→Lab).
 * Called by:  image/BgRungKey (region walk, pockets), image/BgRungSaliency, image/BgRungEnsemble.
 */
package com.customblocks.image;

import java.util.HashMap;

final class BgDist {

    private static final int CAP = 1 << 17; // ≈131k colours, a few MB — covers graphics fully

    private final HashMap<Integer, Double> memo = new HashMap<>();
    private final double[] bgLab;

    BgDist(double[] bgLab) {
        this.bgLab = bgLab;
    }

    double of(int argb) {
        int key = argb | 0xFF000000;
        Double v = memo.get(key);
        if (v != null) return v;
        double d = CieDe2000.of(BackgroundRemover.rgbToLab(key), bgLab);
        if (memo.size() < CAP) memo.put(key, d);
        return d;
    }
}
