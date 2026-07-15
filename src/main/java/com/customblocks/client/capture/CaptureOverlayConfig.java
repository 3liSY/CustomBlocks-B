/**
 * CaptureOverlayConfig.java - GROUP 29 (Shorts Framing Overlay). CLIENT-SIDE ONLY.
 *
 * Responsibility: live client state for the capture helper overlay. This is intentionally
 * separate from the server config: the guide is a local recording aid and never changes
 * server state.
 *
 * Depends on: CaptureOverlayStore.
 * Called by: CaptureOverlayManager, CaptureOverlayPanel, CaptureOverlayFallbackRenderer.
 */
package com.customblocks.client.capture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CaptureOverlayConfig {

    private CaptureOverlayConfig() {}

    public static final boolean DEF_ENABLED = false;
    public static final float DEF_DIM_OPACITY = 0.48f;
    public static final float DEF_CAPTION_X = 0.12f;
    public static final float DEF_CAPTION_Y = 0.70f;
    public static final float DEF_CAPTION_W = 0.76f;
    public static final float DEF_CAPTION_H = 0.16f;
    public static final float DEF_SAFE_MARGIN_X = 0.07f;
    public static final float DEF_SAFE_MARGIN_TOP = 0.08f;
    public static final float DEF_SAFE_MARGIN_BOTTOM = 0.18f;
    public static final float DEF_SUBJECT_W = 0.82f;
    public static final float DEF_SUBJECT_H = 0.72f;
    public static final boolean DEF_OBS_MODE = false;
    public static final boolean DEF_SHOW_DIM = true;
    public static final boolean DEF_SHOW_FRAME = true;
    public static final boolean DEF_SHOW_SAFE = true;
    public static final boolean DEF_SHOW_CAPTION = true;
    public static final boolean DEF_SHOW_SUBJECT = true;

    public static boolean enabled = DEF_ENABLED;
    public static float dimOpacity = DEF_DIM_OPACITY;
    public static float captionX = DEF_CAPTION_X;
    public static float captionY = DEF_CAPTION_Y;
    public static float captionW = DEF_CAPTION_W;
    public static float captionH = DEF_CAPTION_H;
    public static float safeMarginX = DEF_SAFE_MARGIN_X;
    public static float safeMarginTop = DEF_SAFE_MARGIN_TOP;
    public static float safeMarginBottom = DEF_SAFE_MARGIN_BOTTOM;
    public static float subjectW = DEF_SUBJECT_W;
    public static float subjectH = DEF_SUBJECT_H;
    public static boolean obsMode = DEF_OBS_MODE;
    public static boolean showDim = DEF_SHOW_DIM;
    public static boolean showFrame = DEF_SHOW_FRAME;
    public static boolean showSafe = DEF_SHOW_SAFE;
    public static boolean showCaption = DEF_SHOW_CAPTION;
    public static boolean showSubject = DEF_SHOW_SUBJECT;
    public static String lastMode = "off";

    public static void load() {
        CaptureOverlayStore.load();
    }

    public static void save() {
        CaptureOverlayStore.save();
    }

    public static void resetGuideDefaults() {
        dimOpacity = DEF_DIM_OPACITY;
        captionX = DEF_CAPTION_X;
        captionY = DEF_CAPTION_Y;
        captionW = DEF_CAPTION_W;
        captionH = DEF_CAPTION_H;
        safeMarginX = DEF_SAFE_MARGIN_X;
        safeMarginTop = DEF_SAFE_MARGIN_TOP;
        safeMarginBottom = DEF_SAFE_MARGIN_BOTTOM;
        subjectW = DEF_SUBJECT_W;
        subjectH = DEF_SUBJECT_H;
        showDim = DEF_SHOW_DIM;
        showFrame = DEF_SHOW_FRAME;
        showSafe = DEF_SHOW_SAFE;
        showCaption = DEF_SHOW_CAPTION;
        showSubject = DEF_SHOW_SUBJECT;
    }

    static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    static float clampRange(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
