/**
 * HudEditorScreen.java
 *
 * Responsibility: Lunar-style drag-to-reposition editor for the CustomBlocks HUD
 * overlay. Renders a live preview over the game world; the player can drag it to any
 * position and adjust scale, text color, background opacity, and which fields show.
 * "Save" persists to hud-config-server.json; "Reset" restores defaults; closing without
 * saving reverts to the values captured when the editor opened. CLIENT-SIDE ONLY.
 *
 * Depends on: HudConfig, HudRenderer, Screen, ButtonWidget
 * Called by: CustomBlocksClient (OpenGuiPayload mode=HUD_EDITOR), EscMenuButtons
 */
package com.customblocks.client.gui;

import com.customblocks.client.HudConfig;
import com.customblocks.client.HudRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class HudEditorScreen extends Screen {

    private static final String SAMPLE_ID   = "example_id";
    private static final String SAMPLE_NAME = "Example Block";

    // Snapshot of settings when the editor opened (for revert-on-cancel).
    private final int     origX, origY, origColor;
    private final float   origScale, origBg;
    private final boolean origShowId, origShowName;
    private boolean saved = false;

    // Drag state.
    private boolean dragging = false;
    private int grabDX, grabDY;

    // Live preview box bounds (screen pixels), recomputed each render for hit-testing.
    private int boxX, boxY, boxW, boxH;

    // Buttons whose labels reflect live values.
    private ButtonWidget scaleBtn, colorBtn, bgBtn, idBtn, nameBtn;

    public HudEditorScreen() {
        super(Text.literal("HUD Editor"));
        this.origX = HudConfig.x;
        this.origY = HudConfig.y;
        this.origColor = HudConfig.color;
        this.origScale = HudConfig.scale;
        this.origBg = HudConfig.bgOpacity;
        this.origShowId = HudConfig.showId;
        this.origShowName = HudConfig.showName;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int row1 = height - 56;
        int row2 = height - 32;

        // Row 1: scale − / value / + , color , bg − / value / +
        addDrawableChild(ButtonWidget.builder(Text.literal("−"),
                b -> { HudConfig.scale = HudConfig.clampScale(round1(HudConfig.scale - 0.1f)); refresh(); })
                .dimensions(cx - 200, row1, 20, 20).build());
        scaleBtn = ButtonWidget.builder(scaleLabel(), b -> {})
                .dimensions(cx - 178, row1, 90, 20).build();
        addDrawableChild(scaleBtn);
        addDrawableChild(ButtonWidget.builder(Text.literal("+"),
                b -> { HudConfig.scale = HudConfig.clampScale(round1(HudConfig.scale + 0.1f)); refresh(); })
                .dimensions(cx - 86, row1, 20, 20).build());

        colorBtn = ButtonWidget.builder(colorLabel(), b -> { HudConfig.cycleColor(); refresh(); })
                .dimensions(cx - 60, row1, 120, 20).build();
        addDrawableChild(colorBtn);

        addDrawableChild(ButtonWidget.builder(Text.literal("−"),
                b -> { HudConfig.bgOpacity = HudConfig.clamp01(round1(HudConfig.bgOpacity - 0.1f)); refresh(); })
                .dimensions(cx + 66, row1, 20, 20).build());
        bgBtn = ButtonWidget.builder(bgLabel(), b -> {})
                .dimensions(cx + 88, row1, 92, 20).build();
        addDrawableChild(bgBtn);
        addDrawableChild(ButtonWidget.builder(Text.literal("+"),
                b -> { HudConfig.bgOpacity = HudConfig.clamp01(round1(HudConfig.bgOpacity + 0.1f)); refresh(); })
                .dimensions(cx + 182, row1, 20, 20).build());

        // Row 2: show ID , show Name , Reset , Save , Cancel
        idBtn = ButtonWidget.builder(idLabel(), b -> { HudConfig.showId = !HudConfig.showId; refresh(); })
                .dimensions(cx - 200, row2, 78, 20).build();
        addDrawableChild(idBtn);
        nameBtn = ButtonWidget.builder(nameLabel(), b -> { HudConfig.showName = !HudConfig.showName; refresh(); })
                .dimensions(cx - 118, row2, 98, 20).build();
        addDrawableChild(nameBtn);
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), b -> { HudConfig.resetDefaults(); refresh(); })
                .dimensions(cx - 16, row2, 66, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save())
                .dimensions(cx + 54, row2, 66, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> close())
                .dimensions(cx + 124, row2, 76, 20).build());
    }

    private void refresh() {
        if (scaleBtn != null) scaleBtn.setMessage(scaleLabel());
        if (colorBtn != null) colorBtn.setMessage(colorLabel());
        if (bgBtn != null)    bgBtn.setMessage(bgLabel());
        if (idBtn != null)    idBtn.setMessage(idLabel());
        if (nameBtn != null)  nameBtn.setMessage(nameLabel());
    }

    private Text scaleLabel() { return Text.literal(String.format("Scale: %.1fx", HudConfig.scale)); }
    private Text colorLabel() { return Text.literal("Color: " + HudConfig.colorName()); }
    private Text bgLabel()    { return Text.literal(String.format("BG: %d%%", Math.round(HudConfig.bgOpacity * 100))); }
    private Text idLabel()    { return Text.literal("ID: " + (HudConfig.showId ? "On" : "Off")); }
    private Text nameLabel()  { return Text.literal("Name: " + (HudConfig.showName ? "On" : "Off")); }

    private static float round1(float v) { return Math.round(v * 10f) / 10f; }

    private void save() {
        saved = true;
        HudConfig.save();
        close();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        // No vanilla blur/darken — keep the world visible behind the editor overlay.
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Faint dim so the controls read clearly while the world stays visible.
        ctx.fill(0, 0, width, height, 0x66000000);
        super.render(ctx, mx, my, delta);

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§e§lHUD Editor"), width / 2, 10, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Drag the preview to reposition · adjust below · Save to keep"),
                width / 2, 24, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§8x=" + HudConfig.x + "  y=" + HudConfig.y),
                width / 2, 36, 0xFFAAAAAA);

        // Live preview at the configured position.
        int[] size = HudRenderer.boxSize(client, SAMPLE_ID, SAMPLE_NAME);
        boxX = HudConfig.x; boxY = HudConfig.y; boxW = size[0]; boxH = size[1];

        int outline = dragging ? 0xFFFFFF55 : 0x88FFFFFF;
        drawOutline(ctx, boxX - 1, boxY - 1, boxW + 2, boxH + 2, outline);

        HudRenderer.draw(ctx, client, SAMPLE_ID, SAMPLE_NAME);
    }

    private void drawOutline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && mouseX >= boxX && mouseX <= boxX + boxW
                && mouseY >= boxY && mouseY <= boxY + boxH) {
            dragging = true;
            grabDX = (int) Math.round(mouseX) - HudConfig.x;
            grabDY = (int) Math.round(mouseY) - HudConfig.y;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging && button == 0) {
            int nx = (int) Math.round(mouseX) - grabDX;
            int ny = (int) Math.round(mouseY) - grabDY;
            HudConfig.x = Math.max(0, Math.min(width  - Math.max(boxW, 1), nx));
            HudConfig.y = Math.max(0, Math.min(height - Math.max(boxH, 1), ny));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        if (!saved) {
            // Revert live edits the player didn't commit.
            HudConfig.x = origX; HudConfig.y = origY; HudConfig.color = origColor;
            HudConfig.scale = origScale; HudConfig.bgOpacity = origBg;
            HudConfig.showId = origShowId; HudConfig.showName = origShowName;
        }
        super.close();
    }
}
