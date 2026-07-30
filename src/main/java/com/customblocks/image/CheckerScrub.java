/**
 * CheckerScrub.java — residue cleanup for the flattened-checkerboard path, confined to the grid
 * (G10 §H, 2026-07-28).
 *
 * <p>Lossy sources (webp, JPEG) tint some checker pixels just off-neutral, so they miss
 * {@link CheckerboardDetector}'s tone mask and survive as light dots and edge fuzz on ground that is
 * otherwise cleared. Those leftovers are background and have to go; this class removes them.
 *
 * <p>What it must never do is remove ARTWORK, and the version it replaces did exactly that, because
 * both of its rules described residue by colour and size alone:
 *
 * <ul>
 *   <li>"a light-neutral blob under 200px that touches a non-ink pixel is edge grain" — measured on
 *       the owner's subscribe banner, every diamond dot of ش and ﭗ is a white blob of about 100px
 *       sitting against the letters' near-black drop shadow. Artwork touching artwork read as residue
 *       touching background, and the dots baked black.
 *   <li>"a small blob that is not the biggest one is a speck" — the same blindness, one size down.
 * </ul>
 *
 * <p>The detector already KNOWS where the background is: {@code kill} is the grid it identified. So
 * the rules keep their colour and size tests and gain the one that decides them — a candidate is
 * residue only if it lies IN the grid. A white dot enclosed by red banner is artwork whatever its
 * size; a light speck ringed by cleared checkerboard is residue whatever its colour.
 *
 * <p>Removal also grew a second half. Painting residue black left it OPAQUE inside ground that step 8
 * declares transparent — a black dot hanging in mid-air once the block is baked. Residue that is
 * background is now added to {@code kill}, so it is blackened and declared in one move.
 *
 * Depends on: nothing.
 * Called by:  image/CheckerboardDetector.flattenToBlack (steps 7a–7b).
 */
package com.customblocks.image;

import java.util.Arrays;

final class CheckerScrub {

    private CheckerScrub() {} // static-only

    private static final int    NONBLACK    = 24;   // output pixel counts as "ink" if max channel > this
    private static final int    SPECK_MAX   = 64;   // a non-subject blob ≤ this many px is a cleanup candidate
    private static final int    SPECK_NOISE = 8;    // a blob ≤ this many px goes regardless of colour
    private static final int    SPECK_LIGHT = 170;  // "light-neutral" = max channel ≥ this …
    private static final int    SPECK_CH    = 16;   // … AND chroma ≤ this
    private static final double SPECK_FRAC  = 0.50; // remove a ≤SPECK_MAX blob if ≥ this fraction is light-neutral
    private static final int    EDGE_MAX    = 200;  // edge grain: a light-neutral blob ≤ this px hugging the grid

    /** Fraction of a blob's outside neighbours that must be grid before it counts as lying in the grid. */
    private static final double IN_GRID_FRAC = 0.50;

    private static final int BLACK = 0xFF000000;

    /**
     * Remove checker residue from {@code argb} and record it in {@code kill}. Both arrays are modified
     * in place; {@code kill} only ever grows, and only by pixels this pass proved are background.
     *
     * <p>{@code declared} says the grid was clean enough to hand on as authored transparency, which
     * means the feather has already written each boundary pixel's real coverage into the alpha channel.
     * There is then nothing for the edge-grain rule to do, and plenty for it to break: it judges a
     * light-neutral blob of up to 200px, and on a light subject against a light grid that description
     * fits the SUBJECT'S OWN RIM. Measured on the owner's football, it bit chunks out of the ball's
     * left silhouette. Where the grid was too smeared to hand on there is no coverage to rely on, the
     * fuzz is real, and the rule earns its place.
     */
    static void run(int[] argb, int w, int h, int n, boolean[] kill, boolean declared) {
        if (!declared) edgeGrain(argb, w, h, n, kill);
        specks(argb, w, h, n, kill);
    }

