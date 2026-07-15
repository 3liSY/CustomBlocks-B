/**
 * ShortsGeometry.java - GROUP 29 (Shorts Framing Overlay). CLIENT-SIDE ONLY.
 *
 * Responsibility: shared 9:16 frame math for both the native overlay window and
 * the in-game exclusive-fullscreen fallback.
 *
 * Depends on: CaptureOverlayConfig.
 * Called by: CaptureOverlayPanel, CaptureOverlayFallbackRenderer.
 */
package com.customblocks.client.capture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record ShortsGeometry(
        int frameX, int frameY, int frameW, int frameH,
        int safeX, int safeY, int safeW, int safeH,
        int captionX, int captionY, int captionW, int captionH,
        int subjectX, int subjectY, int subjectW, int subjectH
) {
    public static ShortsGeometry of(int width, int height) {
        int frameH = height;
        int frameW = Math.round(height * 9f / 16f);
        if (frameW > width) {
            frameW = width;
            frameH = Math.round(width * 16f / 9f);
        }
        int frameX = (width - frameW) / 2;
        int frameY = (height - frameH) / 2;

        int safeX = frameX + Math.round(frameW * CaptureOverlayConfig.safeMarginX);
        int safeY = frameY + Math.round(frameH * CaptureOverlayConfig.safeMarginTop);
        int safeW = frameW - Math.round(frameW * CaptureOverlayConfig.safeMarginX * 2f);
        int safeH = frameH - Math.round(frameH * (CaptureOverlayConfig.safeMarginTop + CaptureOverlayConfig.safeMarginBottom));

        int captionX = frameX + Math.round(frameW * CaptureOverlayConfig.captionX);
        int captionY = frameY + Math.round(frameH * CaptureOverlayConfig.captionY);
        int captionW = Math.round(frameW * CaptureOverlayConfig.captionW);
        int captionH = Math.round(frameH * CaptureOverlayConfig.captionH);

        int subjectW = Math.round(frameW * CaptureOverlayConfig.subjectW);
        int subjectH = Math.round(frameH * CaptureOverlayConfig.subjectH);
        int subjectX = frameX + (frameW - subjectW) / 2;
        int subjectY = frameY + (frameH - subjectH) / 2;

        return new ShortsGeometry(
                frameX, frameY, frameW, frameH,
                safeX, safeY, safeW, safeH,
                captionX, captionY, captionW, captionH,
                subjectX, subjectY, subjectW, subjectH
        );
    }
}
