/**
 * EyedropScreen.java — Group 10 (screen eyedrop), Group 27 §G27.3 frame + §G27.7 §D polish. CLIENT-ONLY.
 *
 * Responsibility: let the player pick a colour by clicking a pixel of their Minecraft screen. The world
 * stays visible behind a faint dim; on click we capture the just-rendered frame and read the pixel under
 * the cursor, then hand the colour off as a pre-filled "/cb palette add #RRGGBB" chat line (callback mode
 * returns it to a caller instead). Samples MC's own framebuffer only — no OS-level screen capture.
 *
 * §G27.7 §D adds: a first-time intro popup (D1, shown once, dismissal persisted in CbScreenPrefs), a
 * permanent short hint, the shared CbHelpOverlay, and a hide-UI toggle (D2 — H key or the button) that
 * removes the title bar + crosshair so pixels under them are sample-able. The live loupe magnifier lives
 * in the in-panel dropper (§B4, CbColorDropper); here the click-sample is already pixel-exact.
 *
 * Depends on: Screen/DrawContext, Framebuffer + ScreenshotRecorder + NativeImage, ChatScreen, CbHelpOverlay,
 *             CbScreenPrefs.
 * Called by: CustomBlocksClient (OpenGuiPayload mode=EYEDROP), and HudColorPicker eyedrop (callback).
 */
package com.customblocks.client.gui;

import com.customblocks.network.payloads.GuiBackPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

@Environment(EnvType.CLIENT)
public class EyedropScreen extends Screen {

    private static final int BACKDROP = 0x33000000; // faint so the picked colour stays true
    private static final int BAR_BG   = 0xAA000000;
    private static final int GOLD     = 0xFF_FF_AA_00;
    private static final int BAR_H    = 42;

    private boolean pendingSample;
    private double sampleX, sampleY;
    private boolean hideUi, showIntro;
    private int introBtnX, introBtnY, introBtnW, introBtnH;

    private final CbHelpOverlay help = new CbHelpOverlay("Screen Eyedrop", List.of(
            new CbHelpOverlay.Group("HOW", List.of(
                    new CbHelpOverlay.Row("Click", "Sample that pixel's colour"),
                    new CbHelpOverlay.Row("H", "Hide / show the UI"),
                    new CbHelpOverlay.Row("Esc", "Cancel")))));
    private ButtonWidget helpBtn, uiBtn;

    private final IntConsumer onSample;
    private final Screen parent;

    public EyedropScreen() {
        super(Text.literal("Eyedrop"));
        this.onSample = null;
        this.parent = null;
    }

    /** Callback variant: hand the sampled colour to {@code onSample}, then reopen {@code parent}. */
    public EyedropScreen(IntConsumer onSample, Screen parent) {
        super(Text.literal("Eyedrop"));
        this.onSample = onSample;
        this.parent = parent;
    }

    @Override
    protected void init() {
        showIntro = !CbScreenPrefs.get().eyedropIntroSeen;
        uiBtn = ButtonWidget.builder(Text.literal("hide"), b -> hideUi = !hideUi)
                .dimensions(width - 70, 10, 40, 16).build();
        helpBtn = ButtonWidget.builder(Text.literal("?"), b -> help.toggle())
                .dimensions(width - 24, 10, 16, 16).build();
        addDrawableChild(uiBtn);
        addDrawableChild(helpBtn);
    }

    @Override
    public boolean shouldPause() { return false; } // keep the world rendering so we can sample it

    @Override
    public void close() {
        if (onSample != null) { if (client != null) client.setScreen(parent); return; }
        ClientPlayNetworking.send(new GuiBackPayload());
        super.close();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) { /* world must stay visible */ }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Sample BEFORE we draw any of our own UI this frame, so the framebuffer holds world only.
        if (pendingSample) { pendingSample = false; sampleAndFinish(); return; }
        ctx.fill(0, 0, width, height, BACKDROP);

        // Hide-UI gates the chrome (title bar, crosshair, buttons) so covered pixels are sample-able.
        if (helpBtn != null) helpBtn.visible = !hideUi;
        if (uiBtn != null)   uiBtn.visible   = !hideUi;

