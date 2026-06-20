/**
 * CategoryBrowserMenu.java — one category's block browser (Group 11, G11.1/G11.2).
 *
 * A paginated chest GUI of every block in one category, reached by clicking a category in the
 * overview (/cb categories → CategoryListMenu). The top row carries the category header + icon,
 * a "Set icon" button, and the "Give All", "Export" and "Share" action slots; the body
 * (slots 9..44) holds one tile per block. Clicking a block opens CategoryBlockMenu (Give / Edit /
 * Remove). Give All / Export / Share route to the tested /cb givecategory / exportcategory /
 * sharecategory commands.
 *
 * Depends on: SlotManager, SlotBlock, CategoryListMenu (icon), Layout, Icons, GuiFx, GuiRouter, Nav
 * Called by:  GuiRouter (Dest.CATEGORY_BROWSE), CategoryListMenu
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
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CategoryBrowserMenu {

    private CategoryBrowserMenu() {} // static-only

    /** Body slots 9..44 hold blocks; row 0 is the header/actions, row 5 the nav footer. */
    private static final int BODY_START = 9;
    private static final int BODY_PER_PAGE = 36;

    public static ChestMenu build(ServerPlayerEntity player, String category, int page) {
        String cat = category == null ? "" : category.trim();

        List<SlotData> blocks = new ArrayList<>(SlotManager.byCategory(cat));
        blocks.sort(Comparator.comparingInt(SlotData::index));

        int maxPage = blocks.isEmpty() ? 0 : (blocks.size() - 1) / BODY_PER_PAGE;
        int p = Math.max(0, Math.min(page, maxPage));

        ChestMenu m = new ChestMenu(cat + " (" + blocks.size() + " block" + (blocks.size() == 1 ? "" : "s") + ")", 6);
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());

        // Top-row actions.
        m.set(4, Icons.glint(CategoryListMenu.iconFor(cat), "§e" + cat,
                "§7" + blocks.size() + " block" + (blocks.size() == 1 ? "" : "s") + " in this category"));
        m.set(8, Icons.of(Items.WRITABLE_BOOK, "§e§lEdit Category",
                        "§7Open the category editor",
                        "§8Display block, rename, merge…"),
                (pl, b, a) -> {
                    GuiFx.open(pl);
                    GuiRouter.navigate(pl, MenuKey.of(Dest.CATEGORY_EDIT, cat));
                });
        m.set(0, Icons.of(Items.HOPPER, "§a§lGive All",
                        "§7Give yourself one of every block",
                        "§7in this category (/cb category give)"),
                (pl, b, a) -> GuiRouter.runCommand(pl, "category give " + cat));
        m.set(2, Icons.of(Items.WRITABLE_BOOK, "§b§lExport",
                        "§7ZIP every block in this category",
                        "§7(textures + JSON) to cloud_exports/."),
                (pl, b, a) -> GuiRouter.runCommand(pl, "category export " + cat));
        m.set(6, Icons.of(Items.ENDER_PEARL, "§d§lShare",
                        "§7Upload this category to the vault",
                        "§7and get a share code to send to friends.",
                        "§8Needs vaultEndpoint set in config.json."),
                (pl, b, a) -> GuiRouter.runCommand(pl, "category share " + cat));

        if (blocks.isEmpty()) {
            m.set(22, Icons.of(Items.PAPER, "§7No blocks in \"" + cat + "\"",
                    "§8Add one with /cb setcategory <id> " + cat));
        }

        int start = p * BODY_PER_PAGE;
        for (int i = 0; i < BODY_PER_PAGE; i++) {
            int gi = start + i;
            if (gi >= blocks.size()) break;
            SlotData d = blocks.get(gi);
            m.set(BODY_START + i, icon(d), (pl, b, a) -> {
                GuiFx.click(pl);
                GuiRouter.navigate(pl, MenuKey.of(Dest.CATEGORY_BLOCK, d.customId() + " " + cat));
            });
        }

        Layout.pagedFooter(m, p, maxPage, Dest.CATEGORY_BROWSE, cat, blocks.size());
        return m;
    }

    private static ItemStack icon(SlotData d) {
        SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
        Item display = item != null ? item : Items.STONE;
        return Icons.of(display, "§f" + d.displayName(),
                "§7id: §f" + d.customId(),
                "§7slot: §f" + d.index(),
                "§7Click to manage §8(give / edit / remove)");
    }
}
