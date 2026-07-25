/**
 * ImageResampler.java
 *
 * Responsibility: high-quality, pure-Java image resizing (Lanczos-3) plus a light unsharp-mask
 * sharpen, used to bake block textures that look crisper than Java2D's bicubic — especially when a
 * small source picture is enlarged to the block size. All resampling runs in PREMULTIPLIED alpha so
 * transparent logos don't grow a dark / coloured halo around their edges. No native code and no extra
 * dependency, so it behaves identically on the client and on a dedicated server (Group 14 §5c,
 * "Option B"). It adds no new detail — it only makes the unavoidable resize far less mushy.
 *
 * Depends on: nothing but java.awt.image.
 * Called by: ImageProcessor.toBlockPng.
 */
package com.customblocks.image;

import java.awt.image.BufferedImage;

public final class ImageResampler {

    private ImageResampler() {} // static-only

    /** Lanczos window radius (a). a=3 is the usual quality/ringing sweet spot. */
    private static final double LANCZOS_A = 3d;

    /** Lanczos-3 kernel value at x (in output-pixel-spacing units). */
    private static double lanczos(double x) {
        if (x == 0d) return 1d;
        double ax = Math.abs(x);
        if (ax >= LANCZOS_A) return 0d;
        double px = Math.PI * x;
        return (Math.sin(px) / px) * (Math.sin(px / LANCZOS_A) / (px / LANCZOS_A));
    }

    /** One output pixel's source samples + their (normalised) weights, for a single axis. */
    private static final class Contrib {
        final int[] idx;
        final float[] wt;
        Contrib(int[] idx, float[] wt) { this.idx = idx; this.wt = wt; }
    }

    /**
     * Precompute, for every destination index along one axis, which source pixels feed it and with
     * what weights. When shrinking we widen the filter support by 1/scale so the down-scale is
     * properly anti-aliased; edge indices are clamped so borders don't darken.
     */
    private static Contrib[] axisContrib(int srcSize, int dstSize) {
        Contrib[] out = new Contrib[dstSize];
        double scale = (double) dstSize / srcSize;
        double filterScale = scale < 1d ? 1d / scale : 1d; // widen support when shrinking
        double support = LANCZOS_A * filterScale;
        for (int i = 0; i < dstSize; i++) {
            double center = (i + 0.5) / scale - 0.5; // source coordinate this output pixel samples
            int left = (int) Math.floor(center - support);
            int right = (int) Math.ceil(center + support);
            int span = right - left + 1;
            int[] idx = new int[span];
            float[] wt = new float[span];
            double sum = 0d;
            int k = 0;
            for (int j = left; j <= right; j++) {
                double w = lanczos((center - j) / filterScale);
                if (w == 0d) continue;
                idx[k] = j < 0 ? 0 : (j >= srcSize ? srcSize - 1 : j); // clamp to edge
                wt[k] = (float) w;
                sum += w;
                k++;
            }
            int[] fi = new int[k];
            float[] fw = new float[k];
            for (int t = 0; t < k; t++) { fi[t] = idx[t]; fw[t] = (float) (wt[t] / sum); } // normalise
            out[i] = new Contrib(fi, fw);
        }
        return out;
    }

