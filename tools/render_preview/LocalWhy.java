/**
 * LocalWhy.java — for named source coordinates, print which of unit 4's three conditions stopped it
 * (G10 §HA4). Reproduces the unit's own candidate/piece logic against the FINAL mask, so a survivor is
 * attributed to a colour bar, a size floor or the enclosure rule rather than guessed at.
 *
 * Usage: java -cp <out> com.customblocks.image.LocalWhy <image> x,y x,y ...
 */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Queue;

public final class LocalWhy {

    static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public static void main(String[] args) throws Exception {
        File f = new File(args[0]);
        byte[] raw = Files.readAllBytes(f.toPath());
        BufferedImage src = ImageIO.read(f);
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics(); g.drawImage(src, 0, 0, null); g.dispose();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);

        BgCascade.Result r = BgCascade.decide(raw, img, w, h, null);
        boolean[][] isBg = r.mask();
        if (isBg == null) { System.out.println("declined"); return; }

        double bar = BgRungKey.JND * 2.0;
        long floor = BgQc.areaFloor(w, h);
        double[][] plate = BgPlate.build(px, isBg, w, h);

        boolean[][] cand = new boolean[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (isBg[x][y]) continue;
                int i = y * w + x;
                if (CieDe2000.of(BackgroundRemover.rgbToLab(px[i]), BgPlate.labOf(plate[i])) <= bar) {
                    cand[x][y] = true;
                }
            }
        }

        System.out.println("floor=" + floor + " bar=" + String.format("%.2f", bar));
        for (int k = 1; k < args.length; k++) {
            String[] xy = args[k].split(",");
            int x = Integer.parseInt(xy[0]), y = Integer.parseInt(xy[1]);
            int i = y * w + x;
            double d = CieDe2000.of(BackgroundRemover.rgbToLab(px[i]), BgPlate.labOf(plate[i]));
            if (isBg[x][y]) { System.out.printf("(%d,%d) already background%n", x, y); continue; }
            if (!cand[x][y]) {
                System.out.printf("(%d,%d) STOPPED BY COLOUR  dPlate=%.1f > %.1f%n", x, y, d, bar);
                continue;
            }
            long size = 0; boolean touchesBg = false;
            boolean[][] seen = new boolean[w][h];
            Queue<int[]> q = new ArrayDeque<>();
            seen[x][y] = true; q.add(new int[]{x, y});
            while (!q.isEmpty()) {
                int[] p = q.poll(); size++;
                for (int[] dd : DIRS) {
                    int nx = p[0] + dd[0], ny = p[1] + dd[1];
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    if (isBg[nx][ny]) { touchesBg = true; continue; }
                    if (!cand[nx][ny] || seen[nx][ny]) continue;
                    seen[nx][ny] = true; q.add(new int[]{nx, ny});
                }
            }
            String why = size > floor ? "STOPPED BY SIZE" : (!touchesBg ? "STOPPED BY ENCLOSURE" : "SHOULD HAVE BEEN TAKEN");
            System.out.printf("(%d,%d) %s  dPlate=%.1f size=%d touchesBg=%s%n",
                    x, y, why, d, size, touchesBg);
        }
    }
}
