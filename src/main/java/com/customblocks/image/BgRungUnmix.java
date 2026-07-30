/**
 * BgRungUnmix.java — cascade rung 5: colour unmixing along the edge band (G10 §H).
 *
 * <p>Rung 5 never decides which region is the background. It takes whatever mask rungs 1-4 produced
 * and answers a different question about the thin band along its boundary: not "background or
 * subject?" but "HOW MUCH of each?".
 *
 * <p>That band is where the honest answer is "both". A pixel on an anti-aliased edge is physically a
 * mixture of the background colour B and the subject colour F:
 *
 * <pre>   P = a*F + (1 - a)*B</pre>
 *
 * <p>with {@code a} the fraction of the pixel the subject actually covers. Knowing B and F, that
 * equation solves for {@code a} directly — projecting P onto the line from B to F. A binary mask has
 * to round {@code a} to 0 or 1 and is therefore wrong on every edge pixel by up to half a pixel of
 * coverage, which is what makes a hard mask edge look stepped or nibbled. Solving for the fraction
 * instead keeps the edge the source actually has.
 *
 * <p>The mixing happened in LINEAR LIGHT — light adds linearly, sRGB bytes do not — so the solve is
 * done there. Doing it on encoded bytes would bias every edge toward the darker of the two colours,
 * which is the same error the locked linear-light decision removed from the compositing path.
 *
 * <p>B and F are measured LOCALLY, from the confirmed background and confirmed subject either side of
 * the band, rather than taken from one global key. That costs nothing extra and makes the rung correct
 * on a shaded background, where a single key colour would be wrong everywhere except one spot.
 *
 * <p>It declines per pixel, never wholesale: where the neighbourhood does not offer both a background
 * and a subject to interpolate between, or where the two are so close that the projection is
 * meaningless, the pixel keeps the binary decision the deciding rung made.
 *
 * Depends on: LinearBlend.toLinear, BgRungKey.JND, BackgroundRemover.rgbToLab, CieDe2000.
 * Called by:  image/BgCascade (always, after whichever rung decided).
 */
package com.customblocks.image;


final class BgRungUnmix {

    private BgRungUnmix() {} // static-only

    /**
     * How far from the mask boundary a pixel STARTS as part of the mixed band, in pixels.
     *
     * <p>Derivation: a single anti-aliased edge is one pixel of genuine partial coverage. Resampling
     * or JPEG compression smears that transition across about two. This is the same 2 px, for the same
     * physical reason, that the hysteresis flood's weak-run cap already uses — both are statements
     * about how wide an edge transition is, so they are deliberately the same number rather than two
     * independent guesses.
     *
     * <p>It is a starting width, not the whole story: see {@link #grow}. A source that was sharpened,
     * re-compressed, or cut out of another background carries a transition several pixels wide, and a
     * fixed radius leaves whatever sits past it baked in as a rim.
     */
    private static final int BAND_RADIUS = 2;

    /**
     * Hard ceiling on how far {@link #grow} may push the band past {@link #BAND_RADIUS}. Not the thing
     * that stops it — the mixture test is — only a stop against a pathological picture walking the band
     * across the whole subject. Scaled with the picture because an edge transition is a physical width:
     * a 735 px photograph gets 4, a small button 2, and nothing gets more than 8.
     */
    private static final int GROW_CAP = 8;
    private static final int GROW_DIVISOR = 150;

    /**
     * Largest coverage the band may GROW onto. Growth exists to catch background the strict mask left
     * behind, so it takes only pixels that are more background than subject; a pixel past halfway is
     * the subject's own edge, and the starting band already covers those. Being a majority statement
     * rather than a tuned bar is what makes the growth unable to erode a subject: every pixel it takes
     * is, by the picture's own colours, mostly backdrop.
     */
    private static final double GROWTH_MAX_COVERAGE = 0.5;

    /**
     * Radius searched for the solid background and solid subject to interpolate between. Twice the
     * band radius, so the window is guaranteed to reach past the band's own width to unmixed ground on
     * both sides; anything smaller could only see band pixels and would interpolate a mixture against
     * another mixture.
     */
    private static final int SOLID_RADIUS = 2 * BAND_RADIUS;

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * Smallest coverage worth keeping. Below 1/255 the subject's contribution cannot be represented in
     * an 8-bit channel at all, so the pixel is simply background — and recovering a subject colour
     * from a coverage that small would divide by near-zero and amplify noise into an arbitrary colour.
     */
    private static final double MIN_COVERAGE = 1.0 / 255.0;

    /**
     * Coverage at or below which a SOLVED band pixel is called background outright rather than kept at
     * partial strength. A majority statement, the same one {@link #GROWTH_MAX_COVERAGE} makes: a pixel
     * the solve says is more backdrop than subject is backdrop.
     *
     * <p>It only ever reaches pixels the solve already explained and already measured as mostly
     * backdrop, which is what separates it from the deleted tolerance knob. That knob compared EVERY
     * pixel in the picture against a background colour, so a white ball on a white grid matched it and
     * was erased whole — measured 2026-07-29 against the pre-cascade build, which keeps 70 % of the
     * football and 90 % of the share button. Nothing here can touch a pixel that is not on the mask
     * boundary, that the two endpoints do not explain, or that the solve calls mostly subject.
     */
    private static final double SNAP_COVERAGE = 0.50;

