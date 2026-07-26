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
        File maskDir = args.length > 1 ? new File(args[1]) : null;
        if (maskDir != null) maskDir.mkdirs();
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
                if (maskDir != null) {
                    File png = new File(maskDir, strip(f.getName()) + "__rung" + r.rung() + ".png");
                    ImageIO.write(overlay(img, r.mask(), w, h), "PNG", png);
                    System.out.println("  mask -> " + png.getName());
                }
            } else if (r.namedKeyAbsent()) {
                System.out.println("  REFUSED - " + r.note());
            } else {
                System.out.println("  DECLINED - bakes unchanged, player told to use /cb bgpick");
                for (String d : r.note().split("; ")) System.out.println("    - " + d);
            }
            System.out.println("  " + ms + " ms");

            // Rung 4 standalone. The earlier, more certain rungs take almost every picture, so the
            // ensemble's own answer would otherwise never be seen on real content — only on the
            // synthetic histograms of BgThresholdsCheck. Diagnostic only; it is not what the mod bakes.
            dumpEnsembleVotes(img, w, h);
            boolean[][] r4 = BgRungEnsemble.mask(img, w, h);
            if (r4 == null) {
                System.out.println("  [rung 4 alone] no consensus");
            } else {
                String bad = BgQc.reject(r4, w, h);
                System.out.printf("  [rung 4 alone] %.1f%% coverage, QC %s%n",
                        coverage(r4, w, h), bad == null ? "pass" : "REJECT: " + bad);
                if (maskDir != null) {
                    ImageIO.write(overlay(img, r4, w, h), "PNG",
                            new File(maskDir, strip(f.getName()) + "__rung4only.png"));
                }
            }
            System.out.println();
        }
    }

    /**
     * Prints rung 4's five votes and the population of the band they span, which is what its
     * agreement test measures. Rebuilds the histogram the same way the rung does — a diagnostic copy,
     * so if the rung's own binning ever changes this must be updated alongside it.
     */
    private static void dumpEnsembleVotes(BufferedImage img, int w, int h) {
        Integer ref = BgRungKey.borderTone(img, w, h);
        if (ref == null) { System.out.println("  [rung 4 votes] no border tone"); return; }
        BgDist dist = new BgDist(BackgroundRemover.rgbToLab(ref));
        final double floor = BgRungKey.JND;
        long[] hist = new long[256];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double d = dist.of(img.getRGB(x, y));
                if (d <= floor) continue;
                int b = (int) ((d - floor) / (100.0 - floor) * 256);
                hist[Math.max(0, Math.min(255, b))]++;
            }
        }
        int[] votes = {
                BgThresholds.minError(hist), BgThresholds.triangle(hist), BgThresholds.rosin(hist),
                BgThresholds.minCrossEntropy(hist), BgThresholds.weightedObjectVariance(hist),
        };
        int lo = Integer.MAX_VALUE, hi = -1;
        for (int v : votes) if (v >= 0) { lo = Math.min(lo, v); hi = Math.max(hi, v); }
        long between = 0;
        if (hi >= 0) for (int i = lo; i <= hi; i++) between += hist[i];
        System.out.printf("  [rung 4 votes] MET %d Tri %d Rosin %d Li %d WOV %d | band %d-%d holds %d px, floor %d%n",
                votes[0], votes[1], votes[2], votes[3], votes[4], lo, hi, between, BgQc.areaFloor(w, h));
    }

    /** What the cascade decided, drawn so a human can check it: background tinted magenta, subject
     *  left alone. Magenta because nothing in the baseline set contains it, so any tinted pixel is
     *  unambiguously a mask decision rather than picture content. */
    private static BufferedImage overlay(BufferedImage img, boolean[][] mask, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out.setRGB(x, y, mask[x][y] ? 0xFF00FF : (img.getRGB(x, y) & 0xFFFFFF));
            }
        }
        return out;
    }

    private static String strip(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
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
