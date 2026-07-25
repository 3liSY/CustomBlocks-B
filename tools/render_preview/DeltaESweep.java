/**
 * DeltaESweep.java — derives the CIEDE2000 replacement for MAX_DELTA_E (G10 §H Jar A).
 *
 * The old pipeline maps player strength 0-100 linearly onto CIE76 ΔE [0, 22]; the default
 * strength 30 therefore classifies "background" at CIE76 ≤ 6.6. CIEDE2000 sits on a different
 * numeric scale (it compresses saturated distances and stretches near-neutral ones), so 22 cannot
 * be carried over. This tool derives the replacement the only defensible way available while the
 * knob still exists: for every opaque pixel of every baseline picture, compute BOTH distances to
 * the corner-sampled background colour, then find the ΔE00 threshold whose background/subject
 * split agrees best with the old default's split. The chosen MAX is that threshold ÷ 0.30, so the
 * default strength keeps meaning the same thing and every other strength scales around it.
 *
 * Corner sampling and the Lab conversion are copied verbatim from BackgroundRemover (they are
 * private there); this is one-shot derivation tooling, not a second implementation to maintain.
 *
 * Run (from the repo root, after compiling BgGoldenPreview's set plus CieDe2000):
 *   java -cp tools/render_preview/out DeltaESweep tools/render_preview/bg_in
 */
