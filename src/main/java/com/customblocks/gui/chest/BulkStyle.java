/**
 * BulkStyle.java — shared per-operation colours and icons for the two-step bulk GUIs.
 *
 * The redesigned Step 1 (BulkSelectMenu) and Step 2 (BulkActionMenu) colour-code their frame,
 * header and icon by the current operation so every op reads consistently (blue = edit, red =
 * delete, yellow = rename, cyan = category, white = duplicate, gold = export, green = lock,
 * magenta = favorite — see docs/GUI_DESIGN_GUIDE.md §1). Pulled out so both menus share one palette.
 *
 * Pure look-up — no state. Op strings match BulkSession.
 */
package com.customblocks.gui.chest;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

public final class BulkStyle {

    private BulkStyle() {} // static-only

    /** Frame edge colour per op. */
    public static Item framePane(String op) {
        return switch (op) {
            case "delete"    -> Items.RED_STAINED_GLASS_PANE;
            case "rename"    -> Items.YELLOW_STAINED_GLASS_PANE;
            case "category"  -> Items.CYAN_STAINED_GLASS_PANE;
            case "duplicate" -> Items.WHITE_STAINED_GLASS_PANE;
            case "export"    -> Items.BROWN_STAINED_GLASS_PANE;
            case "lock"      -> Items.LIME_STAINED_GLASS_PANE;
            case "favorite"  -> Items.MAGENTA_STAINED_GLASS_PANE;
            default          -> Items.LIGHT_BLUE_STAINED_GLASS_PANE;
        };
    }

    /** Frame corner — a darker tone of the same hue, so the frame reads as a picture frame. */
    public static Item frameCorner(String op) {
        return switch (op) {
            case "delete"    -> Items.BLACK_STAINED_GLASS_PANE;
            case "rename"    -> Items.ORANGE_STAINED_GLASS_PANE;
            case "category"  -> Items.BLUE_STAINED_GLASS_PANE;
            case "duplicate" -> Items.LIGHT_GRAY_STAINED_GLASS_PANE;
            case "export"    -> Items.BLACK_STAINED_GLASS_PANE;
            case "lock"      -> Items.GREEN_STAINED_GLASS_PANE;
            case "favorite"  -> Items.PURPLE_STAINED_GLASS_PANE;
            default          -> Items.BLUE_STAINED_GLASS_PANE;
        };
    }

    /** Header text colour matching the frame. */
    public static String headerColor(String op) {
        return switch (op) {
            case "delete"    -> "§c";
            case "rename"    -> "§e";
            case "category"  -> "§3";
            case "duplicate" -> "§f";
            case "export"    -> "§6";
            case "lock"      -> "§a";
            case "favorite"  -> "§d";
            default          -> "§b";
        };
    }

    /** Darker title colour (the title bar sits on light parchment — dark shades read best). */
    public static String titleColor(String op) {
        return switch (op) {
            case "delete"    -> "§4";
            case "rename"    -> "§6";
            case "category"  -> "§3";
            case "duplicate" -> "§8";
            case "export"    -> "§6";
            case "lock"      -> "§2";
            case "favorite"  -> "§5";
            default          -> "§3";
        };
    }

    /** A fitting icon for an operation. */
    public static Item opIcon(String op) {
        return switch (op) {
            case "delete"    -> Items.TNT;
            case "rename"    -> Items.NAME_TAG;
            case "category"  -> Items.CHEST;
            case "duplicate" -> Items.BOOK;
            case "export"    -> Items.MAP;
            case "lock"      -> Items.TRIPWIRE_HOOK;
            case "favorite"  -> Items.NETHER_STAR;
            default          -> Items.COMPARATOR;
        };
    }
}
