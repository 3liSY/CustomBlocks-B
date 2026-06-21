/**
 * AuditMenu.java — chest GUI for /cb audit (Group 16, slice 3 polish). A paginated, read-only
 * view of the mutation log (newest first), reusing HistoryMenu.placeEntry so each entry renders
 * and clicks (→ editor) identically to the Edit-history menu. An optional player filter (the
 * arg) narrows it to one actor; when filtered, a "Show all" button clears it. Replaces the old
 * chat-text /cb audit output for players (console still gets text).
 */
package com.customblocks.gui.chest;

import com.customblocks.core.MutationLog;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AuditMenu {

    private AuditMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, String filter, int page) {
        MinecraftServer server = player.getServer();
        String f = (filter == null || filter.isBlank()) ? "" : filter;

        // Newest-first log, narrowed to one actor if a filter is set.
        List<MutationLog.Entry> all = MutationLog.recent();
        List<MutationLog.Entry> shown;
        if (f.isEmpty()) {
            shown = all;
        } else {
            shown = new ArrayList<>();
            for (MutationLog.Entry e : all) {
                if (name(server, e.actor()).equalsIgnoreCase(f)) shown.add(e);
            }
        }

        int per = Layout.PER_PAGE;
        int maxPage = shown.isEmpty() ? 0 : (shown.size() - 1) / per;
        int p = Math.max(0, Math.min(page, maxPage));

        String title = f.isEmpty() ? "Audit log" : "Audit · " + f;
        ChestMenu m = new ChestMenu(title, 6);
        if (shown.isEmpty()) {
            m.set(22, Icons.of(Items.PAPER, f.isEmpty() ? "§7No history yet"
                    : "§7No edits by §f" + f));
        }

        int start = p * per;
        for (int i = 0; i < per; i++) {
            int gi = start + i;
            if (gi >= shown.size()) break;
            HistoryMenu.placeEntry(m, i, player, shown.get(gi));
        }

        Layout.pagedFooter(m, p, maxPage, Dest.AUDIT, f, shown.size());
        if (!f.isEmpty()) {
            m.set(47, Icons.of(Items.NAME_TAG, "§eShow all edits", "§8Clear the §f" + f + " §8filter"),
                    (pl, b, a) -> GuiRouter.repage(pl, MenuKey.of(Dest.AUDIT)));
        }
        return m;
    }

    /** Resolve a stored actor (UUID string or "console") to a display name. */
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
