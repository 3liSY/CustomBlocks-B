/**
 * CheckerPreview.java — G10 §D dark-checkerboard fix preview.
 *
 * Runs the REAL CheckerboardDetector over a folder of source PNGs and writes one before/after sheet
 * per image plus a combined contact sheet, so the owner can approve the widened detection by eye
 * before a jar is built. Nothing here reimplements the detector: it is compiled straight from
 * src/main/java and called through its public entry points, so what the sheet shows is what the mod
 * will do.
 *
 * Run (from the repo root):
 *   javac -d tools/render_preview/out src/main/java/com/customblocks/image/CheckerboardDetector.java \
 *         tools/render_preview/CheckerPreview.java
 *   java -cp tools/render_preview/out CheckerPreview <inputDir> <outputDir>
 */
import com.customblocks.image.CheckerboardDetector;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CheckerPreview {

    private static final int CELL = 320;     // one panel's drawn size
    private static final int PAD = 16;
    private static final int LABEL = 34;

    public static void main(String[] args) throws Exception {
        File inDir = new File(args.length > 0 ? args[0] : "tools/render_preview/checker_in");
        File outDir = new File(args.length > 1 ? args[1] : "tools/render_preview/checker_out");
        outDir.mkdirs();

        File[] files = inDir.listFiles((d, n) -> n.toLowerCase().endsWith(".png"));
        if (files == null || files.length == 0) {
            System.out.println("No PNGs in " + inDir.getAbsolutePath());
            return;
        }
        Arrays.sort(files);

        List<BufferedImage> rows = new ArrayList<>();
        System.out.println("file                        detected  changed   verdict");
        System.out.println("--------------------------------------------------------");
        for (File f : files) {
            byte[] raw = Files.readAllBytes(f.toPath());
            boolean detected = CheckerboardDetector.isFlattened(raw);
            byte[] out = CheckerboardDetector.flattenToBlack(raw);
            boolean changed = !Arrays.equals(raw, out);

            BufferedImage before = ImageIO.read(new ByteArrayInputStream(raw));
            BufferedImage after = ImageIO.read(new ByteArrayInputStream(out));

            // A control image must come back untouched; a checkerboard source must not.
            boolean isControl = f.getName().startsWith("control");
            String verdict = isControl ? (changed ? "!! CONTROL CHANGED" : "ok (untouched)")
                                       : (changed ? "flattened" : "!! NOT FLATTENED");
            System.out.printf("%-27s %-9s %-9s %s%n", f.getName(), detected, changed, verdict);

            BufferedImage row = sheet(f.getName(), before, after, detected, changed, verdict);
            ImageIO.write(row, "PNG", new File(outDir, "cmp_" + f.getName()));
            rows.add(row);
        }

        int w = rows.stream().mapToInt(BufferedImage::getWidth).max().orElse(CELL);
        int h = rows.stream().mapToInt(BufferedImage::getHeight).sum();
        BufferedImage all = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = all.createGraphics();
        g.setColor(new Color(0x1A1A1A));
        g.fillRect(0, 0, w, h);
        int y = 0;
        for (BufferedImage r : rows) { g.drawImage(r, 0, y, null); y += r.getHeight(); }
        g.dispose();
        ImageIO.write(all, "PNG", new File(outDir, "_ALL.png"));
        System.out.println("\nWrote " + outDir.getAbsolutePath());
    }

    /** One "name | before | after" strip. */
    private static BufferedImage sheet(String name, BufferedImage before, BufferedImage after,
                                       boolean detected, boolean changed, String verdict) {
        int w = PAD * 3 + CELL * 2;
        int h = PAD * 2 + CELL + LABEL;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x1A1A1A));
        g.fillRect(0, 0, w, h);

        drawFit(g, before, PAD, PAD + LABEL);
        drawFit(g, after, PAD * 2 + CELL, PAD + LABEL);

        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(Color.WHITE);
        g.drawString(name, PAD, PAD + 16);
        boolean bad = verdict.startsWith("!!");
        g.setColor(bad ? new Color(0xFF4040) : new Color(0x40FF00));
        g.drawString("detected=" + detected + "  " + verdict, PAD + 240, PAD + 16);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(0x9A9A9A));
        g.drawString("BEFORE", PAD, PAD + LABEL + CELL + 12);
        g.drawString("AFTER", PAD * 2 + CELL, PAD + LABEL + CELL + 12);
        g.dispose();
        return img;
    }

    /** Draw {@code src} fitted into a CELL box on a mid-grey plate, so black output is still visible. */
    private static void drawFit(Graphics2D g, BufferedImage src, int x, int y) {
        g.setColor(new Color(0x3C3C3C));
        g.fillRect(x, y, CELL, CELL);
        if (src == null) return;
        double s = Math.min(CELL / (double) src.getWidth(), CELL / (double) src.getHeight());
        int dw = Math.max(1, (int) Math.round(src.getWidth() * s));
        int dh = Math.max(1, (int) Math.round(src.getHeight() * s));
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, x + (CELL - dw) / 2, y + (CELL - dh) / 2, dw, dh, null);
        g.setColor(new Color(0x555555));
        g.drawRect(x, y, CELL - 1, CELL - 1);
    }
}
