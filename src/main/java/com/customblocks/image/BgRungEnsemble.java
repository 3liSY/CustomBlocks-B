/**
 * BgRungEnsemble.java — cascade rung 4: five threshold estimators, voting (G10 §H).
 *
 * <p>The last rung that can decide, and the only statistical one. It measures every pixel's ΔE00 from
 * the picture's border tone, builds a histogram of those distances, and asks five independent
 * estimators (BgThresholds) where the background ends. What makes it honest is that it can lose the
 * vote: the estimators fail on DISJOINT histogram shapes, so when they scatter, that scatter is
 * itself the evidence that this picture is not decidable statistically — and rung 4 hands down to
 * {@code /cb bgpick} instead of picking one estimator's answer and hoping.
 *
 * <p>Three guards stand between a vote and an accepted mask:
 * <ol>
 *   <li>outliers are dropped by z-score, and a majority of the five must survive;
 *   <li>the survivors must agree, judged by consequence: the pixels lying between the lowest and
 *       highest proposal must not add up to an area that counts, so whichever proposal you took the
 *       mask would be the same;
 *   <li>the chosen value is the centre of the widest STABLE run inside the agreement band, not any
 *       one estimator's point, so the same picture re-saved or re-compressed lands on it again.
 * </ol>
 *
 * Depends on: BgThresholds, BgRungKey (border tone + the shared region walk), BgDist, BgQc.
 * Called by:  image/BgCascade.
 */
package com.customblocks.image;


final class BgRungEnsemble {

    private BgRungEnsemble() {} // static-only

    /**
     * Histogram resolution. 256 bins because the source pixels are 8-bit: the picture cannot
     * distinguish more levels than that, so a finer histogram would invent detail the data has not
     * got, and a coarser one would throw away detail it has.
     */
    private static final int BINS = 256;

    /**
     * ΔE00 span the histogram covers. CIEDE2000 across the whole sRGB gamut is dominated by the L*
     * axis, which spans 0-100, so 100 bounds the distance any two sRGB colours can sit apart. Fixing
     * the span rather than scaling it to each picture's own maximum means one bin means the same ΔE in
     * every picture — the property that lets a threshold derived here be compared to one derived
     * anywhere else.
     */
    private static final double SPAN = 100.0;

    /**
     * Where the histogram STARTS. Pixels within a JND of the reference tone are left out of it
     * entirely: they are indistinguishable from the background colour, so no threshold decision
     * concerns them, and the estimators are asked only about the pixels whose colour is genuinely in
     * question.
     *
     * <p>This is not a tuning choice, it is what makes two of the five estimators work at all.
     * Measuring distance from a colour puts an IMPULSE at zero — every pixel exactly matching the
     * reference lands in one bin, often most of the picture. Triangle and Rosin are geometric
     * constructions anchored on the histogram's peak, so against that impulse they measure the impulse
     * and both answered "cut at essentially zero" on every baseline picture. Starting the histogram
     * past the JND removes the spike without discarding a single pixel whose classification was ever
     * in doubt, and the two constructions then behave as their authors intended.
     */
    private static final double FLOOR = BgRungKey.JND;

    /**
     * Outlier bound in standard deviations. Two is the conventional bound — about 95% of a normal
     * sample lies inside it — so an estimator further out than that is not describing the same
     * distribution as the others and is not part of their consensus.
     */
    private static final double OUTLIER_Z = 2.0;

    /** How many of the five estimators must survive outlier rejection: a majority. Definitional. */
    private static final int MIN_SURVIVORS = 3;

