/**
 * BakeTimerLabels.java — THROWAWAY build-time bake for Group 31 (BuzzerGame) I1 (Path B).
 *
 * Bakes the two fixed screen words الهدف / النتيجة into GREEN, transparent-background PNG glyphs for the
 * customblocks:timer_label bitmap font. Minecraft re-runs Unicode bidi on any rendered Arabic line and
 * reverses/garbles pre-shaped glyphs (I1); a baked image is bidi-immune, and a bidi-neutral PUA codepoint
 * carries it.
 *
 * 2026-07-19 third-pass changes (both are locked decisions):
 *   • ≤256px both dims. MC's font-atlas page is 256×256; a wider glyph is dropped and renders as a tofu box.
 *     The old FONT_SIZE=240 baked the words 351px / 360px wide → tofu (I1). This bake AUTO-FITS the font size
 *     down until the widest word (النتيجة) and the shared canvas both fit MAX_DIM, with the cursive stroke
 *     weight scaled proportionally so the approved look is preserved at the smaller size.
 *   • Baked GREEN, not white. The Arabic label is locked to brand green #15FF00 and is NOT tinted by
 *     /textcolor (that recolors only the LED digits). So the ink is baked green and the label component
 *     renders it untinted (see TimerDisplayVisual.lineText).
 *
 * Shaping/joining/direction comes from Java2D TextLayout with RUN_DIRECTION_RTL over arabtype.ttf — the same
 * engine as com.customblocks.arabic.ArabicWordRenderer's "ARABIC WORDS" recipe (dilate to read bold, keep the
 * cursive join, no tracking).
 *
 * Run (JDK 21, from the CustomBlocks-B dir):
 *   "%JAVA_HOME%\bin\javac" -d tools/out tools/BakeTimerLabels.java
 *   "%JAVA_HOME%\bin\java"  -cp tools/out BakeTimerLabels
 * Outputs assets/customblocks/textures/font/timer_label_target.png + _result.png and prints the
 * recommended bitmap-provider height/ascent for font/timer_label.json.
 */
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.AttributedString;

public final class BakeTimerLabels {

    // Logical-order Arabic (NOT presentation forms) — TextLayout shapes + joins these correctly.
    private static final int[] TARGET = {0x0627, 0x0644, 0x0647, 0x062F, 0x0641};              // الهدف
    private static final int[] RESULT = {0x0627, 0x0644, 0x0646, 0x062A, 0x064A, 0x062C, 0x0629}; // النتيجة

    private static final String FONT_PATH = "src/main/resources/assets/customblocks/font/arabtype.ttf";
    private static final String OUT_DIR   = "src/main/resources/assets/customblocks/textures/font/";

    // ≤256px both dims (MC atlas page). 250 leaves a safety margin under 256 (I1).
    private static final int   MAX_DIM      = 250;
    private static final float START_SIZE   = 240f;         // start big, auto-fit down
    private static final float DILATE_RATIO  = 10f / 240f;  // approved cursive weight at 240 → same ratio at any size
    private static final int   TARGET_UNITS  = 9;           // on-screen band height ≈ the LED digit font (height 9)
    // Label baked GREEN (design lock): locked brand green #15FF00, NOT tinted by /textcolor.
    private static final Color INK = new Color(0x15, 0xFF, 0x00);

    /** Everything needed to bake both words at one font size, sharing a vertical box (one baseline). */
    private record Layout(float fontSize, float dilate, int pad, int canvasH, int baselineRow, double inkH,
                          Shape tOut, Rectangle2D tb, Shape rOut, Rectangle2D rb, double topY) {
        int targetW() { return (int) Math.ceil(tb.getWidth()) + 2 * pad; }
        int resultW() { return (int) Math.ceil(rb.getWidth()) + 2 * pad; }
        int worst()   { return Math.max(Math.max(targetW(), resultW()), canvasH); }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Font baseFont = Font.createFont(Font.TRUETYPE_FONT, new File(FONT_PATH));
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);

