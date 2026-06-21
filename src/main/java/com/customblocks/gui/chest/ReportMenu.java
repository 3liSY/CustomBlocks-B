/**
 * ReportMenu.java — chest GUI for /cb report (Group 16, slice 3 polish). A 3-row screen that
 * replaces the old chat-only flow: it explains what the diagnostic report contains, shows when
 * one was last written, and offers two buttons — "Generate Report" (writes it now and posts a
 * [download] link in chat) and, when a report already exists, "Get Download Link" (re-posts the
 * link without rewriting). Both delegate to the tested /cb report subcommands via GuiRouter.
 */
package com.customblocks.gui.chest;

import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class ReportMenu {

    private ReportMenu() {} // static-only

    private static final SimpleDateFormat FMT = new SimpleDateFormat("MMM d, HH:mm");
    private static final File REPORT = new File("config/customblocks/data/diag_report.txt");

    public static ChestMenu build(ServerPlayerEntity player) {
        ChestMenu m = new ChestMenu("Diagnostic Report", 3).fill();
        m.set(0, Icons.accent());
        m.set(8, Icons.accent());

        boolean exists = REPORT.exists();
        String last = exists
                ? "§7Last written: §f" + FMT.format(new Date(REPORT.lastModified()))
                        + " §8(" + bytes(REPORT.length()) + ")"
                : "§8No report generated yet.";

        m.set(4, Icons.of(Items.BOOK, "§b§lDiagnostic Report",
                "§7A full plain-text snapshot saved to disk:",
                "§8• server info   • health gauges",
                "§8• last 100 incidents   • last 50 edits",
                " ",
                last));

        m.set(11, Icons.of(Items.WRITABLE_BOOK, "§a§lGenerate Report",
                        "§7Write the report now and post a",
                        "§7§b[download] §7link in chat."),
                (p, b, a) -> GuiRouter.runCommand(p, "report generate"));

        if (exists) {
            m.set(15, Icons.of(Items.LIME_DYE, "§a§lGet Download Link",
                            "§7Re-post the §b[download] §7link for",
                            "§7the report already on disk."),
                    (p, b, a) -> GuiRouter.runCommand(p, "report link"));
        } else {
            m.set(15, Icons.of(Items.GRAY_DYE, "§8Get Download Link",
                    "§8Generate a report first."));
        }

        m.set(18, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    private static String bytes(long b) {
        if (b <= 0) return "0 B";
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return (b / 1024) + " KB";
        return String.format("%.1f MB", b / (1024.0 * 1024));
    }
}
