/**
 * NameCase.java
 *
 * Responsibility: Canonical display-name casing for every created block. The first letter of
 * every word is capitalized (matching the old mod), where a "word" is delimited by spaces or
 * underscores. Underscores are rendered as spaces in the output so names read cleanly
 * ("Test_black" -> "Test Black"); the rest of each word is left exactly as typed, so intentional
 * caps/digits survive (e.g. "E5" stays "E5", "ta2" -> "Ta2").
 *
 * Applied centrally in SlotManager.create/rename so the rule holds everywhere a block is named.
 *
 * Depends on: (none)
 * Called by:  SlotManager, ArabicArt
 */
package com.customblocks.core;

public final class NameCase {

    private NameCase() {}

    /** Capitalize the first letter of every space/underscore-delimited word; emit a space for each
     *  underscore so the result reads cleanly; leave the rest of each word as-is. */
    public static String titleCase(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder b = new StringBuilder(s.length());
        boolean boundary = true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (boundary && Character.isLetter(c)) c = Character.toUpperCase(c);
            boundary = (c == ' ' || c == '_'); // detect on the original separator, before remapping
            b.append(c == '_' ? ' ' : c);      // underscores become spaces in the output
        }
        return b.toString();
    }
}
