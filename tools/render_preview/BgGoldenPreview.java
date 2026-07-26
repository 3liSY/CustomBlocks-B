/**
 * BgGoldenPreview.java — G10 §H golden baselines for the background pipeline.
 *
 * Freezes what the CURRENT jar does to a background, before §H rips `/cb tolerance` out. Nothing here
 * reimplements the pipeline: BackgroundRemover and ImageProcessor are compiled straight from
 * src/main/java and called through their real public entry points, in the same order the real bake
 * calls them, so a sheet shows what the mod actually produces.
 *
 * Seven bakes per picture, one per behaviour the rip touches:
 *
 *   off         mode none  — the "removal off" bake. Proves the unconditional-opaque snap.
 *   edges       mode edges, tol 30 (the config default) — the ordinary Auto case.
 *   closed      mode closed, tol 30 — background + enclosed areas.
 *   smart       mode smart, tol 0 — exercises the hidden `smart ? 35 : 0` default that §H deletes.
 *   edges_fill  edges bake finished with ImageProcessor.fillBackground — the create-with-a-Studio-
 *               background path (CreationCommands), and a gamma-space blend site §H moves to linear
 *               light. Note it calls the 3-arg apply, so the removed background is painted BLACK and
 *               only the padding gets the colour; that is the current behaviour, faithfully frozen.
 *   setbg       apply(4-arg fill) + fillBackground + snapBackgroundColor — the `/cb setbg <colour>`
 *               rail (BackgroundService.rebake), where the fill DOES reach the removed background.
 *   variant     recolorBackground(mode none → forced edges, tol 30) + fillBackground — the colour-
 *               variant rail, i.e. the path the Arabic sets and colour families take.
 *
 * Two outputs, because a picture and a byte are different kinds of evidence:
 *   golden/png/<pic>__<variant>.png   the exact baked bytes, plus a SHA-256 in the manifest. This is
 *                                     the actual baseline; a later change is a provable diff, not an
 *                                     opinion about a thumbnail.
 *   golden/sheets/<pic>.png           source + all six bakes side by side for the owner to look at.
 *
 * Run (from the repo root, JDK 21 on JAVA_HOME):
 *   javac -d tools/render_preview/out \
 *         src/main/java/com/customblocks/image/BackgroundRemover.java \
 *         src/main/java/com/customblocks/image/BgMask.java \
 *         src/main/java/com/customblocks/image/CheckerboardDetector.java \
 *         src/main/java/com/customblocks/image/ImageProcessor.java \
 *         src/main/java/com/customblocks/image/ImageResampler.java \
 *         tools/render_preview/BgGoldenPreview.java
 *   java -cp tools/render_preview/out BgGoldenPreview <inputDir> <outputDir> [baselineDir]
 *
 * With a third argument the run becomes a REVIEW: every bake is compared pixel-for-pixel against the
 * baseline of the same name, the table reports how much moved, and a heat map is written for anything
 * that changed. That is how a Stage 2/3 change gets read — as a diff against the frozen behaviour.
 */
