/**
 * ArabicTileRenderer.java — Group 13 / Pass 4: per-letter CONNECTED-form tile (auto-join look).
 *
 * Graduates the dev-approved FormsPreview "kashida" method into the runtime. Each connected letter
 * (initial/medial/final) is drawn at a FIXED, shape-independent size + baseline (so adjacent tiles
 * line up), and the letter's own connecting hand is extended out to the tile edge by fusing a
 * kashida (tatweel) bar onto the glyph outline as ONE shape — so two touching tiles' hands meet at
 * the seam seamlessly, with no fake bar and no ragged junction. Stroke is dilated to the bundled
 * art's weight so a connected tile sits beside an ISOLATED hand-art tile without a weight jump.
 *
 * Isolated forms do NOT come here — they use the bundled PNG exactly (ArabicLetterBlockEntityRenderer).
 *
 * Depends on: ArabicWordRenderer (shared arabtype font), CustomBlocksConfig (textureSize), AWT/ImageIO
 * Called by:  ArabicLetterBlockEntityRenderer (connected-form tile pixels)
 */
package com.customblocks.arabic;

import com.customblocks.CustomBlocksConfig;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.text.AttributedString;

public final class ArabicTileRenderer {

    private ArabicTileRenderer() {}

    private static final char ZWJ = '‍';
    private static final int  SS  = 4; // supersample → crisp edges (matches ALL_LETTERS), downscaled at the end

    // Stroke weights at a 256px tile, scaled to the real size. These are the dev-approved FormsPreview
    // / InGameMock2 values — a SMALL white core keeps letter counters (the ح bowl, the ba hook, the
    // meem loop) open; bumping them closes the holes into blobs. Dev-tunable from the preview.
    private static final float RING_256  = 12f;
    private static final float WHITE_256 = 3f;

    // Group 13 isolated-floater grounding (dev-locked 2026-06-19, BeforeAfterPreview). An ISOLATED short
    // no-descender letter floats high; nudge it DOWN a little (not to the floor). Tall alef gets only a
    // tiny nudge; anything with a real tail/descender (waw/ra/noon/ain/maqsura…) is left where it sits.
    private static final double ISO_DROP      = 0.09; // short no-descender floaters (dal/dhal/ta-marbuta/round-ha/hamza)
    private static final double ISO_TALL_DROP = 0.03; // tall no-descender (alef family) — tiny nudge only

    /** Fixed, shape-independent metric (computed once): font size + baseline used for ALL letters. */
    private static volatile boolean metricsReady = false;
    private static float FS;
    private static float BASE_Y;
    private static int   H; // working (supersampled) size the metric was computed at

