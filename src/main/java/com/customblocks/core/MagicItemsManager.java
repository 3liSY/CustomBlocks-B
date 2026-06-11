/**
 * MagicItemsManager.java
 *
 * Backing store for the magic-items menu (Group 02, G02.7). The built-in hand tools
 * (Lumina Brush, Chisel, Deleter) are the seeded "magic items"; this manager tracks
 * whether each is enabled and persists that to config/customblocks/data/magic_items.json.
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MagicItemsManager {

    private MagicItemsManager() {} // static-only

    /** A magic item: stable id, display name, and whether it is currently enabled. */
    public record MagicItem(String id, String name, boolean enabled) {}

    private static final Path FILE = Path.of("config/customblocks/data", "magic_items.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Seed: the mod's built-in hand tools + the eight colour shapes (finale fix). Group 06
    // merged Brush + Chisel into the Omni-Tool; Triangles create variants, Squares swap them.
    private static final String[][] SEED = {
            {"omni_tool", "Omni-Tool"},
            {"rainbow_rectangle", "Rainbow Rectangle"},
            {"deleter", "Deleter"},
            {"green_square", "Green Square"},
            {"green_triangle", "Green Triangle"},
            {"yellow_square", "Yellow Square"},
            {"yellow_triangle", "Yellow Triangle"},
            {"red_square", "Red Square"},
            {"red_triangle", "Red Triangle"},
            {"black_square", "Black Square"},
            {"black_triangle", "Black Triangle"},
    };

    private static final Map<String, Boolean> ENABLED = new LinkedHashMap<>();
    private static volatile boolean loaded = false;

    public static synchronized List<MagicItem> all() {
        ensureLoaded();
        List<MagicItem> out = new ArrayList<>();
        for (String[] s : SEED) {
            out.add(new MagicItem(s[0], s[1], ENABLED.getOrDefault(s[0], Boolean.TRUE)));
        }
        return out;
    }

    public static synchronized boolean isEnabled(String id) {
        ensureLoaded();
        return ENABLED.getOrDefault(id, Boolean.TRUE);
    }

    public static synchronized void setEnabled(String id, boolean enabled) {
        ensureLoaded();
        ENABLED.put(id, enabled);
        save();
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        for (String[] s : SEED) ENABLED.put(s[0], Boolean.TRUE);
        try {
            if (Files.exists(FILE)) {
                String json = Files.readString(FILE, StandardCharsets.UTF_8);
                JsonObject o = JsonParser.parseString(json).getAsJsonObject();
                for (String key : o.keySet()) {
                    ENABLED.put(key, o.get(key).getAsBoolean());
                }
            }
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Could not load magic_items.json: {}", e.toString());
        }
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject o = new JsonObject();
            for (Map.Entry<String, Boolean> e : ENABLED.entrySet()) o.addProperty(e.getKey(), e.getValue());
            Files.writeString(FILE, GSON.toJson(o), StandardCharsets.UTF_8);
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Could not save magic_items.json: {}", e.toString());
        }
    }
}
