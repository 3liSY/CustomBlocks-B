/**
 * BackgroundRemover.java
 *
 * Responsibility: Remove a custom block image's background, painting removed pixels an OPAQUE
 * fill — normally BLACK (old-version parity; the slot block renders solid/opaque, so a removed
 * background is a flat fill, not transparency). When the subject itself is near-black, the fill
 * flips to WHITE instead so a dark subject can't disappear into a black background (the
 * "black-on-black" failure). Recoded clean from the old project's ImageProcessor.replaceBackground;
 * the CIE-LAB ΔE math + flood-fill are recycled, the buggy old config is not.
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

    /**
     * Player-facing strength 0-100 maps linearly onto CIE-LAB ΔE [0, MAX_DELTA_E]. Kept at 22
     * (was 40): at 40, strength 55 = ΔE 22 ate a near-white subject only ~ΔE 13 from its gray
     * background (Test 4 — the "6" vanished). 22 caps the strongest setting at a separation
     * genuine subjects clear; truly low-contrast images still can't be split by colour alone.
     */
    private static final double MAX_DELTA_E = 22.0;
    /** Extra ΔE for the 1-pixel anti-fringe dilation that catches anti-aliased edge halo. */
    private static final double FRINGE_EXTRA = 6.0;
    /** Alpha below this counts as transparent → background. */
    private static final int OPAQUE_THRESHOLD = 128;
    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;
    /** If the subject's mean HSV "value" (max RGB channel) is below this, fill WHITE not BLACK so a
     *  near-black subject isn't swallowed by a black background. Saturated-but-bright subjects
     *  (e.g. pure red, value 255) stay on black; only genuinely dark subjects flip. */
    private static final int FILL_DARK_VALUE = 64;
    /** After resize, pixels with every channel ≤ this snap to pure black (kills bicubic gray halos). */
    private static final int SNAP_MAX = 24;
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * Remove the background and return cleaned PNG bytes. On mode {@code none} (or any failure)
     * the original bytes are returned unchanged — background removal must never break a retexture.
     */
    public static byte[] apply(byte[] input, String mode, int tolerance) {
        return process(input, mode, tolerance, null); // null → smart black/white fill
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
        String m = normalize(mode);
        if (NONE.equals(m) || tolerance <= 0) return input; // off
        // Map the player-facing 0-100 strength onto a CIE-LAB ΔE distance.
        final double tol = Math.max(0, Math.min(100, tolerance)) / 100.0 * MAX_DELTA_E;
        final double fringeTol = tol + FRINGE_EXTRA;
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(input));
            if (src == null) return input; // unreadable here → let toBlockPng surface the real error
            int w = src.getWidth(), h = src.getHeight();
            BufferedImage img = toArgb(src);

            int bgArgb = sampleCornerBackground(img, w, h);
            int bgA = (bgArgb >>> 24) & 0xFF;
            double[] bgLab = rgbToLab(bgArgb);

            boolean[][] isBg = new boolean[w][h];

            // Stage 1 — BFS flood-fill from every border pixel that matches the background.
            Queue<int[]> queue = new ArrayDeque<>();
            for (int x = 0; x < w; x++) {
                seed(img, isBg, queue, x, 0, bgA, bgLab, tol);
                seed(img, isBg, queue, x, h - 1, bgA, bgLab, tol);
            }
            for (int y = 1; y < h - 1; y++) {
                seed(img, isBg, queue, 0, y, bgA, bgLab, tol);
                seed(img, isBg, queue, w - 1, y, bgA, bgLab, tol);
            }
            while (!queue.isEmpty()) {
                int[] p = queue.poll();
                for (int[] d : DIRS) {
                    int nx = p[0] + d[0], ny = p[1] + d[1];
                    if (nx >= 0 && nx < w && ny >= 0 && ny < h && !isBg[nx][ny]
                            && isBackground(img.getRGB(nx, ny), bgA, bgLab, tol)) {
                        isBg[nx][ny] = true;
                        queue.add(new int[]{nx, ny});
                    }
                }
            }

            // Stage 1b (CLOSED only) — remove enclosed areas matching the bg colour, even if
            // they aren't edge-connected (a logo's trapped inner background, for example).
            if (CLOSED.equals(m)) {
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        if (!isBg[x][y] && isBackground(img.getRGB(x, y), bgA, bgLab, tol)) {
                            isBg[x][y] = true;
                        }
                    }
                }
            }

            // Stage 2 — 1-pixel anti-fringe dilation: mark non-bg pixels adjacent to bg that are
            // within the looser tolerance, removing the bright halo anti-aliasing leaves behind.
            boolean[][] fringe = new boolean[w][h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (isBg[x][y]) continue;
                    boolean adjacent = false;
                    for (int[] d : DIRS) {
                        int nx = x + d[0], ny = y + d[1];
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h && isBg[nx][ny]) { adjacent = true; break; }
                    }
                    if (adjacent && isBackground(img.getRGB(x, y), bgA, bgLab, fringeTol)) {
                        fringe[x][y] = true;
                    }
                }
            }
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (fringe[x][y]) isBg[x][y] = true;
                }
            }

            // Smart fill: choose the background colour from the subject's brightness. Default is
            // BLACK (old-version parity); but if the subject itself is near-black a black fill
            // would swallow it (black-on-black). Brightness = mean HSV "value" (max RGB channel)
            // over the foreground, so a saturated-but-bright subject (pure red = value 255) keeps
            // a black fill and only a genuinely dark subject flips to WHITE.
            long valueSum = 0, fgCount = 0;
            if (forcedFill == null) { // brightness scan only feeds the smart fill
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        if (isBg[x][y]) continue;
                        int px = img.getRGB(x, y);
                        int r = (px >> 16) & 0xFF, g = (px >> 8) & 0xFF, b = px & 0xFF;
                        valueSum += Math.max(r, Math.max(g, b));
                        fgCount++;
                    }
                }
            }
            int fill = forcedFill != null ? forcedFill
                    : (fgCount > 0 && valueSum / fgCount < FILL_DARK_VALUE) ? WHITE : BLACK;
            int fillR = (fill >> 16) & 0xFF, fillG = (fill >> 8) & 0xFF, fillB = fill & 0xFF;

            // Stage 3 — paint the background with the chosen fill; flatten any leftover transparency
            // to opaque, composited against that same fill so anti-aliased edges resolve toward the
            // background rather than washing out to gray.
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (isBg[x][y]) { img.setRGB(x, y, fill); continue; }
                    int px = img.getRGB(x, y);
                    int a = (px >>> 24) & 0xFF;
                    if (a == 255) continue;             // already opaque
                    if (a == 0) { img.setRGB(x, y, fill); continue; } // transparent, not bg → fill
                    // out = src * (a/255) + fill * (1 - a/255)
                    double fa = a / 255.0, fb = 1.0 - fa;
                    int r = clamp255((int) Math.round(((px >> 16) & 0xFF) * fa + fillR * fb));
                    int g = clamp255((int) Math.round(((px >> 8)  & 0xFF) * fa + fillG * fb));
                    int b = clamp255((int) Math.round(( px        & 0xFF) * fa + fillB * fb));
                    img.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
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
            case "none",   "nobgremove"     -> NONE;
            case "edges",  "bgremove"       -> EDGES;
            case "closed", "bgremove&more"  -> CLOSED;
            default -> null;
        };
    }

    /** Player-facing display name shown in the config menu and chat. */
    public static String displayName(String mode) {
        return switch (normalize(mode)) {
            case EDGES  -> "Background Removal Only";
            case CLOSED -> "Background + Closed Areas Removal";
            default     -> "No Background Removal";
        };
    }

    /** Player-facing command argument for the given mode (for /cb config background). */
    public static String commandArg(String mode) {
        return switch (normalize(mode)) {
            case EDGES  -> "BgRemove";
            case CLOSED -> "BgRemove&More";
            default     -> "NoBgRemove";
        };
    }

    /** Next mode in the cycle none → edges → closed → none (used by the config menu). */
    public static String next(String mode) {
        return switch (normalize(mode)) {
            case NONE  -> EDGES;
            case EDGES -> CLOSED;
            default    -> NONE;
        };
    }

    /**
     * After the texture has been resized, snap near-black pixels to pure #000000. Bicubic
     * downscaling blends the black background with neighbouring bright pixels, leaving dark-gray
     * halos; this restores a true black there. No-op when mode is none, or when the smart fill
     * chose WHITE (a dark subject) — snapping there would turn the subject into black specks.
     * Conservative: only pixels already very close to black are touched, so a bright subject is
     * untouched.
     */
    public static byte[] snapBackgroundBlack(byte[] png, String mode, int tolerance) {
        if (NONE.equals(normalize(mode)) || tolerance <= 0) return png;
        try {
            BufferedImage read = ImageIO.read(new ByteArrayInputStream(png));
            if (read == null) return png;
            BufferedImage img = toArgb(read);
            int w = img.getWidth(), h = img.getHeight();
            // Smart fill may have used a WHITE background (dark subject). The near-black snap is
            // only correct on a BLACK background; on a white-fill block it would blacken the dark
            // subject. Read the fill from the corners and bail if it isn't black.
            int bg = sampleCornerBackground(img, w, h);
            if (((bg >> 16) & 0xFF) > SNAP_MAX || ((bg >> 8) & 0xFF) > SNAP_MAX || (bg & 0xFF) > SNAP_MAX) {
                return png; // non-black fill → nothing to snap
            }
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int px = img.getRGB(x, y);
                    int r = (px >> 16) & 0xFF, g = (px >> 8) & 0xFF, b = px & 0xFF;
                    if (r <= SNAP_MAX && g <= SNAP_MAX && b <= SNAP_MAX) {
                        img.setRGB(x, y, BLACK);
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
                             int x, int y, int bgA, double[] bgLab, double tol) {
        if (!isBg[x][y] && isBackground(img.getRGB(x, y), bgA, bgLab, tol)) {
            isBg[x][y] = true;
            q.add(new int[]{x, y});
        }
    }

    /** Background if (near-)transparent, or within ΔE {@code tol} of the sampled bg colour. */
    private static boolean isBackground(int argb, int bgA, double[] bgLab, double tol) {
        int a = (argb >>> 24) & 0xFF;
        if (a < OPAQUE_THRESHOLD) return true;   // transparent pixels are background
        if (bgA < OPAQUE_THRESHOLD) return false; // bg sampled transparent: only transparency counts
        return deltaE(rgbToLab(argb), bgLab) <= tol;
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

    private static int clamp255(int v) { return v < 0 ? 0 : Math.min(255, v); }

    // ── CIE-LAB (sRGB → XYZ → L*a*b*, D65) + Euclidean ΔE — recycled verbatim ───
    private static double[] rgbToLab(int argb) {
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

    private static double deltaE(double[] a, double[] b) {
        return Math.sqrt(Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2) + Math.pow(a[2] - b[2], 2));
    }
}
