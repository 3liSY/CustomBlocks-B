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

import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class CbGradSlider {

    public static final int HUE = 0, SAT = 1, LIGHT = 2, TEMP = 3, PLAIN = 4;

    public final String label;
    public final double min, max;
    public final int kind;
    public double value;
    public int x, y, w;

    /** Show one decimal in the value chip (e.g. -28.6°) instead of a rounded int. Off by default so the
     *  Recolour screens keep their integer chips; the Guess pose sliders turn it on for fine angle values. */
    public boolean decimals = false;

    // ── Inline number entry (Group 30 pose screen). Other screens never call the edit/nudge methods, so their
    // behaviour is unchanged — this is purely additive. ────────────────────────────────────────────────────
    public boolean editing = false;      // is the value chip currently an editable text field?
    private String editBuf = "";         // what's been typed while editing (raw number, no unit)
    private int chipX, chipW, chipY;     // last-rendered value-chip rect, captured for hit-testing

    public CbGradSlider(String label, double min, double max, double value, int kind) {
        this.label = label; this.min = min; this.max = max; this.value = value; this.kind = kind;
    }

    public void set(int x, int y, int w) { this.x = x; this.y = y; this.w = w; }

    public boolean hit(double mx, double my) { return mx >= x && mx <= x + w && my >= y - 3 && my <= y + 14; }

    /** Was the value chip (to the right of the track) clicked? Used to start inline number entry. */
    public boolean hitChip(double mx, double my) {
        return mx >= chipX && mx <= chipX + chipW && my >= chipY - 2 && my <= chipY + 11;
    }

    public void setFromX(double mx) {
        double f = Math.max(0, Math.min(1, (mx - x) / w));
        value = min + f * (max - min);
    }

    public void reset(double v) { value = v; }

    // ── Inline number entry / nudging ──────────────────────────────────────────
    private double clamp(double v) { return Math.max(min, Math.min(max, v)); }

    /** Format the value for the chip: integer unless decimals are on and it isn't whole. */
    private String show() {
        if (!decimals || Math.abs(value - Math.rint(value)) < 0.05) return Integer.toString((int) Math.rint(value));
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** Click the value chip → it becomes editable, pre-filled with the current value. */
    public void beginEdit() { editing = true; editBuf = show(); }

    /** Leave edit mode WITHOUT applying the typed text (Esc). */
    public void cancelEdit() { editing = false; editBuf = ""; }

    /** Parse + silently clamp the typed buffer into the value, then leave edit mode. Bad text keeps the old value. */
    public void commitEdit() {
        if (!editing) return;
        try { value = clamp(Double.parseDouble(editBuf.trim())); } catch (NumberFormatException ignored) {}
        editing = false; editBuf = "";
    }

    /** Feed one typed character while editing. Accepts digits, one leading '-', and '.'. Returns true if consumed. */
    public boolean charTyped(char c) {
        if (!editing) return false;
        if ((c >= '0' && c <= '9') || c == '.' || (c == '-' && editBuf.isEmpty())) { editBuf += c; return true; }
        return false;
    }

    /** Handle a key while editing: Enter/Esc end it, Backspace trims, ↑/↓ nudge ±1. Returns true if consumed. */
    public boolean keyPressed(int key) {
        if (!editing) return false;
        switch (key) {
            case 257, 335 -> { commitEdit(); return true; }                 // ENTER / KP_ENTER
            case 256 -> { cancelEdit(); return true; }                       // ESC → cancel (don't close screen)
            case 259 -> { if (!editBuf.isEmpty()) editBuf = editBuf.substring(0, editBuf.length() - 1); return true; } // BACKSPACE
            case 265 -> { nudge(1); return true; }                           // ↑
            case 264 -> { nudge(-1); return true; }                          // ↓
        }
        return false;
    }

    /** Nudge the value by delta (clamped). Keeps the edit buffer in sync when nudging mid-edit. */
    public void nudge(double delta) { value = clamp(value + delta); if (editing) editBuf = show(); }

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
        // Value chip to the right of the track. While editing it shows the typed buffer + a caret and lights
        // its border with the accent colour; otherwise it shows the formatted value + unit.
        String chip = editing ? editBuf + "_" : show() + unit;
        int cw = tr.getWidth(chip) + 6, cx = x + w + 6;
        chipX = cx; chipW = cw; chipY = y;
        ctx.fill(cx - 1, y - 2, cx + cw + 1, y + 11, editing ? CbTheme.ACCENT : 0xFF000000);
        ctx.fill(cx, y - 1, cx + cw, y + 10, editing ? 0xFF303030 : 0xFF202020);
        ctx.drawTextWithShadow(tr, Text.literal("§f" + chip), cx + 3, y + 1, 0xFFFFFFFF);
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
