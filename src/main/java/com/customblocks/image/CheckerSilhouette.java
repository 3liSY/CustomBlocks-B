/**
 * CheckerSilhouette.java — where the artwork and the grid are the SAME colour (G10 §H, 2026-07-28).
 *
 * <p>{@link CheckerLattice} separates grid from artwork by geometry, which answers every case except
 * one: artwork painted in one of the grid's own two tones. The owner's football is that case — a pure
 * white ball on a white-and-grey transparency grid — and along its limb there is nothing to measure
 * pixel by pixel, because the ball and the light cells are the same 255 white. The colour pass took
 * whichever white cells looked like grid, so the ball baked with a scalloped bite out of its edge, one
 * bite per cell, and with pinpricks of backdrop inside its panels.
 *
 * <p>What separates them is PERIODICITY rather than colour, asked locally: a grid pixel has the other
 * tone one cell away and its own tone two cells away, and a white panel is still white a cell away in
 * every direction. Locally, because a stock preview is usually scaled and its cells then land on a
 * fractional period — measured on the football, cell boundaries at x=22 and again at x=699, a period of
 * 19.9 — so no single whole-picture phase describes them.
 *
 * <p>Where neither answer holds the pixel is left undecided, filled in from the nearest pixel that was
 * decided, and the resulting boundary is then straightened. That last step matters: evidence arrives in
 * cell-sized patches, so joining the nearest ones alone leaves a staircase with the grid's own tread.
 *
 * Depends on: CheckerLattice.Fit/window, CheckerboardDetector.labOf/de.
 * Called by:  image/CheckerboardDetector.
 */
package com.customblocks.image;

final class CheckerSilhouette {

    private CheckerSilhouette() {} // static-only

    /** How close a pixel must be to one of the two tones to be evidence at all. Shares
     *  CheckerboardDetector's kill verdict bar: the same "this pixel IS that tone". */
    private static final double SOLID_TOL_DE = 6.0;

    /**
     * Resolve the pixels the grid's own two tones cannot tell apart, by asking whether the picture
     * ALTERNATES where they sit (G10 §H, measured 2026-07-28).
     *
     * <p>The case this exists for is the owner's football: the ball is pure white and the grid's light
     * tone is pure white too, so along the ball's limb there is nothing to measure pixel by pixel. The
     * colour verdict took whichever white cells happened to look like grid, which is why the ball baked
     * with a scalloped bite out of its edge — one bite per cell — and with pinpricks of backdrop inside
     * its white panels.
     *
     * <p>What separates them is not colour but PERIODICITY, and it is asked locally rather than against
     * the fitted lattice: a stock preview is often scaled, so its cells land on a fractional period and
     * no single whole-picture phase describes them (measured on the football: cell boundaries at x=22 and
     * again at x=699, a period of 19.9). A local test needs no phase at all — one cell to the left, right,
     * above and below a grid pixel, the grid shows the OTHER tone; one cell away from a white panel, the
     * panel is still white.
     *
     * <ul>
     *   <li>alternates on one axis while the other does not CONTRADICT it → grid. A checkerboard is
     *       periodic in two dimensions and artwork routinely alternates in one, so a tone that repeats
     *       a cell away on the second axis still refuses — the boundary between the ball's lit face and
     *       its shaded side flips tone along x while staying put along y. But an axis whose probes land
     *       on nothing solid has said NOTHING, and along the ball's limb that is exactly what a real
     *       grid cell hears on the side facing the ball; treating that silence as a refusal left the
     *       ball ringed by a cell of its own backdrop.
     *   <li>repeats on both axes → artwork. A white panel a cell wide in every direction is not a grid
     *       cell whatever its colour.
     *   <li>anything else → undecided, and it takes the verdict of the nearest pixel that IS decided.
     *       Along the limb the decided pixels of both kinds run right up to the true silhouette from
     *       either side, so the boundary drawn through the doubt is the silhouette continued, not a cell
     *       edge.
     * </ul>
     *
     * <p>It can only ever SPARE — a pixel the colour pass did not kill is never killed here — so the
     * worst case is grid left behind, never artwork removed.
     */
    static void resolve(boolean[] kill, CheckerLattice.Fit fit, int[] argb, int w, int h,
                        double[] toneA, double[] toneB) {
        if (fit == null) return;
        final int[] tone = fit.tone();
        final int cell = fit.cell();
        final int n = w * h;

        // Only a pixel that IS one of the two tones is evidence; a pixel merely near enough to have
        // joined the candidate mask is a mixture and votes for nothing.
        boolean[] solid = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (tone[i] < 0) continue;
            double[] lab = CheckerboardDetector.labOf(argb[i]);
            solid[i] = Math.min(CheckerboardDetector.de(lab, toneA),
                                CheckerboardDetector.de(lab, toneB)) <= SOLID_TOL_DE;
        }

