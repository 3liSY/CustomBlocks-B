/**
 * BackupConfirmMenu.java — the Yes/No screens for the /cb backup GUI's dangerous actions
 * (Group 09, Slice 2 GUI layer).
 *
 * One small 3-row screen drives two confirmations, chosen by the MenuKey arg:
 *   "load:<name>" — restore that backup over the live data (a safety copy is saved first).
 *   "deletesel"   — permanently delete every backup the player has ticked.
 *
 * The Yes buttons delegate to the tested paths (BackupCommands.guiLoad / BackupManager.delete) —
 * no restore or delete logic is re-implemented here. The chest's Yes IS the confirmation, so the
 * chat "/cb confirm" step is not needed for the GUI flow.
 *
 * Depends on: ChestMenu, Icons, GuiRouter, GuiFx, BackupSelection, BackupManager, BackupCommands, Chat.
 * Called by:  GuiRouter.build (Dest.BACKUP_CONFIRM).
 */
package com.customblocks.gui.chest;

import com.customblocks.command.Chat;
import com.customblocks.command.handlers.BackupCommands;
import com.customblocks.core.BackupManager;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public final class BackupConfirmMenu {

    private BackupConfirmMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, String arg) {
        String a = arg == null ? "" : arg;
        if (a.startsWith("load:")) return loadConfirm(a.substring("load:".length()));
        if (a.equals("deletesel")) return deleteConfirm(player);
        // Unknown action — bounce back.
        ChestMenu m = new ChestMenu("Backups", 3).fill();
        m.set(22, Icons.back(), (p, b, x) -> GuiRouter.back(p));
        return m;
    }

    // ── Load (restore) one backup ─────────────────────────────────────────────
    private static ChestMenu loadConfirm(String name) {
        ChestMenu m = frame("Load \"" + name + "\"?");

        if (!BackupManager.isValidBackup(name)) {
            m.set(13, Icons.of(Items.BARRIER, "§c§lBackup unreadable",
                    "§7\"§f" + name + "§7\" is missing or broken.",
                    "§8Pick another one from the list."));
            m.set(26, Icons.close(), (p, b, a) -> GuiRouter.back(p));
            return m;
        }

        m.set(13, Icons.of(Items.BOOK, "§e§lWhat loading does",
                "§7Replaces ALL current blocks with the ones",
                "§7saved in §f" + name + "§7.",
                "§aA safety copy of your current blocks is saved",
                "§afirst §7— so you can undo this load.",
                "§8Players see one brief pack reload."));

        m.set(11, Icons.glint(Items.LIME_DYE, "§a§lYes — load it",
                        "§7Restore from §f" + name + "§7 now.",
                        "§8Runs on the server; chat reports when done."),
                (p, b, a) -> { GuiFx.danger(p); BackupCommands.guiLoad(p, name); });

        m.set(15, Icons.of(Items.RED_DYE, "§c§lNo — go back",
                        "§7Keep current blocks. Nothing changes."),
                (p, b, a) -> { GuiFx.click(p); GuiRouter.back(p); });

        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    // ── Delete every ticked backup ────────────────────────────────────────────
    private static ChestMenu deleteConfirm(ServerPlayerEntity player) {
        int n = BackupSelection.size(player.getUuid());
        ChestMenu m = frame("Delete " + n + " backup(s)?");

        if (n == 0) {
            m.set(13, Icons.of(Items.PAPER, "§7Nothing selected",
                    "§8Tick some backups first, then try again."));
            m.set(26, Icons.back(), (p, b, a) -> GuiRouter.back(p));
            return m;
        }

        m.set(13, Icons.of(Items.BOOK, "§c§lDelete " + n + " backup(s)?",
                "§7Permanently removes the ticked backup(s).",
                "§aYour live blocks are NOT touched —",
                "§aonly the backup copies are deleted.",
                "§8This can't be undone."));

        m.set(11, Icons.glint(Items.TNT, "§c§lYes — delete " + n,
                        "§7Remove the ticked backup(s) for good."),
                (p, b, a) -> doDeleteSelected(p));

        m.set(15, Icons.of(Items.LIME_DYE, "§a§lNo — keep them",
                        "§7Don't delete anything."),
                (p, b, a) -> { GuiFx.click(p); GuiRouter.back(p); });

        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    private static void doDeleteSelected(ServerPlayerEntity player) {
        GuiFx.danger(player);
        List<String> names = new ArrayList<>(BackupSelection.names(player.getUuid()));
        int ok = 0;
        for (String name : names) if (BackupManager.delete(name)) ok++;
        BackupSelection.clear(player.getUuid());
        Chat.success(player.getCommandSource(), "Deleted " + ok + " backup(s).");
        GuiRouter.back(player); // back to the list, which rebuilds without them
    }

    /** A 3-row screen with light-blue accent top/bottom rows, filled centre. */
    private static ChestMenu frame(String title) {
        ChestMenu m = new ChestMenu(title, 3).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 18; i < 27; i++) m.set(i, Icons.accent());
        return m;
    }
}
