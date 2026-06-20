/**
 * AllIsoV2Preview.java — test a GENERAL isolated-lowering rule: centre by ink CENTROID, not bbox.
 *
 * Dev 2026-06-19: dal/dhal/ta-marbuta "and relevant ones" still float when bbox-centred, because their
 * visual mass sits above the bbox centre. Centring the ink CENTROID (centre of mass) at the tile centre
 * auto-drops every top-heavy compact letter by exactly how top-heavy it is — no per-letter table. Clamped
 * lower-only so tailed letters (waw/ra/noon…) never rise. Connected tiles untouched.
 *
 * Each of the 28 letters shows three tiles: baseline | bbox-centred (old 100%) | centroid-centred (new).
 *
 * Run from project root (JDK 21):
 *   "$JAVA_HOME/bin/javac" -encoding UTF-8 -d tools/render_preview \
 *       tools/render_preview/RenderPreview.java tools/render_preview/AllIsoV2Preview.java
 *   "$JAVA_HOME/bin/java"  -cp tools/render_preview AllIsoV2Preview
 * Output: tools/render_preview/out/ISO_V2.png
 */
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class AllIsoV2Preview {

    static final char ZWJ  = '‍';
    static final int  CELL = 256, SS = 4, H = CELL * SS;
    static final int  FG   = 0xFFFFFFFF;
    static final int  BG   = 0xFF1E8C1E;
    static final float RING  = H * (12f / 256f);
    static final float WHITE = H * (3f  / 256f);

    static final String[][] LET = {
        {"ا","alef"},{"ب","ba"},{"ت","ta"},{"ث","tha"},{"ج","jeem"},{"ح","ha"},{"خ","kha"},
        {"د","dal"},{"ذ","dhal"},{"ر","ra"},{"ز","zay"},{"س","seen"},{"ش","sheen"},{"ص","sad"},
        {"ض","dad"},{"ط","tah"},{"ظ","dhah"},{"ع","ain"},{"غ","ghain"},{"ف","fa"},{"ق","qaf"},
        {"ك","kaf"},{"ل","lam"},{"م","meem"},{"ن","noon"},{"ه","ha2"},{"و","waw"},{"ي","ya"},
        {"ة","ta-marbuta"},{"ء","hamza"}
    };

    static Font  BASE;
    static float FS, BASE_Y;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        BASE = RenderPreview.ARABIC_FONT = RenderPreview.loadFont(RenderPreview.RES + "arabtype.ttf");
        new File("tools/render_preview/out").mkdirs();
        computeMetrics();

        int D = 96, PAD = 16, TITLE = 34, GAP = 4, NAMEH = 15, COLGAP = 22;
        int cellW = 3 * D + 2 * GAP;
        int cols = 3, rows = (LET.length + cols - 1) / cols;
        int blockH = NAMEH + D + 14;
        int W  = PAD * 2 + cols * cellW + (cols - 1) * COLGAP;
        int Ht = TITLE + rows * blockH + PAD;

        BufferedImage cv = new BufferedImage(W, Ht, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = cv.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x222222)); g.fillRect(0, 0, W, Ht);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("Per letter, 3 tiles: [baseline]  [bbox-centred (old)]  [centroid-centred (NEW)]. ISOLATED form.", PAD, 18);

        for (int idx = 0; idx < LET.length; idx++) {
            int r = idx / cols, c = idx % cols;
            int x = PAD + c * (cellW + COLGAP), y = TITLE + r * blockH;
            String ch = LET[idx][0], name = LET[idx][1];
            g.setColor(new Color(0xCFE8CF)); g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.drawString(name + "  " + ch, x, y + 12);
            g.drawImage(isoTile(ch, 0), x,                  y + NAMEH, D, D, null);
            g.drawImage(isoTile(ch, 1), x + (D + GAP),      y + NAMEH, D, D, null);
            g.drawImage(isoTile(ch, 2), x + 2 * (D + GAP),  y + NAMEH, D, D, null);
            g.setColor(new Color(0x444444));
            for (int k = 0; k < 3; k++) g.drawRect(x + k * (D + GAP), y + NAMEH, D, D);
        }
        g.dispose();
        File out = new File("tools/render_preview/out/ISO_V2.png");
        ImageIO.write(cv, "png", out);
        System.out.println("Wrote " + out.getName());
    }

    /** mode: 0 baseline, 1 bbox-centre (lower-only), 2 centroid-centre (lower-only). */
    static BufferedImage isoTile(String ch, int mode) {
        Color fg = new Color(FG, true);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        Shape raw = RenderPreview.layout(ch, BASE.deriveFont(FS),
                TextAttribute.RUN_DIRECTION_RTL, fg, frc, 0f).getOutline(null);
        Rectangle2D b = raw.getBounds2D();
        double tx = H / 2.0 - (b.getX() + b.getWidth() / 2);
        Shape placed = AffineTransform.getTranslateInstance(tx, BASE_Y).createTransformedShape(raw);

        double dy = 0;
        if (mode == 1) {
            double inkCenterY = placed.getBounds2D().getCenterY();
            dy = Math.max(0, H / 2.0 - inkCenterY);
        } else if (mode == 2) {
            double centroidY = centroidY(placed);
            dy = Math.max(0, H / 2.0 - centroidY);
        }
        Shape outline = AffineTransform.getTranslateInstance(0, dy).createTransformedShape(placed);

        BufferedImage img = new BufferedImage(H, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(new Color(0xFF000000, true));
        g.setStroke(new BasicStroke(2f * RING, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.fill(outline); g.draw(outline);
        g.setColor(fg);
        g.setStroke(new BasicStroke(2f * WHITE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.fill(outline); g.draw(outline);
        g.dispose();

        BufferedImage tile = new BufferedImage(CELL, CELL, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gt = tile.createGraphics();
        gt.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        gt.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        gt.setColor(new Color(BG, true)); gt.fillRect(0, 0, CELL, CELL);
        gt.drawImage(img, 0, 0, CELL, CELL, null);
        gt.dispose();
        return tile;
    }

    /** Centre-of-mass Y of the filled glyph, by rasterising the shape and averaging opaque-pixel rows. */
    static double centroidY(Shape outline) {
        BufferedImage m = new BufferedImage(H, H, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = m.createGraphics();
        g.setColor(Color.BLACK); g.fillRect(0, 0, H, H);
        g.setColor(Color.WHITE); g.fill(outline);
        g.dispose();
        long sum = 0, cnt = 0;
        // sample every 4px for speed; plenty accurate for a centroid
        for (int y = 0; y < H; y += 4)
            for (int x = 0; x < H; x += 4)
                if ((m.getRGB(x, y) & 0xFF) > 127) { sum += y; cnt++; }
        return cnt == 0 ? H / 2.0 : (double) sum / cnt;
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
}
