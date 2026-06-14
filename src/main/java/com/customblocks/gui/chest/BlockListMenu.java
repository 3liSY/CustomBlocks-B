/**
 * BlockListMenu.java — paginated browser of every assigned block (Group 02, G02.2), now with an
 * in-list search box (slice A), tick-many multi-select (slice B) and a one-click hand-off of the
 * selected set to the Bulk Hub (slice C).
 *
 * Per block:  §eleft-click§r toggles the ✔ selection · §eright-click§r opens that block's editor.
 * Footer:     back · search (+clear) · "N selected" (click = clear) · prev/page/next ·
 *             select-all-matching · "Bulk actions on N" · close.
 *
 * The search query rides on the MenuKey arg so pagination keeps it. Selection lives in
 * ListSelection (per player). The bulk hand-off only sets BulkSession.filter to the picked ids and
 * opens the existing, tested Bulk Hub — no bulk logic is duplicated here.
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
import java.util.Locale;

public final class BlockListMenu {

    private BlockListMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, String query, int page) {
        String q = query == null ? "" : query.trim();
        String ql = q.toLowerCase(Locale.ROOT);

        List<SlotData> all = new ArrayList<>(SlotManager.assignedSlots());
        all.sort(Comparator.comparingInt(SlotData::index));
        List<SlotData> shown = new ArrayList<>();
        for (SlotData d : all) {
            if (q.isEmpty() || matches(d, ql)) shown.add(d);
        }

        int per = Layout.PER_PAGE;
        int maxPage = shown.isEmpty() ? 0 : (shown.size() - 1) / per;
        int p = Math.max(0, Math.min(page, maxPage));

        ChestMenu m = new ChestMenu("Block List", 6);
        if (shown.isEmpty()) {
            m.set(22, q.isEmpty()
                    ? Icons.of(Items.PAPER, "§7No blocks yet", "§8Create one with /cb create <id>")
                    : Icons.of(Items.PAPER, "§7No blocks match \"" + q + "\"", "§8Right-click Search to clear"));
        }

        int start = p * per;
        for (int i = 0; i < per; i++) {
            int gi = start + i;
            if (gi >= shown.size()) break;
            SlotData d = shown.get(gi);
            boolean sel = ListSelection.has(player.getUuid(), d.customId());
            m.set(i, icon(d, sel), (pl, b, a) -> {
                if (b == 1) { // right-click → edit
                    GuiRouter.navigate(pl, MenuKey.of(Dest.EDITOR, d.customId()));
                } else {      // left-click → toggle the ✔, refresh the list in place
                    ListSelection.toggle(pl.getUuid(), d.customId());
                    GuiFx.select(pl);
                    GuiRouter.repage(pl, new MenuKey(Dest.BLOCK_LIST, q, p));
                }
            });
        }

        Layout.pagedFooter(m, p, maxPage, Dest.BLOCK_LIST, q, shown.size());
        decorateFooter(m, player, q, p, shown);
        return m;
    }

    /** Extra footer controls layered over the standard nav row (slots 46/47/51/52 are free there). */
    private static void decorateFooter(ChestMenu m, ServerPlayerEntity player, String q, int page, List<SlotData> shown) {
        int sel = ListSelection.size(player.getUuid());

        // 46 — Search: left-click types a query, right-click clears it.
        m.set(46, Icons.of(Items.COMPASS, "§eSearch",
                        q.isEmpty() ? "§7Filter by id / name / category" : "§7Showing: §f" + q,
                        "§7Left-click §8type a search",
                        "§7Right-click §8clear the search"),
                (p, b, a) -> {
                    if (b == 1) { GuiFx.click(p); GuiRouter.repage(p, new MenuKey(Dest.BLOCK_LIST, "", 0)); }
                    else openSearch(p);
                });

        // 47 — selection summary / clear.
        m.set(47, sel == 0
                        ? Icons.of(Items.LIGHT_GRAY_DYE, "§7Nothing selected", "§8Left-click a block to tick it")
                        : Icons.glint(Items.LIME_DYE, "§a" + sel + " selected", "§7Click to clear the selection"),
                (p, b, a) -> { ListSelection.clear(p.getUuid()); GuiFx.select(p);
                        GuiRouter.repage(p, new MenuKey(Dest.BLOCK_LIST, q, page)); });

        // 51 — select every block currently shown (respects the search filter).
        m.set(51, Icons.of(Items.BUNDLE, "§eSelect all shown",
                        "§7Tick the " + shown.size() + " block(s) in this list"),
                (p, b, a) -> {
                    List<String> ids = new ArrayList<>();
                    for (SlotData d : shown) ids.add(d.customId());
                    ListSelection.addAll(p.getUuid(), ids);
                    GuiFx.select(p);
                    GuiRouter.repage(p, new MenuKey(Dest.BLOCK_LIST, q, page));
                });

        // 52 — confirm the ticked set. In bulk-pick mode a green-concrete tile returns to the
        // bulk Step-1 builder; otherwise it hands off to the Bulk Hub (slice C). Disabled at 0 ticked.
        if (BulkSession.get(player.getUuid()).listPickForBulk) {
            m.set(52, sel == 0
                            ? Icons.of(Items.GRAY_CONCRETE, "§8Confirm selection", "§8Tick some blocks first")
                            : Icons.glint(Items.GREEN_CONCRETE, "§a✔ Use these " + sel + " block(s)",
                                    "§7Use just the blocks you ticked.",
                                    "§8Returns to the bulk setup."),
                    (p, b, a) -> {
                        if (ListSelection.size(p.getUuid()) == 0) { GuiFx.deny(p); return; }
                        BulkSession s = BulkSession.get(p.getUuid());
                        s.selMode = "picked";
                        s.listPickForBulk = false;
                        GuiFx.select(p);
                        GuiRouter.back(p); // back to the bulk Step-1 menu beneath
                    });
        } else {
            m.set(52, sel == 0
                            ? Icons.of(Items.GRAY_DYE, "§8Bulk actions", "§8Tick some blocks first")
                            : Icons.glint(Items.CHEST, "§dBulk actions on " + sel + " selected",
                                    "§7Opens the Bulk Hub on just these blocks",
                                    "§8(delete / lock / favourite / category / export …)"),
                    (p, b, a) -> {
                        if (ListSelection.size(p.getUuid()) == 0) { GuiFx.deny(p); return; }
                        BulkSession.get(p.getUuid()).filter = ListSelection.joined(p.getUuid());
                        GuiFx.open(p);
                        GuiRouter.navigate(p, MenuKey.of(Dest.BULK_HUB));
                    });
        }
    }

    /** Anvil to type a search term; submit re-opens the list filtered, cancel clears it. */
    private static void openSearch(ServerPlayerEntity player) {
        AnvilPrompt.open(player, "Search blocks", new ItemStack(Items.COMPASS), "",
                text -> GuiRouter.repage(player,
                        new MenuKey(Dest.BLOCK_LIST, text == null ? "" : text.trim(), 0)),
                () -> GuiRouter.repage(player, new MenuKey(Dest.BLOCK_LIST, "", 0)));
    }

    private static boolean matches(SlotData d, String ql) {
        return d.customId().toLowerCase(Locale.ROOT).contains(ql)
                || d.displayName().toLowerCase(Locale.ROOT).contains(ql)
                || d.category().toLowerCase(Locale.ROOT).contains(ql);
    }

    private static ItemStack icon(SlotData d, boolean selected) {
        SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
        Item display = item != null ? item : Items.STONE;
        if (selected) {
            return Icons.glint(display, "§a✔ §f" + d.displayName(),
                    "§7id: §f" + d.customId(),
                    "§7slot: §f" + d.index(),
                    "§7category: §f" + (d.category().isEmpty() ? "none" : d.category()),
                    "§a✔ selected",
                    "§7Left-click §8unselect §8| §7Right-click §8edit");
        }
        return Icons.of(display, "§f" + d.displayName(),
                "§7id: §f" + d.customId(),
                "§7slot: §f" + d.index(),
                "§7category: §f" + (d.category().isEmpty() ? "none" : d.category()),
                "§7Left-click §8select §8| §7Right-click §8edit");
    }
}
