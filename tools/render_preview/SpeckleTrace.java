/**
 * SpeckleTrace.java — trace the baked blue specks back to the stage that produced them (G10 §HA4).
 *
 * Takes the BAKED sheet, finds every strong-blue pixel, maps it back to source coordinates, and prints
 * what the pipeline thought about that spot: mask, distance into the subject, band membership, and the
 * coverage rung 5 assigned. Guessing which stage owns a speck is what made §HA4 take three passes.
 *
 * Usage: java -cp <out> com.customblocks.image.SpeckleTrace <source> <bakedPng>
 */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;

public final class SpeckleTrace {

    public static void main(String[] args) throws Exception {
        File srcF = new File(args[0]);
        byte[] raw = Files.readAllBytes(srcF.toPath());
        BufferedImage src = ImageIO.read(srcF);
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);

        BgCascade.Result r = BgCascade.decide(raw, img, w, h, null);
        if (r.mask() == null) { System.out.println("declined: " + r.note()); return; }
        boolean[][] isBg = r.mask();
        int[] unmixed = r.unmixed();

        BufferedImage baked = ImageIO.read(new File(args[1]));
        int bw = baked.getWidth(), bh = baked.getHeight();
        System.out.println("source " + w + "x" + h + "   baked " + bw + "x" + bh + "   rung " + r.rung());

        int shown = 0, total = 0;
        for (int by = 0; by < bh && shown < 20; by++) {
            for (int bx = 0; bx < bw && shown < 20; bx++) {
                int c = baked.getRGB(bx, by);
                if (((c >>> 24) & 255) < 8) continue;
                int rr = (c >> 16) & 255, gg = (c >> 8) & 255, bb = c & 255;
                if (!(bb > rr + 40 && bb > gg + 20)) continue;
                total++;
                int sx = (int) ((bx + 0.5) * w / bw), sy = (int) ((by + 0.5) * h / bh);
                sx = Math.min(w - 1, sx); sy = Math.min(h - 1, sy);
                int i = sy * w + sx;
                int cov = (unmixed[i] >>> 24) & 255;
                int sp = px[i];
                System.out.printf("  baked(%d,%d)=%02x%02x%02x  src(%d,%d)=%02x%02x%02x  isBg=%-5s cov=%d%n",
                        bx, by, rr, gg, bb, sx, sy,
                        (sp >> 16) & 255, (sp >> 8) & 255, sp & 255, isBg[sx][sy], cov);
                shown++;
            }
        }
        System.out.println("strong-blue baked pixels: " + total + " (first " + shown + " shown)");
    }
}
