/**
 * HudColorPicker.java — GROUP 27 §G27.4 (Lego HUD Builder). CLIENT-SIDE ONLY.
 *
 * Responsibility: the full colour picker popup — a saturation/value square (drag), a hue
 * slider (drag), a hex input + live R/G/B readout, a preset swatch row, a recent-colours row
 * (from HudConfig), and an eyedropper that reuses EyedropScreen to sample a screen pixel.
 * Reports every change live through an IntConsumer (so the caller can recolour its brick in
 * real time); [Done] commits + records the colour in recents, [Cancel] restores the original.
 *
 * Depends on: HudConfig (recent colours), EyedropScreen (eyedropper), MathHelper (HSV→RGB).
 * Called by: HudBrickInspector (text + background colour controls).
 */
package com.customblocks.client.gui.hud;

import com.customblocks.client.HudConfig;
import com.customblocks.client.gui.EyedropScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.IntConsumer;

@Environment(EnvType.CLIENT)
public class HudColorPicker extends Screen {

    private static final int GOLD   = 0xFFFFAA00;
    private static final int PANEL_W = 270, PANEL_H = 210;
    private static final int SQ = 120, HUE_W = 14;

    private static final int[] PRESETS = {
            0xFFFFFF, 0xC0C0C0, 0x808080, 0x000000, 0xFF5555, 0xFFAA00, 0xFFFF55,
            0x55FF55, 0x55FFFF, 0x5555FF, 0xAA00FF, 0xFF55FF
    };

    private final int original;
    private final IntConsumer onChange;
    private final Screen parent;

    private float hue = 0f, sat = 1f, val = 1f;
    private TextFieldWidget hexField;

    // Cached layout (set in init()).
    private int px, py, sqX, sqY, hueX, hueY;

    public HudColorPicker(int initialRgb, IntConsumer onChange, Screen parent) {
        super(Text.literal("Colour Picker"));
        this.original = initialRgb & 0xFFFFFF;
        this.onChange = onChange;
        this.parent = parent;
        setFromRgb(this.original);
    }

    @Override
    protected void init() {
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;
        sqX = px + 12; sqY = py + 30;
        hueX = sqX + SQ + 10; hueY = sqY;

        hexField = new TextFieldWidget(textRenderer, px + 12, sqY + SQ + 10, 90, 16, Text.literal("hex"));
        hexField.setMaxLength(7);
        hexField.setText(HudConfig.toHex(rgb()));
        hexField.setChangedListener(s -> {
            int parsed = HudConfig.parseHex(s, -1);
            if (parsed >= 0) { setFromRgb(parsed); apply(false); }
        });
        addDrawableChild(hexField);

        int by = py + PANEL_H - 24;
        addDrawableChild(ButtonWidget.builder(Text.literal("Eyedrop"), b -> openEyedrop())
                .dimensions(px + 12, by, 70, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("§aDone"), b -> done())
                .dimensions(px + PANEL_W - 150, by, 70, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> cancel())
                .dimensions(px + PANEL_W - 76, by, 70, 18).build());
    }

