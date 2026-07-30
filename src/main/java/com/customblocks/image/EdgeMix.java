/**
 * EdgeMix.java — the two-colour edge solve, in one place (G10 §H, 2026-07-28).
 *
 * <p>A pixel on any edge is physically a mixture of the colour behind it and the colour in front:
 *
 * <pre>   P = a*F + (1 - a)*B</pre>
 *
 * <p>with {@code a} the fraction of the pixel the subject actually covers. Knowing B and F, that solves
 * for {@code a} by projecting P onto the line from B to F, and then for F by removing the backdrop's
 * share. Both answers are needed: the coverage decides how much of the pixel survives, and the
 * recovered F is what stops the OLD backdrop being laid over the new fill as a halo.
 *
 * <p>The mixing happened in LINEAR LIGHT — light adds linearly, sRGB bytes do not — so the solve is
 * done there, matching every other §H blend.
 *
 * <p>Two callers had grown their own copy of this: rung 5 solved it properly against locally measured
 * endpoints, while the checkerboard cleanup estimated coverage from a colour DISTANCE ramp, which is
 * not a coverage at all — a red pixel half covered by a white grid is far from the grid's colour, so
 * the ramp called it fully covered and left it opaque and pale. That is the outline the owner reported
 * round the share button. One implementation, used by both, is what stops the two answering the same
 * question differently.
 *
 * <p>{@link #explains} is the guard that makes the solve safe to trust. A projection always returns a
 * number, including for a pixel that is neither endpoint nor any mixture of them — a dark keyline
 * between a dark backdrop and a bright subject projects to "all backdrop" and would be erased.
 * Rebuilding the pixel from the answer and requiring it to match what is really there separates "this
 * is a mixture" from "this merely projects onto that line".
 *
 * Depends on: LinearBlend.toLinear, BackgroundRemover.rgbToLab, CieDe2000, BgRungKey.JND.
 * Called by:  image/BgRungUnmix (rung 5), image/CheckerboardDetector (the grid's own edge feather).
 */
package com.customblocks.image;

final class EdgeMix {

    private EdgeMix() {} // static-only

    /**
     * How well the mixture must reproduce the pixel before its coverage is believed, in ΔE00.
     * The mop-up's 2×JND — §H's existing statement of "this is that colour" once a lossy source's
     * own noise is allowed for.
     */
    static final double MIX_TOL = 2.0 * BgRungKey.JND;

    /** Linear-light triple for a packed sRGB pixel. */
    static double[] linear(int argb) {
        return new double[]{LinearBlend.toLinear(argb >> 16),
                            LinearBlend.toLinear(argb >> 8),
                            LinearBlend.toLinear(argb)};
    }

    /**
     * Whether the two endpoints are far enough apart for the projection to mean anything. Below one
     * JND they are the same colour to the eye, and dividing by that separation turns pixel noise into
     * arbitrary coverage.
     */
    static boolean separated(double[] bg, double[] fg) {
        return CieDe2000.of(BackgroundRemover.rgbToLab(pack(fg)),
                            BackgroundRemover.rgbToLab(pack(bg))) > BgRungKey.JND;
    }

    /** Subject coverage of {@code p} between the two endpoints, clamped to 0..1, or -1 if they coincide. */
    static double coverage(int p, double[] bg, double[] fg) {
        double[] q = linear(p);
        double dr = fg[0] - bg[0], dg = fg[1] - bg[1], db = fg[2] - bg[2];
        double denom = dr * dr + dg * dg + db * db;
        if (denom <= 0) return -1;
        double a = ((q[0] - bg[0]) * dr + (q[1] - bg[1]) * dg + (q[2] - bg[2]) * db) / denom;
        return Math.max(0.0, Math.min(1.0, a));
    }

