/**
 * ImportChat.java — how a folder-import run reads in chat (Group 12 §B rules 2, 3, 7, 11, 21).
 *
 * All the wording and every clickable line for `/cb importfolder` live here, so the command file stays
 * about behaviour and this one about voice. §B ships the CLICKABLE CHAT flow deliberately: a full preview
 * Screen is G27's, later and optional, so these lines ARE the interface.
 *
 * The preview (rule 2) states, before anything is created: every file, the id and display name it will
 * get, how many slots the run uses, how many are left after, and every problem marked. Each problem file
 * gets its own line carrying the three fixes the owner asked for — rename (anvil), delete the file,
 * ignore (rule 3). Leftovers past the slot cap get a retry line instead of vanishing (rule 7).
 *
 * Brand: one [CB] header per block of lines with the ✔ / ✖ glyph (G04-3), body rows unbranded beneath it.
 * Colours are named through {@link CbFmt}; chips are built by {@link Chat} so aqua stays "clickable".
 *
 * Depends on: Chat, CbFmt, ImportScan.Scan, ImportEntry, ImportReport, BlockExporter (folder label)
 * Called by:  ImportFolderCommands
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.CbPaths;
import com.customblocks.core.ImportEntry;
import com.customblocks.core.ImportReport;
import com.customblocks.core.ImportScan;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.List;

public final class ImportChat {

    private ImportChat() {} // static-only

    /** The folder shown to the owner so they know where to drop pictures — derived from the real path. */
    public static final String FOLDER_LABEL = CbPaths.label(CbPaths.IMPORT);

    /** Longest run of body rows printed before the rest is summarised, so a 300-file run can't flood chat. */
    private static final int MAX_ROWS = 12;

    /** Rule 15 — the folder did not exist, so the run made it and says where to put things. */
    public static void firstRun(ServerCommandSource src) {
        Chat.success(src, "Import folder created");
        Chat.raw(src, Text.literal(CbFmt.DIM + "Drop pictures into " + CbFmt.BODY + FOLDER_LABEL));
        Chat.raw(src, Text.literal(CbFmt.DIM + "Then run " + CbFmt.BODY + "/cb importfolder"
                + CbFmt.DIM + " again to see what it would make."));
    }

    /** The folder exists but holds nothing usable. */
    public static void empty(ServerCommandSource src) {
        Chat.info(src, "Nothing to import — the folder is empty.");
        Chat.raw(src, Text.literal(CbFmt.DIM + "Drop pictures into " + CbFmt.BODY + FOLDER_LABEL));
        Chat.raw(src, Text.literal(CbFmt.FAINT + "Pictures: png, jpg, jpeg, gif, bmp, webp, tiff. "
                + "A zip full of them works too."));
    }

    /**
     * The preview: what WOULD be created, what is wrong, and the two buttons that decide it. Nothing has
     * been created or moved at this point, and cancel leaves the folder exactly as it is (rule 2).
     */
    public static void preview(ServerCommandSource src, ImportScan.Scan scan) {
        int ready = scan.ready().size();
        Chat.success(src, ready == 0
                ? "Nothing is ready to import yet"
                : "Ready to import " + ready + (ready == 1 ? " picture" : " pictures"));
        if (scan.unpacked() > 0) {
            Chat.raw(src, Text.literal(CbFmt.DIM + "Unpacked " + CbFmt.BODY + scan.unpacked()
                    + CbFmt.DIM + " file(s) out of a zip."));
        }
        Chat.raw(src, Text.literal(CbFmt.DIM + "Slots: " + CbFmt.BODY + ready + CbFmt.DIM + " used by this run, "
                + CbFmt.BODY + scan.slotsLeftAfter() + CbFmt.DIM + " left after ("
                + scan.freeSlots() + " free now)."));

        int shown = 0;
        for (ImportEntry e : scan.ready()) {
            if (shown++ >= MAX_ROWS) break;
            Chat.raw(src, Text.literal(CbFmt.DIM + " - " + CbFmt.BODY + e.fileName()
                    + CbFmt.DIM + " -> " + CbFmt.VALUE + e.id()
                    + CbFmt.DIM + " (\"" + CbFmt.BODY + e.name() + CbFmt.DIM + "\")"));
        }
        if (ready > MAX_ROWS) {
            Chat.raw(src, Text.literal(CbFmt.FAINT + "   … and " + (ready - MAX_ROWS) + " more."));
        }

        problems(src, scan.problems());
        leftovers(src, scan.leftover());

        if (ready > 0) {
            Chat.raw(src, Text.literal(CbFmt.DIM + "Confirm to create them: ")
                    .append(Chat.runButton(CbFmt.CLICK + "[✔ Confirm]", "/cb importfolder confirm",
                            "Create the " + ready + " block(s) listed above"))
                    .append(Text.literal("  "))
                    .append(Chat.runButton(CbFmt.CLICK + "[✖ Cancel]", "/cb importfolder cancel",
                            "Change nothing — leave every file where it is")));
        } else if (!scan.problems().isEmpty()) {
            Chat.raw(src, Text.literal(CbFmt.DIM + "Fix or ignore a file above, then run "
                    + CbFmt.BODY + "/cb importfolder" + CbFmt.DIM + " again."));
        }
    }

    /** Rule 3 — one line per problem file, each carrying rename / delete / ignore for THAT file. */
    private static void problems(ServerCommandSource src, List<ImportEntry> problems) {
        if (problems.isEmpty()) return;
        Chat.raw(src, Text.literal(CbFmt.WARN + problems.size()
                + (problems.size() == 1 ? " file needs" : " files need") + " a decision:"));
        int shown = 0;
        for (ImportEntry e : problems) {
            if (shown++ >= MAX_ROWS) break;
            String file = e.fileName();
            MutableText line = Text.literal(CbFmt.DIM + " - " + CbFmt.BODY + file
                            + CbFmt.DIM + " — " + CbFmt.WARN + e.problem() + " ")
                    .append(Chat.runButton(CbFmt.CLICK + "[rename]", "/cb importfolder fix rename " + file,
                            "Type a different name for this file"))
                    .append(Text.literal(" "))
                    .append(Chat.runButton(CbFmt.CLICK + "[delete file]", "/cb importfolder fix delete " + file,
                            "Delete " + file + " from the import folder. No block is touched."))
                    .append(Text.literal(" "))
                    .append(Chat.runButton(CbFmt.CLICK + "[ignore]", "/cb importfolder fix ignore " + file,
                            "Leave the file alone and stop listing it this run"));
            Chat.raw(src, line);
        }
        if (problems.size() > MAX_ROWS) {
            Chat.raw(src, Text.literal(CbFmt.FAINT + "   … and " + (problems.size() - MAX_ROWS)
                    + " more with problems."));
        }
    }

    /** Rule 7 — the run filled every free slot; these did not fit and can be retried after freeing some. */
    private static void leftovers(ServerCommandSource src, List<ImportEntry> leftover) {
        if (leftover.isEmpty()) return;
        Chat.raw(src, Text.literal(CbFmt.WARN + leftover.size()
                + (leftover.size() == 1 ? " picture does not" : " pictures do not")
                + " fit in the free slots — they stay in the folder:"));
        int shown = 0;
        for (ImportEntry e : leftover) {
            if (shown++ >= MAX_ROWS) break;
            Chat.raw(src, Text.literal(CbFmt.DIM + " - " + CbFmt.BODY + e.fileName()));
        }
        if (leftover.size() > MAX_ROWS) {
            Chat.raw(src, Text.literal(CbFmt.FAINT + "   … and " + (leftover.size() - MAX_ROWS) + " more."));
        }
        Chat.raw(src, Text.literal(CbFmt.DIM + "Free some slots, then ")
                .append(Chat.runButton(CbFmt.CLICK + "[retry]", "/cb importfolder",
                        "Scan the folder again and import what is left")));
    }

    /**
     * The result of a finished run: what was created (each with a delete button, rule 10), what was
     * skipped and why, and what is still waiting. Blocks arrive uncategorised — said out loud so nobody
     * hunts for a category that was never set (rule 9).
     */
    public static void result(ServerCommandSource src, ImportReport report, boolean saved) {
        int made = report.made().size();
        if (made == 0) {
            Chat.error(src, "Nothing was imported");
        } else {
            Chat.success(src, "Imported " + made + (made == 1 ? " block" : " blocks"));
        }
        rows(src, report, false);
        if (made > 0) {
            Chat.raw(src, Text.literal(CbFmt.DIM + "They arrived with no category, like /cb create leaves a block. ")
                    .append(Chat.undoButton())
                    .append(Text.literal("  "))
                    .append(Chat.runButton(CbFmt.CLICK + "[⊙ This report]", "/cb importfolder last",
                            "Show this report again after chat scrolls away")));
            Chat.raw(src, Text.literal(CbFmt.FAINT + "Originals moved to " + FOLDER_LABEL + "done/ — nothing was deleted."));
        }
        // §C is explicit that a report which will not survive must SAY so, never be quietly forgotten.
        if (!saved && made > 0) {
            Chat.raw(src, Text.literal(CbFmt.WARN + "This report could not be written to disk, so it will "
                    + "be gone if the server restarts."));
        }
    }

    /**
     * The recalled twin of {@link #result} (§C) — the SAME report object, stated as history.
     *
     * It never claims a block still exists: liveness is read at this moment from {@link ImportReport},
     * so an undone run says it was undone (C5) and a partly-cleaned one says how much is left. The
     * per-block delete buttons stay live for whatever is still there (C3).
     */
    public static void recall(ServerCommandSource src, ImportReport report) {
        int made = report.made().size();
        int live = report.stillLive().size();
        String when = ago(report.whenMs());
        if (report.fullyReversed()) {
            Chat.info(src, "The last import " + when + " made " + made + (made == 1 ? " block" : " blocks")
                    + ", and none of them are here any more — that run has been undone.");
        } else if (live == made) {
            Chat.info(src, "The last import " + when + " made " + made + (made == 1 ? " block" : " blocks") + ":");
        } else {
            Chat.info(src, "The last import " + when + " made " + made + (made == 1 ? " block" : " blocks")
                    + ", " + live + " still here:");
        }
        rows(src, report, true);
    }

    /** Told plainly when the mod has no report to show at all — never a blank answer (C4). */
    public static void noRun(ServerCommandSource src) {
        Chat.info(src, "No folder import has been run yet, so there is no report to show.");
        Chat.raw(src, Text.literal(CbFmt.DIM + "Drop pictures into " + CbFmt.BODY + FOLDER_LABEL
                + CbFmt.DIM + " and run " + CbFmt.BODY + "/cb importfolder" + CbFmt.DIM + "."));
    }

    /** A rough "how long ago", so a recalled report is obviously history and not a fresh result. */
    private static String ago(long whenMs) {
        if (whenMs <= 0) return "(time unknown)";
        long mins = Math.max(0, (System.currentTimeMillis() - whenMs) / 60_000L);
        if (mins < 1)   return "just now";
        if (mins < 60)  return mins + (mins == 1 ? " minute ago" : " minutes ago");
        long hours = mins / 60;
        if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");
        long days = hours / 24;
        return days + (days == 1 ? " day ago" : " days ago");
    }

    /** The body of a result — created rows, then skipped, then leftover. */
    static void rows(ServerCommandSource src, ImportReport report, boolean history) {
        int shown = 0;
        for (ImportReport.Made m : report.made()) {
            if (shown++ >= MAX_ROWS) break;
            boolean live = report.stillLive().contains(m);
            MutableText line = Text.literal(CbFmt.DIM + " - " + CbFmt.VALUE + m.id()
                    + CbFmt.DIM + " (\"" + CbFmt.BODY + m.name() + CbFmt.DIM + "\") ");
            if (live) {
                line.append(Chat.runButton(CbFmt.CLICK + "[delete]", "/cb delete " + m.id(),
                        "Delete " + m.id() + " the normal way"));
            } else {
                line.append(Text.literal(CbFmt.FAINT + "— gone"));
            }
            Chat.raw(src, line);
        }
        if (report.made().size() > MAX_ROWS) {
            Chat.raw(src, Text.literal(CbFmt.FAINT + "   … and " + (report.made().size() - MAX_ROWS) + " more."));
        }

        if (!report.missed().isEmpty()) {
            Chat.raw(src, Text.literal(CbFmt.WARN + report.missed().size()
                    + (report.missed().size() == 1 ? " file was" : " files were") + " skipped:"));
            int s = 0;
            for (ImportReport.Missed m : report.missed()) {
                if (s++ >= MAX_ROWS) break;
                Chat.raw(src, Text.literal(CbFmt.DIM + " - " + CbFmt.BODY + m.file()
                        + CbFmt.DIM + " — " + CbFmt.WARN + m.reason()));
            }
            if (report.missed().size() > MAX_ROWS) {
                Chat.raw(src, Text.literal(CbFmt.FAINT + "   … and " + (report.missed().size() - MAX_ROWS) + " more."));
            }
        }
        if (!report.leftover().isEmpty()) {
            Chat.raw(src, Text.literal(CbFmt.WARN + report.leftover().size()
                    + " did not fit in the free slots and are still in the folder."));
            if (!history) {
                Chat.raw(src, Text.literal(CbFmt.DIM + "Free some slots, then ")
                        .append(Chat.runButton(CbFmt.CLICK + "[retry]", "/cb importfolder",
                                "Scan the folder again and import what is left")));
            }
        }
    }
}
