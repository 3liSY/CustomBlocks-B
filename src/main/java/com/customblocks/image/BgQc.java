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

        // Fragmentation: a colour key firing on scattered noise produces a mask made of many small
        // pieces spread through the picture, and that must be rejected. What identifies such a mask is
        // not the number of pieces — it is that the pieces are INTERIOR. A background is whatever the
        // picture opens onto, so every genuine piece of it reaches the picture edge.
        //
        // Requiring ONE dominant piece was the earlier form of this test, and it rejected a correct
        // mask whenever the subject was CROPPED: a planet running off the top and left edge cuts the
        // white behind it into corners of comparable size, no piece holds a majority, and the whole
        // removal declined on a picture nothing was wrong with (G10 §H, measured 2026-07-28). Cropped
        // subjects are ordinary, so that form was wrong rather than strict.
        //
        // The majority test is kept, applied to the border-touching pieces TOGETHER. Noise still fails
        // it — interior specks contribute nothing to that mass — while a background in four corners
        // passes, because all four reach the edge.
        long reaching = borderReachingMass(mask, w, h);
        if (reaching * 2 <= bg) return "the background came out in scattered pieces, not one area";

        // The same majority test, asked of the SUBJECT. Border-reaching alone is not enough: striped
        // artwork running edge to edge puts every stripe against the border, so a key that takes every
        // other stripe passes the test above while having cut the picture into ribbons (G10 §D6 —
        // one-way stripes must be left alone; measured 2026-07-28). A picture with a subject in it has
        // a subject: one main piece, whatever else floats around it. Artwork sliced into equal parts has
        // no main piece, and that is what identifies the mask as a pattern match rather than a
        // background.
        //
        // Both halves are needed and neither implies the other — the first rejects a key firing on
        // interior noise, the second rejects one firing on a repeating pattern.
        if (largestPiece(mask, w, h, false) * 2 <= fg) {
            return "the picture came apart into pieces, so that is a pattern and not a background";
        }

        return null;
    }

    /**
     * Size of the largest 4-connected run of pixels whose mask value equals {@code want}. Iterative DFS
     * over an int stack of packed indices, allocation-free per pixel for the same §H cost reason as
     * {@link #borderReachingMass}.
     */
    private static long largestPiece(boolean[][] mask, int w, int h, boolean want) {
        boolean[][] seen = new boolean[w][h];
        int[] stack = new int[Math.max(64, Math.min(w * h, 1 << 16))];
        long best = 0;

        for (int x0 = 0; x0 < w; x0++) {
            for (int y0 = 0; y0 < h; y0++) {
                if (mask[x0][y0] != want || seen[x0][y0]) continue;
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
                        if (mask[nx][ny] != want || seen[nx][ny]) continue;
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

    private static boolean touchesBorder(boolean[][] mask, int w, int h) {
        for (int x = 0; x < w; x++) if (mask[x][0] || mask[x][h - 1]) return true;
        for (int y = 0; y < h; y++) if (mask[0][y] || mask[w - 1][y]) return true;
        return false;
    }

    /**
     * Total size of every 4-connected component of {@code mask} that reaches the picture edge.
     *
     * <p>Flooded from the border inward in one pass, which is the same thing as labelling every
     * component and summing the ones that touch it, at a fraction of the cost. Iterative DFS over an
     * int stack of packed {@code y * w + x} indices — no per-pixel object allocation, because this runs
     * once per rung per bake and the bulk rails bake every block on the server (§H cost budget).
     */
    private static long borderReachingMass(boolean[][] mask, int w, int h) {
        boolean[][] seen = new boolean[w][h];
        int[] stack = new int[Math.max(64, Math.min(w * h, 1 << 16))];
        int top = 0;
        long size = 0;

        for (int x = 0; x < w; x++) {
            if (mask[x][0] && !seen[x][0]) { seen[x][0] = true; stack[top++] = x; }
            if (mask[x][h - 1] && !seen[x][h - 1]) { seen[x][h - 1] = true; stack[top++] = (h - 1) * w + x; }
        }
        for (int y = 0; y < h; y++) {
            if (mask[0][y] && !seen[0][y]) { seen[0][y] = true; stack[top++] = y * w; }
            if (mask[w - 1][y] && !seen[w - 1][y]) { seen[w - 1][y] = true; stack[top++] = y * w + w - 1; }
        }

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
        return size;
    }
}
