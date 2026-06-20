/**
 * RecolorToneTools.java — Group 27 §G27.7 §C3 recolour tune tools. CLIENT-ONLY.
 *
 * Responsibility: the four extra tune controls stacked under the H/S/L sliders on the live recolour
 * screen — Temperature, Contrast, a split brightness curve (Lift shadows / Lower highlights), and a row
 * of one-tap filter buttons (Gray / Sepia / Invert / Poster). Holds their state, renders them (reusing
 * {@link CbGradSlider}), routes mouse input, and exposes a single {@link #transformRgb} that both the
 * live preview and the Apply path feed through {@link com.customblocks.image.CbToneMath}. Kept separate
 * so RecolorSliderScreen stays under the 500-line gate.
 *
 * History is owned by the screen: a press / filter toggle calls {@code onBeforeChange} (the screen's
 * pushUndo) so undo covers H/S/L + tone as one snapshot.
 *
 * Depends on: CbGradSlider, CbToneMath, DrawContext.
 * Called by: client/gui/RecolorSliderScreen.
 */
package com.customblocks.client.gui;

import com.customblocks.image.CbToneMath;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class RecolorToneTools {

    private static final String[] FILTERS = { "Gray", "Sepia", "Invert", "Poster" };

    private final Runnable onBeforeChange;
    private final CbGradSlider temp      = new CbGradSlider("Temperature",      -100, 100, 0, CbGradSlider.TEMP);
    private final CbGradSlider contrast  = new CbGradSlider("Contrast",         -100, 100, 0, CbGradSlider.PLAIN);
    private final CbGradSlider shadow    = new CbGradSlider("Lift shadows",        0, 100, 0, CbGradSlider.PLAIN);
    private final CbGradSlider highlight = new CbGradSlider("Lower highlights",    0, 100, 0, CbGradSlider.PLAIN);
    private final CbGradSlider[] sliders = { temp, contrast, shadow, highlight };
    private int filter = CbToneMath.F_NONE;

    private CbGradSlider dragging;
    private int fx, fy, fbw, headX, headY;

    public RecolorToneTools(Runnable onBeforeChange) { this.onBeforeChange = onBeforeChange; }

    public void layout(int x, int y, int w) {
        headX = x; headY = y;
        int sy = y + 14;
        for (int i = 0; i < sliders.length; i++) sliders[i].set(x, sy + i * 26, w);
        fx = x; fy = sy + sliders.length * 26 + 4;
        fbw = (w - 3 * 4) / 4;
    }

    public void render(DrawContext ctx, TextRenderer tr, int mx, int my) {
        ctx.drawTextWithShadow(tr, Text.literal("§7TONE TOOLS"), headX, headY, 0xFFFFFFFF);
        for (CbGradSlider s : sliders) s.render(ctx, tr, "");
        for (int i = 0; i < FILTERS.length; i++) {
            int bx = fx + i * (fbw + 4);
            boolean on = filter == i + 1;
            boolean hover = mx >= bx && mx < bx + fbw && my >= fy && my < fy + 14;
            ctx.fill(bx, fy, bx + fbw, fy + 14, on ? 0xFF_FF_AA_00 : (hover ? 0xFF555555 : 0xFF333333));
            ctx.drawCenteredTextWithShadow(tr, Text.literal((on ? "§0" : "§f") + FILTERS[i]), bx + fbw / 2, fy + 3, 0xFFFFFFFF);
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        for (CbGradSlider s : sliders) {
            if (s.hit(mx, my)) { onBeforeChange.run(); dragging = s; s.setFromX(mx); return true; }
        }
        for (int i = 0; i < FILTERS.length; i++) {
            int bx = fx + i * (fbw + 4);
            if (mx >= bx && mx < bx + fbw && my >= fy && my < fy + 14) {
                onBeforeChange.run();
                filter = (filter == i + 1) ? CbToneMath.F_NONE : i + 1; // click active = off
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mx) {
        if (dragging == null) return false;
        dragging.setFromX(mx);
        return true;
    }

    public void mouseReleased() { dragging = null; }

    /** Run the tone pass on a single 0xRRGGBB colour (live preview). */
    public int transformRgb(int rgb) {
        return CbToneMath.applyRgb(rgb, temp.value, contrast.value, shadow.value, highlight.value, filter);
    }

    public boolean isIdentity() {
        return CbToneMath.isIdentity(temp.value, contrast.value, shadow.value, highlight.value, filter);
    }

    /** Folded into the screen's re-bake version so the preview atlas refreshes when a tool moves. */
    public long version() {
        return Math.round(temp.value) * 131L + Math.round(contrast.value) * 137L
                + Math.round(shadow.value) * 139L + Math.round(highlight.value) * 149L + filter * 151L;
    }

    // ── State for the screen's unified undo/reset/apply ─────────────────────────
    public double[] snapshot() { return new double[]{ temp.value, contrast.value, shadow.value, highlight.value, filter }; }

    public void restore(double[] s) {
        temp.value = s[0]; contrast.value = s[1]; shadow.value = s[2]; highlight.value = s[3]; filter = (int) s[4];
    }

    public void reset() { temp.value = contrast.value = shadow.value = highlight.value = 0; filter = CbToneMath.F_NONE; }

    public float temp()      { return (float) temp.value; }
    public float contrast()  { return (float) contrast.value; }
    public float shadow()    { return (float) shadow.value; }
    public float highlight() { return (float) highlight.value; }
    public int   filter()    { return filter; }
}
