/** FlatDump.java — write what CheckerboardDetector.flattenToBlack produced: the RGB, and an
 *  alpha map (black = declared grid, white = kept). Usage: java ... FlatDump <img> <outPrefix> */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;

public final class FlatDump {
    public static void main(String[] a) throws Exception {
        byte[] raw = Files.readAllBytes(new File(a[0]).toPath());
        byte[] flat = CheckerboardDetector.flattenToBlack(raw);
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(flat));
        BufferedImage img = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics(); g.drawImage(src, 0, 0, null); g.dispose();
        int w = img.getWidth(), h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        BufferedImage alpha = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        BufferedImage over = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        long declared = 0;
        for (int i = 0; i < w * h; i++) {
            int al = (px[i] >>> 24) & 0xFF;
            if (al < 128) declared++;
            alpha.setRGB(i % w, i / w, (al << 16) | (al << 8) | al);
            int fill = 0x22CC22;
            over.setRGB(i % w, i / w, LinearBlend.over(px[i], 0xFF000000 | fill) & 0xFFFFFF);
        }
        ImageIO.write(alpha, "PNG", new File(a[1] + "_alpha.png"));
        ImageIO.write(over, "PNG", new File(a[1] + "_over.png"));
        System.out.printf("%s  %dx%d declared=%d (%.1f%%)%n", a[0], w, h, declared, 100.0 * declared / (w * (double) h));
    }
}
