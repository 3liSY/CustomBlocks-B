/**
 * MainMenu.java — the /cb dashboard (Group 02, G02.1; redesigned in the finale fix).
 * A polished 6-row "double chest" framed with glass panes. Buttons either open chest
 * sub-menus or run the existing chat commands for non-chest features. The Config button
 * opens a Yes/No confirmation screen first (mirrors the old version).
 */
package com.customblocks.gui.chest;

import com.customblocks.command.handlers.BulkSnapshot;
import com.customblocks.core.SlotManager;
import com.customblocks.core.onboarding.AchievementManager;
import com.customblocks.core.onboarding.TipPool;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class MainMenu {

    private MainMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player) {
        ChestMenu m = new ChestMenu("CustomBlocks Dashboard", 6).fill();

        // Glass-pane frame: accent top and bottom rows for a polished look.
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 45; i < 54; i++) m.set(i, Icons.accent());

        // Header.
        m.set(4, Icons.glint(Items.NETHER_STAR, "§b§lCustomBlocks Dashboard",
                "§7Manage every custom block on the server.",
                "§7Blocks: §f" + SlotManager.usedSlots() + " §7/ §f" + SlotManager.getMaxSlots()));

        // Row 1 — content & data features.
        // Block List + Bulk Operations both leave the chest for the Bulk Workbench Screen (§G07-3).
        m.set(19, Icons.of(Items.CHEST, "§a§lBlock List", "§7Browse and edit every block"),
                (p, b, a) -> { GuiFx.open(p); BulkSnapshot.openFromChest(p, BulkSnapshot.TAB_BROWSE); });
        m.set(20, Icons.of(Items.BOOKSHELF, "§eCategories", "§7Browse blocks by category"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.CATEGORY_LIST)));
        m.set(21, Icons.of(Items.PAPER, "§eTemplates", "§7Show /cb template list"),
                (p, b, a) -> GuiRouter.runCommand(p, "template list"));
        m.set(22, Icons.of(Items.REPEATER, "§eMacros", "§7Show /cb macro list"),
                (p, b, a) -> GuiRouter.runCommand(p, "macro list"));
        m.set(23, Icons.of(Items.BOOK, "§eArabic", "§7Show /cb arabic list"),
                (p, b, a) -> GuiRouter.runCommand(p, "arabic list"));
        m.set(24, Icons.of(Items.COMPARATOR, "§eDiagnostics", "§7Live system snapshot"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.DIAG)));
        m.set(25, Icons.of(Items.ENCHANTED_BOOK, "§d§lMagic Items", "§7Special admin tools"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.MAGIC)));

        // Row 2 — history & config.
        m.set(28, Icons.of(Items.RED_DYE, "§cUndo history", "§7Browse and undo recent edits"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.UNDO)));
        m.set(29, Icons.of(Items.LIME_DYE, "§aRedo history", "§7Browse and redo recent edits"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.REDO)));
        m.set(30, Icons.of(Items.CLOCK, "§eEdit history", "§7Who changed what, and when"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.HISTORY)));
        m.set(31, Icons.of(Items.COMMAND_BLOCK, "§5§lBulk Operations", "§7Edit, delete, rename, move,",
                        "§7lock or favorite many blocks at once"),
                (p, b, a) -> { GuiFx.open(p); BulkSnapshot.openFromChest(p, BulkSnapshot.TAB_BULK); });
        m.set(32, Icons.of(Items.BRUSH, "§b§lColoring", "§7Backgrounds, palettes, gradients,",
                        "§7variants — every colour tool in one place"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.COLORS)));
        m.set(34, Icons.of(Items.LEVER, "§6§lConfig", "§7Open the server config",
                        "§8Live server settings — asks to confirm first"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.CONFIG_CONFIRM)));

        // Bottom row — onboarding (Group 23): rotating tip + achievements text view.
        m.set(48, Icons.of(Items.LANTERN, "§e§lTip",
                "§7" + TipPool.next(player.getUuid()),
                "§8A new tip each time you open this."));
        m.set(50, Icons.of(Items.GOLD_INGOT, "§6§lAchievements",
                        "§7" + AchievementManager.progressLine(player.getUuid()),
                        "§7Click to see your list."),
                (p, b, a) -> GuiRouter.runCommand(p, "achievements"));

        m.set(49, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }
}
