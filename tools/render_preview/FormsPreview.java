/**
 * FormsPreview.java — preview for auto-join Arabic letter tiles (NOT shipped).
 *
 * KASHIDA method: each letter is drawn in its contextual form (initial/medial/final) at a
 * FIXED size & stroke — never stretched. Only the connecting hand (the baseline stroke on the
 * joining side) is elongated out to the tile edge by extending the real connecting stroke's own
 * pixels — so the extension is the letter's own stroke continued, not a fake bar.
 *
 * Fixed font size + fixed baseline across all tiles → neighbouring hands meet at the seam at the
 * same height & thickness = seamless.
 *
 * Run from project root:
 *   javac -encoding UTF-8 -d tools/render_preview tools/render_preview/RenderPreview.java tools/render_preview/FormsPreview.java
 *   java  -cp tools/render_preview FormsPreview
 * Output: tools/render_preview/out/CONNECTED_FORMS.png
 */
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class FormsPreview {

    static final char ZWJ  = '‍';
    static final int  CELL = 256, SS = 2, H = CELL * SS;   // 512 working size
    static final int  FG     = 0xFFFFFFFF;
    static final int  GREEN  = 0xFF1A7733;  // ba
    static final int  RED    = 0xFF881111;  // lam
    static final int  YELLOW = 0xFF887700;  // ta2
    static final int  DARK   = 0xFF0A0A0A;

    static final float RING  = H * (12f / 256f);  // 24 — black outline dilation
    static final float WHITE = H * (3f  / 256f);  //  6 — white core dilation
    static final int   INSET = 22;                // px inside the tail tip to sample (clears round cap)

    static Font  BASE;
    static float FS;     // fixed font size for ALL letters
    static float BASE_Y; // fixed baseline row for ALL letters

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        BASE = RenderPreview.ARABIC_FONT = RenderPreview.loadFont(RenderPreview.RES + "arabtype.ttf");
        new File("tools/render_preview/out").mkdirs();

        computeMetrics(new String[]{ "ب", "ل", "ت" });

        // contextual letter tiles
        BufferedImage ba_ini  = letterTile("ب", true,  false, GREEN);   // ba initial  (connects left)
        BufferedImage lam_fin = letterTile("ل", false, true,  RED);     // lam final   (connects right)
        BufferedImage lam_med = letterTile("ل", true,  true,  RED);     // lam medial  (connects both)
        BufferedImage ta2_fin = letterTile("ت", false, true,  YELLOW);  // ta2 final   (connects right)

        BufferedImage ref_bal  = tile("بل",  FG, DARK);
        BufferedImage ref_balt = tile("بلت", FG, DARK);

        final int PAD = 28, LH = 32, SG = 44;
        int W = PAD * 2 + 3 * CELL + 4;
        int Ht = PAD + 4 * (LH + CELL) + 3 * SG + PAD;
        BufferedImage canvas = new BufferedImage(W, Ht, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x1E1E1E)); g.fillRect(0, 0, W, Ht);
        Font lf = new Font("SansSerif", Font.BOLD, 13);
        int y = PAD;

        // Row 1: individual letter tiles (separated) so the hands-to-edge are visible
        label(g, lf, "Row 1 — single tiles: ba(initial) | lam(medial) | ta2(final) — hands reach the edges", PAD, y + 22);
        y += LH;
        place(g, ba_ini,  PAD,              y, CELL);
        place(g, lam_med, PAD + CELL + 14,  y, CELL);
        place(g, ta2_fin, PAD + 2*(CELL+14),y, CELL);
        y += CELL + SG;

        // Row 2: "بل" assembled — lam(red) | ba(green)
        label(g, lf, "Row 2 — \"بل\" placed touching: lam(red) | ba(green)   (yellow = seam)", PAD, y + 22);
        y += LH;
        place(g, lam_fin, PAD,            y, CELL);
        seam( g, PAD + CELL,             y, CELL);
        place(g, ba_ini,  PAD + CELL + 2, y, CELL);
        y += CELL + SG;

        // Row 3: "بلت" assembled — ta2(yellow) | lam(red) | ba(green)
        label(g, lf, "Row 3 — \"بلت\" placed touching: ta2(yellow) | lam(red) | ba(green)", PAD, y + 22);
        y += LH;
        place(g, ta2_fin, PAD,                y, CELL);
        seam( g, PAD + CELL,                  y, CELL);
        place(g, lam_med, PAD + CELL + 2,     y, CELL);
        seam( g, PAD + 2*CELL + 2,            y, CELL);
        place(g, ba_ini,  PAD + 2*CELL + 4,   y, CELL);
        y += CELL + SG;

        // Row 4: reference whole-word for comparison
        label(g, lf, "Row 4 — reference: whole-word \"بل\" and \"بلت\" (one block each)", PAD, y + 22);
        y += LH;
        place(g, ref_bal,  PAD,             y, CELL);
        place(g, ref_balt, PAD + CELL + 14, y, CELL);

        g.dispose();
        File out = new File("tools/render_preview/out/CONNECTED_FORMS.png");
        ImageIO.write(canvas, "png", out);
        System.out.println("Wrote " + out.getAbsolutePath());
    }

    /**
     * One fixed font size + one fixed baseline for ALL letters (so stroke is uniform and the
     * connecting hands line up). Size is driven by the TALLEST single letter filling the tile
     * (like the isolated blocks) — not by the sum of the tallest ascender + deepest descender.
     */
    static void computeMetrics(String[] chars) {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        float probe = H * 0.5f;
        double asc = 0, desc = 0, maxSingle = 0;
        for (String c : chars) {
            Rectangle2D r = RenderPreview.layout(c, BASE.deriveFont(probe),
                    TextAttribute.RUN_DIRECTION_RTL, Color.WHITE, frc, 0f).getOutline(null).getBounds2D();
            double ai = -r.getMinY(), di = r.getMaxY();
            asc  = Math.max(asc,  ai);
            desc = Math.max(desc, di);
            maxSingle = Math.max(maxSingle, ai + di);   // tallest single letter's own extent
        }
        double factor = (H * 0.90 - 2 * RING) / maxSingle;   // tallest letter fills ~90% of the tile
        double ascF = asc * factor, descF = desc * factor;
        double total = ascF + descF + 2 * RING;
        if (total > H * 0.98) { factor *= H * 0.98 / total; ascF = asc * factor; descF = desc * factor; }
        FS = (float) (probe * factor);
        BASE_Y = (float) ((H - (ascF + descF + 2 * RING)) / 2 + ascF + RING);
    }

    /**
     * Renders one letter in its contextual form at the FIXED size/baseline, centred. The
     * connecting hand is a kashida bar FUSED to the glyph outline (one Area) and outlined ONCE,
     * so there are no pixel-copy junctions, no ragged black notches, and a clean uniform border.
     * The bar's thickness = the font's own tatweel (kashida) thickness.
     */
    static BufferedImage letterTile(String ch, boolean connectLeft, boolean connectRight, int bg) {
        String text = (connectRight ? "" + ZWJ : "") + ch + (connectLeft ? "" + ZWJ : "");
        BufferedImage img = new BufferedImage(H, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);

        Boolean dir = TextAttribute.RUN_DIRECTION_RTL;
        Color fg = new Color(FG, true);
        FontRenderContext frc = g.getFontRenderContext();
        TextLayout tl = RenderPreview.layout(text, BASE.deriveFont(FS), dir, fg, frc, 0f);
        Shape raw = tl.getOutline(null);
        Rectangle2D b = raw.getBounds2D();
        double tx = H / 2.0 - (b.getX() + b.getWidth() / 2);  // centre ink horizontally
        double ty = BASE_Y;                                    // baseline (raw y=0) → BASE_Y
        Shape outline = AffineTransform.getTranslateInstance(tx, ty).createTransformedShape(raw);

        // Fuse the kashida bar(s) onto the glyph as ONE shape, then dilate once.
        Area shape = new Area(outline);
        double[] tb = tatweelBand(frc);                        // kashida thickness, relative to baseline
        double yTop = ty + tb[0], barH = tb[1] - tb[0];

        // Find where the glyph body actually sits at bar level so the extending bar
        // attaches at the letter's natural baseline exit — not at arbitrary tile centre.
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

    /** The font's own tatweel (kashida) vertical band relative to the baseline: {minY, maxY}. */
    static double[] tatweelBand(FontRenderContext frc) {
        Rectangle2D r = RenderPreview.layout("ـ", BASE.deriveFont(FS),
                TextAttribute.RUN_DIRECTION_RTL, Color.WHITE, frc, 0f).getOutline(null).getBounds2D();
        return new double[]{ r.getMinY(), r.getMaxY() };
    }

    static BufferedImage tile(String t, int fg, int bg) {
        return RenderPreview.fromPng(RenderPreview.render(t, fg, bg));
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