    /**
     * Resize {@code src} to {@code dw}x{@code dh} with separable Lanczos-3 in premultiplied alpha.
     * Returns {@code src} unchanged when it is already the target size.
     *
     * <p>Anti-ringing: Lanczos's negative lobes overshoot at hard edges (a bright limb against black would
     * be pushed past the brightest real pixel → clamped to pure white, drawing a white ring). Each output
     * pixel is therefore clamped to the min/max of the real source samples that fed it, so no output can be
     * brighter or darker than its true neighbourhood. Kills the ring; keeps the sharpness.
     */
    public static BufferedImage resize(BufferedImage src, int dw, int dh) {
        int sw = src.getWidth();
        int sh = src.getHeight();
        if (sw == dw && sh == dh) return src;

        // 1) read source → premultiplied float planes (so transparent edges don't bleed colour).
        int[] argb = src.getRGB(0, 0, sw, sh, null, 0, sw);
        int sn = sw * sh;
        float[] sr = new float[sn], sg = new float[sn], sb = new float[sn], sa = new float[sn];
        for (int p = 0; p < sn; p++) {
            int c = argb[p];
            float a = ((c >>> 24) & 0xFF) / 255f;
            sa[p] = a;
            sr[p] = ((c >>> 16) & 0xFF) / 255f * a;
            sg[p] = ((c >>> 8) & 0xFF) / 255f * a;
            sb[p] = (c & 0xFF) / 255f * a;
        }

        // 2) horizontal pass: sw -> dw (height stays sh).
        Contrib[] hx = axisContrib(sw, dw);
        int tn = dw * sh;
        float[] tr = new float[tn], tg = new float[tn], tb = new float[tn], ta = new float[tn];
        for (int y = 0; y < sh; y++) {
            int srow = y * sw, trow = y * dw;
            for (int x = 0; x < dw; x++) {
                Contrib c = hx[x];
                float ar = 0, gg = 0, bb = 0, aa = 0;
                // Anti-ringing: remember the real sample extremes so the weighted sum (which the negative
                // Lanczos lobes can push past them) is clamped back into the source range below.
                float rmn = Float.POSITIVE_INFINITY, rmx = Float.NEGATIVE_INFINITY;
                float gmn = Float.POSITIVE_INFINITY, gmx = Float.NEGATIVE_INFINITY;
                float bmn = Float.POSITIVE_INFINITY, bmx = Float.NEGATIVE_INFINITY;
                float amn = Float.POSITIVE_INFINITY, amx = Float.NEGATIVE_INFINITY;
                for (int t = 0; t < c.idx.length; t++) {
                    int sp = srow + c.idx[t];
                    float w = c.wt[t];
                    float vr = sr[sp], vg = sg[sp], vb = sb[sp], va = sa[sp];
                    ar += vr * w; gg += vg * w; bb += vb * w; aa += va * w;
                    if (w > 1e-6f) {
                        if (vr < rmn) rmn = vr; if (vr > rmx) rmx = vr;
                        if (vg < gmn) gmn = vg; if (vg > gmx) gmx = vg;
                        if (vb < bmn) bmn = vb; if (vb > bmx) bmx = vb;
                        if (va < amn) amn = va; if (va > amx) amx = va;
                    }
                }
                int tp = trow + x;
                tr[tp] = clampRange(ar, rmn, rmx); tg[tp] = clampRange(gg, gmn, gmx);
                tb[tp] = clampRange(bb, bmn, bmx); ta[tp] = clampRange(aa, amn, amx);
            }
        }

        // 3) vertical pass: sh -> dh (width stays dw) → straight ARGB out.
        Contrib[] vy = axisContrib(sh, dh);
        int[] outArgb = new int[dw * dh];
        for (int y = 0; y < dh; y++) {
            Contrib c = vy[y];
            int orow = y * dw;
            for (int x = 0; x < dw; x++) {
                float ar = 0, gg = 0, bb = 0, aa = 0;
                float rmn = Float.POSITIVE_INFINITY, rmx = Float.NEGATIVE_INFINITY;
                float gmn = Float.POSITIVE_INFINITY, gmx = Float.NEGATIVE_INFINITY;
                float bmn = Float.POSITIVE_INFINITY, bmx = Float.NEGATIVE_INFINITY;
                float amn = Float.POSITIVE_INFINITY, amx = Float.NEGATIVE_INFINITY;
                for (int t = 0; t < c.idx.length; t++) {
                    int sp = c.idx[t] * dw + x;
                    float w = c.wt[t];
                    float vr = tr[sp], vg = tg[sp], vb = tb[sp], va = ta[sp];
                    ar += vr * w; gg += vg * w; bb += vb * w; aa += va * w;
                    if (w > 1e-6f) {
                        if (vr < rmn) rmn = vr; if (vr > rmx) rmx = vr;
                        if (vg < gmn) gmn = vg; if (vg > gmx) gmx = vg;
                        if (vb < bmn) bmn = vb; if (vb > bmx) bmx = vb;
                        if (va < amn) amn = va; if (va > amx) amx = va;
                    }
                }
                outArgb[orow + x] = packUnpremult(
                        clampRange(ar, rmn, rmx), clampRange(gg, gmn, gmx),
                        clampRange(bb, bmn, bmx), clampRange(aa, amn, amx));
            }
        }

        BufferedImage out = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, dw, dh, outArgb, 0, dw);
        return out;
    }

    /**
     * Light unsharp mask in premultiplied alpha, used after ANY resample to put back the crisp edges the
     * resize softens. {@code amount} ~0.3–0.6 is gentle. A 3-tap [1 2 1] Gaussian is the blur;
     * out = pixel + amount*(pixel - blur).
     *
     * <p>{@code overshoot} is how far a sharpened pixel may pass its own 3×3 neighbourhood, as a fraction
     * of that neighbourhood's contrast (0 = the old hard clamp). It exists because clamping strictly into
     * the neighbourhood cancels most of the sharpen on exactly the soft edges that need it — the reason a
     * resized picture still baked mushy (G10 §G, owner 2026-07-25). Scaling the allowance by the LOCAL
     * contrast is what keeps it safe: on flat colour the contrast is 0, so the allowance is 0 and a halo
     * cannot be created there; only a real edge earns the small overshoot that reads as sharpness.
     */
    public static BufferedImage unsharpMask(BufferedImage img, float amount, float overshoot) {
        int w = img.getWidth(), h = img.getHeight();
        int n = w * h;
        int[] argb = img.getRGB(0, 0, w, h, null, 0, w);
        float[] r = new float[n], g = new float[n], b = new float[n], a = new float[n];
        for (int p = 0; p < n; p++) {
            int c = argb[p];
            float al = ((c >>> 24) & 0xFF) / 255f;
            a[p] = al;
            r[p] = ((c >>> 16) & 0xFF) / 255f * al;
            g[p] = ((c >>> 8) & 0xFF) / 255f * al;
            b[p] = (c & 0xFF) / 255f * al;
        }
        float[] br = blur3(r, w, h), bg = blur3(g, w, h), bb = blur3(b, w, h), ba = blur3(a, w, h);
        int[] out = new int[n];
        // Sharpening is controlled overshoot; unbounded it would rebuild the very ring the resize clamp
        // removed. Each sharpened pixel is held to its 3×3 neighbourhood widened by `overshoot` × that
        // neighbourhood's own contrast — real micro-contrast survives, a halo on flat colour cannot form.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = y * w + x;
                out[p] = packUnpremult(
                        clampLocal(r, w, h, x, y, r[p] + amount * (r[p] - br[p]), overshoot),
                        clampLocal(g, w, h, x, y, g[p] + amount * (g[p] - bg[p]), overshoot),
                        clampLocal(b, w, h, x, y, b[p] + amount * (b[p] - bb[p]), overshoot),
                        clampLocal(a, w, h, x, y, a[p] + amount * (a[p] - ba[p]), overshoot));
            }
        }
        BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        o.setRGB(0, 0, w, h, out, 0, w);
        return o;
    }

    /** Separable 3-tap [1 2 1]/4 Gaussian, edge-clamped. */
    private static float[] blur3(float[] s, int w, int h) {
        float[] tmp = new float[w * h];
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                float l = s[row + Math.max(0, x - 1)];
                float c = s[row + x];
                float r = s[row + Math.min(w - 1, x + 1)];
                tmp[row + x] = (l + 2f * c + r) * 0.25f;
            }
        }
        float[] out = new float[w * h];
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                float u = tmp[Math.max(0, y - 1) * w + x];
                float c = tmp[row + x];
                float d = tmp[Math.min(h - 1, y + 1) * w + x];
                out[row + x] = (u + 2f * c + d) * 0.25f;
            }
        }
        return out;
    }

    /** Convert premultiplied float RGBA back to straight ARGB int, clamped 0–255. */
    private static int packUnpremult(float r, float g, float b, float a) {
        int ai = clamp255(a * 255f);
        if (ai == 0) return 0; // fully transparent → drop colour (avoids divide-by-zero)
        float inv = 1f / a;
        int ri = clamp255(r * inv * 255f);
        int gi = clamp255(g * inv * 255f);
        int bi = clamp255(b * inv * 255f);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    private static int clamp255(float v) {
        int i = Math.round(v);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }

    /** Clamp {@code v} into [mn,mx]. If no extreme was tracked (mx&lt;mn) leave {@code v} unchanged. */
    private static float clampRange(float v, float mn, float mx) {
        if (mx < mn) return v;
        return v < mn ? mn : (v > mx ? mx : v);
    }

    /**
     * Clamp {@code v} into the min/max of plane {@code s}'s 3×3 neighbourhood around (x,y), edge-clamped,
     * widened at both ends by {@code overshoot} × (max − min). The allowance is proportional to the local
     * contrast on purpose: flat colour has none, so nothing there can be pushed brighter or darker.
     */
    private static float clampLocal(float[] s, int w, int h, int x, int y, float v, float overshoot) {
        float mn = Float.POSITIVE_INFINITY, mx = Float.NEGATIVE_INFINITY;
        for (int dy = -1; dy <= 1; dy++) {
            int yy = y + dy < 0 ? 0 : (y + dy >= h ? h - 1 : y + dy);
            for (int dx = -1; dx <= 1; dx++) {
                int xx = x + dx < 0 ? 0 : (x + dx >= w ? w - 1 : x + dx);
                float n = s[yy * w + xx];
                if (n < mn) mn = n;
                if (n > mx) mx = n;
            }
        }
        float slack = (mx - mn) * overshoot;
        float lo = mn - slack, hi = mx + slack;
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
