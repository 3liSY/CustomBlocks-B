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

    /** Close 1-px gaps/pinholes in the bg mask, then drop tiny isolated foreground islands. */
    static void despeckle(boolean[][] isBg, int w, int h) {
        morphClose(isBg, w, h);
        dropForegroundSpecks(isBg, w, h);
    }

    /**
     * Morphological close (radius 1, 4-neighbour): dilate then erode. Net effect — fills 1-px
     * holes and bridges hairline gaps in the background mask while leaving larger shapes (the
     * subject) essentially unchanged. Out-of-bounds counts as background during the erode so the
     * solid border frame isn't eroded away.
     */
    private static void morphClose(boolean[][] mask, int w, int h) {
        boolean[][] dil = new boolean[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                boolean on = mask[x][y];
                if (!on) {
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
}
