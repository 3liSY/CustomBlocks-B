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
    /** Alpha below this counts as transparent → background. Package-private: the cascade's rung 1
     *  reads authored alpha against the same cutoff, so there is one definition of "transparent". */
    static final int OPAQUE_THRESHOLD = 128;
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

    /**
     * Shared pipeline: decide the background with the cascade, then paint it {@code forcedFill}
     * (or plain black when null).
     *
     * <p>G10 §H: the removal threshold is no longer supplied by the player. Everything the old
     * hysteresis flood did from a 0-100 strength — seeding from the border, growing by colour,
     * absorbing enclosed pockets, shaving the edge — now happens inside BgCascade, which derives what
     * it needs from the picture and reports honestly when it cannot. The {@code tolerance} parameter is
     * therefore ignored on this path and is removed from every signature in the following slice.
     *
     * <p>When the cascade declines, the ORIGINAL bytes are returned. That is the designed outcome, not
     * a failure: a picture the cascade will not damage on a guess bakes unchanged, and the player is
     * pointed at {@code /cb bgpick} to supply the one fact that was missing.
     */
    private static byte[] process(byte[] input, String mode, int tolerance, Integer forcedFill) {
        input = CheckerboardDetector.flattenToBlack(input); // G10-6: flattened-preview checkerboard → black in EVERY mode (no-op otherwise)
        String m = normalize(mode);
        if (NONE.equals(m)) return input; // Off — honoured on a normal bake
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(input));
            if (src == null) return input; // unreadable here → let toBlockPng surface the real error
            int w = src.getWidth(), h = src.getHeight();
            BufferedImage img = toArgb(src);

            BgCascade.Result decision = BgCascade.decide(input, img, w, h, null);
            if (!decision.decided()) return input; // no rung could support an answer — leave it alone

            // Paint the background the base fill and composite the edge band's real coverage onto it,
            // IN LINEAR LIGHT (G10 §H) so anti-aliased edges resolve toward the background without the
            // dark rim a gamma-space blend draws against a dark fill. The cascade has already mixed the
            // old background out of those edge pixels, so this lays the subject on the NEW fill rather
            // than over the old colour — which is what stops a halo of the original background tracing
            // the subject.
            final int fill = forcedFill != null ? forcedFill : BLACK;
            // Composited in place and written back in ONE bulk call. Per-pixel setRGB goes through the
            // colour model on every call, which on a multi-megapixel picture costs several times the
            // composite itself; the coverage grid is already a private array, so it doubles as the
            // output buffer and no extra allocation is needed.
            final int[] composited = decision.unmixed();
            for (int i = 0; i < composited.length; i++) {
                composited[i] = LinearBlend.over(composited[i], fill);
            }
            img.setRGB(0, 0, w, h, composited, 0, w);

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
