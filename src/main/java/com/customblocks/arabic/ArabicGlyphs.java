/**
 * ArabicGlyphs.java — Group 13 / Pass 4: glyph sources for the joinable letter block.
 *
 * Two jobs, both pure data (no Minecraft, no rendering):
 *   1. char ↔ bundled art-file base (e.g. 'ج' ↔ "jeem", 'ة' ↔ "ta_marbuta") — the 36 hand-art
 *      glyphs in ArabicArt, reconciling the names ArabicLetterMap uses (ha/ha2/tah/dhah/dhal/nun)
 *      to the art-file names (ha/ha2/ta2/tha2/thal/noon). Used to load the ISOLATED form's PNG.
 *   2. The ZWJ-wrapped text to feed ArabicWordRenderer so arabtype draws the CONNECTED contextual
 *      form (initial/medial/final) of a single letter. Isolated never uses this (it uses the PNG).
 *
 * Keeps ArabicJoining (the pure form-decision brain) untouched; this is the asset/shaping side.
 *
 * Depends on: ArabicJoining (form constants only)
 * Called by:  ArabicLetterBlockEntityRenderer (texture build), the give command (name → char)
 */
package com.customblocks.arabic;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ArabicGlyphs {

    private ArabicGlyphs() {}

    /** Zero-Width Joiner — forces arabtype to shape a single letter into a connected form. */
    private static final String ZWJ = "‍";

    /**
     * char → bundled art-file base, for the 36 hand-art glyphs (ArabicArt.LETTERS). Reconciled to
     * the art names exactly. Any char not here has no bundled isolated art (numbers, unknown).
     */
    private static String artBaseOf(char c) {
        return switch (c) {
            case 'ا' -> "alef";
            case 'آ' -> "alef_madda";
            case 'أ' -> "alef_hamza_above";
            case 'إ' -> "alef_hamza_below";
            case 'ى' -> "alef_maqsura";
            case 'ب' -> "ba";
            case 'ت' -> "ta";
            case 'ث' -> "tha";
            case 'ج' -> "jeem";
            case 'ح' -> "ha";        // ArabicLetterMap "ha"
            case 'خ' -> "kha";
            case 'د' -> "dal";
            case 'ذ' -> "thal";      // ArabicLetterMap "dhal" → art "thal"
            case 'ر' -> "ra";
            case 'ز' -> "zay";
            case 'س' -> "seen";
            case 'ش' -> "sheen";
            case 'ص' -> "sad";
            case 'ض' -> "dad";
            case 'ط' -> "ta2";       // ArabicLetterMap "tah" → art "ta2"
            case 'ظ' -> "tha2";      // ArabicLetterMap "dhah" → art "tha2"
            case 'ع' -> "ain";
            case 'غ' -> "ghain";
            case 'ف' -> "fa";
            case 'ق' -> "qaf";
            case 'ك' -> "kaf";
            case 'ل' -> "lam";
            case 'م' -> "meem";
            case 'ن' -> "noon";      // ArabicLetterMap "nun" → art "noon"
            case 'ه' -> "ha2";       // ArabicLetterMap "ha2"
            case 'و' -> "waw";
            case 'ؤ' -> "waw_hamza";
            case 'ة' -> "ta_marbuta";
            case 'ء' -> "hamza";
            case 'ئ' -> "ya_hamza";
            case 'ي' -> "ya";
            default  -> null;
        };
    }

    /** char → art-file base, or empty if the char has no bundled isolated art. */
    public static Optional<String> artBase(char c) {
        return Optional.ofNullable(artBaseOf(c));
    }

    /** Reverse map (art name → char), built once from the 36 entries — for the give command. */
    private static final Map<String, Character> BY_ART;
    static {
        Map<String, Character> m = new LinkedHashMap<>();
        // Every joinable letter has a unique art base; walk the BMP Arabic block once to fill it.
        for (char c = 'ء'; c <= 'ي'; c++) {
            String b = artBaseOf(c);
            if (b != null) m.putIfAbsent(b, c);
        }
        // also accept the extras outside that range (none currently — all are 0621..064A)
        BY_ART = Map.copyOf(m);
    }

    /** Look up a letter char by its art name (e.g. "jeem", "ta_marbuta", "thal"). */
    public static Optional<Character> charForName(String artName) {
        if (artName == null) return Optional.empty();
        return Optional.ofNullable(BY_ART.get(artName.toLowerCase(Locale.ROOT)));
    }

    /** True if {@code c} is one of the 36 letters that has bundled isolated art. */
    public static boolean hasArt(char c) {
        return artBaseOf(c) != null;
    }

    /**
     * The text to render through ArabicWordRenderer to get {@code c}'s CONNECTED form. Isolated
     * returns the bare char (callers should use the bundled PNG for isolated instead).
     */
    public static String shapedText(char c, int form) {
        return switch (form) {
            case ArabicJoining.INITIAL -> c + ZWJ;   // a letter follows on the left → initial
            case ArabicJoining.MEDIAL  -> ZWJ + c + ZWJ;
            case ArabicJoining.FINAL   -> ZWJ + c;   // a letter precedes on the right → final
            default                    -> String.valueOf(c);
        };
    }
}
