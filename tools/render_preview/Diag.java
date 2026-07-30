/**
 * Diag.java — black-box defect probe for the §H checker path (G10, 2026-07-28).
 *
 * Runs the shipped pipeline (flattenToBlack → BgCascade) and names the two failure shapes the owner
 * reported, without editing the pipeline to instrument it:
 *
 *   ISLAND REMOVAL   a pixel that ends up transparent whose removed-component never reaches the image
 *                    border. The backdrop of a picture always reaches the border; a hole punched in
 *                    the middle of a white panel does not. (football: green dots in the white panels)
 *   INK BLACKENING   a pixel that was light in the source and comes back opaque black, i.e. artwork
 *                    that one of the two cleanup steps scrubbed. (subscribe: the three dots of ش;
 *                    share: dots inside the white arrow)
 *
 * Writes <name>__diag.png: the source, with island removals in RED and ink blackenings in BLUE.
 *
 * Usage: java -cp <out> com.customblocks.image.Diag <inDir> <outDir>
 */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class Diag {

    private static final int LIGHT = 100; // source "was clearly not black" bar
    private static final int INKY  = 24;  // output "is black" bar (CheckerboardDetector.NONBLACK)

    public static void main(String[] args) throws Exception {
        File in = new File(args[0]), outDir = new File(args[1]);
        outDir.mkdirs();
        File[] files = in.listFiles();
        if (files == null) return;
        java.util.Arrays.sort(files);
        for (File f : files) {
            String lower = f.getName().toLowerCase();
            if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) continue;
            run(f, outDir);
        }
    }

    private static void run(File f, File outDir) throws Exception {
        byte[] raw = Files.readAllBytes(f.toPath());
        BufferedImage srcImg = argb(ImageIO.read(new ByteArrayInputStream(raw)));
        int w = srcImg.getWidth(), h = srcImg.getHeight(), n = w * h;
        int[] src = srcImg.getRGB(0, 0, w, h, null, 0, w);

        byte[] flatBytes = CheckerboardDetector.flattenToBlack(raw);
        BufferedImage flatImg = argb(ImageIO.read(new ByteArrayInputStream(flatBytes)));
        int[] flat = flatImg.getRGB(0, 0, w, h, null, 0, w);

        BgCascade.Result r = BgCascade.decide(flatBytes, flatImg, w, h, null);

        boolean[] gone = new boolean[n];
        if (r.mask() != null) {
            int[] un = r.unmixed();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int i = y * w + x;
                    int cov = un == null ? (r.mask()[x][y] ? 0 : 255) : ((un[i] >>> 24) & 0xFF);
                    gone[i] = cov == 0;
                }
            }
        }

        boolean[] blackened = new boolean[n];
        for (int i = 0; i < n; i++) {
            blackened[i] = ((flat[i] >>> 24) & 0xFF) >= 250 && max(flat[i]) <= INKY && max(src[i]) > LIGHT;
        }

        boolean[] maskOnly = new boolean[n];
        if (r.mask() != null) {
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) maskOnly[y * w + x] = r.mask()[x][y];
        }
        long maskIslandPx = 0;
        for (int[] c : components(maskOnly, w, h, true)) maskIslandPx += c[0];

        List<int[]> islands = components(gone, w, h, true);  // only those that never touch the border
        List<int[]> inkBlobs = components(blackened, w, h, false);

        long islandPx = 0; int islandBig = 0;
        for (int[] c : islands) { islandPx += c[0]; if (c[0] > 32) islandBig++; }
        long inkPx = 0;
        for (int[] c : inkBlobs) inkPx += c[0];

        StringBuilder big = new StringBuilder();
        islands.sort((a, b) -> b[0] - a[0]);
        for (int k = 0; k < Math.min(6, islands.size()); k++) {
            int[] c = islands.get(k);
            big.append(' ').append(c[0]).append('[').append(c[2] - c[4] + 1).append('x').append(c[3] - c[5] + 1).append(']');
        }
        System.out.printf("%-22s flat=%-5s rung %s  maskIsland %6d   islandRemoval %6d px /%4d blobs (%d >32px, top:%s)   inkBlackened %6d px /%4d blobs%n",
                f.getName(), CheckerboardDetector.isFlattened(raw),
                r.mask() == null ? "-" : String.valueOf(r.rung()),
                maskIslandPx, islandPx, islands.size(), islandBig, big, inkPx, inkBlobs.size());

        int[] dst = new int[n];
        boolean[] isIsland = paint(islands, w, h, gone);
        boolean[] isInk = paint(inkBlobs, w, h, blackened);
        for (int i = 0; i < n; i++) {
            if (isIsland[i]) dst[i] = 0xFFFF0000;
            else if (isInk[i]) dst[i] = 0xFF0080FF;
            else dst[i] = 0xFF000000 | (src[i] & 0xFFFFFF);
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, dst, 0, w);
        ImageIO.write(out, "PNG", new File(outDir, f.getName().replaceAll("\\.[^.]+$", "") + "__diag.png"));
    }

    /** Components of {@code set}; when {@code interiorOnly}, drops any that touches the image border. */
    private static List<int[]> components(boolean[] set, int w, int h, boolean interiorOnly) {
        int n = w * h;
        boolean[] seen = new boolean[n];
        int[] q = new int[n];
        List<int[]> out = new ArrayList<>(); // {size, seedIndex, maxX, maxY, minX, minY}
        for (int s = 0; s < n; s++) {
            if (!set[s] || seen[s]) continue;
            int head = 0, tail = 0; seen[s] = true; q[tail++] = s;
            boolean border = false;
            int mnx = w, mny = h, mxx = -1, mxy = -1;
            while (head < tail) {
                int i = q[head++], x = i % w, y = i / w;
                if (x < mnx) mnx = x; if (x > mxx) mxx = x;
                if (y < mny) mny = y; if (y > mxy) mxy = y;
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) border = true;
                if (x + 1 < w && set[i + 1] && !seen[i + 1]) { seen[i + 1] = true; q[tail++] = i + 1; }
                if (x > 0     && set[i - 1] && !seen[i - 1]) { seen[i - 1] = true; q[tail++] = i - 1; }
                if (y + 1 < h && set[i + w] && !seen[i + w]) { seen[i + w] = true; q[tail++] = i + w; }
                if (y > 0     && set[i - w] && !seen[i - w]) { seen[i - w] = true; q[tail++] = i - w; }
            }
            if (interiorOnly && border) continue;
            out.add(new int[]{tail, s, mxx, mxy, mnx, mny});
        }
        return out;
    }

    /** Re-flood the listed components so they can be drawn. */
    private static boolean[] paint(List<int[]> comps, int w, int h, boolean[] set) {
        int n = w * h;
        boolean[] hit = new boolean[n];
        int[] q = new int[n];
        for (int[] c : comps) {
            int head = 0, tail = 0; int s = c[1];
            if (hit[s]) continue;
            hit[s] = true; q[tail++] = s;
            while (head < tail) {
                int i = q[head++], x = i % w, y = i / w;
                if (x + 1 < w && set[i + 1] && !hit[i + 1]) { hit[i + 1] = true; q[tail++] = i + 1; }
                if (x > 0     && set[i - 1] && !hit[i - 1]) { hit[i - 1] = true; q[tail++] = i - 1; }
                if (y + 1 < h && set[i + w] && !hit[i + w]) { hit[i + w] = true; q[tail++] = i + w; }
                if (y > 0     && set[i - w] && !hit[i - w]) { hit[i - w] = true; q[tail++] = i - w; }
            }
        }
        return hit;
    }

    private static int max(int argb) {
        return Math.max((argb >> 16) & 0xFF, Math.max((argb >> 8) & 0xFF, argb & 0xFF));
    }

    private static BufferedImage argb(BufferedImage s) {
        if (s.getType() == BufferedImage.TYPE_INT_ARGB) return s;
        BufferedImage o = new BufferedImage(s.getWidth(), s.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics(); g.drawImage(s, 0, 0, null); g.dispose();
        return o;
    }
}
