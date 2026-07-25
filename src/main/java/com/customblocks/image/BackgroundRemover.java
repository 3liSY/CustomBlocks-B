/**
 * BackgroundRemover.java
 *
 * Responsibility: Remove a custom block image's background, painting removed pixels an OPAQUE
 * BLACK fill (old-version parity; the slot block renders solid, so a removed background is a flat
 * fill, not transparency). The fill is always plain BLACK — content is composed on black, so the
 * subject reads on black as-is (no white flip, no keyline; the recolour path keeps its own fill).
 * Recoded clean from the old ImageProcessor.replaceBackground; CIE-LAB ΔE + flood-fill recycled.
 *
 * Three modes:
 *   none   — leave the image untouched.
 *   edges  — background only: flood-fill the edge-connected background from every border pixel.
 *   closed — background + enclosed areas: also remove interior pixels matching the bg colour.
 *
 * Runs on the NATIVE-resolution image, BEFORE any resize/pad (so corner sampling reads the
 * real background, never the transparent padding ImageProcessor adds for non-square images).
 *
 * Called by: command/handlers/CreationCommands.applyTexture (before ImageProcessor.toBlockPng);
 *            core/ColorVariantService (recolorBackground — M2 triangle colour variants).
 */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

public final class BackgroundRemover {

    private BackgroundRemover() {} // static-only

    public static final String NONE   = "none";
    public static final String EDGES  = "edges";   // background only — edge-connected
    public static final String CLOSED = "closed";  // background + enclosed areas
    public static final String SMART  = "smart";   // offline subject isolation (Group 10 — pure Java)

    /**
     * Player-facing strength 0-100 maps linearly onto a CIEDE2000 ΔE [0, MAX_DELTA_E].
     *
     * <p>Derivation (G10 §H Jar A, 2026-07-25, tools/render_preview/DeltaESweep.java): the old scale
     * capped CIE76 at 22, so the default strength 30 split background from subject at CIE76 ≤ 6.6.
     * Sweeping the ΔE00 threshold over every opaque pixel of the seven baseline pictures against
     * that split gives a stability plateau at ΔE00 4.10-4.40 (≤ 0.024% of 21.97M pixels disagreeing,
     * cliff at 4.80); the plateau centre 4.25 ÷ 0.30 = 14.2. The default strength therefore keeps
     * meaning what it meant, and every other strength scales around it on the uniform metric.
     */
    private static final double MAX_DELTA_E = 14.2;
    /** Alpha below this counts as transparent → background. */
    private static final int OPAQUE_THRESHOLD = 128;
    private static final int BLACK = 0xFF000000;
    /** After resize, pixels with every channel ≤ this snap to pure black (kills bicubic gray halos). */
    private static final int SNAP_MAX = 24;
    /** Hysteresis ratio between the weak and strong background thresholds (weak = ratio × tol).
     *  A weak pixel joins the background only by connecting to an already-accepted pixel, so an
     *  edge survives its own momentary dips without the geometric close that ate thin features.
     *  2:1 is Canny's recommended high:low threshold ratio (Canny 1986, §VI suggests 2:1-3:1);
     *  the conservative end is taken because the flood expands regions, not thin edge chains. */
    private static final double HYSTERESIS_RATIO = 2.0;
    /** How many consecutive WEAK pixels the flood may cross before it must touch STRONG ground
     *  again. The weak tier replaces the old radius-1 morphological close, whose whole job was
     *  bridging 1-2 px hairline gaps — so 2 px is the bridging power it inherits, no more. Without
     *  this cap the weak tier makes region-scale decisions: on a JPEG it tunnels through the noise
     *  gaps of a blocky shadow and hollows it into stuttering dashes (owner report, 2026-07-25). */
    private static final int WEAK_MAX_RUN = 2;
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * Remove the background and return cleaned PNG bytes. On mode {@code none} (or any failure)
     * the original bytes are returned unchanged — background removal must never break a retexture.
     * Uses the smart black/white fill (auto-picks based on subject brightness).
     */
    public static byte[] apply(byte[] input, String mode, int tolerance) {
        return process(input, mode, tolerance, null); // null → smart black/white fill
    }

