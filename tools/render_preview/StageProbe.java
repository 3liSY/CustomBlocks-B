package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;

/**
 * StageProbe — dump the checker kill mask at each stage of flattenToBlack, so it is visible WHICH
 * stage leaves a rim of grid alive around the artwork. Reaches the private stages by reflection so
 * production code needs no debug hook.
 */
public final class StageProbe {
    public static void main(String[] a) throws Exception {
        byte[] raw = Files.readAllBytes(new File(a[0]).toPath());
        File outDir = new File(a[1]); outDir.mkdirs();
        String name = new File(a[0]).getName().replaceAll("\\.[^.]+$", "");

        Class<?> cd = CheckerboardDetector.class;
        Method toArgb = m(cd, "toArgb", BufferedImage.class);
        Method detect = m(cd, "detect", BufferedImage.class, int.class, int.class);

        BufferedImage src = ImageIO.read(new ByteArrayInputStream(raw));
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage img = (BufferedImage) toArgb.invoke(null, src);
        Object tones = detect.invoke(null, img, w, h);
        if (tones == null) { System.out.println("not a checkerboard"); return; }
        double[] toneA = (double[]) tones.getClass().getMethod("a").invoke(tones);
        double[] toneB = (double[]) tones.getClass().getMethod("b").invoke(tones);

        int n = w * h;
        int[] argb = img.getRGB(0, 0, w, h, null, 0, w);

        Method computeKill = m(cd, "computeKill", int[].class, int.class, int.class, int.class,
                tones.getClass(), boolean[].class);
        Method coversBorder = m(cd, "coversBorder", boolean[].class, int.class, int.class);

        boolean[] kill = new boolean[n];
        CheckerLattice.Fit fit =
                (CheckerLattice.Fit) computeKill.invoke(null, argb, w, h, n, tones, kill);
        int killed = count(kill);
        dump(argb, kill, w, h, outDir, name + "_1_colour");
        boolean declare = (Boolean) coversBorder.invoke(null, kill, w, h);
        System.out.println("declare=" + declare + "  cell=" + (fit == null ? -1 : fit.cell())
                + "  colour-stage killed " + killed);

        boolean[] before = kill.clone();
        CheckerSilhouette.resolve(kill, fit, argb, w, h, toneA, toneB);
        int spared = 0;
        for (int i = 0; i < n; i++) if (before[i] && !kill[i]) spared++;
        System.out.println("silhouette spared " + spared + " px, kill now " + count(kill));
        dump(argb, kill, w, h, outDir, name + "_2_silhouette");

        if (declare) CheckerLattice.strandEnclosed(kill, fit, w, h);
        System.out.println("after strand kill " + count(kill));
        dump(argb, kill, w, h, outDir, name + "_3_strand");
    }

    private static int count(boolean[] b) {
        int c = 0; for (boolean v : b) if (v) c++; return c;
    }

    private static Method m(Class<?> c, String name, Class<?>... args) throws Exception {
        Method mm = c.getDeclaredMethod(name, args);
        mm.setAccessible(true);
        return mm;
    }

    private static void dump(int[] argb, boolean[] kill, int w, int h, File dir, String name)
            throws Exception {
        BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] px = new int[w * h];
        for (int i = 0; i < px.length; i++) px[i] = kill[i] ? 0xFF00E614 : (argb[i] | 0xFF000000);
        o.setRGB(0, 0, w, h, px, 0, w);
        ImageIO.write(o, "PNG", new File(dir, name + ".png"));
    }
}