        // Auto-fit: shrink the font until the widest word AND the shared canvas both fit MAX_DIM.
        float fontSize = START_SIZE;
        Layout lay = measure(baseFont, frc, fontSize);
        for (int i = 0; i < 16 && lay.worst() > MAX_DIM; i++) {
            fontSize *= (MAX_DIM - 1f) / lay.worst();   // scale down to fit, tiny margin, then remeasure
            lay = measure(baseFont, frc, fontSize);
        }

        bake(lay.tOut, lay.tb, lay, OUT_DIR + "timer_label_target.png");
        bake(lay.rOut, lay.rb, lay, OUT_DIR + "timer_label_result.png");

        int heightUnits = (int) Math.round(TARGET_UNITS * lay.canvasH / lay.inkH);
        int ascentUnits = (int) Math.round(heightUnits * (double) lay.baselineRow / lay.canvasH);
        System.out.println("---- timer_label.json bitmap provider values ----");
        System.out.printf("fontSize=%.1f  dilate=%.1f%n", lay.fontSize, lay.dilate);
        System.out.println("canvasH=" + lay.canvasH + "px  baselineRow=" + lay.baselineRow + "px  inkH=" + (int) lay.inkH + "px");
        System.out.println("target png = " + lay.targetW() + "x" + lay.canvasH + "px   result png = " + lay.resultW() + "x" + lay.canvasH + "px  (must be <=256)");
        System.out.println("recommended  height=" + heightUnits + "  ascent=" + ascentUnits);
    }

    /** Measure both words at a font size into a shared-baseline Layout (no file written). */
    private static Layout measure(Font baseFont, FontRenderContext frc, float fontSize) {
        Font font = baseFont.deriveFont(fontSize);
        float dilate = fontSize * DILATE_RATIO;
        int pad = (int) Math.ceil(dilate) + 4;
        Shape tOut = outline(font, frc, str(TARGET));
        Shape rOut = outline(font, frc, str(RESULT));
        Rectangle2D tb = tOut.getBounds2D();
        Rectangle2D rb = rOut.getBounds2D();
        // Shared vertical box so both words sit on the SAME baseline at the SAME height → one height/ascent pair.
        double topY = Math.min(tb.getMinY(), rb.getMinY());
        double botY = Math.max(tb.getMaxY(), rb.getMaxY());
        int canvasH = (int) Math.ceil(botY - topY) + 2 * pad;
        int baselineRow = (int) Math.round(pad - topY);
        double inkH = botY - topY;
        return new Layout(fontSize, dilate, pad, canvasH, baselineRow, inkH, tOut, tb, rOut, rb, topY);
    }

    /** Shaped, RTL outline for one word (baseline at y=0, left ink at x≈0). */
    private static Shape outline(Font font, FontRenderContext frc, String text) {
        AttributedString as = new AttributedString(text);
        as.addAttribute(TextAttribute.FONT, font);
        as.addAttribute(TextAttribute.RUN_DIRECTION, TextAttribute.RUN_DIRECTION_RTL);
        return new TextLayout(as.getIterator(), frc).getOutline(null);
    }

    /** Paint one word GREEN (fill + bold stroke) on transparent bg, sharing the layout's canvas height. */
    private static void bake(Shape raw, Rectangle2D b, Layout lay, String path) throws Exception {
        int canvasW = (int) Math.ceil(b.getWidth()) + 2 * lay.pad;
        BufferedImage img = new BufferedImage(canvasW, lay.canvasH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);

        double tx = lay.pad - b.getMinX();
        double ty = lay.pad - lay.topY;
        Shape word = AffineTransform.getTranslateInstance(tx, ty).createTransformedShape(raw);

        g.setColor(INK);
        g.fill(word);
        g.setStroke(new BasicStroke(lay.dilate, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(word);
        g.dispose();

        File out = new File(path);
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
        System.out.println("wrote " + path + "  (" + canvasW + "x" + lay.canvasH + ")");
    }

    private static String str(int[] cps) { return new String(cps, 0, cps.length); }
}
