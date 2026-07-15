/**
 * CbFmt.java — the ONE place a colour is chosen (G04-3).
 *
 * Before this class, ~890 string literals across 74 files hand-picked their own §-codes. The same
 * grey meant "a side detail" in one handler and "the whole message" in another; one item used gold,
 * a colour that appeared nowhere else. That is the drift G04-3 exists to kill.
 *
 * The rule: NOTHING outside this file writes a raw §-code. Callers name the MEANING (DIM, VALUE,
 * BAD…) and this file decides what colour that meaning is. Restyling the whole mod is now one edit
 * here. The `verifyChatColour` build gate enforces it — a raw § in a string literal fails the build.
 *
 * Exempt surfaces (they are not chat, and own their own palette):
 *   • client/ · gui/ · mixin/  — screen + chest-GUI render (Group 02 / Group 27; the CbActionBar
 *                                restyle is its own deferred pass, G04-1)
 *   • StarterBook              — written-book pages; black ink on paper is a physical constraint
 *
 * Used by: every command handler, the chat/hotbar helpers in {@link Chat}, and the item-lore builders.
 */
package com.customblocks.command;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class CbFmt {

    private CbFmt() {} // static-only

    // ── Status ───────────────────────────────────────────────────────────────
    /** Something worked. */
    public static final String OK     = "§a";
    /** Something failed. */
    public static final String BAD    = "§c";
    /** Proceed with care — not yet a failure. */
    public static final String WARN   = "§6";
    /** Destructive confirm (delete/reset). Deliberately the loudest colour in the palette. */
    public static final String DANGER = "§4";

    // ── Structure ────────────────────────────────────────────────────────────
    /** A section heading. Pair with {@link #BOLD}. */
    public static final String HEAD  = "§6";
    /**
     * Normal body text — and the colour of every block id / display name.
     * Block names stay pure white on purpose (Group 04 test C16): category colours tint the
     * CATEGORY label, never the block name, and aqua is reserved for clickables.
     */
    public static final String BODY  = "§f";
    /** A value being reported back: a setting, a count, a mode. */
    public static final String VALUE = "§e";
    /** A secondary sub-detail (file path, count) riding on a branded, full-colour line. */
    public static final String DIM   = "§7";
    /** Tertiary — a hint, a placeholder, a "(none)". Quieter than {@link #DIM}. */
    public static final String FAINT = "§8";
    /**
     * CLICKABLE ONLY. Aqua is the vanilla convention players already read as "you can click this",
     * so it is spent exclusively on chips — see {@link Chat#runButton}. Never use it for plain text.
     */
    public static final String CLICK = "§b";

    // ── Styles (not colours — safe to combine with any of the above) ──────────
    public static final String BOLD   = "§l";
    public static final String UNDER  = "§n";
    public static final String ITALIC = "§o";
    public static final String RESET  = "§r";
    /** Black. The [CB] brackets, and ink on a written-book page. */
    public static final String INK    = "§0";

    // ── Hotbar contract (G04-3 §6: green / red / white, and NOTHING else) ────
    // The hotbar had no colour rule at all, so every caller invented one (ColorToolService went
    // green/red with no glyph; RainbowRectangleItem went gold). These three are the whole palette.
    // Reach them through Chat.toolSuccess / Chat.toolError / Chat.tool — not by name.
    public static final String TOOL_OK   = OK;
    public static final String TOOL_BAD  = BAD;
    public static final String TOOL_INFO = BODY;
    /** F3: a neutral tool fact ("Nothing selected") reads gray, not white — quieter than a success. */
    public static final String TOOL_NEUTRAL = DIM;

    // ── Category colour registry ─────────────────────────────────────────────
    // The colours a PLAYER can pick for a category via `/cb category color <cat> <word>`. This is
    // user data, not message styling — but it is still a palette, so it lives here with the rest.
    // Moved out of CategoryService (G04-3) so no §-code is defined outside this file.
    private static final Map<String, String> CATEGORY = new LinkedHashMap<>();
    static {
        CATEGORY.put("none",      "");     CATEGORY.put("default",  "");
        CATEGORY.put("white",     BODY);   CATEGORY.put("green",    OK);
        CATEGORY.put("aqua",      CLICK);  CATEGORY.put("red",      BAD);
        CATEGORY.put("pink",      "§d");   CATEGORY.put("yellow",   VALUE);
        CATEGORY.put("gold",      WARN);   CATEGORY.put("blue",     "§9");
        CATEGORY.put("darkaqua",  "§3");   CATEGORY.put("purple",   "§5");
        CATEGORY.put("darkgreen", "§2");   CATEGORY.put("darkred",  DANGER);
        CATEGORY.put("gray",      FAINT);  CATEGORY.put("grey",     FAINT);
    }

    /** The colour words a player may type. Order is the order they are offered in. */
    public static Set<String> categoryColorWords() { return Collections.unmodifiableSet(CATEGORY.keySet()); }

    /** True if {@code word} is a colour a player may pick for a category. */
    public static boolean isCategoryColor(String word) { return CATEGORY.containsKey(word); }

    /** The §-code for a category colour word, or {@code null} if it isn't one. */
    public static String categoryColor(String word) { return CATEGORY.get(word); }
}
