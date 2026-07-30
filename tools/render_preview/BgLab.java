/**
 * BgLab.java — G10 §H working bench for the background pipeline (tools only, never shipped).
 *
 * For every picture in the input folder it runs the REAL colour-variant rail — the path the owner
 * tests in game — and writes three files:
 *
 *   <pic>__variant.png  the bake, on the variant fill colour (green), exactly as the block shows it
 *   <pic>__mask.png     the accepted background mask (magenta = background) at source resolution
 *   <pic>__edge.png     a 4x zoom of the busiest edge window, so a 1 px rim is actually visible
 *
 * and prints the deciding rung, its note and the mask coverage.
 *
 * Usage (repo root, JDK 21):
 *   javac -d tools/render_preview/out_lab -sourcepath src/main/java \
 *         tools/render_preview/BgLab.java
 *   java -cp tools/render_preview/out_lab com.customblocks.image.BgLab <inDir> <outDir>
 */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

public final class BgLab {

    /** The green the owner's variant blocks use in the screenshots. */
    private static final int FILL = 0x22CC22;
    private static final int SIZE = 512;

    public static void main(String[] args) throws Exception {
        File inDir = new File(args.length > 0 ? args[0] : "tools/render_preview/bg_in");
        File outDir = new File(args.length > 1 ? args[1] : "tools/render_preview/bg_lab");
        outDir.mkdirs();
        File[] files = inDir.listFiles((d, n) -> {
            String s = n.toLowerCase();
            return s.endsWith(".png") || s.endsWith(".jpg") || s.endsWith(".jpeg");
        });
        if (files == null) { System.out.println("no input"); return; }
        Arrays.sort(files);

        for (File f : files) {
            byte[] raw = Files.readAllBytes(f.toPath());
            String base = strip(f.getName());

            // ── diagnostics: what the cascade decided, on the same bytes the bake sees ──
            byte[] flat = CheckerboardDetector.flattenToBlack(raw);
            BufferedImage src = toArgb(ImageIO.read(new ByteArrayInputStream(flat)));
            int w = src.getWidth(), h = src.getHeight();
            int[] px = src.getRGB(0, 0, w, h, null, 0, w);
            BgCascade.Result r = BgCascade.decide(flat, src, w, h, null);

            long bg = 0;
            if (r.mask() != null) {
                for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) if (r.mask()[x][y]) bg++;
            }
            System.out.printf("%-22s %4dx%-4d rung=%d cover=%5.1f%%  %s%n",
                    f.getName(), w, h, r.rung(), 100.0 * bg / (w * (double) h), r.note());

            if (r.mask() != null) {
                BufferedImage m = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        m.setRGB(x, y, r.mask()[x][y] ? 0xFF00FF : (px[y * w + x] & 0xFFFFFF));
                    }
                }
                ImageIO.write(m, "PNG", new File(outDir, base + "__mask.png"));
            }

            // ── the bake the owner sees ──
            byte[] baked = ImageProcessor.fillBackground(
                    ImageProcessor.toBlockPng(BackgroundRemover.recolorBackground(raw, FILL), SIZE), FILL);
            Files.write(new File(outDir, base + "__variant.png").toPath(), baked);
        }
        System.out.println("wrote " + outDir.getAbsolutePath());
    }

    private static BufferedImage toArgb(BufferedImage s) {
        if (s.getType() == BufferedImage.TYPE_INT_ARGB) return s;
        BufferedImage o = new BufferedImage(s.getWidth(), s.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.drawImage(s, 0, 0, null);
        g.dispose();
        return o;
    }

    private static String strip(String n) {
        int d = n.lastIndexOf('.');
        return d > 0 ? n.substring(0, d) : n;
    }
}
