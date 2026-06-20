/**
 * TileFixPreview.java — verifies the ArabicTileRenderer "all-form baseline" fix (v4).
 * Faithful standalone mirror of the FIXED ArabicTileRenderer: the shared size+baseline metric is
 * computed over EVERY form (isolated/initial/medial/final) of all 28 letters, so jeem ج and ha ح no
 * longer clip their deep bowls off the tile bottom. Displays 8 representative letters × 4 forms.
 * NOT shipped. Run from project root with JDK 21:
 *   "$JAVA_HOME/bin/javac" -encoding UTF-8 -d tools/render_preview tools/render_preview/RenderPreview.java tools/render_preview/TileFixPreview.java
 *   "$JAVA_HOME/bin/java"  -cp tools/render_preview TileFixPreview
 * Output: tools/render_preview/out/TILE_LOOK_v4.png
 */
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class TileFixPreview {

    static final char ZWJ  = '‍';
    static final int  CELL = 256, SS = 4, H = CELL * SS;
    static final int  FG   = 0xFFFFFFFF;
    static final int  BG   = 0xFF1A5C30; // forest green (same as v3) so the black ring is visible

    static final float RING  = H * (12f / 256f);
    static final float WHITE = H * (3f  / 256f);

    static Font  BASE;
    static float FS;
    static float BASE_Y;

    // ALL 28 letters — the metric must see every one (matches runtime ensureMetrics).
    static final String[] ALL28 = {
        "ا","ب","ت","ث","ج","ح","خ","د","ذ","ر","ز","س","ش","ص","ض","ط","ظ","ع",
        "غ","ف","ق","ك","ل","م","ن","ه","و","ي"
    };

    // The 8 shown in v3 (incl. the two clipped ones: jeem ج, ha ح).
    static final String[][] SHOW = {
        {"ب","ba"},  {"ج","jeem"}, {"س","seen"}, {"ل","lam"},
        {"م","meem"},{"ع","ain"},  {"ي","ya"},   {"ح","ha"},
    };

    static final String[] FORM_NAME = { "ISOLATED", "INITIAL", "MEDIAL", "FINAL" };

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        BASE = RenderPreview.ARABIC_FONT = RenderPreview.loadFont(RenderPreview.RES + "arabtype.ttf");
        new File("tools/render_preview/out").mkdirs();

        computeMetrics();

        final int DCELL = 150, PAD = 16, TITLE_H = 30, LABEL_H = 18, CAP_H = 16, ROW_GAP = 14;
        int rowH = LABEL_H + DCELL + CAP_H + ROW_GAP;
        int W  = PAD * 2 + 4 * DCELL;
        int Ht = TITLE_H + SHOW.length * rowH + PAD;

        BufferedImage canvas = new BufferedImage(W, Ht, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x282828));
        g.fillRect(0, 0, W, Ht);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("ArabicTileRenderer v4 (all-FORM baseline fix) — isolated | initial | medial | final", PAD, 20);

        for (int r = 0; r < SHOW.length; r++) {
            String ch   = SHOW[r][0];
            String name = SHOW[r][1];
            int y0 = TITLE_H + r * rowH;

            g.setColor(new Color(0xD0D0D0));
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.drawString("(" + name + ")  " + ch, PAD, y0 + 13);

            for (int f = 0; f < 4; f++) {
                boolean cl = (f == 1) || (f == 2); // INITIAL or MEDIAL
                boolean cr = (f == 3) || (f == 2); // FINAL  or MEDIAL
                BufferedImage tile = letterTile(ch, cl, cr, BG);
                int x = PAD + f * DCELL;
                int y = y0 + LABEL_H;
                g.drawImage(tile, x, y, DCELL, DCELL, null);
                g.setColor(new Color(0x8AA88A));
                g.setFont(new Font("SansSerif", Font.PLAIN, 9));
                g.drawString(FORM_NAME[f], x + 4, y + DCELL + 11);
            }
        }
        g.dispose();
        File out = new File("tools/render_preview/out/TILE_LOOK_v4.png");
        ImageIO.write(canvas, "png", out);
        System.out.println("Wrote " + out.getAbsolutePath());
    }

    /** FIXED metric: size+baseline over EVERY form of ALL 28 letters (mirrors ArabicTileRenderer.ensureMetrics). */
    static void computeMetrics() {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        float probe = H * 0.5f;
        double asc = 0, desc = 0, maxSingle = 0;
        for (String ch : ALL28) {
            for (String shown : new String[]{ ch, ch + ZWJ, ZWJ + ch + ZWJ, ZWJ + ch }) {
                Rectangle2D r = RenderPreview.layout(shown, BASE.deriveFont(probe),
                        TextAttribute.RUN_DIRECTION_RTL, Color.WHITE, frc, 0f).getOutline(null).getBounds2D();
                if (r.isEmpty()) continue;
                double ai = -r.getMinY(), di = r.getMaxY();
                asc = Math.max(asc, ai);
                desc = Math.max(desc, di);
                maxSingle = Math.max(maxSingle, ai + di);
            }
        }
        double factor = (H * 0.90 - 2 * RING) / maxSingle;
        double ascF = asc * factor, descF = desc * factor;
        double total = ascF + descF + 2 * RING;
        if (total > H * 0.98) { factor *= (H * 0.98) / total; ascF = asc * factor; descF = desc * factor; }
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

        Color fg = new Color(FG, true);
        FontRenderContext frc = g.getFontRenderContext();
        TextLayout tl = RenderPreview.layout(text, BASE.deriveFont(FS),
                TextAttribute.RUN_DIRECTION_RTL, fg, frc, 0f);
        Shape raw = tl.getOutline(null);
        Rectangle2D b = raw.getBounds2D();
        double tx = H / 2.0 - (b.getX() + b.getWidth() / 2);
        Shape outline = AffineTransform.getTranslateInstance(tx, BASE_Y).createTransformedShape(raw);

        Area shape = new Area(outline);
        double[] tb = tatweelBand(frc);
        double yTop = BASE_Y + tb[0], barH = tb[1] - tb[0];
        Area glyphInBar = new Area(outline);
        glyphInBar.intersect(new Area(new Rectangle2D.Double(-RING, yTop, H + 2 * RING, barH)));
        Rectangle2D gb = glyphInBar.getBounds2D();
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
