/**
 * TrashMenu.java — the /cb deletedblocks browser (Group 09, Slice 4).
 *
 * A paginated 6-row list of recently deleted blocks, newest first (TrashManager.list() lazily prunes
 * expired unpinned entries first). Click a tile to open its actions (Restore / Pin / Delete forever)
 * in TrashEntryMenu. Pinned entries show a star and are never auto-pruned. Read-only here — every
 * mutation happens via TrashEntryMenu / TrashCommands.
 *
 * Depends on: ChestMenu, Icons, Layout, GuiRouter, TrashManager.
 * Called by:  GuiRouter.build (Dest.TRASH_LIST); opened by /cb deletedblocks.
 */
package com.customblocks.gui.chest;

import com.customblocks.core.TrashManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public final class TrashMenu {

    private TrashMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, int page) {
        List<TrashManager.TrashEntry> all = TrashManager.list();
        int per = Layout.PER_PAGE; // 45 content slots (0..44); footer 45..53
        int maxPage = all.isEmpty() ? 0 : (all.size() - 1) / per;
        int p = Math.max(0, Math.min(page, maxPage));

        ChestMenu m = new ChestMenu("Deleted Blocks · " + all.size(), 6).fill();

        if (all.isEmpty()) {
            m.set(22, Icons.of(Items.PAPER, "§7The trash is empty",
                    "§8Deleted blocks land here so you can restore them.",
                    "§8They're kept until pruned (config: trashRetentionDays)."));
        }

        int start = p * per;
        for (int i = 0; i < per; i++) {
            int gi = start + i;
            if (gi >= all.size()) break;
            TrashManager.TrashEntry e = all.get(gi);
            m.set(i, tile(e), (pl, btn, act) -> {
                GuiFx.click(pl);
                GuiRouter.navigate(pl, MenuKey.of(Dest.TRASH_ENTRY, e.entryId()));
            });
        }

        Layout.pagedFooter(m, p, maxPage, Dest.TRASH_LIST, "", all.size());
        return m;
    }

    private static net.minecraft.item.ItemStack tile(TrashManager.TrashEntry e) {
        String when = e.deletedHuman().isEmpty() ? "?" : e.deletedHuman();
        String tex = e.hasTexture() ? "§7texture: §asaved" : "§7texture: §8none";
        if (e.pinned()) {
            return Icons.glint(Items.AMETHYST_SHARD, "§b★ §f" + e.customId(),
                    "§7name: §f" + e.displayName(),
                    "§7deleted: §f" + when,
                    tex,
                    "§bPinned §8— never auto-pruned",
                    "§7Click §8→ restore / unpin / delete");
        }
        return Icons.of(Items.LIGHT_GRAY_DYE, "§f" + e.customId(),
                "§7name: §f" + e.displayName(),
                "§7deleted: §f" + when,
                tex,
                "§7Click §8→ restore / pin / delete");
    }
}
