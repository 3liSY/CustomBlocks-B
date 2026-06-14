/**
 * BrokenConfirmMenu.java — the Yes/No confirm for bulk-deleting ticked broken blocks (Group 09, Slice 5).
 *
 * MenuKey arg "deletesel" → "Delete N broken blocks?" The deleted blocks go through the tested
 * SlotManager.delete, so each is captured into the trash and can still be restored. Yes deletes them all,
 * clears the selection and returns to the broken-blocks report; No backs out.
 *
 * Depends on: ChestMenu, Icons, GuiRouter, GuiFx, BrokenSelection, SlotManager, Chat.
 * Called by:  GuiRouter.build (Dest.BROKEN_CONFIRM).
 */
package com.customblocks.gui.chest;

import com.customblocks.command.Chat;
import com.customblocks.core.SlotManager;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public final class BrokenConfirmMenu {

    private BrokenConfirmMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, String arg) {
        int n = BrokenSelection.size(player.getUuid());
        ChestMenu m = new ChestMenu("Delete " + n + " broken block(s)?", 3).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 18; i < 27; i++) m.set(i, Icons.accent());

        if (n == 0) {
            m.set(13, Icons.of(Items.PAPER, "§7Nothing selected", "§8Tick some blocks first, then try again."));
            m.set(26, Icons.back(), (p, b, a) -> GuiRouter.back(p));
            return m;
        }

        m.set(13, Icons.of(Items.BOOK, "§c§lDelete " + n + " broken block(s)?",
                "§7Removes the ticked blocks.",
                "§aThey go to the trash §7— you can still",
                "§arestore them from /cb deletedblocks.",
                "§8Their slots are freed for new blocks."));

        m.set(11, Icons.glint(Items.TNT, "§c§lYes — delete " + n,
                        "§7Send the ticked blocks to the trash."),
                (p, b, a) -> doDeleteSelected(p));

        m.set(15, Icons.of(Items.LIME_DYE, "§a§lNo — keep them",
                        "§7Don't delete anything."),
                (p, b, a) -> { GuiFx.click(p); GuiRouter.back(p); });

        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    private static void doDeleteSelected(ServerPlayerEntity player) {
        GuiFx.danger(player);
        List<String> ids = new ArrayList<>(BrokenSelection.ids(player.getUuid()));
        int ok = 0;
        for (String id : ids) if (SlotManager.delete(id) != null) ok++;
        BrokenSelection.clear(player.getUuid());
        Chat.success(player.getCommandSource(), "Deleted " + ok + " block(s) — find them in /cb deletedblocks.");
        GuiRouter.back(player); // back to the broken-blocks report, which rebuilds without them
    }
}
