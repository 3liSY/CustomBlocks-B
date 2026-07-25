/**
 * BgFringe.java — the anti-fringe peel (split out of BackgroundRemover for the §9.3 file-size
 * rule). Shaves the noise halo a SOURCE image carries against its own flat background (JPEG
 * ringing hugging a silhouette, a feathered glow baked onto white) WITHOUT eating a crisp subject
 * edge. The gate is the edge SHAPE, not colour alone: an edge pixel is peeled only where the image
 * is a gradient *descending toward the background* — closer to the bg colour than the pixel just
 * inside it — and never a pixel beyond PEEL_CAP. Climbs a halo ring by ring and stops dead at the
 * subject body.
 *
 * History (G10 §H Jar A): the peel was deleted together with the gamma-blend rim it also used to
 * chase, then restored the same day when the owner's chrome-logo JPEG showed its OTHER job was
 * real — source-baked ringing that no blend fix can remove. It is frontier-propagated: the first
 * pass scans the mask once for boundary pixels, every later pass tests only the non-bg neighbours
 * of the pixels just peeled, and ΔE00 comes memoised through BgDist — so the whole-image distance
 * precompute Jar A deleted stays deleted.
 *
 * Depends on: BgDist (memoised ΔE00 to the bake's background colour).
 * Called by:  image/BackgroundRemover (Stage 2, opaque-background bakes only).
 */
package com.customblocks.image;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

final class BgFringe {

    private BgFringe() {} // static-only

    /** Peel ceiling (ΔE00): never shave a pixel this far from the background — it is plainly
     *  subject, not halo. Re-derived for CIEDE2000 from the CIE76 value 45 by the same plateau
     *  ratio that rescaled MAX_DELTA_E (45 × 14.2/22 ≈ 29). Chrome-logo check: a silver outline's
     *  anti-alias pixel sits ≈35 ΔE00 from black, safely above the cap, while the JPEG ringing
     *  it exists to shave sits ≈10. */
    private static final double PEEL_CAP = 29.0;
    /** Minimum ΔE00 a ring must descend toward the bg (vs the pixel just inside it) to count as
     *  feather rather than flat subject. CIE76 1.5 rescaled by the same 14.2/22 ratio ≈ 1.0. */
    private static final double PEEL_MARGIN = 1.0;
    /** Max peel passes = rim depth cap in px. A pixel count, not a colour distance. */
    private static final int FRINGE_PASSES = 8;

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** Grow {@code isBg} over the descending-gradient halo along the mask boundary (in place). */
    static void peel(BufferedImage img, boolean[][] isBg, BgDist dist, int w, int h) {
        List<int[]> candidates = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!isBg[x][y] && touchesBg(isBg, x, y, w, h)) candidates.add(new int[]{x, y});
            }
        }
        for (int pass = 0; pass < FRINGE_PASSES && !candidates.isEmpty(); pass++) {
            List<int[]> fringe = new ArrayList<>();
            for (int[] p : candidates) {
                int x = p[0], y = p[1];
                if (isBg[x][y]) continue; // peeled by an earlier candidate this pass
                double own = dist.of(img.getRGB(x, y));
                if (own > PEEL_CAP) continue; // plainly subject — never peeled, whatever the shape
                for (int[] d : DIRS) {
                    int nx = x + d[0], ny = y + d[1];
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h || !isBg[nx][ny]) continue;
                    int ix = x - d[0], iy = y - d[1]; // inward neighbour = opposite the background side
                    double inward = (ix >= 0 && iy >= 0 && ix < w && iy < h)
                            ? dist.of(img.getRGB(ix, iy)) : Double.POSITIVE_INFINITY;
                    if (own < inward - PEEL_MARGIN) { fringe.add(p); break; }
                }
            }
            if (fringe.isEmpty()) return;
            candidates = new ArrayList<>();
            for (int[] p : fringe) isBg[p[0]][p[1]] = true;
            for (int[] p : fringe) { // next frontier = the non-bg neighbours of what just came off
                for (int[] d : DIRS) {
                    int nx = p[0] + d[0], ny = p[1] + d[1];
                    if (nx >= 0 && ny >= 0 && nx < w && ny < h && !isBg[nx][ny]) {
                        candidates.add(new int[]{nx, ny});
                    }
                }
            }
        }
    }

    private static boolean touchesBg(boolean[][] isBg, int x, int y, int w, int h) {
        for (int[] d : DIRS) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && ny >= 0 && nx < w && ny < h && isBg[nx][ny]) return true;
        }
        return false;
    }
}
