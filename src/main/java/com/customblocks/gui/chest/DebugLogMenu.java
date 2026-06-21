/**
 * DebugLogMenu.java — advanced chest viewer for the mod's own log lines (Group 16, Debug Log slice).
 * A read-only window onto {@code logs/latest.log} filtered to {@code [CustomBlocks]} lines, newest
 * first, so the owner never has to open a text editor. Beyond a plain list it adds:
 *   • a summary tile (row 1) with per-severity counts — at-a-glance "which ones are issues",
 *   • a severity filter cycled from the footer (All → Issues → Errors → Warnings → Info),
 *   • click-to-copy: left-clicking any line posts a [Copy] button to chat (vanilla
 *     COPY_TO_CLIPBOARD — a chest click itself can't reach the clipboard),
 *   • a footer "Copy filtered" button that posts a one-click copy of every shown line,
 *   • issue lines (ERROR/WARN) carry an enchant glint so they pop out of the grid.
 * Layout: row 1 = summary + frame, rows 2-5 = log lines (36/page), row 6 = footer.
 *
 * Depends on: DebugLog (reader/filter/counts), Chat (copy buttons), Layout (footer), Icons
 * Called by:  GuiRouter (Dest.DEBUG_LOG)
 */
package com.customblocks.gui.chest;

