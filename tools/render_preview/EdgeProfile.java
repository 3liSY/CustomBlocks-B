/** EdgeProfile.java — sample the mask boundary and print a profile normal to it, so an edge defect
 *  is read as numbers instead of guessed coordinates.
 *  Usage: java ... com.customblocks.image.EdgeProfile <img> [samples] [reach] */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;

public final class EdgeProfile {
    public static void main(String[] a) throws Exception {
        byte[] raw = Files.readAllBytes(new File(a[0]).toPath());
        int samples = a.length > 1 ? Integer.parseInt(a[1]) : 6;
        int reach = a.length > 2 ? Integer.parseInt(a[2]) : 6;

        byte[] flat = CheckerboardDetector.flattenToBlack(raw);
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(flat));
        BufferedImage img = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics(); g.drawImage(src, 0, 0, null); g.dispose();
        int w = img.getWidth(), h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        BgCascade.Result r = BgCascade.decide(flat, img, w, h, null);
        System.out.println(new File(a[0]).getName() + "  rung=" + r.rung() + "  " + r.note());
        if (r.mask() == null) return;
        boolean[][] m = r.mask();
        int[] un = r.unmixed();

        // walk a horizontal line every h/(samples+1) and report the first background→subject crossing
        for (int s = 1; s <= samples; s++) {
            int y = s * h / (samples + 1);
            int cross = -1;
            for (int x = 1; x < w; x++) {
                if (m[x - 1][y] && !m[x][y]) { cross = x; break; }
            }
            if (cross < 0) { System.out.println("y=" + y + "  no crossing"); continue; }
            StringBuilder sb = new StringBuilder("y=" + y + " x=" + cross + "  ");
            for (int d = -reach; d <= reach; d++) {
                int x = cross + d;
                if (x < 0 || x >= w) continue;
                int i = y * w + x;
                sb.append(String.format("%s%06X/a%02X/%s ", d == 0 ? ">" : "",
                        px[i] & 0xFFFFFF, (px[i] >>> 24) & 0xFF, m[x][y] ? "B" : "f"));
            }
            System.out.println(sb);
            StringBuilder ub = new StringBuilder("        out:   ");
            for (int d = -reach; d <= reach; d++) {
                int x = cross + d;
                if (x < 0 || x >= w) continue;
                ub.append(String.format("%08X ", un[y * w + x]));
            }
            System.out.println(ub);
        }
    }
}
