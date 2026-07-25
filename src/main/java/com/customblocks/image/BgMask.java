/**
 * BgMask.java — background-mask morphology helpers (split out of BackgroundRemover for the §9.3
 * file-size rule). Pure boolean-grid operations on the "is this pixel background?" mask:
 *
 *   despeckle           — morphological close (fill 1-px gaps) then drop tiny foreground specks.
 *   keepLargestForeground — SMART mode: reduce the foreground to its single largest connected blob.
 *
 * No image/colour types — just the mask. Recycled verbatim from BackgroundRemover; behaviour
 * unchanged.
 *
 * Depends on: nothing.
 * Called by:  image/BackgroundRemover.
 */
package com.customblocks.image;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

final class BgMask {

    private BgMask() {} // static-only

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** Floor + divisor for the "tiny foreground island" area: a non-bg blob this small is noise. */
    private static final int SPECK_MIN = 4;
    private static final int SPECK_DIVISOR = 20_000; // ~13 px on a 512² image; tune if specks survive

    /** Pocket thickness floor, and the divisor that scales it with the image. A background-coloured
     *  region must hold a disk of this radius to count as a real enclosed area; anything thinner is
     *  subject shading and is left alone. Required width is {@code 2 * depth + 1} px. */
    private static final int POCKET_MIN_DEPTH = 2;
    private static final int POCKET_DEPTH_DIVISOR = 160; // 512² → 3 (7 px wide); 1024² → 6 (13 px wide)

    /** Close 1-px gaps/pinholes in the bg mask, then drop tiny isolated foreground islands. */
    static void despeckle(boolean[][] isBg, boolean[][] protect, int w, int h) {
        morphClose(isBg, protect, w, h);
        dropForegroundSpecks(isBg, w, h);
    }

