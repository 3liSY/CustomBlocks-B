/**
 * ArabicNumberArt.java — Group 13 §H: repaint a bundled number's flat background to a live config
 * hex, EXACTLY, with no rim.
 *
 * The problem this replaces: coloured numbers used to be baked by running the black art through
 * BackgroundRemover.recolorBackground + ImageProcessor.fillBackground — a background *guess*. On
 * art whose background (#0A0A0A) is a near-neighbour of its own black glyph outline (#000000) the
 * guess eats into the outline and leaves a dirty ring, which is what the owner saw in game
 * (2026-07-30: "the arabic numbers that are left are the broken ones").
 *
 * The exact alternative, and why it needs no guessing at all: every glyph is bundled on FOUR flat
 * backgrounds, drawn by one generator (white core, black stroke, flat fill). So each pixel is
 *
 *     p = a * bg + ink        a = how much of the pixel is background (0..1)
 *                             ink = the glyph's own premultiplied contribution, bg-independent
 *
 * Two versions of the same glyph on two DIFFERENT known backgrounds give one equation each, so a
 * solves exactly, ink follows, and the pair recomposes onto any hex. The two source backgrounds
 * are read from the art itself (corner pixel), so nothing here drifts when the art is regenerated.
 *
 * Verified 2026-07-30 against the whole bundled set: recomposing onto a set's ORIGINAL hex
 * reproduces that set's file to within rounding (<=1/255), i.e. only the background moves.
 *
 * Depends on: nothing but AWT/ImageIO — deliberately dependency-free so the bake path cannot
 *             inherit the background-detection behaviour again.
 * Called by:  ArabicSlotBootstrap.bake (numbers only; letters are font-drawn by ArabicTileRenderer)
 */
package com.customblocks.arabic;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public final class ArabicNumberArt {

    private ArabicNumberArt() {} // static-only

    /** The pixel every bundled tile's background occupies — art is drawn edge-to-edge. */
    private static final int CORNER_X = 0, CORNER_Y = 0;

    /**
     * {@code sampleA} repainted so its flat background becomes {@code bgRgb}.
     *
     * @param sampleA  PNG of the glyph on one background (the black set)
     * @param sampleB  PNG of the SAME glyph on a different background (the red set)
     * @param bgRgb    the wanted background, 0xRRGGBB
     * @return the recomposed PNG, or null when the samples cannot be used (missing, different
     *         sizes, or both drawn on the same background — never guess, just decline)
     */
    public static byte[] repaint(byte[] sampleA, byte[] sampleB, int bgRgb) {
        try {
            if (sampleA == null || sampleB == null) return null;
            BufferedImage a = ImageIO.read(new ByteArrayInputStream(sampleA));
            BufferedImage b = ImageIO.read(new ByteArrayInputStream(sampleB));
            if (a == null || b == null) return null;
            int w = a.getWidth(), h = a.getHeight();
            if (b.getWidth() != w || b.getHeight() != h) return null;

            int bgA = a.getRGB(CORNER_X, CORNER_Y) & 0xFFFFFF;
            int bgB = b.getRGB(CORNER_X, CORNER_Y) & 0xFFFFFF;
            // Solve on the channel where the two source backgrounds are furthest apart: the widest
            // separation is the least rounding-sensitive. Identical backgrounds carry no
            // information, so there is nothing to solve and nothing safe to invent.
            int channel = widestChannel(bgA, bgB);
            int spread = channelOf(bgA, channel) - channelOf(bgB, channel);
            if (spread == 0) return null;

            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int pa = a.getRGB(x, y), pb = b.getRGB(x, y);
                    double cover = (double) (channelOf(pa, channel) - channelOf(pb, channel)) / spread;
                    cover = Math.max(0.0, Math.min(1.0, cover));
                    int argb = 0xFF000000;
                    for (int c = 0; c < 3; c++) {
                        double ink = channelOf(pa, c) - cover * channelOf(bgA, c);
                        int v = (int) Math.round(cover * channelOf(bgRgb, c) + ink);
                        argb |= Math.max(0, Math.min(255, v)) << (16 - c * 8);
                    }
                    out.setRGB(x, y, argb);
                }
            }
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(out, "png", png);
            return png.toByteArray();
        } catch (Exception e) {
            return null; // caller logs and keeps the old texture — degrade, never crash
        }
    }

    /** Channel index (0=R, 1=G, 2=B) where two colours differ most. */
    private static int widestChannel(int rgb1, int rgb2) {
        int best = 0, bestGap = -1;
        for (int c = 0; c < 3; c++) {
            int gap = Math.abs(channelOf(rgb1, c) - channelOf(rgb2, c));
            if (gap > bestGap) { bestGap = gap; best = c; }
        }
        return best;
    }

    /** One channel of a packed colour: 0=R, 1=G, 2=B. */
    private static int channelOf(int rgb, int channel) {
        return (rgb >> (16 - channel * 8)) & 0xFF;
    }
}
