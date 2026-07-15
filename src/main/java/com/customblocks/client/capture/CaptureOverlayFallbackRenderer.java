/**
 * CaptureOverlayFallbackRenderer.java - GROUP 29 (Shorts Framing Overlay). CLIENT-SIDE ONLY.
 *
 * Responsibility: draw the degraded true-fullscreen fallback. These marks stay in the
 * 16:9 sidebars that are removed by a center 9:16 crop.
 *
 * Depends on: CaptureOverlayManager, ShortsGeometry.
 * Called by: HudRenderMixin.
 */
package com.customblocks.client.capture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public final class CaptureOverlayFallbackRenderer {

    private CaptureOverlayFallbackRenderer() {}

    private static final int EDGE = 0xFFFFD038;
    private static final int SIDE = 0xAA000000;

    public static void render(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!CaptureOverlayManager.exclusiveFallbackActive(client)) return;

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        ShortsGeometry s = ShortsGeometry.of(w, h);
        int leftLine = Math.max(0, s.frameX() - 8);
        int rightLine = Math.min(w - 2, s.frameX() + s.frameW() + 6);
        int tick = Math.max(14, w / 80);

        if (CaptureOverlayConfig.showDim) {
            ctx.fill(0, 0, s.frameX(), h, SIDE);
            ctx.fill(s.frameX() + s.frameW(), 0, w, h, SIDE);
        }

        if (!CaptureOverlayConfig.showFrame) return;
        ctx.fill(leftLine, s.frameY(), leftLine + 2, s.frameY() + s.frameH(), EDGE);
        ctx.fill(rightLine, s.frameY(), rightLine + 2, s.frameY() + s.frameH(), EDGE);
        ctx.fill(leftLine - tick, s.frameY(), leftLine + 2, s.frameY() + 2, EDGE);
        ctx.fill(leftLine - tick, s.frameY() + s.frameH() - 2, leftLine + 2, s.frameY() + s.frameH(), EDGE);
        ctx.fill(rightLine, s.frameY(), rightLine + tick, s.frameY() + 2, EDGE);
        ctx.fill(rightLine, s.frameY() + s.frameH() - 2, rightLine + tick, s.frameY() + s.frameH(), EDGE);
    }
}
