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
     * How far from the mask boundary a pixel can be and still be part of the mixed band, in pixels.
     *
     * <p>Derivation: a single anti-aliased edge is one pixel of genuine partial coverage. Resampling
     * or JPEG compression smears that transition across about two. This is the same 2 px, for the same
     * physical reason, that the hysteresis flood's weak-run cap already uses — both are statements
     * about how wide an edge transition is, so they are deliberately the same number rather than two
     * independent guesses.
     */
    private static final int BAND_RADIUS = 2;

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

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!band[x][y]) continue;
                double[] bg = solidMean(px, isBg, band, w, h, x, y, true);
                double[] fg = solidMean(px, isBg, band, w, h, x, y, false);
                if (bg == null || fg == null) continue; // nothing to interpolate between — keep the mask

                // Indistinguishable endpoints make the projection meaningless: dividing by a
                // near-zero separation turns pixel noise into arbitrary coverage.
                if (CieDe2000.of(BackgroundRemover.rgbToLab(pack(fg)),
                                 BackgroundRemover.rgbToLab(pack(bg))) <= BgRungKey.JND) continue;

                int p = px[y * w + x];
                double pr = LinearBlend.toLinear(p >> 16), pg = LinearBlend.toLinear(p >> 8),
                       pb = LinearBlend.toLinear(p);
                double dr = fg[0] - bg[0], dg = fg[1] - bg[1], db = fg[2] - bg[2];
                double denom = dr * dr + dg * dg + db * db;
                if (denom <= 0) continue;
                double a = ((pr - bg[0]) * dr + (pg - bg[1]) * dg + (pb - bg[2]) * db) / denom;
                a = Math.max(0.0, Math.min(1.0, a));
                if (a < MIN_COVERAGE) { out[y * w + x] = p & 0xFFFFFF; continue; } // background
                int fr = encode((pr - (1 - a) * bg[0]) / a);
                int fgn = encode((pg - (1 - a) * bg[1]) / a);
                int fb = encode((pb - (1 - a) * bg[2]) / a);
                int cov = (int) Math.round(255.0 * a);
                out[y * w + x] = (cov << 24) | (fr << 16) | (fgn << 8) | fb;
            }
        }
        return out;
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

    /**
     * Mean linear-light colour of the nearby pixels that are unmixed ground of the requested side —
     * confirmed background when {@code wantBg}, confirmed subject otherwise. Band pixels are excluded
     * on purpose: they are the mixtures being solved for, so averaging them in would make the
     * endpoints drift toward the answer.
     */
    private static double[] solidMean(int[] px, boolean[][] isBg, boolean[][] band,
                                      int w, int h, int cx, int cy, boolean wantBg) {
        double r = 0, g = 0, b = 0;
        int n = 0;
        int x0 = Math.max(0, cx - SOLID_RADIUS), x1 = Math.min(w - 1, cx + SOLID_RADIUS);
        int y0 = Math.max(0, cy - SOLID_RADIUS), y1 = Math.min(h - 1, cy + SOLID_RADIUS);
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

    /** Linear-light triple back to an opaque sRGB int, for the JND separation test. */
    private static int pack(double[] lin) {
        return 0xFF000000 | (encode(lin[0]) << 16) | (encode(lin[1]) << 8) | encode(lin[2]);
    }

    private static int encode(double lin) {
        double c = lin <= 0.0031308 ? lin * 12.92 : 1.055 * Math.pow(lin, 1.0 / 2.4) - 0.055;
        int v = (int) Math.round(c * 255.0);
        return v < 0 ? 0 : Math.min(255, v);
    }
}
