/**
 * ScreenTestImages.java — Group 14 "own-texture" MOCKUP (throwaway, CLIENT-ONLY).
 *
 * Builds the gallery of demo pictures the screen-test block cycles through, each as one or more
 * NativeImage frames. These are uploaded by ScreenTestBlockEntityRenderer straight into the
 * TextureManager (no resource pack, no block atlas, no mipmap) — so they prove what a custom block
 * looks like rendered the SAME way Minecraft maps are: full resolution, crisp at any distance, with
 * a per-picture choice of nearest (pixel-exact) or linear (smooth) filtering.
 *
 * The gallery is a mix on purpose, so the tester can judge every failure mode the block atlas has:
 *   1. SHARP TEXT   — 1024px, fine 1px grid + small text → proves text/lines survive (atlas blurs them).
 *   2. SMOOTH SHADE — 1024px gradient + thin circles, LINEAR → proves smooth photo shading + curves.
 *   3. RES CHART    — 1024px checker + radial fan → the aliasing torture (atlas moirés this).
 *   4. ANIMATED     — many frames of moving content → proves crisp animation with no pack reload.
 *   5+. YOUR BLOCKS — best-effort: the player's OWN created blocks loaded at FULL source resolution
 *      (animated ones decoded to frames), so it's a direct A/B against the same block placed normally.
 *
 * Built once, lazily, then cached. Self-contained: procedural cards need no assets; the "your blocks"
 * entries read the local config/customblocks files (present in single-player) and are skipped on error.
 *
 * Depends on: AWT/ImageIO, NativeImage, TextureStore, AnimationDecoder (all usable client-side).
 * Called by:  ScreenTestBlockEntityRenderer.
 */
package com.customblocks.client.render;

