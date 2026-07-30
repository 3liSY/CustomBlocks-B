package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;

/**
 * SilAb — for every checkerboard baseline, how much grid does CheckerSilhouette.resolve SPARE, and
 * where. Writes an overlay marking only the spared pixels, so "did the repair help or hurt" is a
 * picture and a count rather than an opinion.
 */
public final class SilAb {
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
            if (tones == null) { System.out.printf("%-24s not a checkerboard%n", f.getName()); continue; }
            double[] tA = (double[]) tones.getClass().getMethod("a").invoke(tones);
            double[] tB = (double[]) tones.getClass().getMethod("b").invoke(tones);
            int[] argb = img.getRGB(0, 0, w, h, null, 0, w);

            Method computeKill = m(cd, "computeKill", int[].class, int.class, int.class, int.class,
                    tones.getClass(), boolean[].class);
            boolean[] kill = new boolean[n];
            CheckerLattice.Fit fit =
                    (CheckerLattice.Fit) computeKill.invoke(null, argb, w, h, n, tones, kill);
            boolean[] before = kill.clone();
            CheckerSilhouette.resolve(kill, fit, argb, w, h, tA, tB);
            int spared = 0;
            for (int i = 0; i < n; i++) if (before[i] && !kill[i]) spared++;
            System.out.printf("%-24s killed %7d  spared %7d  (%.2f%% of picture)%n",
                    f.getName(), count(before), spared, 100.0 * spared / n);
            if (spared == 0) continue;
            BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            int[] px = new int[n];
            for (int i = 0; i < n; i++) {
                px[i] = (before[i] && !kill[i]) ? 0xFFFF0000
                        : (kill[i] ? 0xFF00E614 : (argb[i] | 0xFF000000));
            }
            o.setRGB(0, 0, w, h, px, 0, w);
            ImageIO.write(o, "PNG", new File(outDir,
                    f.getName().replaceAll("\\.[^.]+$", "") + "_spared.png"));
        }
    }

    private static int count(boolean[] b) { int c = 0; for (boolean v : b) if (v) c++; return c; }

    private static Method m(Class<?> c, String name, Class<?>... args) throws Exception {
        Method mm = c.getDeclaredMethod(name, args);
        mm.setAccessible(true);
        return mm;
    }
}