        final byte UNKNOWN = 0, GRID = 1, SUBJECT = 2;
        byte[] label = new byte[n];
        int[] q = new int[n];
        int head = 0, tail = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (tone[i] < 0) { label[i] = SUBJECT; q[tail++] = i; continue; } // no grid tone at all
                if (!solid[i]) continue;                                          // a mixture — no vote
                int t = tone[i];
                int xv = axisVote(tone, solid, w, h, x, y, cell, 0, t);
                int yv = axisVote(tone, solid, w, h, x, y, cell, 1, t);
                // One axis must positively keep the beat, and the other must not contradict it.
                // SILENCE IS NOT A VETO: a 0 means the probes a cell away landed on nothing solid —
                // which is what a real grid cell beside the ball hears on the side facing the ball.
                // Requiring both axes to beat made those cells undecided, the fill then handed them to
                // the nearest subject evidence, and the ball baked inside a ring of its own backdrop
                // one cell wide. Measured on the football, 2026-07-29: of the 9450 grid pixels the
                // repair spared, 6738 read one axis beating and the other silent. A tone that REPEATS
                // a cell away still answers -1 and still refuses, so artwork that alternates in one
                // direction only — the ball's own lit/shaded boundary — is unaffected.
                if ((xv > 0 && yv >= 0) || (xv >= 0 && yv > 0)) { label[i] = GRID; q[tail++] = i; }
                else if (xv < 0 && yv < 0) { label[i] = SUBJECT; q[tail++] = i; }
            }
        }
        if (tail == 0 || tail == n) return; // nothing to interpolate between
        // What may not move when the boundary is straightened: a pixel the picture positively answered.
        // Grid evidence is positive — the checkerboard's beat was actually seen there. "Subject" from a
        // shared tone is the ABSENCE of that evidence, and absence arrives a whole cell at a time, which
        // is what makes the interpolated edge step; those pixels are free to be straightened. Anything
        // that is not one of the two tones at all is artwork outright and never moves.
        boolean[] decided = new boolean[n];
        for (int i = 0; i < n; i++) decided[i] = label[i] == GRID || tone[i] < 0;

        // Nearest decided pixel wins, measured with a chamfer distance rather than by flooding. A
        // 4-connected flood measures city blocks, and city blocks make a diamond: the boundary it draws
        // between two coarse patches of evidence comes out as a staircase, which on the football's limb
        // is exactly the stepped edge this method exists to remove. 5-7-11 chamfer weights approximate
        // true distance to within 2 %, so the boundary lands where a straight line between the evidence
        // would put it.
        final int D_ORTH = 5, D_DIAG = 7, D_KNIGHT = 11;
        int[] dist = new int[n];
        java.util.Arrays.fill(dist, Integer.MAX_VALUE / 4);
        for (int i = 0; i < n; i++) if (label[i] != UNKNOWN) dist[i] = 0;

        for (int pass = 0; pass < 2; pass++) {
            int from = pass == 0 ? 0 : n - 1;
            int to = pass == 0 ? n : -1;
            int step = pass == 0 ? 1 : -1;
            for (int i = from; i != to; i += step) {
                int x = i % w, y = i / w;
                int best = dist[i];
                byte bl = label[i];
                for (int[] o : pass == 0 ? FORWARD : BACKWARD) {
                    int nx = x + o[0], ny = y + o[1];
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    int j = ny * w + nx;
                    int cost = o[2] == 1 ? D_ORTH : (o[2] == 2 ? D_DIAG : D_KNIGHT);
                    if (dist[j] + cost < best) { best = dist[j] + cost; bl = label[j]; }
                }
                dist[i] = best;
                label[i] = bl;
            }
        }

        smooth(label, decided, w, h, cell, GRID, SUBJECT);

        for (int i = 0; i < n; i++) if (kill[i] && label[i] != GRID) kill[i] = false;
    }

    /**
     * Straighten the boundary the interpolation drew, wherever it was drawn rather than measured.
     *
     * <p>The evidence along an ambiguous edge arrives in cell-sized patches, so joining the nearest ones
     * gives a boundary that steps from patch to patch — on the football's limb, a staircase with the
     * grid's own tread. A majority vote over a disk of half a cell is the standard cure: it is mean
     * curvature flow on a binary field, so a step half a cell deep is voted away while the smooth arc it
     * sits on is left where it is. Only pixels that were undecided can move; every pixel the picture
     * actually answered stays exactly as measured.
     */
    private static void smooth(byte[] label, boolean[] decided, int w, int h, int cell,
                               byte grid, byte subject) {
        final int r = Math.max(1, cell / 2);
        final int area = (2 * r + 1) * (2 * r + 1);
        int[] sum = new int[(w + 1) * (h + 1)];
        for (int pass = 0; pass < SMOOTH_PASSES; pass++) {
            java.util.Arrays.fill(sum, 0);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int p = (y + 1) * (w + 1) + (x + 1);
                    sum[p] = (label[y * w + x] == grid ? 1 : 0)
                            + sum[p - 1] + sum[p - (w + 1)] - sum[p - (w + 1) - 1];
                }
            }
            boolean moved = false;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int i = y * w + x;
                    if (decided[i]) continue;
                    int x0 = Math.max(0, x - r), x1 = Math.min(w - 1, x + r);
                    int y0 = Math.max(0, y - r), y1 = Math.min(h - 1, y + r);
                    int inBox = (x1 - x0 + 1) * (y1 - y0 + 1);
                    int gridCount = CheckerLattice.window(sum, w, x0, y0, x1, y1)
                            + (area - inBox) / 2; // off the picture edge, neither side gets the casting vote
                    byte want = gridCount * 2 > area ? grid : subject;
                    if (label[i] != want) { label[i] = want; moved = true; }
                }
            }
            if (!moved) break;
        }
    }

    /** How many times the boundary is straightened. Two passes settle every measured case; the loop
     *  stops early the moment a pass changes nothing. */
    private static final int SMOOTH_PASSES = 3;

    /** Chamfer neighbourhoods: {dx, dy, kind} with kind 1 = orthogonal, 2 = diagonal, 3 = knight. */
    private static final int[][] FORWARD = {
            {-1, 0, 1}, {0, -1, 1}, {-1, -1, 2}, {1, -1, 2},
            {-2, -1, 3}, {-1, -2, 3}, {1, -2, 3}, {2, -1, 3}};
    private static final int[][] BACKWARD = {
            {1, 0, 1}, {0, 1, 1}, {1, 1, 2}, {-1, 1, 2},
            {2, 1, 3}, {1, 2, 3}, {-1, 2, 3}, {-2, 1, 3}};

    /**
     * Does the picture keep the checkerboard's beat along this axis, as seen from one pixel?
     *
     * <p>{@code +1} when at least one direction shows the OTHER tone a cell away and this tone again two
     * cells away — the two-cell period a checkerboard has and a gradient does not. {@code -1} when
     * neither direction alternates at all, which is what an area of one colour looks like. {@code 0}
     * when the neighbours had nothing solid to say.
     *
     * <p>Asking for the other tone a cell away, rather than for "the same tone" as evidence AGAINST,
     * is what makes this work along the subject's edge: a real grid cell beside the white ball sees the
     * ball's white on one side, which is no evidence either way, and the grid's own dark cell on the
     * other, which is evidence. Reading the ball's white as counter-evidence — the earlier form — made
     * every grid cell touching the ball look like part of it.
     *
     * <p>Three offsets are sampled at each distance rather than one, because a scaled preview's cells
     * land on a fractional period and a single probe can fall on a seam.
     */
    private static int axisVote(int[] tone, boolean[] solid, int w, int h,
                                int x, int y, int cell, int axis, int t) {
        boolean beats = false, sawSolid = false;
        for (int sign = -1; sign <= 1; sign += 2) {
            int opposite = 0, same = 0;
            for (int d = cell - 1; d <= cell + 1; d++) {
                int j = sample(w, h, x, y, axis, sign * d);
                if (j < 0 || !solid[j]) continue;
                sawSolid = true;
                if (tone[j] == t) same++; else opposite++;
            }
            if (opposite <= same) continue; // this direction does not alternate
            int far = 0, farOff = 0;
            for (int d = 2 * cell - 2; d <= 2 * cell + 2; d++) {
                int j = sample(w, h, x, y, axis, sign * d);
                if (j < 0 || !solid[j]) continue;
                if (tone[j] == t) far++; else farOff++;
            }
            // Two cells away a checkerboard is back to this tone. Off the edge of the picture there is
            // nothing to check, and one confirmed flip is then evidence enough.
            if (far >= farOff) beats = true;
        }
        if (beats) return 1;
        return sawSolid ? -1 : 0;
    }

    /** Index {@code d} pixels away along {@code axis}, or -1 when that falls outside the picture. */
    private static int sample(int w, int h, int x, int y, int axis, int d) {
        int nx = axis == 0 ? x + d : x, ny = axis == 0 ? y : y + d;
        if (nx < 0 || ny < 0 || nx >= w || ny >= h) return -1;
        return ny * w + nx;
    }

}