    // ── 7a: light grain clinging to the grid ────────────────────────────────────────────────────────
    /**
     * Light-neutral fuzz left along the boundary where the grid met the subject. It is 8-connected to
     * the subject, so the speck pass cannot see it as its own blob; it is found here by flooding
     * light-neutral pixels and asking whether the component actually borders cleared grid.
     */
    private static void edgeGrain(int[] argb, int w, int h, int n, boolean[] kill) {
        boolean[] seen = new boolean[n];
        int[] q = new int[n];
        int[] comp = new int[n];
        for (int s = 0; s < n; s++) {
            if (seen[s] || !isLightNeutral(argb[s])) continue;
            int head = 0, tail = 0, sz = 0;
            boolean touchesGrid = false;
            q[tail++] = s; seen[s] = true;
            while (head < tail) {
                int idx = q[head++]; comp[sz++] = idx;
                int x = idx % w, y = idx / w;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) continue;
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                        int ni = ny * w + nx;
                        if (kill[ni]) touchesGrid = true;                 // cleared checkerboard next door
                        else if (isLightNeutral(argb[ni]) && !seen[ni]) { seen[ni] = true; q[tail++] = ni; }
                    }
                }
            }
            if (touchesGrid && sz <= EDGE_MAX) {
                for (int k = 0; k < sz; k++) { argb[comp[k]] = BLACK; kill[comp[k]] = true; }
            }
        }
    }

    // ── 7b: isolated specks stranded on cleared ground ──────────────────────────────────────────────
    private static void specks(int[] argb, int w, int h, int n, boolean[] kill) {
        int[] label = new int[n]; Arrays.fill(label, -1);
        int[] size = new int[n];
        int[] light = new int[n];
        int[] onGrid = new int[n];  // outside neighbours that are grid
        int[] onEdge = new int[n];  // outside neighbours in total
        int[] q = new int[n];
        int labels = 0;
        for (int s = 0; s < n; s++) {
            if (label[s] != -1 || !isInk(argb[s])) continue;
            int id = labels++, head = 0, tail = 0, sz = 0, lt = 0, grid = 0, edge = 0;
            q[tail++] = s; label[s] = id;
            while (head < tail) {
                int idx = q[head++], x = idx % w, y = idx / w;
                sz++; if (isLightNeutral(argb[idx])) lt++;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) continue;
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                        int ni = ny * w + nx;
                        if (isInk(argb[ni])) {
                            if (label[ni] == -1) { label[ni] = id; q[tail++] = ni; }
                        } else {
                            edge++; if (kill[ni]) grid++;
                        }
                    }
                }
            }
            size[id] = sz; light[id] = lt; onGrid[id] = grid; onEdge[id] = edge;
        }
        int idMax = -1, max = -1; // the subject = largest ink blob, always protected
        for (int id = 0; id < labels; id++) if (size[id] > max) { max = size[id]; idMax = id; }
        boolean[] drop = new boolean[labels];
        for (int id = 0; id < labels; id++) {
            if (id == idMax) continue;
            int sz = size[id];
            boolean looksLikeResidue = sz <= SPECK_NOISE || (sz <= SPECK_MAX && light[id] >= SPECK_FRAC * sz);
            drop[id] = looksLikeResidue && onEdge[id] > 0 && onGrid[id] >= IN_GRID_FRAC * onEdge[id];
        }
        for (int i = 0; i < n; i++) {
            int id = label[i];
            if (id >= 0 && drop[id]) { argb[i] = BLACK; kill[i] = true; }
        }
    }

    private static boolean isInk(int argb) {
        return Math.max((argb >> 16) & 0xFF, Math.max((argb >> 8) & 0xFF, argb & 0xFF)) > NONBLACK;
    }

    private static boolean isLightNeutral(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        int mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
        return mx >= SPECK_LIGHT && (mx - mn) <= SPECK_CH;
    }
}