    /**
     * Largest share of the endpoints' OWN separation the rebuild may miss by. The residual and the
     * separation are both ΔE00, so the ratio is dimensionless, and it is the coverage error the miss
     * corresponds to: being off the line by a tenth of its length is being wrong about the mixture by
     * a tenth.
     *
     * <p>Read off the two populations rather than chosen. Every refused band pixel in the twelve
     * baselines was measured as residual-over-separation, 2026-07-29, and they do not form one spread:
     * the genuine anti-aliased edges are done by about 0.35 (the penguin holds 1458 of its 1565 refusals
     * under 0.35 and 5 past 0.45; the letter O holds 4552 of 4561 under 0.30), while the pixels the
     * guard exists to refuse mass past 0.50 (the transparent PNG puts 3143 of 5436 there, the chrome
     * logo 2550 of 3690). The bar sits in the empty middle.
     */
    static final double MIX_RATIO = 0.40;

    /**
     * Whether mixing the endpoints at {@code a} reproduces {@code p} — within {@link #MIX_TOL}
     * outright, or within {@link #MIX_RATIO} of the endpoints' own separation.
     *
     * <p>The absolute bar alone was wrong on exactly the edges this rung exists for. A residual of
     * 8 ΔE00 is a bad fit between two colours that are 10 apart and an excellent one between two that
     * are 90 apart, and an anti-aliased edge runs between the most separated pair in the picture. A
     * lossy source makes that unavoidable: JPEG carries chroma at half resolution, so colour bleeds
     * across a sharp edge and the transition pixels sit slightly OFF the straight line between the two
     * colours while still being plainly mixtures of them. Measured on the penguin, 2026-07-29: the two
     * pixels of its foot's edge rebuild 8.1 and 5.5 ΔE00 off against endpoints 60-plus apart — ratios
     * of 0.13 and 0.08 — and the absolute bar of 4.6 refused both, which is what baked the owner's
     * white outline. Judging a sample pair by the ratio rather than the absolute residual is what
     * robust matting does for the same reason (Wang and Cohen, 2007).
     *
     * <p>The guard it exists to be is untouched. A dark keyline between a dark backdrop and a bright
     * subject is not a mixture of them at all, and it misses by a large FRACTION of their separation,
     * not merely by a large amount: on the chrome logo and the transparent PNG most refused pixels sit
     * past 0.50, well beyond this bar.
     */
    static boolean explains(int p, double[] bg, double[] fg, double a) {
        int predicted = 0xFF000000
                | (encode(a * fg[0] + (1 - a) * bg[0]) << 16)
                | (encode(a * fg[1] + (1 - a) * bg[1]) << 8)
                |  encode(a * fg[2] + (1 - a) * bg[2]);
        double residual = CieDe2000.of(BackgroundRemover.rgbToLab(p | 0xFF000000),
                                       BackgroundRemover.rgbToLab(predicted));
        if (residual <= MIX_TOL) return true;
        double separation = CieDe2000.of(BackgroundRemover.rgbToLab(pack(fg)),
                                         BackgroundRemover.rgbToLab(pack(bg)));
        return residual <= MIX_RATIO * separation;
    }

    /**
     * The subject's own colour at {@code p} with the backdrop's share removed — {@code (P - (1-a)B)/a}
     * — packed as an opaque sRGB int. Undefined below a coverage of {@code 1/255}, where the division
     * amplifies noise into an arbitrary colour; callers treat that as background instead.
     */
    static int recover(int p, double[] bg, double a) {
        double[] q = linear(p);
        return 0xFF000000
                | (encode((q[0] - (1 - a) * bg[0]) / a) << 16)
                | (encode((q[1] - (1 - a) * bg[1]) / a) << 8)
                |  encode((q[2] - (1 - a) * bg[2]) / a);
    }

    /** Linear-light triple back to an opaque sRGB int. */
    static int pack(double[] lin) {
        return 0xFF000000 | (encode(lin[0]) << 16) | (encode(lin[1]) << 8) | encode(lin[2]);
    }

    static int encode(double lin) {
        double c = lin <= 0.0031308 ? lin * 12.92 : 1.055 * Math.pow(lin, 1.0 / 2.4) - 0.055;
        int v = (int) Math.round(c * 255.0);
        return v < 0 ? 0 : Math.min(255, v);
    }
}
