/**
 * ShrinkPreview.java — DECISION SAMPLE (not shipped).
 *
 * Shows the proposed Issue-1 fix: a stuck-isolated letter keeps its bundled hand-art PNG but is
 * scaled down to its NATURAL height (height read from arabtype.ttf, the same metric the runtime
 * ArabicTileRenderer uses), then composited back onto the full coloured block. Tall letters (alef)
 * stay full size; short letters (waw, dal, ra) shrink. Texture pixels are unchanged — only smaller.
 *
 * Three rows per word so the dev can pick by eye:
 *   Now (current)      — art drawn full-tile (the bug: short letters look too big)
 *   Fix — bottom line  — shrunk art sat on a common bottom line (word reads on one line)
 *   Fix — centered     — shrunk art floated in the middle of its block
 *
 * Run from the CustomBlocks-B project root with JDK 21:
 *   javac -encoding UTF-8 -d tools/render_preview tools/render_preview/RenderPreview.java tools/render_preview/ShrinkPreview.java
 *   java  -cp tools/render_preview ShrinkPreview
 * Output: tools/render_preview/out/SHRINK_*.png
 */
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class ShrinkPreview {

    static final String ART = "src/main/resources/assets/customblocks/arabic_art/";

    static final Map<Character,String> ART_NAME = new HashMap<>();
    static {
        ART_NAME.put('ا', "alef"); // ا
        ART_NAME.put('د', "dal");  // د
        ART_NAME.put('ر', "ra");   // ر
        ART_NAME.put('و', "waw");  // و
        ART_NAME.put('ل', "lam");  // ل  (tall reference, not in words but anchors the max)
        ART_NAME.put('ن', "noon"); // ن
    }

    // Two real words made of non-connectors, so every letter is isolated (the bug case).
    static final String DAWUD = "داود"; // داود  dal-alef-waw-dal
    static final String WARD  = "ورد";       // ورد   waw-ra-dal

    static Font BASE;
    static final Map<Character,BufferedImage> CACHE = new HashMap<>();
    static final Map<Character,Double> NAT = new HashMap<>();
    static double NATMAX = 0;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        BASE = RenderPreview.loadFont(RenderPreview.RES + "arabtype.ttf");
        new File("tools/render_preview/out").mkdirs();
        computeNatural();
        scene(DAWUD, "Dawud");
        scene(WARD,  "Ward");
        System.out.println("DONE");
    }

    /** Natural isolated-glyph height per letter, from the font. Reference max = tallest of the set. */
    static void computeNatural() {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        float probe = 600f;
        for (Character ch : ART_NAME.keySet()) {
            Rectangle2D r = RenderPreview.layout(String.valueOf(ch), BASE.deriveFont(probe),
                    TextAttribute.RUN_DIRECTION_RTL, Color.WHITE, frc, 0f).getOutline(null).getBounds2D();
            if (r.isEmpty()) continue;
            double h = (-r.getMinY()) + r.getMaxY();
            NAT.put(ch, h);
            NATMAX = Math.max(NATMAX, h);
        }
    }

    static BufferedImage art(char ch) {
        if (CACHE.containsKey(ch)) return CACHE.get(ch);
        String base = ART_NAME.get(ch);
        BufferedImage img = null;
        if (base != null) {
            File f = new File(ART + "black/" + base + "_black.png");
            try { if (f.exists()) img = ImageIO.read(f); } catch (Exception e) { /* missing */ }
        }
        CACHE.put(ch, img);
        return img;
    }

    /** mode: 0 = full (current), 1 = shrunk bottom-aligned, 2 = shrunk centered. */
    static BufferedImage tile(char ch, int mode) {
        BufferedImage src = art(ch);
        int S = 256;
        BufferedImage out = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        int bg = (src != null) ? src.getRGB(4, 4) : 0xFF0A0A0A;
        g.setColor(new Color(bg, true));
        g.fillRect(0, 0, S, S);
        if (src == null) { g.dispose(); return out; }
        if (mode == 0) {
            g.drawImage(src, 0, 0, S, S, null);
        } else {
            double rel = Math.min(1.0, NAT.getOrDefault(ch, NATMAX) / NATMAX);
            int sw = (int) Math.round(S * rel), sh = sw;
            int x = (S - sw) / 2;
            int y = (mode == 1) ? (S - sh) : (S - sh) / 2;
            g.drawImage(src, x, y, sw, sh, null);
        }
        g.dispose();
        return out;
    }

    static void scene(String word, String roman) throws Exception {
        char[] L = word.toCharArray();
        int n = L.length;
        int blk = 140, labelW = 210, pad = 24;
        int W = labelW + n * blk + pad;
        int rowH = blk + 40, top = 58, H = top + 3 * rowH + 20;
        BufferedImage cv = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = cv.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,     RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setPaint(new GradientPaint(0, 0, new Color(0x6CA6E0), 0, H, new Color(0xBFE0F2)));
        g.fillRect(0, 0, W, H);

        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        g.drawString(roman + "   (read right-to-left; short letters shrink, tall ones stay)", pad, 34);

        String[] rl   = { "Now (current)", "Fix - bottom line", "Fix - centered" };
        int[]    mode = { 0, 1, 2 };
        for (int r = 0; r < 3; r++) {
            int y0 = top + r * rowH;
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            g.setColor(Color.WHITE);
            g.drawString(rl[r], 14, y0 + blk / 2);
            for (int img = 0; img < n; img++) {
                int li = n - 1 - img;          // RTL: last logical char on the far left
                BufferedImage t = tile(L[li], mode[r]);
                int x = labelW + img * blk;
                g.drawImage(t, x, y0, blk, blk, null);
                edges(g, x, y0, blk);
            }
        }
        g.dispose();
        File out = new File("tools/render_preview/out/SHRINK_" + roman + ".png");
        ImageIO.write(cv, "png", out);
        System.out.println("Wrote " + out.getName());
    }

    static void edges(Graphics2D g, int x, int y, int s) {
        g.setColor(new Color(255, 255, 255, 26)); g.fillRect(x, y, s, 3);
        g.setColor(new Color(0, 0, 0, 55));       g.fillRect(x, y + s - 3, s, 3);
        g.setColor(new Color(0, 0, 0, 40));        g.drawRect(x, y, s - 1, s - 1);
    }
}
