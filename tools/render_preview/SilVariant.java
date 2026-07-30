package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;

/**
 * SilVariant — run CheckerSilhouette's resolve with the SUBJECT seeding varied, and report how much
 * grid each variant spares on every checkerboard baseline. A copy of the real method, because the
 * point is to measure a change before it is made.
 *
 *   v0  as shipped: every pixel with no grid tone seeds SUBJECT.
 *   v1  a pixel with no grid tone seeds SUBJECT only when the colour stage did NOT kill it.
 */
public final class SilVariant {

    private static final double SOLID_TOL_DE = 6.0;
    private static final int SMOOTH_PASSES = 3;

    public static void main(String[] a) throws Exception {
        File inDir = new File(a[0]), outDir = new File(a[1]);
        outDir.mkdirs();
        Class<?> cd = CheckerboardDetector.class;
        Method toArgb = m(cd, "toArgb", BufferedImage.class);
        Method detect = m(cd, "detect", BufferedImage.class, int.class, int.class);

        File[] files = inDir.listFiles();
        java.util.Arrays.sort(files);
        for (File f : files) {
            if (f.isDirectory()) continue;
            byte[] raw = Files.readAllBytes(f.toPath());
            BufferedImage src;
            try { src = ImageIO.read(new ByteArrayInputStream(raw)); } catch (Exception e) { continue; }
            if (src == null) continue;
            int w = src.getWidth(), h = src.getHeight(), n = w * h;
            BufferedImage img = (BufferedImage) toArgb.invoke(null, src);
            Object tones = detect.invoke(null, img, w, h);
            if (tones == null) continue;
            double[] tA = (double[]) tones.getClass().getMethod("a").invoke(tones);
            double[] tB = (double[]) tones.getClass().getMethod("b").invoke(tones);
            int[] argb = img.getRGB(0, 0, w, h, null, 0, w);
            Method computeKill = m(cd, "computeKill", int[].class, int.class, int.class, int.class,
                    tones.getClass(), boolean[].class);

            boolean[] base = new boolean[n];
            CheckerLattice.Fit fit =
                    (CheckerLattice.Fit) computeKill.invoke(null, argb, w, h, n, tones, base);

            int noToneKilled = 0;
            if (fit != null) {
                int[] tone = fit.tone();
                for (int i = 0; i < n; i++) if (base[i] && tone[i] < 0) noToneKilled++;
            }

            boolean[] v0 = base.clone();
            resolve(v0, fit, argb, w, h, tA, tB, false);
            boolean[] v1 = base.clone();
            resolve(v1, fit, argb, w, h, tA, tB, true);

            // Attribute every spared pixel to the stage that spared it.
            attribute(base, fit, argb, w, h, tA, tB, f.getName());

            System.out.printf("%-22s killed %7d | no-tone killed %6d | spared v0 %6d  v2 %6d%n",
                    f.getName(), count(base), noToneKilled, diff(base, v0), diff(base, v1));

            write(outDir, f, "_v2", argb, base, v1, w, h);
        }
    }

