/**
 * SafetyMenu.java — the /cb safety dashboard (Group 09, Slice 5).
 *
 * A polished 6-row "data safety" panel that ties every Group 09 feature together: blocks in use, backups,
 * auto-backup status, trash, and broken blocks — each a tile that jumps to the matching screen. The header
 * shows an at-a-glance health line. Read-only except the click hand-offs (which go to the existing,
 * tested screens / commands — nothing is re-implemented here).
 *
 * Depends on: ChestMenu, Icons, GuiRouter, GuiFx, Nav, SlotManager, BackupManager, TrashManager,
 *             BrokenBlockScanner, CustomBlocksConfig.
 * Called by:  GuiRouter.build (Dest.SAFETY); opened by /cb safety.
 */
package com.customblocks.gui.chest;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.core.BackupManager;
import com.customblocks.core.BrokenBlockScanner;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TrashManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class SafetyMenu {

    private SafetyMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player) {
        int used = SlotManager.usedSlots();
        int max = SlotManager.getMaxSlots();
        var backups = BackupManager.list();
        int backupCount = backups.size();
        String newest = backups.isEmpty() ? null : backups.get(0).name();
        int trash = TrashManager.list().size();
        int broken = BrokenBlockScanner.count();
        int iv = CustomBlocksConfig.autoBackupInterval;

        int issues = (backupCount == 0 ? 1 : 0) + (broken > 0 ? 1 : 0);
        String status = issues == 0 ? "§a§lAll good"
                : "§e§l" + issues + " thing" + (issues == 1 ? "" : "s") + " to look at";

        ChestMenu m = new ChestMenu("Data Safety", 6).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 45; i < 54; i++) m.set(i, Icons.accent());

        m.set(4, Icons.glint(Items.NETHER_STAR, "§b§lData Safety  §7— " + status,
                "§7Backups, trash and block health at a glance.",
                "§7Click a tile below to jump to it."));

        // ── Status tiles (row 3) ──────────────────────────────────────────────
        m.set(19, Icons.of(Items.CHEST, "§e§lBlocks §f= §a" + used + "§7/§f" + max,
                "§7Custom blocks in use.",
                "§8" + (max - used) + " free slot(s)."));

        m.set(20, Icons.glint(Items.BARREL, "§a§lBackups §f= §e" + backupCount,
                newest != null ? "§7Newest: §f" + newest : "§cNo backups yet — make one!",
                "§aClick §7→ open the backup manager"),
                (p, b, a) -> { GuiFx.open(p); GuiRouter.navigate(p, MenuKey.of(Dest.BACKUP_LIST)); });

        m.set(21, Icons.glint(Items.CLOCK, "§a§lAuto-Backup §f= "
                        + (iv <= 0 ? "§cOFF" : "§aevery " + iv + " min"),
                "§7Keep newest: §f" + CustomBlocksConfig.autoBackupKeepCount,
                "§aClick §7→ server config (to change it)"),
                (p, b, a) -> { GuiFx.open(p); GuiRouter.navigate(p, MenuKey.of(Dest.CONFIG_CONFIRM)); });

        m.set(22, Icons.glint(Items.CAULDRON, "§a§lTrash §f= §e" + trash,
                "§7Deleted blocks you can still restore.",
                "§aClick §7→ open the trash browser"),
                (p, b, a) -> { GuiFx.open(p); GuiRouter.navigate(p, MenuKey.of(Dest.TRASH_LIST)); });

        m.set(23, broken == 0
                        ? Icons.of(Items.LIME_DYE, "§a§lBroken Blocks §f= §a0 ✔", "§7Every block has its texture.")
                        : Icons.glint(Items.RED_WOOL, "§c§lBroken Blocks §f= §c" + broken,
                                "§7Blocks missing their texture (render purple).",
                                "§aClick §7→ open the broken-blocks fixer"),
                (p, b, a) -> { GuiFx.open(p); GuiRouter.navigate(p, MenuKey.of(Dest.BROKEN_LIST)); });

        // ── Quick action (row 4) ──────────────────────────────────────────────
        m.set(31, Icons.of(Items.WRITABLE_BOOK, "§a§lSave a backup now",
                "§7Snapshot every block right now.",
                "§8Same as /cb backup save."),
                (p, b, a) -> { GuiFx.apply(p); GuiRouter.runCommand(p, "backup save"); });

        m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }
}
