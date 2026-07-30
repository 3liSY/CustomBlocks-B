/**
 * BgMask.java — background-mask morphology helpers (split out of BackgroundRemover for the §9.3
 * file-size rule). Pure boolean-grid operations on the "is this pixel background?" mask:
 *
 *   despeckle           — area opening: drop tiny foreground specks, judged by component size.
 *   keepLargestForeground — SMART mode: reduce the foreground to its single largest connected blob.
 *
 * No image/colour types — just the mask. The old guarded morphological close was removed by
 * G10 §H Jar A (2026-07-25): the hysteresis flood in BackgroundRemover bridges near-tolerance
 * gaps by colour, which is what the close's geometry was for.
 *
 * Depends on: nothing.
 * Called by:  image/BackgroundRemover.
 */
package com.customblocks.image;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

final class BgMask {

    private BgMask() {} // static-only

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** Floor + divisor for the "tiny foreground island" area: a non-bg blob this small is noise. */
    private static final int SPECK_MIN = 4;
    private static final int SPECK_DIVISOR = 20_000; // ~13 px on a 512² image; tune if specks survive

    /** Pocket thickness floor, and the divisor that scales it with the image. A background-coloured
     *  region must hold a disk of this radius to count as a real enclosed area; anything thinner is
     *  subject shading and is left alone. Required width is {@code 2 * depth + 1} px. */
    private static final int POCKET_MIN_DEPTH = 2;
    private static final int POCKET_DEPTH_DIVISOR = 160; // 512² → 3 (7 px wide); 1024² → 6 (13 px wide)

    /** Pinched-pocket rule (G10 §HA4). A pocket too thin to be a region is normally left alone as
     *  subject shading — but a pocket that is BOTH far too small to be a feature AND separated from
     *  the reachable background by only a hairline wall is not shading, it is background the flood
     *  could not squeeze into. Both bars must be cleared; either alone would eat real detail. */
    private static final int PINCH_MIN_AREA = 8;
    private static final int PINCH_AREA_DIVISOR = 80_000; // 800² → 8 px; 1024² → 13 px
    private static final int PINCH_MIN_WALL = 2;
    private static final int PINCH_WALL_DIVISOR = 400;    // 800² → 2 px; 1600² → 4 px

    /** How wide a separator may be before two pieces of background stop being the same enclosed area
     *  (G10 §HA4). Shares the pinch wall bar: a hairline is a hairline whichever side of it you stand. */
    private static final int SEAM_MIN = PINCH_MIN_WALL;
    private static final int SEAM_DIVISOR = PINCH_WALL_DIVISOR;

    /** Area opening: drop tiny isolated foreground islands (below a size scaled to the image) into
     *  the background. Judged purely by component size — a thin but long feature survives. The old
     *  guarded morphological close is gone (G10 §H): the hysteresis flood bridges near-tolerance
     *  gaps by colour, so no geometric bridging remains to swallow a 1-2 px feature. */
    static void despeckle(boolean[][] isBg, int w, int h) {
        dropForegroundSpecks(isBg, w, h);
    }

