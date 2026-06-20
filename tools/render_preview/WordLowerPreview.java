/**
 * WordLowerPreview.java — tune how far ISOLATED no-descender letters (alef, dal) drop.
 *
 * Dev note 2026-06-19: isolated alef/dal "float up" vs the rest of the word because they have no
 * descender, so on the shared baseline their mass sits high. Fix: lower an ISOLATED tile toward the
 * tile's vertical centre — but only the amount needed (clamped so tailed letters like waw/ra never
 * rise). Connected tiles are untouched (their bars must stay on the baseline). This shows three
 * lowering amounts (0% = current, 50%, 100% = ink-centred) on the two worst words so the dev can pick.
 *
 * Run from project root (JDK 21):
 *   "$JAVA_HOME/bin/javac" -encoding UTF-8 -d tools/render_preview \
 *       src/main/java/com/customblocks/arabic/ArabicJoining.java \
 *       tools/render_preview/RenderPreview.java tools/render_preview/WordLowerPreview.java
 *   "$JAVA_HOME/bin/java"  -cp tools/render_preview WordLowerPreview
 * Output: tools/render_preview/out/WORD_LOWER.png
 */
import com.customblocks.arabic.ArabicJoining;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class WordLowerPreview {

    static final char ZWJ  = '‍';
    static final int  CELL = 256, SS = 4, H = CELL * SS;
    static final int  FG   = 0xFFFFFFFF;
    static final float RING  = H * (12f / 256f);
    static final float WHITE = H * (3f  / 256f);

    static final String[] ALL28 = {
        "ا","ب","ت","ث","ج","ح","خ","د","ذ","ر","ز",
        "س","ش","ص","ض","ط","ظ","ع","غ","ف","ق","ك",
        "ل","م","ن","ه","و","ي"
    };
    static final int[] BGS = { 0xFF0A0A0A, 0xFFFF0000, 0xFFF0C814, 0xFF1E8C1E };

    // worst cases for floating no-descender letters
    static final String[][] WORDS = {
        {"داود", "Dawud  — alef + dal isolated"},
        {"راشد", "Rashid — alef isolated"},
        {"ورد",  "Ward   — dal isolated"},
    };
    static final double[]  AMOUNTS = { 0.0, 0.6, 0.75, 0.9, 1.0 };
    static final String[]  ALABEL  = { "current (0%)", "lower 60%", "lower 75%", "lower 90%", "lower 100% (centred)" };

    static Font  BASE;
    static float FS, BASE_Y;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        BASE = RenderPreview.ARABIC_FONT = RenderPreview.loadFont(RenderPreview.RES + "arabtype.ttf");
        new File("tools/render_preview/out").mkdirs();
        computeMetrics();

        int DCELL = 132, PAD = 18, TITLE = 30, WLABEL = 18, ALAB = 16, WGAP = 10, AGAP = 22;
        int maxLen = 0;
        for (String[] w : WORDS) maxLen = Math.max(maxLen, w[0].length());
        int variantH = ALAB + DCELL + WGAP;
        int wordBlockH = WLABEL + AMOUNTS.length * variantH + AGAP;
        int W  = PAD * 2 + maxLen * DCELL;
        int Ht = TITLE + WORDS.length * wordBlockH + PAD;

        BufferedImage cv = new BufferedImage(W, Ht, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = cv.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x222222)); g.fillRect(0, 0, W, Ht);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("Lowering ISOLATED no-descender letters (alef/dal). Tailed letters never move. RTL.", PAD, 20);

        int y = TITLE;
        for (String[] wd : WORDS) {
            String word = wd[0];
            g.setColor(new Color(0xCFE8CF)); g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString(wd[1], PAD, y + 14);
            int yv = y + WLABEL;
            for (int a = 0; a < AMOUNTS.length; a++) {
                g.setColor(new Color(0xBFBF7A)); g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g.drawString(ALABEL[a], PAD, yv + 12);
                char[] L = word.toCharArray();
                int n = L.length;
                for (int vis = 0; vis < n; vis++) {
                    int i = n - 1 - vis;
                    char self  = L[i];
                    char right = (i - 1 >= 0) ? L[i - 1] : 0;
                    char left  = (i + 1 < n)  ? L[i + 1] : 0;
                    int form = ArabicJoining.form(self, right, left);
                    boolean cl = form == ArabicJoining.INITIAL || form == ArabicJoining.MEDIAL;
                    boolean cr = form == ArabicJoining.FINAL   || form == ArabicJoining.MEDIAL;
                    boolean iso = form == ArabicJoining.ISOLATED;
                    int bg = BGS[vis % BGS.length];
                    BufferedImage tile = letterTile(String.valueOf(self), cl, cr, bg, iso ? AMOUNTS[a] : 0.0);
                    int x = PAD + vis * DCELL, ty = yv + ALAB;
                    g.drawImage(tile, x, ty, DCELL, DCELL, null);
                    g.setColor(new Color(0x555555)); g.drawRect(x, ty, DCELL, DCELL);
                }
                yv += variantH;
            }
            y += wordBlockH;
        }
        g.dispose();
        File out = new File("tools/render_preview/out/WORD_LOWER.png");
        ImageIO.write(cv, "png", out);
        System.out.println("Wrote " + out.getAbsolutePath());
    }

    static void computeMetrics() {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        float probe = H * 0.5f;
        double asc = 0, desc = 0, maxSingle = 0;
        for (String ch : ALL28)
            for (String shown : new String[]{ ch, ch + ZWJ, ZWJ + ch + ZWJ, ZWJ + ch }) {
                Rectangle2D r = RenderPreview.layout(shown, BASE.deriveFont(probe),
                        TextAttribute.RUN_DIRECTION_RTL, Color.WHITE, frc, 0f).getOutline(null).getBounds2D();
                if (r.isEmpty()) continue;
                asc = Math.max(asc, -r.getMinY()); desc = Math.max(desc, r.getMaxY());
                maxSingle = Math.max(maxSingle, -r.getMinY() + r.getMaxY());
            }
        double factor = (H * 0.90 - 2 * RING) / maxSingle;
        double ascF = asc * factor, descF = desc * factor;
        double total = ascF + descF + 2 * RING;
        if (total > H * 0.98) { factor *= (H * 0.98) / total; ascF = asc * factor; descF = desc * factor; }
        FS = (float) (probe * factor);
        BASE_Y = (float) ((H - (ascF + descF + 2 * RING)) / 2 + ascF + RING);
    }

    /**
     * Mirror of ArabicTileRenderer, plus {@code lowerFrac}: for an isolated tile, drop the glyph toward
     * the tile's vertical centre by this fraction (clamped so it only ever moves DOWN — tailed letters,
     * already centred-or-below, don't move).
     */
    static BufferedImage letterTile(String ch, boolean connectLeft, boolean connectRight, int bg, double lowerFrac) {
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
        double dy = 0;
        if (lowerFrac > 0) {
            // ink centre at baseline placement, vs tile centre → only lower if it's floating high
            double inkCenterY = BASE_Y + b.getCenterY();
            double delta = Math.max(0, H / 2.0 - inkCenterY);
            dy = lowerFrac * delta;
        }
        Shape outline = AffineTransform.getTranslateInstance(tx, BASE_Y + dy).createTransformedShape(raw);

        Area shape = new Area(outline);
        double[] tb = tatweelBand(frc);
        double yTop = BASE_Y + tb[0], barH = tb[1] - tb[0];
        Area inBar = new Area(outline); inBar.intersect(new Area(new Rectangle2D.Double(-RING, yTop, H + 2 * RING, barH)));
        Rectangle2D gb = inBar.getBounds2D();
        double barLeft = gb.isEmpty() ? H / 2.0 : gb.getMinX(), barRight = gb.isEmpty() ? H / 2.0 : gb.getMaxX();
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
        gt.setColor(new Color(bg, true)); gt.fillRect(0, 0, CELL, CELL);
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
