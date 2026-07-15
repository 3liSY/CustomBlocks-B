/**
 * LoreFormat.java
 *
 * Responsibility: turn a user-typed string with '&' colour/format codes into a styled Text using
 * MODERN semantics — a colour code does NOT erase bold/italic/underline/strike/obfuscated (unlike
 * Minecraft's legacy '§' string parser, where a colour resets formatting). Only '&r' resets, back
 * to the caller's base style. Both '&' and the section sign '§' are accepted as code introducers;
 * an '&' not followed by a valid code is kept literally.
 *
 * Why: storing lines and feeding them through a naive '&'→'§' replace made bold depend on order
 * (e.g. "&l&aText" lost its bold when the colour reset it). Building Style objects per run instead
 * keeps each attribute independent, so '&l' works anywhere in the line. (Group 18 lore fix.)
 *
 * Depends on: (Minecraft text/Formatting only)
 * Called by:  Icons.ofCoded (menu line preview names), SlotBlock.SlotItem.appendTooltip (on-item lore).
 */
package com.customblocks.core;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class LoreFormat {

    private LoreFormat() {} // static-only

    /**
     * Parse '&'/'§' colour/format codes into a Text, starting from {@code base}. A colour code keeps
     * the active format flags; '&r' resets to {@code base}; an unrecognised code stays literal text.
     */
    public static MutableText parse(String s, Style base) {
        MutableText out = Text.empty();
        if (s == null || s.isEmpty()) return out;

        Style style = base;
        StringBuilder run = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < s.length()) {
                Formatting f = Formatting.byCode(Character.toLowerCase(s.charAt(i + 1)));
                if (f != null) {
                    if (run.length() > 0) {
                        out.append(Text.literal(run.toString()).setStyle(style));
                        run.setLength(0);
                    }
                    style = (f == Formatting.RESET) ? base : style.withFormatting(f);
                    i++; // consume the code character
                    continue;
                }
            }
            run.append(c);
        }
        if (run.length() > 0) out.append(Text.literal(run.toString()).setStyle(style));
        return out;
    }
}
