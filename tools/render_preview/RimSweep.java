package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;

/**
 * RimSweep — score the edge solve on every baseline with two numbers that mean something to the owner.
 *
 *   RIM   pixels kept FULLY opaque that sit against removed background and are still within 20 ΔE00 of
 *         the background's own colour. That is leftover backdrop baked solid — the white outline.
 *   ERASE pixels that were solidly subject (far from the background colour) and have been pushed below
 *         half coverage. That is the failure mode any loosening risks, so it is measured every run.
 *
 * Writes both a per-picture line and the alpha map, so a change can be diffed run to run.
 *
 * Usage: java -cp <out> com.customblocks.image.RimSweep <inDir> <outDir> <tag>
 */
public final class RimSweep {
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final double NEAR_BG_DE = 20.0;

    public static void main(String[] args) throws Exception {
        File[] files = new File(args[0]).listFiles();
        File outDir = new File(args[1]); outDir.mkdirs();
        String tag = args[2];
        java.util.Arrays.sort(files);
        long totalRim = 0, totalErase = 0;
        for (File f : files) {
            if (f.isDirectory() || f.getName().startsWith("5_")) continue;
            byte[] raw = CheckerboardDetector.flattenToBlack(Files.readAllBytes(f.toPath()));
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(raw));
            if (src == null) continue;
            int w = src.getWidth(), h = src.getHeight();
            BufferedImage img = toArgb(src);
            int[] px = img.getRGB(0, 0, w, h, null, 0, w);
            BgCascade.Result r = BgCascade.decide(raw, img, w, h, null);
            if (r.mask() == null) { System.out.printf("%-22s declined%n", f.getName()); continue; }
            boolean[][] isBg = r.mask();
            int[] un = r.unmixed();

            double[][] plate = BgPlate.build(px, isBg, w, h);
            int rim = 0, erase = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int i = y * w + x;
                    if (isBg[x][y]) continue;
                    int a = (un[i] >>> 24) & 0xFF;
                    if (plate == null) continue;
                    double[] pl = plate[i];
                    double de = CieDe2000.of(BackgroundRemover.rgbToLab(px[i] | 0xFF000000),
                                             BackgroundRemover.rgbToLab(EdgeMix.pack(pl)));
                    if (a == 255 && de <= NEAR_BG_DE && touchesBg(isBg, w, h, x, y)) rim++;
                    if (a < 128 && de > 2.0 * NEAR_BG_DE) erase++;
                }
            }
            totalRim += rim; totalErase += erase;
            System.out.printf("%-22s RIM %6d   ERASE %6d%n", f.getName(), rim, erase);

            BufferedImage am = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            int[] a = new int[w * h];
            for (int i = 0; i < a.length; i++) {
                int v = (un[i] >>> 24) & 0xFF;
                a[i] = (v << 16) | (v << 8) | v;
            }
            am.setRGB(0, 0, w, h, a, 0, w);
            ImageIO.write(am, "PNG", new File(outDir,
                    f.getName().replaceAll("\\.[^.]+$", "") + "__" + tag + "_alpha.png"));
        }
        System.out.printf("TOTAL  RIM %d   ERASE %d%n", totalRim, totalErase);
    }

    private static boolean touchesBg(boolean[][] isBg, int w, int h, int x, int y) {
        for (int[] d : DIRS) {
            int nx = x + d[0], ny = y + d[1];
            if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
            if (isBg[nx][ny]) return true;
        }
        return false;
    }

    private static BufferedImage toArgb(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }
}
