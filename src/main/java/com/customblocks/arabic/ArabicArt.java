/**
 * ArabicArt.java
 *
 * Responsibility: Catalog of the hand-drawn Arabic art set bundled in the JAR
 * (assets/customblocks/arabic_art/<color>/<file>_<color>.png). 56 glyphs x 4 colors = 224:
 *   - 36 letters + extra forms (isolated form)
 *   - 10 Eastern Arabic numerals  -> id/display A0..A9
 *   - 10 Western (English) numerals -> id/display E0..E9   (art file is still num_0..num_9)
 *
 * Naming (developer's locked rule, 2026-06-15): letters keep their art names; Eastern numbers
 * stay A#, Western numbers are marked E#; every block name is Title-Cased with a color suffix
 * (e.g. Alef_Black, Ta_Marbuta_Red, A0_Black, E5_Green). Block id = arabic_<idBase>_<color>.
 *
 * Depends on: NameCase
 * Called by:  ArabicBlockRegistry, ArabicCommands
 */
package com.customblocks.arabic;

import com.customblocks.core.NameCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ArabicArt {

    private ArabicArt() {}

    /** The four bundled colors (folder + filename suffix). */
    public static final String[] COLORS = {"black", "red", "green", "yellow"};

    public enum Group { LETTER, EASTERN, WESTERN }

    /**
     * One glyph in the art set.
     * @param fileBase art filename base without color (e.g. "alef", "a0", "num_0")
     * @param idBase   id/display base without color (e.g. "alef", "a0", "e0")
     */
    public record Glyph(String fileBase, String idBase, Group group) {}

    /** 36 letters + extra forms, exactly as the art files are named. */
    private static final String[] LETTERS = {
        "ain", "alef", "alef_hamza_above", "alef_hamza_below", "alef_madda", "alef_maqsura",
        "ba", "dad", "dal", "fa", "ghain", "ha2", "ha", "hamza", "jeem", "kaf", "kha", "lam",
        "meem", "noon", "qaf", "ra", "sad", "seen", "sheen", "ta2", "ta", "ta_marbuta",
        "tha2", "tha", "thal", "waw", "waw_hamza", "ya", "ya_hamza", "zay"
    };

    /** All 56 glyphs (letters, then Eastern A0-A9, then Western E0-E9). */
    public static final List<Glyph> ALL;
    static {
        List<Glyph> all = new ArrayList<>();
        for (String l : LETTERS) all.add(new Glyph(l, l, Group.LETTER));
        for (int i = 0; i <= 9; i++) all.add(new Glyph("a" + i, "a" + i, Group.EASTERN));
        for (int i = 0; i <= 9; i++) all.add(new Glyph("num_" + i, "e" + i, Group.WESTERN));
        ALL = List.copyOf(all);
    }

    /** Bundled JAR resource path for one glyph+color. */
    public static String resource(Glyph g, String color) {
        return "/assets/customblocks/arabic_art/" + color + "/" + g.fileBase() + "_" + color + ".png";
    }

    /** Unique block id, e.g. "arabic_alef_black", "arabic_a0_red", "arabic_e5_green". */
    public static String blockId(Glyph g, String color) {
        return "arabic_" + g.idBase() + "_" + color;
    }

    /** Title-Cased display name with color suffix, e.g. "Alef_Black", "A0_Black", "E5_Green". */
    public static String displayName(Glyph g, String color) {
        return NameCase.titleCase(g.idBase() + "_" + color);
    }

    /** Grouping category, used by the browser GUI to put each glyph under the right tab. */
    public static String category(Glyph g) {
        return switch (g.group()) {
            case LETTER  -> "Arabic Letters";
            case EASTERN -> "Arabic Numbers";
            case WESTERN -> "English Numbers";
        };
    }

    /** Look up a letter glyph by its art name (e.g. "alef", "ta_marbuta", "thal"). */
    public static Optional<Glyph> letterByName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return ALL.stream().filter(g -> g.group() == Group.LETTER && g.idBase().equals(n)).findFirst();
    }

    /** True if {@code color} is one of the four bundled colors. */
    public static boolean isColor(String color) {
        for (String c : COLORS) if (c.equalsIgnoreCase(color)) return true;
        return false;
    }

    /** Comma-separated letter names, for command error messages. */
    public static String letterNames() {
        StringBuilder b = new StringBuilder();
        for (Glyph g : ALL) {
            if (g.group() != Group.LETTER) continue;
            if (b.length() > 0) b.append(", ");
            b.append(g.idBase());
        }
        return b.toString();
    }
}
