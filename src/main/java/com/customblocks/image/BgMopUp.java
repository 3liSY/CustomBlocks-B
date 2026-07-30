/**
 * BgMopUp.java — the mop-up pass: leftover foreground that is, as a piece, the background colour
 * (G10 §H, 2026-07-26; second unit added 2026-07-27).
 *
 * <p>It exists because {@link BgMask#despeckle} is deliberately colour-blind — it judges a foreground
 * island purely on area, which is what lets it drop a speck while keeping a hair-thin outline. Between
 * its area floor and the smallest area the cascade will call a REGION sits the band the letter-O
 * regression lived in: JPEG ringing knocks blocks of the blue background just outside rung 2's JND
 * match, so they survive the flood as foreground and bake as blue dots around the glyph.
 *
 * <p>Widening a rung's match would take those fragments and everything else within the wider bar,
 * which is the subject-eating trade §H is built to avoid. This pass instead keeps the strict match for
 * the mask and applies a looser bar ONLY to a piece already too small to be a region.
 *
 * <h2>Leftover background comes in two shapes, so the pass has two units (§HA4, measured 2026-07-27)</h2>
 *
 * The first version knew only one: connected components of RAW FOREGROUND, judged by mean colour.
 * Measured on the real letter-O source, that pass spared exactly ZERO fragments on colour — while 857
 * pixels of the background colour survived in the bake, every one of them inside a single 89,335-pixel
 * component. The ringing sits directly against the glyph, so it is 4-connected to it: subject and
 * residue are one component, a hundred times over the size floor, and the oversize skip dropped the
 * whole thing without colour-judging a single pixel of it. No value of the size floor or the colour bar
 * could have reached that residue. The numbers were never the fault — one unit was missing.
 *
 * <ul>
 *   <li>{@link #absorbDetached} — the original unit. A whole foreground island whose MEAN is the
 *       background colour. The mean is what cancels compression noise across a fragment of mixed
 *       pixels, so it catches residue no per-pixel rule would (the letter-O source's watermark, whose
 *       glyphs are individually well off the key, is absorbed only by this one).</li>
 *   <li>{@link #absorbFused} — residue welded to the subject's own edge, which never forms an island.
 *       Selecting the near-key pixels FIRST and taking connected pieces of those is what lets the
 *       fringe be seen at all: the subject's body is nowhere near the key, so it is not in the
 *       selection and cannot swallow the residue attached to it.</li>
 * </ul>
 *
 * <p>Both numbers are unchanged and still derived — {@link BgQc#areaFloor} for "too small to be a
 * region", 2 × the JND for "this is that colour" — and the mask's own match stays strict.
 *
 * <p>A fused piece is dropped only when it also TOUCHES the background mask. That is a fact, not a
 * threshold: a background-coloured piece welded to the background boundary is where the strict flood
 * stopped, while one floating inside the subject is the subject's own shading — the same distinction
 * {@link BgMask#absorbEnclosedPockets} makes, and for the same reason (a dark logo's shadow lines match
 * a dark background and must survive).
 *
 * <p>Rung 1 does not run it. Authored alpha is the author's own statement of what is background, and
 * second-guessing it by colour would replace a fact with a measurement.
 *
 * Depends on: BgQc.areaFloor, BgRungKey.JND, BgDist, BackgroundRemover.rgbToLab, CieDe2000.
 * Called by:  image/BgCascade (rungs 2, 3 and 4, after despeckle).
 */
package com.customblocks.image;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

final class BgMopUp {

    private BgMopUp() {} // static-only

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * The mop-up colour bar, as a multiple of rung 2's JND.
     *
     * <p>Derivation: one JND is the point at which two colours first become distinguishable WHEN
     * COMPARED SIDE BY SIDE. A leftover fragment is not seen side by side — it sits surrounded by the
     * key colour it came from — so the bar for "this fragment is that colour" is the next honest step
     * out, twice the JND. It is not a tuned tolerance: nothing above the size floor is ever judged by
     * it, and the mask's own match stays at the strict JND.
     */
    private static final double JND_MULTIPLE = 2.0;