    private static void write(File dir, File f, String tag, int[] argb,
                              boolean[] base, boolean[] after, int w, int h) throws Exception {
        BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] px = new int[w * h];
        for (int i = 0; i < px.length; i++) {
            px[i] = (base[i] && !after[i]) ? 0xFFFF0000
                    : (after[i] ? 0xFF00E614 : (argb[i] | 0xFF000000));
        }
        o.setRGB(0, 0, w, h, px, 0, w);
        ImageIO.write(o, "PNG", new File(dir, f.getName().replaceAll("\\.[^.]+$", "") + tag + ".png"));
    }

    /**
     * Which stage spares the grid: the seeding loop's own verdict, the chamfer fill of the undecided,
     * or the boundary smoothing. Run three times over the same seeds, each time stopping one stage
     * later, and report the running total.
     */
    private static void attribute(boolean[] base, CheckerLattice.Fit fit, int[] argb, int w, int h,
                                  double[] tA, double[] tB, String name) {
        boolean[] seedOnly = base.clone();
        resolveStages(seedOnly, fit, argb, w, h, tA, tB, false, false);
        boolean[] chamfer = base.clone();
        resolveStages(chamfer, fit, argb, w, h, tA, tB, true, false);
        boolean[] full = base.clone();
        resolveStages(full, fit, argb, w, h, tA, tB, true, true);
        System.out.printf("   stage split: seeds %6d | +chamfer %6d | +smooth %6d%n",
                diff(base, seedOnly), diff(base, chamfer), diff(base, full));

        // What did the seeding loop actually SEE at each pixel the repair went on to spare?
        int[] tone = fit.tone();
        int cell = fit.cell();
        int n = w * h;
        boolean[] solid = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (tone[i] < 0) continue;
            double[] lab = CheckerboardDetector.labOf(argb[i]);
            solid[i] = Math.min(CheckerboardDetector.de(lab, tA),
                                CheckerboardDetector.de(lab, tB)) <= SOLID_TOL_DE;
        }
        java.util.Map<String, Integer> hist = new java.util.TreeMap<>();
        for (int i = 0; i < n; i++) {
            if (!base[i] || full[i]) continue; // only the pixels the repair spared
            int x = i % w, y = i / w;
            String key;
            if (tone[i] < 0) key = "no tone";
            else if (!solid[i]) key = "not solid (a mixture)";
            else key = "x=" + axisVote(tone, solid, w, h, x, y, cell, 0, tone[i], false)
                    + " y=" + axisVote(tone, solid, w, h, x, y, cell, 1, tone[i], false);
            hist.merge(key, 1, Integer::sum);
        }
        for (var e : hist.entrySet()) System.out.printf("      %-22s %6d%n", e.getKey(), e.getValue());
    }

    private static int diff(boolean[] a, boolean[] b) {
        int c = 0; for (int i = 0; i < a.length; i++) if (a[i] && !b[i]) c++; return c;
    }

    private static int count(boolean[] b) { int c = 0; for (boolean v : b) if (v) c++; return c; }

    private static Method m(Class<?> c, String name, Class<?>... args) throws Exception {
        Method mm = c.getDeclaredMethod(name, args); mm.setAccessible(true); return mm;
    }

    /** Same as resolve, but able to stop before the chamfer fill or before the smoothing. */
    static void resolveStages(boolean[] kill, CheckerLattice.Fit fit, int[] argb, int w, int h,
                              double[] toneA, double[] toneB, boolean doChamfer, boolean doSmooth) {
        if (fit == null) return;
        final int[] tone = fit.tone();
        final int cell = fit.cell();
        final int n = w * h;

        boolean[] solid = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (tone[i] < 0) continue;
            double[] lab = CheckerboardDetector.labOf(argb[i]);
            solid[i] = Math.min(CheckerboardDetector.de(lab, toneA),
                                CheckerboardDetector.de(lab, toneB)) <= SOLID_TOL_DE;
        }
        final byte UNKNOWN = 0, GRID = 1, SUBJECT = 2;
        byte[] label = new byte[n];
        int tail = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (tone[i] < 0) { label[i] = SUBJECT; tail++; continue; }
                if (!solid[i]) continue;
                int t = tone[i];
                int xv = axisVote(tone, solid, w, h, x, y, cell, 0, t, false);
                int yv = axisVote(tone, solid, w, h, x, y, cell, 1, t, false);
                if (xv > 0 && yv > 0) { label[i] = GRID; tail++; }
                else if (xv < 0 && yv < 0) { label[i] = SUBJECT; tail++; }
            }
        }
        if (tail == 0 || tail == n) return;
        boolean[] decided = new boolean[n];
        for (int i = 0; i < n; i++) decided[i] = label[i] == GRID || tone[i] < 0;

        if (doChamfer) {
            final int D_ORTH = 5, D_DIAG = 7, D_KNIGHT = 11;
            int[] dist = new int[n];
            java.util.Arrays.fill(dist, Integer.MAX_VALUE / 4);
            for (int i = 0; i < n; i++) if (label[i] != UNKNOWN) dist[i] = 0;
            for (int pass = 0; pass < 2; pass++) {
                int from = pass == 0 ? 0 : n - 1;
                int to = pass == 0 ? n : -1;
                int step = pass == 0 ? 1 : -1;
                for (int i = from; i != to; i += step) {
                    int x = i % w, y = i / w;
                    int best = dist[i];
                    byte bl = label[i];
                    for (int[] o : pass == 0 ? FORWARD : BACKWARD) {
                        int nx = x + o[0], ny = y + o[1];
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        int j = ny * w + nx;
                        int cost = o[2] == 1 ? D_ORTH : (o[2] == 2 ? D_DIAG : D_KNIGHT);
                        if (dist[j] + cost < best) { best = dist[j] + cost; bl = label[j]; }
                    }
                    dist[i] = best;
                    label[i] = bl;
                }
            }
        }
        if (doSmooth) smooth(label, decided, w, h, cell, GRID, SUBJECT);
        for (int i = 0; i < n; i++) if (kill[i] && label[i] != GRID) kill[i] = false;
    }

    // ── copy of CheckerSilhouette.resolve, with the one seeding line switched ────────────────────────
    static void resolve(boolean[] kill, CheckerLattice.Fit fit, int[] argb, int w, int h,
                        double[] toneA, double[] toneB, boolean respectKill) {
        if (fit == null) return;
        final int[] tone = fit.tone();
        final int cell = fit.cell();
        final int n = w * h;

        boolean[] solid = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (tone[i] < 0) continue;
            double[] lab = CheckerboardDetector.labOf(argb[i]);
            solid[i] = Math.min(CheckerboardDetector.de(lab, toneA),
                                CheckerboardDetector.de(lab, toneB)) <= SOLID_TOL_DE;
        }

        final byte UNKNOWN = 0, GRID = 1, SUBJECT = 2;
        byte[] label = new byte[n];
        int tail = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (tone[i] < 0) { label[i] = SUBJECT; tail++; continue; }
                if (!solid[i]) continue;
                int t = tone[i];
                int xv = axisVote(tone, solid, w, h, x, y, cell, 0, t, false);
                int yv = axisVote(tone, solid, w, h, x, y, cell, 1, t, false);
                boolean isGrid = respectKill
                        ? ((xv > 0 && yv >= 0) || (xv >= 0 && yv > 0)) // silence does not veto a beat
                        : (xv > 0 && yv > 0);
                if (isGrid) { label[i] = GRID; tail++; }
                else if (xv < 0 && yv < 0) { label[i] = SUBJECT; tail++; }
            }
        }
        if (tail == 0 || tail == n) return;
        boolean[] decided = new boolean[n];
        for (int i = 0; i < n; i++) decided[i] = label[i] == GRID || tone[i] < 0;

        final int D_ORTH = 5, D_DIAG = 7, D_KNIGHT = 11;
        int[] dist = new int[n];
        java.util.Arrays.fill(dist, Integer.MAX_VALUE / 4);
        for (int i = 0; i < n; i++) if (label[i] != UNKNOWN) dist[i] = 0;

        for (int pass = 0; pass < 2; pass++) {
            int from = pass == 0 ? 0 : n - 1;
            int to = pass == 0 ? n : -1;
            int step = pass == 0 ? 1 : -1;
            for (int i = from; i != to; i += step) {
                int x = i % w, y = i / w;
                int best = dist[i];
                byte bl = label[i];
                for (int[] o : pass == 0 ? FORWARD : BACKWARD) {
                    int nx = x + o[0], ny = y + o[1];
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    int j = ny * w + nx;
                    int cost = o[2] == 1 ? D_ORTH : (o[2] == 2 ? D_DIAG : D_KNIGHT);
                    if (dist[j] + cost < best) { best = dist[j] + cost; bl = label[j]; }
                }
                dist[i] = best;
                label[i] = bl;
            }
        }

        smooth(label, decided, w, h, cell, GRID, SUBJECT);

        for (int i = 0; i < n; i++) if (kill[i] && label[i] != GRID) kill[i] = false;
    }

    private static void smooth(byte[] label, boolean[] decided, int w, int h, int cell,
                               byte grid, byte subject) {
        final int r = Math.max(1, cell / 2);
        final int area = (2 * r + 1) * (2 * r + 1);
        int[] sum = new int[(w + 1) * (h + 1)];
        for (int pass = 0; pass < SMOOTH_PASSES; pass++) {
            java.util.Arrays.fill(sum, 0);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int p = (y + 1) * (w + 1) + (x + 1);
                    sum[p] = (label[y * w + x] == grid ? 1 : 0)
                            + sum[p - 1] + sum[p - (w + 1)] - sum[p - (w + 1) - 1];
                }
            }
            boolean moved = false;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int i = y * w + x;
                    if (decided[i]) continue;
                    int x0 = Math.max(0, x - r), x1 = Math.min(w - 1, x + r);
                    int y0 = Math.max(0, y - r), y1 = Math.min(h - 1, y + r);
                    int inBox = (x1 - x0 + 1) * (y1 - y0 + 1);
                    int gridCount = CheckerLattice.window(sum, w, x0, y0, x1, y1) + (area - inBox) / 2;
                    byte want = gridCount * 2 > area ? grid : subject;
                    if (label[i] != want) { label[i] = want; moved = true; }
                }
            }
            if (!moved) break;
        }
    }

    private static final int[][] FORWARD = {
            {-1, 0, 1}, {0, -1, 1}, {-1, -1, 2}, {1, -1, 2},
            {-2, -1, 3}, {-1, -2, 3}, {1, -2, 3}, {2, -1, 3}};
    private static final int[][] BACKWARD = {
            {1, 0, 1}, {0, 1, 1}, {1, 1, 2}, {-1, 1, 2},
            {2, 1, 3}, {1, 2, 3}, {-1, 2, 3}, {-2, 1, 3}};

    private static int axisVote(int[] tone, boolean[] solid, int w, int h,
                                int x, int y, int cell, int axis, int t, boolean sameIsSilence) {
        boolean beats = false, sawSolid = false;
        for (int sign = -1; sign <= 1; sign += 2) {
            int opposite = 0, same = 0;
            for (int d = cell - 1; d <= cell + 1; d++) {
                int j = sample(w, h, x, y, axis, sign * d);
                if (j < 0 || !solid[j]) continue;
                sawSolid = true;
                if (tone[j] == t) same++; else opposite++;
            }
            if (sameIsSilence ? opposite == 0 : opposite <= same) continue;
            int far = 0, farOff = 0;
            for (int d = 2 * cell - 2; d <= 2 * cell + 2; d++) {
                int j = sample(w, h, x, y, axis, sign * d);
                if (j < 0 || !solid[j]) continue;
                if (tone[j] == t) far++; else farOff++;
            }
            if (far >= farOff) beats = true;
        }
        if (beats) return 1;
        return sawSolid ? -1 : 0;
    }

    private static int sample(int w, int h, int x, int y, int axis, int d) {
        int nx = axis == 0 ? x + d : x, ny = axis == 0 ? y : y + d;
        if (nx < 0 || ny < 0 || nx >= w || ny >= h) return -1;
        return ny * w + nx;
    }
}
