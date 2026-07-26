/**
 * BgCascade.java — the five-rung background decision, ordered by certainty (G10 §H).
 *
 * <p>The replacement for the deleted {@code /cb tolerance} knob. Rather than one cleverer threshold,
 * the mask is decided by the FIRST rung that can both produce an answer and support it: each rung
 * states what it assumes, and a rung whose assumption does not hold hands down instead of guessing.
 * Every proposed mask clears the same shape gate (BgQc) before it is accepted; a rejected mask is
 * never partially used. If every rung declines, that is the honest outcome — removal does not run,
 * the picture bakes unchanged, and the player is pointed at {@code /cb bgpick}.
 *
 * <pre>
 *   1 authored alpha    the file's own alpha channel                     (a fact)
 *   2 known key         a named colour, a tRNS colour, or a flat border  (a fact)
 *   3 saliency          how much of a region's perimeter is on the frame (a measurement)
 *   4 estimator ensemble MET, Triangle, Rosin, Li, weighted-object-variance (a statistic)
 *   5 colour unmixing   refines the edge band of whatever 1-4 decided    (never decides)
 * </pre>
 *
 * <p>The order is strictly decreasing certainty: moving a lower rung up would let a guess overrule a
 * fact. Rung 5 is separate because sub-pixel edge quality is a different question from which region
 * is the background.
 *
 * <p>Cost note (§H budget): the bulk rails bake every block on the server, so the cheap rungs are
 * first not merely because they are more certain but so that the common picture never reaches the
 * expensive one.
 *
 * Depends on: BgQc, BgMask, BgRungAlpha, BgRungKey, BgRungSaliency, BgRungEnsemble, BgRungUnmix.
 * Called by:  image/BackgroundRemover (Auto path).
 */
package com.customblocks.image;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

final class BgCascade {

    private BgCascade() {} // static-only

    /**
     * What the cascade concluded.
     *
     * @param mask           the accepted background mask, or {@code null} when every rung declined
     * @param unmixed        the picture as row-major ARGB with rung 5's subject COVERAGE in the alpha channel and
     *                       the background mixed back out of the RGB, or {@code null} alongside a null
     *                       mask. This, not the mask, is what a caller should composite: the mask says
     *                       which side each pixel is on, this says how much of the subject is really
     *                       there and what colour it is underneath
     * @param rung           which rung decided (1-4), or 0 when none did
     * @param note           player-facing summary: how it was decided, or why it was not
     * @param namedKeyAbsent the colour handed in by {@code /cb bgpick} is not in the picture — the one
     *                       decline the caller must refuse out loud rather than treat as "unsure"
     */
    record Result(boolean[][] mask, int[] unmixed, int rung, String note, boolean namedKeyAbsent) {
        boolean decided() { return mask != null; }
    }