        if (!hideUi) {
            ctx.fill(0, 0, width, BAR_H, BAR_BG);
            ctx.fill(0, BAR_H - 1, width, BAR_H, GOLD);
            ctx.drawTextWithShadow(textRenderer, Text.literal("§6§lScreen Eyedrop"), 8, 10, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, Text.literal("§7click any pixel · H = hide UI · Esc = cancel"), 8, 22, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, Text.literal("§8the colour drops into your palette"), 8, 32, 0xFFFFFFFF);
            ctx.fill(mx - 6, my, mx + 7, my + 1, 0xFFFFFFFF); // crosshair
            ctx.fill(mx, my - 6, mx + 1, my + 7, 0xFFFFFFFF);
        } else {
            ctx.drawTextWithShadow(textRenderer, Text.literal("§8H = show UI · click = sample · Esc"), 4, height - 12, 0xFFFFFFFF);
        }

        super.render(ctx, mx, my, delta); // [?] + hide-UI buttons (visibility set above)
        help.render(ctx, width, height, textRenderer, mx, my);
        if (showIntro) renderIntro(ctx);
    }

    private void renderIntro(DrawContext ctx) {
        int cx = width / 2, cy = height / 2, pw = 260, ph = 92;
        ctx.fill(cx - pw / 2 - 1, cy - ph / 2 - 1, cx + pw / 2 + 1, cy + ph / 2 + 1, GOLD);
        ctx.fill(cx - pw / 2, cy - ph / 2, cx + pw / 2, cy + ph / 2, 0xF0101010);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§6§lScreen Eyedrop"), cx, cy - 32, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§fClick any pixel to grab its colour."), cx, cy - 14, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7It drops straight into your palette."), cx, cy - 2, 0xFFFFFFFF);
        introBtnW = 90; introBtnH = 18; introBtnX = cx - introBtnW / 2; introBtnY = cy + 16;
        ctx.fill(introBtnX, introBtnY, introBtnX + introBtnW, introBtnY + introBtnH, 0xFF2E8B2E);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§fGot it"), cx, introBtnY + 5, 0xFFFFFFFF);
    }

    private void dismissIntro() {
        showIntro = false;
        CbScreenPrefs.get().markEyedropIntroSeen();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (showIntro) {                                          // modal — only "Got it" responds; no sampling
            if (mx >= introBtnX && mx < introBtnX + introBtnW && my >= introBtnY && my < introBtnY + introBtnH) dismissIntro();
            return true;
        }
        if (help.mouseClicked(mx, my)) return true;               // help open → only the red X closes it
        if (super.mouseClicked(mx, my, button)) return true;      // [?] + hide-UI buttons
        if (button == 0) {
            sampleX = mx; sampleY = my;
            pendingSample = true; // captured at the top of the next render()
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (showIntro) {                                          // Enter / Esc dismiss the intro
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_ESCAPE) { dismissIntro(); return true; }
            return true;
        }
        if (help.isOpen()) { if (key == GLFW.GLFW_KEY_ESCAPE) help.close(); return true; }
        if (key == GLFW.GLFW_KEY_SLASH) { help.open(); return true; }
        if (key == GLFW.GLFW_KEY_H) { hideUi = !hideUi; return true; }
        return super.keyPressed(key, scan, mods);                 // Esc → close()
    }

    private void sampleAndFinish() {
        if (client == null) { close(); return; }
        String hex = null;
        int rgb = -1;
        try {
            double sf = client.getWindow().getScaleFactor();
            try (NativeImage shot = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
                int fx = (int) Math.round(sampleX * sf);
                int fy = (int) Math.round(sampleY * sf);
                fx = Math.max(0, Math.min(shot.getWidth() - 1, fx));
                fy = Math.max(0, Math.min(shot.getHeight() - 1, fy));
                int abgr = shot.getColor(fx, fy); // NativeImage packs little-endian ABGR
                int r = abgr & 0xFF, g = (abgr >> 8) & 0xFF, b = (abgr >> 16) & 0xFF;
                rgb = (r << 16) | (g << 8) | b;
                hex = String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b);
            }
        } catch (Exception ignored) {}
        if (hex == null) { close(); return; }
        if (onSample != null) { onSample.accept(rgb); client.setScreen(parent); return; }
        client.setScreen(new ChatScreen("/cb palette add " + hex));
    }
}
