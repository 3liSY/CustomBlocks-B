/**
 * CheckerboardDetector.java — G10-6 fix (v3 + speck-cleanup).
 *
 * Responsibility: recognise a "flattened transparency-checkerboard" source — an OPAQUE image whose
 * background is the editor's grey/white transparency checkerboard baked into pixels (no alpha), the
 * kind that stock sites (rawpixel `image_png_800`, pngtree `ourlarge`) hand out as a *preview*
 * instead of a real transparent PNG — and clean that checkerboard to solid BLACK so it bakes like a
 * normal block instead of rendering the checkerboard in-world.
 *
 * Why v3 (not the old edge-flood): the earlier fix flooded the checkerboard inward from the image
 * border, so checker "pockets" walled off from the border by the subject (e.g. a gap between glyphs)
 * could not be reached and stayed in the bake. v3 is connectivity-free: it builds a neutral, near-tone
 * MASK over the whole image, finds connected components, and kills any component that genuinely carries
 * BOTH checker tones (a real 2-tone checker patch) — border patches AND trapped pockets alike — while a
 * single-tone subject white (text, planet body) survives. A short feather then peels residual checker
 * bleed off the subject edge without darkening the subject itself (a bright-neutral GUARD protects white
 * text edges and a planet limb). detect() stays the strict gate so this never touches a normal image.
 *
 * Why the final speck-cleanup: webp/jpeg compression tints some checker pixels just off-neutral
 * (e.g. chroma 11 > the mask's 10), so they escape the mask and survive as tiny light dots scattered on
 * the black. After v3 the legitimate subject is one large connected mass, so any *small isolated* blob
 * left on the background is residue: cleanup paints black every small light-neutral blob (and any ≤4px
 * speck) that is not the main subject. Removes the dot fuzz; cannot touch text/arrow/planet (all part of
 * the one big blob). Whole pass is gated behind detect() — it only runs on a confirmed checkerboard.
 *
 * Called by: BackgroundRemover.process (flattenToBlack, before the mode-gated flood) ·
 *            command/handlers/CreationCommands (isFlattened, to warn the player the source was a
 *            flattened preview — G10-6 option C).
 */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public final class CheckerboardDetector {

    private CheckerboardDetector() {} // static-only

    // ── detection thresholds (validated offline on the rawpixel + pngtree previews) ──
    private static final int    LIGHT_MIN         = 190;  // a "light" pixel has max channel ≥ this
    private static final int    NEUTRAL_DETECT    = 18;   // detection: max-min ≤ this = near-neutral
    private static final double BORDER_LIGHT_FRAC = 0.85; // ≥ this fraction of the border ring must be light-neutral
    private static final double TONE_MIN_FRAC     = 0.20; // each of the two tones ≥ this fraction of light border pixels
    private static final double DL_MIN            = 2.0;  // the two tones must differ by L* in [DL_MIN, DL_MAX]
    private static final double DL_MAX            = 22.0; // distinct but close (a true checker is two near-white greys)
    private static final int    MIN_ALT           = 4;    // ≥ this many tone switches along a border line = periodic grid
    private static final int    OPAQUE_ALPHA      = 250;  // alpha ≥ this = opaque

    // ── v3 flatten params (validated offline: share_real.webp, uranus_real.png) ──
    private static final int    NEUTRAL_V3   = 10;    // mask chroma gate: max-min ≤ this
    private static final double MASK_TOL_DE  = 14.0;  // mask ΔE: pixel within this of either tone
    private static final int    KILL_MIN_TONE = 8;    // a killed component needs ≥ this many px of EACH tone
    private static final double KILL_MIN_FRAC = 0.10; // …and the minority tone ≥ this fraction of the component
    private static final int    FEATHER_R    = 3;     // feather radius (px) out from killed pixels
    private static final double RAMP_LO      = 6.0;   // ΔE below this → fully peel the checker colour
    private static final double RAMP_HI      = 34.0;  // ΔE above this → keep the pixel untouched
    private static final int    GUARD_LUM    = 235;   // feather GUARD: keep a pixel if max channel > this …
    private static final int    GUARD_CH     = 12;    // … AND chroma < this (protects white text edges / planet limb)

    // ── speck-cleanup params ──
    private static final int    NONBLACK     = 24;    // output pixel counts as "ink" if max channel > this
    private static final int    SPECK_MAX    = 64;    // a non-subject blob ≤ this many px is a cleanup candidate
    private static final int    SPECK_NOISE  = 8;     // a blob ≤ this many px is removed regardless of colour (sub-pixel at block res)
    private static final int    SPECK_LIGHT  = 170;   // "light-neutral" = max channel ≥ this …
    private static final int    SPECK_CH     = 16;    // … AND chroma ≤ this
    private static final double SPECK_FRAC   = 0.50;  // remove a ≤SPECK_MAX blob if ≥ this fraction is light-neutral
    private static final int    EDGE_MAX     = 200;   // edge grain: a light-neutral blob ≤ this px that touches black is checker fuzz

    private static final int    BLACK        = 0xFF000000;

    /** Two background tones (CIE-LAB) of a detected checkerboard. */
    private record Tones(double[] a, double[] b) {}

    /** True if {@code input} is a flattened transparency-checkerboard preview (used to warn the player). */
    public static boolean isFlattened(byte[] input) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(input));
            if (src == null) return false;
            BufferedImage img = toArgb(src);
            return detect(img, img.getWidth(), img.getHeight()) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * If {@code input} is a flattened checkerboard, return PNG bytes with the checkerboard painted
     * opaque BLACK; otherwise return {@code input} unchanged. Never throws — a failure leaves the
     * image untouched so it can never break a retexture.
     */
    public static byte[] flattenToBlack(byte[] input) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(input));
            if (src == null) return input;
            int w = src.getWidth(), h = src.getHeight();
            BufferedImage img = toArgb(src);
            Tones tones = detect(img, w, h);
            if (tones == null) return input; // not a checkerboard → leave it for the normal pipeline

            int n = w * h;
            int[] argb = img.getRGB(0, 0, w, h, null, 0, w);

            boolean[] kill = computeKill(argb, w, h, n, tones); // steps 2–3
            feather(argb, w, h, n, kill, tones);                 // steps 4–5
            for (int i = 0; i < n; i++) if (kill[i]) argb[i] = BLACK; // step 6
            edgeGrainCleanup(argb, w, h, n);                     // step 7a: light grain hugging the subject edge
            speckCleanup(argb, w, h, n);                         // step 7b: isolated specks

            img.setRGB(0, 0, w, h, argb, 0, w);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            return input;
        }
    }

    // ── v3 steps 2–3: mask → connected components → kill genuine 2-tone patches ──────────────────────
    private static boolean[] computeKill(int[] argb, int w, int h, int n, Tones t) {
        // step 2: neutral, near-tone mask
        boolean[] mask = new boolean[n];
        for (int i = 0; i < n; i++) {
            int p = argb[i];
            if (((p >>> 24) & 0xFF) < OPAQUE_ALPHA) continue;
            int r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF;
            if (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > NEUTRAL_V3) continue;
            double[] lab = rgbToLab(p);
            if (Math.min(deltaE(lab, t.a()), deltaE(lab, t.b())) <= MASK_TOL_DE) mask[i] = true;
        }
        // step 3: 4-connected components; kill any that carries both tones
        boolean[] kill = new boolean[n];
        boolean[] seen = new boolean[n];
        int[] comp = new int[n]; // reused per component as a BFS queue + member list
        for (int s = 0; s < n; s++) {
            if (!mask[s] || seen[s]) continue;
            int head = 0, tail = 0;
            comp[tail++] = s; seen[s] = true;
            while (head < tail) {
                int idx = comp[head++], x = idx % w, y = idx / w;
                if (x + 1 < w && mask[idx + 1] && !seen[idx + 1]) { seen[idx + 1] = true; comp[tail++] = idx + 1; }
                if (x > 0     && mask[idx - 1] && !seen[idx - 1]) { seen[idx - 1] = true; comp[tail++] = idx - 1; }
                if (y + 1 < h && mask[idx + w] && !seen[idx + w]) { seen[idx + w] = true; comp[tail++] = idx + w; }
                if (y > 0     && mask[idx - w] && !seen[idx - w]) { seen[idx - w] = true; comp[tail++] = idx - w; }
            }
            int nA = 0, nB = 0;
            for (int k = 0; k < tail; k++) {
                double[] lab = rgbToLab(argb[comp[k]]);
                if (deltaE(lab, t.a()) <= deltaE(lab, t.b())) nA++; else nB++;
            }
            if (nA >= KILL_MIN_TONE && nB >= KILL_MIN_TONE && Math.min(nA, nB) >= KILL_MIN_FRAC * tail) {
                for (int k = 0; k < tail; k++) kill[comp[k]] = true;
            }
        }
        return kill;
    }

    // ── v3 steps 4–5: multi-source BFS from kill pixels, then peel checker bleed off subject edge ────
    private static void feather(int[] argb, int w, int h, int n, boolean[] kill, Tones t) {
        int[] dist = new int[n]; Arrays.fill(dist, Integer.MAX_VALUE);
        int[] near = new int[n]; Arrays.fill(near, -1); // nearest kill pixel's ORIG argb (local checker tone)
        int[] q = new int[n];
        int head = 0, tail = 0;
        for (int i = 0; i < n; i++) if (kill[i]) { dist[i] = 0; near[i] = argb[i]; q[tail++] = i; }
        while (head < tail) {
            int idx = q[head++];
            if (dist[idx] >= FEATHER_R) continue;
            int x = idx % w, y = idx / w, nd = dist[idx] + 1, nc = near[idx];
            if (x + 1 < w && dist[idx + 1] > nd) { dist[idx + 1] = nd; near[idx + 1] = nc; q[tail++] = idx + 1; }
            if (x > 0     && dist[idx - 1] > nd) { dist[idx - 1] = nd; near[idx - 1] = nc; q[tail++] = idx - 1; }
            if (y + 1 < h && dist[idx + w] > nd) { dist[idx + w] = nd; near[idx + w] = nc; q[tail++] = idx + w; }
            if (y > 0     && dist[idx - w] > nd) { dist[idx - w] = nd; near[idx - w] = nc; q[tail++] = idx - w; }
        }
        for (int i = 0; i < n; i++) {
            if (kill[i] || dist[i] > FEATHER_R || near[i] == -1) continue;
            int p = argb[i];
            int r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF;
            int mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
            if (mx > GUARD_LUM && (mx - mn) < GUARD_CH) continue; // GUARD: keep bright-neutral (text edge / planet limb)
            int c = near[i], cr = (c >> 16) & 0xFF, cg = (c >> 8) & 0xFF, cb = c & 0xFF;
            double a = clamp01((deltaE(rgbToLab(p), rgbToLab(c)) - RAMP_LO) / (RAMP_HI - RAMP_LO));
            int nr = clampByte(r - (1 - a) * cr), ng = clampByte(g - (1 - a) * cg), nb = clampByte(b - (1 - a) * cb);
            argb[i] = 0xFF000000 | (nr << 16) | (ng << 8) | nb;
        }
    }

    // ── step 7a: remove light "edge grain" — checker fuzz that clings to the subject's outer edge ──
    //    (these light-neutral specks are 8-connected to the subject blob, so speckCleanup can't see them).
    //    Rule: kill any light-neutral connected component that TOUCHES the black exterior AND is ≤ EDGE_MAX
    //    px. The subject's white body (planet) / text letters are far larger and the letters never touch
    //    black, so they survive; interior light spots (cloud highlights) don't touch black either.
    private static void edgeGrainCleanup(int[] argb, int w, int h, int n) {
        boolean[] seen = new boolean[n];
        int[] q = new int[n];
        int[] comp = new int[n];
        for (int s = 0; s < n; s++) {
            if (seen[s] || !isLightNeutral(argb[s])) continue;
            int head = 0, tail = 0, sz = 0;
            boolean touchesBlack = false;
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
                        if (!isInk(argb[ni])) touchesBlack = true;        // black exterior neighbour
                        else if (isLightNeutral(argb[ni]) && !seen[ni]) { seen[ni] = true; q[tail++] = ni; }
                    }
                }
            }
            if (touchesBlack && sz <= EDGE_MAX) for (int k = 0; k < sz; k++) argb[comp[k]] = BLACK;
        }
    }

    // ── step 7b: remove residual specks (small isolated blobs left on the black background) ──────────
    private static void speckCleanup(int[] argb, int w, int h, int n) {
        int[] label = new int[n]; Arrays.fill(label, -1);
        int[] size = new int[n];   // size per label
        int[] light = new int[n];  // light-neutral px per label
        int[] q = new int[n];
        int labels = 0;
        for (int s = 0; s < n; s++) {
            if (label[s] != -1 || !isInk(argb[s])) continue;
            int id = labels++, head = 0, tail = 0, sz = 0, lt = 0;
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
                        if (label[ni] == -1 && isInk(argb[ni])) { label[ni] = id; q[tail++] = ni; }
                    }
                }
            }
            size[id] = sz; light[id] = lt;
        }
        int idMax = -1, max = -1; // the subject = largest ink blob, always protected
        for (int id = 0; id < labels; id++) if (size[id] > max) { max = size[id]; idMax = id; }
        for (int i = 0; i < n; i++) {
            int id = label[i];
            if (id < 0 || id == idMax) continue;
            int sz = size[id];
            boolean remove = sz <= SPECK_NOISE || (sz <= SPECK_MAX && light[id] >= SPECK_FRAC * sz);
            if (remove) argb[i] = BLACK;
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

    private static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
    private static int clampByte(double v) { int i = (int) Math.round(v); return i < 0 ? 0 : (i > 255 ? 255 : i); }

    // ── detection ────────────────────────────────────────────────────────────────────────────────

    /** Returns the two background tones if {@code img} is a flattened 2-tone neutral checkerboard, else null. */
    private static Tones detect(BufferedImage img, int w, int h) {
        if (w < 16 || h < 16) return null;
        if (hasRealTransparency(img, w, h)) return null; // a genuine transparent PNG → leave it alone

        // Border ring: how light-neutral is it, and which luminance buckets dominate?
        long[] hist = new long[64]; // luminance / 4
        long lightCount = 0, borderCount = 0;
        for (int x = 0; x < w; x++) { borderCount += 2; lightCount += tallyBorder(img.getRGB(x, 0), hist); lightCount += tallyBorder(img.getRGB(x, h - 1), hist); }
        for (int y = 1; y < h - 1; y++) { borderCount += 2; lightCount += tallyBorder(img.getRGB(0, y), hist); lightCount += tallyBorder(img.getRGB(w - 1, y), hist); }
        if (lightCount < borderCount * BORDER_LIGHT_FRAC) return null;

        int b1 = -1, b2 = -1; // two most-common luminance buckets
        for (int i = 0; i < 64; i++) {
            if (b1 < 0 || hist[i] > hist[b1]) { b2 = b1; b1 = i; }
            else if (b2 < 0 || hist[i] > hist[b2]) { b2 = i; }
        }
        if (b1 < 0 || b2 < 0 || hist[b2] == 0) return null;
        if (hist[b1] < lightCount * TONE_MIN_FRAC || hist[b2] < lightCount * TONE_MIN_FRAC) return null;

        int c1 = b1 * 4, c2 = b2 * 4; // bucket centre luminances
        double l1 = rgbToLab(0xFF000000 | gray(c1))[0];
        double l2 = rgbToLab(0xFF000000 | gray(c2))[0];
        double gap = Math.abs(l1 - l2);
        if (gap < DL_MIN || gap > DL_MAX) return null;

        // Periodicity: a true checkerboard switches tone repeatedly along a border line; two solid
        // halves (a normal image) do not.
        int altTop = alternations(img, w, h, true, c1, c2);
        int altLeft = alternations(img, w, h, false, c1, c2);
        if (Math.max(altTop, altLeft) < MIN_ALT) return null;

        return new Tones(meanToneLab(img, w, h, b1), meanToneLab(img, w, h, b2));
    }

    /** Count this border pixel into the luminance histogram if it is light-neutral; returns 1 if light, else 0. */
    private static long tallyBorder(int argb, long[] hist) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        if (Math.max(r, Math.max(g, b)) < LIGHT_MIN) return 0;
        if (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > NEUTRAL_DETECT) return 0;
        hist[luminance(r, g, b) / 4]++;
        return 1;
    }

    /** Tone switches along the top row (horizontal=true) or left column, classifying each light pixel by nearest tone. */
    private static int alternations(BufferedImage img, int w, int h, boolean horizontal, int c1, int c2) {
        int n = horizontal ? w : h, switches = 0, prev = 0;
        for (int i = 0; i < n; i++) {
            int argb = horizontal ? img.getRGB(i, 0) : img.getRGB(0, i);
            int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
            if (Math.max(r, Math.max(g, b)) < LIGHT_MIN
                    || Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > NEUTRAL_DETECT) { prev = 0; continue; }
            int lum = luminance(r, g, b);
            int cls = Math.abs(lum - c1) <= Math.abs(lum - c2) ? 1 : 2;
            if (prev != 0 && cls != prev) switches++;
            prev = cls;
        }
        return switches;
    }

    /** Mean RGB (as LAB) of the light border pixels whose luminance bucket is within ±1 of {@code bucket}. */
    private static double[] meanToneLab(BufferedImage img, int w, int h, int bucket) {
        long sr = 0, sg = 0, sb = 0, n = 0;
        for (int x = 0; x < w; x++) { long[] a = sampleTone(img.getRGB(x, 0), bucket); sr += a[0]; sg += a[1]; sb += a[2]; n += a[3];
                                      long[] c = sampleTone(img.getRGB(x, h - 1), bucket); sr += c[0]; sg += c[1]; sb += c[2]; n += c[3]; }
        for (int y = 1; y < h - 1; y++) { long[] a = sampleTone(img.getRGB(0, y), bucket); sr += a[0]; sg += a[1]; sb += a[2]; n += a[3];
                                          long[] c = sampleTone(img.getRGB(w - 1, y), bucket); sr += c[0]; sg += c[1]; sb += c[2]; n += c[3]; }
        if (n == 0) return rgbToLab(0xFF000000 | gray(bucket * 4));
        return rgbToLab(0xFF000000 | ((int) (sr / n) << 16) | ((int) (sg / n) << 8) | (int) (sb / n));
    }

    private static long[] sampleTone(int argb, int bucket) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        if (Math.max(r, Math.max(g, b)) < LIGHT_MIN
                || Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > NEUTRAL_DETECT) return new long[]{0, 0, 0, 0};
        if (Math.abs(luminance(r, g, b) / 4 - bucket) > 1) return new long[]{0, 0, 0, 0};
        return new long[]{r, g, b, 1};
    }

    /** Sample a grid; true if a meaningful fraction of pixels are non-opaque (a real transparent image). */
    private static boolean hasRealTransparency(BufferedImage img, int w, int h) {
        int stepX = Math.max(1, w / 50), stepY = Math.max(1, h / 50);
        long total = 0, trans = 0;
        for (int x = 0; x < w; x += stepX) {
            for (int y = 0; y < h; y += stepY) {
                total++;
                if (((img.getRGB(x, y) >>> 24) & 0xFF) < OPAQUE_ALPHA) trans++;
            }
        }
        return total > 0 && (double) trans / total > 0.01;
    }

    private static int luminance(int r, int g, int b) { return (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b); }
    private static int gray(int v) { int c = Math.min(255, Math.max(0, v)); return (c << 16) | (c << 8) | c; }

    private static BufferedImage toArgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    // ── CIE-LAB (sRGB → XYZ → L*a*b*, D65) + Euclidean ΔE — same formula as BackgroundRemover ───
    private static double[] rgbToLab(int argb) {
        double rF = ((argb >> 16) & 0xFF) / 255.0;
        double gF = ((argb >> 8) & 0xFF) / 255.0;
        double bF = (argb & 0xFF) / 255.0;
        rF = (rF > 0.04045) ? Math.pow((rF + 0.055) / 1.055, 2.4) : (rF / 12.92);
        gF = (gF > 0.04045) ? Math.pow((gF + 0.055) / 1.055, 2.4) : (gF / 12.92);
        bF = (bF > 0.04045) ? Math.pow((bF + 0.055) / 1.055, 2.4) : (bF / 12.92);
        rF *= 100.0; gF *= 100.0; bF *= 100.0;
        double x = rF * 0.4124 + gF * 0.3576 + bF * 0.1805;
        double y = rF * 0.2126 + gF * 0.7152 + bF * 0.0722;
        double z = rF * 0.0193 + gF * 0.1192 + bF * 0.9505;
        x /= 95.047; y /= 100.000; z /= 108.883;
        x = (x > 0.008856) ? Math.cbrt(x) : (7.787 * x) + (16.0 / 116.0);
        y = (y > 0.008856) ? Math.cbrt(y) : (7.787 * y) + (16.0 / 116.0);
        z = (z > 0.008856) ? Math.cbrt(z) : (7.787 * z) + (16.0 / 116.0);
        return new double[]{(116.0 * y) - 16.0, 500.0 * (x - y), 200.0 * (y - z)};
    }

    private static double deltaE(double[] a, double[] b) {
        return Math.sqrt(Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2) + Math.pow(a[2] - b[2], 2));
    }
}
