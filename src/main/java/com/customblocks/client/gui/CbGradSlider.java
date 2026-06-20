/**
 * CbGradSlider.java — Group 27 §G27.7 §C2 gradient slider track. CLIENT-ONLY.
 *
 * Responsibility: a horizontal drag slider whose track is a real colour gradient that communicates what
 * the slider does (hue = rainbow, saturation = grey→vivid, lightness = black→white, temperature =
 * cool→warm, plain = neutral ramp), with a bigger knob and a value chip. Shared by the live recolour
 * screen's H/S/L sliders and the §C3 tone tools so they look and behave identically.
 *
 * Depends on: DrawContext, TextRenderer, MathHelper (HSV→RGB).
 * Called by: client/gui/RecolorSliderScreen, client/gui/RecolorToneTools.
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class CbGradSlider {

    public static final int HUE = 0, SAT = 1, LIGHT = 2, TEMP = 3, PLAIN = 4;

    public final String label;
    public final double min, max;
    public final int kind;
    public double value;
    public int x, y, w;

    public CbGradSlider(String label, double min, double max, double value, int kind) {
        this.label = label; this.min = min; this.max = max; this.value = value; this.kind = kind;
    }

    public void set(int x, int y, int w) { this.x = x; this.y = y; this.w = w; }

    public boolean hit(double mx, double my) { return mx >= x && mx <= x + w && my >= y - 3 && my <= y + 14; }

    public void setFromX(double mx) {
        double f = Math.max(0, Math.min(1, (mx - x) / w));
        value = min + f * (max - min);
    }

    public void reset(double v) { value = v; }

    /** Colour shown at track fraction {@code f} (0..1). */
    private int trackColor(double f) {
        return switch (kind) {
            case HUE   -> MathHelper.hsvToRgb((float) f, 1f, 1f);
            case SAT   -> MathHelper.hsvToRgb(0.55f, (float) f, 1f);
            case TEMP  -> f < 0.5 ? lerp(0x3060FF, 0x888888, f * 2) : lerp(0x888888, 0xFF6030, (f - 0.5) * 2);
            case PLAIN -> lerp(0x202020, 0xC0C0C0, f);
            default    -> { int g = (int) Math.round(f * 255); yield (g << 16) | (g << 8) | g; } // LIGHT: black→white
        };
    }

    public void render(DrawContext ctx, TextRenderer tr, String unit) {
        ctx.drawTextWithShadow(tr, Text.literal("§f" + label), x, y - 13, 0xFFFFFFFF);
        int t0 = y + 1, t1 = y + 9;
        ctx.fill(x - 1, t0 - 1, x + w + 1, t1 + 1, 0xFF000000); // track border
        for (int sx = 0; sx < w; sx += 2) {
            int c = 0xFF000000 | (trackColor(sx / (double) w) & 0xFFFFFF);
            ctx.fill(x + sx, t0, x + Math.min(w, sx + 2), t1, c);
        }
        double f = (value - min) / (max - min);
        int knob = x + (int) Math.round(f * w);
        ctx.fill(knob - 3, y - 3, knob + 3, y + 13, 0xFF000000);
        ctx.fill(knob - 2, y - 2, knob + 2, y + 12, 0xFFFFFFFF);
        // Value chip to the right of the track.
        String chip = (int) Math.round(value) + unit;
        int cw = tr.getWidth(chip) + 6, cx = x + w + 6;
        ctx.fill(cx - 1, y - 2, cx + cw + 1, y + 11, 0xFF000000);
        ctx.fill(cx, y - 1, cx + cw, y + 10, 0xFF202020);
        ctx.drawTextWithShadow(tr, Text.literal("§e" + chip), cx + 3, y + 1, 0xFFFFFFFF);
    }

    private static int lerp(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) Math.round(ar + (br - ar) * t);
        int g = (int) Math.round(ag + (bg - ag) * t);
        int bl = (int) Math.round(ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
