/**
 * CaptureOverlayLayouts.java - GROUP 29 (Record Overlay Studio). CLIENT-SIDE ONLY.
 *
 * Responsibility: named local layout persistence for the Shorts overlay.
 */
package com.customblocks.client.capture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class CaptureOverlayLayouts {

    private CaptureOverlayLayouts() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "config/customblocks/data/shorts-overlay-layouts.json";

    public static String saveCurrent(String rawName) {
        String name = cleanName(rawName);
        if (name.isEmpty()) return "";
        JsonObject root = readRoot();
        layouts(root).add(name, CaptureOverlayLayout.current().toJson());
        writeRoot(root);
        return name;
    }

    public static boolean load(String rawName) {
        String name = cleanName(rawName);
        if (name.isEmpty()) return false;
        JsonObject all = layouts(readRoot());
        if (!all.has(name) || !all.get(name).isJsonObject()) return false;
        CaptureOverlayLayout.fromJson(all.getAsJsonObject(name)).apply();
        return true;
    }

    public static boolean delete(String rawName) {
        String name = cleanName(rawName);
        if (name.isEmpty()) return false;
        JsonObject root = readRoot();
        JsonObject all = layouts(root);
        if (!all.has(name)) return false;
        all.remove(name);
        writeRoot(root);
        return true;
    }

    public static List<String> listNames() {
        JsonObject all = layouts(readRoot());
        List<String> names = new ArrayList<>(all.keySet());
        Collections.sort(names);
        return names;
    }

    public static String exportLayout(String rawName) {
        String name = cleanName(rawName);
        if (name.isEmpty()) return "";
        JsonObject all = layouts(readRoot());
        if (!all.has(name) || !all.get(name).isJsonObject()) return "";
        JsonObject out = new JsonObject();
        out.addProperty("type", "customblocks:record_overlay_layout");
        out.addProperty("name", name);
        out.add("layout", all.getAsJsonObject(name));
        return GSON.toJson(out);
    }

    public static String importLayout(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String name = root.has("name") ? cleanName(root.get("name").getAsString()) : "imported";
            JsonObject layout = root.has("layout") && root.get("layout").isJsonObject()
                    ? root.getAsJsonObject("layout")
                    : root;
            if (name.isEmpty()) name = "imported";
            CaptureOverlayLayout parsed = CaptureOverlayLayout.fromJson(layout);
            JsonObject file = readRoot();
            layouts(file).add(name, parsed.toJson());
            writeRoot(file);
            return name;
        } catch (Exception ignored) {
            return "";
        }
    }

    private static JsonObject readRoot() {
        Path file = Path.of(FILE);
        try {
            if (Files.exists(file)) {
                JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                if (parsed.isJsonObject()) return parsed.getAsJsonObject();
            }
        } catch (Exception ignored) {}
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.add("layouts", new JsonObject());
        return root;
    }

    private static void writeRoot(JsonObject root) {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("shorts-overlay-layouts.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }

    private static JsonObject layouts(JsonObject root) {
        if (!root.has("layouts") || !root.get("layouts").isJsonObject()) {
            root.add("layouts", new JsonObject());
        }
        return root.getAsJsonObject("layouts");
    }

    private static String cleanName(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        while (s.contains("__")) s = s.replace("__", "_");
        if (s.length() > 40) s = s.substring(0, 40);
        return s;
    }
}