import com.customblocks.command.Chat;
import com.customblocks.core.DebugLog;
import com.customblocks.core.DebugLog.Counts;
import com.customblocks.core.DebugLog.Filter;
import com.customblocks.core.DebugLog.Level;
import com.customblocks.core.DebugLog.Line;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class DebugLogMenu {

    private DebugLogMenu() {} // static-only

    /** Log lines per page — rows 2-5 (slots 9..44); row 1 is the summary, row 6 the footer. */
    private static final int PER = 36;
    private static final int CONTENT_START = 9;

    public static ChestMenu build(ServerPlayerEntity player, String filterArg, int page) {
        List<Line> all = DebugLog.recent();
        Counts counts = DebugLog.counts(all);
        Filter filter = Filter.of(filterArg);

        List<Line> shown = new ArrayList<>();
        for (Line l : all) if (filter.accepts(l.level())) shown.add(l);

        int maxPage = shown.isEmpty() ? 0 : (shown.size() - 1) / PER;
        int p = Math.max(0, Math.min(page, maxPage));

        ChestMenu m = new ChestMenu("Debug Log · " + filter.label, 6);
        header(m, counts, filter, shown.size());

        if (all.isEmpty()) {
            m.set(22, Icons.of(Items.PAPER, "§7No [CustomBlocks] log lines",
                    "§8Nothing logged this session yet,",
                    "§8or logs/latest.log isn't readable."));
        } else if (shown.isEmpty()) {
            m.set(22, Icons.of(Items.PAPER, "§7No lines match: §f" + filter.label,
                    "§8" + counts.total() + " line(s) logged, none of this severity.",
                    "§8Use the filter button to change it."));
        }

        int start = p * PER;
        for (int i = 0; i < PER; i++) {
            int gi = start + i;
            if (gi >= shown.size()) break;
            placeLine(m, CONTENT_START + i, shown.get(gi));
        }

        // Footer (45..53): back / pagination / close — arg carries the filter so paging keeps it.
        Layout.pagedFooter(m, p, maxPage, Dest.DEBUG_LOG, filter.token, shown.size());
        // Slot 46 — cycle the severity filter.
        Filter nxt = filter.next();
        m.set(46, Icons.of(Items.HOPPER, "§e§lFilter: §f" + filter.label,
                        "§7Showing §f" + shown.size() + "§7 of §f" + counts.total() + "§7 line(s).",
                        " ",
                        "§eClick → " + nxt.label,
                        "§8Cycle: All · Issues · Errors · Warnings · Info"),
                (pl, b, a) -> GuiRouter.repage(pl, MenuKey.of(Dest.DEBUG_LOG, nxt.token)));
        // Slot 47 — copy every shown line to clipboard (via a chat button; chest clicks can't copy).
        if (!shown.isEmpty()) {
            List<Line> snapshot = shown;
            m.set(47, Icons.of(Items.WRITABLE_BOOK, "§b§lCopy filtered",
                            "§7Post a one-click copy of all §f" + snapshot.size(),
                            "§7shown line(s) to your chat.",
                            " ",
                            "§eClick → copy button in chat"),
                    (pl, b, a) -> postCopyAll(pl, snapshot));
        }
        return m;
    }

    /** Row 1: a frame plus the summary tile (counts + active filter), glinting if any errors. */
    private static void header(ChestMenu m, Counts c, Filter filter, int shownCount) {
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        String name = "§e§lLog Summary §8(" + filter.label + ")";
        String[] lore = {
                "§c● §f" + c.error() + " §7error(s)",
                "§e● §f" + c.warn() + " §7warning(s)",
                "§7● §f" + c.info() + " §7info",
                " ",
                "§8" + shownCount + " shown · " + c.total() + " total this session",
                " ",
                "§eLeft-click → " + (problemFilter(filter) ? "show all" : "jump to issues")
        };
        Item icon = c.error() > 0 ? Items.REDSTONE_TORCH : c.issues() > 0 ? Items.TORCH : Items.BOOK;
        var stack = c.issues() > 0 ? Icons.glint(icon, name, lore) : Icons.of(icon, name, lore);
        // Quick toggle: from a "problem" view go back to All; otherwise jump straight to Issues.
        Filter target = problemFilter(filter) ? Filter.ALL : Filter.ISSUES;
        m.set(4, stack, (pl, b, a) -> GuiRouter.repage(pl, MenuKey.of(Dest.DEBUG_LOG, target.token)));
    }

    private static boolean problemFilter(Filter f) {
        return f == Filter.ISSUES || f == Filter.ERRORS || f == Filter.WARNS;
    }

    private static void placeLine(ChestMenu m, int slot, Line line) {
        String col = colorOf(line.level());
        String title = col + clip(line.message(), 48);
        List<String> lore = new ArrayList<>();
        lore.add("§8" + line.level());
        lore.add(" ");
        for (String w : wrap(line.raw(), 50)) lore.add("§7" + w);
        lore.add(" ");
        lore.add("§eLeft-click → copy button in chat");
        var stack = line.level() == Level.INFO
                ? Icons.of(iconFor(line.level()), title, lore.toArray(new String[0]))
                : Icons.glint(iconFor(line.level()), title, lore.toArray(new String[0]));
        m.set(slot, stack, (pl, b, a) -> postCopyLine(pl, line));
    }

    /** Post a single line to chat with a one-click [Copy] (chest clicks can't reach the clipboard). */
    private static void postCopyLine(ServerPlayerEntity pl, Line line) {
        MutableText msg = Text.literal(Chat.PREFIX + colorOf(line.level()) + clip(line.message(), 50) + "  ")
                .append(Chat.copyButton("§b§l[Copy]", line.raw(),
                        "§7Click to copy the full raw line:\n§f" + line.raw()));
        pl.sendMessage(msg, false);
    }

    /** Post a [Copy N lines] button that copies the whole shown set, newest first, one per row. */
    private static void postCopyAll(ServerPlayerEntity pl, List<Line> lines) {
        StringBuilder sb = new StringBuilder();
        for (Line l : lines) sb.append(l.raw()).append('\n');
        MutableText msg = Text.literal(Chat.PREFIX + "§7Debug log (" + lines.size() + " line(s)): ")
                .append(Chat.copyButton("§b§l[Copy " + lines.size() + " lines]", sb.toString(),
                        "§7Click to copy all " + lines.size() + " shown line(s)."));
        pl.sendMessage(msg, false);
    }

    private static String colorOf(Level lv) {
        return switch (lv) { case ERROR -> "§c"; case WARN -> "§e"; case INFO -> "§7"; };
    }

    private static Item iconFor(Level lv) {
        return switch (lv) {
            case ERROR -> Items.RED_WOOL;
            case WARN -> Items.YELLOW_WOOL;
            case INFO -> Items.PAPER;
        };
    }

    private static String clip(String s, int max) {
        if (s == null) return "?";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /** Hard-wrap a long line into <=width chunks for tooltip lore (vanilla won't wrap it for us). */
    private static List<String> wrap(String s, int width) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isBlank()) { out.add(""); return out; }
        for (int i = 0; i < s.length(); i += width) {
            out.add(s.substring(i, Math.min(s.length(), i + width)));
        }
        return out;
    }
}
