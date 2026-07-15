/**
 * CaptureOverlayStore.java - GROUP 29 (Shorts Framing Overlay). CLIENT-SIDE ONLY.
 *
 * Responsibility: load and save the local Shorts overlay config with atomic writes.
 * The file is local-client only because the overlay is a recording aid, not server state.
 *
 * Depends on: CaptureOverlayConfig, Gson.
 * Called by: CaptureOverlayConfig.
 */
package com.customblocks.client.capture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Environment(EnvType.CLIENT)
final class CaptureOverlayStore {

    private CaptureOverlayStore() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "config/customblocks/data/shorts-overlay-client.json";

    static void load() {
        Path file = Path.of(FILE);
        try {
            if (!Files.exists(file)) {
                save();
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            CaptureOverlayConfig.enabled = bool(root, "enabled", CaptureOverlayConfig.DEF_ENABLED);
            CaptureOverlayConfig.dimOpacity = clamp01(num(root, "dimOpacity", CaptureOverlayConfig.DEF_DIM_OPACITY));
            CaptureOverlayConfig.captionX = clamp01(num(root, "captionX", CaptureOverlayConfig.DEF_CAPTION_X));
            CaptureOverlayConfig.captionY = clamp01(num(root, "captionY", CaptureOverlayConfig.DEF_CAPTION_Y));
            CaptureOverlayConfig.captionW = clampRange(num(root, "captionW", CaptureOverlayConfig.DEF_CAPTION_W), 0.10f, 1.0f);
            CaptureOverlayConfig.captionH = clampRange(num(root, "captionH", CaptureOverlayConfig.DEF_CAPTION_H), 0.05f, 0.50f);
            CaptureOverlayConfig.safeMarginX = clampRange(num(root, "safeMarginX", CaptureOverlayConfig.DEF_SAFE_MARGIN_X), 0.0f, 0.30f);
            CaptureOverlayConfig.safeMarginTop = clampRange(num(root, "safeMarginTop", CaptureOverlayConfig.DEF_SAFE_MARGIN_TOP), 0.0f, 0.40f);
            CaptureOverlayConfig.safeMarginBottom = clampRange(num(root, "safeMarginBottom", CaptureOverlayConfig.DEF_SAFE_MARGIN_BOTTOM), 0.0f, 0.45f);
            CaptureOverlayConfig.subjectW = clampRange(num(root, "subjectW", CaptureOverlayConfig.DEF_SUBJECT_W), 0.20f, 1.0f);
            CaptureOverlayConfig.subjectH = clampRange(num(root, "subjectH", CaptureOverlayConfig.DEF_SUBJECT_H), 0.20f, 1.0f);
            CaptureOverlayConfig.obsMode = bool(root, "obsMode", CaptureOverlayConfig.DEF_OBS_MODE);
            CaptureOverlayConfig.showDim = bool(root, "showDim", CaptureOverlayConfig.DEF_SHOW_DIM);
            CaptureOverlayConfig.showFrame = bool(root, "showFrame", CaptureOverlayConfig.DEF_SHOW_FRAME);
            CaptureOverlayConfig.showSafe = bool(root, "showSafe", CaptureOverlayConfig.DEF_SHOW_SAFE);
            CaptureOverlayConfig.showCaption = bool(root, "showCaption", CaptureOverlayConfig.DEF_SHOW_CAPTION);
            CaptureOverlayConfig.showSubject = bool(root, "showSubject", CaptureOverlayConfig.DEF_SHOW_SUBJECT);
            CaptureOverlayConfig.lastMode = str(root, "lastMode", "off");
        } catch (Exception ignored) {
            CaptureOverlayConfig.enabled = CaptureOverlayConfig.DEF_ENABLED;
        }
    }

    static void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("enabled", CaptureOverlayConfig.enabled);
            root.addProperty("dimOpacity", CaptureOverlayConfig.dimOpacity);
            root.addProperty("captionX", CaptureOverlayConfig.captionX);
            root.addProperty("captionY", CaptureOverlayConfig.captionY);
            root.addProperty("captionW", CaptureOverlayConfig.captionW);
            root.addProperty("captionH", CaptureOverlayConfig.captionH);
            root.addProperty("safeMarginX", CaptureOverlayConfig.safeMarginX);
            root.addProperty("safeMarginTop", CaptureOverlayConfig.safeMarginTop);
            root.addProperty("safeMarginBottom", CaptureOverlayConfig.safeMarginBottom);
            root.addProperty("subjectW", CaptureOverlayConfig.subjectW);
            root.addProperty("subjectH", CaptureOverlayConfig.subjectH);
            root.addProperty("obsMode", CaptureOverlayConfig.obsMode);
            root.addProperty("showDim", CaptureOverlayConfig.showDim);
            root.addProperty("showFrame", CaptureOverlayConfig.showFrame);
            root.addProperty("showSafe", CaptureOverlayConfig.showSafe);
            root.addProperty("showCaption", CaptureOverlayConfig.showCaption);
            root.addProperty("showSubject", CaptureOverlayConfig.showSubject);
            root.addProperty("lastMode", CaptureOverlayConfig.lastMode);
            Path tmp = file.resolveSibling("shorts-overlay-client.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }

    private static String str(JsonObject o, String k, String d) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : d;
    }

    private static boolean bool(JsonObject o, String k, boolean d) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsBoolean() : d;
    }

    private static float num(JsonObject o, String k, float d) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsFloat() : d;
    }

    private static float clamp01(float v) {
        return CaptureOverlayConfig.clamp01(v);
    }

    private static float clampRange(float v, float min, float max) {
        return CaptureOverlayConfig.clampRange(v, min, max);
    }
}
