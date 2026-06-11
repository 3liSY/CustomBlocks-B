/**
 * CustomColorMenu.java — the /cb customcolor "Color Studio" (Group 06), rebuilt from
 * scratch on the old project's studio idea: a grid of 29 ready-made colours (shown as the
 * closest dye), where clicking one hands over that colour's magic Square + Triangle pair,
 * plus an anvil prompt for any custom "#RRGGBB". The pair's colour lives in the items'
 * NBT, so everything given here keeps working after a restart.
 *
 * Depends on: ChestMenu, Icons, GuiRouter/Nav, AnvilPrompt, ColorLibrary,
 *             CustomColorToolItem (givePair), Chat
 * Called by:  GuiRouter.build (Dest.CUSTOM_COLOR); opened by /cb customcolor.
 */
package com.customblocks.gui.chest;

import com.customblocks.command.Chat;
import com.customblocks.core.ColorLibrary;
import com.customblocks.core.ColorLibrary.LibColor;
import com.customblocks.item.CustomColorToolItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Locale;

public final class CustomColorMenu {

    private CustomColorMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player) {
        ChestMenu m = new ChestMenu("Color Studio", 6).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 45; i < 54; i++) m.set(i, Icons.accent());

        m.set(4, Icons.glint(Items.MAGENTA_DYE, "§d§lColor Studio",
                "§7Magic tools in any colour you like.",
                "§aClick a colour §7→ get its Square + Triangle pair.",
                "§8" + ColorLibrary.ALL.size() + " ready-made colours · anvil below for a custom hex."));

        // The colour grid — rows 2-4 plus the first two slots of row 5 (29 colours).
        for (int i = 0; i < ColorLibrary.ALL.size() && 9 + i <= 37; i++) {
            LibColor c = ColorLibrary.ALL.get(i);
            int rgb = c.rgb();
            m.set(9 + i, Icons.of(dyeFor(c), "§f§l" + c.name() + " §7" + c.hex(),
                    "§8RGB: §7" + ((rgb >> 16) & 0xFF) + ", " + ((rgb >> 8) & 0xFF) + ", " + (rgb & 0xFF),
                    "§aClick §7→ get the " + c.name() + " Square + Triangle"),
                    (p, b, a) -> givePair(p, c.hex()));
        }

        m.set(40, Icons.glint(Items.ANVIL, "§e§lCustom Hex…",
                "§7Any colour the presets don't cover.",
                "§aClick §7→ type §f#RRGGBB §7(or a colour name)",
                "§7in an anvil and take the result."),
                (p, b, a) -> AnvilPrompt.open(p, "Custom colour (#RRGGBB)",
                        new ItemStack(Items.MAGENTA_DYE), "#",
                        text -> applyCustom(p, text),
                        () -> GuiRouter.render(p, Nav.MenuKey.of(Nav.Dest.CUSTOM_COLOR))));

        m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    /** Hand over the pair for a "#RRGGBB" and report it; the studio stays open. */
    private static void givePair(ServerPlayerEntity p, String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        CustomColorToolItem.givePair(p, rgb);
        String name = ColorLibrary.nameForHex(hex);
        Chat.success(p.getCommandSource(), "Gave you the " + (name != null ? name : hex)
                + " Square + Triangle §7(" + hex + ")§a.");
    }

    /** Anvil-prompt submit: resolve a typed hex/name, give the pair, return to the studio. */
    private static void applyCustom(ServerPlayerEntity p, String text) {
        String hex = ColorLibrary.resolve(text);
        if (hex == null) {
            Chat.error(p.getCommandSource(), "\"" + text + "\" isn't a colour — use #RRGGBB "
                    + "(e.g. #FF8800) or a colour name like purple.");
        } else {
            givePair(p, hex);
        }
        GuiRouter.render(p, Nav.MenuKey.of(Nav.Dest.CUSTOM_COLOR));
    }

    /** The dye that best shows a library colour (recycled from the old studio's mapping). */
    private static Item dyeFor(LibColor c) {
        return switch (c.name().toLowerCase(Locale.ROOT)) {
            case "red", "crimson", "maroon" -> Items.RED_DYE;
            case "orange", "coral", "peach" -> Items.ORANGE_DYE;
            case "yellow", "gold", "butter" -> Items.YELLOW_DYE;
            case "lime", "mint" -> Items.LIME_DYE;
            case "green", "forest" -> Items.GREEN_DYE;
            case "cyan" -> Items.CYAN_DYE;
            case "blue", "navy", "baby blue", "indigo" -> Items.BLUE_DYE;
            case "purple", "lavender" -> Items.PURPLE_DYE;
            case "magenta" -> Items.MAGENTA_DYE;
            case "pink", "rose" -> Items.PINK_DYE;
            case "white" -> Items.WHITE_DYE;
            case "light gray" -> Items.LIGHT_GRAY_DYE;
            case "gray", "dark gray" -> Items.GRAY_DYE;
            case "black" -> Items.BLACK_DYE;
            case "brown" -> Items.BROWN_DYE;
            default -> Items.PAPER;
        };
    }
}
