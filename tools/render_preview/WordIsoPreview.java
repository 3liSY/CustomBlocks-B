/**
 * WordIsoPreview.java — show ISOLATED letters IN REAL WORDS, beside their connected neighbours.
 *
 * Dev call 2026-06-19: an isolated letter stuck between auto-joining letters must look IDENTICAL to the
 * connected ones — same font, stroke, size, baseline — the ONLY difference being it has no connecting
 * hands (it's isolated). This renders several real words as a row of coloured tiles (RTL, like in-game),
 * each tile drawn EXACTLY by the runtime recipe (shared all-form metric from ArabicTileRenderer), forms
 * computed by the real ArabicJoining table. Isolated tiles are tagged "ISO" so you can confirm they match.
 *
 * Run from project root (JDK 21):
 *   "$JAVA_HOME/bin/javac" -encoding UTF-8 -d tools/render_preview \
 *       src/main/java/com/customblocks/arabic/ArabicJoining.java \
 *       tools/render_preview/RenderPreview.java tools/render_preview/WordIsoPreview.java
 *   "$JAVA_HOME/bin/java"  -cp tools/render_preview WordIsoPreview
 * Output: tools/render_preview/out/WORD_ISO.png
 */
import com.customblocks.arabic.ArabicJoining;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class WordIsoPreview {

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

    // colour cycle (bundled bg colours) so we also prove colour-independence + the in-game multi-colour look
    static final int[] BGS = { 0xFF0A0A0A, 0xFFFF0000, 0xFFF0C814, 0xFF1E8C1E };

    // Real words. Each has at least one ISOLATED letter (tagged in the render). Logical order (as typed).
    static final String[][] WORDS = {
        {"داود", "Dawud  (waw isolated between alef & dal)"},
        {"نور",  "Noor   (ra isolated beside joined noon-waw)"},
        {"ورد",  "Ward   (all isolated: waw-ra-dal)"},
        {"راشد", "Rashid (ra isolated, then sheen-dal joined)"},
        {"لؤي",  "Luay   (waw-hamza isolated)"},
        {"دوري", "Doori  (waw isolated between dal & ra-ya)"},
    };

    static Font  BASE;
    static float FS, BASE_Y;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        BASE = RenderPreview.ARABIC_FONT = RenderPreview.loadFont(RenderPreview.RES + "arabtype.ttf");
        new File("tools/render_preview/out").mkdirs();
        computeMetrics();

        int DCELL = 150, PAD = 20, TITLE = 34, LABEL = 22, TAG = 16, ROWGAP = 24;
        int maxLen = 0;
        for (String[] w : WORDS) maxLen = Math.max(maxLen, w[0].length());
        int rowH = LABEL + DCELL + TAG + ROWGAP;
        int W  = PAD * 2 + maxLen * DCELL;
        int Ht = TITLE + WORDS.length * rowH + PAD;

        BufferedImage cv = new BufferedImage(W, Ht, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = cv.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x222222)); g.fillRect(0, 0, W, Ht);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.drawString("Isolated letters IN WORDS — same font/stroke/size/baseline as connected, just no hands.  (read right-to-left)", PAD, 22);

        for (int r = 0; r < WORDS.length; r++) {
            String word = WORDS[r][0];
            int y0 = TITLE + r * rowH;
            g.setColor(new Color(0xCFE8CF)); g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString(WORDS[r][1], PAD, y0 + 15);

            char[] L = word.toCharArray();
            int n = L.length;
            for (int vis = 0; vis < n; vis++) {
                int i = n - 1 - vis;                 // RTL: visual-left = last logical char
                char self  = L[i];
                char right = (i - 1 >= 0) ? L[i - 1] : 0;   // toward word START
                char left  = (i + 1 < n)  ? L[i + 1] : 0;   // toward word END
                int form = ArabicJoining.form(self, right, left);
                boolean cl = form == ArabicJoining.INITIAL || form == ArabicJoining.MEDIAL;
                boolean cr = form == ArabicJoining.FINAL   || form == ArabicJoining.MEDIAL;

                int bg = BGS[vis % BGS.length];
                BufferedImage tile = letterTile(String.valueOf(self), cl, cr, bg);
                int x = PAD + vis * DCELL, y = y0 + LABEL;
                g.drawImage(tile, x, y, DCELL, DCELL, null);
                g.setColor(new Color(0x555555)); g.drawRect(x, y, DCELL, DCELL);

                String tag = ArabicJoining.formName(form);
                boolean iso = form == ArabicJoining.ISOLATED;
                g.setColor(iso ? new Color(0xFFD24A) : new Color(0x8AA88A));
                g.setFont(new Font("SansSerif", iso ? Font.BOLD : Font.PLAIN, 11));
                g.drawString(iso ? "ISOLATED" : tag, x + 5, y + DCELL + 13);
            }
        }
        g.dispose();
        File out = new File("tools/render_preview/out/WORD_ISO.png");
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

    /** EXACT mirror of ArabicTileRenderer.render (shared metric; hands only when connecting). */
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
