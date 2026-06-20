/**
 * HudBrickInspector.java — GROUP 27 §G27.4 (Lego HUD Builder). CLIENT-SIDE ONLY.
 *
 * Responsibility: the per-brick settings popup (the ⚙ on a brick row). Edits one HudField
 * live: type-swap, size, text colour (full picker), bold, shadow, prefix label, alignment,
 * cosmetic effect, and the background override (own colour + opacity, or off). For text-type
 * bricks (Custom text / Header) it also exposes the brick's text. All edits mutate the shared
 * HudField in HudConfig.fields, so the editor preview reflects them on return.
 *
 * Depends on: HudConfig, HudField, HudFieldType, HudColorPicker.
 * Called by: HudEditorScreen (⚙ on a brick row).
 */
package com.customblocks.client.gui.hud;

import com.customblocks.client.HudConfig;
import com.customblocks.client.hud.HudField;
import com.customblocks.client.hud.HudFieldType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class HudBrickInspector extends Screen {

    private static final int GOLD = 0xFFFFAA00;
    private static final int PW = 224;

    private final HudField field;
    private final Screen parent;

    private int px, py, ph;
    private ButtonWidget typeBtn, sizeBtn, boldBtn, shadowBtn, alignBtn, effectBtn, bgOverBtn, bgOffBtn, bgOpacBtn;
    private TextFieldWidget prefixField, textField;

    public HudBrickInspector(HudField field, Screen parent) {
        super(Text.literal("Brick"));
        this.field = field;
        this.parent = parent;
    }

    @Override
    protected void init() {
        boolean textType = field.type.isText();
        int rows = textType ? 11 : 10;
        ph = 28 + rows * 22;
        px = (width - PW) / 2;
        py = (height - ph) / 2;

        int x = px + 12, w = PW - 24, y = py + 26;

        typeBtn = reg(ButtonWidget.builder(typeLabel(), b -> cycleType()).dimensions(x, y, w, 18).build());
        y += 22;

        // Size − [value] +
        reg(ButtonWidget.builder(Text.literal("−"), b -> { field.size = HudField.clampSize(round1(field.size - 0.1f)); sizeBtn.setMessage(sizeLabel()); }).dimensions(x, y, 20, 18).build());
        sizeBtn = reg(ButtonWidget.builder(sizeLabel(), b -> {}).dimensions(x + 22, y, w - 44, 18).build());
        reg(ButtonWidget.builder(Text.literal("+"), b -> { field.size = HudField.clampSize(round1(field.size + 0.1f)); sizeBtn.setMessage(sizeLabel()); }).dimensions(x + w - 20, y, 20, 18).build());
        y += 22;

        reg(ButtonWidget.builder(Text.literal("Text colour ■"), b -> openPicker(false)).dimensions(x, y, w, 18).build());
        y += 22;

        int half = (w - 4) / 2;
        boldBtn   = reg(ButtonWidget.builder(boldLabel(),   b -> { field.bold   = !field.bold;   boldBtn.setMessage(boldLabel()); }).dimensions(x, y, half, 18).build());
        shadowBtn = reg(ButtonWidget.builder(shadowLabel(), b -> { field.shadow = !field.shadow; shadowBtn.setMessage(shadowLabel()); }).dimensions(x + half + 4, y, half, 18).build());
        y += 22;

        alignBtn  = reg(ButtonWidget.builder(alignLabel(),  b -> cycleAlign()).dimensions(x, y, half, 18).build());
        effectBtn = reg(ButtonWidget.builder(effectLabel(), b -> cycleEffect()).dimensions(x + half + 4, y, half, 18).build());
        y += 22;

        prefixField = new TextFieldWidget(textRenderer, x, y, w, 18, Text.literal("prefix"));
        prefixField.setMaxLength(24);
        prefixField.setText(field.prefix);
        prefixField.setChangedListener(s -> field.prefix = s);
        reg(prefixField);
        y += 22;

        if (textType) {
            textField = new TextFieldWidget(textRenderer, x, y, w, 18, Text.literal("text"));
            textField.setMaxLength(96);
            textField.setText(field.text);
            textField.setChangedListener(s -> field.text = s);
            reg(textField);
            y += 22;
        }

        bgOverBtn = reg(ButtonWidget.builder(bgOverLabel(), b -> { field.bgOverride = !field.bgOverride; bgOverBtn.setMessage(bgOverLabel()); }).dimensions(x, y, half, 18).build());
        bgOffBtn  = reg(ButtonWidget.builder(bgOffLabel(),  b -> { field.bgOff = !field.bgOff; bgOffBtn.setMessage(bgOffLabel()); }).dimensions(x + half + 4, y, half, 18).build());
        y += 22;

        reg(ButtonWidget.builder(Text.literal("BG colour ■"), b -> openPicker(true)).dimensions(x, y, half, 18).build());
        reg(ButtonWidget.builder(Text.literal("−"), b -> { field.bgOpacity = HudField.clamp01(round1(field.bgOpacity - 0.1f)); bgOpacBtn.setMessage(bgOpacLabel()); }).dimensions(x + half + 4, y, 20, 18).build());
        bgOpacBtn = reg(ButtonWidget.builder(bgOpacLabel(), b -> {}).dimensions(x + half + 26, y, half - 46, 18).build());
        reg(ButtonWidget.builder(Text.literal("+"), b -> { field.bgOpacity = HudField.clamp01(round1(field.bgOpacity + 0.1f)); bgOpacBtn.setMessage(bgOpacLabel()); }).dimensions(x + w - 20, y, 20, 18).build());
        y += 22;

        reg(ButtonWidget.builder(Text.literal("§aDone"), b -> back()).dimensions(x, y, w, 18).build());
    }

    private <T extends net.minecraft.client.gui.Element & net.minecraft.client.gui.Drawable & net.minecraft.client.gui.Selectable> T reg(T w) {
        return addDrawableChild(w);
    }

    // ── Labels ───────────────────────────────────────────────────────────────
    private Text typeLabel()   { return Text.literal("Type: §f" + field.type.label()); }
    private Text sizeLabel()   { return Text.literal(String.format("Size %.1fx", field.size)); }
    private Text boldLabel()   { return Text.literal("Bold: " + onOff(field.bold)); }
    private Text shadowLabel() { return Text.literal("Shadow: " + onOff(field.shadow)); }
    private Text alignLabel()  { return Text.literal("Align: §f" + switch (field.align) { case LEFT -> "L"; case CENTER -> "C"; case RIGHT -> "R"; }); }
    private Text effectLabel() { return Text.literal("FX: §f" + cap(field.effect.name())); }
    private Text bgOverLabel() { return Text.literal("BG over: " + onOff(field.bgOverride)); }
    private Text bgOffLabel()  { return Text.literal("BG off: " + onOff(field.bgOff)); }
    private Text bgOpacLabel() { return Text.literal(Math.round(field.bgOpacity * 100) + "%"); }

    private static String onOff(boolean b) { return b ? "§aOn" : "§7Off"; }
    private static String cap(String s) { return s.charAt(0) + s.substring(1).toLowerCase(); }

    // ── Cycles ───────────────────────────────────────────────────────────────
    private void cycleType() {
        HudFieldType[] v = HudFieldType.values();
        field.type = v[(field.type.ordinal() + 1) % v.length];
        if (field.type.isText() && field.text.isEmpty()) field.text = field.type == HudFieldType.HEADER ? "Header" : "Text";
        rebuild();   // text-field row may appear/disappear
    }

    private void cycleAlign() {
        HudField.Align[] v = HudField.Align.values();
        field.align = v[(field.align.ordinal() + 1) % v.length];
        alignBtn.setMessage(alignLabel());
    }

    private void cycleEffect() {
        HudField.Effect[] v = HudField.Effect.values();
        field.effect = v[(field.effect.ordinal() + 1) % v.length];
        effectBtn.setMessage(effectLabel());
    }

    private void openPicker(boolean background) {
        int initial = background ? field.bgColor : field.color;
        if (client != null)
            client.setScreen(new HudColorPicker(initial,
                    rgb -> { if (background) field.bgColor = rgb; else field.color = rgb; }, this));
    }

    private void rebuild() { clearChildren(); init(); }

    // ── Render ───────────────────────────────────────────────────────────────
    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) { }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        if (parent != null) parent.render(ctx, mx, my, 0);
        ctx.fill(0, 0, width, height, 0x66000000);
        ctx.fill(px - 1, py - 1, px + PW + 1, py + ph + 1, GOLD);
        ctx.fill(px, py, px + PW, py + ph, 0xF0101010);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§6§lBrick · " + field.type.label()), px + 12, py + 9, 0xFFFFFFFF);
        // Colour chips next to the colour buttons.
        super.render(ctx, mx, my, delta);
    }

    private void back() { if (client != null) client.setScreen(parent); }

    @Override
    public void close() { back(); }

    @Override
    public boolean shouldPause() { return false; }

    private static float round1(float v) { return Math.round(v * 10f) / 10f; }
}