    /**
     * Run the cascade. {@code raw} is the source bytes (rung 1 reads them for a {@code tRNS} chunk
     * Java's decoder drops); {@code img} is the same picture already decoded to ARGB and
     * checkerboard-flattened. {@code pickedKey} is a {@code /cb bgpick} colour as {@code 0xFFRRGGBB},
     * or {@code null} when the player has not named one.
     */
    static Result decide(byte[] raw, BufferedImage img, int w, int h, Integer pickedKey) {
        List<String> declines = new ArrayList<>();
        // One raster grab for the whole cascade. BufferedImage.getRGB(x, y) goes through the colour
        // model on every call, which on a multi-megapixel picture costs several times what an array
        // read does across the handful of full passes the rungs make; extracting once also stops each
        // rung allocating its own copy of a large image.
        final int[] px = img.getRGB(0, 0, w, h, null, 0, w);

        // ── Rung 1 — authored alpha ────────────────────────────────────────────────────────────
        boolean[][] authored = BgRungAlpha.mask(px, w, h);
        if (authored == null) {
            declines.add("rung 1: the file has no transparency of its own");
        } else {
            String bad = clean(authored, w, h);
            if (bad == null) {
                // No unmixing here, and the raster passes straight through: the file's alpha channel
                // ALREADY states the coverage of every edge pixel, in exactly the place the composite
                // looks for it. Estimating it from neighbouring colours could only be a worse answer to
                // a question the author already answered.
                return new Result(authored, px, 1,
                        "used the picture's own transparency", false);
            }
            declines.add("rung 1: its transparency " + bad);
        }

        // ── Rung 2 — known key ────────────────────────────────────────────────────────────────
        Integer key = pickedKey;
        String keySource = "the colour you named";
        if (key == null) {
            key = BgRungAlpha.trnsKey(raw);
            keySource = "the transparent colour the file declares";
        }
        if (key == null) {
            key = BgRungKey.flatBorderTone(px, w, h);
            keySource = "the picture's own flat border colour";
        }
        if (key == null) {
            declines.add("rung 2: no single flat colour runs along all four edges");
        } else if (!BgRungKey.present(px, w, h, key)) {
            if (pickedKey != null) {
                return new Result(null, null, 0, "that colour is not in this picture", true);
            }
            declines.add("rung 2: the keyed colour covers too little of the picture");
        } else {
            boolean[][] keyed = BgRungKey.mask(px, w, h, key);
            String bad = clean(keyed, w, h);
            if (bad == null) return unmixed(px, keyed, w, h, 2, "matched " + keySource);
            if (pickedKey != null) {
                // The player asserted a fact. If keying it does not produce a sane background, the honest
                // answer is that their colour does not work — NOT to ignore them and start guessing with
                // the lower rungs, which is what falling through would do.
                return new Result(null, null, 0, "keying that colour " + bad, false);
            }
            declines.add("rung 2: keying " + keySource + " " + bad);
        }

        // ── Rung 3 — boundary-connectivity saliency ───────────────────────────────────────────
        boolean[][] salient = BgRungSaliency.mask(px, w, h);
        if (salient == null) {
            declines.add("rung 3: no area sits against the picture edge clearly enough to be the background");
        } else {
            String bad = clean(salient, w, h);
            if (bad == null) {
                return unmixed(px, salient, w, h, 3, "found the area wrapping the picture edge");
            }
            declines.add("rung 3: the wrapping area " + bad);
        }

        // ── Rung 4 — estimator ensemble ───────────────────────────────────────────────────────
        boolean[][] voted = BgRungEnsemble.mask(px, w, h);
        if (voted == null) {
            declines.add("rung 4: the measurements of where the background ends did not agree");
        } else {
            String bad = clean(voted, w, h);
            if (bad == null) {
                return unmixed(px, voted, w, h, 4, "measured where the background ends");
            }
            declines.add("rung 4: the measured background " + bad);
        }

        return new Result(null, null, 0, String.join("; ", declines), false);
    }

    /**
     * Clean a proposed mask, then judge it. Returns the QC rejection reason, or {@code null} to accept.
     *
     * <p>The cleanup is an AREA OPENING (§H: judged on component size, never on width): a foreground
     * island too small to be an area is dropped into the background. Without it, single stray pixels
     * that happened to fall outside the mask survive as "subject" and bake as coloured specks along an
     * edge — visible on the letter-O baseline as blue dots scattered around the glyph. Judging by size
     * rather than width is what lets it remove a speck while leaving a hair-thin outline, which is why
     * a morphological opening is not used.
     *
     * <p>It runs before the gate so QC judges the mask that will actually be used.
     */
    private static String clean(boolean[][] mask, int w, int h) {
        BgMask.despeckle(mask, w, h);
        return BgQc.reject(mask, w, h);
    }

    /**
     * Rung 5 — run on every mask a colour-based rung produced (2, 3 and 4), which is why it is not a
     * branch of its own: it does not compete with them, it finishes their work by turning the binary
     * boundary into the fractional coverage the edge really has.
     */
    private static Result unmixed(int[] px, boolean[][] mask, int w, int h,
                                  int rung, String note) {
        return new Result(mask, BgRungUnmix.refine(px, mask, w, h), rung, note, false);
    }

}