    /**
     * Remove the background and paint the removed area with {@code fillRgb} (0xRRGGBB).
     * Used by BgStudio when the player picks a custom fill colour.
     */
    public static byte[] apply(byte[] input, String mode, int tolerance, int fillRgb) {
        return process(input, mode, tolerance, 0xFF000000 | (fillRgb & 0xFFFFFF));
    }

    /**
     * M2 (colour variants): same background detection as {@link #apply}, but the background is
     * painted {@code fillRgb} (0xRRGGBB) instead of the smart black/white fill. Mode {@code none}
     * is treated as {@code edges} — a recolour without background detection would do nothing.
     */
    public static byte[] recolorBackground(byte[] input, String mode, int tolerance, int fillRgb) {
        String m = normalize(mode);
        if (NONE.equals(m)) m = EDGES;
        return process(input, m, tolerance, 0xFF000000 | (fillRgb & 0xFFFFFF));
    }

    /** Shared pipeline: detect the background, then paint it {@code forcedFill} (or smart fill when null). */
    private static byte[] process(byte[] input, String mode, int tolerance, Integer forcedFill) {
        input = CheckerboardDetector.flattenToBlack(input); // G10-6: flattened-preview checkerboard → black in EVERY mode (no-op otherwise)
        String m = normalize(mode);
        final boolean smart = SMART.equals(m);
        if (NONE.equals(m)) return input; // off
        // Smart mode auto-picks a sensible strength when none is given; the classic modes need one.
        int effTol = tolerance > 0 ? tolerance : (smart ? 35 : 0);
        if (effTol <= 0) return input; // off
        // Map the player-facing 0-100 strength onto a CIE-LAB ΔE distance.
        final double tol = Math.max(0, Math.min(100, effTol)) / 100.0 * MAX_DELTA_E;
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(input));
            if (src == null) return input; // unreadable here → let toBlockPng surface the real error
            int w = src.getWidth(), h = src.getHeight();
            BufferedImage img = toArgb(src);

            int bgArgb = sampleCornerBackground(img, w, h);
            int bgA = (bgArgb >>> 24) & 0xFF;
            double[] bgLab = rgbToLab(bgArgb);
            BgDist dist = new BgDist(bgLab);

            boolean[][] isBg = new boolean[w][h];

            // Stage 1 — hysteresis flood-fill (G10 §H). Seeds are border pixels that STRONGLY match
            // the background (ΔE ≤ tol); the flood then also accepts WEAK pixels (ΔE ≤ 2·tol) that
            // connect to an already-accepted one, but a weak RUN is capped at WEAK_MAX_RUN px — the
            // tier bridges hairline gaps the way the old radius-1 close did, and touching strong
            // ground resets the run. A background edge survives its own near-tolerance dips, a pixel
            // plainly far from the background can never be swallowed, and the flood cannot tunnel
            // region-deep through the sub-threshold noise gaps of a JPEG shadow.
            final double weakTol = tol * HYSTERESIS_RATIO;
            byte[][] weakRun = new byte[w][h]; // 0 for strong/unvisited; weak pixels carry their run length
            Queue<int[]> queue = new ArrayDeque<>();
            for (int x = 0; x < w; x++) {
                seed(img, isBg, queue, x, 0, bgA, dist, tol);
                seed(img, isBg, queue, x, h - 1, bgA, dist, tol);
            }
            for (int y = 1; y < h - 1; y++) {
                seed(img, isBg, queue, 0, y, bgA, dist, tol);
                seed(img, isBg, queue, w - 1, y, bgA, dist, tol);
            }
            while (!queue.isEmpty()) {
                int[] p = queue.poll();
                int run = weakRun[p[0]][p[1]];
                for (int[] d : DIRS) {
                    int nx = p[0] + d[0], ny = p[1] + d[1];
                    if (nx < 0 || nx >= w || ny < 0 || ny >= h || isBg[nx][ny]) continue;
                    int px = img.getRGB(nx, ny);
                    if (isBackground(px, bgA, dist, tol)) {          // strong — always joins, resets the run
                        isBg[nx][ny] = true;
                        queue.add(new int[]{nx, ny});
                    } else if (run < WEAK_MAX_RUN && isBackground(px, bgA, dist, weakTol)) {
                        isBg[nx][ny] = true;
                        weakRun[nx][ny] = (byte) (run + 1);
                        queue.add(new int[]{nx, ny});
                    }
                }
            }

