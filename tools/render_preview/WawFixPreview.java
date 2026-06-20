/**
 * WawFixPreview.java — DIAGNOSE the stuck-isolated waw (Group 13 dev call 2026-06-19).
 *
 * Mirrors ArabicTileRenderer EXACTLY (shared all-form metric: FS + BASE_Y) and renders waw و four ways
 * on a yellow tile (matches the in-game shot), each with guide lines so we can SEE the bug:
 *   1) Bundled ART  — the hand-art PNG full-tile (the old "working" look the dev likes)
 *   2) FINAL  (font) — connected form WITH the kashida hand  ← the "working one on the left"
 *   3) ISOLATED (font, NOW) — current stuck-isolated render   ← the "right one that needs fixing"
 *   4) ISOLATED (font, FIX) — drawn on the SAME baseline+size as FINAL, just no hand
 * Guides: cyan = shared baseline (BASE_Y), red = tile bottom. Lets us compare height + descender.
 *
 * Run from project root (JDK 21):
 *   "$JAVA_HOME/bin/javac" -encoding UTF-8 -d tools/render_preview tools/render_preview/RenderPreview.java tools/render_preview/WawFixPreview.java
 *   "$JAVA_HOME/bin/java"  -cp tools/render_preview WawFixPreview
 * Output: tools/render_preview/out/WAW_FIX.png
 */
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class WawFixPreview {

    static final char ZWJ  = '‍';
    static final int  CELL = 256, SS = 4, H = CELL * SS;
    static final int  FG   = 0xFFFFFFFF;
    static final int  BG   = 0xFFF0C814; // yellow, like the in-game block

    static final float RING  = H * (12f / 256f);
    static final float WHITE = H * (3f  / 256f);

    static final String ART = "src/main/resources/assets/customblocks/arabic_art/";

    static final String[] ALL28 = {
        "ا","ب","ت","ث","ج","ح","خ","د","ذ","ر","ز",
        "س","ش","ص","ض","ط","ظ","ع","غ","ف","ق","ك",
        "ل","م","ن","ه","و","ي"
    };
    static final String WAW = "و"; // و

    static Font  BASE;
    static float FS, BASE_Y;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        BASE = RenderPreview.ARABIC_FONT = RenderPreview.loadFont(RenderPreview.RES + "arabtype.ttf");
        new File("tools/render_preview/out").mkdirs();
        computeMetrics();

        double artH = artInkHeight();                 // bundled art waw's natural ink height (tile space)
        String[] caps = { "1) bundled ART", "2) ISOLATED now (BUG)",
                          "3) FIX: grow on baseline", "4) FIX: fit-alone centered" };
        BufferedImage[] tiles = {
            artTile(WAW),
            fontTile(WAW, false, false, 0, artH),  // ISOLATED now
            fontTile(WAW, false, false, 1, artH),  // FIX: scale up about baseline → tail drops below
            fontTile(WAW, false, false, 2, artH),  // FIX: fit glyph alone, centered (art-style)
        };

        int DCELL = 230, PAD = 20, TITLE = 30, CAP = 20, GAP = 16;
        int W = PAD * 2 + tiles.length * DCELL + (tiles.length - 1) * GAP;
        int Ht = TITLE + DCELL + CAP + PAD;
        BufferedImage cv = new BufferedImage(W, Ht, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = cv.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x282828)); g.fillRect(0, 0, W, Ht);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("waw و  —  cyan = shared baseline,  red = tile bottom", PAD, 20);

        for (int i = 0; i < tiles.length; i++) {
            int x = PAD + i * (DCELL + GAP), y = TITLE;
            g.drawImage(tiles[i], x, y, DCELL, DCELL, null);
            // guides (baseline + bottom), scaled to DCELL
            int by = y + (int) Math.round(BASE_Y / (double) H * DCELL);
            g.setColor(new Color(0, 220, 220)); g.drawLine(x, by, x + DCELL, by);
            g.setColor(new Color(230, 40, 40));  g.drawLine(x, y + DCELL - 1, x + DCELL, y + DCELL - 1);
            g.setColor(new Color(0x9ad29a)); g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.drawString(caps[i], x + 4, y + DCELL + 15);
        }
        g.dispose();
        File out = new File("tools/render_preview/out/WAW_FIX.png");
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

    /** Bundled hand-art waw, full tile (the old isolated look). */
    static BufferedImage artTile(String ch) throws Exception {
        BufferedImage src = ImageIO.read(new File(ART + "yellow/waw_yellow.png"));
        BufferedImage tile = new BufferedImage(CELL, CELL, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tile.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, CELL, CELL, null);
        g.dispose();
        return tile;
    }

    /**
     * Font tile mirroring ArabicTileRenderer. {@code mode}: 0 = current (shared-small);
     * 1 = grow uniformly about the shared baseline so the ink fills like the art (tail drops below the
     * baseline); 2 = fit the glyph ALONE to ~88% of the tile, centered (the bundled-art generator style).
     */
    static BufferedImage fontTile(String ch, boolean connectLeft, boolean connectRight, int mode, double artH) {
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
        AffineTransform at = AffineTransform.getTranslateInstance(tx, BASE_Y);

        if (mode == 1) {
            // Grow the glyph uniformly about the shared baseline until its ink height ≈ the bundled art's.
            // Anchored at the baseline → the loop rises, the tail drops below the baseline (extends below),
            // and it still lines up with connected neighbours on that same baseline.
            Rectangle2D ib = at.createTransformedShape(raw).getBounds2D();
            double s = artH / ib.getHeight();
            AffineTransform grow = new AffineTransform();
            grow.translate(0, BASE_Y);
            grow.scale(s, s);
            grow.translate(0, -BASE_Y);
            at.preConcatenate(grow);
            // recentre horizontally after scaling
            Rectangle2D nb = at.createTransformedShape(raw).getBounds2D();
            at.preConcatenate(AffineTransform.getTranslateInstance(H / 2.0 - (nb.getX() + nb.getWidth() / 2), 0));
        } else if (mode == 2) {
            // Fit the glyph ALONE to ~88% of the tile, centered — exactly how the bundled art was generated.
            double target = H * 0.88 - 2 * RING;
            double s = target / Math.max(b.getWidth(), b.getHeight());
            AffineTransform fit = AffineTransform.getScaleInstance(s, s);
            Rectangle2D sb = fit.createTransformedShape(raw).getBounds2D();
            at = AffineTransform.getTranslateInstance(
                    H / 2.0 - (sb.getX() + sb.getWidth() / 2),
                    H / 2.0 - (sb.getY() + sb.getHeight() / 2));
            at.concatenate(fit);
        }
        Shape outline = at.createTransformedShape(raw);

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
        gt.setColor(new Color(BG, true)); gt.fillRect(0, 0, CELL, CELL);
        gt.drawImage(img, 0, 0, CELL, CELL, null);
        gt.dispose();
        return tile;
    }

    /** Natural ink height (in H-tile space) of the bundled art waw — its opaque rows × (H/CELL). */
    static double artInkHeight() throws Exception {
        BufferedImage src = ImageIO.read(new File(ART + "yellow/waw_yellow.png"));
        int top = -1, bot = -1, w = src.getWidth(), h = src.getHeight();
        int bg = src.getRGB(4, 4);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if (src.getRGB(x, y) != bg) { if (top < 0) top = y; bot = y; break; }
        }
        double frac = (top < 0) ? 0.86 : (bot - top + 1) / (double) h;
        return frac * H;
    }

    static double[] tatweelBand(FontRenderContext frc) {
        Rectangle2D r = RenderPreview.layout("ـ", BASE.deriveFont(FS),
                TextAttribute.RUN_DIRECTION_RTL, Color.WHITE, frc, 0f).getOutline(null).getBounds2D();
        return new double[]{ r.getMinY(), r.getMaxY() };
    }
}
