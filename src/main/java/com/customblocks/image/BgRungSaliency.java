/**
 * BgRungSaliency.java — cascade rung 3: boundary-connectivity saliency (G10 §H).
 *
 * <p>The first rung that MEASURES rather than reads a fact. It answers "which of these areas is the
 * background?" using only spatial layout: a background wraps around the picture and therefore spends
 * much of itself along the frame border, while a subject is enclosed and touches the border barely or
 * not at all. No colour is assumed, so it works on a background that rung 2 could not key — a JPEG
 * whose flat blue is too noisy to read as one tone, a background with a gentle gradient.
 *
 * <p>The measure is boundary connectivity from Zhu, Liang, Wei &amp; Sun, "Saliency Optimization from
 * Robust Background Detection" (CVPR 2014):
 *
 * <pre>   BndCon(R) = (pixels of R lying on the frame border) / sqrt(area of R)</pre>
 *
 * <p>The square root is what makes it scale-free: a compact region's border contact grows like its
 * perimeter, i.e. like sqrt of its area, so the ratio does not drift as regions get bigger.
 *
 * <p>Regions are built by joining neighbouring pixels that are the same colour to a human eye — the
 * same JND bar rung 2 keys on. Transitive growth means a smoothly shaded background stays one region,
 * which is the case rung 2 could not handle.
 *
 * Depends on: BgQc.areaFloor, BgRungKey.JND, BackgroundRemover.rgbToLab, CieDe2000.
 * Called by:  image/BgCascade.
 */
package com.customblocks.image;

import java.awt.image.BufferedImage;
import java.util.HashMap;

final class BgRungSaliency {

    private BgRungSaliency() {} // static-only

    /**
     * Boundary connectivity at or above which a region counts as background.
     *
     * <p>Derivation is geometric, read straight off the measure. Take a square region of side s lying
     * flush against ONE frame edge: it contributes s border pixels over an area of s², so its BndCon
     * is s/s = 1 exactly. Put the same square in a CORNER, touching two edges, and it contributes 2s
     * over s² — BndCon = 2. A region sealed inside the picture touches no border at all and scores 0.
     *
     * <p>So 1 is the score of something merely resting against an edge, and 2 is the score of
     * something genuinely wrapping the frame. A background wraps; a subject that happens to be
     * cropped rests. Requiring 2 asks for the wrapping case and nothing weaker.
     */
    private static final double BG_BNDCON = 2.0;

    /**
     * Below this a region is treated as plainly enclosed — subject, not background. Between
     * {@link #FG_BNDCON} and {@link #BG_BNDCON} sits the band where the geometry does not say, and a
     * sizeable region landing in it is exactly the "no region separates by a clear margin" case that
     * makes rung 3 hand down instead of guessing. The gap between the two is the clear margin; both
     * ends are the derived square-flush and corner-flush scores, so neither is tuned.
     */
    private static final double FG_BNDCON = 1.0;

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** Lab cache bound, matching BgDist: past it Lab is recomputed rather than stored. */
    private static final int LAB_CAP = 1 << 17;