    /** Rung 4's proposed mask, or {@code null} when the estimators do not reach a consensus. */
    static boolean[][] mask(int[] px, int w, int h) {
        Integer ref = BgRungKey.borderTone(px, w, h);
        if (ref == null) return null;

        BgDist dist = new BgDist(BackgroundRemover.rgbToLab(ref));
        long[] hist = new long[BINS];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double d = dist.of(px[y * w + x]);
                if (d > FLOOR) hist[bin(d)]++;
            }
        }

        int[] votes = {
                BgThresholds.minError(hist),
                BgThresholds.triangle(hist),
                BgThresholds.rosin(hist),
                BgThresholds.minCrossEntropy(hist),
                BgThresholds.weightedObjectVariance(hist),
        };

        int[] kept = survivors(votes);
        if (kept == null) return null; // too few estimators answered, or they scattered

        int lo = kept[0], hi = kept[kept.length - 1];
        if (!agree(hist, lo, hi, w, h)) return null; // no agreement worth the name

        int consensus = borda(kept);
        int cut = stablePlateauCentre(hist, lo, hi, consensus);
        return BgRungKey.regionWalk(px, w, h, ref, deltaE(cut));
    }

    /**
     * Whether the surviving estimators agree, judged by CONSEQUENCE rather than by how close their
     * numbers look. Two thresholds are the same answer when the pixels lying between them do not add
     * up to an area that counts — take either one and the mask you get differs by nothing anyone
     * would see. So the test is the pixel count in the band the proposals span, against the same
     * geometric area floor the QC gate uses.
     *
     * <p>Comparing the proposals as NUMBERS was tried first and was wrong: requiring five independent
     * estimators to land within a JND of one another is a handful of bins out of 256, which no real
     * picture satisfies, and it made the whole rung dead code. The band's population is the quantity
     * that actually decides whether the disagreement matters — and it discriminates correctly, because
     * a genuine valley between two modes is nearly empty however widely the estimators scatter across
     * it, while a histogram with no real valley fills the band immediately.
     */
    private static boolean agree(long[] hist, int lo, int hi, int w, int h) {
        long between = 0;
        for (int i = lo; i <= hi; i++) between += hist[i];
        return between <= BgQc.areaFloor(w, h);
    }

    /**
     * Drops estimators that did not answer and those more than {@link #OUTLIER_Z} deviations from the
     * group, then returns what is left in ascending order, or {@code null} when fewer than a majority
     * survive. Rejection uses the z-score so the bar scales with how tightly the group already agrees
     * rather than being a fixed number of bins.
     */
    private static int[] survivors(int[] votes) {
        double sum = 0;
        int n = 0;
        for (int v : votes) if (v >= 0) { sum += v; n++; }
        if (n < MIN_SURVIVORS) return null;
        double mean = sum / n;

        double var = 0;
        for (int v : votes) if (v >= 0) var += (v - mean) * (v - mean);
        double sd = Math.sqrt(var / n);

        int[] kept = new int[n];
        int k = 0;
        for (int v : votes) {
            if (v < 0) continue;
            // With sd == 0 every survivor sits exactly on the mean, so nothing is an outlier.
            if (sd > 0 && Math.abs(v - mean) / sd > OUTLIER_Z) continue;
            kept[k++] = v;
        }
        if (k < MIN_SURVIVORS) return null;

        int[] trimmed = java.util.Arrays.copyOf(kept, k);
        java.util.Arrays.sort(trimmed);
        return trimmed;
    }

    /**
     * Borda count over the surviving proposals. Every estimator ranks all the proposals by how close
     * each sits to its own, and awards points by that rank; the proposal with the most points wins.
     *
     * <p>The winner is therefore the proposal the group as a whole is nearest to, which is not
     * necessarily the arithmetic middle — a lone estimator far out on one side shifts a mean but wins
     * no ranks, so a positional count resists it. This is the consensus point the plateau search
     * anchors on.
     */
    private static int borda(int[] proposals) {
        final int n = proposals.length;
        int[] points = new int[n];
        for (int voter = 0; voter < n; voter++) {
            Integer[] order = new Integer[n];
            for (int i = 0; i < n; i++) order[i] = i;
            final int from = proposals[voter];
            java.util.Arrays.sort(order, (a, b) ->
                    Integer.compare(Math.abs(proposals[a] - from), Math.abs(proposals[b] - from)));
            for (int rank = 0; rank < n; rank++) points[order[rank]] += n - rank;
        }
        int best = 0;
        for (int i = 1; i < n; i++) if (points[i] > points[best]) best = i;
        return proposals[best];
    }

    /**
     * Centre of the widest run of STABLE bins inside the agreement band. A bin is stable when its
     * count is at or below the mean count across the band: the running mask area is the cumulative
     * histogram, so a bin's count IS the rate at which the mask grows there, and a low-count bin is a
     * threshold that can move without changing the answer. Taking the centre of the longest such run
     * puts the cut as far as possible from the nearest place the answer changes.
     *
     * <p>The search is bounded by the estimators' own band rather than the whole histogram — sweeping
     * globally would find the vast empty stretch above the subject's distances and centre the cut in
     * it, which is a wide plateau and a completely wrong answer.
     */
    private static int stablePlateauCentre(long[] hist, int lo, int hi, int fallback) {
        int from = Math.max(0, lo), to = Math.min(hist.length - 1, hi);
        if (to <= from) return fallback;

        double sum = 0;
        for (int i = from; i <= to; i++) sum += hist[i];
        double mean = sum / (to - from + 1);

        int bestStart = -1, bestLen = 0, runStart = -1;
        for (int i = from; i <= to; i++) {
            if (hist[i] <= mean) {
                if (runStart < 0) runStart = i;
                int len = i - runStart + 1;
                if (len > bestLen) { bestLen = len; bestStart = runStart; }
            } else {
                runStart = -1;
            }
        }
        return bestLen > 0 ? bestStart + bestLen / 2 : fallback;
    }

    private static int bin(double deltaE) {
        int b = (int) ((deltaE - FLOOR) / (SPAN - FLOOR) * BINS);
        return b < 0 ? 0 : Math.min(b, BINS - 1);
    }

    private static double deltaE(double bin) {
        return FLOOR + bin * (SPAN - FLOOR) / BINS;
    }
}
