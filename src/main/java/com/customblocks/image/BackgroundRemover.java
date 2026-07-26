/**
 * BackgroundRemover.java
 *
 * Responsibility: Remove a custom block image's background, painting removed pixels an OPAQUE
 * BLACK fill (old-version parity; the slot block renders solid, so a removed background is a flat
 * fill, not transparency). The fill is always plain BLACK — content is composed on black, so the
 * subject reads on black as-is (no white flip, no keyline; the recolour path keeps its own fill).
 * Recoded clean from the old ImageProcessor.replaceBackground; CIE-LAB ΔE + flood-fill recycled.
 *
 * Two modes (G10 §H):
 *   none — Off: leave the image untouched.
 *   auto — decide the background from the picture itself (BgCascade) and paint it the fill.
 *
 * There is no strength number anywhere. The three old removal modes differed only in how hard one
 * player-set threshold was applied, and that threshold is gone, so they collapse into Auto.
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

    /** Off — the picture is left exactly as it arrived. */
    public static final String NONE = "none";
    /** Auto — the cascade decides the background. The only removal mode there is. */
    public static final String AUTO = "auto";

    /** Alpha below this counts as transparent → background. Package-private: the cascade's rung 1
     *  reads authored alpha against the same cutoff, so there is one definition of "transparent". */
    static final int OPAQUE_THRESHOLD = 128;
    private static final int BLACK = 0xFF000000;
    /** After resize, pixels with every channel ≤ this snap to pure black (kills bicubic gray halos). */
    private static final int SNAP_MAX = 24;

    /**
     * Remove the background and return cleaned PNG bytes. On mode {@code none} (or any failure)
     * the original bytes are returned unchanged — background removal must never break a retexture.
     * Uses the smart black/white fill (auto-picks based on subject brightness).
     */
    public static byte[] apply(byte[] input, String mode) {
        return process(input, mode, null); // null → plain black fill
    }

    /**
     * Remove the background and paint the removed area with {@code fillRgb} (0xRRGGBB).
     * Used by BgStudio when the player picks a custom fill colour.
     */
    public static byte[] apply(byte[] input, String mode, int fillRgb) {
        return process(input, mode, 0xFF000000 | (fillRgb & 0xFFFFFF));
    }

    /**
     * A bake plus, when the cascade declined, the reason — so a caller can TELL the player instead of
     * handing back unchanged bytes that look like nothing happened.
     *
     * @param png     the bake; the original bytes unchanged when the cascade declined
     * @param declineNote why no background was removed, or {@code null} when some was
     */
    public record Applied(byte[] png, String declineNote) {
        public boolean declined() { return declineNote != null; }
    }

    /**
     * Same as {@link #apply(byte[], String)} but reports a decline. §H requires the player to be told
     * which picture confused the detector and pointed at {@code /cb bgpick}; a byte array alone cannot
     * say that, and silently returning the original is what makes an honest refusal look like a bug.
     */
    public static Applied applyReporting(byte[] input, String mode) {
        byte[] flat = CheckerboardDetector.flattenToBlack(input);
        if (NONE.equals(normalize(mode))) return new Applied(input, null); // Off: not a decline
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(flat));
            if (src == null) return new Applied(input, null); // toBlockPng surfaces the real error
            int w = src.getWidth(), h = src.getHeight();
            BufferedImage img = toArgb(src);
            BgCascade.Result decision = BgCascade.decide(flat, img, w, h, null);
            if (!decision.decided()) return new Applied(input, decision.note());
            return new Applied(process(input, mode, null), null);
        } catch (Exception e) {
            return new Applied(input, null);
        }
    }

    /**
     * Outcome of a {@code /cb bgpick} re-bake.
     *
     * @param png     the new texture, or {@code null} when nothing was baked
     * @param refusal a player-facing reason when {@code png} is null; never null then
     */
    public record Keyed(byte[] png, String refusal) {
        public boolean ok() { return png != null; }
    }

    /**
     * {@code /cb bgpick}: remove the background using a colour the PLAYER named, rather than one the
     * cascade worked out. The colour seeds rung 2 as a known key, which promotes the picture to the
     * most certain rung that can use it — an escape hatch that supplies the missing fact instead of
     * re-running the same algorithm and hoping.
     *
     * <p>It refuses out loud in two distinct cases, because they need different answers from the
     * player: the named colour is not in the picture (wrong colour — pick another), or the keyed
     * region failed the shape gate (right colour, but keying it would not produce a sane background).
     */
    public static Keyed applyNamedKey(byte[] input, int keyRgb, Integer fillRgb) {
        byte[] flat = CheckerboardDetector.flattenToBlack(input);
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(flat));
            if (src == null) return new Keyed(null, "that picture could not be read");
            int w = src.getWidth(), h = src.getHeight();
            BufferedImage img = toArgb(src);

            BgCascade.Result decision =
                    BgCascade.decide(flat, img, w, h, 0xFF000000 | (keyRgb & 0xFFFFFF));
            if (!decision.decided()) return new Keyed(null, decision.note());

            final int fill = fillRgb != null ? (0xFF000000 | (fillRgb & 0xFFFFFF)) : BLACK;
            final int[] composited = decision.unmixed();
            for (int i = 0; i < composited.length; i++) {
                composited[i] = LinearBlend.over(composited[i], fill);
            }
            img.setRGB(0, 0, w, h, composited, 0, w);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", out);
            return new Keyed(out.toByteArray(), null);
        } catch (Exception e) {
            return new Keyed(null, "that picture could not be processed");
        }
    }

    /**
     * The recolour rail (colour variants, colour families, the bundled Arabic sets): find the
     * background and paint it {@code fillRgb} (0xRRGGBB).
     *
     * <p>It takes no mode, because it always runs Auto (G10 §H locked decision). Painting a background
     * a new colour requires knowing where the background IS, so with removal genuinely off a variant
     * would come out identical to its base. This replaces the old "mode {@code none} is treated as
     * {@code edges}" coercion plus the six scattered {@code tol > 0 ? tol : 30} fallbacks that existed
     * only because 0 meant both "strictest" and "off".
     */
    public static byte[] recolorBackground(byte[] input, int fillRgb) {
        return process(input, AUTO, 0xFF000000 | (fillRgb & 0xFFFFFF));
    }

    /**
     * Shared pipeline: decide the background with the cascade, then paint it {@code forcedFill}
     * (or plain black when null).
     *
     * <p>G10 §H: the removal threshold is no longer supplied by the player. Everything the old
     * hysteresis flood did from a 0-100 strength — seeding from the border, growing by colour,
     * absorbing enclosed pockets, shaving the edge — now happens inside BgCascade, which derives what
     * it needs from the picture and reports honestly when it cannot.
     *
     * <p>When the cascade declines, the ORIGINAL bytes are returned. That is the designed outcome, not
     * a failure: a picture the cascade will not damage on a guess bakes unchanged, and the player is
     * pointed at {@code /cb bgpick} to supply the one fact that was missing.
     */
    private static byte[] process(byte[] input, String mode, Integer forcedFill) {
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

    /**
     * Canonicalize any stored or typed value to NONE or AUTO.
     *
     * <p>Anything unrecognised becomes AUTO, never Off (G10 §H). The old behaviour answered Off for an
     * unknown value, which after this rip would have turned a server whose config still said
     * {@code smart} into one that quietly stopped removing backgrounds with nothing said in chat.
     * Failing toward "still works" is the only safe direction.
     */
    public static String normalize(String raw) {
        String m = fromArg(raw);
        return m == null ? AUTO : m;
    }

    /**
     * Parse a value to its canonical mode, accepting the internal ids, the player-facing command
     * arguments, and every retired spelling. Returns null only for something genuinely unrecognised,
     * so a command can show a usage error while {@link #normalize} still fails safe to Auto.
     *
     * <p>The retired names are kept as INPUT only: {@code BgRemove} and {@code BgRemove&More} both mean
     * Auto now, so an existing {@code /cb config background BgRemove&More} line does not hard-error,
     * and {@code BgSmart} resolves to Auto as well. None of them is offered by tab-complete or shown
     * back to the player — {@code BgSmart} in particular was the same code path with the threshold
     * hard-coded to 35, dressed up as intelligence it did not have.
     */
    public static String fromArg(String raw) {
        if (raw == null) return null;
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "none", "nobgremove", "off"                          -> NONE;
            case "auto", "edges", "closed", "smart",
                 "bgremove", "bgremove&more", "bgsmart", "ai"          -> AUTO;
            default -> null;
        };
    }

    /** Player-facing display name shown in the config menu and chat. */
    public static String displayName(String mode) {
        return switch (normalize(mode)) {
            case AUTO -> "Automatic Background Removal";
            default   -> "No Background Removal";
        };
    }

    /** Player-facing command argument for the given mode (for /cb config background). */
    public static String commandArg(String mode) {
        return switch (normalize(mode)) {
            case AUTO -> "Auto";
            default   -> "NoBgRemove";
        };
    }

    /** Next mode in the cycle: there are only two, so this toggles Auto and Off. */
    public static String next(String mode) {
        return NONE.equals(normalize(mode)) ? AUTO : NONE;
    }

    /**
     * After the texture has been resized, snap near-fill pixels to the exact fill colour.
     * The classic case is snapping near-black halos to pure black after bicubic downscaling.
     * No-op when mode is none. When {@code fillRgb} is -1, defaults to smart detection (reads
     * the corners — if they aren't near-black, bails out so a dark subject isn't destroyed).
     */
    public static byte[] snapBackgroundBlack(byte[] png, String mode) {
        return snapBackgroundColor(png, mode, -1);
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
    public static byte[] snapBackgroundColor(byte[] png, String mode, int fillRgb) {
        try {
            BufferedImage read = ImageIO.read(new ByteArrayInputStream(png));
            if (read == null) return png;
            BufferedImage img = toArgb(read);
            int w = img.getWidth(), h = img.getHeight();
            // Flattening alpha to opaque is UNCONDITIONAL (G10 §H): it is what guarantees a baked
            // texture never hands the cutout layer a semi-transparent texel, and it was previously
            // gated on `tolerance > 0`, so with a strength of 0 the always-opaque rule silently did not
            // apply and the hairline came back. Snapping near-fill pixels to the exact fill is still
            // Auto-only, because it cleans up removal artifacts and there are none when removal is off.
            boolean snap = !NONE.equals(normalize(mode));
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
