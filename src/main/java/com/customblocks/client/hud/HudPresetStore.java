/**
 * HudPresetStore.java — GROUP 27 §G27.4 (Lego HUD Builder). CLIENT-SIDE ONLY.
 *
 * Responsibility: HUD layout presets. Provides the 3 built-ins (Minimal / Detailed / Builder),
 * lists + saves custom presets as .json files in config/customblocks/exports/Hud_Presets/
 * (atomic write; the exports/ dir already exists per BlockExporter, the subfolder is created on
 * first save), and encodes/decodes a layout to a compact shareable code string ("CBHUD:" +
 * base64 of the field-list JSON; decode also accepts a raw JSON array).
 *
 * Depends on: HudField, HudFieldType, Gson.
 * Called by: HudPresetBrowser (browse / save-as / import / export), HudEditorScreen (copy/paste).
 */
package com.customblocks.client.hud;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class HudPresetStore {

    private HudPresetStore() {}

    public static final String DIR = "config/customblocks/exports/Hud_Presets";
    private static final Gson GSON = new Gson();
    private static final String CODE_PREFIX = "CBHUD:";

    /** A named HUD layout. */
    public record Preset(String name, boolean builtin, List<HudField> fields) {}

    // ── Built-ins ────────────────────────────────────────────────────────────
    public static List<Preset> builtins() {
        List<Preset> out = new ArrayList<>();
        out.add(new Preset("Minimal", true, minimal()));
        out.add(new Preset("Detailed", true, detailed()));
        out.add(new Preset("Builder", true, builder()));
        return out;
    }

    private static List<HudField> minimal() {
        List<HudField> l = new ArrayList<>();
        HudField name = new HudField(HudFieldType.DISPLAY_NAME, 6, 6, HudField.Anchor.TL, 1.0f);
        name.bold = true;
        l.add(name);
        return l;
    }

    private static List<HudField> detailed() {
        List<HudField> l = new ArrayList<>();
        HudField name = new HudField(HudFieldType.DISPLAY_NAME, 6, 6, HudField.Anchor.TL, 1.2f); name.bold = true;
        HudField id   = new HudField(HudFieldType.BLOCK_ID, 6, 24, HudField.Anchor.TL, 0.8f);    id.color = 0xAAAAAA;
        HudField slot = new HudField(HudFieldType.SLOT, 6, 38, HudField.Anchor.TL, 0.7f);        slot.color = 0x888888;
        l.add(name); l.add(id); l.add(slot);
        return l;
    }

    private static List<HudField> builder() {
        List<HudField> l = new ArrayList<>();
        l.add(new HudField(HudFieldType.COORDS, 6, 6, HudField.Anchor.TL, 1.0f));
        l.add(new HudField(HudFieldType.LIGHT, 6, 22, HudField.Anchor.TL, 1.0f));
        l.add(new HudField(HudFieldType.DISTANCE, 6, 38, HudField.Anchor.TL, 1.0f));
        return l;
    }

    // ── Saved presets (disk) ──────────────────────────────────────────────────
    public static List<Preset> saved() {
        List<Preset> out = new ArrayList<>();
        try {
            Path dir = Path.of(DIR);
            if (!Files.isDirectory(dir)) return out;
            try (var stream = Files.list(dir)) {
                stream.filter(p -> p.toString().toLowerCase().endsWith(".json")).sorted().forEach(p -> {
                    try {
                        JsonObject o = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
                        if (o == null) return;
                        String name = o.has("name") ? o.get("name").getAsString() : p.getFileName().toString().replace(".json", "");
                        out.add(new Preset(name, false, fieldsFrom(o.getAsJsonArray("fields"))));
                    } catch (Exception ignored) {}
                });
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static boolean exists(String name) {
        return Files.exists(Path.of(DIR, sanitize(name) + ".json"));
    }

    /** Write (or overwrite) a named preset as .json (atomic). */
    public static boolean save(String name, List<HudField> fields) {
        try {
            Path dir = Path.of(DIR);
            Files.createDirectories(dir);
            JsonObject o = new JsonObject();
            o.addProperty("name", name);
            o.add("fields", fieldsArray(fields));
            Path file = dir.resolve(sanitize(name) + ".json");
            Path tmp = file.resolveSibling(sanitize(name) + ".json.tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) { return false; }
    }

    public static boolean delete(String name) {
        try { return Files.deleteIfExists(Path.of(DIR, sanitize(name) + ".json")); }
        catch (Exception e) { return false; }
    }

    // ── Code string (clipboard sharing) ────────────────────────────────────────
    public static String encode(List<HudField> fields) {
        String json = fieldsArray(fields).toString();
        return CODE_PREFIX + Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    /** Decode a code string (CBHUD: + base64) OR a raw JSON array. Returns null on failure. */
    public static List<HudField> decode(String code) {
        if (code == null) return null;
        try {
            String s = code.trim();
            String json;
            if (s.startsWith(CODE_PREFIX)) {
                json = new String(Base64.getDecoder().decode(s.substring(CODE_PREFIX.length()).trim()), StandardCharsets.UTF_8);
            } else if (s.startsWith("[")) {
                json = s;
            } else {
                return null;
            }
            List<HudField> list = fieldsFrom(JsonParser.parseString(json).getAsJsonArray());
            return list.isEmpty() ? null : list;
        } catch (Exception e) { return null; }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────
    private static JsonArray fieldsArray(List<HudField> fields) {
        JsonArray arr = new JsonArray();
        for (HudField f : fields) arr.add(f.toJson());
        return arr;
    }

    private static List<HudField> fieldsFrom(JsonArray arr) {
        List<HudField> list = new ArrayList<>();
        if (arr == null) return list;
        for (JsonElement e : arr) if (e.isJsonObject()) list.add(HudField.fromJson(e.getAsJsonObject()));
        return list;
    }

    public static List<HudField> copyOf(List<HudField> fields) {
        List<HudField> out = new ArrayList<>(fields.size());
        for (HudField f : fields) out.add(f.copy());
        return out;
    }

    private static String sanitize(String name) {
        String s = name == null ? "preset" : name.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
        return s.isEmpty() ? "preset" : s;
    }
}
