/**
 * ArabicWordRenderer.java
 *
 * Responsibility: Render Arabic or Latin text onto a square PNG image using Java2D.
 * Outputs PNG bytes suitable for storage in TextureStore. Font + recipe are chosen per text by
 * script (Group 13). Three recipes, all dev-approved 2026-06-15 via the headless preview harness
 * (tools/render_preview):
 *
 *   • LATIN (english letters / words / numbers) — arabtype is bold already, so DO NOT dilate
 *     (dilation closes the a/o/e/6/8 counters). Draw a thin black outline RING under a natural
 *     white FILL so counters stay open. ring 6, size .90, letter spacing .08.
 *   • ARABIC WORDS (cursive) — thin arabtype glyph, so DILATE to read bold, but keep the white
 *     core small so the ح bowl stays open. outline 12, white core 3, size .86, no tracking
 *     (tracking would break the cursive join).
 *   • ARABIC NUMBERS (digits only) — DILATE with a thick outline, pulled tight + bigger.
 *     outline 14, auto white core, size .86, tracking -0.18.
 *
 * Arabic font lookup order:
 *   1. config/customblocks/arabtype.ttf  (bundled, auto-extracted by FontAssets)
 *   2. First .ttf in config/customblocks/fonts/
 *   3. System SansSerif fallback (may lack full Arabic shaping)
 * Latin font lookup order:
 *   1. config/customblocks/fonts/RockwellCondensed.ttf  (bundled, auto-extracted)
 *   2. System SansSerif fallback
 *
 * Depends on: CustomBlocksConfig (for textureSize), FontAssets (boot extraction),
 *             standard Java AWT + ImageIO
 * Called by: ArabicBlockRegistry
 */
package com.customblocks.arabic;

import com.customblocks.CustomBlocksConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.AttributedString;

public final class ArabicWordRenderer {

    private static final Logger LOG = LoggerFactory.getLogger("CustomBlocks/Arabic");
    private static Font ARABIC_FONT = null;
    private static Font LATIN_FONT  = null;

    // ── LOCKED render settings (dev-approved 2026-06-15) ───────────────────────
    // Values are expressed at a 256px texture and scaled to the real size at render time.
    private static final int   LATIN_RING     = 10;      // english (dev pick 2026-06-16): outline 10
    private static final float LATIN_FILL     = 0.90f;
    private static final float LATIN_TRACK    = 0.08f;
    private static final int   ARWORD_OUTLINE = 12;      // arabic words (dev pick): outline 12
    private static final float ARWORD_WHITE   = 3f;      //   small white core so the ح bowl stays open
    private static final float ARWORD_FILL    = 0.86f;
    private static final float ARWORD_TRACK   = 0f;      //   cursive must stay joined
    private static final int   ARNUM_OUTLINE  = 12;      // arabic numbers (dev pick): outline 12 (was 14)
    private static final float ARNUM_WHITE    = 3f;      //   match letters' core so the stroke reads the same
    private static final float ARNUM_FILL     = 0.86f;
    private static final float ARNUM_TRACK    = -0.18f;  //   pull digits tight
    // GOLDEN RULE: 1-char block = full stroke; longer text shrinks the stroke in proportion, floored
    // here so long words stay solid (dev pick "medium", 2026-06-16) instead of going hairline-thin.
    private static final float STROKE_FLOOR   = 0.65f;

    static {
        System.setProperty("java.awt.headless", "true");
        ARABIC_FONT = loadArabicFont();
        LATIN_FONT  = loadLatinFont();
    }

    private ArabicWordRenderer() {}

    /** The loaded arabtype font (or system fallback) — shared with ArabicTileRenderer (auto-join). */
    public static Font arabicFont() {
        return (ARABIC_FONT != null) ? ARABIC_FONT : new Font("SansSerif", Font.PLAIN, 24);
    }

