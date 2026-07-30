/** PixLine.java — print a scanline across an edge: source rgb, ΔE00 to the deciding key,
 *  the mask verdict, and what the unmix rung wrote there.
 *  Usage: java ... com.customblocks.image.PixLine <img> <x0> <y0> <dx> <dy> <count> */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;

public final class PixLine {
    public static void main(String[] a) throws Exception {
        byte[] raw = Files.readAllBytes(new File(a[0]).toPath());
        byte[] flat = CheckerboardDetector.flattenToBlack(raw);
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(flat));
        BufferedImage img = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics(); g.drawImage(src, 0, 0, null); g.dispose();
        int w = img.getWidth(), h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        BgCascade.Result r = BgCascade.decide(flat, img, w, h, null);
        Integer key = BgRungKey.flatBorderTone(px, w, h);
        Integer border = BgRungKey.borderTone(px, w, h);
        System.out.println("rung=" + r.rung() + " key=" + (key == null ? "none" : String.format("%06X", key & 0xFFFFFF))
                + " border=" + (border == null ? "none" : String.format("%06X", border & 0xFFFFFF)) + "  JND=" + BgRungKey.JND);
        BgDist dist = key != null ? new BgDist(BackgroundRemover.rgbToLab(key))
                : (border != null ? new BgDist(BackgroundRemover.rgbToLab(border)) : null);

        int x = Integer.parseInt(a[1]), y = Integer.parseInt(a[2]);
        int dx = Integer.parseInt(a[3]), dy = Integer.parseInt(a[4]), n = Integer.parseInt(a[5]);
        int[] un = r.unmixed();
        for (int i = 0; i < n; i++, x += dx, y += dy) {
            if (x < 0 || y < 0 || x >= w || y >= h) break;
            int p = px[y * w + x];
            int u = un == null ? 0 : un[y * w + x];
            System.out.printf("(%4d,%4d) src=%06X dE=%6.2f bg=%-5s out=%08X%n",
                    x, y, p & 0xFFFFFF, dist == null ? -1 : dist.of(p),
                    r.mask() == null ? "-" : String.valueOf(r.mask()[x][y]), u);
        }
    }
}
