/**
 * UndoMenu.java — visual undo/redo history (Group 02, G02.4 / G02.5). Lists the player's
 * undo or redo stack newest-first; clicking the Nth entry rolls the state back (or forward)
 * to that point by repeatedly delegating to HistoryCommands.undoOnce / redoOnce.
 */
package com.customblocks.gui.chest;

import com.customblocks.command.handlers.HistoryCommands;
import com.customblocks.core.SlotData;
import com.customblocks.core.UndoManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public final class UndoMenu {

    private UndoMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, boolean redo, int page) {
        List<UndoManager.Op> ops = redo
                ? UndoManager.redoStack(player.getUuid())
                : UndoManager.undoStack(player.getUuid());

        int per = Layout.PER_PAGE;
        int maxPage = ops.isEmpty() ? 0 : (ops.size() - 1) / per;
        int p = Math.max(0, Math.min(page, maxPage));
        Dest dest = redo ? Dest.REDO : Dest.UNDO;

        ChestMenu m = new ChestMenu(redo ? "Redo history" : "Undo history", 6);
        if (ops.isEmpty()) {
            m.set(22, Icons.of(Items.PAPER, redo ? "§7Nothing to redo" : "§7Nothing to undo"));
        }

        int start = p * per;
        for (int i = 0; i < per; i++) {
            int gi = start + i;
            if (gi >= ops.size()) break;
            UndoManager.Op op = ops.get(gi);
            int times = gi + 1;
            String bid = blockId(op);
            String verb = redo ? "redo" : "undo";
            m.set(i, Icons.of(redo ? Items.LIME_DYE : Items.RED_DYE,
                            "§f" + friendlyAction(op.label()) + " §7- §f" + bid,
                            "§7Block: §f" + bid,
                            "§7Position: §f#" + times + " §7from the top",
                            "§eClick to " + verb + " §fthe last " + times + (times == 1 ? " change" : " changes"),
                            "§8(rolls the block state " + (redo ? "forward" : "back") + " to this point)"),
                    (pl, b, a) -> {
                        for (int t = 0; t < times; t++) {
                            boolean ok = redo ? HistoryCommands.redoOnce(pl) : HistoryCommands.undoOnce(pl);
                            if (!ok) break;
                        }
                        GuiRouter.repage(pl, MenuKey.of(dest));
                    });
        }

        Layout.pagedFooter(m, p, maxPage, dest, "", ops.size());
        return m;
    }

    private static String friendlyAction(String label) {
        if (label == null || label.isEmpty()) return "Edit";
        return switch (label.toLowerCase()) {
            case "setglow" -> "Changed glow";
            case "sethardness" -> "Changed hardness";
            case "setsound" -> "Changed sound";
            case "setcollision" -> "Changed collision";
            case "setcategory" -> "Changed category";
            case "rename" -> "Renamed";
            case "retexture" -> "Retextured";
            case "note" -> "Edited note";
            case "create" -> "Created block";
            case "delete" -> "Deleted block";
            case "give" -> "Gave block";
            default -> Character.toUpperCase(label.charAt(0)) + label.substring(1);
        };
    }

    private static String blockId(UndoManager.Op op) {
        SlotData d = op.after() != null ? op.after() : op.before();
        return d != null ? d.customId() : "?";
    }
}
