/**
 * BulkConfirmMenu.java — the shared Yes/No confirmation for EVERY bulk op (Group 07 / Area 4).
 *
 * Replaces the old in-chat "[✔ Confirm]" line: a premium, op-coloured screen that states exactly
 * what will happen, on how many blocks (with a sample id list), then Yes runs it and No goes back.
 * Reached from Step 2 (BulkActionMenu) and from the pre-picked fast path (BulkHubMenu → here for
 * no-option ops). The scope is the Step-1 / pre-picked selection; "Yes" calls the tested
 * apply*FromGui paths in force mode (the GUI IS the confirm, so the chat threshold is skipped),
 * then clears the picked set.
 *
 * Depends on: BulkSession, BulkStyle, BulkScope, Icons, GuiFx, GuiRouter/Nav, ListSelection,
 *             Bulk*Commands (apply*FromGui)
 * Called by:  GuiRouter (Dest.BULK_CONFIRM)
 */
package com.customblocks.gui.chest;

import com.customblocks.command.handlers.BulkCategoryCommands;
import com.customblocks.command.handlers.BulkCommands;
import com.customblocks.command.handlers.BulkDuplicateCommands;
import com.customblocks.command.handlers.BulkExportCommands;
import com.customblocks.command.handlers.BulkFlagCommands;
import com.customblocks.core.BulkScope;
import com.customblocks.core.SlotData;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BulkConfirmMenu {

    private BulkConfirmMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        BulkSession s = BulkSession.get(uuid);
        String op = s.op;
        String scope = s.selScopeExpr(uuid);
        List<SlotData> matches = scope.isBlank() ? List.of() : BulkScope.resolve(scope, uuid);
        int count = matches.size();

        ChestMenu m = new ChestMenu(BulkStyle.titleColor(op) + "Confirm " + BulkSession.prettyOp(op), 4).fill()
                .frame(Icons.of(BulkStyle.framePane(op), " "), Icons.of(BulkStyle.frameCorner(op), " "));

        if (count == 0) {
            m.set(4, Icons.of(Items.BARRIER, "§cNothing to do",
                    "§7That selection matches no blocks.", "§7Go back and pick some."));
            m.set(31, Icons.back(), (p, b, a) -> GuiRouter.back(p));
            return m;
        }

        // Header — what's about to happen + the exact count, with a sample id list.
        List<String> lore = new ArrayList<>();
        lore.add(summary(s, op, count));
        lore.add("");
        int shown = Math.min(8, count);
        for (int i = 0; i < shown; i++) lore.add("§8• §f" + matches.get(i).customId());
        if (count > shown) lore.add("§8…and " + (count - shown) + " more");
        lore.add("");
        lore.add(noteFor(op));
        m.set(4, Icons.glint(BulkStyle.opIcon(op), BulkStyle.headerColor(op) + "§l"
                + BulkSession.prettyOp(op) + " — confirm?", lore.toArray(new String[0])));

        boolean delete = "delete".equals(op);
        m.set(29, Icons.of(Items.RED_CONCRETE, "§c§lNo, go back", "§7Cancel — nothing changes."),
                (p, b, a) -> { GuiFx.click(p); GuiRouter.back(p); });
        m.set(33, Icons.glint(delete ? Items.TNT : Items.LIME_CONCRETE,
                        (delete ? "§4§l⚠ Yes, DELETE " : "§a§l✔ Yes, " + verb(op) + " ") + count + " block(s)",
                        "§7" + summary(s, op, count), "§eClick §7to run it"),
                (p, b, a) -> run(p, op));

        m.set(31, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    /** Run the chosen op on the current scope via the tested force paths, then clear the picks. */
    private static void run(ServerPlayerEntity p, String op) {
        BulkSession ss = BulkSession.get(p.getUuid());
        String scope = ss.selScopeExpr(p.getUuid());            // a literal id list / filter — captured now
        if (scope.isBlank()) { GuiFx.deny(p); return; }
        switch (op) {
            case "delete"    -> { GuiFx.danger(p); BulkCommands.applyDeleteFromGui(p, scope); }
            case "rename"    -> { GuiFx.apply(p);  BulkCommands.applyRenameFromGui(p, scope, ss.renameMode, ss.renameA, ss.renameB); }
            case "category"  -> { GuiFx.apply(p);  BulkCategoryCommands.applyCategoryFromGui(p, scope, ss.category); }
            case "duplicate" -> { GuiFx.apply(p);  BulkDuplicateCommands.applyDuplicateFromGui(p, scope); }
            case "export"    -> { GuiFx.apply(p);  BulkExportCommands.applyExportFromGui(p, scope, ss.exportFormat); }
            case "lock", "favorite" -> { GuiFx.apply(p); BulkFlagCommands.applyFlagFromGui(p, ss.flagCommandOp(), scope); }
            default          -> { GuiFx.apply(p);  BulkCommands.applyFromGui(p, scope, ss.property, ss.value); }
        }
        // The op ran on a captured scope string; clear the hand-picked set + selection state.
        ListSelection.clear(p.getUuid());
        ss.resetSelection();
    }

    private static String summary(BulkSession s, String op, int count) {
        return switch (op) {
            case "delete"    -> "§cPermanently delete §e" + count + " §cblock(s).";
            case "rename"    -> "§7" + renameDesc(s) + " on §e" + count + " §7block(s).";
            case "category"  -> "§7" + ("none".equalsIgnoreCase(s.category) || s.category.isEmpty()
                                    ? "Clear the category of" : "Move to §b" + s.category + "§7,")
                                    + " §e" + count + " §7block(s).";
            case "duplicate" -> "§7Make a copy of §e" + count + " §7block(s).";
            case "export"    -> "§7Export §e" + count + " §7block(s) as §6." + s.exportFormat + "§7.";
            case "lock", "favorite" -> "§7" + s.flagCommandOp() + " §e" + count + " §7block(s).";
            default          -> "§7Set §b" + s.property + "§7=§b" + s.value + "§7 on §e" + count + " §7block(s).";
        };
    }

    private static String noteFor(String op) {
        return switch (op) {
            case "delete"    -> "§8Locked blocks are skipped. §8/cb undo restores them.";
            case "duplicate" -> "§8Copies are named <id>_copy. §8/cb undo removes them.";
            case "export"    -> "§8Read-only — saved in config/customblocks/exports/.";
            case "lock", "favorite" -> "§8Self-inverse — the opposite undoes it.";
            default          -> "§8One /cb undo reverts the whole batch.";
        };
    }

    private static String verb(String op) {
        return switch (op) {
            case "rename"    -> "Rename";
            case "category"  -> "Move";
            case "duplicate" -> "Duplicate";
            case "export"    -> "Export";
            case "lock"      -> "Lock";
            case "favorite"  -> "Favorite";
            default          -> "Apply to";
        };
    }

    private static String renameDesc(BulkSession s) {
        return switch (s.renameMode) {
            case "prefix" -> "Add prefix \"" + s.renameA + "\"";
            case "suffix" -> "Add suffix \"" + s.renameA + "\"";
            default       -> "Replace \"" + s.renameA + "\" → \"" + s.renameB + "\"";
        };
    }
}
