/**
 * EyedropScreen.java — Group 10 (screen eyedrop). CLIENT-SIDE ONLY.
 *
 * Responsibility: let the player pick a colour by clicking a pixel of their Minecraft screen.
 * The world stays visible behind a faint dim; on click we capture the just-rendered frame and read
 * the pixel under the cursor, then hand the colour off as a pre-filled "/cb palette add #RRGGBB"
 * chat line (so it lands in the player's palette through the tested command — no new packet, server
 * stays authoritative). Samples MC's own framebuffer only — no OS-level screen capture.
 *
 * Depends on: Screen/DrawContext, Framebuffer + ScreenshotRecorder + NativeImage, ChatScreen.
 * Called by: CustomBlocksClient (OpenGuiPayload mode=EYEDROP).
 */
package com.customblocks.client.gui;

import com.customblocks.network.payloads.GuiBackPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public class EyedropScreen extends Screen {

    private boolean pendingSample;
    private double sampleX, sampleY;

    public EyedropScreen() {
        super(Text.literal("Eyedrop"));
    }

    @Override
    public boolean shouldPause() { return false; } // keep the world rendering so we can sample it

    /** Esc / cancel — ask the server to reopen the menu we came from (picking a colour doesn't call this). */
    @Override
    public void close() {
        ClientPlayNetworking.send(new GuiBackPayload());
        super.close();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        // no blur/darken — the world must stay visible to sample
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Sample BEFORE we draw any of our own UI this frame, so the framebuffer holds world only.
        if (pendingSample) {
            pendingSample = false;
            sampleAndFinish();
            return;
        }
        super.render(ctx, mx, my, delta);
        ctx.fill(0, 0, width, height, 0x33000000); // very faint so the picked colour stays true
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§e§lScreen Eyedrop"), width / 2, 16, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Click any pixel to grab its colour · Esc to cancel"), width / 2, 30, 0xFFFFFFFF);
        // Crosshair at the cursor.
        ctx.fill(mx - 6, my, mx + 7, my + 1, 0xFFFFFFFF);
        ctx.fill(mx, my - 6, mx + 1, my + 7, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            sampleX = mx; sampleY = my;
            pendingSample = true; // captured at the top of the next render()
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void sampleAndFinish() {
        if (client == null) { close(); return; }
        String hex = null;
        try {
            double sf = client.getWindow().getScaleFactor();
            try (NativeImage shot = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
                int fx = (int) Math.round(sampleX * sf);
                int fy = (int) Math.round(sampleY * sf);
                fx = Math.max(0, Math.min(shot.getWidth() - 1, fx));
                fy = Math.max(0, Math.min(shot.getHeight() - 1, fy));
                int abgr = shot.getColor(fx, fy); // NativeImage packs little-endian ABGR
                int r = abgr & 0xFF, g = (abgr >> 8) & 0xFF, b = (abgr >> 16) & 0xFF;
                hex = String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b);
            }
        } catch (Exception ignored) {}
        if (hex == null) { close(); return; }
        // Hand the colour to the tested palette command (player presses Enter to commit).
        client.setScreen(new ChatScreen("/cb palette add " + hex));
    }
}
