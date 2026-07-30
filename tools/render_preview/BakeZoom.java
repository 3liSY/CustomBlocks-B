package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;

/**
 * BakeZoom — run the REAL colour-variant rail on a source and write the bake, plus a magnified crop
 * of one edge, so a pale outline is something to look at rather than something to argue about.
 *
 * Usage: java -cp <out> com.customblocks.image.BakeZoom <src> <outDir> <fillRgbHex> [x y size scale]
 */
public final class BakeZoom {
    public static void main(String[] a) throws Exception {
        File src = new File(a[0]);
        File outDir = new File(a[1]); outDir.mkdirs();
        int fill = (int) Long.parseLong(a[2], 16);
        byte[] raw = Files.readAllBytes(src.toPath());

        byte[] baked = BackgroundRemover.recolorBackground(raw, fill);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(baked));
        String base = src.getName().replaceAll("\\.[^.]+$", "");
        ImageIO.write(img, "PNG", new File(outDir, base + "__bake.png"));

        if (a.length >= 7) {
            int x = Integer.parseInt(a[3]), y = Integer.parseInt(a[4]);
            int size = Integer.parseInt(a[5]), scale = Integer.parseInt(a[6]);
            BufferedImage crop = img.getSubimage(x, y, size, size);
            BufferedImage big = new BufferedImage(size * scale, size * scale, BufferedImage.TYPE_INT_ARGB);
            for (int yy = 0; yy < size * scale; yy++)
                for (int xx = 0; xx < size * scale; xx++)
                    big.setRGB(xx, yy, crop.getRGB(xx / scale, yy / scale));
            ImageIO.write(big, "PNG", new File(outDir, base + "__zoom.png"));
        }
        System.out.println("wrote " + base + " (" + img.getWidth() + "x" + img.getHeight() + ")");
    }
}
