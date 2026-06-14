/**
 * Swatch.java — Group 10 (Coloring hub) shared colour-swatch helpers.
 *
 * Responsibility: the one place that maps a "#RRGGBB" colour to the nearest of the 16 dye-wools
 * for a visual chest-GUI swatch, plus the small hex<->int helpers the colour menus all need.
 * Extracted so BgStudioMenu / PaletteMenu / GradientPickerMenu stop carrying three identical
 * copies of the wool table (Royal Directive: reuse, don't duplicate).
 *
 * Depends on: (none — pure data + Minecraft Items).
 * Called by:  gui/chest/{BgStudioMenu, PaletteMenu, GradientPickerMenu, ColorsMenu, CustomColorMenu}.
 */
package com.customblocks.gui.chest;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.Locale;

public final class Swatch {

    private Swatch() {} // static-only

    /** The 16 dye-wools, paired below with their approximate map-colour RGB (vanilla wool tints). */
    private static final Item[] WOOLS = {
            Items.WHITE_WOOL, Items.ORANGE_WOOL, Items.MAGENTA_WOOL, Items.LIGHT_BLUE_WOOL,
            Items.YELLOW_WOOL, Items.LIME_WOOL, Items.PINK_WOOL, Items.GRAY_WOOL,
            Items.LIGHT_GRAY_WOOL, Items.CYAN_WOOL, Items.PURPLE_WOOL, Items.BLUE_WOOL,
            Items.BROWN_WOOL, Items.GREEN_WOOL, Items.RED_WOOL, Items.BLACK_WOOL};

    private static final int[] WOOL_RGB = {
            0xEAECEC, 0xF07613, 0xBD44B3, 0x3AAFD9,
            0xF8C627, 0x70B919, 0xED8DAC, 0x3E4447,
            0x8E8E86, 0x158991, 0x792AAC, 0x35399D,
            0x724728, 0x546D1B, 0xA12722, 0x141519};

    /** Nearest dye-wool to an 0xRRGGBB colour (squared-distance in RGB). */
    public static Item woolFor(int rgb) {
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        Item best = Items.WHITE_WOOL;
        long bestD = Long.MAX_VALUE;
        for (int i = 0; i < WOOL_RGB.length; i++) {
            int wr = WOOL_RGB[i] >> 16 & 0xFF, wg = WOOL_RGB[i] >> 8 & 0xFF, wb = WOOL_RGB[i] & 0xFF;
            long d = (long) (r - wr) * (r - wr) + (long) (g - wg) * (g - wg) + (long) (b - wb) * (b - wb);
            if (d < bestD) { bestD = d; best = WOOLS[i]; }
        }
        return best;
    }

    /** Nearest dye-wool to a "#RRGGBB" string (falls back to white wool on bad input). */
    public static Item woolFor(String hex) {
        return woolFor(parseHex(hex, 0xFFFFFF));
    }

    /** Parse "#RRGGBB" (or "RRGGBB") to an 0xRRGGBB int, or {@code fallback} on bad input. */
    public static int parseHex(String hex, int fallback) {
        if (hex == null) return fallback;
        try { return Integer.parseInt(hex.replace("#", "").trim(), 16) & 0xFFFFFF; }
        catch (Exception e) { return fallback; }
    }

    /** Format an 0xRRGGBB int as canonical "#RRGGBB" (upper-case). */
    public static String hex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }
}
