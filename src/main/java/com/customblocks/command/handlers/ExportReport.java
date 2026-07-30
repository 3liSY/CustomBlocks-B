/**
 * ExportReport.java — the ONE export result message (Group 12 §A, owner-locked 2026-07-30).
 *
 * Every export route used to answer differently: one said "12 block(s) → blocks-….json", another named
 * a folder it had not written to, none said how big the file was, and a couple reported success without
 * ever checking the file landed. The owner picked the fullest variant, so all of them now say the same
 * four things — HOW MANY blocks, WHICH file, HOW BIG, and WHICH folder:
 *
 *   [CB] Exported 12 blocks ✔
 *        File: all-20260730-142310.zip  (3.4 MB)
 *        Folder: config/customblocks/cloud_exports/
 *
 * Brand: the header is a normal branded [CB] ✔ / ✖ line via {@link Chat}; the two detail rows ride under
 * it unbranded, which is the house rule of one [CB] per block of lines (G04-3), not per line.
 *
 * A route that carries a clickable chip (the [download] link, which belongs to G20 §L and is untouched
 * here) passes it to {@link #wrote} and it rides the File row.
 *
 * Console parity (TG12 A8): every line here is plain text through Chat, so the server console gets the
 * identical report a player gets — no route tries to open a screen to say what it wrote.
 *
 * No route prints a server host or IP: this file only ever formats a file name and a relative folder.
 *
 * Depends on: Chat, CbFmt, BlockExporter (size + folder label)
 * Called by:  UtilityCommands, CategoryCommands, BulkExportCommands
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.BlockExporter;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.nio.file.Path;

public final class ExportReport {

    private ExportReport() {} // static-only

    /** The one "nothing exists yet" answer, so every route says it the same way. */
    public static int nothingToExport(ServerCommandSource src) {
        Chat.info(src, "Nothing to export yet — make a block with /cb create <id>");
        return 1;
    }

    /** The one write-failure answer. Returns 0 so the command reports failure to Brigadier. */
    public static int failed(ServerCommandSource src, String what) {
        Chat.error(src, "Export failed — couldn't write the " + what + ". Nothing was saved.");
        return 0;
    }

    /**
     * Report a written artifact: count + file + size + folder. {@code noun} is what was counted
     * ("block", "texture"), pluralised here. {@code chips} ride the File row — pass none for a plain
     * route, or the existing [download] chip for a route that has one.
     */
    public static int wrote(ServerCommandSource src, int count, String noun, Path file, MutableText... chips) {
        return headline(src, "Exported " + count + " " + noun + (count == 1 ? "" : "s"), file, chips);
    }

    /**
     * The single-block twin of {@link #wrote}: names the block instead of a count, because "Exported 1
     * block" hides which one when three routes can each save a different file for the same id.
     */
    public static int wroteOne(ServerCommandSource src, String id, Path file, MutableText... chips) {
        return headline(src, "Exported \"" + id + "\"", file, chips);
    }

    /**
     * Same report with a caller-written first line, for a route whose count needs more than a noun —
     * "Exported 12 blocks of Arabic Letters". The File and Folder rows are identical either way.
     */
    public static int headline(ServerCommandSource src, String headline, Path file, MutableText... chips) {
        Chat.success(src, headline);
        Chat.raw(src, fileRow(file, chips));
        Chat.raw(src, Text.literal(CbFmt.DIM + "Folder: " + CbFmt.BODY + BlockExporter.FOLDER_LABEL));
        return 1;
    }

    /** The File row: the artifact's name, its size in brackets, then any chips the route carries. */
    private static MutableText fileRow(Path file, MutableText... chips) {
        String name = file == null ? "(unknown)" : file.getFileName().toString();
        MutableText row = Text.literal(CbFmt.DIM + "File: " + CbFmt.BODY + name
                + "  " + CbFmt.FAINT + "(" + size(BlockExporter.sizeOf(file)) + ")");
        for (MutableText chip : chips) {
            if (chip != null) row.append(Text.literal("  ")).append(chip);
        }
        return row;
    }

    /**
     * A byte count the owner can read at a glance. Kept to one decimal from KB up, because "3.4 MB"
     * answers "did that actually contain my blocks?" and "3568194 bytes" does not.
     */
    public static String size(long bytes) {
        if (bytes <= 0) return "empty";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
