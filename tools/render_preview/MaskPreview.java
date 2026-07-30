/**
 * MaskPreview.java — render what the cascade actually REMOVED, at source resolution (G10 §H).
 *
 * The golden rails bake an opaque block texture, so their alpha channel says nothing about the
 * decision. This writes the source with every removed pixel painted a loud magenta, which is the only
 * honest way to look at a mask: anything magenta inside the artwork is over-removal, anything of the
 * original backdrop still showing is under-removal.
 *
 * Usage: java -cp <out> com.customblocks.image.MaskPreview <inDir> <outDir>
 */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;

public final class MaskPreview {

    private static final int MAGENTA = 0xFF00E614;

    public static void main(String[] args) throws Exception {
        File in = new File(args[0]), outDir = new File(args[1]);
        outDir.mkdirs();
        File[] files = in.listFiles();
        if (files == null) return;
        java.util.Arrays.sort(files);
        for (File f : files) {
            String lower = f.getName().toLowerCase();
            if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) continue;

            byte[] raw = Files.readAllBytes(f.toPath());
            byte[] flat = CheckerboardDetector.flattenToBlack(raw);
            BufferedImage src = ImageIO.read(new java.io.ByteArrayInputStream(flat));
            int w = src.getWidth(), h = src.getHeight();
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.drawImage(src, 0, 0, null);
            g.dispose();

            BgCascade.Result r = BgCascade.decide(flat, img, w, h, null);
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            int[] px = img.getRGB(0, 0, w, h, null, 0, w);
            int[] dst = new int[w * h];

            if (r.mask() == null) {
                System.arraycopy(px, 0, dst, 0, w * h);
                System.out.printf("%-22s DECLINED — nothing removed%n", f.getName());
            } else {
                // Composite the REAL result: rung 5 hands back subject coverage in the alpha channel
                // and the subject colour with the backdrop divided out, so a boundary pixel is partly
                // transparent. Painting the raw source for every non-mask pixel would invent a hard
                // opaque rim that the bake does not have.
                long removed = 0;
                int[] un = r.unmixed();
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int i = y * w + x;
                        int cov = un == null ? (r.mask()[x][y] ? 0 : 255) : ((un[i] >>> 24) & 0xFF);
                        int rgb = un == null ? px[i] : un[i];
                        if (cov == 0) { dst[i] = MAGENTA; removed++; continue; }
                        int sr = (rgb >> 16) & 255, sg = (rgb >> 8) & 255, sb = rgb & 255;
                        double a = cov / 255.0;
                        int orr = (int) Math.round(sr * a + 0   * (1 - a));
                        int og  = (int) Math.round(sg * a + 230 * (1 - a));
                        int ob  = (int) Math.round(sb * a + 20  * (1 - a));
                        dst[i] = 0xFF000000 | (orr << 16) | (og << 8) | ob;
                    }
                }
                System.out.printf("%-22s rung %d, removed %.1f%%%n",
                        f.getName(), r.rung(), 100.0 * removed / (w * (double) h));
            }
            out.setRGB(0, 0, w, h, dst, 0, w);
            ImageIO.write(out, "PNG", new File(outDir, f.getName().replaceAll("\\.[^.]+$", "") + "__mask.png"));
        }
    }
}
