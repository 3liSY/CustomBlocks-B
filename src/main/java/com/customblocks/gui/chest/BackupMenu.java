/**
 * BackupMenu.java — the advanced /cb backup GUI (Group 09, Slice 2 GUI layer).
 *
 * A paginated 6-row chest browser of every saved backup, newest first. It re-uses the tested
 * BackupManager (list/save/delete) and BackupCommands orchestration — no backup logic is
 * duplicated here.
 *
 *   Per backup tile: §eleft-click§r ticks it for bulk delete · §eright-click§r opens a load confirm.
 *   Footer (45..53): back · create-new · selection summary/clear · prev · page · next ·
 *                    select-all · delete-selected · close.
 *
 * Auto/safety backups (auto-… and pre-restore-…) show with a barrel icon and an "(auto)" tag so the
 * developer can tell hand-made saves from machine-made ones. Selection lives in BackupSelection.
 *
 * Depends on: ChestMenu, Icons, Layout, GuiRouter, GuiFx, AnvilPrompt, BackupSelection,
 *             BackupManager, BackupCommands.
 * Called by:  GuiRouter.build (Dest.BACKUP_LIST); opened by /cb backup (no args) and /cb backupgui.
 */
package com.customblocks.gui.chest;

import com.customblocks.command.handlers.BackupCommands;
import com.customblocks.core.BackupManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public final class BackupMenu {

    private BackupMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, int page) {
        List<BackupManager.BackupInfo> all = BackupManager.list();
        int per = Layout.PER_PAGE; // 45 content slots (0..44); footer is 45..53
        int maxPage = all.isEmpty() ? 0 : (all.size() - 1) / per;
        int p = Math.max(0, Math.min(page, maxPage));

        ChestMenu m = new ChestMenu("Backups · " + all.size() + " saved", 6).fill();

        if (all.isEmpty()) {
            m.set(22, Icons.of(Items.PAPER, "§7No backups yet",
                    "§8Click §fCreate new backup §8below to make one.",
                    "§8A backup snapshots every block, its textures and config."));
        }

        int start = p * per;
        for (int i = 0; i < per; i++) {
            int gi = start + i;
            if (gi >= all.size()) break;
            BackupManager.BackupInfo b = all.get(gi);
            boolean sel = BackupSelection.has(player.getUuid(), b.name());
            m.set(i, tile(b, sel), (pl, btn, act) -> {
                if (btn == 1) { // right-click → load this backup (with a confirm screen)
                    GuiFx.click(pl);
                    GuiRouter.navigate(pl, MenuKey.of(Dest.BACKUP_CONFIRM, "load:" + b.name()));
                } else {        // left-click → tick for bulk delete, refresh in place
                    BackupSelection.toggle(pl.getUuid(), b.name());
                    GuiFx.select(pl);
                    GuiRouter.repage(pl, new MenuKey(Dest.BACKUP_LIST, "", p));
                }
            });
        }

        footer(m, player, p, maxPage, all.size());
        return m;
    }

    /** Bottom-row controls (slots 45..53). */
    private static void footer(ChestMenu m, ServerPlayerEntity player, int page, int maxPage, int total) {
        for (int i = 45; i < 54; i++) m.set(i, Icons.filler());
        int sel = BackupSelection.size(player.getUuid());

        // 45 — back (or home if /cb backup was the first screen).
        m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));

        // 46 — create a new backup (anvil name prompt, pre-filled with an auto name).
        m.set(46, Icons.of(Items.WRITABLE_BOOK, "§a§lCreate new backup",
                        "§7Save a snapshot of every block right now.",
                        "§8Type a name, or keep the suggested one."),
                (p, b, a) -> openCreate(p));

        // 47 — selection summary / clear.
        m.set(47, sel == 0
                        ? Icons.of(Items.LIGHT_GRAY_DYE, "§7Nothing selected",
                                "§8Left-click a backup to tick it for delete")
                        : Icons.glint(Items.LIME_DYE, "§a" + sel + " selected",
                                "§7Click to clear the selection"),
                (p, b, a) -> {
                    BackupSelection.clear(p.getUuid());
                    GuiFx.select(p);
                    GuiRouter.repage(p, new MenuKey(Dest.BACKUP_LIST, "", page));
                });

        // 48 / 49 / 50 — pagination.
        if (page > 0) {
            m.set(48, Icons.of(Items.ARROW, "§ePrevious page", "§8Page " + page + " / " + (maxPage + 1)),
                    (p, b, a) -> GuiRouter.repage(p, new MenuKey(Dest.BACKUP_LIST, "", page - 1)));
        }
        m.set(49, Icons.of(Items.PAPER, "§7Page §f" + (page + 1) + " §7/ §f" + (maxPage + 1),
                "§8" + total + " backup(s) total"));
        if (page < maxPage) {
            m.set(50, Icons.of(Items.ARROW, "§eNext page", "§8Page " + (page + 2) + " / " + (maxPage + 1)),
                    (p, b, a) -> GuiRouter.repage(p, new MenuKey(Dest.BACKUP_LIST, "", page + 1)));
        }

        // 51 — select every backup (across all pages).
        m.set(51, total == 0
                        ? Icons.of(Items.GRAY_DYE, "§8Select all", "§8No backups to select")
                        : Icons.of(Items.BUNDLE, "§eSelect all backups",
                                "§7Tick all " + total + " backup(s) for delete"),
                (p, b, a) -> {
                    if (total == 0) { GuiFx.deny(p); return; }
                    List<String> names = new ArrayList<>();
                    for (BackupManager.BackupInfo bi : BackupManager.list()) names.add(bi.name());
                    BackupSelection.addAll(p.getUuid(), names);
                    GuiFx.select(p);
                    GuiRouter.repage(p, new MenuKey(Dest.BACKUP_LIST, "", page));
                });

        // 52 — delete the ticked backups (confirm screen first). Disabled at 0 ticked.
        m.set(52, sel == 0
                        ? Icons.of(Items.GRAY_CONCRETE, "§8Delete selected", "§8Tick some backups first")
                        : Icons.glint(Items.TNT, "§c§lDelete " + sel + " selected",
                                "§7Permanently remove the ticked backup(s).",
                                "§8Your live blocks are NOT touched — only backups."),
                (p, b, a) -> {
                    if (BackupSelection.size(p.getUuid()) == 0) { GuiFx.deny(p); return; }
                    GuiFx.danger(p);
                    GuiRouter.navigate(p, MenuKey.of(Dest.BACKUP_CONFIRM, "deletesel"));
                });

        // 53 — close.
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
    }

    /** Anvil to name a new backup, pre-filled with an auto name; hands off to the tested save path. */
    private static void openCreate(ServerPlayerEntity player) {
        GuiFx.click(player);
        AnvilPrompt.open(player, "Name the backup", new ItemStack(Items.NAME_TAG),
                BackupManager.timestampName("backup"),
                text -> BackupCommands.guiCreate(player, text),
                () -> GuiRouter.render(player, MenuKey.of(Dest.BACKUP_LIST)));
    }

    private static ItemStack tile(BackupManager.BackupInfo b, boolean selected) {
        String when = b.created().isEmpty() ? "?" : b.created();
        String blocks = b.blocks() >= 0 ? (b.blocks() + " block(s)") : "?";
        String autoTag = b.auto() ? " §8(auto)" : "";
        if (selected) {
            return Icons.glint(Items.LIME_SHULKER_BOX, "§a✔ §f" + b.name(),
                    "§7saved: §f" + when,
                    "§7" + blocks + autoTag,
                    "§a✔ ticked for delete",
                    "§7Left-click §8untick §8| §7Right-click §8load this backup");
        }
        Item icon = b.auto() ? Items.BARREL : Items.CHEST;
        return Icons.of(icon, "§f" + b.name(),
                "§7saved: §f" + when,
                "§7" + blocks + autoTag,
                "§7Left-click §8tick for delete §8| §7Right-click §8load this backup");
    }
}