    /** Rung 3's proposed mask, or {@code null} when the layout does not separate cleanly. */
    static boolean[][] mask(BufferedImage img, int w, int h) {
        final int n = w * h;
        int[] argb = new int[n];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) argb[y * w + x] = img.getRGB(x, y) | 0xFF000000;
        }

        int[] label = new int[n];
        java.util.Arrays.fill(label, -1);
        HashMap<Integer, double[]> lab = new HashMap<>();
        int[] stack = new int[Math.max(64, Math.min(n, 1 << 16))];

        // Region areas and border-contact counts, grown as labels are handed out.
        long[] area = new long[16];
        long[] onBorder = new long[16];
        int regions = 0;

        for (int start = 0; start < n; start++) {
            if (label[start] >= 0) continue;
            if (regions == area.length) {
                area = java.util.Arrays.copyOf(area, regions * 2);
                onBorder = java.util.Arrays.copyOf(onBorder, regions * 2);
            }
            final int id = regions++;
            int top = 0;
            label[start] = id;
            stack[top++] = start;
            while (top > 0) {
                int p = stack[--top];
                int px = p % w, py = p / w;
                area[id]++;
                if (px == 0 || py == 0 || px == w - 1 || py == h - 1) onBorder[id]++;
                for (int[] d : DIRS) {
                    int nx = px + d[0], ny = py + d[1];
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    int q = ny * w + nx;
                    if (label[q] >= 0) continue;
                    if (!sameColour(argb[p], argb[q], lab)) continue;
                    label[q] = id;
                    if (top == stack.length) {
                        int[] bigger = new int[(int) Math.min(Integer.MAX_VALUE - 8L, stack.length * 2L)];
                        System.arraycopy(stack, 0, bigger, 0, stack.length);
                        stack = bigger;
                    }
                    stack[top++] = q;
                }
            }
        }

        final long floor = BgQc.areaFloor(w, h);
        boolean[] isBgRegion = new boolean[regions];
        boolean anyBg = false;
        for (int id = 0; id < regions; id++) {
            double bndCon = onBorder[id] / Math.sqrt((double) area[id]);
            if (bndCon >= BG_BNDCON) {
                isBgRegion[id] = true;
                anyBg = true;
            } else if (bndCon > FG_BNDCON && area[id] > floor) {
                return null; // a sizeable region the geometry cannot place — hand down, do not guess
            }
        }
        if (!anyBg) return null; // nothing wraps the frame

        boolean[][] out = new boolean[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) out[x][y] = isBgRegion[label[y * w + x]];
        }

        // Enclosed pockets of the SAME background — the counter of a letter "O", the gap between two
        // glyphs. Boundary connectivity scores an enclosed region 0 by construction, so saliency alone
        // can never reach one however plainly it is background; without this step the hole inside an O
        // keeps its original colour while everything around it is removed. Absorption reuses the
        // thickness-gated region walk (BgMask), so a real enclosed area fills and a hairline of subject
        // shading is still left alone.
        int bgKey = dominantColour(argb, label, isBgRegion, area, regions, n);
        BgDist keyDist = new BgDist(BackgroundRemover.rgbToLab(bgKey));
        boolean[][] pocket = new boolean[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!out[x][y] && keyDist.of(argb[y * w + x]) <= BgRungKey.JND) pocket[x][y] = true;
            }
        }
        BgMask.absorbEnclosedPockets(out, pocket, w, h);
        return out;
    }

    /**
     * Mean colour of the LARGEST accepted background region, as the colour an enclosed pocket must
     * match to count as more of the same background. The largest region is used rather than every
     * accepted one because averaging across several unrelated background areas can land on a colour
     * that appears in none of them.
     */
    private static int dominantColour(int[] argb, int[] label, boolean[] isBgRegion,
                                      long[] area, int regions, int n) {
        int biggest = -1;
        long bestArea = -1;
        for (int id = 0; id < regions; id++) {
            if (isBgRegion[id] && area[id] > bestArea) { bestArea = area[id]; biggest = id; }
        }
        long r = 0, g = 0, b = 0, count = 0;
        for (int p = 0; p < n; p++) {
            if (label[p] != biggest) continue;
            int px = argb[p];
            r += (px >> 16) & 0xFF;
            g += (px >> 8) & 0xFF;
            b += px & 0xFF;
            count++;
        }
        if (count == 0) return 0xFF000000;
        return 0xFF000000 | ((int) (r / count) << 16) | ((int) (g / count) << 8) | (int) (b / count);
    }

    /**
     * Whether two neighbouring pixels read as one colour. Identical bytes short-circuit before any
     * colour maths, which is the common case in flat artwork and keeps the scan affordable on the
     * bulk rails; only genuinely different neighbours cost a CIEDE2000.
     */
    private static boolean sameColour(int a, int b, HashMap<Integer, double[]> lab) {
        if (a == b) return true;
        return CieDe2000.of(labOf(a, lab), labOf(b, lab)) <= BgRungKey.JND;
    }

    private static double[] labOf(int argb, HashMap<Integer, double[]> cache) {
        double[] v = cache.get(argb);
        if (v != null) return v;
        v = BackgroundRemover.rgbToLab(argb);
        if (cache.size() < LAB_CAP) cache.put(argb, v);
        return v;
    }
}
