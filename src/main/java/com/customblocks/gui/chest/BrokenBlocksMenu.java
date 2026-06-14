/**
 * BrokenBlocksMenu.java — the /cb showbrokenblocks report + bulk fixer (Group 09, Slice 5).
 *
 * A paginated 6-row list of blocks whose baked texture is missing (they'd render purple), one red-wool
 * tile each. Per tile: §eleft-click§r ticks it for a bulk action · §eright-click§r fixes just that one
 * (re-bake from saved source, or pre-fill /cb retexture if there's no source).
 *
 * Footer adds the "many cool actions": Fix selected (re-bake the ticked ones that have a saved image),
 * selection summary/clear, Select-all, and Delete selected (→ confirm → trash). Everything reuses tested
 * paths — SafetyCommands.guiRebake/guiRebakeMany and SlotManager.delete; nothing is re-implemented here.
 *
 * Depends on: ChestMenu, Icons, Layout, GuiRouter, GuiFx, BrokenSelection, BrokenBlockScanner, SafetyCommands.
 * Called by:  GuiRouter.build (Dest.BROKEN_LIST); opened by /cb showbrokenblocks and the Safety dashboard.
 */
package com.customblocks.gui.chest;

import com.customblocks.command.handlers.SafetyCommands;
import com.customblocks.core.BrokenBlockScanner;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public final class BrokenBlocksMenu {

    private BrokenBlocksMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, int page) {
        List<BrokenBlockScanner.Broken> all = BrokenBlockScanner.scan();
        int per = Layout.PER_PAGE; // 45 content slots (0..44); footer 45..53
        int maxPage = all.isEmpty() ? 0 : (all.size() - 1) / per;
        int p = Math.max(0, Math.min(page, maxPage));

        ChestMenu m = new ChestMenu("Broken Blocks · " + all.size(), 6).fill();

        if (all.isEmpty()) {
            m.set(22, Icons.glint(Items.LIME_DYE, "§a§lNo broken blocks",
                    "§7Every block has its texture file. Nothing to fix."));
            Layout.pagedFooter(m, 0, 0, Dest.BROKEN_LIST, "", 0);
            return m;
        }

        int start = p * per;
        for (int i = 0; i < per; i++) {
            int gi = start + i;
            if (gi >= all.size()) break;
            BrokenBlockScanner.Broken b = all.get(gi);
            boolean sel = BrokenSelection.has(player.getUuid(), b.customId());
            m.set(i, tile(b, sel), (pl, btn, act) -> {
                if (btn == 1) {               // right-click → fix just this one
                    if (b.hasSource()) { GuiFx.apply(pl); SafetyCommands.guiRebake(pl, b.customId()); }
                    else { GuiFx.click(pl); GuiRouter.promptCommand(pl,
                            "/cb retexture " + b.customId() + " ", "Retexture " + b.customId()); }
                } else {                       // left-click → tick for a bulk action
                    BrokenSelection.toggle(pl.getUuid(), b.customId());
                    GuiFx.select(pl);
                    GuiRouter.repage(pl, new MenuKey(Dest.BROKEN_LIST, "", p));
                }
            });
        }

        Layout.pagedFooter(m, p, maxPage, Dest.BROKEN_LIST, "", all.size());
        decorateFooter(m, player, p, all);
        return m;
    }

    /** Extra footer controls over the standard nav row (46/47/51/52 are free there). */
    private static void decorateFooter(ChestMenu m, ServerPlayerEntity player, int page,
                                       List<BrokenBlockScanner.Broken> all) {
        int sel = BrokenSelection.size(player.getUuid());

        // 46 — Fix selected (re-bake the ticked blocks that have a saved image).
        m.set(46, sel == 0
                        ? Icons.of(Items.GRAY_DYE, "§8Fix selected", "§8Tick some blocks first")
                        : Icons.glint(Items.SLIME_BALL, "§a§lFix " + sel + " selected",
                                "§7Rebuild textures from saved images.",
                                "§8Ticked blocks with no saved image are skipped."),
                (p, b, a) -> {
                    List<String> ids = new ArrayList<>(BrokenSelection.ids(p.getUuid()));
                    if (ids.isEmpty()) { GuiFx.deny(p); return; }
                    GuiFx.apply(p);
                    SafetyCommands.guiRebakeMany(p, ids);
                });

        // 47 — selection summary / clear.
        m.set(47, sel == 0
                        ? Icons.of(Items.LIGHT_GRAY_DYE, "§7Nothing selected", "§8Left-click a block to tick it")
                        : Icons.glint(Items.LIME_DYE, "§a" + sel + " selected", "§7Click to clear the selection"),
                (p, b, a) -> { BrokenSelection.clear(p.getUuid()); GuiFx.select(p);
                        GuiRouter.repage(p, new MenuKey(Dest.BROKEN_LIST, "", page)); });

        // 51 — select all broken blocks (across pages).
        m.set(51, Icons.of(Items.BUNDLE, "§eSelect all broken",
                        "§7Tick all " + all.size() + " broken block(s)"),
                (p, b, a) -> {
                    List<String> ids = new ArrayList<>();
                    for (BrokenBlockScanner.Broken bk : all) ids.add(bk.customId());
                    BrokenSelection.addAll(p.getUuid(), ids);
                    GuiFx.select(p);
                    GuiRouter.repage(p, new MenuKey(Dest.BROKEN_LIST, "", page));
                });

        // 52 — Delete selected (→ confirm → trash). Disabled at 0 ticked.
        m.set(52, sel == 0
                        ? Icons.of(Items.GRAY_CONCRETE, "§8Delete selected", "§8Tick some blocks first")
                        : Icons.glint(Items.TNT, "§c§lDelete " + sel + " selected",
                                "§7Remove these blocks (they go to the trash,",
                                "§7so you can still restore them later)."),
                (p, b, a) -> {
                    if (BrokenSelection.size(p.getUuid()) == 0) { GuiFx.deny(p); return; }
                    GuiFx.danger(p);
                    GuiRouter.navigate(p, MenuKey.of(Dest.BROKEN_CONFIRM, "deletesel"));
                });
    }

    private static ItemStack tile(BrokenBlockScanner.Broken b, boolean selected) {
        String src = b.hasSource() ? "§7Saved image: §ayes" : "§7Saved image: §8none";
        String fix = b.hasSource()
                ? "§aRight-click §8→ rebuild from the saved image"
                : "§eRight-click §8→ retexture it (needs an image URL)";
        if (selected) {
            return Icons.glint(Items.RED_WOOL, "§a✔ §c" + b.customId(),
                    "§7slot: §f" + b.index(),
                    "§7Missing baked texture — renders purple.",
                    src,
                    "§a✔ ticked",
                    "§7Left-click §8untick §8| " + fix);
        }
        return Icons.of(Items.RED_WOOL, "§c" + b.customId(),
                "§7slot: §f" + b.index(),
                "§7Missing baked texture — renders purple.",
                src,
                "§7Left-click §8tick §8| " + fix);
    }
}