import com.customblocks.CustomBlocksMod;
import com.customblocks.core.TextureStore;
import com.customblocks.image.AnimationDecoder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class ScreenTestImages {

    private ScreenTestImages() {}

    /** One gallery entry: a label, its frame(s), and whether it wants smooth (linear) filtering. */
    public static final class Example {
        public final String label;
        public final List<NativeImage> frames;
        public final boolean smooth;
        Example(String label, List<NativeImage> frames, boolean smooth) {
            this.label = label; this.frames = frames; this.smooth = smooth;
        }
    }

    private static volatile List<Example> CACHE;

    /** The gallery, built once on the first frame it's needed. Never throws — a bad entry is skipped. */
    public static List<Example> get() {
        List<Example> local = CACHE;
        if (local != null) return local;
        synchronized (ScreenTestImages.class) {
            if (CACHE != null) return CACHE;
            List<Example> out = new ArrayList<>();
            long t0 = System.currentTimeMillis();
            try { out.add(new Example("Sharp text + 1px grid (nearest)", one(sharpTextCard()), false)); } catch (Exception ignored) {}
            try { out.add(new Example("Smooth shading + circles (linear)", one(gradientCard()), true)); } catch (Exception ignored) {}
            try { out.add(new Example("Resolution chart (nearest)", one(resChartCard()), false)); } catch (Exception ignored) {}
            try { out.add(new Example("Animated sweep (no pack reload)", motionFrames(24), false)); } catch (Exception ignored) {}
            try { addYourBlocks(out, 4); } catch (Exception ignored) {}
            CACHE = out;
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Screen-test gallery built: {} examples in {} ms.",
                    out.size(), System.currentTimeMillis() - t0);
            return out;
        }
    }

    private static List<NativeImage> one(NativeImage img) {
        List<NativeImage> l = new ArrayList<>(1);
        l.add(img);
        return l;
    }

    // ── procedural high-res cards ──────────────────────────────────────────────

    /** 1024px white card: title, a small-text legibility ladder, and a 1px grid (the atlas killers). */
    private static NativeImage sharpTextCard() {
        int N = 1024;
        BufferedImage bi = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = quality(bi.createGraphics());
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, N, N);

        g.setColor(new Color(0xFFDDDDDD, true));   // 1px grid every 64px — survives only at full res
        g.setStroke(new BasicStroke(1f));
        for (int p = 0; p <= N; p += 64) { g.drawLine(p, 0, p, N); g.drawLine(0, p, N, p); }

        g.setColor(new Color(0xFF111111, true));
        g.setStroke(new BasicStroke(3f));
        g.drawRect(4, 4, N - 8, N - 8);

        g.setColor(new Color(0xFF0A66C2, true));
        g.setFont(new Font("SansSerif", Font.BOLD, 120));
        center(g, "OWN TEXTURE", N, 220);
        g.setColor(new Color(0xFF333333, true));
        g.setFont(new Font("SansSerif", Font.PLAIN, 46));
        center(g, "1024px  -  no atlas  -  no mipmap", N, 300);

        // Small-text ladder: if these read cleanly, the atlas-muffle problem is gone.
        int[] sizes = {14, 18, 24, 32, 44};
        int y = 430;
        g.setColor(new Color(0xFF000000, true));
        for (int s : sizes) {
            g.setFont(new Font("SansSerif", Font.PLAIN, s));
            g.drawString(s + "px  Sharp text 0123456789  ABCabc  the quick brown fox", 70, y);
            y += s + 38;
        }
        g.dispose();
        return toNative(bi);
    }

    /** 1024px diagonal gradient + thin concentric circles — proves smooth shading and crisp curves. */
    private static NativeImage gradientCard() {
        int N = 1024;
        BufferedImage bi = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                int r = clamp(x * 255 / N);
                int gg = clamp(y * 255 / N);
                int b = clamp(255 - (x + y) * 255 / (2 * N));
                bi.setRGB(x, y, 0xFF000000 | (r << 16) | (gg << 8) | b);
            }
        }
        Graphics2D g = quality(bi.createGraphics());
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(2f));
        for (int rad = 60; rad < N; rad += 60) {
            g.draw(new Ellipse2D.Float(N / 2f - rad, N / 2f - rad, rad * 2f, rad * 2f));
        }
        g.dispose();
        return toNative(bi);
    }

    /** 1024px aliasing torture: fine checkerboard + a radial line fan. Atlas moirés this; map-texture won't. */
    private static NativeImage resChartCard() {
        int N = 1024;
        BufferedImage bi = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                boolean on = (((x >> 2) + (y >> 2)) & 1) == 0;   // 4px checker
                bi.setRGB(x, y, on ? 0xFF202020 : 0xFFFFFFFF);
            }
        }
        Graphics2D g = quality(bi.createGraphics());
        g.setColor(new Color(0xFFD32F2F, true));
        g.setStroke(new BasicStroke(2f));
        double cx = N / 2.0, cy = N / 2.0;
        for (int a = 0; a < 360; a += 6) {
            double rad = Math.toRadians(a);
            g.drawLine((int) cx, (int) cy, (int) (cx + Math.cos(rad) * N), (int) (cy + Math.sin(rad) * N));
        }
        g.dispose();
        return toNative(bi);
    }

    /** N frames of a rotating sweep + scrolling label — crisp animation, no resource-pack involvement. */
    private static List<NativeImage> motionFrames(int n) {
        int N = 384;
        List<NativeImage> frames = new ArrayList<>(n);
        for (int f = 0; f < n; f++) {
            BufferedImage bi = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = quality(bi.createGraphics());
            g.setColor(new Color(0xFF0E1116, true));
            g.fillRect(0, 0, N, N);
            double ang = 2 * Math.PI * f / n;
            g.setColor(new Color(0xFF22D3EE, true));
            g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(N / 2, N / 2,
                    (int) (N / 2 + Math.cos(ang) * N * 0.42),
                    (int) (N / 2 + Math.sin(ang) * N * 0.42));
            g.setColor(new Color(0xFFFFFFFF, true));
            g.setFont(new Font("SansSerif", Font.BOLD, 40));
            center(g, "ANIMATED", N, N / 2 + 130);
            g.dispose();
            frames.add(toNative(bi));
        }
        return frames;
    }

    // ── the player's own created blocks (best-effort, full source resolution) ──

    /** Append up to {@code max} of the player's real blocks: full-res image, or decoded frames if animated. */
    private static void addYourBlocks(List<Example> out, int max) {
        int added = 0;
        for (int index = 0; index < 1028 && added < max; index++) {
            byte[] tex = TextureStore.load(index);
            if (tex == null) continue;
            try {
                byte[] src = TextureStore.loadSource(index);
                if (src != null && AnimationDecoder.isAnimated(src)) {
                    AnimationDecoder.Decoded dec = AnimationDecoder.decode(src, 512); // own-texture path: full 512px/frame, no atlas
                    if (dec != null && dec.frameCount() > 1) {
                        List<NativeImage> frames = sliceStrip(dec.stripPng(), dec.frameCount());
                        if (!frames.isEmpty()) {
                            out.add(new Example("Your block #" + index + " (animated, full-res)", frames, true));
                            added++;
                            continue;
                        }
                    }
                }
                // Static: show the source at its native resolution if we have it, else the baked texture.
                byte[] best = (src != null && src.length > 0) ? src : tex;
                NativeImage img = NativeImage.read(new ByteArrayInputStream(best));
                out.add(new Example("Your block #" + index + " (full-res)", one(img), true));
                added++;
            } catch (Exception e) {
                // skip this slot — a single unreadable block must never break the gallery
            }
        }
    }

    /** Slice a vertical strip PNG (width W, height W*frames) into {@code n} square NativeImage frames. */
    private static List<NativeImage> sliceStrip(byte[] stripPng, int n) throws Exception {
        NativeImage strip = NativeImage.read(new ByteArrayInputStream(stripPng));
        int w = strip.getWidth();
        int fh = strip.getHeight() / Math.max(1, n);
        List<NativeImage> out = new ArrayList<>(n);
        for (int f = 0; f < n; f++) {
            NativeImage fr = new NativeImage(w, fh, false);
            for (int y = 0; y < fh; y++) {
                for (int x = 0; x < w; x++) fr.setColor(x, y, strip.getColor(x, f * fh + y));
            }
            out.add(fr);
        }
        strip.close();
        return out;
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static Graphics2D quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    /** Draw {@code text} horizontally centered in a {@code width}px image, baseline at {@code baselineY}. */
    private static void center(Graphics2D g, String text, int width, int baselineY) {
        int tw = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (width - tw) / 2, baselineY);
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }

    /** AWT ARGB BufferedImage → NativeImage (which stores pixels as 0xAABBGGRR / ABGR). */
    private static NativeImage toNative(BufferedImage bi) {
        int w = bi.getWidth(), h = bi.getHeight();
        NativeImage ni = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = bi.getRGB(x, y);
                int a = (p >>> 24) & 0xFF, r = (p >> 16) & 0xFF, gg = (p >> 8) & 0xFF, b = p & 0xFF;
                ni.setColor(x, y, (a << 24) | (b << 16) | (gg << 8) | r);
            }
        }
        return ni;
    }
}
