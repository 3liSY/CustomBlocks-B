/**
 * BgThresholds.java — the five threshold estimators rung 4 votes with (G10 §H).
 *
 * <p>Pure histogram maths: every method takes a bin-count array and returns the bin it would cut at,
 * or -1 when it cannot answer. No image types, no colour types, no shared state — which is what makes
 * the ensemble testable and each estimator's failure independent of the others.
 *
 * <p>They are deliberately founded on DIFFERENT assumptions, because that is the whole point of
 * voting: MET models two Gaussians, Triangle and Rosin are geometric constructions that need no
 * modes at all, Li minimises an information distance, and WOV is Otsu corrected for the case where
 * the two classes are very different sizes. Any one of them is a coin flip on the wrong histogram
 * shape; wide disagreement between them is itself the signal that the picture is not decidable this
 * way, which is what lets rung 4 hand down honestly rather than arbitrarily.
 *
 * <p>Otsu itself is deliberately NOT here (§H locked decision): it assumes the two classes have equal
 * spread, which a flat background against a busy subject violates badly, and it answers by cutting
 * into the subject. WOV is the corrected form that keeps the between-class-variance idea.
 *
 * Depends on: nothing.
 * Called by:  image/BgRungEnsemble.
 */
package com.customblocks.image;

final class BgThresholds {

    private BgThresholds() {} // static-only

    /**
     * Minimum-error thresholding — Kittler &amp; Illingworth (1986). Models each side of the cut as a
     * Gaussian and minimises the resulting classification error:
     *
     * <pre>   J(t) = 1 + 2[P1 ln s1 + P2 ln s2] - 2[P1 ln P1 + P2 ln P2]</pre>
     *
     * <p>§H's primary estimator: it models the two spreads SEPARATELY, so a flat background beside a
     * busy subject is described rather than compensated for.
     */
    static int minError(long[] hist) {
        final int n = hist.length;
        long total = 0;
        for (long c : hist) total += c;
        if (total == 0) return -1;

        double best = Double.POSITIVE_INFINITY;
        int bestT = -1;
        for (int t = 0; t < n - 1; t++) {
            double p1 = 0, m1 = 0, v1 = 0, p2 = 0, m2 = 0, v2 = 0;
            for (int i = 0; i <= t; i++) { p1 += hist[i]; m1 += (double) i * hist[i]; }
            for (int i = t + 1; i < n; i++) { p2 += hist[i]; m2 += (double) i * hist[i]; }
            if (p1 == 0 || p2 == 0) continue;
            m1 /= p1; m2 /= p2;
            for (int i = 0; i <= t; i++) v1 += (i - m1) * (i - m1) * hist[i];
            for (int i = t + 1; i < n; i++) v2 += (i - m2) * (i - m2) * hist[i];
            double s1 = Math.sqrt(v1 / p1), s2 = Math.sqrt(v2 / p2);
            if (s1 <= 0 || s2 <= 0) continue; // a zero-spread class makes the log term meaningless
            double q1 = p1 / total, q2 = p2 / total;
            double j = 1 + 2 * (q1 * Math.log(s1) + q2 * Math.log(s2))
                         - 2 * (q1 * Math.log(q1) + q2 * Math.log(q2));
            if (j < best) { best = j; bestT = t; }
        }
        return bestT;
    }

    /**
     * Triangle method — Zack, Rogers &amp; Latt (1977). Draws a straight line from the histogram's peak
     * to its farthest non-empty end and cuts where the histogram falls furthest below that line. A
     * geometric construction: it needs no modes, so it still answers on a histogram with only one.
     */
    static int triangle(long[] hist) {
        int peak = peakBin(hist);
        int first = firstNonEmpty(hist), last = lastNonEmpty(hist);
        if (peak < 0 || first < 0) return -1;
        // Work down whichever tail is longer — that is the side the cut belongs on.
        int end = (peak - first) >= (last - peak) ? first : last;
        return maxLineDistance(hist, peak, end);
    }

    /**
     * Rosin's unimodal method (2001). Same geometric idea as Triangle, but anchored deliberately at
     * the END of the long right tail rather than the longer side. Built for a histogram with one
     * dominant peak and a trailing shoulder — the shape a picture with a large flat background makes.
     */
    static int rosin(long[] hist) {
        int peak = peakBin(hist);
        int last = lastNonEmpty(hist);
        if (peak < 0 || last <= peak) return -1;
        return maxLineDistance(hist, peak, last);
    }

