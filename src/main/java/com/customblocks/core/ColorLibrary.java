/**
 * ColorLibrary.java
 *
 * Responsibility: The named colour library behind /cb customcolor — 29 preset colours in
 * display order plus name→hex resolution (exact name → alias → "#RRGGBB" parse). The colour
 * data and alias table are recycled from the old project's proven ColorLibrary; the code is
 * a clean rewrite.
 *
 * Depends on: nothing (pure data + parsing).
 * Called by:  gui/chest/CustomColorMenu (the colour grid), command/handlers/HexCommands
 *             (/cb customcolor <colour>), item/CustomColorToolItem (labelling).
 */
package com.customblocks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ColorLibrary {

    private ColorLibrary() {} // static-only

    /** One named colour. {@code hex} is always "#RRGGBB". */
    public record LibColor(String name, String hex) {
        /** The packed 0xRRGGBB int. */
        public int rgb() { return Integer.parseInt(hex.substring(1), 16); }
    }

    /** All 29 preset colours in display order: vibrants, then neutrals/warm, then rich/pastel. */
    public static final List<LibColor> ALL;

    private static final Map<String, LibColor> BY_NAME;

    static {
        List<LibColor> all = new ArrayList<>(List.of(
                // Vibrants
                new LibColor("Red",        "#EE3333"),
                new LibColor("Orange",     "#FF8800"),
                new LibColor("Yellow",     "#FFDD00"),
                new LibColor("Lime",       "#44DD00"),
                new LibColor("Green",      "#1A8C00"),
                new LibColor("Cyan",       "#00BBCC"),
                new LibColor("Blue",       "#1155CC"),
                new LibColor("Purple",     "#7700CC"),
                new LibColor("Magenta",    "#EE00BB"),
                // Neutrals + warm
                new LibColor("Pink",       "#FF88BB"),
                new LibColor("White",      "#F5F5F5"),
                new LibColor("Light Gray", "#C8C8C8"),
                new LibColor("Gray",       "#808080"),
                new LibColor("Dark Gray",  "#404040"),
                new LibColor("Black",      "#111111"),
                new LibColor("Brown",      "#8B4513"),
                new LibColor("Crimson",    "#CC1133"),
                new LibColor("Maroon",     "#800000"),
                new LibColor("Gold",       "#FFD700"),
                // Rich + pastel
                new LibColor("Forest",     "#1E6B1E"),
                new LibColor("Navy",       "#003377"),
                new LibColor("Indigo",     "#4B0082"),
                new LibColor("Coral",      "#FF5E47"),
                new LibColor("Baby Blue",  "#88BBEE"),
                new LibColor("Lavender",   "#BB88DD"),
                new LibColor("Mint",       "#AAEEBB"),
                new LibColor("Peach",      "#FFCC99"),
                new LibColor("Rose",       "#FF8899"),
                new LibColor("Butter",     "#FFFAAA")));
        ALL = Collections.unmodifiableList(all);

        Map<String, LibColor> map = new LinkedHashMap<>();
        for (LibColor c : ALL) map.put(c.name().toLowerCase(Locale.ROOT), c);
        alias(map, "light gray", "light_gray", "lightgray", "silver");
        alias(map, "dark gray",  "dark_gray", "darkgray", "charcoal");
        alias(map, "baby blue",  "baby_blue", "sky", "skyblue", "sky blue");
        alias(map, "black",  "obsidian", "noir");
        alias(map, "white",  "snow", "ivory");
        alias(map, "red",    "scarlet", "ruby");
        alias(map, "maroon", "dark red", "darkred", "wine", "burgundy");
        alias(map, "green",  "emerald", "olive");
        alias(map, "blue",   "sapphire", "cobalt");
        alias(map, "purple", "violet", "amethyst");
        alias(map, "gold",   "yellow-gold", "auric");
        alias(map, "brown",  "wood", "dirt", "earth");
        alias(map, "coral",  "salmon", "terracotta");
        alias(map, "rose",   "blush");
        alias(map, "mint",   "seafoam");
        alias(map, "forest", "dark green", "darkgreen", "jungle");
        alias(map, "navy",   "midnight", "darkblue", "dark blue");
        alias(map, "indigo", "deep purple", "deeppurple");
        alias(map, "peach",  "apricot");
        BY_NAME = Collections.unmodifiableMap(map);
    }

    private static void alias(Map<String, LibColor> map, String canonical, String... aliases) {
        LibColor target = map.get(canonical);
        if (target == null) return;
        for (String a : aliases) map.putIfAbsent(a.toLowerCase(Locale.ROOT), target);
    }

    /**
     * Resolve a colour input to canonical "#RRGGBB": exact name → alias → hex parse
     * (a missing "#" and 3-digit shorthand are tolerated). Null when unparseable.
     */
    public static String resolve(String input) {
        if (input == null || input.isBlank()) return null;
        String low = input.trim().toLowerCase(Locale.ROOT);
        LibColor named = BY_NAME.get(low);
        if (named != null) return named.hex();
        String s = low.startsWith("#") ? low.substring(1) : low;
        if (s.matches("[0-9a-f]{3}")) {
            s = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2);
        }
        if (s.matches("[0-9a-f]{6}")) return "#" + s.toUpperCase(Locale.ROOT);
        return null;
    }

    /** The library name for an exact "#RRGGBB" hex, or null when it isn't a preset. */
    public static String nameForHex(String hex) {
        if (hex == null) return null;
        for (LibColor c : ALL) {
            if (c.hex().equalsIgnoreCase(hex)) return c.name();
        }
        return null;
    }

    /** Up to 3 preset names similar to a failed input (for friendlier errors). */
    public static List<String> suggest(String input) {
        List<String> out = new ArrayList<>();
        if (input == null) return out;
        String low = input.trim().toLowerCase(Locale.ROOT);
        for (LibColor c : ALL) {
            String name = c.name().toLowerCase(Locale.ROOT);
            if (name.contains(low) || (name.length() >= 3 && low.contains(name.substring(0, 3)))) {
                out.add(c.name());
                if (out.size() >= 3) break;
            }
        }
        return out;
    }
}
