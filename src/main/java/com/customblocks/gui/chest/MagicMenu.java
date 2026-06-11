/**
 * MagicMenu.java — magic-items menu (Group 02; expanded to a double chest in the finale fix).
 * In view mode, clicking an enabled item gives the player that tool. In edit mode, clicking
 * toggles whether the item is enabled (persisted via MagicItemsManager). The button at the
 * bottom switches between modes. The title stays "Magic Items" in both modes so toggling
 * refreshes in place (no cursor reset).
 *
 * Layout (6 rows): a light-blue frame around the edge; the three hand tools (Omni-Tool,
 * Rainbow Rectangle, Deleter) on the top interior row; the eight colour shapes below in a
 * four-colour grid — each colour's Square sits directly above its Triangle. Items resolve by
 * id straight from the item registry, so adding a new seed item only needs a slot mapping.
 */
package com.customblocks.gui.chest;

import com.customblocks.CustomBlocksMod;
import com.customblocks.command.Chat;
import com.customblocks.core.MagicItemsManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class MagicMenu {

    private MagicMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, boolean edit, int page) {
        ChestMenu m = new ChestMenu("Magic Items", 6).fill();
        decorateFrame(m, edit);

        for (MagicItemsManager.MagicItem it : MagicItemsManager.all()) {
            int slot = slotFor(it.id());
            if (slot < 0) continue; // seeded item with no layout position — skip rather than guess
            Item display = toolFor(it.id());
            boolean on = it.enabled();
            String status = on ? "§aenabled" : "§cdisabled";

            // Enabled items get a subtle glint; disabled ones are dimmed.
            ItemStack icon = on
                    ? Icons.glint(display, "§f" + it.name(), "§7Status: " + status,
                            edit ? "§eClick to turn off" : "§eClick to receive this item")
                    : Icons.of(display, "§8" + it.name(), "§7Status: " + status,
                            edit ? "§eClick to turn on" : "§8Disabled by an admin");

            if (edit) {
                m.set(slot, icon, (pl, b, a) -> {
                    MagicItemsManager.setEnabled(it.id(), !it.enabled());
                    GuiRouter.repage(pl, MenuKey.of(Dest.MAGIC_EDIT));
                });
            } else {
                m.set(slot, icon, (pl, b, a) -> {
                    if (it.enabled()) {
                        pl.getInventory().insertStack(new ItemStack(toolFor(it.id())));
                        Chat.tool(pl, "§fGave §e" + it.name());
                    }
                });
            }
        }
        return m;
    }

    /** Frame, header, section label and the bottom-row controls. */
    private static void decorateFrame(ChestMenu m, boolean edit) {
        // Light-blue accent border: top + bottom rows, plus the left/right columns between.
        for (int c = 0; c < 9; c++) {
            m.set(c, Icons.accent());
            m.set(45 + c, Icons.accent());
        }
        for (int r = 1; r < 5; r++) {
            m.set(r * 9, Icons.accent());
            m.set(r * 9 + 8, Icons.accent());
        }

        // Header + the squares/triangles section label.
        m.set(4, Icons.of(Items.NETHER_STAR, "§d§lMagic Items",
                edit ? "§7Edit mode — click an item to toggle it." : "§7Click an item to receive it."));
        m.set(22, Icons.of(Items.ITEM_FRAME, "§bSquares §f& §bTriangles",
                "§7Triangles create colour variants — Squares swap them."));

        // Bottom-row controls (overlay the accent border).
        m.set(45, Icons.back(), (pl, b, a) -> GuiRouter.back(pl));
        m.set(49, Icons.of(Items.COMPARATOR, edit ? "§eExit edit mode" : "§eEdit mode",
                        "§7Toggle configuration mode",
                        edit ? "§8Currently: editing" : "§8Currently: viewing"),
                (pl, b, a) -> GuiRouter.repage(pl, MenuKey.of(edit ? Dest.MAGIC : Dest.MAGIC_EDIT)));
        m.set(53, Icons.close(), (pl, b, a) -> pl.closeHandledScreen());
    }

    /** Fixed slot for each seeded item: tools across the top, shapes in a 4-colour grid below. */
    private static int slotFor(String id) {
        return switch (id) {
            case "omni_tool" -> 11;
            case "rainbow_rectangle" -> 13;
            case "deleter" -> 15;
            // Squares (row 3) sit directly above their Triangles (row 4): green, yellow, red, black.
            case "green_square" -> 28;
            case "yellow_square" -> 30;
            case "red_square" -> 32;
            case "black_square" -> 34;
            case "green_triangle" -> 37;
            case "yellow_triangle" -> 39;
            case "red_triangle" -> 41;
            case "black_triangle" -> 43;
            default -> -1;
        };
    }

    /** Resolve a seed id to its registered item; PAPER is a visible fallback if absent. */
    private static Item toolFor(String id) {
        Item item = Registries.ITEM.get(Identifier.of(CustomBlocksMod.MOD_ID, id));
        return item == Items.AIR ? Items.PAPER : item;
    }
}
