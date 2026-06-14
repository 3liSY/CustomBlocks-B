/**
 * Icons.java
 *
 * Shared item-stack builders for the server-side chest GUIs (Group 02). Every menu item
 * is a decorated stack: a custom name (italics off) plus optional gray lore lines. Uses
 * only data components present in 1.21.1, so the menus render on vanilla clients with no
 * client-side mod required.
 */
package com.customblocks.gui.chest;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class Icons {

    private Icons() {} // static-only

    /** Build a named item with optional gray lore lines. Italics are disabled for a clean look. */
    public static ItemStack of(Item item, String name, String... lore) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).styled(st -> st.withItalic(false)));
        if (lore != null && lore.length > 0) {
            List<Text> lines = new ArrayList<>();
            for (String l : lore) lines.add(Text.literal(l).styled(st -> st.withItalic(false)));
            s.set(DataComponentTypes.LORE, new LoreComponent(lines));
        }
        return s;
    }

    /** Like {@link #of} but adds an enchantment glint to mark a current/selected value. */
    public static ItemStack glint(Item item, String name, String... lore) {
        ItemStack s = of(item, name, lore);
        s.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return s;
    }

    /** A blank gray glass pane used to fill empty/decorative slots. */
    public static ItemStack filler() {
        return of(Items.GRAY_STAINED_GLASS_PANE, " ");
    }

    /** A coloured glass pane used to frame/border a menu for a polished look. */
    public static ItemStack accent() {
        return of(Items.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
    }

    /** Standard back-arrow button. */
    public static ItemStack back() {
        return of(Items.ARROW, "§eBack", "§8Previous menu — or home if you started here");
    }

    /** Standard close button. */
    public static ItemStack close() {
        return of(Items.BARRIER, "§cClose");
    }
}