    /**
     * Minimum cross-entropy — Li &amp; Lee (1993). Chooses the cut whose two-level reconstruction is
     * closest in information terms to the original, minimising
     *
     * <pre>   H(t) = -SUM(i&lt;=t) i*h[i]*ln(m1) - SUM(i&gt;t) i*h[i]*ln(m2)</pre>
     *
     * <p>It judges a cut by how much of the picture's structure survives it, which is an independent
     * question from either the Gaussian fit or the geometric constructions.
     */
    static int minCrossEntropy(long[] hist) {
        final int n = hist.length;
        double best = Double.POSITIVE_INFINITY;
        int bestT = -1;
        for (int t = 0; t < n - 1; t++) {
            double p1 = 0, s1 = 0, p2 = 0, s2 = 0;
            for (int i = 0; i <= t; i++) { p1 += hist[i]; s1 += (double) i * hist[i]; }
            for (int i = t + 1; i < n; i++) { p2 += hist[i]; s2 += (double) i * hist[i]; }
            if (p1 == 0 || p2 == 0) continue;
            double m1 = s1 / p1, m2 = s2 / p2;
            if (m1 <= 0 || m2 <= 0) continue; // ln of a zero mean is undefined; bin 0 alone can do this
            double h = -s1 * Math.log(m1) - s2 * Math.log(m2);
            if (h < best) { best = h; bestT = t; }
        }
        return bestT;
    }

    /**
     * Weighted object variance — Yuan's correction to Otsu (an improved Otsu method using the
     * weighted object variance, 2015). Otsu maximises between-class variance with the two classes
     * weighted only by their priors, which mis-cuts when one class is far smaller than the other. WOV
     * multiplies the OBJECT term by that class's own cumulative probability, which pulls the cut back
     * toward the valley of a lopsided histogram instead of into the larger class.
     *
     * <p>The weight is the cumulative probability of the class BELOW the cut, matching the paper's
     * weighting of the first term of the between-class variance. On this rung's axis — ΔE00 distance
     * from the border tone — the class below the cut is the background, and because that cumulative
     * probability rises with the threshold the weight pushes the cut clear of a large tight background
     * mode instead of settling inside it. Weighting the far side instead was tried and is wrong: the
     * far side's probability FALLS as the cut rises, which drags the answer down into the background
     * peak (BgThresholdsCheck's lopsided case catches exactly that).
     */
    static int weightedObjectVariance(long[] hist) {
        final int n = hist.length;
        long total = 0;
        double grand = 0;
        for (int i = 0; i < n; i++) { total += hist[i]; grand += (double) i * hist[i]; }
        if (total == 0) return -1;
        final double mu = grand / total;

        double best = -1;
        int bestT = -1;
        for (int t = 0; t < n - 1; t++) {
            double c1 = 0, s1 = 0, c2 = 0, s2 = 0;
            for (int i = 0; i <= t; i++) { c1 += hist[i]; s1 += (double) i * hist[i]; }
            for (int i = t + 1; i < n; i++) { c2 += hist[i]; s2 += (double) i * hist[i]; }
            if (c1 == 0 || c2 == 0) continue;
            double p1 = c1 / total, p2 = c2 / total;
            double m1 = s1 / c1, m2 = s2 / c2;
            double j = p1 * (p1 * (m1 - mu) * (m1 - mu)) + p2 * (m2 - mu) * (m2 - mu);
            if (j > best) { best = j; bestT = t; }
        }
        return bestT;
    }

    // ── shared geometry ───────────────────────────────────────────────────────────────────────

    /** Bin of the histogram's tallest count. */
    private static int peakBin(long[] hist) {
        int peak = -1;
        long best = -1;
        for (int i = 0; i < hist.length; i++) if (hist[i] > best) { best = hist[i]; peak = i; }
        return peak;
    }

    private static int firstNonEmpty(long[] hist) {
        for (int i = 0; i < hist.length; i++) if (hist[i] > 0) return i;
        return -1;
    }

    private static int lastNonEmpty(long[] hist) {
        for (int i = hist.length - 1; i >= 0; i--) if (hist[i] > 0) return i;
        return -1;
    }

    /**
     * Bin between {@code peak} and {@code end} lying furthest from the straight line joining them.
     * Counts are normalised against the peak so the distance is measured on a comparable scale
     * whatever the absolute pixel count — otherwise the bin axis and the count axis are incomparable
     * and the "distance" is dominated by whichever happens to be numerically larger.
     */
    private static int maxLineDistance(long[] hist, int peak, int end) {
        double peakCount = hist[peak];
        if (peakCount <= 0) return -1;
        double x1 = peak, y1 = 1.0;                       // peak normalises to height 1
        double x2 = end, y2 = hist[end] / peakCount;
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.hypot(dx, dy);
        if (len == 0) return -1;

        double best = -1;
        int bestT = -1;
        int lo = Math.min(peak, end), hi = Math.max(peak, end);
        for (int i = lo; i <= hi; i++) {
            double y = hist[i] / peakCount;
            double d = Math.abs(dy * (i - x1) - dx * (y - y1)) / len;
            if (d > best) { best = d; bestT = i; }
        }
        return bestT;
    }
}
