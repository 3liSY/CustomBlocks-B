package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;

/**
 * FeatherWhy — for a checkerboard preview, find the pixels that still bake as a pale outline and say
 * why the GRID FEATHER did not solve them. The feather is the only stage that can: it runs while the
 * grid's own tone is still in the picture, and everything after it sees that tone already painted out.
 *
 * Usage: java -cp <out> com.customblocks.image.FeatherWhy <src> [x,y ...]
 */
public final class FeatherWhy {
    private static final int FEATHER_R = 3;
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public static void main(String[] args) throws Exception {
        byte[] raw = Files.readAllBytes(new File(args[0]).toPath());
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(raw));
        int w = src.getWidth(), h = src.getHeight(), n = w * h;
        Class<?> cd = CheckerboardDetector.class;
        Method toArgb = m(cd, "toArgb", BufferedImage.class);
        Method detect = m(cd, "detect", BufferedImage.class, int.class, int.class);
        BufferedImage img = (BufferedImage) toArgb.invoke(null, src);
        Object tones = detect.invoke(null, img, w, h);
        if (tones == null) { System.out.println("not a checkerboard"); return; }
        double[] tA = (double[]) tones.getClass().getMethod("a").invoke(tones);
        double[] tB = (double[]) tones.getClass().getMethod("b").invoke(tones);
        int[] argb = img.getRGB(0, 0, w, h, null, 0, w);
        Method computeKill = m(cd, "computeKill", int[].class, int.class, int.class, int.class,
                tones.getClass(), boolean[].class);
        boolean[] kill = new boolean[n];
        CheckerLattice.Fit fit = (CheckerLattice.Fit) computeKill.invoke(null, argb, w, h, n, tones, kill);
        CheckerSilhouette.resolve(kill, fit, argb, w, h, tA, tB);

        SRC = argb;
        int[] dist = bfs(kill, w, h, n, true, null);
        int[] near = lastNear;
        int[] adist = bfs(kill, w, h, n, false, dist);
        int[] art = lastNear;

        java.util.TreeMap<String, Integer> why = new java.util.TreeMap<>();
        for (int i = 0; i < n; i++) {
            if (kill[i]) continue;
            int p = argb[i];
            int r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
            if (Math.min(r, Math.min(g, b)) < 90) continue;    // not a pale pixel
            if (!touchesKill(kill, w, h, i)) continue;          // not on the grid boundary
            why.merge(reason(argb, kill, dist, near, art, i), 1, Integer::sum);
        }
        System.out.println("pale boundary pixels by reason:");
        for (var e : why.entrySet()) System.out.printf("   %-46s %5d%n", e.getKey(), e.getValue());

        for (int k = 1; k < args.length; k++) {
            String[] q = args[k].split(",");
            int x = Integer.parseInt(q[0]), y = Integer.parseInt(q[1]), i = y * w + x;
            System.out.printf("%n(%d,%d) rgb=%06x kill=%b  distToGrid=%s  gridTone=%s  artwork=%s  -> %s%n",
                    x, y, argb[i] & 0xFFFFFF, kill[i],
                    dist[i] == Integer.MAX_VALUE ? "none" : String.valueOf(dist[i]),
                    near[i] == -1 ? "none" : String.format("%06x", near[i] & 0xFFFFFF),
                    art[i] == -1 ? "none" : String.format("%06x", art[i] & 0xFFFFFF),
                    reason(argb, kill, dist, near, art, i));
        }
    }

    private static String reason(int[] argb, boolean[] kill, int[] dist, int[] near, int[] art, int i) {
        if (dist[i] > FEATHER_R) return "1 further from the grid than the feather reaches";
        if (near[i] == -1) return "2 no grid tone carried to it";
        if (art[i] == -1) return "3 no unmixed artwork carried to it";
        double[] bg = EdgeMix.linear(near[i]);
        double[] fg = EdgeMix.linear(art[i]);
        if (!EdgeMix.separated(bg, fg)) return "4 grid tone and artwork are the same colour";
        double a = EdgeMix.coverage(argb[i], bg, fg);
        if (a < 0) return "5 endpoints coincide";
        if (!EdgeMix.explains(argb[i], bg, fg, a)) return "6 mixture test refused";
        if (a >= 0.999) return "7 solved as fully artwork";
        return "8 solved and softened";
    }

    private static boolean touchesKill(boolean[] kill, int w, int h, int i) {
        int x = i % w, y = i / w;
        for (int[] d : DIRS) {
            int nx = x + d[0], ny = y + d[1];
            if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
            if (kill[ny * w + nx]) return true;
        }
        return false;
    }

    private static int[] lastNear;

    /** Mirrors the two BFS passes in CheckerboardDetector.feather. */
    private static int[] bfs(boolean[] kill, int w, int h, int n, boolean fromKill, int[] gridDist) {
        int[] dist = new int[n];
        java.util.Arrays.fill(dist, Integer.MAX_VALUE);
        int[] near = new int[n];
        java.util.Arrays.fill(near, -1);
        int[] q = new int[n];
        int head = 0, tail = 0;
        for (int i = 0; i < n; i++) {
            boolean seed = fromKill ? kill[i] : (!kill[i] && gridDist[i] > FEATHER_R);
            if (seed) { dist[i] = 0; near[i] = SRC[i]; q[tail++] = i; }
        }
        while (head < tail) {
            int idx = q[head++];
            if (dist[idx] >= FEATHER_R) continue;
            int x = idx % w, y = idx / w, nd = dist[idx] + 1, nc = near[idx];
            if (x + 1 < w && dist[idx + 1] > nd) { dist[idx + 1] = nd; near[idx + 1] = nc; q[tail++] = idx + 1; }
            if (x > 0     && dist[idx - 1] > nd) { dist[idx - 1] = nd; near[idx - 1] = nc; q[tail++] = idx - 1; }
            if (y + 1 < h && dist[idx + w] > nd) { dist[idx + w] = nd; near[idx + w] = nc; q[tail++] = idx + w; }
            if (y > 0     && dist[idx - w] > nd) { dist[idx - w] = nd; near[idx - w] = nc; q[tail++] = idx - w; }
        }
        lastNear = near;
        return dist;
    }

    static int[] SRC;

    private static Method m(Class<?> c, String name, Class<?>... a) throws Exception {
        Method mm = c.getDeclaredMethod(name, a); mm.setAccessible(true); return mm;
    }
}