    /**
     * Render text onto a square image of textureSize. Colors are ARGB ints.
     * The recipe (font, outline, dilation, size, spacing) is auto-picked by script. Returns PNG
     * bytes, or null on failure.
     */
    public static byte[] render(String text, int textColor, int bgColor) {
        try {
            int outSize = CustomBlocksConfig.textureSize;
            int ss = 2;                          // 2x supersample → crisp, bold edges; downscaled at the end
            int size = outSize * ss;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,    RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

            // Background (opaque). The bundled letter art uses #0A0A0A for "black"; we honour
            // whatever bg is passed so callers can match it.
            g.setColor(new Color(bgColor, true));
            g.fillRect(0, 0, size, size);

            // Pick recipe by script: latin (clean ring), arabic word (dilate, open ح), arabic number.
            boolean arabic  = isArabic(text);
            boolean cursive = arabic && hasArabicLetter(text);  // joined script → no tracking
            boolean dilate;
            int   outline256; float fillPct, track, whiteCore256;
            if (!arabic) {
                dilate = false; outline256 = LATIN_RING;     fillPct = LATIN_FILL;  track = LATIN_TRACK;  whiteCore256 = 0f;
            } else if (cursive) {
                dilate = true;  outline256 = ARWORD_OUTLINE;  fillPct = ARWORD_FILL; track = ARWORD_TRACK; whiteCore256 = ARWORD_WHITE;
            } else {
                dilate = true;  outline256 = ARNUM_OUTLINE;   fillPct = ARNUM_FILL;  track = ARNUM_TRACK;  whiteCore256 = ARNUM_WHITE;
            }

            // arabtype performs Arabic shaping, so a whole word joins cursively here.
            Font pick = arabic ? ARABIC_FONT : LATIN_FONT;
            Font base = (pick != null) ? pick : new Font("SansSerif", Font.PLAIN, 12);
            Boolean dir = arabic ? TextAttribute.RUN_DIRECTION_RTL : TextAttribute.RUN_DIRECTION_LTR;
            Color fg = new Color(textColor, true);
            FontRenderContext frc = g.getFontRenderContext();

            // Outline at FULL (single-letter) thickness; reserve it so it never clips the square.
            float ringMax  = size * (outline256 / 256f);
            float whiteRatio = (whiteCore256 >= 0f) ? whiteCore256 : (outline256 * 7f / 18f);
            float whiteMax = size * (whiteRatio / 256f);
            float target = Math.max(8f, size * fillPct - 2f * ringMax);
            float trialSize = size * 0.5f;
            double l0 = outlineLongest(text, base.deriveFont(trialSize), dir, fg, frc, track);
            float finalSize = (l0 > 0) ? (float) (trialSize * (target / l0)) : trialSize;
            double l1 = outlineLongest(text, base.deriveFont(finalSize), dir, fg, frc, track);
            if (l1 > 0) finalSize = (float) (finalSize * (target / l1)); // refine against measured ink
            finalSize = Math.max(8f, Math.min(finalSize, size * 8f));

            // Build the shaped outline and centre it on its real ink bounds.
            TextLayout tl = layout(text, base.deriveFont(finalSize), dir, fg, frc, track);
            Shape raw = tl.getOutline(null);
            Rectangle2D b = raw.getBounds2D();
            double tx = (size - b.getWidth())  / 2 - b.getX();
            double ty = (size - b.getHeight()) / 2 - b.getY();
            Shape outline = AffineTransform.getTranslateInstance(tx, ty).createTransformedShape(raw);

            // GOLDEN RULE: a 1-char block uses the FULL stroke; as more/smaller glyphs pack in, the
            // stroke shrinks in proportion (floored at STROKE_FLOOR so long words stay solid, not
            // hairline). Reference = the first glyph fitted ALONE, so 1-char text → f = 1 exactly,
            // regardless of the glyph's shape.
            String refCh = text.isEmpty() ? text : new String(Character.toChars(text.codePointAt(0)));
            double rl0 = outlineLongest(refCh, base.deriveFont(trialSize), dir, fg, frc, track);
            float refSize = (rl0 > 0) ? (float) (trialSize * (target / rl0)) : trialSize;
            double rl1 = outlineLongest(refCh, base.deriveFont(refSize), dir, fg, frc, track);
            if (rl1 > 0) refSize = (float) (refSize * (target / rl1));
            float f = (refSize > 0) ? Math.min(1f, finalSize / refSize) : 1f;
            f = Math.max(STROKE_FLOOR, f);
            float ring  = ringMax  * f;
            float white = whiteMax * f;

            if (dilate) {
                // ARABIC: thin source glyph → dilate twice (black ring then a fatter white core) so it
                // reads bold like the bundled art. Both scaled by f so the stroke ratio matches 1 char.
                g.setColor(new Color(0xFF000000, true));
                g.setStroke(new BasicStroke(2f * ring, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.fill(outline); g.draw(outline);
                g.setColor(fg);
                g.setStroke(new BasicStroke(2f * white, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.fill(outline); g.draw(outline);
            } else {
                // LATIN: Rockwell is already bold. DO NOT dilate (it would fill the a/o/e/6/8 holes).
                // Black ring as a stroke UNDER, then the white glyph FILL on top — the ring's inner half
                // is covered by the fill on the body, so the counters stay open.
                g.setColor(new Color(0xFF000000, true));
                g.setStroke(new BasicStroke(2f * ring, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(outline);
                g.setColor(fg);
                g.fill(outline);
            }
            g.dispose();

            // Downscale the supersampled render to the final texture size with quality interpolation.
            BufferedImage out = img;
            if (ss != 1) {
                out = new BufferedImage(outSize, outSize, BufferedImage.TYPE_INT_ARGB);
                Graphics2D go = out.createGraphics();
                go.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                go.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
                go.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
                go.drawImage(img, 0, 0, outSize, outSize, null);
                go.dispose();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(out, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            LOG.error("[CustomBlocks/Arabic] Render failed for '{}': {}", text, e.getMessage());
            return null;
        }
    }

    /** Build a TextLayout for the shaped, directioned, coloured run at the given font + tracking. */
    private static TextLayout layout(String text, Font font, Boolean dir, Color fg, FontRenderContext frc, float track) {
        AttributedString as = new AttributedString(text);
        as.addAttribute(TextAttribute.FONT,          font);
        as.addAttribute(TextAttribute.RUN_DIRECTION, dir);
        as.addAttribute(TextAttribute.FOREGROUND,    fg);
        if (track != 0f) as.addAttribute(TextAttribute.TRACKING, track);
        return new TextLayout(as.getIterator(), frc);
    }

    /** Longest visual dimension (px) of the shaped glyph ink at the given font — used by the auto-fit. */
    private static double outlineLongest(String text, Font font, Boolean dir, Color fg, FontRenderContext frc, float track) {
        Rectangle2D r = layout(text, font, dir, fg, frc, track).getOutline(null).getBounds2D();
        return Math.max(r.getWidth(), r.getHeight());
    }

    /** True if the text contains any Arabic-script codepoint (main + supplement + presentation forms). */
    private static boolean isArabic(String text) {
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if ((cp >= 0x0600 && cp <= 0x06FF) ||   // Arabic
                (cp >= 0x0750 && cp <= 0x077F) ||   // Arabic Supplement
                (cp >= 0x08A0 && cp <= 0x08FF) ||   // Arabic Extended-A
                (cp >= 0xFB50 && cp <= 0xFDFF) ||   // Arabic Presentation Forms-A
                (cp >= 0xFE70 && cp <= 0xFEFF)) {   // Arabic Presentation Forms-B
                return true;
            }
            i += Character.charCount(cp);
        }
        return false;
    }

    /** True if the text contains an Arabic LETTER (cursive) — excludes the Arabic-Indic digits. */
    private static boolean hasArabicLetter(String text) {
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

    /** Arabic font: arabtype.ttf (config), then any .ttf in fonts/, then system SansSerif. */
    private static Font loadArabicFont() {
        // 1. arabtype.ttf beside config folder (auto-extracted by FontAssets)
        Path p1 = Path.of("config/customblocks/arabtype.ttf");
        if (Files.exists(p1)) {
            try (InputStream in = Files.newInputStream(p1)) {
                LOG.info("[CustomBlocks/Arabic] Loaded arabtype.ttf from {}", p1);
                return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(24f);
            } catch (Exception e) {
                LOG.warn("[CustomBlocks/Arabic] Failed to load arabtype.ttf: {}", e.getMessage());
            }
        }
        // 2. Any .ttf in config/customblocks/fonts/ (skip the Latin Rockwell font)
        Path fontDir = Path.of("config/customblocks/fonts");
        if (Files.isDirectory(fontDir)) {
            try {
                var found = Files.list(fontDir)
                        .filter(p -> p.toString().endsWith(".ttf"))
                        .filter(p -> !p.getFileName().toString().equalsIgnoreCase("RockwellCondensed.ttf"))
                        .findFirst();
                if (found.isPresent()) {
                    try (InputStream in = Files.newInputStream(found.get())) {
                        LOG.info("[CustomBlocks/Arabic] Loaded Arabic font from {}", found.get());
                        return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(24f);
                    }
                }
            } catch (Exception e) {
                LOG.warn("[CustomBlocks/Arabic] Font dir scan failed: {}", e.getMessage());
            }
        }
        // 3. System fallback
        LOG.info("[CustomBlocks/Arabic] No Arabic font found — using system SansSerif. " +
                 "Place arabtype.ttf in config/customblocks/ for proper Arabic shaping.");
        return new Font("SansSerif", Font.PLAIN, 24);
    }

    /** Latin font: Rockwell Condensed (config/customblocks/fonts/), then system SansSerif. */
    private static Font loadLatinFont() {
        Path p = Path.of("config/customblocks/fonts/RockwellCondensed.ttf");
        if (Files.exists(p)) {
            try (InputStream in = Files.newInputStream(p)) {
                LOG.info("[CustomBlocks/Arabic] Loaded RockwellCondensed.ttf from {}", p);
                return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(24f);
            } catch (Exception e) {
                LOG.warn("[CustomBlocks/Arabic] Failed to load RockwellCondensed.ttf: {}", e.getMessage());
            }
        }
        LOG.info("[CustomBlocks/Arabic] Rockwell font not found — English text uses system SansSerif.");
        return new Font("SansSerif", Font.PLAIN, 24);
    }
}
