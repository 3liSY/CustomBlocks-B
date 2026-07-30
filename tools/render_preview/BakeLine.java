/** BakeLine.java — recolorBackground at NATIVE resolution, then print a line of the result next to
 *  the source. Usage: java ... com.customblocks.image.BakeLine <img> <x> <y> <dx> <dy> <n> [outPng] */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;

public final class BakeLine {
    public static void main(String[] a) throws Exception {
        byte[] raw = Files.readAllBytes(new File(a[0]).toPath());
        byte[] baked = BackgroundRemover.recolorBackground(raw, 0x22CC22);
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(baked));
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(
                CheckerboardDetector.flattenToBlack(raw)));
        int x = Integer.parseInt(a[1]), y = Integer.parseInt(a[2]);
        int dx = Integer.parseInt(a[3]), dy = Integer.parseInt(a[4]), n = Integer.parseInt(a[5]);
        for (int i = 0; i < n; i++, x += dx, y += dy) {
            if (x < 0 || y < 0 || x >= out.getWidth() || y >= out.getHeight()) break;
            System.out.printf("(%3d,%3d) src=%08X bake=%06X%n", x, y,
                    src.getRGB(x, y), out.getRGB(x, y) & 0xFFFFFF);
        }
        if (a.length > 6) ImageIO.write(out, "PNG", new File(a[6]));
    }
}