    // ── Render ───────────────────────────────────────────────────────────────
    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) { }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        if (parent != null) parent.render(ctx, mx, my, 0);   // keep the editor visible behind
        ctx.fill(0, 0, width, height, 0x66000000);

        ctx.fill(px - 1, py - 1, px + PANEL_W + 1, py + PANEL_H + 1, GOLD);
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, 0xF0101010);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§6§lColour Picker"), px + 12, py + 10, 0xFFFFFFFF);

        drawSvSquare(ctx);
        drawHueBar(ctx);

        // Preview swatch + RGB readout.
        int rgb = rgb();
        ctx.fill(px + 110, sqY + SQ + 8, px + 140, sqY + SQ + 24, 0xFF000000 | rgb);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal(String.format("§7R%d G%d B%d", (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF)),
                px + 146, sqY + SQ + 12, 0xFFFFFFFF);

        drawSwatchRow(ctx, PRESETS_X(), presetsY(), PRESETS, PRESETS.length);
        // Recents row.
        int[] recents = HudConfig.recentColors.stream().mapToInt(Integer::intValue).toArray();
        ctx.drawTextWithShadow(textRenderer, Text.literal("§8recent"), px + 12, recentsY() - 10, 0xFFFFFFFF);
        drawSwatchRow(ctx, px + 12, recentsY(), recents, Math.min(recents.length, 12));

        super.render(ctx, mx, my, delta);
    }

    private int PRESETS_X() { return px + 12; }
    private int presetsY()  { return sqY + SQ + 32; }
    private int recentsY()  { return sqY + SQ + 56; }

    private void drawSvSquare(DrawContext ctx) {
        int step = 3;
        for (int sx = 0; sx < SQ; sx += step) {
            for (int sy = 0; sy < SQ; sy += step) {
                float s = sx / (float) SQ;
                float v = 1f - sy / (float) SQ;
                ctx.fill(sqX + sx, sqY + sy, sqX + sx + step, sqY + sy + step, 0xFF000000 | MathHelper.hsvToRgb(hue, s, v));
            }
        }
        // Cursor reticle.
        int cxp = sqX + Math.round(sat * SQ);
        int cyp = sqY + Math.round((1f - val) * SQ);
        ctx.fill(cxp - 2, cyp, cxp + 3, cyp + 1, 0xFFFFFFFF);
        ctx.fill(cxp, cyp - 2, cxp + 1, cyp + 3, 0xFFFFFFFF);
    }

    private void drawHueBar(DrawContext ctx) {
        for (int sy = 0; sy < SQ; sy++) {
            float h = sy / (float) SQ;
            ctx.fill(hueX, hueY + sy, hueX + HUE_W, hueY + sy + 1, 0xFF000000 | MathHelper.hsvToRgb(h, 1f, 1f));
        }
        int hy = hueY + Math.round(hue * SQ);
        ctx.fill(hueX - 1, hy - 1, hueX + HUE_W + 1, hy + 1, 0xFFFFFFFF);
    }

    private void drawSwatchRow(DrawContext ctx, int x, int y, int[] colors, int count) {
        for (int i = 0; i < count; i++) {
            int sx = x + i * 18;
            ctx.fill(sx, y, sx + 16, y + 16, 0xFF000000 | (colors[i] & 0xFFFFFF));
            ctx.fill(sx, y, sx + 16, y + 1, 0xFF000000);
        }
    }

    // ── Input ────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (handleSvHue(mx, my)) return true;
        // Swatch rows.
        int hit = swatchHit(mx, my, PRESETS_X(), presetsY(), PRESETS.length);
        if (hit >= 0) { setFromRgb(PRESETS[hit]); apply(true); return true; }
        int[] recents = HudConfig.recentColors.stream().mapToInt(Integer::intValue).toArray();
        int rhit = swatchHit(mx, my, px + 12, recentsY(), Math.min(recents.length, 12));
        if (rhit >= 0) { setFromRgb(recents[rhit]); apply(true); return true; }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && handleSvHue(mx, my)) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    private boolean handleSvHue(double mx, double my) {
        if (mx >= sqX && mx <= sqX + SQ && my >= sqY && my <= sqY + SQ) {
            sat = (float) clamp01((mx - sqX) / SQ);
            val = (float) clamp01(1.0 - (my - sqY) / SQ);
            apply(true);
            return true;
        }
        if (mx >= hueX && mx <= hueX + HUE_W && my >= hueY && my <= hueY + SQ) {
            hue = (float) clamp01((my - hueY) / SQ);
            apply(true);
            return true;
        }
        return false;
    }

    private int swatchHit(double mx, double my, int x, int y, int count) {
        if (my < y || my > y + 16) return -1;
        for (int i = 0; i < count; i++) {
            int sx = x + i * 18;
            if (mx >= sx && mx <= sx + 16) return i;
        }
        return -1;
    }

    // ── Colour state ───────────────────────────────────────────────────────
    private void setFromRgb(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        val = max;
        sat = max == 0f ? 0f : d / max;
        if (d == 0f) hue = 0f;
        else if (max == r) hue = (((g - b) / d) % 6f) / 6f;
        else if (max == g) hue = ((b - r) / d + 2f) / 6f;
        else               hue = ((r - g) / d + 4f) / 6f;
        if (hue < 0) hue += 1f;
    }

    private int rgb() { return MathHelper.hsvToRgb(hue, sat, val) & 0xFFFFFF; }

    /** Push the current colour to the caller; optionally refresh the hex field text. */
    private void apply(boolean refreshHex) {
        if (onChange != null) onChange.accept(rgb());
        if (refreshHex && hexField != null && !hexField.isFocused()) hexField.setText(HudConfig.toHex(rgb()));
    }

    private void openEyedrop() {
        if (client != null)
            client.setScreen(new EyedropScreen(rgb -> { setFromRgb(rgb); apply(true); }, this));
    }

    private void done() {
        int rgb = rgb();
        if (onChange != null) onChange.accept(rgb);
        HudConfig.pushRecent(rgb);
        back();
    }

    private void cancel() {
        if (onChange != null) onChange.accept(original);   // restore
        back();
    }

    private void back() { if (client != null) client.setScreen(parent); }

    @Override
    public void close() { cancel(); }

    @Override
    public boolean shouldPause() { return false; }

    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
