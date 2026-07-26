/**
 * BgRungKey.java — cascade rung 2: a known background colour (G10 §H).
 *
 * <p>Rung 2 runs when the background colour is a FACT rather than an estimate. It comes from one of
 * two places: the player named it with {@code /cb bgpick}, or a {@code tRNS} chunk declared it (see
 * BgRungAlpha), or the picture's own border is so plainly one flat tone on all four sides that
 * reading it is not a guess. Anything less certain than that belongs to rung 3 and below.
 *
 * <p>Rung 2 declines when the named colour is not actually in the picture — which for a
 * {@code /cb bgpick} colour is refused out loud rather than silently ignored — or when no side-by-side
 * flat border tone exists to read.
 *
 * <p>The keyed region is a REGION WALK, not a per-pixel colour key. Taking every pixel that matches
 * the key is the rule that made "background + closed areas" destructive: dark shading inside a
 * subject matches a dark background, so a chrome logo lost its shadow lines. So the mask floods
 * inward from the border, then absorbs only enclosed pockets thick enough to be real areas — the
 * counter of a letter "O" fills, a hairline of subject shading does not.
 *
 * Depends on: BgDist (memoised ΔE00), BgMask.absorbEnclosedPockets, BgQc.areaFloor,
 *             BackgroundRemover.rgbToLab.
 * Called by:  image/BgCascade.
 */
package com.customblocks.image;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

final class BgRungKey {

    private BgRungKey() {} // static-only

    /**
     * How close a pixel must sit to the key colour to count as that colour, in ΔE00.
     *
     * <p>Derivation: 2.3 is the average-observer just-noticeable difference for a colour difference
     * (Mahy, Van Eycken &amp; Oosterlinck 1994) — the published point below which two colours are not
     * seen as different colours at all. It is not a tuned tolerance: rung 2's whole claim is that the
     * background colour is KNOWN, so its match bar is "indistinguishable from the key", which is
     * precisely what a JND measures. A background that needs a looser bar than human eyesight is not
     * a known key, and handing such a picture down to rung 3 is the correct outcome.
     */
    static final double JND = 2.3;

    /**
     * Fraction of one side's pixels that must read as the candidate tone for the border to count as
     * unambiguous there. A plain majority — definitional rather than tuned: a side where the
     * candidate is a minority tone is a side the candidate does not describe.
     */
    private static final double SIDE_MAJORITY = 0.5;

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * The picture's border tone when it is unambiguous on all four sides, else {@code null}.
     * The candidate is the most common exact colour on the border; each side must then be a majority
     * of that tone within a JND. Requiring all four sides is what separates a background from a large
     * flat area of the subject that happens to touch one edge.
     */
    static Integer flatBorderTone(BufferedImage img, int w, int h) {
        Map<Integer, Integer> tally = new HashMap<>();
        for (int x = 0; x < w; x++) {
            tally.merge(img.getRGB(x, 0) | 0xFF000000, 1, Integer::sum);
            tally.merge(img.getRGB(x, h - 1) | 0xFF000000, 1, Integer::sum);
        }
        for (int y = 1; y < h - 1; y++) {
            tally.merge(img.getRGB(0, y) | 0xFF000000, 1, Integer::sum);
            tally.merge(img.getRGB(w - 1, y) | 0xFF000000, 1, Integer::sum);
        }
        Integer candidate = null;
        int best = -1;
        for (Map.Entry<Integer, Integer> e : tally.entrySet()) {
            if (e.getValue() > best) { best = e.getValue(); candidate = e.getKey(); }
        }
        if (candidate == null) return null;

        BgDist dist = new BgDist(BackgroundRemover.rgbToLab(candidate));
        int topHits = 0, bottomHits = 0, leftHits = 0, rightHits = 0;
        for (int x = 0; x < w; x++) {
            if (dist.of(img.getRGB(x, 0)) <= JND) topHits++;
            if (dist.of(img.getRGB(x, h - 1)) <= JND) bottomHits++;
        }
        for (int y = 0; y < h; y++) {
            if (dist.of(img.getRGB(0, y)) <= JND) leftHits++;
            if (dist.of(img.getRGB(w - 1, y)) <= JND) rightHits++;
        }
        boolean allSides = topHits > w * SIDE_MAJORITY && bottomHits > w * SIDE_MAJORITY
                && leftHits > h * SIDE_MAJORITY && rightHits > h * SIDE_MAJORITY;
        return allSides ? candidate : null;
    }

    /**
     * Whether {@code key} is genuinely in the picture — enough of it to be an area rather than a few
     * stray pixels, judged by the same geometric floor the QC gate uses. This is what lets a
     * {@code /cb bgpick} colour that matches nothing be refused out loud.
     */
    static boolean present(BufferedImage img, int w, int h, int key) {
        BgDist dist = new BgDist(BackgroundRemover.rgbToLab(key));
        long floor = BgQc.areaFloor(w, h);
        long hits = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (dist.of(img.getRGB(x, y)) <= JND && ++hits > floor) return true;
            }
        }
        return false;
    }

    /**
     * Rung 2's proposed mask: flood inward from every border pixel matching {@code key}, then absorb
     * enclosed pockets of the key colour that are thick enough to be real areas.
     */
    static boolean[][] mask(BufferedImage img, int w, int h, int key) {
        BgDist dist = new BgDist(BackgroundRemover.rgbToLab(key));
        boolean[][] isBg = new boolean[w][h];
        Queue<int[]> queue = new ArrayDeque<>();

        for (int x = 0; x < w; x++) {
            seed(img, isBg, queue, x, 0, dist);
            seed(img, isBg, queue, x, h - 1, dist);
        }
        for (int y = 1; y < h - 1; y++) {
            seed(img, isBg, queue, 0, y, dist);
            seed(img, isBg, queue, w - 1, y, dist);
        }
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            for (int[] d : DIRS) {
                int nx = p[0] + d[0], ny = p[1] + d[1];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h || isBg[nx][ny]) continue;
                if (dist.of(img.getRGB(nx, ny)) > JND) continue;
                isBg[nx][ny] = true;
                queue.add(new int[]{nx, ny});
            }
        }

        boolean[][] pocket = new boolean[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!isBg[x][y] && dist.of(img.getRGB(x, y)) <= JND) pocket[x][y] = true;
            }
        }
        BgMask.absorbEnclosedPockets(isBg, pocket, w, h);
        return isBg;
    }

    private static void seed(BufferedImage img, boolean[][] isBg, Queue<int[]> q, int x, int y, BgDist dist) {
        if (!isBg[x][y] && dist.of(img.getRGB(x, y)) <= JND) {
            isBg[x][y] = true;
            q.add(new int[]{x, y});
        }
    }
}
