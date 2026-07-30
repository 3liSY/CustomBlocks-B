package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;

/**
 * MixRatioProbe — for every band pixel the edge solve DECLINES, measure the rebuild residual against
 * the separation of the two endpoints it was solving between. Robust matting judges a sample pair by
 * that RATIO rather than by an absolute residual; this prints the distribution so a bar can be read
 * off measured data instead of picked.
 *
 * Usage: java -cp <out> com.customblocks.image.MixRatioProbe <inDir>
 */
public final class MixRatioProbe {
    public static void main(String[] args) throws Exception {
        File[] files = new File(args[0]).listFiles();
        java.util.Arrays.sort(files);
        for (File f : files) {
            if (f.isDirectory() || f.getName().startsWith("5_")) continue; // the 20 MP table is too slow
            byte[] raw = CheckerboardDetector.flattenToBlack(Files.readAllBytes(f.toPath()));
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(raw));
            if (src == null) continue;
            int w = src.getWidth(), h = src.getHeight();
            BufferedImage img = toArgb(src);
            int[] px = img.getRGB(0, 0, w, h, null, 0, w);
            BgCascade.Result r = BgCascade.decide(raw, img, w, h, null);
            if (r.mask() == null) { System.out.printf("%-22s declined%n", f.getName()); continue; }
            boolean[][] isBg = r.mask();

            Class<?> u = BgRungUnmix.class;
            Method bandM = u.getDeclaredMethod("band", boolean[][].class, int.class, int.class);
            bandM.setAccessible(true);
            Method growM = u.getDeclaredMethod("grow", int[].class, boolean[][].class,
                    boolean[][].class, int.class, int.class);
            growM.setAccessible(true);
            Method solidM = u.getDeclaredMethod("solidMean", int[].class, boolean[][].class,
                    boolean[][].class, int.class, int.class, int.class, int.class, boolean.class,
                    int.class);
            solidM.setAccessible(true);

            boolean[][] band = (boolean[][]) bandM.invoke(null, isBg, w, h);
            int reach = 4 + (Integer) growM.invoke(null, px, isBg, band, w, h);

            int declined = 0, accepted = 0;
            int[] bucket = new int[11]; // ratio 0.00-0.05, .05-.10, ... , >0.50
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (!band[x][y]) continue;
                    double[] bg = (double[]) solidM.invoke(null, px, isBg, band, w, h, x, y, true, reach);
                    double[] fg = (double[]) solidM.invoke(null, px, isBg, band, w, h, x, y, false, reach);
                    if (bg == null || fg == null) continue;
                    if (!EdgeMix.separated(bg, fg)) continue;
                    int p = px[y * w + x];
                    double a = EdgeMix.coverage(p, bg, fg);
                    if (a < 0) continue;
                    if (EdgeMix.explains(p, bg, fg, a)) { accepted++; continue; }
                    declined++;
                    double res = residual(p, bg, fg, a);
                    double sep = CieDe2000.of(BackgroundRemover.rgbToLab(EdgeMix.pack(fg)),
                                              BackgroundRemover.rgbToLab(EdgeMix.pack(bg)));
                    int b = sep <= 0 ? 10 : Math.min(10, (int) (res / sep / 0.05));
                    bucket[b]++;
                }
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) sb.append(String.format(" %.2f:%d", (i + 1) * 0.05, bucket[i]));
            System.out.printf("%-22s accepted %6d  declined %6d | residual/separation ->%s  >0.50:%d%n",
                    f.getName(), accepted, declined, sb, bucket[10]);
        }
    }

    private static double residual(int p, double[] bg, double[] fg, double a) {
        int predicted = 0xFF000000
                | (EdgeMix.encode(a * fg[0] + (1 - a) * bg[0]) << 16)
                | (EdgeMix.encode(a * fg[1] + (1 - a) * bg[1]) << 8)
                | EdgeMix.encode(a * fg[2] + (1 - a) * bg[2]);
        return CieDe2000.of(BackgroundRemover.rgbToLab(p | 0xFF000000),
                            BackgroundRemover.rgbToLab(predicted));
    }

    private static BufferedImage toArgb(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }
}
