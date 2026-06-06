/**
 * ArabicLetterMap.java
 *
 * Responsibility: Complete map of the 28 base Arabic letters with their transliterated
 * names (used as block IDs), Arabic names, and Unicode code points.
 * Java's built-in AWT text renderer handles joining forms automatically when rendering.
 *
 * Depends on: (none)
 * Called by: ArabicBlockRegistry, ArabicCommands, ArabicWordRenderer
 */
package com.customblocks.arabic;

import java.util.*;

public final class ArabicLetterMap {

    private ArabicLetterMap() {}

    /** One Arabic letter entry. */
    public record Letter(String nameEn, String nameAr, char codePoint) {
        /** Block ID for this letter: "arabic_alef", etc. */
        public String blockId() { return "arabic_" + nameEn.toLowerCase(Locale.ROOT); }
        public String asString() { return String.valueOf(codePoint); }
    }

    /** All 28 base Arabic letters in traditional order. */
    public static final List<Letter> ALL = List.of(
        new Letter("alef",  "الف",  'ا'),
        new Letter("ba",    "باء",   'ب'),
        new Letter("ta",    "تاء",   'ت'),
        new Letter("tha",   "ثاء",   'ث'),
        new Letter("jeem",  "جيم",   'ج'),
        new Letter("ha",    "حاء",   'ح'),
        new Letter("kha",   "خاء",   'خ'),
        new Letter("dal",   "دال",   'د'),
        new Letter("dhal",  "ذال",   'ذ'),
        new Letter("ra",    "راء",   'ر'),
        new Letter("zay",   "زاي",   'ز'),
        new Letter("seen",  "سين",   'س'),
        new Letter("sheen", "شين",   'ش'),
        new Letter("sad",   "صاد",   'ص'),
        new Letter("dad",   "ضاد",   'ض'),
        new Letter("tah",   "طاء",   'ط'),
        new Letter("dhah",  "ظاء",   'ظ'),
        new Letter("ain",   "عين",   'ع'),
        new Letter("ghain", "غين",   'غ'),
        new Letter("fa",    "فاء",   'ف'),
        new Letter("qaf",   "قاف",   'ق'),
        new Letter("kaf",   "كاف",   'ك'),
        new Letter("lam",   "لام",   'ل'),
        new Letter("meem",  "ميم",   'م'),
        new Letter("nun",   "نون",   'ن'),
        new Letter("ha2",   "هاء",   'ه'),
        new Letter("waw",   "واو",   'و'),
        new Letter("ya",    "ياء",   'ي')
    );

    private static final Map<String, Letter> BY_NAME;
    static {
        Map<String, Letter> m = new LinkedHashMap<>();
        ALL.forEach(l -> m.put(l.nameEn().toLowerCase(Locale.ROOT), l));
        BY_NAME = Collections.unmodifiableMap(m);
    }

    public static Optional<Letter> byName(String name) {
        return Optional.ofNullable(BY_NAME.get(name.toLowerCase(Locale.ROOT)));
    }

    public static Optional<Letter> byCodePoint(char cp) {
        return ALL.stream().filter(l -> l.codePoint() == cp).findFirst();
    }

    /** Comma-separated list of all letter names for error messages. */
    public static String nameList() {
        return ALL.stream().map(Letter::nameEn).reduce((a, b) -> a + ", " + b).orElse("");
    }
}
