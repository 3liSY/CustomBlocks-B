/**
 * StressPreview.java — stress-tests the glyph-boundary bar attachment across many letter shapes.
 * NOT shipped with the mod. Run from project root:
 *   javac -encoding UTF-8 -d tools/render_preview tools/render_preview/RenderPreview.java tools/render_preview/StressPreview.java
 *   java  -cp tools/render_preview StressPreview
 * Output: tools/render_preview/out/STRESS_TEST.png
 */
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public final class StressPreview {

    static final char ZWJ  = '‍';
    static final int  CELL = 256, SS = 2, H = CELL * SS;
    static final int  FG   = 0xFFFFFFFF;

    static final float RING  = H * (12f / 256f);   // 24 — black dilation
    static final float WHITE = H * (3f  / 256f);   //  6 — white dilation

    static Font  BASE;
    static float FS;
    static float BASE_Y;

    // Palette
    static final int TEAL    = 0xFF0E6655;
    static final int PURPLE  = 0xFF5B2C6F;
    static final int ORANGE  = 0xFF7D4800;
    static final int RED     = 0xFF881111;
    static final int NAVY    = 0xFF154360;
    static final int CORAL   = 0xFF7B2D1A;
    static final int FOREST  = 0xFF1A5C30;
    static final int OCEAN   = 0xFF1A4F7A;
    static final int CRIMSON = 0xFF7B1A1A;
    static final int GOLD    = 0xFF6B5500;
    static final int SLATE   = 0xFF2C3E50;
    static final int MOSS    = 0xFF2E4A1A;

    // Per-tile spec: char, connectLeft (bar→left edge), connectRight (bar→right edge), bg color
    static final int[][] WORDS = {
        // Word 1: نبيل (nabil) — nun | ba | ya | lam
        // Image order left→right = ل fin, ي med, ب med, ن ini
        { 'ل', 0, 1, RED,
          'ي', 1, 1, ORANGE,
          'ب', 1, 1, PURPLE,
          'ن', 1, 0, TEAL },

        // Word 2: كعب (ka3b = heel/ankle) — kaf | ain | ba
        // Image order: ب fin, ع med, ك ini
        { 'ب', 0, 1, FOREST,
          'ع', 1, 1, CORAL,
          'ك', 1, 0, NAVY },

        // Word 3: سمك (samak = fish) — sin | mim | kaf
        // Image order: ك fin, م med, س ini
        { 'ك', 0, 1, PURPLE,
          'م', 1, 1, CRIMSON,
          'س', 1, 0, OCEAN },

        // Word 4: فهم (fahm = understanding) — feh | heh | mim
        // Image order: م fin, ه med, ف ini
        { 'م', 0, 1, NAVY,
          'ه', 1, 1, TEAL,
          'ف', 1, 0, GOLD },

        // Word 5: حكيم (hakim = wise) — ha | kaf | ya | mim
        // Image order: م fin, ي med, ك med, ح ini
        { 'م', 0, 1, OCEAN,
          'ي', 1, 1, RED,
          'ك', 1, 1, ORANGE,
          'ح', 1, 0, FOREST },

        // Word 6: شعبي (sha3bi = folk/popular) — shin | ain | ba | ya
        // Image order: ي fin, ب med, ع med, ش ini
        { 'ي', 0, 1, SLATE,
          'ب', 1, 1, CORAL,
          'ع', 1, 1, GOLD,
          'ش', 1, 0, CRIMSON },

        // Word 7: طيب (tayyib = good/kind) — ta | ya | ba
        // Image order: ب fin, ي med, ط ini   (ط is tall vertical — good stress test)
        { 'ب', 0, 1, MOSS,
          'ي', 1, 1, ORANGE,
          'ط', 1, 0, NAVY },
    };

    static final String[] LABELS = {
        "نبيل (nabil)    — lam fin | ya med | ba med | nun ini",
        "كعب  (ka3b)     — ba fin  | ain med | kaf ini",
        "سمك  (samak/fish)  — kaf fin | mim med | sin ini",
        "فهم  (fahm)     — mim fin | heh med | feh ini",
        "حكيم (hakim)    — mim fin | ya med  | kaf med | ha ini",
        "شعبي (sha3bi)   — ya fin  | ba med  | ain med | shin ini",
        "طيب  (tayyib)   — ba fin  | ya med  | ta(tall) ini",
    };

    // word row stride: each row in WORDS has (numLetters * 4) ints
    static final int[] WORD_LENS = { 4, 3, 3, 3, 4, 4, 3 };

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        BASE = RenderPreview.ARABIC_FONT = RenderPreview.loadFont(RenderPreview.RES + "arabtype.ttf");
        new File("tools/render_preview/out").mkdirs();

        int maxLen = 0;
        for (int len : WORD_LENS) maxLen = Math.max(maxLen, len);

        final int PAD = 28, LH = 28, SG = 36;
        int W  = PAD * 2 + maxLen * CELL + (maxLen - 1) * 2;
        int Ht = PAD + WORDS.length * (LH + CELL + SG) + PAD;

        BufferedImage canvas = new BufferedImage(W, Ht, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x1E1E1E));
        g.fillRect(0, 0, W, Ht);
        Font lf = new Font("SansSerif", Font.BOLD, 12);
        int y = PAD;

        for (int wi = 0; wi < WORDS.length; wi++) {
            int[] row = WORDS[wi];
            int   len = WORD_LENS[wi];

            // Per-word metrics: size to THIS word's chars only so letters are as large as possible.
            Set<String> wordChars = new HashSet<>();
            for (int j = 0; j < len; j++) wordChars.add(String.valueOf((char) row[j * 4]));
            computeMetrics(wordChars.toArray(new String[0]));

            label(g, lf, LABELS[wi], PAD, y + 20);
            y += LH;
            for (int j = 0; j < len; j++) {
                int base = j * 4;
                String ch       = String.valueOf((char) row[base]);
                boolean cl      = row[base + 1] == 1;
                boolean cr      = row[base + 2] == 1;
                int     color   = row[base + 3];
                BufferedImage tile = letterTile(ch, cl, cr, color);
                int x = PAD + j * (CELL + 2);
                place(g, tile, x, y, CELL);
                if (j < len - 1) seam(g, x + CELL, y, CELL);
            }
            y += CELL + SG;
        }

        g.dispose();
        File out = new File("tools/render_preview/out/STRESS_TEST.png");
        ImageIO.write(canvas, "png", out);
        System.out.println("Wrote " + out.getAbsolutePath());
    }

    static void computeMetrics(String[] chars) {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        float probe = H * 0.5f;
        // Measure ALL four contextual forms (isolated/initial/final/medial) per char so that
        // kaf and other letters whose ZWJ forms are larger than their isolated form don't overflow.
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

        // Glyph boundary at bar level → bar attaches at letter's natural exit, not tile centre
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

    static void place(Graphics2D g, BufferedImage img, int x, int y, int size) {
        g.drawImage(img, x, y, size, size, null);
    }
    static void seam(Graphics2D g, int x, int y, int h) {
        g.setColor(new Color(0xFFEE00)); g.fillRect(x, y, 1, h);
    }
    static void label(Graphics2D g, Font f, String text, int x, int y) {
        g.setFont(f); g.setColor(new Color(0x99AA99)); g.drawString(text, x, y);
    }
}