    /**
     * Absorb background-coloured POCKETS into the background mask — the enclosed areas an edge flood
     * can never reach, such as the hole inside a letter "O" or the gap between two glyphs.
     *
     * <p>{@code pocket} carries every non-background pixel whose colour matches the background. Taking
     * all of them is what made "background + closed areas" destructive: dark shading inside a subject
     * matches a dark background, so a chrome logo lost its shadow lines and the repainted background
     * showed through the artwork. A genuine enclosed area is a REGION, so each connected component is
     * kept or dropped as a whole, and only components thick enough to hold a small disk are absorbed.
     * Thickness comes from a two-pass city-block distance transform, so the whole test is O(w·h).
     */
    static void absorbEnclosedPockets(boolean[][] isBg, boolean[][] pocket, int w, int h) {
        int minDepth = Math.max(POCKET_MIN_DEPTH, Math.min(w, h) / POCKET_DEPTH_DIVISOR);

        // City-block distance from each pocket pixel to the nearest non-pocket pixel (outside = 0).
        int[][] dist = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (!pocket[x][y]) { dist[x][y] = 0; continue; }
                int up = (y > 0) ? dist[x][y - 1] : 0;
                int left = (x > 0) ? dist[x - 1][y] : 0;
                dist[x][y] = Math.min(up, left) + 1;
            }
        }
        for (int x = w - 1; x >= 0; x--) {
            for (int y = h - 1; y >= 0; y--) {
                if (!pocket[x][y]) continue;
                int down = (y < h - 1) ? dist[x][y + 1] : 0;
                int right = (x < w - 1) ? dist[x + 1][y] : 0;
                dist[x][y] = Math.min(dist[x][y], Math.min(down, right) + 1);
            }
        }

        boolean[][] seen = new boolean[w][h];
        for (int x0 = 0; x0 < w; x0++) {
            for (int y0 = 0; y0 < h; y0++) {
                if (!pocket[x0][y0] || seen[x0][y0]) continue;
                List<int[]> blob = new ArrayList<>();
                Queue<int[]> q = new ArrayDeque<>();
                seen[x0][y0] = true;
                q.add(new int[]{x0, y0});
                int maxDepth = 0;
                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    blob.add(p);
                    if (dist[p[0]][p[1]] > maxDepth) maxDepth = dist[p[0]][p[1]];
                    for (int[] d : DIRS) {
                        int nx = p[0] + d[0], ny = p[1] + d[1];
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h && pocket[nx][ny] && !seen[nx][ny]) {
                            seen[nx][ny] = true;
                            q.add(new int[]{nx, ny});
                        }
                    }
                }
                if (maxDepth > minDepth) {
                    for (int[] p : blob) isBg[p[0]][p[1]] = true;
                }
            }
        }
    }

    /**
     * Anti-fringe peel — shave the soft anti-aliased halo a photo carries against a flat page WITHOUT
     * eating a crisp subject edge. The gate is the edge SHAPE, not colour alone: an edge pixel is peeled
     * only where the image is a gradient *descending toward the background*, i.e. it sits closer to the
     * background colour ({@code dToBg}, a precomputed ΔE grid) than the pixel just inside it. That climbs
     * a feather ring by ring and stops dead at the flat subject body, however pale that body is — a flat
     * light-grey logo edge is not a descending gradient, so it keeps its 1-px anti-alias and no more.
     *
     * <p>Distances come from the ORIGINAL pixels, so each pass reads the true gradient as the outer ring
     * is removed. {@code passes} caps the rim depth; {@code peelCap} stops a long gentle slope from being
     * chased deep into a real subject; {@code peelMargin} is how much a ring must descend to count as
     * feather rather than flat subject.
     */
    static void peelFringe(boolean[][] isBg, double[][] dToBg, int w, int h,
                           int passes, double peelCap, double peelMargin) {
        for (int pass = 0; pass < passes; pass++) {
            boolean[][] fringe = new boolean[w][h];
            boolean any = false;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (isBg[x][y] || dToBg[x][y] > peelCap) continue;
                    for (int[] d : DIRS) {
                        int nx = x + d[0], ny = y + d[1];
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h || !isBg[nx][ny]) continue;
                        int ix = x - d[0], iy = y - d[1]; // inward neighbour = opposite the background side
                        double inward = (ix >= 0 && iy >= 0 && ix < w && iy < h)
                                ? dToBg[ix][iy] : Double.POSITIVE_INFINITY;
                        if (dToBg[x][y] < inward - peelMargin) { fringe[x][y] = true; any = true; break; }
                    }
                }
            }
            if (!any) return;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) if (fringe[x][y]) isBg[x][y] = true;
            }
        }
    }

    /**
     * Grow the background mask inward, one ring per pass, but only across pixels the caller marked
     * {@code eligible}. A pixel joins the background when it is eligible and already touches background;
     * the next pass then sees the enlarged mask. Self-stops as soon as a pass adds nothing, so a run of
     * eligible pixels bounded by ineligible ones can never be chased further than it actually extends.
     *
     * <p>Used for the recolour path's dark anti-alias ring: eligibility is "near-black and opaque", so a
     * bright subject bounds the growth immediately and {@code passes} caps its depth regardless.
     */
    static void growInto(boolean[][] isBg, boolean[][] eligible, int w, int h, int passes) {
        for (int pass = 0; pass < passes; pass++) {
            boolean[][] grow = new boolean[w][h];
            boolean any = false;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (isBg[x][y] || !eligible[x][y]) continue;
                    for (int[] d : DIRS) {
                        int nx = x + d[0], ny = y + d[1];
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h && isBg[nx][ny]) { grow[x][y] = true; any = true; break; }
                    }
                }
            }
            if (!any) return;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) if (grow[x][y]) isBg[x][y] = true;
            }
        }
    }

    /**
     * Morphological close (radius 1, 4-neighbour): dilate then erode. Net effect — fills 1-px
     * holes and bridges hairline gaps in the background mask while leaving larger shapes (the
     * subject) essentially unchanged. Out-of-bounds counts as background during the erode so the
     * solid border frame isn't eroded away.
     *
     * <p>{@code protect} marks pixels whose colour is plainly not the background. The dilate step skips
     * them, because a close over a subject feature 1-2 px wide is not reversible: the dilate joins the
     * background across the feature and the erode cannot reopen it, so thin outlines and small strokes
     * were being deleted from the bake. Near-tolerance pixels — the ones this close exists to bridge —
     * are not protected, so the original hairline-gap fix is unaffected.
     */
    private static void morphClose(boolean[][] mask, boolean[][] protect, int w, int h) {
        boolean[][] dil = new boolean[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                boolean on = mask[x][y];
                if (!on && !protect[x][y]) {
                    for (int[] d : DIRS) {
                        int nx = x + d[0], ny = y + d[1];
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h && mask[nx][ny]) { on = true; break; }
                    }
                }
                dil[x][y] = on;
            }
        }
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                boolean on = dil[x][y];
                if (on) {
                    for (int[] d : DIRS) {
                        int nx = x + d[0], ny = y + d[1];
                        boolean nb = (nx < 0 || nx >= w || ny < 0 || ny >= h) || dil[nx][ny]; // OOB = bg
                        if (!nb) { on = false; break; }
                    }
                }
                mask[x][y] = on;
            }
        }
    }

    /** Flood each foreground (non-bg) island; islands at or below the speck area become background. */
    private static void dropForegroundSpecks(boolean[][] isBg, int w, int h) {
        int speckArea = Math.max(SPECK_MIN, (w * h) / SPECK_DIVISOR);
        boolean[][] seen = new boolean[w][h];
        for (int x0 = 0; x0 < w; x0++) {
            for (int y0 = 0; y0 < h; y0++) {
                if (isBg[x0][y0] || seen[x0][y0]) continue;
                List<int[]> blob = new ArrayList<>();
                Queue<int[]> q = new ArrayDeque<>();
                seen[x0][y0] = true;
                q.add(new int[]{x0, y0});
                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    blob.add(p);
                    for (int[] d : DIRS) {
                        int nx = p[0] + d[0], ny = p[1] + d[1];
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h && !isBg[nx][ny] && !seen[nx][ny]) {
                            seen[nx][ny] = true;
                            q.add(new int[]{nx, ny});
                        }
                    }
                }
                if (blob.size() <= speckArea) {
                    for (int[] p : blob) isBg[p[0]][p[1]] = true;
                }
            }
        }
    }

    /**
     * SMART mode: reduce the foreground to its single largest connected component. Every other
     * foreground pixel becomes background. Safe by construction — if there is no foreground at all
     * it is a no-op, and the caller wraps everything in a try/catch that returns the original image.
     */
    static void keepLargestForeground(boolean[][] isBg, int w, int h) {
        boolean[][] seen = new boolean[w][h];
        List<int[]> largest = null;
        for (int x0 = 0; x0 < w; x0++) {
            for (int y0 = 0; y0 < h; y0++) {
                if (isBg[x0][y0] || seen[x0][y0]) continue;
                List<int[]> blob = new ArrayList<>();
                Queue<int[]> q = new ArrayDeque<>();
                seen[x0][y0] = true;
                q.add(new int[]{x0, y0});
                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    blob.add(p);
                    for (int[] d : DIRS) {
                        int nx = p[0] + d[0], ny = p[1] + d[1];
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h && !isBg[nx][ny] && !seen[nx][ny]) {
                            seen[nx][ny] = true;
                            q.add(new int[]{nx, ny});
                        }
                    }
                }
                if (largest == null || blob.size() > largest.size()) largest = blob;
            }
        }
        if (largest == null) return; // no subject found — leave the mask as-is
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) if (!isBg[x][y]) isBg[x][y] = true; // everything → bg…
        }
        for (int[] p : largest) isBg[p[0]][p[1]] = false;                   // …then carve back the subject
    }

    /**
     * Disk-dilate the {@code seed} mask by {@code radius} px, marking only pixels that are background
     * ({@code isBg[x][y]}). Returns a fresh mask. Used to grow the thin white keyline outward from the
     * subject's dark silhouette edge: stamping a Euclidean disk per seed gives an even, rounded width
     * that reads as a clean anti-aliased outline once the texture is downscaled. Pure mask op — the
     * caller decides which seeds are "dark edges"; this only grows them into the background.
     */
    static boolean[][] dilateInto(boolean[][] seed, boolean[][] isBg, int w, int h, int radius) {
        boolean[][] out = new boolean[w][h];
        int r2 = radius * radius;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!seed[x][y]) continue;
                for (int dy = -radius; dy <= radius; dy++) {
                    int ny = y + dy;
                    if (ny < 0 || ny >= h) continue;
                    for (int dx = -radius; dx <= radius; dx++) {
                        int nx = x + dx;
                        if (nx < 0 || nx >= w || dx * dx + dy * dy > r2) continue;
                        if (isBg[nx][ny]) out[nx][ny] = true;
                    }
                }
            }
        }
        return out;
    }
}
