/**
 * AutoCategorizeManager.java — suggest a category for a block from its name (Group 11, G11.8).
 *
 * A lightweight, deterministic keyword matcher over the block's display name + id. Material
 * keywords are checked before colour keywords, so "RedBrickWall" → "bricks" (not "red"). Pure
 * read; no disk, no state — safe to call from a command or right after block creation.
 *
 * Texture-colour analysis is intentionally out of scope here: name matching is reliable,
 * instant and explainable, which is what the suggestion flow needs.
 *
 * Depends on: SlotData
 * Called by:  CategoryCommands.suggestOnCreate() (create-time hint on /cb create)
 */
package com.customblocks.core;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class AutoCategorizeManager {

    private AutoCategorizeManager() {} // static-only

    /** keyword → category, in priority order (materials first, colours last). First hit wins. */
    private static final Map<String, String> RULES = new LinkedHashMap<>();
    static {
        // Materials / block families.
        put("bricks", "brick");
        put("wood", "plank", "wood", "log", "oak", "birch", "spruce", "acacia", "jungle", "mangrove", "cherry", "bamboo");
        put("stone", "cobble", "deepslate", "granite", "andesite", "diorite", "tuff", "basalt", "blackstone", "stone");
        put("sandstone", "sandstone");
        put("glass", "glass");
        put("ores", "ore");
        put("metal", "iron", "gold", "copper", "steel", "netherite", "bronze", "metal");
        put("wool", "wool", "carpet");
        put("concrete", "concrete");
        put("terrain", "dirt", "grass", "sand", "gravel", "mud", "clay", "soil");
        put("plants", "leaf", "leaves", "flower", "plant", "sapling", "vine", "moss");
        put("ice", "ice");
        put("snow", "snow");
        put("nether", "nether", "soul");
        // Colours (fallback when no material matched).
        put("red", "red", "crimson");
        put("orange", "orange");
        put("yellow", "yellow");
        put("green", "green");
        put("blue", "blue", "cyan");
        put("purple", "purple", "magenta");
        put("pink", "pink");
        put("brown", "brown");
        put("black", "black");
        put("white", "white");
        put("gray", "gray", "grey");
    }

    private static void put(String category, String... keywords) {
        for (String k : keywords) RULES.put(k, category);
    }

    /**
     * Suggest a category for the block, or "" when nothing matches. Matches keywords as
     * substrings of "displayName id" (lower-cased).
     */
    public static String suggest(SlotData d) {
        if (d == null) return "";
        String hay = (d.displayName() + " " + d.customId()).toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> e : RULES.entrySet()) {
            if (hay.contains(e.getKey())) return e.getValue();
        }
        return "";
    }
}