            // Stage 1b (CLOSED + SMART) — absorb background-coloured POCKETS the edge flood cannot reach:
            // the hole inside a letter "O", the gap between two glyphs. This used to be a plain per-pixel
            // colour key over the whole image, which is why "BgRemove&More" ate into artwork — every dark
            // shading pixel inside a subject matches a dark background colour, so a chrome logo's shadow
            // lines were deleted and the repainted background showed straight through the subject. A
            // pocket is a REGION, not a colour, so the pixels are grouped into connected components and a
            // component is only absorbed when it is thick enough to be a real enclosed area rather than a
            // hairline of subject shading (BgMask.absorbEnclosedPockets).
            if (CLOSED.equals(m) || smart) {
                boolean[][] pocket = new boolean[w][h];
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        if (!isBg[x][y] && isBackground(img.getRGB(x, y), bgA, dist, tol)) {
                            pocket[x][y] = true;
                        }
                    }
                }
                BgMask.absorbEnclosedPockets(isBg, pocket, w, h);
            }

            // Stage 1c — area opening (G10 §H): drop tiny isolated foreground islands into the
            // background, judged by component SIZE, never by width. A long hair-thin outline is a
            // large component and survives; an isolated speck of checker residue does not. The old
            // guarded morphological close is gone — hysteresis above already bridges the
            // near-tolerance gaps the close existed for, without geometry that could swallow a
            // feature 1-2 px wide.
            BgMask.despeckle(isBg, w, h);

            // Stage 1d (SMART only) — keep just the single largest connected subject and drop every
            // other free-floating foreground blob into the background. This is the offline "smart"
            // win: a busy/cluttered background that the colour flood can't fully reach is removed
            // because only the main subject survives. Pure heuristic — never neural — but it isolates
            // a clear central subject far better than corners/flood alone.
            if (smart) BgMask.keepLargestForeground(isBg, w, h);

            // Stage 2 — anti-fringe peel (BgFringe): shave the descending-gradient halo a SOURCE
            // image carries against its own flat background, without eating a crisp subject edge.
            // Skipped when the bg was transparent (no colour halo to shave).
            if (bgA >= OPAQUE_THRESHOLD) {
                BgFringe.peel(img, isBg, dist, w, h);
            }

            // Background is always plain BLACK (owner: content is composed on black backgrounds, so the
            // subject reads on black as-is). The old smart dark-subject specials — the whole-bg WHITE FLIP
            // (Tux rectangle) and the thin WHITE KEYLINE (blobbed thin strokes / hid solid-dark subjects)
            // — are removed: no flip, no outline. The recolour path (forcedFill) keeps its own fill.
            int fill = forcedFill != null ? forcedFill : BLACK;

            // Stage 3 — paint the background the base fill; flatten any leftover transparency to opaque,
            // composited against that fill IN LINEAR LIGHT (G10 §H) so anti-aliased edges resolve toward
            // the background without the dark rim a gamma-space blend draws against a dark fill.
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (isBg[x][y]) { img.setRGB(x, y, fill); continue; }
                    int px = img.getRGB(x, y);
                    int a = (px >>> 24) & 0xFF;
                    if (a == 255) continue;             // already opaque
                    img.setRGB(x, y, LinearBlend.over(px, fill)); // transparent or mixed → fill/composite
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            return input; // never break a retexture because of background removal
        }
    }

    /** Canonicalize any stored/typed value to NONE / EDGES / CLOSED (unknown → NONE). */
    public static String normalize(String raw) {
        String m = fromArg(raw);
        return m == null ? NONE : m;
    }

    /**
     * Parse a value to its canonical mode, accepting both the internal ids (none/edges/closed)
     * and the player-facing command arguments (NoBgRemove / BgRemove / BgRemove&More).
     * Returns null for anything unrecognized (so the command can show a usage error).
     */
    public static String fromArg(String raw) {
        if (raw == null) return null;
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "none",   "nobgremove"           -> NONE;
            case "edges",  "bgremove"             -> EDGES;
            case "closed", "bgremove&more"        -> CLOSED;
            case "smart",  "ai", "bgsmart"        -> SMART;
            default -> null;
        };
    }

    /** Player-facing display name shown in the config menu and chat. */
    public static String displayName(String mode) {
        return switch (normalize(mode)) {
            case EDGES  -> "Background Removal Only";
            case CLOSED -> "Background + Closed Areas Removal";
            case SMART  -> "Smart Removal (offline)";
            default     -> "No Background Removal";
        };
    }

    /** Player-facing command argument for the given mode (for /cb config background). */
    public static String commandArg(String mode) {
        return switch (normalize(mode)) {
            case EDGES  -> "BgRemove";
            case CLOSED -> "BgRemove&More";
            case SMART  -> "BgSmart";
            default     -> "NoBgRemove";
        };
    }

    /** Next mode in the cycle none → edges → closed → smart → none (used by the config menu). */
    public static String next(String mode) {
        return switch (normalize(mode)) {
            case NONE   -> EDGES;
            case EDGES  -> CLOSED;
            case CLOSED -> SMART;
            default     -> NONE;
        };
    }

    /**
     * After the texture has been resized, snap near-fill pixels to the exact fill colour.
     * The classic case is snapping near-black halos to pure black after bicubic downscaling.
     * No-op when mode is none. When {@code fillRgb} is -1, defaults to smart detection (reads
     * the corners — if they aren't near-black, bails out so a dark subject isn't destroyed).
     */
    public static byte[] snapBackgroundBlack(byte[] png, String mode, int tolerance) {
        return snapBackgroundColor(png, mode, tolerance, -1);
    }

    /**
     * Snap near-fill-colour pixels after resize. {@code fillRgb} = -1 for smart (detect from corners).
     *
     * <p>This is the LAST step of every base (non-recolour) bake, so it also owns the guarantee that a
     * baked block texture is fully OPAQUE. Slot blocks draw on the alpha-tested cutout layer: a texel
     * left semi-transparent by the Lanczos resample flickers in and out of the alpha test per mip level,
     * which reads in-world as a glitched coloured hairline tracing the subject — the artifact colour
     * variants never showed, because their rail always ends in {@code ImageProcessor.fillBackground}.
     * Leftover alpha (source transparency, and the transparent padding {@code toBlockPng} adds around a
     * non-square picture) is therefore composited onto the fill here even when background removal is off,
     * which is the only path that could previously hand the atlas a texture with alpha.
     */
    public static byte[] snapBackgroundColor(byte[] png, String mode, int tolerance, int fillRgb) {
        try {
            BufferedImage read = ImageIO.read(new ByteArrayInputStream(png));
            if (read == null) return png;
            BufferedImage img = toArgb(read);
            int w = img.getWidth(), h = img.getHeight();
            // Snapping needs an active mode + strength; flattening alpha to opaque always runs.
            boolean snap = !NONE.equals(normalize(mode)) && tolerance > 0;
            int targetR, targetG, targetB;
            if (fillRgb >= 0) {
                targetR = (fillRgb >> 16) & 0xFF;
                targetG = (fillRgb >> 8) & 0xFF;
                targetB = fillRgb & 0xFF;
            } else {
                // Smart: read the fill from the corners and skip the snap if it isn't near-black (a dark
                // subject must not be destroyed). The flatten below still uses black — the project's
                // default background — because that is what an opaque bake composites onto.
                int bg = sampleCornerBackground(img, w, h);
                if (((bg >> 16) & 0xFF) > SNAP_MAX || ((bg >> 8) & 0xFF) > SNAP_MAX || (bg & 0xFF) > SNAP_MAX) {
                    snap = false; // non-black fill → nothing to snap
                }
                targetR = 0; targetG = 0; targetB = 0;
            }
            int target = 0xFF000000 | (targetR << 16) | (targetG << 8) | targetB;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int px = img.getRGB(x, y);
                    int a = (px >>> 24) & 0xFF;
                    if (a != 255) { // flatten: composite onto the fill so the cutout layer never sees alpha
                        if (a == 0) { img.setRGB(x, y, target); continue; }
                        px = LinearBlend.over(px, target); // linear light — no dark rim against a dark fill
                        img.setRGB(x, y, px);
                    }
                    if (!snap) continue;
                    int r = (px >> 16) & 0xFF, g = (px >> 8) & 0xFF, b = px & 0xFF;
                    if (Math.abs(r - targetR) <= SNAP_MAX && Math.abs(g - targetG) <= SNAP_MAX && Math.abs(b - targetB) <= SNAP_MAX) {
                        img.setRGB(x, y, target);
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            return png;
        }
    }


    private static void seed(BufferedImage img, boolean[][] isBg, Queue<int[]> q,
                             int x, int y, int bgA, BgDist dist, double tol) {
        if (!isBg[x][y] && isBackground(img.getRGB(x, y), bgA, dist, tol)) {
            isBg[x][y] = true;
            q.add(new int[]{x, y});
        }
    }

    /** Background if (near-)transparent, or within ΔE {@code tol} of the sampled bg colour. */
    private static boolean isBackground(int argb, int bgA, BgDist dist, double tol) {
        int a = (argb >>> 24) & 0xFF;
        if (a < OPAQUE_THRESHOLD) return true;   // transparent pixels are background
        if (bgA < OPAQUE_THRESHOLD) return false; // bg sampled transparent: only transparency counts
        return dist.of(argb) <= tol;
    }

    /** Median of 3×3 samples from each of the four corners (robust to a stray edge pixel). */
    private static int sampleCornerBackground(BufferedImage img, int w, int h) {
        List<Integer> samples = new ArrayList<>();
        int[][] corners = {{0, 0}, {Math.max(0, w - 3), 0}, {0, Math.max(0, h - 3)}, {Math.max(0, w - 3), Math.max(0, h - 3)}};
        for (int[] c : corners) {
            for (int dx = 0; dx < 3 && c[0] + dx < w; dx++) {
                for (int dy = 0; dy < 3 && c[1] + dy < h; dy++) {
                    samples.add(img.getRGB(c[0] + dx, c[1] + dy));
                }
            }
        }
        Collections.sort(samples);
        return samples.get(samples.size() / 2);
    }

    private static BufferedImage toArgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }


    // ── CIE-LAB (sRGB → XYZ → L*a*b*, D65); distance is CIEDE2000 via CieDe2000 ───
    static double[] rgbToLab(int argb) { // package-private: BgDist reads it
        double rF = ((argb >> 16) & 0xFF) / 255.0;
        double gF = ((argb >> 8)  & 0xFF) / 255.0;
        double bF = ( argb        & 0xFF) / 255.0;

        rF = (rF > 0.04045) ? Math.pow((rF + 0.055) / 1.055, 2.4) : (rF / 12.92);
        gF = (gF > 0.04045) ? Math.pow((gF + 0.055) / 1.055, 2.4) : (gF / 12.92);
        bF = (bF > 0.04045) ? Math.pow((bF + 0.055) / 1.055, 2.4) : (bF / 12.92);

        rF *= 100.0; gF *= 100.0; bF *= 100.0;

        double x = rF * 0.4124 + gF * 0.3576 + bF * 0.1805;
        double y = rF * 0.2126 + gF * 0.7152 + bF * 0.0722;
        double z = rF * 0.0193 + gF * 0.1192 + bF * 0.9505;

        x /= 95.047; y /= 100.000; z /= 108.883; // D65 reference white

        x = (x > 0.008856) ? Math.cbrt(x) : (7.787 * x) + (16.0 / 116.0);
        y = (y > 0.008856) ? Math.cbrt(y) : (7.787 * y) + (16.0 / 116.0);
        z = (z > 0.008856) ? Math.cbrt(z) : (7.787 * z) + (16.0 / 116.0);

        return new double[]{(116.0 * y) - 16.0, 500.0 * (x - y), 200.0 * (y - z)};
    }

    /** CIEDE2000 (G10 §H) — uniform across the palette, unlike the old Euclidean CIE76. */
    private static double deltaE(double[] a, double[] b) {
        return CieDe2000.of(a, b);
    }
}
