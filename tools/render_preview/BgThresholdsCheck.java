/**
 * BgThresholdsCheck.java — validates rung 4's five threshold estimators against histograms whose
 * correct answer is known in advance (G10 §H).
 *
 * <p>Rung 4 declines on every baseline picture, because the earlier rungs are more certain and take
 * them first. That leaves its statistics unexercised by the golden set, so they are checked here
 * instead: synthetic histograms with an analytically known cut, plus the degenerate inputs that would
 * otherwise only be discovered by a player uploading one.
 *
 * <p>The assertions are deliberately loose where the estimator is inherently biased — Triangle and
 * Rosin are geometric constructions and are not supposed to find the Bayes point — and tight only
 * where a method has a defensible exact answer. Tightening them to whatever the code currently prints
 * would test nothing.
 *
 * Usage: java -cp <classes> com.customblocks.image.BgThresholdsCheck
 */
package com.customblocks.image;

public final class BgThresholdsCheck {

    private static int failures = 0;

    public static void main(String[] args) {
        bimodalEqual();
        bimodalLopsided();
        unimodalLongTail();
        degenerate();

        System.out.println();
        if (failures == 0) {
            System.out.println("BgThresholdsCheck: ALL CHECKS PASSED");
        } else {
            System.out.println("BgThresholdsCheck: " + failures + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    /**
     * Two equal Gaussians, equal spread. The Bayes-optimal cut is exactly halfway between the means,
     * which is the case minimum-error thresholding is derived to find, so MET is held to it closely.
     * Every method must at least land strictly between the two modes.
     */
    private static void bimodalEqual() {
        long[] h = new long[256];
        addGaussian(h, 60, 15, 100_000);
        addGaussian(h, 190, 15, 100_000);
        System.out.println("two equal modes at 60 and 190 (Bayes cut = 125)");
        int met = report("  MET  ", BgThresholds.minError(h));
        int tri = report("  Tri  ", BgThresholds.triangle(h));
        int ros = report("  Rosin", BgThresholds.rosin(h));
        int li  = report("  Li   ", BgThresholds.minCrossEntropy(h));
        int wov = report("  WOV  ", BgThresholds.weightedObjectVariance(h));

        expect("MET finds the Bayes cut within 10 bins", Math.abs(met - 125) <= 10);
        for (int t : new int[]{met, tri, ros, li, wov}) {
            expect("every estimator cuts between the modes (got " + t + ")", t > 60 && t < 190);
        }
    }

    /**
     * A large flat background against a small subject — the shape a picture with one dominant colour
     * makes, and the case Otsu is documented to mis-cut. WOV exists for it, so WOV must stay out of
     * the big class rather than slicing into it.
     */
    private static void bimodalLopsided() {
        long[] h = new long[256];
        addGaussian(h, 20, 8, 900_000);   // the background: huge and tight
        addGaussian(h, 150, 25, 60_000);  // the subject: small and spread
        System.out.println("lopsided: background 900k at bin 20, subject 60k at bin 150");
        int met = report("  MET  ", BgThresholds.minError(h));
        int wov = report("  WOV  ", BgThresholds.weightedObjectVariance(h));
        int li  = report("  Li   ", BgThresholds.minCrossEntropy(h));

        expect("WOV cuts clear of the background mode", wov > 20 + 3 * 8);
        expect("WOV cuts below the subject mode", wov < 150);
        expect("MET cuts between the two modes", met > 20 && met < 150);
        expect("Li cuts between the two modes", li > 20 && li < 150);
    }

    /**
     * One peak with a long decaying tail and no second mode at all. The geometric methods are built
     * for exactly this and must answer; the Gaussian-mixture and variance methods are entitled to
     * land anywhere sensible, so they are only required to stay inside the data.
     */
    private static void unimodalLongTail() {
        long[] h = new long[256];
        for (int i = 0; i < 256; i++) h[i] = (long) (500_000 * Math.exp(-(i - 15) / 40.0));
        for (int i = 0; i < 15; i++) h[i] = h[15] * i / 15;
        System.out.println("unimodal: peak at bin 15, exponential tail");
        int tri = report("  Tri  ", BgThresholds.triangle(h));
        int ros = report("  Rosin", BgThresholds.rosin(h));

        expect("Triangle answers on a unimodal histogram", tri > 0);
        expect("Rosin answers on a unimodal histogram", ros > 0);
        expect("Rosin lands on the tail shoulder, not the peak (got " + ros + ")", ros > 15 && ros < 160);
    }

    /** Inputs that must not throw and must report "no answer" rather than a made-up bin. */
    private static void degenerate() {
        System.out.println("degenerate inputs");
        long[] empty = new long[256];
        expect("empty histogram: MET declines", BgThresholds.minError(empty) < 0);
        expect("empty histogram: Triangle declines", BgThresholds.triangle(empty) < 0);
        expect("empty histogram: Rosin declines", BgThresholds.rosin(empty) < 0);
        expect("empty histogram: WOV declines", BgThresholds.weightedObjectVariance(empty) < 0);

        long[] spike = new long[256];
        spike[0] = 1_000_000; // every pixel identical — a single flat colour, no cut exists
        expect("single spike: MET declines", BgThresholds.minError(spike) < 0);
        expect("single spike: Rosin declines", BgThresholds.rosin(spike) < 0);

        // A two-colour picture — flat pixel art. MET is entitled to decline here and does: with one
        // spike per class both spreads are zero, and a Gaussian of zero variance has no defined log
        // likelihood, so answering would mean inventing a spread the data has not got. What must hold
        // is that the ENSEMBLE still has a quorum from the estimators that do not need a spread.
        long[] two = new long[256];
        two[0] = 10; two[255] = 10;
        int answered = 0;
        for (int t : new int[]{BgThresholds.minError(two), BgThresholds.triangle(two),
                BgThresholds.rosin(two), BgThresholds.minCrossEntropy(two),
                BgThresholds.weightedObjectVariance(two)}) {
            if (t >= 0) answered++;
        }
        System.out.println("  two far spikes -> " + answered + " of 5 estimators answered");
        expect("two far spikes: a majority of estimators still answer", answered >= 3);
    }

    private static void addGaussian(long[] h, double mean, double sd, long mass) {
        double norm = 0;
        double[] w = new double[h.length];
        for (int i = 0; i < h.length; i++) {
            w[i] = Math.exp(-0.5 * Math.pow((i - mean) / sd, 2));
            norm += w[i];
        }
        for (int i = 0; i < h.length; i++) h[i] += (long) (mass * w[i] / norm);
    }

    private static int report(String label, int t) {
        System.out.println(label + " -> bin " + t);
        return t;
    }

    private static void expect(String what, boolean ok) {
        if (!ok) {
            failures++;
            System.out.println("  FAIL: " + what);
        }
    }
}
