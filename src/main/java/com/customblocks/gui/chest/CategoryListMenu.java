/**
 * CategoryListMenu.java — the category overview (Group 11, G11.3), opened by /cb categories.
 *
 * A paginated chest GUI listing every category currently in use, each tile showing the
 * category's icon (its display block if set, else a bookshelf), name and block count.
 * Left-clicking a category opens CategoryBrowserMenu for it. Right-clicking opens
 * CategoryEditMenu. The category name is tinted by its colour tag if set.
 *
 * Depends on: SlotManager, CategoryDisplayBlockManager, CategoryMetadataStore,
 *             SlotBlock, Layout, Icons, GuiRouter, Nav
 * Called by:  GuiRouter (Dest.CATEGORY_LIST), MainMenu (Categories tile),
 *             UtilityCommands (/cb categories)
 */
package com.customblocks.gui.chest;

import com.customblocks.block.SlotBlock;
import com.customblocks.core.CategoryDisplayBlockManager;
import com.customblocks.core.CategoryMetadataStore;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public final class CategoryListMenu {

    private CategoryListMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, int page) {
        List<String> cats = new ArrayList<>(SlotManager.categories()); // already sorted (TreeSet)

        int per = Layout.PER_PAGE;
        int maxPage = cats.isEmpty() ? 0 : (cats.size() - 1) / per;
        int p = Math.max(0, Math.min(page, maxPage));

        ChestMenu m = new ChestMenu("Categories", 6);
        if (cats.isEmpty()) {
            m.set(22, Icons.of(Items.PAPER, "§7No categories yet",
                    "§8Set one with /cb setcategory <id> <name>"));
        }

        int start = p * per;
        for (int i = 0; i < per; i++) {
            int gi = start + i;
            if (gi >= cats.size()) break;
            String cat = cats.get(gi);
            int count = SlotManager.byCategory(cat).size();

            // Colour tag tints the category name
            String colorTag = CategoryMetadataStore.getColorTag(cat);
            String nameColor = colorTag.isEmpty() ? "§e" : colorTag;
            String desc = CategoryMetadataStore.getDescription(cat);

            // Build lore lines
            List<String> lore = new ArrayList<>();
            lore.add("§7" + count + " block" + (count == 1 ? "" : "s"));
            if (!desc.isEmpty()) lore.add("§7" + desc);
            lore.add("§a▸ Left-click to browse");
            lore.add("§e▸ Right-click to edit");

            m.set(i, Icons.of(iconFor(cat), nameColor + cat,
                            lore.toArray(new String[0])),
                    (pl, b, a) -> {
                        if (b == 1) { // right-click → edit menu
                            GuiFx.open(pl);
                            GuiRouter.navigate(pl, MenuKey.of(Dest.CATEGORY_EDIT, cat));
                        } else { // left-click → browser
                            GuiFx.open(pl);
                            GuiRouter.navigate(pl, MenuKey.of(Dest.CATEGORY_BROWSE, cat));
                        }
                    });
        }

        Layout.pagedFooter(m, p, maxPage, Dest.CATEGORY_LIST, "", cats.size());
        return m;
    }

    /** A category's icon: its display block's item if set and still present, else a bookshelf. */
    static Item iconFor(String category) {
        String iconId = CategoryDisplayBlockManager.get(category);
        if (iconId != null) {
            SlotData d = SlotManager.getById(iconId);
            if (d != null) {
                SlotBlock.SlotItem it = SlotManager.itemAt(d.index());
                if (it != null) return it;
            }
        }
        return Items.BOOKSHELF;
    }
}
