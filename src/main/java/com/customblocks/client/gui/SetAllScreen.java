/**
 * SetAllScreen.java — Group 07 (Set All Screen). CLIENT-ONLY.
 *
 * The dedicated Screen behind a bare {@code /cb setall}: pick ONE setting (glow · hardness · sound · collision ·
 * category · shape), choose its value, and apply it to EVERY custom block at once. Glow/hardness/sound/collision
 * cycle through the same value lists the Bulk Hub's Edit op uses (BulkOpSpec); category/shape take a text field.
 *
 * It holds no authoritative state — "Apply to all" opens a confirm modal (which notes the automatic pre-change
 * backup), then ships a {@link SetAllActionPayload}; the server runs the same validated auto-backup + apply-to-all
 * rail as the chat command and reports through chat. Group 27 CbScreenTemplate look (red/black), left rail of
 * settings, opaque confirm modal (UI_SCREEN_RULES).
 *
 * Depends on: SetAllActionPayload, BulkOpSpec, CbTheme, CbTextField, ClientSlotCache.
 * Called by: CustomBlocksClient (OpenGuiPayload mode=SETALL_SCREEN).
 */
package com.customblocks.client.gui;

import com.customblocks.client.ClientSlotCache;
import com.customblocks.network.payloads.SetAllActionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class SetAllScreen extends Screen {

    private static final String[] SETTINGS = {"glow", "hardness", "sound", "collision", "category", "shape"};
    private static final int BAR_H = 38, LEFT_W = 116, FOOT_H = 34;

    private int sel = 0;
    private String propValue = BulkOpSpec.defaultValueForProperty(SETTINGS[0]);
    private CbTextField textField;       // category / shape value
    private boolean confirmOpen;

    public SetAllScreen() { super(Text.literal("Set All")); }

    private boolean isText() { return SETTINGS[sel].equals("category") || SETTINGS[sel].equals("shape"); }

    private String valueString() {
        return isText() ? (textField != null ? textField.getText().trim() : "") : propValue;
    }

    @Override
    protected void init() {
        // Left rail — one button per setting; the active one is red.
        for (int i = 0; i < SETTINGS.length; i++) {
            final int idx = i;
            String label = (sel == i ? "§c" : "§f") + cap(SETTINGS[i]);
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> select(idx))
                    .dimensions(8, BAR_H + 8 + i * 22, LEFT_W - 16, 20).build());
        }

        int cx = LEFT_W + 12;
        if (isText()) {
            textField = new CbTextField(textRenderer, cx, BAR_H + 34, 190, 18,
                    Text.literal(SETTINGS[sel].equals("category") ? "category (none = clear)" : "shape name"));
            textField.setMaxLength(48);
            addDrawableChild(textField);
        } else {
            addDrawableChild(ButtonWidget.builder(Text.literal("Value: §e" + propValue), b -> cycleValue())
                    .dimensions(cx, BAR_H + 32, 190, 20).build());
        }

        int by = height - FOOT_H;
        addDrawableChild(ButtonWidget.builder(Text.literal("§aApply to all " + ClientSlotCache.entries().size()),
                        b -> { if (canApply()) { confirmOpen = true; reinit(); } })
                .dimensions(cx, by + 7, 160, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(width - 64, by + 7, 56, 20).build());

        if (confirmOpen) buildModal();
    }

    private void reinit() { clearChildren(); init(); }

    private void select(int i) {
        if (sel == i) return;
        sel = i;
        if (!isText()) propValue = BulkOpSpec.defaultValueForProperty(SETTINGS[i]);
        reinit();
    }

    private void cycleValue() {
        propValue = BulkOpSpec.cycle(BulkOpSpec.valuesForProperty(SETTINGS[sel]), propValue, 1);
        reinit();
    }

    private boolean canApply() { return !valueString().isBlank(); }

    private void buildModal() {
        for (var child : new java.util.ArrayList<>(children())) if (child instanceof ButtonWidget bw) bw.active = false;
        if (textField != null) textField.active = false;
        int cx = width / 2, by = height / 2 + 16;
        addDrawableChild(ButtonWidget.builder(Text.literal("§aConfirm"), b -> send())
                .dimensions(cx - 130, by, 126, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> { confirmOpen = false; reinit(); })
                .dimensions(cx + 4, by, 126, 20).build());
    }

    private void send() {
        ClientPlayNetworking.send(new SetAllActionPayload(SETTINGS[sel], valueString()));
        close();
    }

    // ── Render ─────────────────────────────────────────────────────────────────
    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) { /* own backdrop below */ }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xFF000000);
        ctx.fill(0, BAR_H, LEFT_W, height - FOOT_H, CbTheme.BAR_BG);
        ctx.fill(LEFT_W, BAR_H, LEFT_W + 1, height - FOOT_H, CbTheme.ACCENT_DIM);
        ctx.fill(0, 0, width, BAR_H, CbTheme.BAR_BG);
        ctx.fill(0, BAR_H - 1, width, BAR_H, CbTheme.ACCENT);
        ctx.drawTextWithShadow(textRenderer, CbTheme.title("Set All", "one setting → every block"), 8, 10, 0xFFFFFFFF);

        int cx = LEFT_W + 12;
        ctx.drawTextWithShadow(textRenderer, CbTheme.red(cap(SETTINGS[sel]) + " for all " + ClientSlotCache.entries().size() + " blocks"), cx, BAR_H + 10, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§8A safety backup is taken automatically before the change · one /cb undo reverts it."), cx, height - FOOT_H - 14, 0xFFFFFFFF);

        int by = height - FOOT_H;
        ctx.fill(0, by, width, height, CbTheme.BAR_BG);
        ctx.fill(0, by, width, by + 1, CbTheme.ACCENT);

        super.render(ctx, mx, my, delta);
        if (confirmOpen) renderModal(ctx);
    }

    private void renderModal(DrawContext ctx) {
        int cx = width / 2, cy = height / 2;
        int pw = 300, ph = 96;
        ctx.fill(0, 0, width, height, 0x66000000);
        ctx.fill(cx - pw / 2 - 1, cy - ph / 2 - 1, cx + pw / 2 + 1, cy + ph / 2 + 1, CbTheme.ACCENT);
        ctx.fill(cx - pw / 2, cy - ph / 2, cx + pw / 2, cy + ph / 2, CbTheme.DIALOG_BG);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§fSet §e" + SETTINGS[sel] + "§f = §e" + valueString()), cx, cy - ph / 2 + 10, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7on all " + ClientSlotCache.entries().size() + " blocks?"), cx, cy - ph / 2 + 24, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8A backup is saved first — undo with /cb undo."), cx, cy - ph / 2 + 40, 0xFFFFFFFF);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (confirmOpen) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { confirmOpen = false; reinit(); return true; }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { send(); return true; }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && !(getFocused() instanceof CbTextField)) { close(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean shouldPause() { return false; }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
