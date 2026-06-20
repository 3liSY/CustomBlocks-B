/**
 * ArabicNaming.java — Group 13 / O6 (virtual ids + display names). COMMON (server + client).
 *
 * Deterministically turns one join-letter variant (letter, colour, contextual form) into:
 *   - a clean DISPLAY name  → "Jeem Black", "Jeem Black Mid"   (spaces, Title-Case)
 *   - a stable VIRTUAL id   → "Jeem_Black", "Jeem_Black_Mid"   (underscores)
 *
 * Scheme (ADR-006, dev-locked): order = Letter _ Colour _ Form. The letter words come from the
 * bundled art base (jeem, ta2, ta_marbuta…) Title-Cased per token, keeping digits (Ta2, Ha2).
 * ISOLATED carries NO form word, so a lone join block reads identically to a bundled "Jeem Black"
 * (the 224 confirmed bundled names never change). The three connected-form words are the LIVE
 * {@link ArabicLabels} values (default Ini / Mid / Fin), so a label change re-labels everything at
 * display time with no bake and no reload.
 *
 * No NBT, no Minecraft text here — pure strings, so the give command, the item name, the HUD and
 * the preview harness can all share one source of truth.
 *
 * Depends on: ArabicGlyphs (art base), ArabicJoining (form constants), ArabicLabels (live words)
 * Called by:  ArabicLetterItem.getName, HudRenderer.buildContext, ArabicCommands (chat feedback)
 */
package com.customblocks.arabic;

import java.util.Locale;

public final class ArabicNaming {

    private ArabicNaming() {} // static-only

    /** Clean display name, e.g. "Jeem Black" (isolated) or "Jeem Black Mid" (medial). */
    public static String displayName(char letter, String color, int form) {
        String suffix = formWord(form);
        String name = letterWords(letter, " ") + " " + titleCase(color);
        return suffix.isEmpty() ? name : name + " " + suffix;
    }

    /** Stable virtual id, e.g. "Jeem_Black" or "Jeem_Black_Mid". Tracks the live form labels. */
    public static String virtualId(char letter, String color, int form) {
        String suffix = formWord(form);
        String id = letterWords(letter, "_") + "_" + titleCase(color);
        return suffix.isEmpty() ? id : id + "_" + suffix;
    }

    /** The live form word for a contextual form, or "" for isolated (no suffix). */
    private static String formWord(int form) {
        return switch (form) {
            case ArabicJoining.INITIAL -> ArabicLabels.ini();
            case ArabicJoining.MEDIAL  -> ArabicLabels.mid();
            case ArabicJoining.FINAL   -> ArabicLabels.fin();
            default                    -> ""; // ISOLATED
        };
    }

    /** Art base ("ta_marbuta") → Title-Cased words joined by {@code sep} ("Ta Marbuta" / "Ta_Marbuta"). */
    private static String letterWords(char letter, String sep) {
        String base = ArabicGlyphs.artBase(letter).orElse(String.valueOf(letter));
        String[] toks = base.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < toks.length; i++) {
            if (i > 0) sb.append(sep);
            sb.append(titleCase(toks[i]));
        }
        return sb.toString();
    }

    /** Upper-case the first char only, keep the rest (so digits like "ta2" → "Ta2" survive). */
    private static String titleCase(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }
}
