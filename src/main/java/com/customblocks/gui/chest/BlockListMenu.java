/**
 * BlockListMenu.java — paginated browser of every assigned block (Group 02, G02.2).
 * Each icon shows the block's display item plus its id / slot / category; clicking opens
 * the editor for that block. Content fills slots 0..44; the footer provides pagination.
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

public final class BlockListMenu {

    private BlockListMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, int page) {
        List<SlotData> all = new ArrayList<>(SlotManager.assignedSlots());
        all.sort(Comparator.comparingInt(SlotData::index));

        int per = Layout.PER_PAGE;
        int maxPage = all.isEmpty() ? 0 : (all.size() - 1) / per;
        int p = Math.max(0, Math.min(page, maxPage));

        ChestMenu m = new ChestMenu("Block List", 6);
        if (all.isEmpty()) {
            m.set(22, Icons.of(Items.PAPER, "§7No blocks yet", "§8Create one with /cb create <id>"));
        }

        int start = p * per;
        for (int i = 0; i < per; i++) {
            int gi = start + i;
            if (gi >= all.size()) break;
            SlotData d = all.get(gi);
            m.set(i, icon(d), (pl, b, a) -> GuiRouter.navigate(pl, MenuKey.of(Dest.EDITOR, d.customId())));
        }

        Layout.pagedFooter(m, p, maxPage, Dest.BLOCK_LIST, "", all.size());
        return m;
    }

    private static ItemStack icon(SlotData d) {
        SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
        Item display = item != null ? item : Items.STONE;
        return Icons.of(display, "§f" + d.displayName(),
                "§7id: §f" + d.customId(),
                "§7slot: §f" + d.index(),
                "§7category: §f" + (d.category().isEmpty() ? "none" : d.category()),
                "§eClick to edit");
    }
}
