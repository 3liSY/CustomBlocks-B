/**
 * CheckerLattice.java — the geometric half of the flattened-checkerboard cleanup (G10 §H, 2026-07-28).
 *
 * <p>{@link CheckerboardDetector} finds candidate grid by COLOUR: a connected run carrying both checker
 * tones. That is a good first pass and a poor verdict, because artwork sharing a tone with the grid
 * joins the same run. Measured on the owner's football — white panels on a white-and-grey grid — panels
 * and grid are one component carrying both tones, so the colour test killed the ball's own face and it
 * baked with bites out of it.
 *
 * <p>No component-level number can separate them; at component level they are the same object. This
 * class supplies what colour cannot. A checkerboard is a square LATTICE whose tone flips with the
 * parity of the cell you stand in, and artwork is not on that lattice. The lattice is fitted from the
 * picture's own border — cell size from the border's run lengths, phase by trying every offset — and
 * each candidate is then kept or spared by how well its NEIGHBOURHOOD matches it.
 *
 * <p>Two details make that work on real sources rather than only clean ones:
 *
 * <ul>
 *   <li>The bar is calibrated on the border ring, where the grid is guaranteed to be showing, instead
 *       of being a constant. A clean PNG grid scores near 1.0; the same grid re-compressed as a JPEG
 *       scores far lower because its tones smear across cell edges. A fixed bar therefore asked lossy
 *       sources for evidence they cannot produce, and left the chrome JPEG mottled.
 *   <li>Where a neighbourhood holds too little grid to judge — which is the situation ALONG THE
 *       SUBJECT'S EDGE — the colour verdict stands. No evidence is not counter-evidence, and sparing
 *       those pixels left a pale ring tracing the football.
 * </ul>
 *
 * <p>Split out of CheckerboardDetector for the §9.3 file-size rule. One idea, no state.
 *
 * Depends on: CheckerboardDetector.labOf/de.
 * Called by:  image/CheckerboardDetector.
 */
