package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** HaloProbe — print the raw alpha/RGB profile across a scanline, to see what the surviving rim IS. */
public final class HaloProbe {
    public static void main(String[] a) throws Exception {
        BufferedImage src = ImageIO.read(new File(a[0]));
        int w = src.getWidth(), h = src.getHeight();
        int y = Integer.parseInt(a[1]);
        int x0 = Integer.parseInt(a[2]), x1 = Integer.parseInt(a[3]);
        System.out.println(w + "x" + h + "  type=" + src.getType() + "  row y=" + y);
        for (int x = x0; x <= x1 && x < w; x++) {
            int p = src.getRGB(x, y);
            System.out.printf("x=%3d  A=%3d  R=%3d G=%3d B=%3d%n",
                    x, (p >>> 24) & 0xFF, (p >> 16) & 0xFF, (p >> 8) & 0xFF, p & 0xFF);
        }
    }
}
