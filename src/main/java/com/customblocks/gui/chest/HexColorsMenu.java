/**
 * HexColorsMenu.java
 *
 * Responsibility: The "Variant Colours" sub-GUI of the Config dashboard (Group 06 / M3 hex).
 * The ONLY place the four tool hexes are edited by GUI: each magic-tool colour is shown as
 * its dye (Red / Yellow / Green / Black); clicking a dye opens an anvil prompt where the
 * new "#RRGGBB" is typed. A valid new value runs the tested `/cb config hex` command, so
 * saving, item-art re-tinting and the "recolour existing blocks?" confirm all behave
 * exactly like the chat command — no mutation logic is duplicated here.
 *
 * Depends on: ChestMenu, Icons, GuiRouter/Nav, AnvilPrompt, ColorVariantService,
 *             CustomBlocksConfig (read + hex validation), Chat
 * Called by:  GuiRouter.build (Dest.HEX_COLORS); opened from ConfigMenu.
 */
package com.customblocks.gui.chest;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.ColorVariantService;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class HexColorsMenu {

    private HexColorsMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player) {
        ChestMenu m = new ChestMenu("Variant Colours", 4).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 27; i < 36; i++) m.set(i, Icons.accent());

        m.set(4, Icons.glint(Items.BRUSH, "§6§lVariant Colours",
                "§7The fill colour each magic tool paints with.",
                "§7Changing one re-tints that tool's item art and",
                "§7offers to repaint blocks that already use it.",
                "§aClick a dye §7→ type the new hex in an anvil."));

        colourEntry(m, 10, "red",    Items.RED_DYE,    "§c", CustomBlocksConfig.TRIANGLE_RED_DEFAULT);
        colourEntry(m, 12, "yellow", Items.YELLOW_DYE, "§e", CustomBlocksConfig.TRIANGLE_YELLOW_DEFAULT);
        colourEntry(m, 14, "green",  Items.GREEN_DYE,  "§a", CustomBlocksConfig.TRIANGLE_GREEN_DEFAULT);
        colourEntry(m, 16, "black",  Items.BLACK_DYE,  "§8", CustomBlocksConfig.TRIANGLE_BLACK_DEFAULT);

        m.set(27, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(35, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    /** One editable colour: its dye, the live hex, and the click → anvil-prompt flow. */
    private static void colourEntry(ChestMenu m, int slot, String key, Item dye,
                                    String code, String defaultHex) {
        String hex = ColorVariantService.hexFor(key);
        String name = ColorVariantService.capitalize(key);
        int count = ColorVariantService.variantCount(key);
        m.set(slot, Icons.glint(dye, code + "§l" + name + " Tools §f= §e" + hex,
                "§7Used by the " + name + " Square + Triangle.",
                "§7Default: §f" + defaultHex + (hex.equals(defaultHex) ? " §8(current)" : ""),
                "§7Existing \"_" + key + "\" blocks: §f" + count,
                "§aClick §7→ edit in an anvil (§f#RRGGBB§7)"),
                (p, b, a) -> openEditor(p, key, dye, name));
    }

    /** Anvil prompt for one colour; valid input runs the tested /cb config hex command. */
    private static void openEditor(ServerPlayerEntity player, String key, Item dye, String name) {
        AnvilPrompt.open(player, "Set " + name + " hex (#RRGGBB)", new ItemStack(dye),
                ColorVariantService.hexFor(key),
                text -> applyHex(player, key, text),
                () -> GuiRouter.render(player, Nav.MenuKey.of(Nav.Dest.HEX_COLORS)));
    }

    private static void applyHex(ServerPlayerEntity player, String key, String text) {
        String norm = CustomBlocksConfig.normalizeHexColor(text, null);
        if (norm == null) {
            Chat.error(player.getCommandSource(),
                    "That's not a colour code — use #RRGGBB (e.g. #FF8800).");
            GuiRouter.render(player, Nav.MenuKey.of(Nav.Dest.HEX_COLORS));
            return;
        }
        if (norm.equals(ColorVariantService.hexFor(key))) {
            Chat.info(player.getCommandSource(),
                    ColorVariantService.capitalize(key) + " is already " + norm + ".");
            GuiRouter.render(player, Nav.MenuKey.of(Nav.Dest.HEX_COLORS));
            return;
        }
        // Counted BEFORE the command: when variants exist, setHex opens the recolour confirm
        // GUI itself and this menu must stay out of its way.
        int variants = ColorVariantService.variantCount(key);
        MinecraftServer s = player.getServer();
        if (s == null) return;
        // Strip the '#' from the dispatched command (belt-and-braces with the greedyString
        // arg change in HexCommands): keeps the chat-parsed command '#'-free. BUG 1.
        s.getCommandManager().executeWithPrefix(player.getCommandSource(),
                "cb config hex " + key + " " + norm.replace("#", ""));
        if (variants == 0) GuiRouter.render(player, Nav.MenuKey.of(Nav.Dest.HEX_COLORS));
    }
}