package com.customblocks.image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class CheckerLattice {

    private CheckerLattice() {} // static-only

    /**
     * Chance agreement with a two-tone lattice. A pixel with no relationship to the grid matches it
     * half the time, so this is the floor any real evidence has to beat.
     */
    private static final double CHANCE = 0.5;

    /** Fewest mask pixels in a neighbourhood before the lattice is worth consulting. */
    private static final int MIN_CONTEXT = 8;

    /** Fewest tone runs along the border before a measured cell size is believable. */
    private static final int MIN_RUNS = 4;

    /** An enclosed piece must have this share of its pixels in the minority cell parity to have shown
     *  the grid alternating rather than sitting inside a single cell. */
    private static final double PARITY_MIN_FRAC = 0.10;

    /**
     * The lattice fitted to one picture, kept so the caller can run {@link #strandEnclosed} later —
     * after it has decided whether this grid is good enough to be handed on as authored transparency.
     */
    record Fit(int[] tone, int cell, int ox, int oy, int flip, double bar) {}

    /**
     * Spare every candidate that is not LOCALLY a checkerboard. {@code kill} is modified in place;
     * nothing is ever added to it, so this can only ever protect artwork, never remove more. Returns
     * the fitted lattice, or {@code null} when the picture gave no readable grid geometry.
     */
    static Fit confine(boolean[] kill, boolean[] mask, int[] argb, int w, int h, int n,
                       double[] toneA, double[] toneB) {
        int cell = cellSize(argb, mask, w, h, toneA, toneB);
        if (cell <= 0) return null;

        int[] tone = new int[n]; // 0 = tone A, 1 = tone B, -1 = not mask
        for (int i = 0; i < n; i++) {
            if (!mask[i]) { tone[i] = -1; continue; }
            tone[i] = nearer(argb[i], toneA, toneB);
        }

        int[] phase = bestPhase(tone, w, h, cell);
        int ox = phase[0], oy = phase[1], flip = phase[2];
        double ref = borderAgreement(tone, w, h, cell, ox, oy, flip);
        double bar = (CHANCE + Math.max(ref, CHANCE)) / 2.0;

        int[] agree = new int[(w + 1) * (h + 1)];
        int[] total = new int[(w + 1) * (h + 1)];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                int ag = 0, to = 0;
                if (tone[i] >= 0) {
                    to = 1;
                    if (tone[i] == parity(x, y, cell, ox, oy, flip)) ag = 1;
                }
                int p = (y + 1) * (w + 1) + (x + 1);
                agree[p] = ag + agree[p - 1] + agree[p - (w + 1)] - agree[p - (w + 1) - 1];
                total[p] = to + total[p - 1] + total[p - (w + 1)] - total[p - (w + 1) - 1];
            }
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (!kill[i]) continue;
                int x0 = Math.max(0, x - cell), x1 = Math.min(w - 1, x + cell);
                int y0 = Math.max(0, y - cell), y1 = Math.min(h - 1, y + cell);
                int tot = window(total, w, x0, y0, x1, y1);
                if (tot < MIN_CONTEXT) continue; // too little grid nearby to ask — colour verdict stands
                if (window(agree, w, x0, y0, x1, y1) < bar * tot) {
                    kill[i] = false; // no relationship to the lattice — painted, not printed
                }
            }
        }

        return new Fit(tone, cell, ox, oy, flip, bar);
    }

    /**
     * Second look at every killed piece the artwork completely encloses.
     *
     * <p>The per-pixel test above judges a neighbourhood, and a neighbourhood is the wrong unit for a
     * long thin streak: measured on the owner's football, the gloss highlights running down the white
     * panels sit in windows whose grid context comes from elsewhere, so they scored their way past the
     * bar and baked as holes through the ball.
     *
     * <p>A whole enclosed PIECE is the right unit. Real grid showing through a gap in the artwork
     * agrees with the fitted lattice from end to end; a painted streak agrees with it about half the
     * time, which is what "no relationship" looks like. Pieces that reach the image border are left
     * alone — that is the picture's actual backdrop, and it is not this rule's business. Pieces with
     * too few judged pixels are left alone too, on the same principle as above: no evidence is not
     * counter-evidence, and the narrow checker pockets trapped between glyphs live there.
     *
     * <p>Runs only where the grid is about to be handed on as authored transparency. On a source whose
     * grid was too smeared to hand on, the flatten's whole remaining job is to black the checkerboard
     * out for the colour rungs, and sparing pieces there just leaves them light — measured on the
     * chrome JPEG, that moved which rung won and took its removal from 81% to 28%.
     */
    static void strandEnclosed(boolean[] kill, Fit fit, int w, int h) {
        if (fit == null) return;
        final int[] tone = fit.tone();
        final int cell = fit.cell(), ox = fit.ox(), oy = fit.oy(), flip = fit.flip();
        final double bar = fit.bar();
        int n = w * h;
        boolean[] seen = new boolean[n];
        int[] q = new int[n];
        for (int s = 0; s < n; s++) {
            if (!kill[s] || seen[s]) continue;
            int head = 0, tail = 0; seen[s] = true; q[tail++] = s;
            boolean border = false;
            int agree = 0, judged = 0, even = 0, odd = 0;
            int mnx = w, mny = h, mxx = -1, mxy = -1;
            while (head < tail) {
                int i = q[head++], x = i % w, y = i / w;
                if (x < mnx) mnx = x;
                if (x > mxx) mxx = x;
                if (y < mny) mny = y;
                if (y > mxy) mxy = y;
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) border = true;
                if (tone[i] >= 0) {
                    judged++;
                    int want = parity(x, y, cell, ox, oy, flip);
                    if (want == 0) even++; else odd++;
                    if (tone[i] == want) agree++;
                }
                if (x + 1 < w && kill[i + 1] && !seen[i + 1]) { seen[i + 1] = true; q[tail++] = i + 1; }
                if (x > 0     && kill[i - 1] && !seen[i - 1]) { seen[i - 1] = true; q[tail++] = i - 1; }
                if (y + 1 < h && kill[i + w] && !seen[i + w]) { seen[i + w] = true; q[tail++] = i + w; }
                if (y > 0     && kill[i - w] && !seen[i - w]) { seen[i - w] = true; q[tail++] = i - w; }
            }
            if (border || judged < MIN_CONTEXT) continue;
            // Agreement is only evidence from a piece big enough to show the pattern. A checkerboard's
            // period is TWO cells — one of each tone — so anything narrower than that has not shown a
            // checkerboard, it has shown a patch, and a patch agrees or disagrees by luck. Measured on
            // the football: the specular streak down the left panel is 25×36 against a 20px cell, wide
            // enough to clip a second cell and score a pass, nowhere near wide enough to be a grid, and
            // it punched a hole through the ball. Both parities must also actually be present, which is
            // what "one of each tone" means when the piece is a ragged shape rather than a rectangle.
            int minor = Math.min(even, odd);
            boolean period = mxx - mnx + 1 >= 2 * cell && mxy - mny + 1 >= 2 * cell;
            boolean alternates = minor >= MIN_CONTEXT && minor >= PARITY_MIN_FRAC * judged;
            if (!period || !alternates || agree < bar * judged) {
                for (int k = 0; k < tail; k++) kill[q[k]] = false;
            }
        }
    }

    /** Which of the two tones a pixel is nearer to. */
    private static int nearer(int argb, double[] toneA, double[] toneB) {
        double[] lab = CheckerboardDetector.labOf(argb);
        return CheckerboardDetector.de(lab, toneA) <= CheckerboardDetector.de(lab, toneB) ? 0 : 1;
    }

    /** Which tone the lattice calls for at a coordinate. */
    private static int parity(int x, int y, int cell, int ox, int oy, int flip) {
        int cx = Math.floorDiv(x - ox, cell), cy = Math.floorDiv(y - oy, cell);
        return ((cx + cy) & 1) ^ flip;
    }

    /** Summed-area lookup over an inclusive box. Shared with CheckerSilhouette. */
    static int window(int[] sum, int w, int x0, int y0, int x1, int y1) {
        int s = w + 1;
        return sum[(y1 + 1) * s + (x1 + 1)] - sum[y0 * s + (x1 + 1)]
             - sum[(y1 + 1) * s + x0] + sum[y0 * s + x0];
    }

    /** How well the border ring — known grid — matches the fitted lattice. */
    private static double borderAgreement(int[] tone, int w, int h, int cell,
                                          int ox, int oy, int flip) {
        long hits = 0, seen = 0;
        for (int x = 0; x < w; x++) {
            for (int y : new int[]{0, h - 1}) {
                int i = y * w + x;
                if (tone[i] < 0) continue;
                seen++;
                if (tone[i] == parity(x, y, cell, ox, oy, flip)) hits++;
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x : new int[]{0, w - 1}) {
                int i = y * w + x;
                if (tone[i] < 0) continue;
                seen++;
                if (tone[i] == parity(x, y, cell, ox, oy, flip)) hits++;
            }
        }
        return seen == 0 ? 0.0 : hits / (double) seen;
    }

    /**
     * The lattice offset the border agrees with best, as {@code {ox, oy, flip}}. Every offset within
     * one cell is tried; {@code flip} decides which tone owns the even cells.
     */
    private static int[] bestPhase(int[] tone, int w, int h, int cell) {
        int bestOx = 0, bestOy = 0, bestFlip = 0;
        long best = -1;
        for (int oy = 0; oy < cell; oy++) {
            for (int ox = 0; ox < cell; ox++) {
                long hits = 0, seen = 0;
                for (int x = 0; x < w; x++) {
                    for (int y : new int[]{0, h - 1}) {
                        int i = y * w + x;
                        if (tone[i] < 0) continue;
                        seen++;
                        if (tone[i] == parity(x, y, cell, ox, oy, 0)) hits++;
                    }
                }
                for (int y = 0; y < h; y++) {
                    for (int x : new int[]{0, w - 1}) {
                        int i = y * w + x;
                        if (tone[i] < 0) continue;
                        seen++;
                        if (tone[i] == parity(x, y, cell, ox, oy, 0)) hits++;
                    }
                }
                long best0 = hits, best1 = seen - hits;
                long local = Math.max(best0, best1);
                if (local > best) {
                    best = local; bestOx = ox; bestOy = oy; bestFlip = best1 > best0 ? 1 : 0;
                }
            }
        }
        return new int[]{bestOx, bestOy, bestFlip};
    }

    /**
     * The grid's cell size in pixels — the median run of one tone along the border ring, where the
     * checkerboard is guaranteed to be showing. Returns 0 when no run pattern is readable, which
     * switches the confinement off rather than guessing a scale.
     */
    private static int cellSize(int[] argb, boolean[] mask, int w, int h,
                                double[] toneA, double[] toneB) {
        List<Integer> runs = new ArrayList<>();
        runsAlong(runs, argb, mask, w, toneA, toneB, w, 0, true);
        runsAlong(runs, argb, mask, w, toneA, toneB, w, h - 1, true);
        runsAlong(runs, argb, mask, w, toneA, toneB, h, 0, false);
        runsAlong(runs, argb, mask, w, toneA, toneB, h, w - 1, false);
        if (runs.size() < MIN_RUNS) return 0;
        Collections.sort(runs);
        return runs.get(runs.size() / 2);
    }

    /** Run lengths of a single tone along one border line. */
    private static void runsAlong(List<Integer> out, int[] argb, boolean[] mask, int w,
                                  double[] toneA, double[] toneB, int to, int fixed,
                                  boolean horizontal) {
        int cur = -1, len = 0;
        for (int k = 0; k < to; k++) {
            int i = horizontal ? fixed * w + k : k * w + fixed;
            if (!mask[i]) { if (len > 0) out.add(len); cur = -1; len = 0; continue; }
            int tone = nearer(argb[i], toneA, toneB);
            if (tone == cur) len++;
            else { if (len > 0) out.add(len); cur = tone; len = 1; }
        }
        if (len > 0) out.add(len);
    }
}
