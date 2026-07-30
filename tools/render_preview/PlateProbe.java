/**
 * PlateProbe.java — where the letter-O speckles actually live (G10 §HA4, 2026-07-28).
 *
 * Runs the REAL cascade, then reports every surviving bluish foreground pixel: its component size,
 * whether that component touches the mask, its ΔE00 to the global key, and its ΔE00 to the CLEAN
 * PLATE estimate at its own coordinate. That is the pair of numbers unit 4 is judged on — if the
 * plate distance is large too, the residue is not background-coloured locally either and the plate
 * is the wrong instrument.
 *
 * Usage: java -cp <out> com.customblocks.image.PlateProbe <image>
 */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public final class PlateProbe {

    static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public static void main(String[] args) throws Exception {
        File f = new File(args[0]);
        byte[] raw = Files.readAllBytes(f.toPath());
        BufferedImage src = ImageIO.read(f);
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);

        BgCascade.Result r = BgCascade.decide(raw, img, w, h, null);
        if (r.mask() == null) { System.out.println("cascade declined: " + r.note()); return; }
        boolean[][] isBg = r.mask();
        System.out.println("rung " + r.rung() + "  " + r.note());

        Integer key = BgRungKey.borderTone(px, w, h);
        double[] keyLab = BackgroundRemover.rgbToLab(key);
        double[][] plate = BgPlate.build(px, isBg, w, h);

        // Bluish survivors, as components of foreground.
        boolean[][] blue = new boolean[w][h];
        int n = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (isBg[x][y]) continue;
                int c = px[y * w + x];
                int rr = (c >> 16) & 255, gg = (c >> 8) & 255, bb = c & 255;
                if (bb > rr + 15 && bb > gg + 8) { blue[x][y] = true; n++; }
            }
        }
        System.out.println("bluish foreground pixels: " + n);
        if (n == 0) return;

        boolean[][] seen = new boolean[w][h];
        int comps = 0;
        for (int x0 = 0; x0 < w && comps < 25; x0++) {
            for (int y0 = 0; y0 < h && comps < 25; y0++) {
                if (!blue[x0][y0] || seen[x0][y0]) continue;
                List<int[]> piece = new ArrayList<>();
                Queue<int[]> q = new ArrayDeque<>();
                seen[x0][y0] = true; q.add(new int[]{x0, y0});
                boolean touchesBg = false;
                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    piece.add(p);
                    for (int[] d : DIRS) {
                        int nx = p[0] + d[0], ny = p[1] + d[1];
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        if (isBg[nx][ny]) { touchesBg = true; continue; }
                        if (!blue[nx][ny] || seen[nx][ny]) continue;
                        seen[nx][ny] = true; q.add(new int[]{nx, ny});
                    }
                }
                double dkMin = 999, dkMax = 0, dpMin = 999, dpMax = 0;
                for (int[] p : piece) {
                    int i = p[1] * w + p[0];
                    double[] lab = BackgroundRemover.rgbToLab(px[i]);
                    double dk = CieDe2000.of(lab, keyLab);
                    double dp = plate == null ? -1 : CieDe2000.of(lab, plate[i]);
                    dkMin = Math.min(dkMin, dk); dkMax = Math.max(dkMax, dk);
                    dpMin = Math.min(dpMin, dp); dpMax = Math.max(dpMax, dp);
                }
                System.out.printf("  comp @%d,%d size=%-5d touchesBg=%-5s dKey=%.1f-%.1f  dPlate=%.1f-%.1f%n",
                        x0, y0, piece.size(), touchesBg, dkMin, dkMax, dpMin, dpMax);
                comps++;
            }
        }
        System.out.println("area floor = " + BgQc.areaFloor(w, h) + "   JND=" + BgRungKey.JND);
    }
}
