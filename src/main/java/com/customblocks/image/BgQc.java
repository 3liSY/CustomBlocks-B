/**
 * BgQc.java — the shared degenerate-mask quality gate every cascade rung must pass (G10 §H).
 *
 * <p>A rung proposes a background mask; this gate decides whether the mask is even the right SHAPE
 * to be a background, without knowing what the right answer was. The four checks are the four ways
 * every documented auto-removal failure looks from the outside: nothing removed, everything removed,
 * the mask shattered into confetti, or a "background" floating in the middle of the picture that
 * never reaches its edge. Failing any check demotes to the next rung — a failed mask is never
 * partially used.
 *
 * <p>Checking the mask's shape catches a wrong answer without needing to know the right one. That is
 * what turns "the algorithm was confident and wrong" into "the algorithm handed down".
 *
 * <p>No tunable constant lives here by design (§H: a number that cannot be explained is a tolerance
 * knob with a different name). The one size floor is geometric — see AREA_FLOOR_AXIS — and the
 * fragmentation bar is a plain majority, which is definitional rather than tuned.
 *
 * Depends on: nothing.
 * Called by:  image/BgCascade (once per proposed mask).
 */
package com.customblocks.image;

final class BgQc {

    private BgQc() {} // static-only

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * Smallest area that can count as a REGION, expressed as a multiple of the picture's shorter
     * axis: {@code min(w, h)} pixels, i.e. the area of a single one-pixel-wide strip laid across the
     * picture. Derivation is geometric, not empirical — a connected area that reaches the frame edge
     * yet covers less ground than one such strip is an edge artifact, not a background. The same
     * floor is applied to the surviving subject, because a "subject" below it means the mask ate the
     * picture. Scaling with the image rather than a pixel count keeps the meaning identical at any
     * resolution.
     */
    private static final int AREA_FLOOR_AXIS = 1;

    /** The geometric area floor described at {@link #AREA_FLOOR_AXIS}. Shared with the rungs so the
     *  cascade has exactly one definition of "too small to be an area". */
    static long areaFloor(int w, int h) {
        return (long) AREA_FLOOR_AXIS * Math.min(w, h);
    }

    /**
     * Reasons a proposed mask is rejected, phrased for a player. {@code null} means the mask passed.
     * Checks run cheapest-first: two counts, then a border scan, then component labelling last.
     */
    static String reject(boolean[][] mask, int w, int h) {
        if (mask == null) return "no mask was produced";
        final long floor = areaFloor(w, h);

        long bg = 0;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) if (mask[x][y]) bg++;
        }
        if (bg <= floor) return "found no background to remove";

        long fg = (long) w * h - bg;
        if (fg <= floor) return "would remove the whole picture";

        if (!touchesBorder(mask, w, h)) return "the background never reaches the picture edge";

        // Fragmentation: a real background is one region (plus any enclosed pockets), so its largest
        // connected piece must hold a MAJORITY of the mask. A colour key firing on scattered noise
        // fails this because no single piece dominates. Majority is definitional — a mask whose
        // biggest piece is a minority is by definition made mostly of other pieces.
        long largest = largestComponent(mask, w, h);
        if (largest * 2 <= bg) return "the background came out in scattered pieces, not one area";

        return null;
    }

    private static boolean touchesBorder(boolean[][] mask, int w, int h) {
        for (int x = 0; x < w; x++) if (mask[x][0] || mask[x][h - 1]) return true;
        for (int y = 0; y < h; y++) if (mask[0][y] || mask[w - 1][y]) return true;
        return false;
    }

    /**
     * Size of the largest 4-connected component of {@code mask}. Iterative DFS over an int stack of
     * packed {@code y * w + x} indices — no per-pixel object allocation, because this runs once per
     * rung per bake and the bulk rails bake every block on the server (§H cost budget).
     */
    private static long largestComponent(boolean[][] mask, int w, int h) {
        boolean[][] seen = new boolean[w][h];
        int[] stack = new int[Math.max(64, Math.min(w * h, 1 << 16))];
        long best = 0;

        for (int x0 = 0; x0 < w; x0++) {
            for (int y0 = 0; y0 < h; y0++) {
                if (!mask[x0][y0] || seen[x0][y0]) continue;
                int top = 0;
                seen[x0][y0] = true;
                stack[top++] = y0 * w + x0;
                long size = 0;
                while (top > 0) {
                    int p = stack[--top];
                    int px = p % w, py = p / w;
                    size++;
                    for (int[] d : DIRS) {
                        int nx = px + d[0], ny = py + d[1];
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        if (!mask[nx][ny] || seen[nx][ny]) continue;
                        seen[nx][ny] = true;
                        if (top == stack.length) {
                            int[] bigger = new int[(int) Math.min(Integer.MAX_VALUE - 8L, stack.length * 2L)];
                            System.arraycopy(stack, 0, bigger, 0, stack.length);
                            stack = bigger;
                        }
                        stack[top++] = ny * w + nx;
                    }
                }
                if (size > best) best = size;
            }
        }
        return best;
    }
}