    /**
     * Absorb every leftover piece of background colour that is too small to be a region.
     *
     * <p>Size floor is {@link BgQc#areaFloor} — the cascade's existing definition of "too small to be
     * an area", reused rather than restated so the two cannot drift apart.
     *
     * <p>Detached islands go first, because that pass reads the mask the rung produced. Absorbing the
     * fused fringe first would grow the mask along every subject edge and change what the island pass
     * sees, for no gain: the two shapes do not overlap.
     *
     * @param isBg the mask, modified in place
     * @param px   the picture as row-major ARGB
     * @param key  the background colour this mask was decided from, as {@code 0xFFRRGGBB}
     */
    static void sweep(boolean[][] isBg, int[] px, int w, int h, int key) {
        final long floor = BgQc.areaFloor(w, h);
        final double bar = BgRungKey.JND * JND_MULTIPLE;
        absorbDetached(isBg, px, w, h, key, floor, bar);
        absorbFused(isBg, px, w, h, key, floor, bar);
        absorbStamped(isBg, px, w, h, key, floor);
        for (int pass = 0; pass < LOCAL_PASSES; pass++) {
            if (!absorbLocal(isBg, px, w, h, floor, bar)) break;
        }
        absorbRim(isBg, px, w, h, bar);
    }

    /**
     * Unit 5 — the RIM (G10 §H, measured 2026-07-28).
     *
     * <p>Every unit above judges a PIECE: it collects a connected fragment and asks whether the piece
     * as a whole is leftover background. That is the right shape for a crumb and the wrong shape for a
     * halo. The pale outline the owner reported around Jupiter and the penguin is one connected ring
     * following the whole silhouette, far larger than {@link BgQc#areaFloor}, so every unit classified
     * it as "a real area" and spared it — while it is, pixel for pixel, the backdrop.
     *
     * <p>It exists because a mask boundary is drawn where the STRICT match stops, and on any real edge
     * the picture keeps reading as backdrop for a pixel or two past that: resampling and compression
     * smear the transition, and a photographic limb is genuinely soft. Those pixels are the backdrop
     * seen through a partial edge, not artwork.
     *
     * <p>So this unit judges pixels, not pieces — the background edge grows outward one ring at a time
     * while the picture at that pixel still matches the CLEAN PLATE at the same spot, and stops the
     * moment it does not. Three properties keep it from running away, and none is a tuned number:
     *
     * <ul>
     *   <li>It only ever starts from the mask, so it cannot open a hole anywhere else.
     *   <li>It stops at the first pixel that is not the backdrop's colour, which for artwork is
     *       immediately — the penguin's outline is black against white, the letter O's stroke white
     *       against blue.
     *   <li>It is depth-capped at {@link #rimDepth}, the width an edge transition can physically have.
     * </ul>
     *
     * <p>The bar is the same 2×JND the other units use, and the comparison is local, so a fading
     * backdrop is handled for free — the halo on a gradient is as absorbable as one on a flat colour.
     */
    private static void absorbRim(boolean[][] isBg, int[] px, int w, int h, double bar) {
        final int depth = rimDepth(w, h);
        for (int ring = 0; ring < depth; ring++) {
            double[][] plate = BgPlate.build(px, isBg, w, h);
            if (plate == null) return;

            boolean[][] add = new boolean[w][h];
            boolean any = false;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (isBg[x][y]) continue;
                    boolean touches = false;
                    for (int[] d : DIRS) {
                        int nx = x + d[0], ny = y + d[1];
                        if (nx >= 0 && ny >= 0 && nx < w && ny < h && isBg[nx][ny]) { touches = true; break; }
                    }
                    if (!touches) continue;
                    int i = y * w + x;
                    if (CieDe2000.of(BackgroundRemover.rgbToLab(px[i] | 0xFF000000),
                                     BgPlate.labOf(plate[i])) <= bar) {
                        add[x][y] = true; any = true;
                    }
                }
            }
            if (!any) return;
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) if (add[x][y]) isBg[x][y] = true;
            }
        }
    }

    /**
     * How far the rim may grow, in pixels.
     *
     * <p>An edge transition is a physical width, so this scales with the picture rather than being a
     * constant: two pixels on a small source, more on a large one where the same soft limb covers more
     * of them. The divisor puts a 735 px photograph at 3 and a 4000 px one at 20, which is the range
     * measured edges actually occupy; the floor of 2 matches rung 5's band, the existing §H statement
     * of how wide one anti-aliased edge is.
     */
    private static int rimDepth(int w, int h) {
        return Math.max(2, Math.min(w, h) / 200);
    }

    /**
     * How many times unit 4 may peel (G10 §HA4, 2026-07-28).
     *
     * <p>Unit 4 only absorbs a piece TOUCHING the mask, which on a soft edge is one layer of residue.
     * Absorbing it exposes the next layer, so a single pass leaves a thinner version of the same
     * problem — measured on the letter O, one pass takes 30 blue pixels to 18. Repeating is not a
     * loosened bar: every pass applies exactly the same three conditions, so a pixel that never
     * qualifies never will, and the loop stops the moment a pass changes nothing.
     *
     * <p>The cap exists only so a pathological picture cannot walk the mask inward forever. Four is
     * past the point any measured edge keeps producing new layers; the letter O settles before it.
     */
    private static final int LOCAL_PASSES = 4;

    /**
     * Unit 4 — residue judged against the CLEAN PLATE instead of the key (G10 §HA4, 2026-07-28).
     *
     * <p>Units 1-3 all measure against one colour, so on a fading backdrop they can only reach the
     * residue sitting near the tone the key was read from. The letter-O backdrop spans 6.5 ΔE00
     * corner to corner — wider than this pass's own 2×JND bar — so ringing near the far corner reads as
     * subject to every global test while being a dead match for the backdrop touching it. That is the
     * whole remaining speckle population.
     *
     * <p>So this unit re-runs unit 2's shape — small piece, welded to the subject, touching the mask —
     * with ONE substitution: each pixel is compared to {@link BgPlate}'s estimate AT ITS OWN
     * COORDINATE instead of to the key. The bar is the same 2×JND units 1 and 2 use, deliberately: the
     * fade and the compression noise are two separate errors, the plate cancels the first, and the
     * second still needs the allowance it always did. Measured on the letter-O baseline the residue
     * sits 4.8-6.3 ΔE00 from the key — just outside the bar — and 3.2-3.9 from the plate, inside it.
     * That gap is the whole fix; tightening the bar as well would close it again.
     *
     * <p>The three conditions that make it safe are unit 2's, unchanged and still facts rather than
     * thresholds — under {@link BgQc#areaFloor} is the cascade's own "too small to be a region",
     * touching the mask is where the strict flood stopped, and a piece floating inside the subject is
     * that subject's shading and is left alone. What changes is only what "is that colour" means.
     */
    private static boolean absorbLocal(boolean[][] isBg, int[] px, int w, int h,
                                       long floor, double bar) {
        final double[][] plate = BgPlate.build(px, isBg, w, h);
        if (plate == null) return false;

        boolean[][] cand = new boolean[w][h];
        boolean any = false;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (isBg[x][y]) continue;
                int i = y * w + x;
                if (CieDe2000.of(BackgroundRemover.rgbToLab(px[i]), BgPlate.labOf(plate[i])) <= bar) {
                    cand[x][y] = true; any = true;
                }
            }
        }
        if (!any) return false;

        boolean changed = false;
        boolean[][] seen = new boolean[w][h];
        for (int x0 = 0; x0 < w; x0++) {
            for (int y0 = 0; y0 < h; y0++) {
                if (!cand[x0][y0] || seen[x0][y0]) continue;

                List<int[]> piece = new ArrayList<>();
                Queue<int[]> q = new ArrayDeque<>();
                seen[x0][y0] = true;
                q.add(new int[]{x0, y0});
                long size = 0;
                boolean touchesBg = false;
                boolean oversize = false;

                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    size++;
                    if (size > floor) {
                        if (!oversize) { oversize = true; piece.clear(); }
                    } else {
                        piece.add(p);
                    }
                    for (int[] d : DIRS) {
                        int nx = p[0] + d[0], ny = p[1] + d[1];
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        if (isBg[nx][ny]) { touchesBg = true; continue; }
                        if (!cand[nx][ny] || seen[nx][ny]) continue;
                        seen[nx][ny] = true;
                        q.add(new int[]{nx, ny});
                    }
                }

                if (oversize) continue;   // a real area — never mopped
                if (!touchesBg) continue; // floating inside the subject — its own shading
                for (int[] p : piece) isBg[p[0]][p[1]] = true;
                changed = !piece.isEmpty() || changed;
            }
        }
        return changed;
    }

    /**
     * Unit 3 — a STAMP: a watermark's own ink (G10 §HA4, 2026-07-27). Units 1 and 2 both ask "is this
     * leftover the background colour", and the ink is not — measured on the letter-O baseline at 81 %
     * of the way to white and 40.5 ΔE00 from the key, with only 7 % of it close enough to the veil line
     * to be caught there. A stamp is not background showing through; it is pigment laid over it.
     *
     * <p>Four conditions must hold at once, and together they are what make this safe to run at all:
     *
     * <ul>
     *   <li>ISOLATED — every neighbour the island has is background. Not "touches background", which
     *       units 1 and 2 use: touching NOTHING ELSE. The subject is therefore unreachable by
     *       construction. A stamp printed across the artwork is welded to it and does not qualify,
     *       which is the correct outcome: erasing it would leave a hole in the artwork.
     *   <li>SMALL — under {@link BgQc#areaFloor}, the cascade's existing "too small to be a region".
     *       The white letter O is one component far above it and cannot qualify on any picture.
     *   <li>PALE — lighter than the background and less saturated than it, which is what laying white
     *       over a colour does. A small DARK detail floating on a light background is untouched.
     *   <li>CHROMATIC KEY — {@link BgVeil#applies}. Against a black or grey background "lighter and
     *       less saturated" describes every highlight, which is the chrome logo's silver bevel, so the
     *       whole unit switches off there.
     * </ul>
     */
    private static void absorbStamped(boolean[][] isBg, int[] px, int w, int h, int key, long floor) {
        if (!BgVeil.applies(key)) return;
        final double[] keyLab = BackgroundRemover.rgbToLab(key);
        final double keyChroma = Math.hypot(keyLab[1], keyLab[2]);
        boolean[][] seen = new boolean[w][h];

        for (int x0 = 0; x0 < w; x0++) {
            for (int y0 = 0; y0 < h; y0++) {
                if (isBg[x0][y0] || seen[x0][y0]) continue;

                List<int[]> blob = new ArrayList<>();
                Queue<int[]> q = new ArrayDeque<>();
                seen[x0][y0] = true;
                q.add(new int[]{x0, y0});
                double sumL = 0, sumA = 0, sumB = 0;
                long size = 0;
                boolean oversize = false;
                boolean touchesFrame = false;

                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    size++;
                    if (size > floor) {
                        if (!oversize) { oversize = true; blob.clear(); }
                    } else {
                        blob.add(p);
                        double[] lab = BackgroundRemover.rgbToLab(px[p[1] * w + p[0]]);
                        sumL += lab[0]; sumA += lab[1]; sumB += lab[2];
                    }
                    for (int[] d : DIRS) {
                        int nx = p[0] + d[0], ny = p[1] + d[1];
                        // The frame is not background, so an island running off the edge of the picture
                        // is not enclosed by anything and must not be read as isolated.
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) { touchesFrame = true; continue; }
                        if (isBg[nx][ny] || seen[nx][ny]) continue;
                        seen[nx][ny] = true;
                        q.add(new int[]{nx, ny});
                    }
                }

                if (oversize || touchesFrame) continue;
                double[] mean = {sumL / size, sumA / size, sumB / size};
                boolean lighter = mean[0] > keyLab[0];
                boolean paler = Math.hypot(mean[1], mean[2]) < keyChroma;
                if (lighter && paler) {
                    for (int[] p : blob) isBg[p[0]][p[1]] = true;
                }
            }
        }
    }

    /**
     * Unit 1 — a whole foreground island whose MEAN Lab is the background colour, judged on the mean
     * for the same reason despeckle judges by area rather than width: averaging is exactly what cancels
     * the compression noise that created the fragment, while a real subject detail keeps its own colour
     * under the average.
     */
    private static void absorbDetached(boolean[][] isBg, int[] px, int w, int h, int key,
                                       long floor, double bar) {
        final double[] keyLab = BackgroundRemover.rgbToLab(key);
        boolean[][] seen = new boolean[w][h];

        for (int x0 = 0; x0 < w; x0++) {
            for (int y0 = 0; y0 < h; y0++) {
                if (isBg[x0][y0] || seen[x0][y0]) continue;

                List<int[]> blob = new ArrayList<>();
                Queue<int[]> q = new ArrayDeque<>();
                seen[x0][y0] = true;
                q.add(new int[]{x0, y0});
                double sumL = 0, sumA = 0, sumB = 0;
                long size = 0;
                boolean oversize = false;

                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    size++;
                    // Past the floor the island can no longer qualify, so stop paying for its colour
                    // and drop the pixels already collected — on a full-frame subject that list would
                    // otherwise hold the whole foreground. The walk itself must finish: it is what
                    // marks the component seen, and abandoning it would re-enter the same pixels from
                    // every other side.
                    if (size > floor) {
                        if (!oversize) { oversize = true; blob.clear(); }
                    } else {
                        blob.add(p);
                        double[] lab = BackgroundRemover.rgbToLab(px[p[1] * w + p[0]]);
                        sumL += lab[0]; sumA += lab[1]; sumB += lab[2];
                    }
                    for (int[] d : DIRS) {
                        int nx = p[0] + d[0], ny = p[1] + d[1];
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h && !isBg[nx][ny] && !seen[nx][ny]) {
                            seen[nx][ny] = true;
                            q.add(new int[]{nx, ny});
                        }
                    }
                }

                if (oversize) continue; // a real area — never mopped, whatever colour it is
                double[] mean = {sumL / size, sumA / size, sumB / size};
                if (CieDe2000.of(mean, keyLab) <= bar) {
                    for (int[] p : blob) isBg[p[0]][p[1]] = true;
                }
            }
        }
    }

    /**
     * Unit 2 — residue welded to the subject, which is never an island of its own. Candidates are the
     * foreground pixels that are themselves the background colour; connected pieces are taken OF THOSE,
     * and a piece is absorbed when it is under the region floor and touches the mask.
     */
    private static void absorbFused(boolean[][] isBg, int[] px, int w, int h, int key,
                                    long floor, double bar) {
        // Memoised through BgDist because a graphics source repeats a small palette across the whole
        // picture, so this full-frame pass costs a few thousand real CIEDE2000 calls, not w·h of them.
        final BgDist dist = new BgDist(BackgroundRemover.rgbToLab(key));
        final boolean veil = BgVeil.applies(key);
        boolean[][] cand = new boolean[w][h];
        boolean any = false;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (isBg[x][y]) continue;
                int argb = px[y * w + x];
                if (dist.of(argb) <= bar || (veil && BgVeil.veiled(argb, key, bar))) {
                    cand[x][y] = true; any = true;
                }
            }
        }
        if (!any) return;

        boolean[][] seen = new boolean[w][h];
        for (int x0 = 0; x0 < w; x0++) {
            for (int y0 = 0; y0 < h; y0++) {
                if (!cand[x0][y0] || seen[x0][y0]) continue;

                List<int[]> piece = new ArrayList<>();
                Queue<int[]> q = new ArrayDeque<>();
                seen[x0][y0] = true;
                q.add(new int[]{x0, y0});
                long size = 0;
                boolean touchesBg = false;
                boolean oversize = false;

                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    size++;
                    if (size > floor) {
                        if (!oversize) { oversize = true; piece.clear(); }
                    } else {
                        piece.add(p);
                    }
                    for (int[] d : DIRS) {
                        int nx = p[0] + d[0], ny = p[1] + d[1];
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        if (isBg[nx][ny]) { touchesBg = true; continue; }
                        if (!cand[nx][ny] || seen[nx][ny]) continue;
                        seen[nx][ny] = true;
                        q.add(new int[]{nx, ny});
                    }
                }

                if (oversize) continue;   // a real area of that colour — never mopped
                if (!touchesBg) continue; // floating inside the subject — its own shading, left alone
                for (int[] p : piece) isBg[p[0]][p[1]] = true;
            }
        }
    }
}
