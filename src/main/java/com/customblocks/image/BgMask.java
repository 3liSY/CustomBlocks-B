/**
 * BgMask.java — background-mask morphology helpers (split out of BackgroundRemover for the §9.3
 * file-size rule). Pure boolean-grid operations on the "is this pixel background?" mask:
 *
 *   despeckle           — area opening: drop tiny foreground specks, judged by component size.
 *   keepLargestForeground — SMART mode: reduce the foreground to its single largest connected blob.
 *
 * No image/colour types — just the mask. The old guarded morphological close was removed by
 * G10 §H Jar A (2026-07-25): the hysteresis flood in BackgroundRemover bridges near-tolerance
 * gaps by colour, which is what the close's geometry was for.
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

    /** Area opening: drop tiny isolated foreground islands (below a size scaled to the image) into
     *  the background. Judged purely by component size — a thin but long feature survives. The old
     *  guarded morphological close is gone (G10 §H): the hysteresis flood bridges near-tolerance
     *  gaps by colour, so no geometric bridging remains to swallow a 1-2 px feature. */
    static void despeckle(boolean[][] isBg, int w, int h) {
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

}
