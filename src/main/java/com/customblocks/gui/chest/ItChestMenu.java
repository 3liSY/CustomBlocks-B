/**
 * ItChestMenu.java — the IT Chest dashboard (Group 16, slice 1). One 6-row chest that
 * replaces the old 3-row DiagMenu:
 *   Row 1 (0–8)   — live system health (TPS, registry, network sync, pack, memory).
 *   Rows 2–4 (9–35) — incident log: one wool per incident, colour = severity, newest first.
 *   Row 5 (36–44) — the last 9 mutation-log entries (reuses HistoryMenu.placeEntry).
 *   Row 6 (45–53) — controls: Refresh, Clear Incidents, Back, Close.
 * Read-only: clicking an incident opens its block's editor (if it still exists) or prints the
 * full detail to chat. Auto-fix arrives in slice 2.
 */
package com.customblocks.gui.chest;

import com.customblocks.command.CbFmt;

import com.customblocks.command.Chat;
import com.customblocks.core.DiagnosticsHelper;
import com.customblocks.core.DiagnosticsHelper.Gauge;
import com.customblocks.core.DiagnosticsHelper.Health;
import com.customblocks.core.IncidentRecorder;
import com.customblocks.core.IncidentRecorder.Incident;
import com.customblocks.core.MutationLog;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public final class ItChestMenu {

    private ItChestMenu() {} // static-only

    private static final int INCIDENT_START = 9;   // rows 2–4
    private static final int INCIDENT_SLOTS = 27;
    private static final int MUTATION_START = 36;   // row 5
    private static final int MUTATION_SLOTS = 9;

    public static ChestMenu build(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        ChestMenu m = new ChestMenu("IT Chest", 6).fill();

        // Row 1 — live health, five gauges centred in slots 2–6.
        m.set(0, Icons.accent()); m.set(1, Icons.accent());
        m.set(7, Icons.accent()); m.set(8, Icons.accent());
        m.set(2, gauge(Items.LIME_STAINED_GLASS_PANE, "TPS", DiagnosticsHelper.tps(server), true));
        m.set(3, gauge(Items.COMPARATOR, "Block Registry", DiagnosticsHelper.registry(), false));
        m.set(4, gauge(Items.REDSTONE, "Network Sync", DiagnosticsHelper.networkSync(server), false));
        m.set(5, gauge(Items.BOOK, "Pack Status", DiagnosticsHelper.packStatus(), false));
        m.set(6, gauge(Items.BARREL, "Memory", DiagnosticsHelper.memory(), false));

        // Rows 2–4 — incidents (newest first; show the latest 27).
        List<Incident> incidents = IncidentRecorder.recent();
        if (incidents.isEmpty()) {
            m.set(22, Icons.of(Items.PAPER, "§7No incidents recorded",
                    "§8Runtime errors and warnings appear here."));
        } else {
            int n = Math.min(INCIDENT_SLOTS, incidents.size());
            for (int i = 0; i < n; i++) placeIncident(m, INCIDENT_START + i, incidents.get(i));
        }

        // Row 5 — last 9 mutations (reuse the history renderer).
        List<MutationLog.Entry> muts = MutationLog.recent();
        int mn = Math.min(MUTATION_SLOTS, muts.size());
        for (int i = 0; i < mn; i++) HistoryMenu.placeEntry(m, MUTATION_START + i, player, muts.get(i));
        if (muts.isEmpty()) m.set(40, Icons.of(Items.PAPER, "§7No mutations yet"));

        // Row 6 — controls.
        m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(47, Icons.of(Items.CLOCK, "§a§lRefresh", "§7Re-read live health in place"),
                (p, b, a) -> GuiRouter.repage(p, MenuKey.of(Dest.DIAG)));
        m.set(49, Icons.of(Items.LAVA_BUCKET, "§c§lClear Incidents",
                        "§7Clears the incident rows only.",
                        "§8Health + mutation log untouched."),
                (p, b, a) -> { IncidentRecorder.clear(); GuiRouter.repage(p, MenuKey.of(Dest.DIAG)); });
        m.set(48, Icons.of(Items.FIREWORK_ROCKET, "§d§lFeedback FX",
                        "§7Toggle the particle + sound cues the",
                        "§7mod plays on key events."),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.PARTICLES)));
        m.set(51, Icons.of(Items.WRITABLE_BOOK, "§b§lDiagnostic Report",
                        "§7Open the report screen — generate",
                        "§7+ get a [download] link."),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.REPORT)));
        m.set(50, Icons.of(Items.BOOKSHELF, "§e§lDebug Log",
                        "§7Browse this session's §f[CustomBlocks]",
                        "§7log lines in-game — newest first."),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.DEBUG_LOG)));
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    /** Build one health-gauge item. For TPS the pane itself is recoloured by status. */
    private static net.minecraft.item.ItemStack gauge(Item item, String label, Gauge g, boolean coloredPane) {
        Item shown = coloredPane ? paneFor(g.health()) : item;
        String[] lore = g.hover().stream().filter(s -> !s.isEmpty()).toArray(String[]::new);
        return Icons.of(shown, colorOf(g.health()) + "§l" + label, lore);
    }

    private static void placeIncident(ChestMenu m, int slot, Incident in) {
        String col = colorOf(in.severity());
        String time = in.time().length() >= 19 ? in.time().substring(0, 19).replace('T', ' ') : in.time();
        String block = (in.block() == null || in.block().isBlank()) ? "—" : in.block();
        boolean exists = !"—".equals(block) && SlotManager.getById(block) != null;
        // Slice 2 auto-fix: a failed texture download whose block still exists + has a known URL.
        boolean canFix = exists && in.url() != null && !in.url().isBlank();

        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("§7When: §f" + time);
        lore.add("§7By: §f" + in.player());
        lore.add("§7Block: §f" + block);
        if (in.error() != null) lore.add("§c" + in.error());
        if (canFix) lore.add("§8URL: " + clip(in.url()));
        lore.add(canFix ? "§aClick to re-download from the last URL"
                : exists ? "§eClick to open this block's editor"
                : "§7Click for full detail");

        m.set(slot, Icons.of(woolFor(in.severity()), col + clip(in.context()), lore.toArray(new String[0])),
                (p, b, a) -> {
                    if (canFix) {
                        GuiRouter.runCommand(p, "retexture " + block + " " + in.url());
                    } else if (exists) {
                        GuiRouter.navigate(p, MenuKey.of(Dest.EDITOR, block));
                    } else {
                        Chat.toPlayer(p, col + in.context());
                        Chat.toPlayer(p, CbFmt.DIM + time + CbFmt.FAINT + " · " + CbFmt.DIM + in.player()
                                + (in.block() == null ? "" : CbFmt.FAINT + " · " + CbFmt.DIM + in.block())
                                + (in.error() == null ? "" : CbFmt.FAINT + " · " + CbFmt.BAD + in.error()));
                    }
                });
    }

    private static String clip(String s) {
        if (s == null) return "?";
        return s.length() <= 40 ? s : s.substring(0, 39) + "…";
    }

    private static String colorOf(Health h) {
        return switch (h) { case GREEN -> "§a"; case YELLOW -> "§e"; case RED -> "§c"; };
    }

    private static String colorOf(IncidentRecorder.Severity s) {
        return switch (s) { case ERROR -> "§c"; case WARN -> "§e"; case INFO -> "§a"; };
    }

    private static Item paneFor(Health h) {
        return switch (h) {
            case GREEN -> Items.LIME_STAINED_GLASS_PANE;
            case YELLOW -> Items.YELLOW_STAINED_GLASS_PANE;
            case RED -> Items.RED_STAINED_GLASS_PANE;
        };
    }

    private static Item woolFor(IncidentRecorder.Severity s) {
        return switch (s) {
            case ERROR -> Items.RED_WOOL;
            case WARN -> Items.YELLOW_WOOL;
            case INFO -> Items.LIME_WOOL;
        };
    }
}
