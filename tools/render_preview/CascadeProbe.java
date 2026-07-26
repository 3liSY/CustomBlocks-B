/**
 * CascadeProbe.java — prints which cascade rung decides each baseline picture, and why the earlier
 * rungs declined (G10 §H). A text diagnostic, not a bake: it calls the REAL BgCascade against the
 * REAL pipeline classes, so what it reports is what the mod will do.
 *
 * <p>It exists because the cascade is built one rung at a time. A golden bake cannot show progress
 * while the cascade is still unwired, but this can: run it after each rung lands and the routing is
 * visible immediately. It also prints mask coverage, which is the quickest way to spot a rung that
 * cleared the QC gate with an answer that is technically valid but obviously wrong.
 *
 * <p>It declares the pipeline's own package so it can call the package-private cascade directly. That
 * keeps the diagnostic entirely inside {@code tools/} — no debug hook is added to production code for
 * it. {@code tools/} is outside the Gradle source set, so this never ships in the jar.
 *
 * Usage (from repo root, JDK 21 on PATH):
 *   javac -d <out> src/main/java/com/customblocks/image/BackgroundRemover.java ... tools/render_preview/CascadeProbe.java
 *   java -cp <out> com.customblocks.image.CascadeProbe [inDir]
 */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

public final class CascadeProbe {

    public static void main(String[] args) throws Exception {
        File inDir = new File(args.length > 0 ? args[0] : "tools/render_preview/bg_in");
        File[] files = inDir.listFiles((d, n) -> {
            String s = n.toLowerCase();
            return s.endsWith(".png") || s.endsWith(".jpg") || s.endsWith(".jpeg");
        });
        if (files == null || files.length == 0) {
            System.out.println("No images in " + inDir.getAbsolutePath());
            return;
        }
        Arrays.sort(files);

        System.out.println("G10 SH cascade routing - " + files.length + " baseline pictures\n");

        for (File f : files) {
            byte[] raw = Files.readAllBytes(f.toPath());
            // Same pre-pass the real bake runs before the cascade sees anything (G10 §H: checkerboard
            // flattening stays ahead of rung 1, in Auto and Off alike).
            byte[] flat = CheckerboardDetector.flattenToBlack(raw);
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(flat));
            if (src == null) {
                System.out.println(f.getName() + "  UNREADABLE");
                continue;
            }
            BufferedImage img = toArgb(src);
            int w = img.getWidth(), h = img.getHeight();

            long t0 = System.nanoTime();
            BgCascade.Result r = BgCascade.decide(flat, img, w, h, null);
            long ms = (System.nanoTime() - t0) / 1_000_000L;

            System.out.println(f.getName() + "  (" + w + "x" + h + ")");
            if (r.decided()) {
                System.out.printf("  DECIDED by rung %d - %s%n", r.rung(), r.note());
                System.out.printf("  background covers %.1f%% of the picture%n", coverage(r.mask(), w, h));
            } else if (r.namedKeyAbsent()) {
                System.out.println("  REFUSED - " + r.note());
            } else {
                System.out.println("  DECLINED - bakes unchanged, player told to use /cb bgpick");
                for (String d : r.note().split("; ")) System.out.println("    - " + d);
            }
            System.out.println("  " + ms + " ms\n");
        }
    }

    private static double coverage(boolean[][] mask, int w, int h) {
        long n = 0;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) if (mask[x][y]) n++;
        }
        return 100.0 * n / ((long) w * h);
    }

    private static BufferedImage toArgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }
}
