package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;

/**
 * RimWhy — find every pixel that still bakes as leftover backdrop (kept fully opaque, sitting against
 * removed background, still close to the background's own colour) and report WHY the edge solve did
 * not soften it, grouped by reason. One run says whether what is left is a band that never reached the
 * pixel, endpoints it could not find, or a mixture test that refused.
 *
 * Usage: java -cp <out> com.customblocks.image.RimWhy <inDir> [pictureNamePrefix]
 */
public final class RimWhy {
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final double NEAR_BG_DE = 20.0;

    public static void main(String[] args) throws Exception {
        File[] files = new File(args[0]).listFiles();
        java.util.Arrays.sort(files);
        String only = args.length > 1 ? args[1] : null;
        for (File f : files) {
            if (f.isDirectory() || f.getName().startsWith("5_")) continue;
            if (only != null && !f.getName().startsWith(only)) continue;
            byte[] raw = CheckerboardDetector.flattenToBlack(Files.readAllBytes(f.toPath()));
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(raw));
            if (src == null) continue;
            int w = src.getWidth(), h = src.getHeight();
            BufferedImage img = toArgb(src);
            int[] px = img.getRGB(0, 0, w, h, null, 0, w);
            BgCascade.Result r = BgCascade.decide(raw, img, w, h, null);
            if (r.mask() == null) continue;
            boolean[][] isBg = r.mask();
            int[] un = r.unmixed();
            double[][] plate = BgPlate.build(px, isBg, w, h);
            if (plate == null) continue;

            Class<?> u = BgRungUnmix.class;
            Method bandM = u.getDeclaredMethod("band", boolean[][].class, int.class, int.class);
            bandM.setAccessible(true);
            Method growM = u.getDeclaredMethod("grow", int[].class, boolean[][].class,
                    boolean[][].class, double[][].class, int.class, int.class);
            growM.setAccessible(true);
            Method solidM = u.getDeclaredMethod("solidMean", int[].class, boolean[][].class,
                    boolean[][].class, int.class, int.class, int.class, int.class, boolean.class,
                    int.class);
            solidM.setAccessible(true);
            boolean[][] band = (boolean[][]) bandM.invoke(null, isBg, w, h);
            int reach = 4 + (Integer) growM.invoke(null, px, isBg, band, plate, w, h);

            java.util.TreeMap<String, Integer> why = new java.util.TreeMap<>();
            String sample = "";
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int i = y * w + x;
                    if (isBg[x][y]) continue;
                    if (((un[i] >>> 24) & 0xFF) != 255) continue;
                    double de = CieDe2000.of(BackgroundRemover.rgbToLab(px[i] | 0xFF000000),
                                             BackgroundRemover.rgbToLab(EdgeMix.pack(plate[i])));
                    if (de > NEAR_BG_DE || !touchesBg(isBg, w, h, x, y)) continue;

                    String reason;
                    if (!band[x][y]) reason = "1 never in the band";
                    else {
                        double[] bg = (double[]) solidM.invoke(null, px, isBg, band, w, h, x, y, true, reach);
                        if (bg == null) bg = plate[i];
                        double[] fg = (double[]) solidM.invoke(null, px, isBg, band, w, h, x, y, false, reach);
                        if (bg == null) reason = "2 no confirmed background within reach";
                        else if (fg == null) reason = "3 no confirmed subject within reach";
                        else if (!EdgeMix.separated(bg, fg)) reason = "4 the two sides are the same colour";
                        else {
                            double a = EdgeMix.coverage(px[i], bg, fg);
                            if (a < 0) reason = "5 endpoints coincide";
                            else if (!EdgeMix.explains(px[i], bg, fg, a)) reason = "6 mixture test refused";
                            else reason = "7 solved, but coverage came out full";
                        }
                    }
                    why.merge(reason, 1, Integer::sum);
                    if (sample.isEmpty() && (reason.startsWith("7") || reason.startsWith("2")))
                        sample = " e.g. (" + x + "," + y + ") reason " + reason.charAt(0);
                }
            }
            int total = why.values().stream().mapToInt(Integer::intValue).sum();
            System.out.printf("%-22s leftover backdrop %5d%s%n", f.getName(), total, sample);
            for (var e : why.entrySet()) System.out.printf("      %-42s %5d%n", e.getKey(), e.getValue());
        }
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
