package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;

/**
 * UnmixProbe — for named pixels, print exactly what rung 5 saw: whether the pixel is in the band, the
 * two endpoints it measured, the coverage it solved, and whether the mixture rebuilt the pixel. The
 * point is to see WHICH condition declines, rather than infer it from the bake.
 *
 * Usage: java -cp <out> com.customblocks.image.UnmixProbe <src> <x,y> [<x,y> ...]
 */
public final class UnmixProbe {
    public static void main(String[] args) throws Exception {
        byte[] raw = Files.readAllBytes(new File(args[0]).toPath());
        raw = CheckerboardDetector.flattenToBlack(raw);
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(raw));
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage img = toArgb(src);
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);

        BgCascade.Result r = BgCascade.decide(raw, img, w, h, null);
        if (r.mask() == null) { System.out.println("declined"); return; }
        boolean[][] isBg = r.mask();

        Class<?> u = BgRungUnmix.class;
        Method bandM = u.getDeclaredMethod("band", boolean[][].class, int.class, int.class);
        bandM.setAccessible(true);
        Method growM = u.getDeclaredMethod("grow", int[].class, boolean[][].class, boolean[][].class,
                int.class, int.class);
        growM.setAccessible(true);
        Method solidM = u.getDeclaredMethod("solidMean", int[].class, boolean[][].class,
                boolean[][].class, int.class, int.class, int.class, int.class, boolean.class, int.class);
        solidM.setAccessible(true);

        boolean[][] band = (boolean[][]) bandM.invoke(null, isBg, w, h);
        int grown = (Integer) growM.invoke(null, px, isBg, band, w, h);
        int reach = 4 + grown; // SOLID_RADIUS = 2 * BAND_RADIUS

        System.out.println("rung " + r.rung() + "   band grew " + grown + " rings, reach " + reach
                + "   MIX_TOL " + EdgeMix.MIX_TOL);

        for (int i = 1; i < args.length; i++) {
            String[] parts = args[i].split(",");
            int x = Integer.parseInt(parts[0]), y = Integer.parseInt(parts[1]);
            int p = px[y * w + x];
            System.out.printf("%n(%d,%d) rgb=%06x  isBg=%b  inBand=%b%n",
                    x, y, p & 0xFFFFFF, isBg[x][y], band[x][y]);
            double[] bg = (double[]) solidM.invoke(null, px, isBg, band, w, h, x, y, true, reach);
            double[] fg = (double[]) solidM.invoke(null, px, isBg, band, w, h, x, y, false, reach);
            if (bg == null || fg == null) {
                System.out.println("   DECLINED: no " + (bg == null ? "background" : "subject")
                        + " ground within reach");
                continue;
            }
            System.out.printf("   window-mean B=%06x  F=%06x%n",
                    EdgeMix.pack(bg) & 0xFFFFFF, EdgeMix.pack(fg) & 0xFFFFFF);
            report(p, bg, fg, "window mean");

            double a0 = EdgeMix.coverage(p, bg, fg);
            System.out.printf("   growth gate: coverage %.3f %s 0.50  (growth takes only mostly-backdrop)%n",
                    a0, a0 <= 0.5 ? "<=" : ">");

            double[] nf = nearestSubject(px, isBg, band, w, h, x, y, reach);
            if (nf != null) {
                System.out.printf("   nearest    B=%06x  F=%06x%n",
                        EdgeMix.pack(bg) & 0xFFFFFF, EdgeMix.pack(nf) & 0xFFFFFF);
                report(p, bg, nf, "nearest subject");
            }
        }
    }

    private static void report(int p, double[] bg, double[] fg, String label) {
        if (!EdgeMix.separated(bg, fg)) { System.out.println("   [" + label + "] DECLINED: endpoints under a JND apart"); return; }
        double a = EdgeMix.coverage(p, bg, fg);
        int predicted = 0xFF000000
                | (EdgeMix.encode(a * fg[0] + (1 - a) * bg[0]) << 16)
                | (EdgeMix.encode(a * fg[1] + (1 - a) * bg[1]) << 8)
                | EdgeMix.encode(a * fg[2] + (1 - a) * bg[2]);
        double de = CieDe2000.of(BackgroundRemover.rgbToLab(p | 0xFF000000),
                                 BackgroundRemover.rgbToLab(predicted));
        boolean ok = EdgeMix.explains(p, bg, fg, a);
        System.out.printf("   [%s] coverage %.3f  rebuilt %06x  off by %.2f dE  -> %s%n",
                label, a, predicted & 0xFFFFFF, de, ok ? "ACCEPTED" : "DECLINED (not a mixture)");
    }

    /** Linear colour of the closest confirmed subject pixel that is not itself in the band. */
    private static double[] nearestSubject(int[] px, boolean[][] isBg, boolean[][] band,
                                           int w, int h, int cx, int cy, int radius) {
        double best = Double.MAX_VALUE;
        int bestP = 0;
        for (int y = Math.max(0, cy - radius); y <= Math.min(h - 1, cy + radius); y++) {
            for (int x = Math.max(0, cx - radius); x <= Math.min(w - 1, cx + radius); x++) {
                if (band[x][y] || isBg[x][y]) continue;
                double d = (x - cx) * (x - cx) + (y - cy) * (y - cy);
                if (d < best) { best = d; bestP = px[y * w + x]; }
            }
        }
        return best == Double.MAX_VALUE ? null : EdgeMix.linear(bestP);
    }

    private static BufferedImage toArgb(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }
}
