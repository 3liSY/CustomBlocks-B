/**
 * BulkSelectMenu.java — Step 1 of every two-step bulk op: choose which blocks.
 *
 * Shared by all operations (the op is read from BulkSession.op, which only colours/labels the
 * screen — the selection itself is op-agnostic). Two ways to select, shown side-by-side:
 *   Option A — a Filter tile that cycles All blocks / Category / Favorited / Locked / Name contains
 *              / ID starts. The three text kinds (category/name/id) reveal an anvil "value" tile.
 *   Option B — "Select specific blocks" → opens the block list in pick mode, where a green-concrete
 *              tile confirms the ticked set and returns here.
 * Until something is chosen the header reads "None selected currently"; §aNext →§r only lights once
 * the selection resolves to ≥1 block. Next → opens the Step-2 action GUI for the current op.
 *
 * State lives on BulkSession (selMode / selFilterKind / selFilterValue) and ListSelection (the
 * picked ids). No bulk logic here — the resolve is the read-only BulkScope used everywhere else.
 *
 * Opened by: every /cb bulk<op> (no args) and the Bulk Hub's op tiles.
 */
package com.customblocks.gui.chest;

import com.customblocks.core.BulkScope;
import com.customblocks.core.SlotData;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BulkSelectMenu {

    private BulkSelectMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        BulkSession s = BulkSession.get(uuid);
        String op = s.op;
        s.listPickForBulk = false; // we're back on Step 1 — the list is no longer in pick mode

        boolean filterMode = "filter".equals(s.selMode);
        boolean has = s.selHasSelection(uuid);
        String expr = s.selScopeExpr(uuid);
        List<SlotData> matches = (has && !expr.isBlank()) ? BulkScope.resolve(expr, uuid) : List.of();
        int count = matches.size();

        ChestMenu m = new ChestMenu(BulkStyle.titleColor(op) + "Bulk " + BulkSession.prettyOp(op)
                        + " — Step 1: Choose blocks", 6).fill()
                .frame(Icons.of(BulkStyle.framePane(op), " "), Icons.of(BulkStyle.frameCorner(op), " "));

        m.set(4, Icons.glint(BulkStyle.opIcon(op), BulkStyle.headerColor(op) + "§l"
                        + BulkSession.prettyOp(op) + " · Step 1 — Choose blocks",
                "§7Pick which blocks this affects.",
                "§7Selected: §f" + s.selLabel(uuid) + (has ? " §8(" + count + ")" : ""),
                "§8Use §bOption A§8 (a rule) or §dOption B§8 (hand-pick)."));

        // ── Option A — the filter-kind cycle.
        List<String> aLore = new ArrayList<>();
        aLore.add("§7Match blocks by a rule.");
        aLore.add("");
        for (String k : BulkSession.SEL_FILTER_KINDS) {
            boolean cur = filterMode && k.equals(s.selFilterKind);
            aLore.add(cur ? "§e▸ §f§l" + BulkSession.selKindLabel(k) : "§8• " + BulkSession.selKindLabel(k));
        }
        aLore.add("");
        aLore.add("§eLeft§7/§eright-click §7to cycle");
        ItemStack aTile = filterMode
                ? Icons.glint(Items.HOPPER, "§b§lOption A — Filter", aLore.toArray(new String[0]))
                : Icons.of(Items.HOPPER, "§b§lOption A — Filter", aLore.toArray(new String[0]));
        m.set(20, aTile, (p, b, a) -> {
            GuiFx.click(p);
            BulkSession ss = BulkSession.get(p.getUuid());
            if (!"filter".equals(ss.selMode)) ss.selMode = "filter"; // first click selects the shown kind
            else ss.cycleSelFilterKind(b == 1 ? -1 : 1);
            GuiRouter.repage(p, MenuKey.of(Dest.BULK_SELECT));
        });

        // Value tile for the text kinds (category / name / id) — only when needed.
        if (filterMode && s.selFilterNeedsValue()) {
            String vlabel = s.selFilterValue.isBlank() ? "§8(click to type)" : "§f" + s.selFilterValue;
            m.set(29, Icons.of(Items.NAME_TAG,
                            "§b§l↳ " + BulkSession.selKindLabel(s.selFilterKind) + ": " + vlabel,
                            "§7Type the text to match.", "", "§eClick §7to type it"),
                    (p, b, a) -> { GuiFx.click(p); openValue(p, s.selFilterKind, s.selFilterValue); });
        }

        // ── divider
        m.set(22, Icons.of(Items.LIGHT_GRAY_STAINED_GLASS_PANE, "§7— or —"));

        // ── Option B — hand-pick from the block list.
        boolean picked = "picked".equals(s.selMode);
        int pn = ListSelection.size(uuid);
        m.set(24, (picked && pn > 0)
                        ? Icons.glint(Items.CHEST, "§d§lOption B — Picked: " + pn,
                                "§7You hand-picked §f" + pn + " §7block(s).",
                                "", "§eClick §7to change the picks")
                        : Icons.of(Items.CHEST, "§d§lOption B — Select specific blocks",
                                "§7Open the block list and tick the ones you want.",
                                "§8Confirm there with the §agreen block§8.",
                                "", "§eClick §7to open the list"),
                (p, b, a) -> {
                    GuiFx.open(p);
                    BulkSession ss = BulkSession.get(p.getUuid());
                    if (!"picked".equals(ss.selMode)) ListSelection.clear(p.getUuid()); // fresh pick
                    ss.listPickForBulk = true;
                    GuiRouter.navigate(p, MenuKey.of(Dest.BLOCK_LIST));
                });

        // ── live selection summary
        List<String> sl = new ArrayList<>();
        sl.add("§7What this op will affect.");
        sl.add("");
        if (!has) {
            sl.add("§8None selected currently");
        } else {
            sl.add("§7" + s.selLabel(uuid));
            sl.add("§7Matches: §e" + count + " §7block(s)");
            sl.add("");
            int shown = Math.min(6, count);
            for (int i = 0; i < shown; i++) sl.add("§8• §f" + matches.get(i).customId());
            if (count > shown) sl.add("§8…and " + (count - shown) + " more");
        }
        m.set(31, Icons.of(Items.SPYGLASS, "§b§lSelected: §e" + (has ? count : 0) + " §b§lblock(s)",
                sl.toArray(new String[0])));

        // ── Next → (lit only with a real selection)
        if (has && count > 0) {
            m.set(40, Icons.glint(Items.LIME_DYE, "§a§lNext →",
                            "§7Go to the §f" + BulkSession.prettyOp(op) + "§7 options.",
                            "§7On §e" + count + " §7block(s)."),
                    (p, b, a) -> { GuiFx.select(p); GuiRouter.navigate(p, MenuKey.of(Dest.BULK_ACTION)); });
        } else {
            m.set(40, Icons.of(Items.GRAY_DYE, "§8Next →",
                            (has && count == 0) ? "§7That selection matches no blocks."
                                                : "§7Select some blocks first."),
                    (p, b, a) -> GuiFx.deny(p));
        }

        m.set(45, Icons.back(), (p, b, a) -> { GuiFx.click(p); GuiRouter.back(p); });
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    /** Anvil for the category/name/id text; stores it on the session and re-shows Step 1 (stack kept). */
    private static void openValue(ServerPlayerEntity player, String kind, String current) {
        String title = switch (kind) {
            case "category" -> "Category name";
            case "name"     -> "Name contains";
            case "id"       -> "ID starts with";
            default         -> "Value";
        };
        AnvilPrompt.open(player, title, new ItemStack(Items.NAME_TAG), current,
                text -> {
                    BulkSession ss = BulkSession.get(player.getUuid());
                    ss.selFilterValue = text == null ? "" : text.trim();
                    ss.selMode = "filter";
                    GuiRouter.render(player, MenuKey.of(Dest.BULK_SELECT));
                },
                () -> GuiRouter.render(player, MenuKey.of(Dest.BULK_SELECT)));
    }
}
