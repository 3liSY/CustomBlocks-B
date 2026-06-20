/**
 * ArabicJoining.java — Group 13 / Pass 4 step 4a: the JOINING BRAIN (pure logic, no Minecraft).
 *
 * Given a letter and the two letters touching it along the word axis, decides which contextual
 * FORM it should draw: 0 isolated, 1 initial, 2 medial, 3 final (the FORM index locked in ADR-005).
 * No rendering, no world access — just the real-Arabic joining table. The letter BlockEntity (4b)
 * stores the result; the re-flow updater (4c) feeds it the neighbours.
 *
 * Reading model (ADR-003 rules, still in force): a word reads RIGHT-TO-LEFT. For one block the
 * neighbour toward the word START is on its RIGHT (the earlier letter); the neighbour toward the
 * word END is on its LEFT (the later letter). A char value of 0 means "no joining neighbour there"
 * (empty, a gap, a number, or any non-letter block — all end the word).
 *
 * Joining types (developer-locked spec, GROUP_13_ARABIC O3 / handoff 2026-06-18):
 *   - DUAL  : joins on BOTH sides (most letters).
 *   - RIGHT : the 6 non-connectors (ا د ذ ر ز و) + cousins (آ أ إ ؤ ة ى ء) — join on their RIGHT
 *             only (to a preceding letter); never initial/medial. The block placed to their LEFT
 *             therefore starts a fresh word.
 *   - NONE  : numbers and everything else — never join.
 * Two adjacent letters connect iff the right one can join on its LEFT (DUAL) and the left one can
 * join on its RIGHT (DUAL or RIGHT). The لا lam-alef ligature is out of scope.
 *
 * Depends on: (none — pure Java, so the preview harness can run it without the mod)
 * Called by:  the letter BlockEntity / re-flow updater (Pass 4 steps 4b–4c)
 */
package com.customblocks.arabic;

public final class ArabicJoining {

    private ArabicJoining() {}

    /** Contextual form indices — MUST match the ADR-005 FORM order the renderer uses. */
    public static final int ISOLATED = 0;
    public static final int INITIAL  = 1;
    public static final int MEDIAL   = 2;
    public static final int FINAL    = 3;

    /** Internal joining classes for one character. */
    private static final int NONE = 0; // never joins (numbers, gaps, unknown)
    private static final int DUAL = 1; // joins on both sides
    private static final int RIGHT = 2; // joins on its right side only (non-connectors)

    /**
     * Joining class of one character. Arabic chars are literal (UTF-8, no BOM — same as
     * ArabicLetterMap), each named in the comment beside it.
     */
    private static int joinClass(char c) {
        return switch (c) {
            // --- Non-connectors: join on their RIGHT only (final/isolated) ---
            case 'ا', // ا alef
                 'آ', // آ alef madda
                 'أ', // أ alef hamza above
                 'إ', // إ alef hamza below
                 'د', // د dal
                 'ذ', // ذ dhal / thal
                 'ر', // ر ra
                 'ز', // ز zay
                 'و', // و waw
                 'ؤ', // ؤ waw hamza
                 'ة', // ة ta marbuta
                 'ى', // ى alef maqsura
                 'ء'  // ء hamza
                 -> RIGHT;

            // --- Dual-joining letters: join on both sides ---
            case 'ب', // ب ba
                 'ت', // ت ta
                 'ث', // ث tha
                 'ج', // ج jeem
                 'ح', // ح ha
                 'خ', // خ kha
                 'س', // س seen
                 'ش', // ش sheen
                 'ص', // ص sad
                 'ض', // ض dad
                 'ط', // ط tah
                 'ظ', // ظ dhah
                 'ع', // ع ain
                 'غ', // غ ghain
                 'ف', // ف fa
                 'ق', // ق qaf
                 'ك', // ك kaf
                 'ل', // ل lam
                 'م', // م meem
                 'ن', // ن nun
                 'ه', // ه ha2
                 'ي', // ي ya
                 'ئ'  // ئ ya hamza
                 -> DUAL;

            default -> NONE;
        };
    }

    /** True if {@code c} is an Arabic letter that participates in joining at all (DUAL or RIGHT). */
    public static boolean isJoiningLetter(char c) {
        return joinClass(c) != NONE;
    }

    /**
     * Decide the contextual form for {@code self}, given its in-axis neighbours.
     *
     * @param self  this block's letter
     * @param right the letter touching this block on its RIGHT (toward the word start), or 0 for none
     * @param left  the letter touching this block on its LEFT (toward the word end), or 0 for none
     * @return ISOLATED / INITIAL / MEDIAL / FINAL
     */
    public static int form(char self, char right, char left) {
        int t = joinClass(self);
        if (t == NONE) return ISOLATED; // not a joining letter — always standalone

        // Connect to the previous letter (on the right): the right neighbour must be able to join
        // on ITS left, i.e. be DUAL. self always joins on its right when it joins at all.
        boolean connectsRight = joinClass(right) == DUAL;

        // Connect to the next letter (on the left): self must join on its left (DUAL), and the left
        // neighbour must join at all (DUAL or RIGHT).
        boolean connectsLeft = (t == DUAL) && (joinClass(left) != NONE);

        if (connectsRight && connectsLeft) return MEDIAL;
        if (connectsRight) return FINAL;
        if (connectsLeft) return INITIAL;
        return ISOLATED;
    }

    /** Human-readable form name (for previews / logs / tests). */
    public static String formName(int form) {
        return switch (form) {
            case INITIAL -> "initial";
            case MEDIAL  -> "medial";
            case FINAL   -> "final";
            default      -> "isolated";
        };
    }
}