import com.customblocks.image.CieDe2000;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DeltaESweep {

    private static final double T76 = 30.0 / 100.0 * 22.0; // the old default's CIE76 threshold, 6.6
    private static final double T00_MIN = 0.5, T00_MAX = 15.0, T00_STEP = 0.05;

    public static void main(String[] args) throws Exception {
        File inDir = new File(args.length > 0 ? args[0] : "tools/render_preview/bg_in");
        File[] files = inDir.listFiles((d, n) -> {
            String s = n.toLowerCase();
            return s.endsWith(".png") || s.endsWith(".jpg") || s.endsWith(".jpeg");
        });
        if (files == null || files.length == 0) { System.out.println("No images in " + inDir); return; }
        Arrays.sort(files);

        int steps = (int) Math.round((T00_MAX - T00_MIN) / T00_STEP) + 1;
        long[] totalDisagree = new long[steps];
        long totalOpaque = 0;

        System.out.println("picture                      opaque px   bg76(px)   best T00   disagree at best");
        System.out.println("--------------------------------------------------------------------------------");
        for (File f : files) {
            BufferedImage src = ImageIO.read(f);
            if (src == null) { System.out.println(f.getName() + "  unreadable"); continue; }
            BufferedImage img = toArgb(src);
            int w = img.getWidth(), h = img.getHeight();
            int bg = sampleCornerBackground(img, w, h);
            if (((bg >>> 24) & 0xFF) < 128) {
                // Transparent background: classification is alpha-driven, metric plays no part.
                System.out.printf("%-28s (transparent bg — skipped, metric not involved)%n", f.getName());
                continue;
            }
            double[] bgLab = rgbToLab(bg);

            long opaque = 0, bg76 = 0;
            long[] disagree = new long[steps];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int px = img.getRGB(x, y);
                    if (((px >>> 24) & 0xFF) < 128) continue; // transparent → bg under both metrics
                    opaque++;
                    double[] lab = rgbToLab(px);
                    double d76 = cie76(lab, bgLab);
                    double d00 = CieDe2000.of(lab, bgLab);
                    boolean was = d76 <= T76;
                    if (was) bg76++;
                    // A pixel disagrees at threshold t when (d00 <= t) != was. d00 crosses that
                    // boundary once, so count range edges instead of testing every t per pixel.
                    int cross = (int) Math.ceil((d00 - T00_MIN) / T00_STEP); // first step index with t >= d00
                    if (cross < 0) cross = 0;
                    if (was) {
                        // disagrees for every t below d00 → steps [0, cross)
                        if (cross > 0) { disagree[0]++; if (cross < steps) disagree[cross]--; }
                    } else {
                        // disagrees for every t at/above d00 → steps [cross, end)
                        if (cross < steps) disagree[cross]++;
                    }
                }
            }
            // prefix-sum the range marks into absolute counts
            long run = 0;
            int bestI = 0; long bestV = Long.MAX_VALUE;
            for (int i = 0; i < steps; i++) {
                run += disagree[i];
                disagree[i] = run;
                totalDisagree[i] += run;
                if (run < bestV) { bestV = run; bestI = i; }
            }
            totalOpaque += opaque;
            System.out.printf("%-28s %-11d %-10d %-10.2f %d (%.3f%%)%n",
                    f.getName(), opaque, bg76, T00_MIN + bestI * T00_STEP, bestV, 100.0 * bestV / opaque);
        }

        int bestI = 0; long bestV = Long.MAX_VALUE;
        for (int i = 0; i < steps; i++) {
            if (totalDisagree[i] < bestV) { bestV = totalDisagree[i]; bestI = i; }
        }
        double bestT = T00_MIN + bestI * T00_STEP;
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("combined best T00 = %.2f  (disagree %d of %d opaque px, %.4f%%)%n",
                bestT, bestV, totalOpaque, 100.0 * bestV / totalOpaque);
        System.out.printf("=> MAX_DELTA_E(2000) = T00 / 0.30 = %.2f%n", bestT / 0.30);
        // Show the plateau around the optimum so the chosen value is visibly stable, not a spike.
        System.out.println("\nplateau (t : disagree%):");
        for (int i = Math.max(0, bestI - 10); i <= Math.min(steps - 1, bestI + 10); i += 2) {
            System.out.printf("  %5.2f : %.4f%%%n", T00_MIN + i * T00_STEP, 100.0 * totalDisagree[i] / totalOpaque);
        }
    }

    // ── copied verbatim from BackgroundRemover (private there) ──────────────────────────────

    private static int sampleCornerBackground(BufferedImage img, int w, int h) {
        List<Integer> samples = new ArrayList<>();
        int[][] corners = {{0, 0}, {Math.max(0, w - 3), 0}, {0, Math.max(0, h - 3)}, {Math.max(0, w - 3), Math.max(0, h - 3)}};
        for (int[] c : corners) {
            for (int dx = 0; dx < 3 && c[0] + dx < w; dx++) {
                for (int dy = 0; dy < 3 && c[1] + dy < h; dy++) {
                    samples.add(img.getRGB(c[0] + dx, c[1] + dy));
                }
            }
        }
        Collections.sort(samples);
        return samples.get(samples.size() / 2);
    }

    private static BufferedImage toArgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    private static double[] rgbToLab(int argb) {
        double rF = ((argb >> 16) & 0xFF) / 255.0;
        double gF = ((argb >> 8)  & 0xFF) / 255.0;
        double bF = ( argb        & 0xFF) / 255.0;

        rF = (rF > 0.04045) ? Math.pow((rF + 0.055) / 1.055, 2.4) : (rF / 12.92);
        gF = (gF > 0.04045) ? Math.pow((gF + 0.055) / 1.055, 2.4) : (gF / 12.92);
        bF = (bF > 0.04045) ? Math.pow((bF + 0.055) / 1.055, 2.4) : (bF / 12.92);

        rF *= 100.0; gF *= 100.0; bF *= 100.0;

        double x = rF * 0.4124 + gF * 0.3576 + bF * 0.1805;
        double y = rF * 0.2126 + gF * 0.7152 + bF * 0.0722;
        double z = rF * 0.0193 + gF * 0.1192 + bF * 0.9505;

        x /= 95.047; y /= 100.000; z /= 108.883;

        x = (x > 0.008856) ? Math.cbrt(x) : (7.787 * x) + (16.0 / 116.0);
        y = (y > 0.008856) ? Math.cbrt(y) : (7.787 * y) + (16.0 / 116.0);
        z = (z > 0.008856) ? Math.cbrt(z) : (7.787 * z) + (16.0 / 116.0);

        return new double[]{(116.0 * y) - 16.0, 500.0 * (x - y), 200.0 * (y - z)};
    }

    private static double cie76(double[] a, double[] b) {
        return Math.sqrt(Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2) + Math.pow(a[2] - b[2], 2));
    }
}