import com.customblocks.image.BackgroundRemover;
import com.customblocks.image.ImageProcessor;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BgGoldenPreview {

    private static final int CELL = 256;   // one panel's drawn size
    private static final int PAD = 12;
    private static final int LABEL = 30;

    /** The config defaults the real bake reads, pinned here so a golden is reproducible. */
    private static final int TEXTURE_SIZE = 512;   // CustomBlocksConfig.textureSize
    /** The colour-variant "red" fill, used for both the stored-background and variant bakes. */
    private static final int FILL_RGB = 0xEE3333;

    /** Amplification on the diff heat map — an 8× scale makes a 1-value channel drift visible. */
    private static final int DIFF_GAIN = 8;

    private interface Bake { byte[] run(byte[] raw) throws Exception; }

    private record Variant(String label, String what, Bake bake) {}

    private static final List<Variant> VARIANTS = List.of(
            new Variant("off", "mode none",
                    raw -> snap(page(BackgroundRemover.apply(raw, "none")), "none")),
            new Variant("edges", "legacy BgRemove spelling -> auto",
                    raw -> snap(page(BackgroundRemover.apply(raw, "edges")), "edges")),
            new Variant("closed", "legacy BgRemove&More spelling -> auto",
                    raw -> snap(page(BackgroundRemover.apply(raw, "closed")), "closed")),
            new Variant("smart", "retired BgSmart spelling -> auto",
                    raw -> snap(page(BackgroundRemover.apply(raw, "smart")), "smart")),
            new Variant("edges_fill", "auto + stored bg fill",
                    raw -> ImageProcessor.fillBackground(page(BackgroundRemover.apply(raw, "auto")), FILL_RGB)),
            new Variant("setbg", "/cb setbg colour rail",
                    raw -> BackgroundRemover.snapBackgroundColor(
                            ImageProcessor.fillBackground(
                                    page(BackgroundRemover.apply(raw, "auto", FILL_RGB)), FILL_RGB),
                            "auto", FILL_RGB)),
            new Variant("variant", "recolour rail (always auto)",
                    raw -> ImageProcessor.fillBackground(
                            page(BackgroundRemover.recolorBackground(raw, FILL_RGB)), FILL_RGB))
    );

    private static byte[] page(byte[] cleaned) throws Exception {
        return ImageProcessor.toBlockPng(cleaned, TEXTURE_SIZE);
    }

    private static byte[] snap(byte[] png, String mode) {
        return BackgroundRemover.snapBackgroundBlack(png, mode);
    }

    public static void main(String[] args) throws Exception {
        File inDir = new File(args.length > 0 ? args[0] : "tools/render_preview/bg_in");
        File outDir = new File(args.length > 1 ? args[1] : "tools/render_preview/bg_golden");
        File baseDir = args.length > 2 ? new File(args[2]) : null;

        File pngDir = new File(outDir, "png");
        File sheetDir = new File(outDir, "sheets");
        File diffDir = new File(outDir, "diff");
        pngDir.mkdirs();
        sheetDir.mkdirs();
        if (baseDir != null) diffDir.mkdirs();

        File[] files = inDir.listFiles((d, n) -> {
            String s = n.toLowerCase();
            return s.endsWith(".png") || s.endsWith(".jpg") || s.endsWith(".jpeg");
        });
        if (files == null || files.length == 0) {
            System.out.println("No images in " + inDir.getAbsolutePath());
            return;
        }
        Arrays.sort(files);

        StringBuilder manifest = new StringBuilder();
        manifest.append("# G10 §H golden baselines — background pipeline\n");
        manifest.append("# texture size ").append(TEXTURE_SIZE)
                .append(", fill #").append(String.format("%06X", FILL_RGB)).append('\n');
        manifest.append("# picture | variant | out bytes | ms | sha256").append(baseDir != null ? " | vs baseline" : "")
                .append('\n');

        List<BufferedImage> sheets = new ArrayList<>();
        int changedBakes = 0;

        for (File f : files) {
            byte[] raw = Files.readAllBytes(f.toPath());
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(raw));
            String dims = src == null ? "unreadable" : src.getWidth() + "x" + src.getHeight();
            System.out.println("\n" + f.getName() + "  (" + dims + ", " + raw.length + " bytes)");
            System.out.println("  variant      ms     bytes    sha256(12)   " + (baseDir != null ? "diff" : ""));

            List<BufferedImage> panels = new ArrayList<>();
            List<String> captions = new ArrayList<>();
            panels.add(src);
            captions.add("SOURCE " + dims);

            for (Variant v : VARIANTS) {
                String outName = strip(f.getName()) + "__" + v.label() + ".png";
                byte[] baked;
                long ms;
                try {
                    long t0 = System.nanoTime();
                    baked = v.bake().run(raw);
                    ms = (System.nanoTime() - t0) / 1_000_000L;
                } catch (Exception e) {
                    System.out.printf("  %-12s FAILED  %s%n", v.label(), e);
                    manifest.append(f.getName()).append(" | ").append(v.label())
                            .append(" | FAILED | - | ").append(e).append('\n');
                    panels.add(null);
                    captions.add(v.label() + " FAILED");
                    continue;
                }

                Files.write(new File(pngDir, outName).toPath(), baked);
                String sha = sha256(baked);
                BufferedImage after = ImageIO.read(new ByteArrayInputStream(baked));

                String diffNote = "";
                if (baseDir != null) {
                    File basePng = new File(new File(baseDir, "png"), outName);
                    if (!basePng.isFile()) {
                        diffNote = "no baseline";
                    } else {
                        byte[] baseBytes = Files.readAllBytes(basePng.toPath());
                        if (Arrays.equals(baseBytes, baked)) {
                            diffNote = "identical";
                        } else {
                            BufferedImage before = ImageIO.read(new ByteArrayInputStream(baseBytes));
                            int[] d = compare(before, after);
                            diffNote = d[0] + " px moved, max " + d[1];
                            changedBakes++;
                            BufferedImage heat = heatMap(before, after);
                            if (heat != null) ImageIO.write(heat, "PNG", new File(diffDir, "diff_" + outName));
                        }
                    }
                }

                System.out.printf("  %-12s %-6d %-8d %-12s %s%n",
                        v.label(), ms, baked.length, sha.substring(0, 12), diffNote);
                manifest.append(f.getName()).append(" | ").append(v.label())
                        .append(" | ").append(baked.length).append(" | ").append(ms)
                        .append(" | ").append(sha);
                if (baseDir != null) manifest.append(" | ").append(diffNote);
                manifest.append('\n');

                panels.add(after);
                captions.add(v.label() + "  (" + v.what() + ")");
            }

            BufferedImage sheet = sheet(f.getName() + "   " + dims, panels, captions);
            ImageIO.write(sheet, "PNG", new File(sheetDir, "sheet_" + strip(f.getName()) + ".png"));
            sheets.add(sheet);
        }

        int w = sheets.stream().mapToInt(BufferedImage::getWidth).max().orElse(CELL);
        int h = sheets.stream().mapToInt(BufferedImage::getHeight).sum();
        BufferedImage all = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = all.createGraphics();
        g.setColor(new Color(0x1A1A1A));
        g.fillRect(0, 0, w, h);
        int y = 0;
        for (BufferedImage s : sheets) { g.drawImage(s, 0, y, null); y += s.getHeight(); }
        g.dispose();
        ImageIO.write(all, "PNG", new File(outDir, "_ALL.png"));

        Files.writeString(new File(outDir, "MANIFEST.txt").toPath(), manifest.toString());
        System.out.println("\nWrote " + outDir.getAbsolutePath());
        if (baseDir != null) {
            System.out.println(changedBakes == 0
                    ? "No bake changed against the baseline."
                    : changedBakes + " bake(s) changed — heat maps in " + diffDir.getAbsolutePath());
        }
    }

    /** Changed-pixel count and the largest single-channel move, so "it looks fine" gets a number. */
    private static int[] compare(BufferedImage a, BufferedImage b) {
        if (a == null || b == null) return new int[]{-1, -1};
        int w = Math.min(a.getWidth(), b.getWidth()), h = Math.min(a.getHeight(), b.getHeight());
        int moved = 0, max = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = a.getRGB(x, y), q = b.getRGB(x, y);
                if (p == q) continue;
                moved++;
                for (int sh = 0; sh <= 24; sh += 8) {
                    int d = Math.abs(((p >> sh) & 0xFF) - ((q >> sh) & 0xFF));
                    if (d > max) max = d;
                }
            }
        }
        return new int[]{moved, max};
    }

    /** Black where nothing moved, brightening with the size of the move. Alpha drift shows red. */
    private static BufferedImage heatMap(BufferedImage a, BufferedImage b) {
        if (a == null || b == null) return null;
        int w = Math.min(a.getWidth(), b.getWidth()), h = Math.min(a.getHeight(), b.getHeight());
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = a.getRGB(x, y), q = b.getRGB(x, y);
                int dc = 0;
                for (int sh = 0; sh <= 16; sh += 8) {
                    dc = Math.max(dc, Math.abs(((p >> sh) & 0xFF) - ((q >> sh) & 0xFF)));
                }
                int da = Math.abs(((p >>> 24) & 0xFF) - ((q >>> 24) & 0xFF));
                int v = Math.min(255, dc * DIFF_GAIN);
                int r = Math.max(v, Math.min(255, da * DIFF_GAIN));
                out.setRGB(x, y, (r << 16) | (v << 8) | v);
            }
        }
        return out;
    }

    /** One "source | six bakes" strip. */
    private static BufferedImage sheet(String title, List<BufferedImage> panels, List<String> captions) {
        int n = panels.size();
        int w = PAD * (n + 1) + CELL * n;
        int h = PAD * 2 + LABEL + CELL + 34;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x1A1A1A));
        g.fillRect(0, 0, w, h);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(Color.WHITE);
        g.drawString(title, PAD, PAD + 16);

        for (int i = 0; i < n; i++) {
            int x = PAD * (i + 1) + CELL * i;
            drawFit(g, panels.get(i), x, PAD + LABEL);
            g.setFont(new Font("SansSerif", i == 0 ? Font.BOLD : Font.PLAIN, 12));
            g.setColor(i == 0 ? new Color(0x40FF00) : new Color(0xCCCCCC));
            String cap = captions.get(i);
            g.drawString(cap.length() > 40 ? cap.substring(0, 40) : cap, x, PAD + LABEL + CELL + 15);
        }
        g.dispose();
        return img;
    }

    /** Draw {@code src} fitted into a CELL box on a mid-grey plate, so a black bake is still visible. */
    private static void drawFit(Graphics2D g, BufferedImage src, int x, int y) {
        g.setColor(new Color(0x3C3C3C));
        g.fillRect(x, y, CELL, CELL);
        if (src != null) {
            double s = Math.min(CELL / (double) src.getWidth(), CELL / (double) src.getHeight());
            int dw = Math.max(1, (int) Math.round(src.getWidth() * s));
            int dh = Math.max(1, (int) Math.round(src.getHeight() * s));
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, x + (CELL - dw) / 2, y + (CELL - dh) / 2, dw, dh, null);
        }
        g.setColor(new Color(0x555555));
        g.drawRect(x, y, CELL - 1, CELL - 1);
    }

    private static String strip(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String sha256(byte[] data) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder sb = new StringBuilder(d.length * 2);
        for (byte b : d) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