    /**
     * The picture re-expressed as {@code ARGB} where the ALPHA channel is subject coverage (0 entirely
     * background, 255 entirely subject) and the RGB is the subject's OWN colour with the background
     * mixed back out of it.
     *
     * <p>Returning the recovered colour matters as much as the coverage. A band pixel's stored colour
     * is a mixture that already contains the old background; compositing that mixture onto a new fill
     * would lay the old background over the new one and leave a halo of the original colour tracing
     * every edge — the classic matting error. Solving {@code F = (P - (1-a)B) / a} first, then
     * compositing F at coverage {@code a}, replaces the old background instead of layering over it.
     *
     * <p>The result is shaped so {@link LinearBlend#over} consumes it directly: coverage is already in
     * the alpha channel, where that method expects its mix weight.
     */
    static int[] refine(int[] px, boolean[][] isBg, int w, int h) {
        int[] out = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = px[y * w + x] & 0xFFFFFF;
                out[y * w + x] = isBg[x][y] ? rgb : (0xFF000000 | rgb);
            }
        }

        boolean[][] band = band(isBg, w, h);
        // The backdrop estimate the pipeline already builds, as a fallback for the one case a local
        // window cannot cover: an edge where the mask stopped short and there is no confirmed
        // background pixel left within reach to sample. Measured on Jupiter, 2026-07-29 — the planet
        // runs off the top of the frame, so along that stretch of limb the anti-aliased ramp is the
        // ONLY background-side colour present and the search returned nothing, which left the ramp
        // baked opaque as a white line 1-3 px deep. The plate answers the same question by push-pull
        // over the whole picture, so it still has a backdrop colour where the window has no sample.
        final double[][] plate = BgPlate.build(px, isBg, w, h);
        final int reach = SOLID_RADIUS + grow(px, isBg, band, plate, w, h);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!band[x][y]) continue;
                double[] bg = background(px, isBg, band, plate, w, h, x, y, reach);
                double[] fg = solidMean(px, isBg, band, w, h, x, y, false, reach);
                if (bg == null || fg == null) continue; // nothing to interpolate between — keep the mask

                // Indistinguishable endpoints make the projection meaningless: dividing by a
                // near-zero separation turns pixel noise into arbitrary coverage.
                if (!EdgeMix.separated(bg, fg)) continue;

                int p = px[y * w + x];
                double a = EdgeMix.coverage(p, bg, fg);
                if (a < 0) continue;                           // degenerate endpoints — keep the mask
                if (!EdgeMix.explains(p, bg, fg, a)) continue; // not a mixture of these two — keep the mask
                // Mostly backdrop is backdrop: leaving it at partial strength is what draws the soft
                // pale fringe the owner reported round the ball, the bird and the planet.
                if (a <= SNAP_COVERAGE) { out[y * w + x] = p & 0xFFFFFF; continue; } // background
                int cov = (int) Math.round(255.0 * a);
                out[y * w + x] = (cov << 24) | (EdgeMix.recover(p, bg, a) & 0xFFFFFF);
            }
        }
        return out;
    }

    /**
     * Push the band outward into the subject side while the pixels there are still mostly backdrop,
     * and return how many rings were added (G10 §H, 2026-07-28).
     *
     * <p>{@link #BAND_RADIUS} states how wide ONE anti-aliased edge is. Real sources routinely carry
     * wider transitions and the picture says so: a photograph cut out of another backdrop keeps a rim of
     * that backdrop, an over-sharpened JPEG carries an overshoot ring just inside its own outline, and a
     * re-compressed grid smears its tones across several pixels. Measured 2026-07-28, Jupiter's limb
     * holds four such pixels and the fixed radius reached two of them, so the other two baked as the
     * white outline the owner reported; the chrome JPEG shows the same shape in the dark.
     *
     * <p>Growth is by ADJACENCY, one ring at a time, so it cannot jump a subject outline into a
     * background-coloured area behind it — the penguin's white belly sits behind a black outline whose
     * pixels are not mixtures, and the ring stops there. Three conditions must hold for a pixel to join,
     * and each one alone would be unsafe:
     *
     * <ul>
     *   <li>it is MOSTLY BACKDROP — {@link #GROWTH_MAX_COVERAGE}, a majority, not a tolerance;
     *   <li>the mixture EXPLAINS it — {@link EdgeMix#explains}, which is what refuses a dark keyline
     *       that merely projects onto the line between a dark backdrop and a bright subject;
     *   <li>both sides are separated by more than a JND, the same guard the solve itself uses.
     * </ul>
     */
    /**
     * The background colour to solve against: the local confirmed background where there is one, and
     * the picture's own backdrop estimate where the window has no sample to offer.
     */
    private static double[] background(int[] px, boolean[][] isBg, boolean[][] band, double[][] plate,
                                       int w, int h, int x, int y, int reach) {
        double[] bg = solidMean(px, isBg, band, w, h, x, y, true, reach);
        if (bg != null) return bg;
        return plate == null ? null : plate[y * w + x];
    }

    private static int grow(int[] px, boolean[][] isBg, boolean[][] band, double[][] plate,
                            int w, int h) {
        final int cap = Math.max(2, Math.min(GROW_CAP, Math.min(w, h) / GROW_DIVISOR));
        java.util.List<int[]> frontier = new java.util.ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) if (band[x][y] && !isBg[x][y]) frontier.add(new int[]{x, y});
        }

        int added = 0;
        // One scratch grid for the whole walk, cleared through the list of cells it actually touched.
        // A fresh boolean[w][h] per ring is 20 MB per ring on the periodic table, allocated eight times.
        final boolean[][] queued = new boolean[w][h];
        for (int ring = 1; ring <= cap && !frontier.isEmpty(); ring++) {
            final int reach = SOLID_RADIUS + ring;
            java.util.List<int[]> take = new java.util.ArrayList<>();
            java.util.List<Long> touched = new java.util.ArrayList<>();
            for (int[] f : frontier) {
                for (int[] d : DIRS) {
                    int x = f[0] + d[0], y = f[1] + d[1];
                    if (x < 0 || y < 0 || x >= w || y >= h) continue;
                    if (isBg[x][y] || band[x][y] || queued[x][y]) continue;
                    queued[x][y] = true;
                    touched.add(queuedAt(x, y));
                    double[] bg = background(px, isBg, band, plate, w, h, x, y, reach);
                    double[] fg = solidMean(px, isBg, band, w, h, x, y, false, reach);
                    if (bg == null || fg == null) continue;
                    if (!EdgeMix.separated(bg, fg)) continue;
                    int p = px[y * w + x];
                    double a = EdgeMix.coverage(p, bg, fg);
                    if (a < 0 || a > GROWTH_MAX_COVERAGE) continue;
                    if (!EdgeMix.explains(p, bg, fg, a)) continue;
                    take.add(new int[]{x, y});
                }
            }
            for (long t : touched) queued[(int) (t >> 32)][(int) (t & 0xFFFFFFFFL)] = false;
            if (take.isEmpty()) break;
            for (int[] p : take) band[p[0]][p[1]] = true;
            frontier = take;
            added = ring;
        }
        return added;
    }

    /** Pixels within {@link #BAND_RADIUS} of the mask boundary, on either side of it. */
    private static boolean[][] band(boolean[][] isBg, int w, int h) {
        boolean[][] band = new boolean[w][h];
        int[][] depth = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) depth[x][y] = Integer.MAX_VALUE;
        }
        java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                for (int[] d : DIRS) {
                    int nx = x + d[0], ny = y + d[1];
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    if (isBg[nx][ny] != isBg[x][y]) {
                        band[x][y] = true;
                        depth[x][y] = 0;
                        q.add(new int[]{x, y});
                        break;
                    }
                }
            }
        }
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int d0 = depth[p[0]][p[1]];
            if (d0 >= BAND_RADIUS) continue;
            for (int[] d : DIRS) {
                int nx = p[0] + d[0], ny = p[1] + d[1];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                if (depth[nx][ny] <= d0 + 1) continue;
                depth[nx][ny] = d0 + 1;
                band[nx][ny] = true;
                q.add(new int[]{nx, ny});
            }
        }
        return band;
    }

    private static long queuedAt(int x, int y) { return ((long) x << 32) | (y & 0xFFFFFFFFL); }

    /**
     * Mean linear-light colour of the nearby pixels that are unmixed ground of the requested side —
     * confirmed background when {@code wantBg}, confirmed subject otherwise. Band pixels are excluded
     * on purpose: they are the mixtures being solved for, so averaging them in would make the
     * endpoints drift toward the answer.
     */
    private static double[] solidMean(int[] px, boolean[][] isBg, boolean[][] band,
                                      int w, int h, int cx, int cy, boolean wantBg, int radius) {
        double r = 0, g = 0, b = 0;
        int n = 0;
        int x0 = Math.max(0, cx - radius), x1 = Math.min(w - 1, cx + radius);
        int y0 = Math.max(0, cy - radius), y1 = Math.min(h - 1, cy + radius);
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (band[x][y] || isBg[x][y] != wantBg) continue;
                int p = px[y * w + x];
                r += LinearBlend.toLinear(p >> 16);
                g += LinearBlend.toLinear(p >> 8);
                b += LinearBlend.toLinear(p);
                n++;
            }
        }
        return n == 0 ? null : new double[]{r / n, g / n, b / n};
    }

}
