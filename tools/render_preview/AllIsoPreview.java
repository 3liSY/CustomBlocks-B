/**
 * AllIsoPreview.java — audit EVERY isolated letter + fine-tune dal's extra drop.
 *
 * Dev note 2026-06-19: 100%-centred grounded most letters, but dal still reads high → it wants to go a
 * little BELOW centre, on its own. Model: an isolated tile drops toward tile centre (clamped so tailed
 * letters never rise); a per-letter EXTRA can push past centre for the few that still float (dal/dhal).
 *
 * Outputs two sheets:
 *   ISO_ALL.png  — all 28 letters, ISOLATED form: left = current baseline, right = lowered (centred+extra).
 *   DAL_TUNE.png — dal isolated at 100/115/130/145% beside waw & ra, to pick dal's drop.
 *
 * Run from project root (JDK 21):
 *   "$JAVA_HOME/bin/javac" -encoding UTF-8 -d tools/render_preview \
 *       tools/render_preview/RenderPreview.java tools/render_preview/AllIsoPreview.java
 *   "$JAVA_HOME/bin/java"  -cp tools/render_preview AllIsoPreview
 */
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AllIsoPreview {

    static final char ZWJ  = '‍';
    static final int  CELL = 256, SS = 4, H = CELL * SS;
    static final int  FG   = 0xFFFFFFFF;
    static final int  BG   = 0xFF1E8C1E; // green: black ring + white core both visible
    static final float RING  = H * (12f / 256f);
    static final float WHITE = H * (3f  / 256f);

    // 28 letters with romanised labels (display order = alphabet)
    static final String[][] LET = {
        {"ا","alef"},{"ب","ba"},{"ت","ta"},{"ث","tha"},{"ج","jeem"},{"ح","ha"},{"خ","kha"},
        {"د","dal"},{"ذ","dhal"},{"ر","ra"},{"ز","zay"},{"س","seen"},{"ش","sheen"},{"ص","sad"},
        {"ض","dad"},{"ط","tah"},{"ظ","dhah"},{"ع","ain"},{"غ","ghain"},{"ف","fa"},{"ق","qaf"},
        {"ك","kaf"},{"ل","lam"},{"م","meem"},{"ن","noon"},{"ه","ha2"},{"و","waw"},{"ي","ya"}
    };

    // per-letter EXTRA drop past centre (fraction of the centring delta). 0 = stop at centre.
    static final Map<String,Double> EXTRA = new LinkedHashMap<>();
    static { EXTRA.put("د", 0.45); EXTRA.put("ذ", 0.45); } // dal / dhal go a bit below centre

    static Font  BASE;
    static float FS, BASE_Y;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        BASE = RenderPreview.ARABIC_FONT = RenderPreview.loadFont(RenderPreview.RES + "arabtype.ttf");
        new File("tools/render_preview/out").mkdirs();
        computeMetrics();
        sheetAll();
        sheetDal();
        System.out.println("DONE");
    }

    // ── ISO_ALL: every letter, baseline vs lowered ───────────────────────────
    static void sheetAll() throws Exception {
        int D = 110, PAD = 16, TITLE = 30, GAP = 6, NAMEH = 16, COLGAP = 26;
        int cellW = 2 * D + GAP;          // baseline tile + lowered tile
        int cols = 4, rows = (LET.length + cols - 1) / cols;
        int blockH = NAMEH + D + 18;
        int W  = PAD * 2 + cols * cellW + (cols - 1) * COLGAP;
        int Ht = TITLE + rows * blockH + PAD;

        BufferedImage cv = new BufferedImage(W, Ht, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = cv.createGraphics();
        hints(g);
        g.setColor(new Color(0x222222)); g.fillRect(0, 0, W, Ht);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("All 28 letters, ISOLATED form — left tile = current baseline, right tile = lowered (centred; dal/dhal extra).", PAD, 20);

        for (int idx = 0; idx < LET.length; idx++) {
            int r = idx / cols, c = idx % cols;
            int x = PAD + c * (cellW + COLGAP), y = TITLE + r * blockH;
            String ch = LET[idx][0], name = LET[idx][1];
            double extra = EXTRA.getOrDefault(ch, 0.0);
            g.setColor(new Color(0xCFE8CF)); g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.drawString(name + "  " + ch, x, y + 12);
            g.drawImage(isoTile(ch, 0.0,        BG), x,           y + NAMEH, D, D, null);   // baseline
            g.drawImage(isoTile(ch, 1.0 + extra, BG), x + D + GAP, y + NAMEH, D, D, null);   // lowered
            g.setColor(new Color(0x555555));
            g.drawRect(x, y + NAMEH, D, D); g.drawRect(x + D + GAP, y + NAMEH, D, D);
        }
        g.dispose();
        write(cv, "ISO_ALL.png");
    }

    // ── DAL_TUNE: dal at several drops, beside waw + ra ──────────────────────
    static void sheetDal() throws Exception {
        double[] amt = { 1.00, 1.15, 1.30, 1.45 };
        String[] lab = { "100% (centre)", "115%", "130%", "145%" };
        int D = 150, PAD = 18, TITLE = 30, GAP = 10, CAP = 16, REFGAP = 30;
        int W  = PAD * 2 + amt.length * (D + GAP) + REFGAP + 2 * (D + GAP);
        int Ht = TITLE + CAP + D + PAD;

        BufferedImage cv = new BufferedImage(W, Ht, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = cv.createGraphics();
        hints(g);
        g.setColor(new Color(0x222222)); g.fillRect(0, 0, W, Ht);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("dal drop — pick the amount.  (waw + ra at right for reference; they don't move)", PAD, 20);

        int x = PAD, y = TITLE + CAP;
        for (int i = 0; i < amt.length; i++) {
            g.drawImage(isoTile("د", amt[i], 0xFF0A0A0A), x, y, D, D, null);
            g.setColor(new Color(0x555555)); g.drawRect(x, y, D, D);
            g.setColor(new Color(0xBFBF7A)); g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.drawString("dal " + lab[i], x + 4, y + D + 13);
            x += D + GAP;
        }
        x += REFGAP;
        g.drawImage(isoTile("و", 0.0, 0xFFFF0000), x, y, D, D, null);
        g.setColor(new Color(0xBFBF7A)); g.drawString("waw (ref)", x + 4, y + D + 13);
        x += D + GAP;
        g.drawImage(isoTile("ر", 0.0, 0xFFF0C814), x, y, D, D, null);
        g.setColor(new Color(0xBFBF7A)); g.drawString("ra (ref)", x + 4, y + D + 13);
        g.dispose();
        write(cv, "DAL_TUNE.png");
    }

    // ── one isolated tile, lowered by lowerFrac (1.0 = centre, >1 = below centre) ──
    static BufferedImage isoTile(String ch, double lowerFrac, int bg) {
        BufferedImage img = new BufferedImage(H, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);
        Color fg = new Color(FG, true);
        FontRenderContext frc = g.getFontRenderContext();

        Shape raw = RenderPreview.layout(ch, BASE.deriveFont(FS),
                TextAttribute.RUN_DIRECTION_RTL, fg, frc, 0f).getOutline(null);
        Rectangle2D b = raw.getBounds2D();
        double tx = H / 2.0 - (b.getX() + b.getWidth() / 2);
        double dy = 0;
        if (lowerFrac > 0) {
            double inkCenterY = BASE_Y + b.getCenterY();
            double delta = Math.max(0, H / 2.0 - inkCenterY);
            dy = lowerFrac * delta;
        }
        Shape outline = AffineTransform.getTranslateInstance(tx, BASE_Y + dy).createTransformedShape(raw);

        Area shape = new Area(outline); // isolated → no kashida bars
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

    static void computeMetrics() {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        float probe = H * 0.5f;
        double asc = 0, desc = 0, maxSingle = 0;
        for (String[] e : LET) {
            String ch = e[0];
            for (String shown : new String[]{ ch, ch + ZWJ, ZWJ + ch + ZWJ, ZWJ + ch }) {
                Rectangle2D r = RenderPreview.layout(shown, BASE.deriveFont(probe),
                        TextAttribute.RUN_DIRECTION_RTL, Color.WHITE, frc, 0f).getOutline(null).getBounds2D();
                if (r.isEmpty()) continue;
                asc = Math.max(asc, -r.getMinY()); desc = Math.max(desc, r.getMaxY());
                maxSingle = Math.max(maxSingle, -r.getMinY() + r.getMaxY());
            }
        }
        double factor = (H * 0.90 - 2 * RING) / maxSingle;
        double ascF = asc * factor, descF = desc * factor;
        double total = ascF + descF + 2 * RING;
        if (total > H * 0.98) { factor *= (H * 0.98) / total; ascF = asc * factor; descF = desc * factor; }
        FS = (float) (probe * factor);
        BASE_Y = (float) ((H - (ascF + descF + 2 * RING)) / 2 + ascF + RING);
    }

    static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
    static void write(BufferedImage cv, String name) throws Exception {
        File out = new File("tools/render_preview/out/" + name);
        ImageIO.write(cv, "png", out);
        System.out.println("Wrote " + out.getName());
    }
}
