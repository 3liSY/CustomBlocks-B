/**
 * HistoryMenu.java — read-only audit log of block mutations (Group 02, G02.8). Reads
 * entries from MutationLog (newest-first), resolving the actor's display name at render
 * time, and paginates them. Purely informational; clicking an entry does nothing.
 */
package com.customblocks.gui.chest;

import com.customblocks.command.Chat;
import com.customblocks.core.MutationLog;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class HistoryMenu {

    private HistoryMenu() {} // static-only

    private static final SimpleDateFormat FMT = new SimpleDateFormat("MMM d, HH:mm");

    public static ChestMenu build(ServerPlayerEntity player, int page) {
        List<MutationLog.Entry> all = MutationLog.recent();

        int per = Layout.PER_PAGE;
        int maxPage = all.isEmpty() ? 0 : (all.size() - 1) / per;
        int p = Math.max(0, Math.min(page, maxPage));

        ChestMenu m = new ChestMenu("Edit history", 6);
        if (all.isEmpty()) {
            m.set(22, Icons.of(Items.PAPER, "§7No history yet"));
        }

        int start = p * per;
        for (int i = 0; i < per; i++) {
            int gi = start + i;
            if (gi >= all.size()) break;
            MutationLog.Entry e = all.get(gi);
            String bid = e.blockId();
            boolean exists = bid != null && !bid.isEmpty() && SlotManager.getById(bid) != null;
            m.set(i, Icons.of(Items.PAPER,
                            "§f" + friendlyAction(e.action()) + " §7- §f" + bid,
                            "§7Block: §f" + bid,
                            "§7By: §f" + name(player.getServer(), e.actor()),
                            "§7When: §f" + FMT.format(new Date(e.time())),
                            exists ? "§eClick to open this block's editor" : "§8(block no longer exists)"),
                    (pl, b, a) -> {
                        if (SlotManager.getById(bid) != null) {
                            GuiRouter.navigate(pl, MenuKey.of(Dest.EDITOR, bid));
                        } else {
                            pl.sendMessage(Text.literal(Chat.PREFIX + "§7Block §f" + bid + " §7no longer exists."), true);
                        }
                    });
        }

        Layout.pagedFooter(m, p, maxPage, Dest.HISTORY, "", all.size());
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

    private static String name(MinecraftServer server, String actor) {
        if (actor == null || actor.equals("console")) return "Console";
        try {
            UUID u = UUID.fromString(actor);
            if (server != null) {
                ServerPlayerEntity sp = server.getPlayerManager().getPlayer(u);
                if (sp != null) return sp.getName().getString();
            }
            return actor.substring(0, 8);
        } catch (IllegalArgumentException ex) {
            return actor;
        }
    }
}