    /**
     * PNG bytes for one CONNECTED form of {@code letter}. {@code form} is ArabicJoining.INITIAL /
     * MEDIAL / FINAL (ISOLATED should use the bundled PNG instead). Returns null on failure.
     */
    public static byte[] render(char letter, int form, int fgArgb, int bgArgb) {
        try {
            ensureMetrics();
            int cell = CustomBlocksConfig.textureSize;
            int h = cell * SS;

            boolean connectLeft  = (form == ArabicJoining.INITIAL) || (form == ArabicJoining.MEDIAL);
            boolean connectRight = (form == ArabicJoining.FINAL)   || (form == ArabicJoining.MEDIAL);
            String text = (connectRight ? "" + ZWJ : "") + letter + (connectLeft ? "" + ZWJ : "");

            float ring  = h * (RING_256  / 256f);
            float white = h * (WHITE_256 / 256f);
            // Re-scale the cached baseline/size if the working size differs from when it was computed.
            float fs    = FS     * (h / (float) H);
            float baseY = BASE_Y * (h / (float) H);

            BufferedImage img = new BufferedImage(h, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);

            Font font = ArabicWordRenderer.arabicFont().deriveFont(fs);
            Color fg = new Color(fgArgb, true);
            FontRenderContext frc = g.getFontRenderContext();

            TextLayout tl = layout(text, font, fg, frc);
            Shape raw = tl.getOutline(null);
            Rectangle2D b = raw.getBounds2D();
            double tx = h / 2.0 - (b.getX() + b.getWidth() / 2); // centre ink horizontally
            Shape outline = AffineTransform.getTranslateInstance(tx, baseY).createTransformedShape(raw);

            // Ground an ISOLATED floater a little (see ISO_DROP). Short no-descender letters drop ISO_DROP;
            // tall alef drops a tiny ISO_TALL_DROP; tailed/bowled letters (real descender) stay put. Mirrors
            // BeforeAfterPreview.isoDrop() exactly. Connected forms never reach here with bars affected —
            // isolated has no kashida, so shifting the outline only moves the glyph.
            if (form == ArabicJoining.ISOLATED) {
                Rectangle2D ob = outline.getBounds2D();
                double desc = ob.getMaxY() - baseY, ht = ob.getHeight();
                double dy = (desc > h * 0.02) ? 0 : (ht < h * 0.62 ? h * ISO_DROP : h * ISO_TALL_DROP);
                if (dy > 0) outline = AffineTransform.getTranslateInstance(0, dy).createTransformedShape(outline);
            }

            // Fuse kashida bar(s) onto the glyph as ONE Area, attached at the letter's own baseline
            // exit, extended to the tile edge on the joining side(s).
            Area shape = new Area(outline);
            double[] tb = tatweelBand(font, fg, frc, baseY);
            double yTop = tb[0], barH = tb[1] - tb[0];
            Area glyphInBar = new Area(outline);
            glyphInBar.intersect(new Area(new Rectangle2D.Double(-ring, yTop, h + 2 * ring, barH)));
            Rectangle2D gb = glyphInBar.getBounds2D();
            double barLeft  = gb.isEmpty() ? h / 2.0 : gb.getMinX();
            double barRight = gb.isEmpty() ? h / 2.0 : gb.getMaxX();
            if (connectLeft)  shape.add(new Area(new Rectangle2D.Double(-ring,    yTop, barLeft + ring,        barH)));
            if (connectRight) shape.add(new Area(new Rectangle2D.Double(barRight, yTop, (h - barRight) + ring, barH)));

            // Dilate twice: black ring, then a fatter white core (same as the blessed word recipe).
            g.setColor(new Color(0xFF000000, true));
            g.setStroke(new BasicStroke(2f * ring, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.fill(shape); g.draw(shape);
            g.setColor(fg);
            g.setStroke(new BasicStroke(2f * white, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.fill(shape); g.draw(shape);
            g.dispose();

            // Compose onto the opaque background and downscale to the final tile size.
            BufferedImage out = new BufferedImage(cell, cell, BufferedImage.TYPE_INT_ARGB);
            Graphics2D go = out.createGraphics();
            go.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            go.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
            go.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
            go.setColor(new Color(bgArgb, true));
            go.fillRect(0, 0, cell, cell);
            go.drawImage(img, 0, 0, cell, cell, null);
            go.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(out, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    /** One fixed size + baseline for ALL letters, from a shape-independent reference set. */
    private static synchronized void ensureMetrics() {
        if (metricsReady) return;
        int cell = CustomBlocksConfig.textureSize;
        H = cell * SS;
        float ring = H * (RING_256 / 256f);
        // Metric over ALL 28 letters in their shown (medial) form — same as ALL_LETTERS, so the shared
        // baseline + size match that sheet exactly and any two tiles line up at the seam.
        Font base = ArabicWordRenderer.arabicFont();
        FontRenderContext frc = new FontRenderContext(null, true, true);
        float probe = H * 0.5f;
        double asc = 0, desc = 0, maxSingle = 0;
        // Measure EVERY form the renderer can draw (isolated/initial/medial/final), not just medial.
        // Jeem ج and Ha ح have a far deeper bowl in their ISOLATED/FINAL forms than in medial; a
        // medial-only metric placed the baseline too low and clipped those bowls off the tile bottom.
        for (ArabicLetterMap.Letter l : ArabicLetterMap.ALL) {
            String ch = l.asString();
            for (String shown : new String[]{ ch, ch + ZWJ, ZWJ + ch + ZWJ, ZWJ + ch }) {
                Rectangle2D r = layout(shown, base.deriveFont(probe), Color.WHITE, frc).getOutline(null).getBounds2D();
                if (r.isEmpty()) continue;
                double ai = -r.getMinY(), di = r.getMaxY();
                asc = Math.max(asc, ai);
                desc = Math.max(desc, di);
                maxSingle = Math.max(maxSingle, ai + di);
            }
        }
        double factor = (H * 0.90 - 2 * ring) / maxSingle; // tallest single letter fills ~90% of the tile
        double ascF = asc * factor, descF = desc * factor;
        double total = ascF + descF + 2 * ring;
        if (total > H * 0.98) { factor *= H * 0.98 / total; ascF = asc * factor; descF = desc * factor; }
        FS = (float) (probe * factor);
        BASE_Y = (float) ((H - (ascF + descF + 2 * ring)) / 2 + ascF + ring);
        metricsReady = true;
    }

    // ── isolated natural-height scale (Group 13 stuck-isolated size fix) ───────
    // The bundled isolated art is normalised to ~full tile height for every letter, so a naturally-short
    // letter (waw/dal/ra) towers over its font neighbours when stuck isolated in a word. isolatedScale gives
    // a 0..1 factor (from the font's own glyph metrics) to shrink that art to the letter's natural height.
    // Reference = the tallest isolated glyph (alef ≈ 1.0); shorter letters return < 1.0.
    private static volatile double maxIsoH = 0;
    private static final java.util.Map<Character, Double> ISO_SCALE = new java.util.HashMap<>();

    /** Natural-height scale 0..1 for {@code letter}'s ISOLATED glyph (1.0 = tallest letter; shorter < 1). */
    public static synchronized double isolatedScale(char letter) {
        if (maxIsoH <= 0) {
            for (ArabicLetterMap.Letter l : ArabicLetterMap.ALL) {
                String s = l.asString();
                if (!s.isEmpty()) maxIsoH = Math.max(maxIsoH, isoHeight(s.charAt(0)));
            }
            if (maxIsoH <= 0) maxIsoH = 1;
        }
        Double cached = ISO_SCALE.get(letter);
        if (cached != null) return cached;
        double h = isoHeight(letter);
        double s = (h > 0) ? Math.min(1.0, h / maxIsoH) : 1.0;
        ISO_SCALE.put(letter, s);
        return s;
    }

    /** Outline height (ascent+descent) of a letter's ISOLATED glyph at a fixed probe size; 0 if empty. */
    private static double isoHeight(char letter) {
        if (letter == 0) return 0;
        Font base = ArabicWordRenderer.arabicFont();
        if (base == null) return 0;
        FontRenderContext frc = new FontRenderContext(null, true, true);
        Rectangle2D r = layout(String.valueOf(letter), base.deriveFont(512f), Color.WHITE, frc)
                .getOutline(null).getBounds2D();
        return r.isEmpty() ? 0 : (-r.getMinY()) + r.getMaxY();
    }

    /** The font's own tatweel (kashida) vertical band, relative to the baseline at {@code baseY}. */
    private static double[] tatweelBand(Font font, Color fg, FontRenderContext frc, double baseY) {
        Rectangle2D r = layout("ـ", font, fg, frc).getOutline(null).getBounds2D(); // ـ tatweel
        return new double[]{ baseY + r.getMinY(), baseY + r.getMaxY() };
    }

    private static TextLayout layout(String text, Font font, Color fg, FontRenderContext frc) {
        AttributedString as = new AttributedString(text);
        as.addAttribute(TextAttribute.FONT, font);
        as.addAttribute(TextAttribute.RUN_DIRECTION, TextAttribute.RUN_DIRECTION_RTL);
        as.addAttribute(TextAttribute.FOREGROUND, fg);
        return new TextLayout(as.getIterator(), frc);
    }
}