    /**
     * Absorb background-coloured POCKETS into the background mask — the enclosed areas an edge flood
     * can never reach, such as the hole inside a letter "O" or the gap between two glyphs.
     *
     * <p>{@code pocket} carries every non-background pixel whose colour matches the background. Taking
     * all of them is what made "background + closed areas" destructive: dark shading inside a subject
     * matches a dark background, so a chrome logo lost its shadow lines and the repainted background
     * showed through the artwork. A genuine enclosed area is a REGION, so each connected component is
     * kept or dropped as a whole, and only components thick enough to hold a small disk are absorbed.
     * Thickness comes from a two-pass city-block distance transform, so the whole test is O(w·h).
     *
     * <p>A second, much narrower door was added for §HA4 (2026-07-27): a PINCHED pocket. A lossy source
     * roughens the subject's outline, and the roughness pinches off crumbs of genuine background that
     * the edge flood can no longer reach — measured on the letter-O baseline as 41 pockets of 6 px or
     * less, sealed behind a 2 px wall, which baked as the blue dots ringing the glyph. They cannot be
     * reached by despeckle either: each crumb is fenced in by the outline it was pinched from, so the
     * foreground component it belongs to is the whole glyph and far too large to be a speck.
     *
     * <p>The two bars TOGETHER are what make this safe, and neither is a tuned threshold. On the same
     * baselines the pinched crumbs measure 6 px and under while the chrome logo's genuine shading
     * pockets — the exact detail this method exists to protect — measure 200 to 1525 px behind walls
     * far thicker than a hairline. The rule sits in the empty gap between the two, not on the edge of
     * either, and the one-way striped source produces no walled-off pocket at all.
     */
    static void absorbEnclosedPockets(boolean[][] isBg, boolean[][] pocket, int w, int h) {
        int minDepth = Math.max(POCKET_MIN_DEPTH, Math.min(w, h) / POCKET_DEPTH_DIVISOR);
        int pinchArea = Math.max(PINCH_MIN_AREA, (w * h) / PINCH_AREA_DIVISOR);
        int pinchWall = Math.max(PINCH_MIN_WALL, Math.min(w, h) / PINCH_WALL_DIVISOR);

        // City-block distance from every pixel to the nearest REACHED background pixel, taken once
        // from the incoming mask: absorbing a blob below must not shorten the wall measured for the
        // next one, or a chain of crumbs could unzip a subject edge one pocket at a time.
        int[][] wall = distanceToBackground(isBg, w, h);

        // City-block distance from each pocket pixel to the nearest non-pocket pixel (outside = 0).
        int[][] dist = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (!pocket[x][y]) { dist[x][y] = 0; continue; }
                int up = (y > 0) ? dist[x][y - 1] : 0;
                int left = (x > 0) ? dist[x - 1][y] : 0;
                dist[x][y] = Math.min(up, left) + 1;
            }
        }
        for (int x = w - 1; x >= 0; x--) {
            for (int y = h - 1; y >= 0; y--) {
                if (!pocket[x][y]) continue;
                int down = (y < h - 1) ? dist[x][y + 1] : 0;
                int right = (x < w - 1) ? dist[x + 1][y] : 0;
                dist[x][y] = Math.min(dist[x][y], Math.min(down, right) + 1);
            }
        }

        // Group the pockets before judging them. Components are taken on the pocket set widened by a
        // hairline, so two pieces of background parted by a seam thinner than that are one area again;
        // only the true pocket pixels are ever absorbed or measured.
        boolean[][] linked = widen(pocket, w, h, Math.max(SEAM_MIN, Math.min(w, h) / SEAM_DIVISOR));

        boolean[][] seen = new boolean[w][h];
        for (int x0 = 0; x0 < w; x0++) {
            for (int y0 = 0; y0 < h; y0++) {
                if (!linked[x0][y0] || seen[x0][y0]) continue;
                List<int[]> blob = new ArrayList<>();
                Queue<int[]> q = new ArrayDeque<>();
                seen[x0][y0] = true;
                q.add(new int[]{x0, y0});
                int maxDepth = 0;
                int minWall = Integer.MAX_VALUE;
                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    if (pocket[p[0]][p[1]]) {
                        blob.add(p);
                        if (dist[p[0]][p[1]] > maxDepth) maxDepth = dist[p[0]][p[1]];
                        if (wall[p[0]][p[1]] < minWall) minWall = wall[p[0]][p[1]];
                    }
                    for (int[] d : DIRS) {
                        int nx = p[0] + d[0], ny = p[1] + d[1];
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h && linked[nx][ny] && !seen[nx][ny]) {
                            seen[nx][ny] = true;
                            q.add(new int[]{nx, ny});
                        }
                    }
                }
                if (blob.isEmpty()) continue;
                boolean isRegion = maxDepth > minDepth;
                boolean isPinched = blob.size() <= pinchArea && minWall <= pinchWall;
                if ((isRegion || isPinched)
                        && opensTheSubject(isBg, blob, w, h)
                        && !shatters(isBg, blob, w, h)) {
                    for (int[] p : blob) isBg[p[0]][p[1]] = true;
                }
            }
        }
    }

    /**
     * Smallest share of the enclosing artwork an enclosed pocket must hold before it is read as an
     * OPENING through that artwork rather than a feature of it.
     *
     * <p>Derived from the gap between the two, not tuned between them. A letter O's counter is most of
     * the glyph it sits in — the whole point of the shape. The penguin's eye whites are about half a
     * percent of the penguin, and Jupiter's bright cloud patch about two tenths of a percent of the
     * planet (both measured 2026-07-28). One tenth sits in the empty space between "most of it" and
     * "a speck on it", and no measured case has ever landed near it.
     */
    private static final double POCKET_MIN_SHARE = 0.10;

    /**
     * Whether an enclosed pocket is big enough, relative to the artwork enclosing it, to be a hole
     * THROUGH that artwork (G10 §H, measured 2026-07-28).
     *
     * <p>The enclosed-pocket rule exists for the counter of a letter O: an area walled off from the
     * background by the subject, but plainly background. It has no way to tell that from a small
     * background-COLOURED feature inside the artwork, and both owner-reported survivors are exactly
     * that — the penguin's white eyes ringed by his black head, and a bright white cloud band inside
     * Jupiter. Colour cannot separate them; each really is the background's colour.
     *
     * <p>Scale can. A hole through a shape is comparable in size to the shape; a detail on a shape is
     * not. So the pocket is measured against the artwork that encloses it rather than against the
     * picture, which is what makes the same rule work on a glyph and on a photograph of a planet.
     *
     * <p>{@link #shatters} is the other half of the same question and neither replaces the other: this
     * one catches a small feature inside a large subject, that one catches a pocket whose removal drops
     * the subject to pieces.
     */
    private static boolean opensTheSubject(boolean[][] isBg, List<int[]> blob, int w, int h) {
        // Any foreground pixel adjacent to the pocket belongs to the artwork enclosing it. The pocket's
        // own pixels are held as packed indices so the adjacency test is a lookup, not a list scan.
        java.util.HashSet<Integer> own = new java.util.HashSet<>(blob.size() * 2);
        for (int[] p : blob) own.add(p[1] * w + p[0]);

        int sx = -1, sy = -1;
        outer:
        for (int[] p : blob) {
            for (int[] d : DIRS) {
                int nx = p[0] + d[0], ny = p[1] + d[1];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                if (!isBg[nx][ny] && !own.contains(ny * w + nx)) { sx = nx; sy = ny; break outer; }
            }
        }
        if (sx < 0) return true; // no artwork around it at all — not this rule's case

        boolean[][] seen = new boolean[w][h];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        seen[sx][sy] = true;
        q.add(new int[]{sx, sy});
        long subject = 0;
        long cap = (long) (blob.size() / POCKET_MIN_SHARE) + 1; // past this the answer cannot change
        while (!q.isEmpty() && subject <= cap) {
            int[] p = q.poll();
            subject++;
            for (int[] d : DIRS) {
                int nx = p[0] + d[0], ny = p[1] + d[1];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                if (isBg[nx][ny] || seen[nx][ny]) continue;
                seen[nx][ny] = true;
                q.add(new int[]{nx, ny});
            }
        }
        return blob.size() >= POCKET_MIN_SHARE * subject;
    }

    /**
     * Whether absorbing {@code blob} would SHATTER the subject — break the artwork it sits in into more
     * separate pieces than it was (G10 §H, measured 2026-07-28).
     *
     * <p>This is the missing half of the enclosed-pocket rule. Clearing an enclosed area is right when
     * the area is a hole the artwork encloses — a letter O's counter — and wrong when it is PART of the
     * artwork that merely shares the background's colour. Colour cannot tell those apart, because in
     * both cases the pixels are the background's colour; that is the whole difficulty. Owner-reported
     * failures all have the same shape: a football on white, whose white panels are enclosed by its own
     * dark seams, and a penguin on white, whose white body is enclosed by its outline. Both bake as
     * wreckage — the ball shot through with holes, the outline in fragments.
     *
     * <p>Geometry tells them apart cleanly. Clearing a real hole leaves the surrounding artwork intact:
     * a letter O stays one connected ring. Clearing artwork that was holding the picture together drops
     * whatever it connected — the ball's seams fall into a dozen loose islands, the penguin's outline
     * into pieces. So the test is the piece count of the subject, before against after.
     *
     * <p>Nothing here is tuned. It is a fact about the picture and it needs no setting, which is what
     * lets background removal stay automatic instead of growing a per-picture option.
     *
     * <p>Counted 4-connected, matching every other component test in §H, and only over the blob's own
     * neighbourhood — a pocket cannot change how anything far from it is connected, so the count runs
     * over the bounding box grown by one pixel rather than the whole picture.
     */
    private static boolean shatters(boolean[][] isBg, List<int[]> blob, int w, int h) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (int[] p : blob) {
            minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]);
        }
        int x0 = Math.max(0, minX - 1), x1 = Math.min(w - 1, maxX + 1);
        int y0 = Math.max(0, minY - 1), y1 = Math.min(h - 1, maxY + 1);

        // Dust does not count. A picture can carry any number of specks — a watermark's stipple, JPEG
        // crumbs — and clearing an area around them detaches them, which is not the artwork breaking
        // apart. Counting them as pieces is what stopped the letter O's own counter from clearing
        // (measured 2026-07-28: the counter holds watermark stipple, so absorbing it "raised" the piece
        // count and the guard blocked a case that has always been correct). The floor is
        // {@link BgQc#areaFloor}, the cascade's existing "too small to be an area", reused rather than
        // restated.
        long dust = BgQc.areaFloor(w, h);

        int before = subjectPieces(isBg, x0, y0, x1, y1, dust);

        boolean[][] after = new boolean[w][h];
        for (int x = x0; x <= x1; x++) System.arraycopy(isBg[x], 0, after[x], 0, h);
        for (int[] p : blob) after[p[0]][p[1]] = true;
        int post = subjectPieces(after, x0, y0, x1, y1, dust);

        return post > before;
    }

    /** Number of 4-connected subject pieces inside the given window, ignoring any below {@code dust}. */
    private static int subjectPieces(boolean[][] isBg, int x0, int y0, int x1, int y1, long dust) {
        int bw = x1 - x0 + 1, bh = y1 - y0 + 1;
        boolean[][] seen = new boolean[bw][bh];
        int pieces = 0;
        ArrayDeque<int[]> q = new ArrayDeque<>();
        for (int x = 0; x < bw; x++) {
            for (int y = 0; y < bh; y++) {
                if (isBg[x0 + x][y0 + y] || seen[x][y]) continue;
                seen[x][y] = true;
                q.add(new int[]{x, y});
                long size = 0;
                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    size++;
                    for (int[] d : DIRS) {
                        int nx = p[0] + d[0], ny = p[1] + d[1];
                        if (nx < 0 || ny < 0 || nx >= bw || ny >= bh) continue;
                        if (isBg[x0 + nx][y0 + ny] || seen[nx][ny]) continue;
                        seen[nx][ny] = true;
                        q.add(new int[]{nx, ny});
                    }
                }
                if (size >= dust) pieces++;
            }
        }
        return pieces;
    }

    /** Grow a mask by {@code steps} city-block pixels. Used only to GROUP pockets, never to absorb. */
    private static boolean[][] widen(boolean[][] src, int w, int h, int steps) {
        boolean[][] cur = new boolean[w][h];
        for (int x = 0; x < w; x++) System.arraycopy(src[x], 0, cur[x], 0, h);
        for (int s = 0; s < steps; s++) {
            boolean[][] next = new boolean[w][h];
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    if (cur[x][y]) { next[x][y] = true; continue; }
                    for (int[] d : DIRS) {
                        int nx = x + d[0], ny = y + d[1];
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h && cur[nx][ny]) { next[x][y] = true; break; }
                    }
                }
            }
            cur = next;
        }
        return cur;
    }

    /**
     * Two-pass city-block distance from every pixel to the nearest background pixel — how thick the
     * wall between a pocket and the background it was pinched off from actually is. Background pixels
     * are 0, their neighbours 1, and so on. O(w·h), same shape as the pocket-thickness transform above.
     */
    private static int[][] distanceToBackground(boolean[][] isBg, int w, int h) {
        final int far = w + h; // larger than any reachable city-block distance, and cannot overflow
        int[][] dist = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (isBg[x][y]) { dist[x][y] = 0; continue; }
                int up = (y > 0) ? dist[x][y - 1] : far;
                int left = (x > 0) ? dist[x - 1][y] : far;
                dist[x][y] = Math.min(far, Math.min(up, left) + 1);
            }
        }
        for (int x = w - 1; x >= 0; x--) {
            for (int y = h - 1; y >= 0; y--) {
                if (isBg[x][y]) continue;
                int down = (y < h - 1) ? dist[x][y + 1] : far;
                int right = (x < w - 1) ? dist[x + 1][y] : far;
                dist[x][y] = Math.min(dist[x][y], Math.min(down, right) + 1);
            }
        }
        return dist;
    }




    /**
     * The other half of the area opening: drop tiny BACKGROUND islands stranded inside the subject.
     *
     * <p>{@link #despeckle} judges the foreground and has always had a mirror image nobody wrote. A
     * pixel of the subject that happens to match the background colour is not background — measured
     * 2026-07-28, Jupiter's brightest cloud tops and Tux's white belly give 74 and 101 such holes, one
     * to thirteen pixels each, scattered across the middle of the subject and baked as pinpricks of
     * see-through. Being "too small to be an area" is the same disqualification in both directions.
     *
     * <p>The second bar is what keeps this from undoing the pinched-pocket rule above, which exists to
     * absorb crumbs of REAL background that a roughened outline sealed off. Those sit against the
     * outline they were pinched from, a hairline from background that the flood did reach. A pinprick
     * in a cloud top is deep inside the subject. Same wall measurement, opposite side of it, so the two
     * rules cannot both claim the same pixel.
     */
    static void fillPinholes(boolean[][] isBg, int w, int h) {
        final int speckArea = Math.max(SPECK_MIN, (w * h) / SPECK_DIVISOR);
        final int wallBar = Math.max(PINCH_MIN_WALL, Math.min(w, h) / PINCH_WALL_DIVISOR);

        boolean[][] reached = reachableBackground(isBg, w, h);
        int[][] wall = distanceToBackground(reached, w, h);

        boolean[][] seen = new boolean[w][h];
        for (int x0 = 0; x0 < w; x0++) {
            for (int y0 = 0; y0 < h; y0++) {
                if (!isBg[x0][y0] || reached[x0][y0] || seen[x0][y0]) continue;
                List<int[]> blob = new ArrayList<>();
                Queue<int[]> q = new ArrayDeque<>();
                seen[x0][y0] = true;
                q.add(new int[]{x0, y0});
                int minWall = Integer.MAX_VALUE;
                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    blob.add(p);
                    if (wall[p[0]][p[1]] < minWall) minWall = wall[p[0]][p[1]];
                    for (int[] d : DIRS) {
                        int nx = p[0] + d[0], ny = p[1] + d[1];
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h
                                && isBg[nx][ny] && !reached[nx][ny] && !seen[nx][ny]) {
                            seen[nx][ny] = true;
                            q.add(new int[]{nx, ny});
                        }
                    }
                }
                if (blob.size() <= speckArea && minWall > wallBar) {
                    for (int[] p : blob) isBg[p[0]][p[1]] = false;
                }
            }
        }
    }

    /** The background the picture's edge can actually walk to — everything else is enclosed. */
    private static boolean[][] reachableBackground(boolean[][] isBg, int w, int h) {
        boolean[][] reached = new boolean[w][h];
        Queue<int[]> q = new ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            for (int y : new int[]{0, h - 1}) {
                if (isBg[x][y] && !reached[x][y]) { reached[x][y] = true; q.add(new int[]{x, y}); }
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x : new int[]{0, w - 1}) {
                if (isBg[x][y] && !reached[x][y]) { reached[x][y] = true; q.add(new int[]{x, y}); }
            }
        }
        while (!q.isEmpty()) {
            int[] p = q.poll();
            for (int[] d : DIRS) {
                int nx = p[0] + d[0], ny = p[1] + d[1];
                if (nx >= 0 && nx < w && ny >= 0 && ny < h && isBg[nx][ny] && !reached[nx][ny]) {
                    reached[nx][ny] = true;
                    q.add(new int[]{nx, ny});
                }
            }
        }
        return reached;
    }

    /** Flood each foreground (non-bg) island; islands at or below the speck area become background. */
    private static void dropForegroundSpecks(boolean[][] isBg, int w, int h) {
        int speckArea = Math.max(SPECK_MIN, (w * h) / SPECK_DIVISOR);
        boolean[][] seen = new boolean[w][h];
        for (int x0 = 0; x0 < w; x0++) {
            for (int y0 = 0; y0 < h; y0++) {
                if (isBg[x0][y0] || seen[x0][y0]) continue;
                List<int[]> blob = new ArrayList<>();
                Queue<int[]> q = new ArrayDeque<>();
                seen[x0][y0] = true;
                q.add(new int[]{x0, y0});
                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    blob.add(p);
                    for (int[] d : DIRS) {
                        int nx = p[0] + d[0], ny = p[1] + d[1];
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h && !isBg[nx][ny] && !seen[nx][ny]) {
                            seen[nx][ny] = true;
                            q.add(new int[]{nx, ny});
                        }
                    }
                }
                if (blob.size() <= speckArea) {
                    for (int[] p : blob) isBg[p[0]][p[1]] = true;
                }
            }
        }
    }

    /**
     * SMART mode: reduce the foreground to its single largest connected component. Every other
     * foreground pixel becomes background. Safe by construction — if there is no foreground at all
     * it is a no-op, and the caller wraps everything in a try/catch that returns the original image.
     */
    static void keepLargestForeground(boolean[][] isBg, int w, int h) {
        boolean[][] seen = new boolean[w][h];
        List<int[]> largest = null;
        for (int x0 = 0; x0 < w; x0++) {
            for (int y0 = 0; y0 < h; y0++) {
                if (isBg[x0][y0] || seen[x0][y0]) continue;
                List<int[]> blob = new ArrayList<>();
                Queue<int[]> q = new ArrayDeque<>();
                seen[x0][y0] = true;
                q.add(new int[]{x0, y0});
                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    blob.add(p);
                    for (int[] d : DIRS) {
                        int nx = p[0] + d[0], ny = p[1] + d[1];
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h && !isBg[nx][ny] && !seen[nx][ny]) {
                            seen[nx][ny] = true;
                            q.add(new int[]{nx, ny});
                        }
                    }
                }
                if (largest == null || blob.size() > largest.size()) largest = blob;
            }
        }
        if (largest == null) return; // no subject found — leave the mask as-is
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) if (!isBg[x][y]) isBg[x][y] = true; // everything → bg…
        }
        for (int[] p : largest) isBg[p[0]][p[1]] = false;                   // …then carve back the subject
    }

}
