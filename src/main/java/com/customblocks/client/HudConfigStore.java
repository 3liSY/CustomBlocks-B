/**
 * HudConfigStore.java — GROUP 27 §G27.4 (Lego HUD Builder). CLIENT-SIDE ONLY.
 *
 * Responsibility: all disk I/O for HudConfig — load (new brick-list format), migrate the old
 * flat format (hudX/hudY/hudColor/hudShowId/hudShowName → ID + Name bricks), and atomic save
 * (temp file + ATOMIC_MOVE, NFR-13). Split out of HudConfig so HudConfig stays under the 300-
 * line *Config gate. Per-brick (de)serialisation lives in HudField.
 *
 * Depends on: HudConfig (state), HudField, HudFieldType, CustomBlocksConfig, Gson.
 * Called by: HudConfig.load() / HudConfig.save() (the only entry points).
 */
package com.customblocks.client;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.client.hud.HudField;
import com.customblocks.client.hud.HudFieldType;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
final class HudConfigStore {

    private HudConfigStore() {}

    private static final String FILE = "config/customblocks/data/hud-config-server.json";
    private static final Gson   GSON = new Gson();

    static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) { HudConfig.syncFromConfig(); return; }
            JsonObject o = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null) { HudConfig.syncFromConfig(); return; }
            if (o.has("fields")) loadNew(o);
            else                 migrateOld(o);
        } catch (Exception ignored) {
            HudConfig.syncFromConfig();
        }
    }

    private static void loadNew(JsonObject o) {
        HudConfig.visible      = bool(o, "visible",      CustomBlocksConfig.hudEnabled);
        HudConfig.snapEnabled  = bool(o, "snap",         HudConfig.DEF_SNAP);
        HudConfig.bgColor      = num (o, "bgColor",      HudConfig.DEF_BG_COLOR) & 0xFFFFFF;
        HudConfig.bgOpacity    = HudConfig.clamp01((float) dbl(o, "bgOpacity", HudConfig.DEF_BG_OPACITY));
        HudConfig.masterScale  = HudConfig.clampScale((float) dbl(o, "masterScale", HudConfig.DEF_MASTER_SCALE));
        HudConfig.hoverTrigger = num (o, "hoverTrigger", HudConfig.DEF_HOVER_TRIGGER);
        HudConfig.hoverSound   = str (o, "hoverSound",   HudConfig.DEF_HOVER_SOUND);
        HudConfig.hoverVolume  = Math.max(0, Math.min(100, num(o, "hoverVolume", HudConfig.DEF_HOVER_VOLUME)));

        HudConfig.recentColors.clear();
        if (o.has("recent") && o.get("recent").isJsonArray())
            for (JsonElement e : o.getAsJsonArray("recent"))
                HudConfig.recentColors.add(e.getAsInt() & 0xFFFFFF);

        List<HudField> list = new ArrayList<>();
        if (o.has("fields") && o.get("fields").isJsonArray())
            for (JsonElement e : o.getAsJsonArray("fields"))
                if (e.isJsonObject()) list.add(HudField.fromJson(e.getAsJsonObject()));
        if (list.isEmpty()) list = HudConfig.defaultFields();
        HudConfig.fields.clear();
        HudConfig.fields.addAll(list);
    }

    /** Map the pre-rebuild flat format to two bricks (Name + ID) at the saved position. */
    private static void migrateOld(JsonObject o) {
        HudConfig.visible   = bool(o, "hudEnabled", CustomBlocksConfig.hudEnabled);
        HudConfig.bgOpacity = HudConfig.clamp01((float) dbl(o, "hudBgOpacity", HudConfig.DEF_BG_OPACITY));

        int   x     = num(o, "hudX", 6);
        int   y     = num(o, "hudY", 6);
        float scale = HudConfig.clampScale((float) dbl(o, "hudScale", 1.0));
        int   color = HudConfig.parseHex(str(o, "hudColor", "#FFFFFF"), 0xFFFFFF);
        boolean showId   = bool(o, "hudShowId",   true);
        boolean showName = bool(o, "hudShowName", true);

        List<HudField> list = new ArrayList<>();
        int yy = y;
        int step = Math.round(scale * 12f) + 2;
        if (showId)   { HudField f = new HudField(HudFieldType.BLOCK_ID, x, yy, HudField.Anchor.TL, scale); f.color = color; list.add(f); yy += step; }
        if (showName) { HudField f = new HudField(HudFieldType.DISPLAY_NAME, x, yy, HudField.Anchor.TL, scale); f.color = color; list.add(f); }
        if (list.isEmpty()) list = HudConfig.defaultFields();
        HudConfig.fields.clear();
        HudConfig.fields.addAll(list);

        save();   // rewrite immediately in the new format so this runs only once
    }

    static void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());

            JsonObject o = new JsonObject();
            o.addProperty("visible",      HudConfig.visible);
            o.addProperty("snap",         HudConfig.snapEnabled);
            o.addProperty("bgColor",      HudConfig.bgColor & 0xFFFFFF);
            o.addProperty("bgOpacity",    HudConfig.bgOpacity);
            o.addProperty("masterScale",  HudConfig.masterScale);
            o.addProperty("hoverTrigger", HudConfig.hoverTrigger);
            o.addProperty("hoverSound",   HudConfig.hoverSound);
            o.addProperty("hoverVolume",  HudConfig.hoverVolume);

            JsonArray recent = new JsonArray();
            for (int c : HudConfig.recentColors) recent.add(c & 0xFFFFFF);
            o.add("recent", recent);

            JsonArray fields = new JsonArray();
            for (HudField f : HudConfig.fields) fields.add(f.toJson());
            o.add("fields", fields);

            Path tmp = file.resolveSibling("hud-config-server.json.tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }

    // ── JSON helpers ─────────────────────────────────────────────────────────
    private static String  str (JsonObject o, String k, String def)  { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString()  : def; }
    private static int     num (JsonObject o, String k, int def)     { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt()     : def; }
    private static double  dbl (JsonObject o, String k, double def)  { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble()  : def; }
    private static boolean bool(JsonObject o, String k, boolean def) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsBoolean() : def; }
}
