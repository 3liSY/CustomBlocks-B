/**
 * TrashEntryMenu.java — the per-entry actions for a deleted block (Group 09, Slice 4).
 *
 * Two states, chosen by the MenuKey arg:
 *   "&lt;entryId&gt;"        — view: Restore · Pin/Unpin · Delete forever · Back.
 *   "purge:&lt;entryId&gt;"  — a Yes/No confirm for the permanent delete.
 *
 * Restore hands off to TrashCommands.guiRestore (recreates the block + rebuilds the pack). Pin and the
 * permanent delete are pure data ops against TrashManager. The confirm swaps in place via repage, so
 * the nav stack stays one level deep and Yes → back returns straight to the trash list.
 *
 * Depends on: ChestMenu, Icons, GuiRouter, GuiFx, TrashManager, TrashCommands, Chat.
 * Called by:  GuiRouter.build (Dest.TRASH_ENTRY).
 */
package com.customblocks.gui.chest;

import com.customblocks.command.Chat;
import com.customblocks.command.handlers.TrashCommands;
import com.customblocks.core.TrashManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class TrashEntryMenu {

    private TrashEntryMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, String arg) {
        String a = arg == null ? "" : arg;
        if (a.startsWith("purge:")) return purgeConfirm(a.substring("purge:".length()));
        return view(a);
    }

    // ── View: restore / pin / delete ──────────────────────────────────────────
    private static ChestMenu view(String entryId) {
        TrashManager.TrashEntry e = TrashManager.get(entryId);
        ChestMenu m = frame(e == null ? "Deleted block" : "Deleted: " + e.customId());

        if (e == null) {
            m.set(13, Icons.of(Items.BARRIER, "§c§lGone",
                    "§7This entry is no longer in the trash."));
            m.set(26, Icons.back(), (p, b, a) -> GuiRouter.back(p));
            return m;
        }

        String when = e.deletedHuman().isEmpty() ? "?" : e.deletedHuman();
        m.set(13, Icons.of(Items.PAPER, "§f" + e.customId(),
                "§7name: §f" + e.displayName(),
                "§7deleted: §f" + when,
                "§7category: §f" + (e.category().isEmpty() ? "none" : e.category()),
                "§7texture: " + (e.hasTexture() ? "§asaved" : "§8none"),
                e.pinned() ? "§bPinned §8— never auto-pruned" : "§8Not pinned"));

        // Restore.
        m.set(10, Icons.glint(Items.LIME_DYE, "§a§lRestore this block",
                        "§7Recreate §f" + e.customId() + "§7 with its texture",
                        "§7and settings, then remove it from the trash.",
                        "§8Fails if a block with that id already exists."),
                (p, b, a) -> { GuiFx.apply(p); TrashCommands.guiRestore(p, e.entryId()); });

        // Pin / unpin.
        m.set(12, Icons.of(e.pinned() ? Items.AMETHYST_SHARD : Items.AMETHYST_CLUSTER,
                        e.pinned() ? "§b§lUnpin" : "§b§lPin",
                        e.pinned() ? "§7Allow this entry to be auto-pruned again."
                                   : "§7Keep this entry forever (never auto-pruned)."),
                (p, b, a) -> {
                    TrashManager.setPinned(e.entryId(), !e.pinned());
                    GuiFx.select(p);
                    GuiRouter.repage(p, MenuKey.of(Dest.TRASH_ENTRY, e.entryId()));
                });

        // Delete permanently (→ confirm).
        m.set(14, Icons.glint(Items.TNT, "§c§lDelete forever",
                        "§7Permanently remove this from the trash.",
                        "§8Can't be undone — the block can't be restored after."),
                (p, b, a) -> { GuiFx.danger(p);
                        GuiRouter.repage(p, MenuKey.of(Dest.TRASH_ENTRY, "purge:" + e.entryId())); });

        m.set(18, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    // ── Confirm: delete forever ───────────────────────────────────────────────
    private static ChestMenu purgeConfirm(String entryId) {
        TrashManager.TrashEntry e = TrashManager.get(entryId);
        ChestMenu m = frame("Delete forever?");

        if (e == null) {
            m.set(13, Icons.of(Items.BARRIER, "§c§lGone", "§7This entry is no longer in the trash."));
            m.set(26, Icons.back(), (p, b, a) -> GuiRouter.back(p));
            return m;
        }

        m.set(13, Icons.of(Items.TNT, "§c§lDelete \"" + e.customId() + "\" forever?",
                "§7Removes it from the trash permanently.",
                "§8You won't be able to restore the block after this."));

        m.set(11, Icons.glint(Items.RED_DYE, "§c§lYes — delete forever",
                        "§7Permanently remove this entry."),
                (p, b, a) -> {
                    boolean ok = TrashManager.purge(e.entryId());
                    GuiFx.danger(p);
                    Chat.success(p.getCommandSource(), ok
                            ? "Permanently deleted \"" + e.customId() + "\" from the trash."
                            : "That entry was already gone.");
                    GuiRouter.back(p); // pop the entry screen → refreshed trash list
                });

        m.set(15, Icons.of(Items.LIME_DYE, "§a§lNo — keep it",
                        "§7Leave it in the trash."),
                (p, b, a) -> { GuiFx.click(p);
                        GuiRouter.repage(p, MenuKey.of(Dest.TRASH_ENTRY, e.entryId())); });

        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    /** A 3-row screen with light-blue accent top/bottom rows, filled centre. */
    private static ChestMenu frame(String title) {
        ChestMenu m = new ChestMenu(title, 3).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 18; i < 27; i++) m.set(i, Icons.accent());
        return m;
    }
}
