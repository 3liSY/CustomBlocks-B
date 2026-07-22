/**
 * BakeLedFont.java — THROWAWAY build-time redraw for Group 31 (BuzzerGame) Step 4 (cooler LED).
 *
 * Redraws customblocks:led — led_digits.png (240×40, ten 24×40 glyphs) + led_dot.png (12×40) — as clean,
 * uniform 7-segment glyphs. The old sheet had malformed bars (the "5" top bar stopped short); every digit here
 * is built from ONE shared segment map so all ten are consistent.
 *
 * Each digit glyph bakes three layers so the authentic LED look + the design-locked ghost-8 and bloom follow
 * /textcolor with NO extra entities (the whole glyph is tinted by the TEXT_DISPLAY colour at render time):
 *   1. GHOST  — all 7 segments in a dark grey → tints to a faint shade of the textcolor (the "ghost 8" unlit
 *               segments visible behind the lit digit).
 *   2. BLOOM  — a blurred halo of just this digit's LIT segments in mid grey → tints to a soft green glow.
 *   3. LIT    — this digit's segments crisp white → tints to the full bright textcolor.
 * Dimensions are unchanged, so font/led.json (height 9, ascent 7) still lines up on the label baseline.
 *
 * Run (JDK 21, from the CustomBlocks-B dir):
 *   "%JAVA_HOME%\bin\javac" -d tools/out tools/BakeLedFont.java
 *   "%JAVA_HOME%\bin\java"  -cp tools/out BakeLedFont
 */
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;

public final class BakeLedFont {

    private static final int CELL_W = 24, CELL_H = 40, N = 10;
    private static final Color GHOST = new Color(46, 46, 46);   // faint all-segment ghost (× textcolor)
    private static final Color BLOOM = new Color(150, 150, 150); // soft glow (× textcolor), blurred
    private static final Color LIT   = new Color(255, 255, 255); // full lit segment (× textcolor)
    private static final String OUT  = "src/main/resources/assets/customblocks/textures/font/";

    // Segment rectangles [x,y,w,h] in cell coords, order a,b,c,d,e,f,g (classic 7-segment).
    private static final int[][] SEG = {
        {5, 4, 14, 3},   // a  top
        {17, 5, 3, 13},  // b  top-right
        {17, 21, 3, 13}, // c  bottom-right
        {5, 33, 14, 3},  // d  bottom
        {4, 21, 3, 13},  // e  bottom-left
        {4, 5, 3, 13},   // f  top-left
        {5, 18, 14, 3},  // g  middle
    };
    // Lit segments per digit (a,b,c,d,e,f,g).
    private static final boolean[][] DIGIT = {
        {true,  true,  true,  true,  true,  true,  false}, // 0
        {false, true,  true,  false, false, false, false}, // 1
        {true,  true,  false, true,  true,  false, true }, // 2
        {true,  true,  true,  true,  false, false, true }, // 3
        {false, true,  true,  false, false, true,  true }, // 4
        {true,  false, true,  true,  false, true,  true }, // 5
        {true,  false, true,  true,  true,  true,  true }, // 6
        {true,  true,  true,  false, false, false, false}, // 7
        {true,  true,  true,  true,  true,  true,  true }, // 8
        {true,  true,  true,  true,  false, true,  true }, // 9
    };

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        BufferedImage digits = new BufferedImage(CELL_W * N, CELL_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = digits.createGraphics();
        for (int d = 0; d < N; d++) g.drawImage(renderDigit(DIGIT[d]), d * CELL_W, 0, null);
        g.dispose();
        write(digits, OUT + "led_digits.png");

        // Dot: a lit round dot near the baseline (segment-d row) with a soft bloom; no ghost (the dot is always on).
        BufferedImage dot = new BufferedImage(12, CELL_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gd = dot.createGraphics();
        gd.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int dia = 5, cx = 6, cy = 33;
        BufferedImage mask = new BufferedImage(12, CELL_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gm = mask.createGraphics();
        gm.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gm.setColor(LIT);
        gm.fillOval(cx - dia / 2, cy - dia / 2, dia, dia);
        gm.dispose();
        gd.drawImage(tint(blur(mask, 3), BLOOM, 0.6f), 0, 0, null);
        gd.setColor(LIT);
        gd.fillOval(cx - dia / 2, cy - dia / 2, dia, dia);
        gd.dispose();
        write(dot, OUT + "led_dot.png");

        System.out.println("LED font redrawn: led_digits.png (240x40, 10 uniform 7-seg glyphs) + led_dot.png (12x40)");
    }

    private static BufferedImage renderDigit(boolean[] lit) {
        BufferedImage img = new BufferedImage(CELL_W, CELL_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1) ghost — all segments, dark grey.
        g.setColor(GHOST);
        for (int[] s : SEG) segment(g, s);

        // 2) bloom — blurred halo of the lit segments only, mid grey.
        BufferedImage litMask = new BufferedImage(CELL_W, CELL_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gm = litMask.createGraphics();
        gm.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gm.setColor(LIT);
        for (int i = 0; i < 7; i++) if (lit[i]) segment(gm, SEG[i]);
        gm.dispose();
        g.drawImage(tint(blur(litMask, 3), BLOOM, 0.55f), 0, 0, null);

        // 3) lit — crisp white on top.
        g.setColor(LIT);
        for (int i = 0; i < 7; i++) if (lit[i]) segment(g, SEG[i]);
        g.dispose();
        return img;
    }

    private static void segment(Graphics2D g, int[] s) {
        g.fill(new RoundRectangle2D.Float(s[0], s[1], s[2], s[3], 2f, 2f));
    }

    /** Gaussian blur (keeps the same size; outer border copied). Only the resulting ALPHA is used by tint(). */
    private static BufferedImage blur(BufferedImage src, int radius) {
        int size = radius * 2 + 1;
        float[] data = new float[size * size];
        float sigma = radius / 1.5f, sum = 0f;
        int i = 0;
        for (int y = -radius; y <= radius; y++)
            for (int x = -radius; x <= radius; x++) {
                float v = (float) Math.exp(-(x * x + y * y) / (2 * sigma * sigma));
                data[i++] = v; sum += v;
            }
        for (int k = 0; k < data.length; k++) data[k] /= sum;
        ConvolveOp op = new ConvolveOp(new Kernel(size, size, data), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(src, new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB));
    }

    /** Recolour every non-transparent pixel to {@code c}, scaling its alpha by {@code alphaScale}. */
    private static BufferedImage tint(BufferedImage src, Color c, float alphaScale) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int rgb = (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
        for (int y = 0; y < src.getHeight(); y++)
            for (int x = 0; x < src.getWidth(); x++) {
                int a = src.getRGB(x, y) >>> 24;
                if (a == 0) continue;
                int na = Math.min(255, Math.round(a * alphaScale));
                out.setRGB(x, y, (na << 24) | rgb);
            }
        return out;
    }

    private static void write(BufferedImage img, String path) throws Exception {
        File f = new File(path);
        f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
    }
}
