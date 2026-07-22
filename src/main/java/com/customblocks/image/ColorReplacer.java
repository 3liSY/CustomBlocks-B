/**
 * ColorReplacer.java
 *
 * Responsibility: Targeted pixel colour operations for the colour-variant system (Group 06 /
 * M3 hex). swapColor replaces pixels near one RGB with another — a direct per-pixel swap with
 * NO flood-fill, so design pixels away from the old colour are never touched (the old
 * project's safe batch-recolour path). tint repaints base item art (Squares / Triangles) to a
 * configured hex by scaling the target colour with each pixel's brightness, so outlines and
 * shading survive.
 *
 * Depends on: javax.imageio only.
 * Called by:  core/ColorVariantService.recolorVariants (swapColor),
 *             network/ServerPackGenerator (tint).
 */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public final class ColorReplacer {

    private ColorReplacer() {} // static-only

    /**
     * Replace every pixel within {@code tol} per-channel distance of {@code oldRgb} with
     * {@code newRgb}. Alpha is preserved; all other pixels pass through untouched.
     */
    public static byte[] swapColor(byte[] png, int oldRgb, int newRgb, int tol) throws Exception {
        BufferedImage img = read(png);
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int or = oldRgb >> 16 & 0xFF, og = oldRgb >> 8 & 0xFF, ob = oldRgb & 0xFF;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int r = argb >> 16 & 0xFF, g = argb >> 8 & 0xFF, b = argb & 0xFF;
                if (Math.abs(r - or) <= tol && Math.abs(g - og) <= tol && Math.abs(b - ob) <= tol) {
                    argb = (argb & 0xFF000000) | (newRgb & 0xFFFFFF);
                }
                out.setRGB(x, y, argb);
            }
        }
        return write(out);
    }

    /**
     * Re-tint item art to {@code rgb} by HSB transfer: every pixel takes the target's hue AND
     * saturation, and its brightness is NORMALISED against the bundled fill ({@code baseRgb}) before
     * scaling by the target's brightness — so the fill lands exactly on the configured hex while the
     * art's outline / shading survive as relative light/dark.
     *
     * Saturation must be the TARGET's, not target-sat × pixel-sat: the bundled tool art is already a
     * coloured shape (e.g. red_square.png is #EE3333 with shading), so multiplying by the pixel's own
     * (sub-1.0) saturation capped the output back at that muted source colour.
     *
     * Brightness must be NORMALISED, not raw {@code p[2] × t[2]}: the green art's fill is dark
     * (#1E8C1E, brightness ≈0.55), so {@code p[2] × t[2]} kept the fill at ≈0.55 no matter how bright
     * the target — re-tinting it to a bright green (#10FF01) reproduced the SAME dark green and looked
     * unchanged (green→green has no hue shift either, so nothing moved). Dividing each pixel's
     * brightness by the bundled fill's brightness ({@code baseV}) first maps the fill to 1.0, so it
     * reaches the target's brightness exactly; highlights clamp at full, shadows scale down relative.
     * Red/yellow art is already bright (base ≈0.93) so this also lets them hit their exact hex.
     */
    public static byte[] tint(byte[] png, int rgb, int baseRgb) throws Exception {
        BufferedImage img = read(png);
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float[] t = java.awt.Color.RGBtoHSB(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, null);
        float[] base = java.awt.Color.RGBtoHSB(baseRgb >> 16 & 0xFF, baseRgb >> 8 & 0xFF, baseRgb & 0xFF, null);
        float baseV = Math.max(base[2], 1f / 255f); // bundled fill brightness; floor avoids ÷0 (e.g. near-black art)
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                float[] p = java.awt.Color.RGBtoHSB(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, null);
                float v = Math.min(1f, p[2] / baseV * t[2]); // fill (p≈baseV) → target brightness; shading stays relative
                int tinted = java.awt.Color.HSBtoRGB(t[0], t[1], v);
                out.setRGB(x, y, (argb & 0xFF000000) | (tinted & 0xFFFFFF));
            }
        }
        return write(out);
    }

    /**
     * Recolour a BAKED variant's flat background to {@code newRgb} WITHOUT flood-fill (Group 06 §C
     * no-source fallback). A variant made through {@code createVariant}/{@code recolorVariants} has a
     * flat fill background (its transparent padding was filled, and its detected bg was flooded, to one
     * hex), so the four corners ARE that fill — we sample them instead of trusting the old config hex.
     * Every pixel within {@code tol} of that fill is swapped to {@code newRgb}; nothing floods, so design
     * pixels away from the fill are mathematically untouched (CLAUDE.md §7) — no second-pass peel/fringe
     * to eat the art. Returns {@code null} (→ caller counts it "unchanged, retexture it") when there is no
     * flat background to swap: corners disagree (a full-bleed design, no bg), any corner is transparent,
     * the fill already equals {@code newRgb}, or no pixel matched. This replaces the fragile re-run of the
     * full BgRemove flood pipeline on an already-baked PNG.
     */
    public static byte[] recolorFlatBg(byte[] png, int newRgb, int tol) throws Exception {
        BufferedImage img = read(png);
        int w = img.getWidth(), h = img.getHeight();
        int[] corners = {img.getRGB(0, 0), img.getRGB(w - 1, 0), img.getRGB(0, h - 1), img.getRGB(w - 1, h - 1)};
        int br = corners[0] >> 16 & 0xFF, bgc = corners[0] >> 8 & 0xFF, bb = corners[0] & 0xFF;
        for (int c : corners) {
            if ((c >>> 24) < OPAQUE_MIN) return null;              // a transparent corner → no flat fill
            int r = c >> 16 & 0xFF, g = c >> 8 & 0xFF, b = c & 0xFF;
            if (Math.abs(r - br) > tol || Math.abs(g - bgc) > tol || Math.abs(b - bb) > tol) return null; // corners disagree → full-bleed design, skip
        }
        if ((corners[0] & 0xFFFFFF) == (newRgb & 0xFFFFFF)) return null; // fill already the target hex
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int changed = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int r = argb >> 16 & 0xFF, g = argb >> 8 & 0xFF, b = argb & 0xFF;
                if (Math.abs(r - br) <= tol && Math.abs(g - bgc) <= tol && Math.abs(b - bb) <= tol) {
                    argb = (argb & 0xFF000000) | (newRgb & 0xFFFFFF);
                    changed++;
                }
                out.setRGB(x, y, argb);
            }
        }
        return changed == 0 ? null : write(out);
    }

    private static final int OPAQUE_MIN = 200; // corner alpha floor: a flat fill is opaque

    private static BufferedImage read(byte[] png) throws Exception {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        if (img == null) throw new Exception("Could not read that texture.");
        return img;
    }

    private static byte[] write(BufferedImage img) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }
}
