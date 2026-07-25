/**
 * ResizePreview.java — HEADLESS PREVIEW HARNESS (not shipped in the jar). G10 §G "sharper resizing".
 *
 * Bakes the same block texture the server would (ImageProcessor.toBlockPng's Lanczos-3 resample from
 * the REAL ImageResampler) under the current settings and under candidate sharpen settings, then
 * writes labelled side-by-side PNGs — full tile plus a 3x nearest-neighbour crop, because softness is
 * only visible zoomed in.
 *
 * Only the unsharp step is duplicated here (with the candidate's overshoot allowance); the resize
 * itself calls the shipped ImageResampler, so the pixels are the game's pixels. Winning constants get
 * ported into ImageResampler/ImageProcessor after the owner picks.
 *
 * Run: javac -cp <classes> ResizePreview.java && java ResizePreview <outDir> <img>...
 */

import com.customblocks.image.ImageResampler;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class ResizePreview {

    static final int SIZE = 512; // CustomBlocksConfig.textureSize

    /** One candidate: how hard to sharpen when enlarging / when shrinking, and how far a sharpened
     *  pixel may overshoot its own 3x3 neighbourhood, as a fraction of that neighbourhood's contrast. */
    record Setting(String name, float enlarge, float shrink, float overshoot) {}

    static final Setting[] SETTINGS = {
            new Setting("NOW (shipped)",   0.45f, 0.00f, 0.00f),
            new Setting("A - gentle",      0.60f, 0.35f, 0.20f),
            new Setting("B - strong",      0.80f, 0.50f, 0.32f),
    };

    public static void main(String[] args) throws Exception {
        File outDir = new File(args[0]);
        outDir.mkdirs();
        for (int i = 1; i < args.length; i++) {
            File f = new File(args[i]);
            BufferedImage src = ImageIO.read(f);
            if (src == null) { System.out.println("unreadable: " + f); continue; }
            BufferedImage[] baked = new BufferedImage[SETTINGS.length];
            for (int s = 0; s < SETTINGS.length; s++) baked[s] = bake(src, SETTINGS[s]);
            String base = f.getName().replaceAll("\\.[^.]+$", "");
            double scale = Math.min((double) SIZE / src.getWidth(), (double) SIZE / src.getHeight());
            String head = base + "  -  source " + src.getWidth() + "x" + src.getHeight() + " -> 512px block  ("
                    + (scale > 1 ? "ENLARGED x" : "shrunk x") + String.format("%.2f", scale) + ")";
            ImageIO.write(sheet(baked, head, focus(baked[0])), "PNG", new File(outDir, "resize_" + base + ".png"));
            System.out.println("wrote resize_" + base + ".png   " + head);
        }
    }

    /** ImageProcessor.toBlockPng's geometry + the candidate's sharpen. */
    static BufferedImage bake(BufferedImage src, Setting st) {
        int sw = src.getWidth(), sh = src.getHeight();
        double scale = Math.min((double) SIZE / sw, (double) SIZE / sh);
        int dw = Math.max(1, (int) Math.round(sw * scale));
        int dh = Math.max(1, (int) Math.round(sh * scale));
        BufferedImage scaled = ImageResampler.resize(src, dw, dh);
        float amount = scale > 1.0 ? st.enlarge() : (scale < 1.0 ? st.shrink() : 0f); // already-target-size = never sharpen
        if (amount > 0f) scaled = unsharp(scaled, amount, st.overshoot());
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.BLACK); // the bake composites onto black; do the same so crops read like the block
        g.fillRect(0, 0, SIZE, SIZE);
        g.drawImage(scaled, (SIZE - dw) / 2, (SIZE - dh) / 2, null);
        g.dispose();
        return out;
    }

    // ── candidate unsharp: ImageResampler.unsharpMask + a contrast-proportional overshoot allowance ──
    static BufferedImage unsharp(BufferedImage img, float amount, float overshoot) {
        int w = img.getWidth(), h = img.getHeight(), n = w * h;
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
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = y * w + x;
                out[p] = pack(
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

    static float[] blur3(float[] s, int w, int h) {
        float[] tmp = new float[w * h], out = new float[w * h];
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                tmp[row + x] = (s[row + Math.max(0, x - 1)] + 2f * s[row + x] + s[row + Math.min(w - 1, x + 1)]) * 0.25f;
            }
        }
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                out[row + x] = (tmp[Math.max(0, y - 1) * w + x] + 2f * tmp[row + x] + tmp[Math.min(h - 1, y + 1) * w + x]) * 0.25f;
            }
        }
        return out;
    }

    /** Clamp into the 3x3 neighbourhood, widened by overshoot x the neighbourhood's own contrast.
     *  On FLAT colour the contrast is 0, so the allowance is 0 and no halo can be created. */
    static float clampLocal(float[] s, int w, int h, int x, int y, float v, float overshoot) {
        float mn = Float.POSITIVE_INFINITY, mx = Float.NEGATIVE_INFINITY;
        for (int dy = -1; dy <= 1; dy++) {
            int yy = Math.min(h - 1, Math.max(0, y + dy));
            for (int dx = -1; dx <= 1; dx++) {
                int xx = Math.min(w - 1, Math.max(0, x + dx));
                float nv = s[yy * w + xx];
                if (nv < mn) mn = nv;
                if (nv > mx) mx = nv;
            }
        }
        float slack = (mx - mn) * overshoot;
        float lo = mn - slack, hi = mx + slack;
        return v < lo ? lo : (v > hi ? hi : v);
    }

    static int pack(float r, float g, float b, float a) {
        int ai = c255(a * 255f);
        if (ai == 0) return 0;
        float inv = 1f / a;
        return (ai << 24) | (c255(r * inv * 255f) << 16) | (c255(g * inv * 255f) << 8) | c255(b * inv * 255f);
    }

    static int c255(float v) { int i = Math.round(v); return i < 0 ? 0 : Math.min(255, i); }

    // ── sheet building ────────────────────────────────────────────────────────
    /** Pick the busiest 128px square (highest neighbour contrast) — that's where softness shows. */
    static Rectangle focus(BufferedImage im) {
        int win = 128, step = 32, bestX = 0, bestY = 0;
        long best = -1;
        for (int y = 0; y + win <= SIZE; y += step) {
            for (int x = 0; x + win <= SIZE; x += step) {
                long e = 0;
                for (int yy = y; yy < y + win; yy += 2) {
                    for (int xx = x; xx < x + win - 2; xx += 2) {
                        int p = im.getRGB(xx, yy), q = im.getRGB(xx + 2, yy);
                        e += Math.abs(((p >> 16) & 0xFF) - ((q >> 16) & 0xFF));
                    }
                }
                if (e > best) { best = e; bestX = x; bestY = y; }
            }
        }
        return new Rectangle(bestX, bestY, win, win);
    }

    static BufferedImage sheet(BufferedImage[] baked, String head, Rectangle crop) {
        int cell = 300, zoom = 300, pad = 14, headH = 34, capH = 22;
        int w = pad + (cell + pad) * baked.length;
        int h = headH + cell + capH + zoom + capH + pad;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x111111));
        g.fillRect(0, 0, w, h);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 16));
        g.drawString(head, pad, 22);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        for (int i = 0; i < baked.length; i++) {
            int x = pad + i * (cell + pad);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(baked[i], x, headH, cell, cell, null);
            Setting s = SETTINGS[i];
            g.setColor(Color.WHITE);
            g.drawString(s.name(), x, headH + cell + 16);
            // zoomed crop, nearest neighbour so pixels stay pixels
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(baked[i].getSubimage(crop.x, crop.y, crop.width, crop.height),
                    x, headH + cell + capH, zoom, zoom, null);
            g.setColor(new Color(0x666666));
            g.drawRect(x, headH + cell + capH, zoom, zoom);
            g.setColor(Color.WHITE);
            g.drawString("zoom 2.3x  -  sharpen " + s.enlarge() + " up / " + s.shrink() + " down, overshoot " + s.overshoot(),
                    x, headH + cell + capH + zoom + 16);
        }
        g.dispose();
        return out;
    }
}
