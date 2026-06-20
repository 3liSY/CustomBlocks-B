/**
 * RenderPreview.java — HEADLESS PREVIEW HARNESS (not shipped in the jar).
 *
 * Faithful standalone copy of ArabicWordRenderer.render() math (parametrized: outline thickness,
 * fill %, letter spacing) to preview the EXACT pixels the game produces WITHOUT building the jar.
 * Tune here, eyeball the PNGs, port the winning constants back into the real ArabicWordRenderer.java.
 */

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.AttributedString;

public final class RenderPreview {

    static final int TEXTURE_SIZE = 256;
    static float STROKE_FLOOR = 0f;   // how far the stroke may shrink on long words (0 = to nothing, 1 = never)
    static Font ARABIC_FONT, LATIN_FONT;
    static final String RES = "src/main/resources/assets/customblocks/fonts/";

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        ARABIC_FONT = loadFont(RES + "arabtype.ttf");
        LATIN_FONT  = loadFont(RES + "RockwellCondensed.ttf");
        new File("tools/render_preview/out").mkdirs();
        int fg = 0xFFFFFFFF, bg = 0xFF1FA8B8; // teal bg so the BLACK outline is visible (can't judge it on black)

        // FINAL: medium floor (your pick) + fixed 1-char reference. 1 char = full stroke; long words
        // stay solid-medium. Each band goes 1 char → long so you can confirm both ends.
        STROKE_FLOOR = 0.65f;
        band("out/_e1.png", "ENGLISH  (1 char = full → long = medium)", fg, bg,
             new String[]{ "G", "Good", "Type here" });
        band("out/_a1.png", "ARABIC words", fg, bg,
             new String[]{ "ب", "مرحبا", "محمد بحر" });
        band("out/_a2.png", "ARABIC numbers", fg, bg,
             new String[]{ "٦", "٣٢١", "٥٥٥٥" });
        stack("out/FINAL_stroke.png", "out/_e1.png", "out/_a1.png", "out/_a2.png");
        System.out.println("DONE");
    }

    record Col(String label, int outline, float fill, float track, float white) {}
    record Row(String label, String text) {}

    static void grid(String out, String title, int fg, int bg, Col[] cols, Row[] rows) throws Exception {
        int cell = 190, pad = 12, gut = 96, head = 56;
        int W = gut + cols.length * (cell + pad) + pad;
        int H = head + rows.length * (cell + pad) + pad;
        BufferedImage s = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = s.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x2b2b2b)); g.fillRect(0, 0, W, H);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString(title, pad, 26);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        for (int c = 0; c < cols.length; c++)
            { g.setColor(new Color(0x9ad29a)); g.drawString(cols[c].label(), gut + c*(cell+pad) + 4, head - 8); }
        for (int r = 0; r < rows.length; r++) {
            int y = head + r*(cell+pad);
            g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.drawString(rows[r].label(), 8, y + cell/2);
            for (int c = 0; c < cols.length; c++) {
                int x = gut + c*(cell+pad);
                BufferedImage img = fromPng(render(rows[r].text(), fg, bg, cols[c].outline(), cols[c].fill(), cols[c].track(), cols[c].white()));
                g.drawImage(img, x, y, cell, cell, null);
                g.setColor(new Color(0x707070)); g.drawRect(x, y, cell, cell);
            }
        }
        g.dispose();
        ImageIO.write(s, "png", new File("tools/render_preview/" + out));
        System.out.println("wrote " + out);
    }

    /** One labelled band: a title + a row of samples, each at its LOCKED auto setting. */
    static void band(String out, String title, int fg, int bg, String[] texts) throws Exception {
        int cell = 190, pad = 12, head = 40;
        int W = pad + texts.length * (cell + pad);
        int H = head + cell + pad;
        BufferedImage s = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = s.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x2b2b2b)); g.fillRect(0, 0, W, H);
        g.setColor(new Color(0x9ad29a)); g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString(title, pad, 26);
        int x = pad;
        for (String t : texts) {
            BufferedImage img = fromPng(render(t, fg, bg));
            g.drawImage(img, x, head, cell, cell, null);
            g.setColor(new Color(0x707070)); g.drawRect(x, head, cell, cell);
            x += cell + pad;
        }
        g.dispose();
        ImageIO.write(s, "png", new File("tools/render_preview/" + out));
        System.out.println("wrote " + out);
    }

    /** Stack several PNGs vertically into one image (so a single window shows everything). */
    static void stack(String out, String... ins) throws Exception {
        BufferedImage[] imgs = new BufferedImage[ins.length];
        int W = 0, H = 0, gap = 16;
        for (int i = 0; i < ins.length; i++) {
            imgs[i] = ImageIO.read(new File("tools/render_preview/" + ins[i]));
            W = Math.max(W, imgs[i].getWidth());
            H += imgs[i].getHeight() + (i > 0 ? gap : 0);
        }
        BufferedImage s = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = s.createGraphics();
        g.setColor(new Color(0x202020)); g.fillRect(0, 0, W, H);
        int y = 0;
        for (BufferedImage im : imgs) { g.drawImage(im, 0, y, null); y += im.getHeight() + gap; }
        g.dispose();
        ImageIO.write(s, "png", new File("tools/render_preview/" + out));
        System.out.println("wrote " + out);
    }

    static BufferedImage fromPng(byte[] png) {
        try { return ImageIO.read(new java.io.ByteArrayInputStream(png)); }
        catch (Exception e) { return new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB); }
    }
    static Font loadFont(String path) throws Exception {
        try (InputStream in = Files.newInputStream(Path.of(path))) {
            return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(24f);
        }
    }

    // ── LOCKED settings (dev-approved 2026-06-15) — mirror these in ArabicWordRenderer ─────────
    static final int   LATIN_RING     = 10;      // english (dev pick 2026-06-16): outline 10
    static final float LATIN_FILL     = 0.90f;
    static final float LATIN_TRACK    = 0.08f;
    static final int   ARWORD_OUTLINE = 12;      // arabic words (dev pick): outline 12
    static final float ARWORD_WHITE   = 3f;      //   small white core so the ح bowl stays open
    static final float ARWORD_FILL    = 0.86f;
    static final int   ARNUM_OUTLINE  = 12;      // arabic numbers (dev pick): outline 12 (was 14)
    static final float ARNUM_WHITE    = 3f;      //   match letters' core so the stroke reads the same (was auto/thinner)
    static final float ARNUM_FILL     = 0.86f;
    static final float ARNUM_TRACK    = -0.18f;

    /** Auto-pick the recipe by script — this is the real shipping logic. */
    public static byte[] render(String text, int fg, int bg) {
        if (!isArabic(text))            return render(text, fg, bg, LATIN_RING,     LATIN_FILL,  LATIN_TRACK, 0f);
        if (hasArabicLetter(text))      return render(text, fg, bg, ARWORD_OUTLINE, ARWORD_FILL, 0f,          ARWORD_WHITE);
        return                                 render(text, fg, bg, ARNUM_OUTLINE,  ARNUM_FILL,  ARNUM_TRACK, ARNUM_WHITE);
    }

    // ── render math (parametrized copy of ArabicWordRenderer.render) ───────────
    public static byte[] render(String text, int textColor, int bgColor, int outlineBlack256, float fillPct, float track, float whiteCore256) {
        try {
            int outSize = TEXTURE_SIZE, ss = 2, size = outSize * ss;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,    RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(new Color(bgColor, true)); g.fillRect(0, 0, size, size);

            boolean arabic  = isArabic(text);
            boolean cursive = arabic && hasArabicLetter(text);   // joined → tracking would break the join
            float tracking  = cursive ? 0f : track;

            Font pick = arabic ? ARABIC_FONT : LATIN_FONT;
            Font base = (pick != null) ? pick : new Font("SansSerif", Font.PLAIN, 12);
            Boolean dir = arabic ? TextAttribute.RUN_DIRECTION_RTL : TextAttribute.RUN_DIRECTION_LTR;
            Color fg = new Color(textColor, true);
            FontRenderContext frc = g.getFontRenderContext();

            // Outline at FULL (single-letter) thickness; reserve it so it never clips the square.
            float ringMax = size * (outlineBlack256 / 256f);
            float whiteRatio = (whiteCore256 >= 0f) ? whiteCore256 : (outlineBlack256 * 7f / 18f);
            float whiteMax = size * (whiteRatio / 256f);
            float target = Math.max(8f, size * fillPct - 2f * ringMax);
            float trialSize = size * 0.5f;
            double l0 = outlineLongest(text, base.deriveFont(trialSize), dir, fg, frc, tracking);
            float finalSize = (l0 > 0) ? (float) (trialSize * (target / l0)) : trialSize;
            double l1 = outlineLongest(text, base.deriveFont(finalSize), dir, fg, frc, tracking);
            if (l1 > 0) finalSize = (float) (finalSize * (target / l1));
            finalSize = Math.max(8f, Math.min(finalSize, size * 8f));

            TextLayout tl = layout(text, base.deriveFont(finalSize), dir, fg, frc, tracking);
            Shape raw = tl.getOutline(null);
            Rectangle2D b = raw.getBounds2D();
            // GOLDEN RULE: scale the stroke DOWN as the ink gets shorter (more characters) so the
            // stroke-to-glyph ratio always equals a single full-height letter. f = 1 for one big glyph.
            // Stroke scale: 1.0 for a single glyph (so a 1-char block is ALWAYS the full chosen stroke,
            // regardless of the glyph's shape), shrinking as the text packs more/smaller glyphs. The
            // reference is the first glyph fitted ALONE — for a 1-char text that equals finalSize → f=1.
            String refCh = text.isEmpty() ? text : new String(Character.toChars(text.codePointAt(0)));
            double rl0 = outlineLongest(refCh, base.deriveFont(trialSize), dir, fg, frc, 0f);
            float refSize = (rl0 > 0) ? (float) (trialSize * (target / rl0)) : trialSize;
            double rl1 = outlineLongest(refCh, base.deriveFont(refSize), dir, fg, frc, 0f);
            if (rl1 > 0) refSize = (float) (refSize * (target / rl1));
            float f = (refSize > 0) ? Math.min(1f, finalSize / refSize) : 1f;
            f = Math.max(STROKE_FLOOR, f);   // floor: long words keep a solid stroke, never hairline
            float ring  = ringMax  * f;
            float white = whiteMax * f;
            double tx = (size - b.getWidth())  / 2 - b.getX();
            double ty = (size - b.getHeight()) / 2 - b.getY();
            Shape outline = AffineTransform.getTranslateInstance(tx, ty).createTransformedShape(raw);

            if (arabic) {
                // ARABIC: thin source glyph → DILATE twice (black ring then fatter white core) so it
                // reads bold like the bundled art. Both rings scaled by f (above) to keep the ratio.
                g.setColor(new Color(0xFF000000, true));
                g.setStroke(new BasicStroke(2f * ring, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.fill(outline); g.draw(outline);
                g.setColor(fg);
                g.setStroke(new BasicStroke(2f * white, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.fill(outline); g.draw(outline);
            } else {
                // LATIN: Rockwell is already bold. DO NOT dilate (it would fill the a/o/e/6/8 holes).
                // Draw a black ring as a stroke UNDER, then the white glyph FILL on top — the ring's
                // inner half is covered by the fill on the body, and the counters stay open.
                g.setColor(new Color(0xFF000000, true));
                g.setStroke(new BasicStroke(2f * ring, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(outline);
                g.setColor(fg);
                g.fill(outline);
            }
            g.dispose();

            BufferedImage out = new BufferedImage(outSize, outSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D go = out.createGraphics();
            go.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            go.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
            go.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
            go.drawImage(img, 0, 0, outSize, outSize, null); go.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(out, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("Render failed for '" + text + "': " + e.getMessage());
            return null;
        }
    }

    static TextLayout layout(String text, Font font, Boolean dir, Color fg, FontRenderContext frc, float track) {
        AttributedString as = new AttributedString(text);
        as.addAttribute(TextAttribute.FONT,          font);
        as.addAttribute(TextAttribute.RUN_DIRECTION, dir);
        as.addAttribute(TextAttribute.FOREGROUND,    fg);
        if (track != 0f) as.addAttribute(TextAttribute.TRACKING, track);
        return new TextLayout(as.getIterator(), frc);
    }
    static double outlineLongest(String text, Font font, Boolean dir, Color fg, FontRenderContext frc, float track) {
        Rectangle2D r = layout(text, font, dir, fg, frc, track).getOutline(null).getBounds2D();
        return Math.max(r.getWidth(), r.getHeight());
    }
    static boolean isArabic(String text) {
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if ((cp >= 0x0600 && cp <= 0x06FF) || (cp >= 0x0750 && cp <= 0x077F) ||
                (cp >= 0x08A0 && cp <= 0x08FF) || (cp >= 0xFB50 && cp <= 0xFDFF) ||
                (cp >= 0xFE70 && cp <= 0xFEFF)) return true;
            i += Character.charCount(cp);
        }
        return false;
    }
    static boolean hasArabicLetter(String text) {
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            boolean ar = (cp >= 0x0600 && cp <= 0x06FF) || (cp >= 0x0750 && cp <= 0x077F) ||
                         (cp >= 0x08A0 && cp <= 0x08FF) || (cp >= 0xFB50 && cp <= 0xFDFF) ||
                         (cp >= 0xFE70 && cp <= 0xFEFF);
            boolean digit = (cp >= 0x0660 && cp <= 0x0669) || (cp >= 0x06F0 && cp <= 0x06F9);
            if (ar && !digit) return true;
            i += Character.charCount(cp);
        }
        return false;
    }
}
