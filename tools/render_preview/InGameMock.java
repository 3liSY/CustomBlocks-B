/**
 * InGameMock.java — renders real names as per-letter tile blocks (option 1 look),
 * composited into a Minecraft-style scene so the dev sees how it reads "in the game".
 * NOT shipped. Run from project root:
 *   javac -encoding UTF-8 -d tools/render_preview tools/render_preview/RenderPreview.java tools/render_preview/InGameMock.java
 *   java  -cp tools/render_preview InGameMock
 * Output: tools/render_preview/out/INGAME_<name>.png  (one per word)
 */
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public final class InGameMock {

    static final char ZWJ  = '‍';
    static final int  CELL = 256, SS = 2, H = CELL * SS;
    static final int  FG   = 0xFFFFFFFF;
    static final float RING  = H * (12f / 256f);
    static final float WHITE = H * (3f  / 256f);

    static Font  BASE;
    static float FS;
    static float BASE_Y;

    // Letters that join ONLY on their right side (do not connect to the next letter).
    static final Set<Character> NON_CONN = new HashSet<>();
    static {
        for (char c : "اأإآدذرزوؤةىءًٱ".toCharArray()) NON_CONN.add(c);
    }

    // A tasteful rotating palette so each block is clearly its own block (join survives colour).
    static final int[] PALETTE = {
        0xFF154360, 0xFF7B1A1A, 0xFF1A5C30, 0xFF5B2C6F,
        0xFF7D4800, 0xFF0E6655, 0xFF6B5500, 0xFF2C3E50
    };

    // word logical text, romanisation label
    static final String[][] WORDS = {
        { "علي",      "Ali"      },
        { "عبدالله",  "Abdullah" },
        { "محمد",     "Muhammad" },
        { "خالد",     "Khalid"   },
        { "مصطفى",    "Mustafa"  },
        { "لؤي",      "Lu'ay"    },
    };

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        BASE = RenderPreview.ARABIC_FONT = RenderPreview.loadFont(RenderPreview.RES + "arabtype.ttf");
        new File("tools/render_preview/out").mkdirs();

        for (String[] w : WORDS) renderScene(w[0], w[1]);
        System.out.println("Done. See tools/render_preview/out/INGAME_*.png");
    }

    static void renderScene(String word, String roman) throws Exception {
        char[] L = word.toCharArray();
        int n = L.length;

        // Per-letter joining: joinsPrev = previous is a connector; joinsNext = this is a connector & not last.
        boolean[] joinsPrev = new boolean[n];
        boolean[] joinsNext = new boolean[n];
        for (int i = 0; i < n; i++) {
            joinsPrev[i] = i > 0 && !NON_CONN.contains(L[i - 1]);
            joinsNext[i] = !NON_CONN.contains(L[i]) && i < n - 1;
        }

        // Size glyphs to THIS word's letters.
        Set<String> chars = new HashSet<>();
        for (char c : L) chars.add(String.valueOf(c));
        computeMetrics(chars.toArray(new String[0]));

        // Block render size on screen.
        int blk = Math.min(170, 1000 / Math.max(1, n));
        int gap = 0;                                   // flush, like real placed blocks
        int rowW = n * blk;
        int margin = 90;
        int W  = rowW + margin * 2;
        int top = 150;                                 // sky above the wall
        int Hc = top + blk + 230;                      // wall + ground below

        BufferedImage canvas = new BufferedImage(W, Hc, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,     RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        scene(g, W, Hc);

        int x0 = margin, y0 = top;

        // Soft shadow under the row of blocks (on the ground).
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(x0 - 24, y0 + blk - 6, rowW + 48, 70);

        // Place tiles in image order (RTL): image-left tile = logical LAST letter.
        for (int img = 0; img < n; img++) {
            int li = n - 1 - img;
            boolean cl = joinsNext[li];                // bar toward next letter (image-left)
            boolean cr = joinsPrev[li];                // bar toward prev letter (image-right)
            int bg = PALETTE[img % PALETTE.length];
            BufferedImage tile = letterTile(String.valueOf(L[li]), cl, cr, bg);
            int x = x0 + img * (blk + gap);
            g.drawImage(tile, x, y0, blk, blk, null);
            cubeEdges(g, x, y0, blk);                  // subtle bevel so it reads as a placed cube
        }

        // Caption.
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.setColor(new Color(0xFFFFFFFF, true));
        String cap = word + "   —   " + roman + "   (" + n + " blocks)";
        g.drawString(cap, margin, Hc - 34);

        g.dispose();
        File out = new File("tools/render_preview/out/INGAME_" + roman.replaceAll("[^A-Za-z]", "") + ".png");
        ImageIO.write(canvas, "png", out);
        System.out.println("Wrote " + out.getName() + "  (" + n + " blocks)");
    }

    /* ---- Minecraft-ish backdrop: sky gradient, clouds, grass+dirt ground ---- */
    static void scene(Graphics2D g, int W, int Hc) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(0x6CA6E0), 0, Hc, new Color(0xBFE0F2));
        g.setPaint(sky); g.fillRect(0, 0, W, Hc);
        // blocky clouds
        g.setColor(new Color(255, 255, 255, 220));
        cloud(g, (int)(W * 0.12), 46, 30);
        cloud(g, (int)(W * 0.62), 78, 24);
        // ground: dirt then grass cap
        int groundY = Hc - 150;
        g.setColor(new Color(0x6B4A2B)); g.fillRect(0, groundY, W, 150);     // dirt
        g.setColor(new Color(0x5A8B36)); g.fillRect(0, groundY, W, 26);      // grass top
        g.setColor(new Color(0x4C7A2E));
        for (int x = 0; x < W; x += 16) g.fillRect(x, groundY + 26, 8, 8);   // grass speckle
    }
    static void cloud(Graphics2D g, int x, int y, int s) {
        g.fillRect(x, y, s * 4, s); g.fillRect(x + s, y - s, s * 2, s);
    }

    static void cubeEdges(Graphics2D g, int x, int y, int s) {
        g.setColor(new Color(255, 255, 255, 26)); g.fillRect(x, y, s, 3);          // top highlight
        g.setColor(new Color(0, 0, 0, 55));       g.fillRect(x, y + s - 3, s, 3);  // bottom shade
        g.setColor(new Color(0, 0, 0, 30));       g.drawRect(x, y, s - 1, s - 1);  // outline
    }

    /* ---- tile rendering (ported from StressPreview) ---- */
    static void computeMetrics(String[] chars) {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        float probe = H * 0.5f;
        double maxAsc = 0, maxDesc = 0, maxW = 0;
        for (String c : chars) {
            String[] forms = { c, c + ZWJ, "" + ZWJ + c, "" + ZWJ + c + ZWJ };
            for (String form : forms) {
                Rectangle2D r = RenderPreview.layout(form, BASE.deriveFont(probe),
                        TextAttribute.RUN_DIRECTION_RTL, Color.WHITE, frc, 0f).getOutline(null).getBounds2D();
                if (r.isEmpty()) continue;
                maxAsc  = Math.max(maxAsc,  -r.getMinY());
                maxDesc = Math.max(maxDesc,   r.getMaxY());
                maxW    = Math.max(maxW,      r.getWidth());
            }
        }
        double fV  = (H * 0.94 - 2 * RING) / (maxAsc + maxDesc);
        double fH  = (H * 0.92 - 2 * RING) / maxW;
        double fac = Math.min(fV, fH);
        FS     = (float) (probe * fac);
        double ascF = maxAsc * fac, descF = maxDesc * fac;
        BASE_Y = (float) ((H - (ascF + descF + 2 * RING)) / 2 + ascF + RING);
    }

    static BufferedImage letterTile(String ch, boolean connectLeft, boolean connectRight, int bg) {
        String text = (connectRight ? "" + ZWJ : "") + ch + (connectLeft ? "" + ZWJ : "");
        BufferedImage img = new BufferedImage(H, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);

        Boolean dir = TextAttribute.RUN_DIRECTION_RTL;
        Color   fg  = new Color(FG, true);
        FontRenderContext frc = g.getFontRenderContext();
        TextLayout tl  = RenderPreview.layout(text, BASE.deriveFont(FS), dir, fg, frc, 0f);
        Shape  raw     = tl.getOutline(null);
        Rectangle2D b  = raw.getBounds2D();
        double tx = H / 2.0 - (b.getX() + b.getWidth() / 2);
        double ty = BASE_Y;
        Shape outline  = AffineTransform.getTranslateInstance(tx, ty).createTransformedShape(raw);

        Area shape = new Area(outline);
        double[] tb  = tatweelBand(frc);
        double yTop  = ty + tb[0], barH = tb[1] - tb[0];

        Area glyphInBar = new Area(outline);
        glyphInBar.intersect(new Area(new Rectangle2D.Double(-RING, yTop, H + 2 * RING, barH)));
        Rectangle2D gb  = glyphInBar.getBounds2D();
        double barLeft  = gb.isEmpty() ? H / 2.0 : gb.getMinX();
        double barRight = gb.isEmpty() ? H / 2.0 : gb.getMaxX();

        if (connectLeft)  shape.add(new Area(new Rectangle2D.Double(-RING,    yTop, barLeft + RING,        barH)));
        if (connectRight) shape.add(new Area(new Rectangle2D.Double(barRight, yTop, (H - barRight) + RING, barH)));

        g.setColor(new Color(0xFF000000, true));
        g.setStroke(new BasicStroke(2f * RING, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.fill(shape); g.draw(shape);
        g.setColor(fg);
        g.setStroke(new BasicStroke(2f * WHITE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.fill(shape); g.draw(shape);
        g.dispose();

        BufferedImage tile = new BufferedImage(CELL, CELL, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gt = tile.createGraphics();
        gt.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        gt.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        gt.setColor(new Color(bg, true));
        gt.fillRect(0, 0, CELL, CELL);
        gt.drawImage(img, 0, 0, CELL, CELL, null);
        gt.dispose();
        return tile;
    }

    static double[] tatweelBand(FontRenderContext frc) {
        Rectangle2D r = RenderPreview.layout("ـ", BASE.deriveFont(FS),
                TextAttribute.RUN_DIRECTION_RTL, Color.WHITE, frc, 0f).getOutline(null).getBounds2D();
        return new double[]{ r.getMinY(), r.getMaxY() };
    }
}
