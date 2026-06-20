/**
 * CategoryBlockMenu.java — per-block actions inside a category (Group 11, G11.2).
 *
 * The sub-menu opened by clicking a block in CategoryBrowserMenu. Three actions:
 *   Give   → /cb give <id>, stays open so several can be taken.
 *   Edit   → opens the block editor (Dest.EDITOR).
 *   Remove → /cb setcategory <id> none, then back to the browser (block now gone from it).
 *
 * The MenuKey arg packs "<id> <category>" — id is a Brigadier word (no spaces), so the
 * first token is the id and the remainder is the category we return to.
 *
 * Depends on: SlotManager, SlotBlock, Icons, GuiFx, GuiRouter, Nav
 * Called by:  GuiRouter (Dest.CATEGORY_BLOCK)
 */
package com.customblocks.gui.chest;

import com.customblocks.block.SlotBlock;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CategoryBlockMenu {

    private CategoryBlockMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, String arg) {
        String packed = arg == null ? "" : arg.trim();
        int sp = packed.indexOf(' ');
        String id  = sp < 0 ? packed : packed.substring(0, sp);
        String cat = sp < 0 ? "" : packed.substring(sp + 1).trim();

        ChestMenu m = new ChestMenu("Manage: " + id, 3).fill();

        SlotData d = SlotManager.getById(id);
        if (d == null) {
            m.set(13, Icons.of(Items.BARRIER, "§cBlock not found", "§7\"" + id + "\" no longer exists."));
            m.set(18, Icons.back(), (p, b, a) -> GuiRouter.back(p));
            m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
            return m;
        }

        SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
        Item display = item != null ? item : Items.STONE;
        m.set(4, Icons.glint(display, "§f" + d.displayName(),
                "§7id: §f" + d.customId(),
                "§7category: §f" + (d.category().isEmpty() ? "none" : d.category())));

        // Give — keep the menu open so the player can take several.
        m.set(11, Icons.of(Items.HOPPER, "§a§lGive", "§7Put one in your inventory"),
                (p, b, a) -> { GuiFx.apply(p); GuiRouter.runAndReopen(p, "give " + id,
                        MenuKey.of(Dest.CATEGORY_BLOCK, id + " " + cat)); });

        // Edit — open the block editor (Back returns here).
        m.set(13, Icons.of(Items.WRITABLE_BOOK, "§e§lEdit", "§7Open this block's editor"),
                (p, b, a) -> { GuiFx.open(p); GuiRouter.navigate(p, MenuKey.of(Dest.EDITOR, id)); });

        // Remove from category — clear it, then drop back into the (now-smaller) browser.
        m.set(15, Icons.of(Items.SHEARS, "§c§lRemove from category",
                        "§7Clears this block's category.",
                        "§8The block itself is kept."),
                (p, b, a) -> {
                    MinecraftServer s = p.getServer();
                    if (s == null) return;
                    s.execute(() -> {
                        GuiFx.danger(p);
                        s.getCommandManager().executeWithPrefix(p.getCommandSource(), "cb setcategory " + id + " none");
                        GuiRouter.back(p); // pops this sub-menu, rebuilds the browser fresh
                    });
                });

        m.set(18, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }
}
