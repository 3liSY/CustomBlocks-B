/**
 * CaptureOverlayLayout.java - GROUP 29 (Record Overlay Studio). CLIENT-SIDE ONLY.
 *
 * Responsibility: one serializable snapshot of the built-in Shorts overlay guides.
 * Custom mark data will extend this shape in a later Build B slice.
 */
package com.customblocks.client.capture;

import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record CaptureOverlayLayout(
        float dimOpacity,
        float captionX,
        float captionY,
        float captionW,
        float captionH,
        float safeMarginX,
        float safeMarginTop,
        float safeMarginBottom,
        float subjectW,
        float subjectH,
        boolean showDim,
        boolean showFrame,
        boolean showSafe,
        boolean showCaption,
        boolean showSubject
) {
    public static CaptureOverlayLayout current() {
        return new CaptureOverlayLayout(
                CaptureOverlayConfig.dimOpacity,
                CaptureOverlayConfig.captionX,
                CaptureOverlayConfig.captionY,
                CaptureOverlayConfig.captionW,
                CaptureOverlayConfig.captionH,
                CaptureOverlayConfig.safeMarginX,
                CaptureOverlayConfig.safeMarginTop,
                CaptureOverlayConfig.safeMarginBottom,
                CaptureOverlayConfig.subjectW,
                CaptureOverlayConfig.subjectH,
                CaptureOverlayConfig.showDim,
                CaptureOverlayConfig.showFrame,
                CaptureOverlayConfig.showSafe,
                CaptureOverlayConfig.showCaption,
                CaptureOverlayConfig.showSubject
        );
    }

    public void apply() {
        CaptureOverlayConfig.dimOpacity = CaptureOverlayConfig.clamp01(dimOpacity);
        CaptureOverlayConfig.captionX = CaptureOverlayConfig.clamp01(captionX);
        CaptureOverlayConfig.captionY = CaptureOverlayConfig.clamp01(captionY);
        CaptureOverlayConfig.captionW = CaptureOverlayConfig.clampRange(captionW, 0.10f, 1.0f);
        CaptureOverlayConfig.captionH = CaptureOverlayConfig.clampRange(captionH, 0.05f, 0.50f);
        CaptureOverlayConfig.safeMarginX = CaptureOverlayConfig.clampRange(safeMarginX, 0.0f, 0.30f);
        CaptureOverlayConfig.safeMarginTop = CaptureOverlayConfig.clampRange(safeMarginTop, 0.0f, 0.40f);
        CaptureOverlayConfig.safeMarginBottom = CaptureOverlayConfig.clampRange(safeMarginBottom, 0.0f, 0.45f);
        CaptureOverlayConfig.subjectW = CaptureOverlayConfig.clampRange(subjectW, 0.20f, 1.0f);
        CaptureOverlayConfig.subjectH = CaptureOverlayConfig.clampRange(subjectH, 0.20f, 1.0f);
        CaptureOverlayConfig.showDim = showDim;
        CaptureOverlayConfig.showFrame = showFrame;
        CaptureOverlayConfig.showSafe = showSafe;
        CaptureOverlayConfig.showCaption = showCaption;
        CaptureOverlayConfig.showSubject = showSubject;
        CaptureOverlayConfig.save();
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("dimOpacity", dimOpacity);
        root.addProperty("captionX", captionX);
        root.addProperty("captionY", captionY);
        root.addProperty("captionW", captionW);
        root.addProperty("captionH", captionH);
        root.addProperty("safeMarginX", safeMarginX);
        root.addProperty("safeMarginTop", safeMarginTop);
        root.addProperty("safeMarginBottom", safeMarginBottom);
        root.addProperty("subjectW", subjectW);
        root.addProperty("subjectH", subjectH);
        root.addProperty("showDim", showDim);
        root.addProperty("showFrame", showFrame);
        root.addProperty("showSafe", showSafe);
        root.addProperty("showCaption", showCaption);
        root.addProperty("showSubject", showSubject);
        return root;
    }

    public static CaptureOverlayLayout fromJson(JsonObject root) {
        return new CaptureOverlayLayout(
                num(root, "dimOpacity", CaptureOverlayConfig.DEF_DIM_OPACITY),
                num(root, "captionX", CaptureOverlayConfig.DEF_CAPTION_X),
                num(root, "captionY", CaptureOverlayConfig.DEF_CAPTION_Y),
                num(root, "captionW", CaptureOverlayConfig.DEF_CAPTION_W),
                num(root, "captionH", CaptureOverlayConfig.DEF_CAPTION_H),
                num(root, "safeMarginX", CaptureOverlayConfig.DEF_SAFE_MARGIN_X),
                num(root, "safeMarginTop", CaptureOverlayConfig.DEF_SAFE_MARGIN_TOP),
                num(root, "safeMarginBottom", CaptureOverlayConfig.DEF_SAFE_MARGIN_BOTTOM),
                num(root, "subjectW", CaptureOverlayConfig.DEF_SUBJECT_W),
                num(root, "subjectH", CaptureOverlayConfig.DEF_SUBJECT_H),
                bool(root, "showDim", CaptureOverlayConfig.DEF_SHOW_DIM),
                bool(root, "showFrame", CaptureOverlayConfig.DEF_SHOW_FRAME),
                bool(root, "showSafe", CaptureOverlayConfig.DEF_SHOW_SAFE),
                bool(root, "showCaption", CaptureOverlayConfig.DEF_SHOW_CAPTION),
                bool(root, "showSubject", CaptureOverlayConfig.DEF_SHOW_SUBJECT)
        );
    }

    private static boolean bool(JsonObject root, String key, boolean def) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsBoolean() : def;
    }

    private static float num(JsonObject root, String key, float def) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsFloat() : def;
    }
}
