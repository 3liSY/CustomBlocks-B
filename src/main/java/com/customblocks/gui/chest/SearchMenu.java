/**
 * SearchMenu.java — advanced block search GUI (finale fix; mirrors the old openSearchPicker).
 * Shows a paginated grid of blocks matching a query (name / id / category) via
 * SlotManager.search; clicking a block opens its editor. A bare /cb search opens the same
 * grid over every block plus a "New search" button that pre-fills the search command.
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

public final class SearchMenu {

    private SearchMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, String query, int page) {
        String q = query == null ? "" : query.trim();
        List<SlotData> hits = q.isEmpty()
                ? new ArrayList<>(SlotManager.assignedSlots())
                : new ArrayList<>(SlotManager.search(q));
        hits.sort(Comparator.comparingInt(SlotData::index));

        int per = Layout.PER_PAGE;
        int maxPage = hits.isEmpty() ? 0 : (hits.size() - 1) / per;
        int p = Math.max(0, Math.min(page, maxPage));

        ChestMenu m = new ChestMenu(q.isEmpty() ? "Search Blocks" : "Search: " + q, 6);

        if (hits.isEmpty()) {
            m.set(22, Icons.of(Items.BARRIER, "§cNo matches",
                    q.isEmpty() ? "§8No blocks exist yet" : "§8Nothing matches \"" + q + "\"",
                    "§7Use the spyglass below to search again"));
        }

        int start = p * per;
        for (int i = 0; i < per; i++) {
            int gi = start + i;
            if (gi >= hits.size()) break;
            SlotData d = hits.get(gi);
            m.set(i, icon(d), (pl, b, a) -> GuiRouter.navigate(pl, MenuKey.of(Dest.EDITOR, d.customId())));
        }

        Layout.pagedFooter(m, p, maxPage, Dest.SEARCH, q, hits.size());
        // Slot 46 is a plain filler from the footer; repurpose it as the "new search" action.
        m.set(46, Icons.glint(Items.SPYGLASS, "§b§lNew search",
                        "§7Search by name, id or category",
                        "§8Click, then type your query in chat"),
                (pl, b, a) -> GuiRouter.promptCommand(pl, "/cb search ", "search blocks"));
        return m;
    }

    private static ItemStack icon(SlotData d) {
        SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
        Item display = item != null ? item : Items.STONE;
        String cat = d.category() == null || d.category().isEmpty() ? "§8(none)" : "§f" + d.category();
        return Icons.of(display, "§f§l" + d.displayName(),
                "§7ID: §b" + d.customId(),
                "§7Slot: §f" + d.index() + " §8• §7Category: " + cat,
                "§7Sound: §f" + d.soundType() + " §8• §7Light: §e" + d.glow(),
                "§aClick §7→ open the block editor");
    }
}
