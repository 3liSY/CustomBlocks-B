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
 * Depends on: BgQc, BgRungAlpha, BgRungKey.
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
     * @param rung           which rung decided (1-4), or 0 when none did
     * @param note           player-facing summary: how it was decided, or why it was not
     * @param namedKeyAbsent the colour handed in by {@code /cb bgpick} is not in the picture — the one
     *                       decline the caller must refuse out loud rather than treat as "unsure"
     */
    record Result(boolean[][] mask, int rung, String note, boolean namedKeyAbsent) {
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

        // ── Rung 1 — authored alpha ────────────────────────────────────────────────────────────
        boolean[][] alpha = BgRungAlpha.mask(img, w, h);
        if (alpha == null) {
            declines.add("rung 1: the file has no transparency of its own");
        } else {
            String bad = BgQc.reject(alpha, w, h);
            if (bad == null) return new Result(alpha, 1, "used the picture's own transparency", false);
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
            key = BgRungKey.flatBorderTone(img, w, h);
            keySource = "the picture's own flat border colour";
        }
        if (key == null) {
            declines.add("rung 2: no single flat colour runs along all four edges");
        } else if (!BgRungKey.present(img, w, h, key)) {
            if (pickedKey != null) {
                return new Result(null, 0, "that colour is not in this picture", true);
            }
            declines.add("rung 2: the keyed colour covers too little of the picture");
        } else {
            boolean[][] keyed = BgRungKey.mask(img, w, h, key);
            String bad = BgQc.reject(keyed, w, h);
            if (bad == null) return new Result(keyed, 2, "matched " + keySource, false);
            declines.add("rung 2: keying " + keySource + " " + bad);
        }

        // ── Rungs 3-4 — measurement and statistics ────────────────────────────────────────────
        // Built in the following slices; until then the cascade honestly reports that it ran out of
        // rungs rather than falling back on a threshold, which is the behaviour being replaced.
        declines.add("rungs 3-4: not available yet");

        return new Result(null, 0, String.join("; ", declines), false);
    }
}
