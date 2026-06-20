/**
 * AllLettersPreview.java — renders all 28 Arabic letters to verify none overflow their tile.
 * Joining letters shown in medial form (both bars). Non-joining shown in final form (right bar only).
 * NOT shipped with the mod. Run from project root:
 *   javac -encoding UTF-8 -d tools/render_preview tools/render_preview/RenderPreview.java tools/render_preview/AllLettersPreview.java
 *   java  -cp tools/render_preview AllLettersPreview
 * Output: tools/render_preview/out/ALL_LETTERS.png
 */
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class AllLettersPreview {

    static final char ZWJ  = '‍';
    static final int  CELL = 256, SS = 4, H = CELL * SS;   // SS=4 → 1024px working size for clean dot anti-aliasing
    static final int  FG   = 0xFFFFFFFF;

    static final float RING  = H * (12f / 256f);   // scales with H automatically
    static final float WHITE = H * (3f  / 256f);

    static Font  BASE;
    static float FS;
    static float BASE_Y;

    // {unicode char, isJoining, latin name}  — abjad order
    static final Object[][] LETTERS = {
        {"ا", false, "alef"},  {"ب", true,  "ba"},    {"ت", true,  "ta"},
        {"ث", true,  "tha"},   {"ج", true,  "jim"},   {"ح", true,  "ha"},
        {"خ", true,  "kha"},   {"د", false, "dal"},   {"ذ", false, "dhal"},
        {"ر", false, "ra"},    {"ز", false, "zayn"},  {"س", true,  "sin"},
        {"ش", true,  "shin"},  {"ص", true,  "sad"},   {"ض", true,  "dad"},
        {"ط", true,  "tah"},   {"ظ", true,  "zah"},   {"ع", true,  "ain"},
        {"غ", true,  "ghain"}, {"ف", true,  "fa"},    {"ق", true,  "qaf"},
        {"ك", true,  "kaf"},   {"ل", true,  "lam"},   {"م", true,  "mim"},
        {"ن", true,  "nun"},   {"ه", true,  "ha2"},   {"و", false, "waw"},
        {"ي", true,  "ya"},
    };

    static final int[] PALETTE = {
        0xFF1A5276, // ocean
        0xFF7B1A1A, // crimson
        0xFF1A5C30, // forest
        0xFF5B2C6F, // purple
        0xFF6B5500, // gold
        0xFF0E6655, // teal
        0xFF7D4800, // burnt orange
    };

    static final int PER_ROW = 7;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        BASE = RenderPreview.ARABIC_FONT = RenderPreview.loadFont(RenderPreview.RES + "arabtype.ttf");
        new File("tools/render_preview/out").mkdirs();

        // Pass the SHOWN form of each letter — joiners: medial, non-joiners: final (right-connecting).
        // computeMetrics sizes the font to the tallest letter actually rendered, not a phantom max.
        String[] shownForms = new String[LETTERS.length];
        for (int i = 0; i < LETTERS.length; i++) {
            String  ch   = (String)  LETTERS[i][0];
            boolean join = (Boolean) LETTERS[i][1];
            shownForms[i] = join ? (ZWJ + ch + ZWJ) : (ZWJ + ch);
        }
        computeMetrics(shownForms);

        int numRows = (LETTERS.length + PER_ROW - 1) / PER_ROW;
        final int PAD = 28, LABEL_H = 22, ROW_GAP = 10;
        int W  = PAD * 2 + PER_ROW * CELL;
        int Ht = PAD + numRows * (CELL + LABEL_H + ROW_GAP) + PAD;

        BufferedImage canvas = new BufferedImage(W, Ht, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x1E1E1E));
        g.fillRect(0, 0, W, Ht);
        Font lf = new Font("SansSerif", Font.BOLD, 11);

        for (int i = 0; i < LETTERS.length; i++) {
            String  ch    = (String)  LETTERS[i][0];
            boolean join  = (Boolean) LETTERS[i][1];
            String  name  = (String)  LETTERS[i][2];
            int col = i % PER_ROW;
            int row = i / PER_ROW;
            // RTL: letter index 0 (alef) at rightmost column
            int x   = PAD + (PER_ROW - 1 - col) * CELL;
            int y   = PAD + row * (CELL + LABEL_H + ROW_GAP);
            int bg  = PALETTE[i % PALETTE.length];

            // Joiners → medial (both bars). Non-joiners → final (right bar only — they receive but don't pass).
            BufferedImage tile = join
                    ? letterTile(ch, true,  true,  bg)
                    : letterTile(ch, false, true,  bg);
            g.drawImage(tile, x, y, CELL, CELL, null);

            // Transliteration label centred below tile
            g.setFont(lf);
            g.setColor(new Color(0x99AA99));
            String label = name + (join ? "" : " *");
            FontMetrics fm = g.getFontMetrics();
            int lx = x + (CELL - fm.stringWidth(label)) / 2;
            g.drawString(label, lx, y + CELL + 14);
        }

        // Legend
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(new Color(0x557755));
        g.drawString("* = non-joining (final form, right bar only)", PAD, Ht - 8);

        g.dispose();
        File out = new File("tools/render_preview/out/ALL_LETTERS.png");
        ImageIO.write(canvas, "png", out);
        System.out.println("Wrote " + out.getAbsolutePath());
    }

    /**
     * Sizes the font so the tallest SINGLE shown letter fills 90% of the tile height.
     * Uses FormsPreview's proven maxSingle approach — NOT the inflated maxAsc+maxDesc sum
     * which combines peaks from different letters and underestimates how big letters can be.
     * No width constraint: bars always extend to the tile edge regardless of glyph width.
     */
    static void computeMetrics(String[] shownForms) {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        float probe = H * 0.5f;
        double maxAsc = 0, maxDesc = 0, maxSingle = 0;
        for (String t : shownForms) {
            Rectangle2D r = RenderPreview.layout(t, BASE.deriveFont(probe),
                    TextAttribute.RUN_DIRECTION_RTL, Color.WHITE, frc, 0f).getOutline(null).getBounds2D();
            if (r.isEmpty()) continue;
            double ai = -r.getMinY(), di = r.getMaxY();
            maxAsc    = Math.max(maxAsc,    ai);
            maxDesc   = Math.max(maxDesc,   di);
            maxSingle = Math.max(maxSingle, ai + di);   // height of THIS letter as a unit
        }
        double factor = (H * 0.90 - 2 * RING) / maxSingle;
        double ascF = maxAsc * factor, descF = maxDesc * factor;
        double total = ascF + descF + 2 * RING;
        if (total > H * 0.98) { factor *= (H * 0.98) / total; ascF = maxAsc * factor; descF = maxDesc * factor; }
        FS     = (float) (probe * factor);
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
