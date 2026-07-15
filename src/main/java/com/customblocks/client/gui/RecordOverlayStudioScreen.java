/**
 * RecordOverlayStudioScreen.java - GROUP 29 Build B slice. CLIENT-SIDE ONLY.
 *
 * Responsibility: first usable Record Overlay Studio surface for the built-in Shorts
 * guides: layer visibility, guide geometry sliders, live 16:9 preview, and recording
 * mode controls. Custom draggable marks are a later Build B slice.
 */
package com.customblocks.client.gui;

import com.customblocks.client.capture.CaptureOverlayConfig;
import com.customblocks.client.capture.CaptureOverlayManager;
import com.customblocks.client.capture.ShortsGeometry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class RecordOverlayStudioScreen extends Screen {

    private enum Layer { CROP, SAFE, CAPTION, SUBJECT, DIM }

    private static final int BAR_H = 56;
    private static final int LEFT_W = 190;
    private static final int RIGHT_W = 300;
    private static final int BOTTOM_H = 56;
    private static final int BAR_BG = CbTheme.BAR_BG;
    private static final int ACCENT = 0xFFFFD038;
    private static final int CYAN = 0xFF33DDFF;
    private static final int SAFE = 0xFFFFFFFF;
    private static final int CAPTION = 0xFFFFD038;
    private static final int SUBJECT = 0xFF8DFF5B;
    private static final int DIM = 0xAA000000;

    private Layer selected = Layer.CROP;
    private final List<GuideSlider> sliders = new ArrayList<>();
    private long saveFlashEnd;

    private final CbHelpOverlay help = new CbHelpOverlay("Record Overlay Studio", List.of(
            new CbHelpOverlay.Group("GUIDES", List.of(
                    new CbHelpOverlay.Row("Left rail", "Select or show/hide built-in guides"),
                    new CbHelpOverlay.Row("Right panel", "Tune the selected guide"),
                    new CbHelpOverlay.Row("Reset", "Restore default guide geometry"))),
            new CbHelpOverlay.Group("RECORDING", List.of(
                    new CbHelpOverlay.Row("F8", "Overlay on/off"),
                    new CbHelpOverlay.Row("F9", "Borderless helper"),
                    new CbHelpOverlay.Row("F10", "Open this Studio")))));

    public RecordOverlayStudioScreen() {
        super(Text.literal("Record Overlay Studio"));
    }

    @Override
    protected void init() {
        sliders.clear();
        buildLayerRail();
        buildInspector();
        buildBottomBar();
        addDrawableChild(CbButton.normal(Text.literal("?"), width - 24, 10, 16, 16, b -> help.toggle()));
    }

    public void refreshFromConfig() {
        clearChildren();
        init();
    }

    private void buildLayerRail() {
        int x = 12;
        int y = BAR_H + 34;
        addLayerButton(x, y, Layer.CROP, "9:16 Crop", CaptureOverlayConfig.showFrame);
        addLayerButton(x, y + 30, Layer.SAFE, "Safe Area", CaptureOverlayConfig.showSafe);
        addLayerButton(x, y + 60, Layer.CAPTION, "Caption Zone", CaptureOverlayConfig.showCaption);
        addLayerButton(x, y + 90, Layer.SUBJECT, "Subject Box", CaptureOverlayConfig.showSubject);
        addLayerButton(x, y + 120, Layer.DIM, "Dim Sidebars", CaptureOverlayConfig.showDim);
    }

    private void addLayerButton(int x, int y, Layer layer, String label, boolean visible) {
        String text = label + (visible ? " On" : " Off");
        addDrawableChild(CbButton.tab(Text.literal(text), x, y, LEFT_W - 24, 22,
                selected == layer, true, b -> {
                    if (selected == layer) toggleSelectedLayer();
                    else selected = layer;
                    refreshFromConfig();
                }));
    }

    private void buildInspector() {
        int x = width - RIGHT_W + 22;
        int y = BAR_H + 78;
        int w = RIGHT_W - 54;
        switch (selected) {
            case DIM -> addSlider("Sidebar dim", 0, 80, CaptureOverlayConfig.dimOpacity * 100f, "%", x, y, w,
                    v -> CaptureOverlayConfig.dimOpacity = (float) v / 100f);
            case SAFE -> {
                addSlider("Side margin", 0, 30, CaptureOverlayConfig.safeMarginX * 100f, "%", x, y, w,
                        v -> CaptureOverlayConfig.safeMarginX = (float) v / 100f);
                addSlider("Top margin", 0, 40, CaptureOverlayConfig.safeMarginTop * 100f, "%", x, y + 32, w,
                        v -> CaptureOverlayConfig.safeMarginTop = (float) v / 100f);
                addSlider("Bottom margin", 0, 45, CaptureOverlayConfig.safeMarginBottom * 100f, "%", x, y + 64, w,
                        v -> CaptureOverlayConfig.safeMarginBottom = (float) v / 100f);
            }
            case CAPTION -> {
                addSlider("Caption X", 0, 90, CaptureOverlayConfig.captionX * 100f, "%", x, y, w,
                        v -> CaptureOverlayConfig.captionX = (float) v / 100f);
                addSlider("Caption Y", 0, 92, CaptureOverlayConfig.captionY * 100f, "%", x, y + 32, w,
                        v -> CaptureOverlayConfig.captionY = (float) v / 100f);
                addSlider("Caption W", 10, 100, CaptureOverlayConfig.captionW * 100f, "%", x, y + 64, w,
                        v -> CaptureOverlayConfig.captionW = (float) v / 100f);
                addSlider("Caption H", 5, 50, CaptureOverlayConfig.captionH * 100f, "%", x, y + 96, w,
                        v -> CaptureOverlayConfig.captionH = (float) v / 100f);
            }
            case SUBJECT -> {
                addSlider("Subject W", 20, 100, CaptureOverlayConfig.subjectW * 100f, "%", x, y, w,
                        v -> CaptureOverlayConfig.subjectW = (float) v / 100f);
                addSlider("Subject H", 20, 100, CaptureOverlayConfig.subjectH * 100f, "%", x, y + 32, w,
                        v -> CaptureOverlayConfig.subjectH = (float) v / 100f);
            }
            case CROP -> { }
        }
    }

    private void addSlider(String label, double min, double max, double value, String unit,
                           int x, int y, int w, Setter setter) {
        GuideSlider slider = new GuideSlider(label, min, max, value, unit, x, y, w, setter);
        sliders.add(slider);
        addDrawableChild(slider);
    }

    private void buildBottomBar() {
        int y = height - BOTTOM_H + 18;
        int x = 12;
        addDrawableChild(CbButton.normal(Text.literal("Overlay " + (CaptureOverlayConfig.enabled ? "On" : "Off")),
                x, y, 100, 22, b -> {
                    CaptureOverlayManager.toggle(client);
                    refreshFromConfig();
                }));
        addDrawableChild(CbButton.normal(Text.literal("OBS " + (CaptureOverlayConfig.obsMode ? "On" : "Off")),
                x + 108, y, 86, 22, b -> {
                    CaptureOverlayManager.toggleObsMode(client);
                    refreshFromConfig();
                }));
        addDrawableChild(CbButton.normal(Text.literal("Borderless"),
                x + 202, y, 104, 22, b -> {
                    CaptureOverlayManager.toggleBorderless(client);
                    refreshFromConfig();
                }));

        int cx = width / 2;
        addDrawableChild(CbButton.primary(Text.literal("Save"), cx - 56, y, 112, 22, b -> save()));

        int right = width - 8;
        addDrawableChild(CbButton.ghost(Text.literal("Close"), right - 76, y, 76, 22, b -> close()));
        addDrawableChild(CbButton.normal(Text.literal("Reset"), right - 162, y, 76, 22, b -> reset()));
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        /* Keep the world visible behind the editor. */
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xE8050507);
        drawChrome(ctx);
        drawPreview(ctx);
        drawInspectorText(ctx);
        super.render(ctx, mx, my, delta);
        if (System.currentTimeMillis() < saveFlashEnd) {
            ctx.fill(width / 2 - 56, height - BOTTOM_H + 18, width / 2 + 56, height - BOTTOM_H + 40, CbTheme.FLASH_OK);
        }
        help.render(ctx, width, height, textRenderer, mx, my);
    }

    private void drawChrome(DrawContext ctx) {
        ctx.fill(0, 0, width, BAR_H, BAR_BG);
        ctx.fill(0, BAR_H - 1, width, BAR_H, CbTheme.ACCENT);
        ctx.drawTextWithShadow(textRenderer, CbTheme.title("Record Overlay Studio"), 8, 10, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Built-in Shorts guides now. Close Studio before recording."), 8, 24, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("F8 overlay   F9 borderless   F10 Studio"), 8, 38, 0xFFFFFFFF);

        ctx.fill(0, BAR_H, LEFT_W, height - BOTTOM_H, 0xF0101016);
        ctx.fill(LEFT_W - 1, BAR_H, LEFT_W, height - BOTTOM_H, CbTheme.CARD_EDGE);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Layers"), 12, BAR_H + 12, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("click selected layer to toggle"), 12, BAR_H + 24, 0xFFB0B0B0);

        ctx.fill(width - RIGHT_W, BAR_H, width, height - BOTTOM_H, 0xF0101016);
        ctx.fill(width - RIGHT_W, BAR_H, width - RIGHT_W + 1, height - BOTTOM_H, CbTheme.CARD_EDGE);

        int by = height - BOTTOM_H;
        ctx.fill(0, by, width, height, BAR_BG);
        ctx.fill(0, by, width, by + 1, CbTheme.ACCENT);
    }

    private void drawPreview(DrawContext ctx) {
        int areaX = LEFT_W + 18;
        int areaY = BAR_H + 24;
        int areaW = Math.max(80, width - LEFT_W - RIGHT_W - 36);
        int areaH = Math.max(80, height - BAR_H - BOTTOM_H - 48);
        int previewW = areaW;
        int previewH = Math.round(previewW * 9f / 16f);
        if (previewH > areaH) {
            previewH = areaH;
            previewW = Math.round(previewH * 16f / 9f);
        }
        int x = areaX + (areaW - previewW) / 2;
        int y = areaY + (areaH - previewH) / 2;

        ctx.fill(x - 2, y - 2, x + previewW + 2, y + previewH + 2, 0xFF000000);
        ctx.fill(x, y, x + previewW, y + previewH, 0xFF15202A);
        drawWorldGrid(ctx, x, y, previewW, previewH);

        ShortsGeometry s = ShortsGeometry.of(previewW, previewH);
        int fx = x + s.frameX();
        int fy = y + s.frameY();

        if (CaptureOverlayConfig.showDim) {
            ctx.fill(x, y, fx, y + previewH, DIM);
            ctx.fill(fx + s.frameW(), y, x + previewW, y + previewH, DIM);
        }
        if (CaptureOverlayConfig.showFrame) drawRect(ctx, fx, fy, s.frameW(), s.frameH(), CYAN);
        if (CaptureOverlayConfig.showSafe) drawRect(ctx, x + s.safeX(), y + s.safeY(), s.safeW(), s.safeH(), SAFE);
        if (CaptureOverlayConfig.showSubject) drawRect(ctx, x + s.subjectX(), y + s.subjectY(), s.subjectW(), s.subjectH(), SUBJECT);
        if (CaptureOverlayConfig.showCaption) {
            drawRect(ctx, x + s.captionX(), y + s.captionY(), s.captionW(), s.captionH(), CAPTION);
            ctx.drawTextWithShadow(textRenderer, Text.literal("captions"), x + s.captionX() + 6, y + s.captionY() + 6, CAPTION);
        }

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("16:9 source preview"), x + previewW / 2, y - 12, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("center 9:16 crop"), fx + s.frameW() / 2, y + previewH + 6, 0xFFFFFFFF);
    }

    private void drawWorldGrid(DrawContext ctx, int x, int y, int w, int h) {
        int sky = 0xFF20354A;
        int ground = 0xFF26331F;
        ctx.fill(x, y, x + w, y + h / 2, sky);
        ctx.fill(x, y + h / 2, x + w, y + h, ground);
        int step = Math.max(14, w / 22);
        for (int gx = x; gx < x + w; gx += step) ctx.fill(gx, y, gx + 1, y + h, 0x2233DDFF);
        for (int gy = y; gy < y + h; gy += step) ctx.fill(x, gy, x + w, gy + 1, 0x2233DDFF);
        ctx.fill(x + w / 2 - 8, y + h / 2 - 8, x + w / 2 + 8, y + h / 2 + 8, 0x66404040);
    }

    private void drawInspectorText(DrawContext ctx) {
        int x = width - RIGHT_W + 22;
        int y = BAR_H + 18;
        int maxW = RIGHT_W - 44;
        ctx.drawTextWithShadow(textRenderer, Text.literal("Inspector"), x, y, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal(layerTitle()), x, y + 14, 0xFFB0B0B0);
        if (selected == Layer.CROP) {
            drawWrapped(ctx, "The crop frame always stays 9:16 and centered.", x, y + 48, maxW, 0xFFFFFFFF);
            drawWrapped(ctx, "Click the selected layer on the left to hide or show it.", x, y + 76, maxW, 0xFFB0B0B0);
        }
        String mode = "Mode: overlay " + (CaptureOverlayConfig.enabled ? "on" : "off")
                + " | OBS " + (CaptureOverlayConfig.obsMode ? "on" : "off")
                + " | borderless " + (CaptureOverlayManager.borderlessActive() ? "on" : "off");
        ctx.drawTextWithShadow(textRenderer, Text.literal(mode), 12, height - BOTTOM_H - 16, 0xFFFFFFFF);
    }

    private void drawWrapped(DrawContext ctx, String text, int x, int y, int maxWidth, int color) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int yy = y;
        for (String word : words) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (textRenderer.getWidth(next) > maxWidth && !line.isEmpty()) {
                ctx.drawTextWithShadow(textRenderer, Text.literal(line.toString()), x, yy, color);
                yy += 12;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        if (!line.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal(line.toString()), x, yy, color);
        }
    }

    private String layerTitle() {
        return switch (selected) {
            case CROP -> "9:16 crop frame";
            case SAFE -> "safe-area margins";
            case CAPTION -> "caption clearance box";
            case SUBJECT -> "subject guide box";
            case DIM -> "side dim opacity";
        };
    }

    private void drawRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 2, color);
        ctx.fill(x, y + h - 2, x + w, y + h, color);
        ctx.fill(x, y, x + 2, y + h, color);
        ctx.fill(x + w - 2, y, x + w, y + h, color);
    }

    private void toggleSelectedLayer() {
        switch (selected) {
            case CROP -> CaptureOverlayConfig.showFrame = !CaptureOverlayConfig.showFrame;
            case SAFE -> CaptureOverlayConfig.showSafe = !CaptureOverlayConfig.showSafe;
            case CAPTION -> CaptureOverlayConfig.showCaption = !CaptureOverlayConfig.showCaption;
            case SUBJECT -> CaptureOverlayConfig.showSubject = !CaptureOverlayConfig.showSubject;
            case DIM -> CaptureOverlayConfig.showDim = !CaptureOverlayConfig.showDim;
        }
        CaptureOverlayConfig.save();
    }

    private void reset() {
        CaptureOverlayConfig.resetGuideDefaults();
        CaptureOverlayConfig.save();
        refreshFromConfig();
    }

    private void save() {
        CaptureOverlayConfig.save();
        saveFlashEnd = System.currentTimeMillis() + 650;
        CbToast.success("Record overlay settings saved."); // §G27.13 — toast, not action bar
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (help.mouseClicked(mx, my)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (help.isOpen()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) help.close();
            return true;
        }
        boolean ctrl = Screen.hasControlDown();
        if (key == GLFW.GLFW_KEY_F8) {
            CaptureOverlayManager.toggle(client);
            refreshFromConfig();
            return true;
        }
        if (key == GLFW.GLFW_KEY_F9) {
            CaptureOverlayManager.toggleBorderless(client);
            refreshFromConfig();
            return true;
        }
        if (key == GLFW.GLFW_KEY_R && ctrl) {
            reset();
            return true;
        }
        if (key == GLFW.GLFW_KEY_S && ctrl) {
            save();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private interface Setter {
        void set(double value);
    }

    private static final class GuideSlider extends SliderWidget {
        private final String label;
        private final double min;
        private final double max;
        private final String unit;
        private final Setter setter;

        private GuideSlider(String label, double min, double max, double actual, String unit,
                            int x, int y, int w, Setter setter) {
            super(x, y, w, 18, Text.empty(), toNorm(min, max, actual));
            this.label = label;
            this.min = min;
            this.max = max;
            this.unit = unit;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (label == null || unit == null) return;
            setMessage(Text.literal(label + " " + Math.round(actual()) + unit));
        }

        @Override
        protected void applyValue() {
            if (setter == null) return;
            setter.set(actual());
            CaptureOverlayConfig.save();
        }

        private double actual() {
            return min + value * (max - min);
        }

        private static double toNorm(double min, double max, double actual) {
            if (max <= min) return 0.0;
            return Math.max(0.0, Math.min(1.0, (actual - min) / (max - min)));
        }
    }
}
