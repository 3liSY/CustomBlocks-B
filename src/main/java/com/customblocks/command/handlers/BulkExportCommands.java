/**
 * BulkExportCommands.java
 *
 * Bulk export (Group 07): write just the matched subset of blocks to a timestamped JSON/TXT file
 * in config/customblocks/exports/, the filtered counterpart of /cb export json|txt (which always
 * dumps everything). Read-only — no mutation, no undo, no confirm guard. Reuses BlockExporter.
 *
 *   /cb bulkexport <filter> [json|txt|csv|md|html|yaml|png]   /cb bulkexport  (no args → Bulk Workbench)
 *
 * Default format is json; png writes each matched block's baked texture image. Kept in its own
 * handler (own domain; keeps BulkCommands under the gate).
 *
 * Depends on: BulkScope, BlockExporter, Chat
 * Called by:  CommandRegistrar, BulkApply (the Screen calls writeExport directly)
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.BlockExporter;
import com.customblocks.core.BulkScope;
import com.customblocks.core.SlotData;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class BulkExportCommands {

    private BulkExportCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("bulkexport")
                .executes(ctx -> BulkCommands.openOp(ctx.getSource(), "export"))
                .then(CommandManager.argument("args", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.EXPORT_ARGS)
                        .executes(ctx -> bulkExport(ctx.getSource(), StringArgumentType.getString(ctx, "args")))));
    }

    private static int bulkExport(ServerCommandSource src, String args) {
        String[] parts = args.trim().split("\\s+");
        String format = "json";
        String scope = args.trim();
        // If the last token is a format, peel it off; otherwise the whole thing is the filter.
        if (parts.length >= 2) {
            String last = parts[parts.length - 1].toLowerCase(Locale.ROOT);
            if (isExportFormat(last)) {
                format = last;
                scope = String.join(" ", Arrays.copyOf(parts, parts.length - 1));
            }
        }

        List<SlotData> blocks = BulkScope.resolve(scope, BulkConfirm.actor(src));
        if (blocks.isEmpty()) { Chat.error(src, "No blocks matched: " + scope); return 0; }
        return writeExport(src, blocks, format);
    }

    /** Write the resolved batch out. Shared by the chat command and the Workbench (via BulkApply). */
    static int writeExport(ServerCommandSource src, List<SlotData> blocks, String format) {
        if (format.equals("png")) {
            BlockExporter.PngBatch r = BlockExporter.exportAllPng(blocks);
            if (r == null) { BulkResult.record(src, "Export failed — write error."); Chat.error(src, "Export failed — write error."); return 0; }
            BulkResult.record(src, "Exported " + r.written() + " texture PNG(s)"
                    + (r.skipped() > 0 ? " · " + r.skipped() + " had no texture" : "")); // X3
            Chat.success(src, "Exported " + CbFmt.VALUE + r.written() + CbFmt.RESET + " texture PNG(s) → " + CbFmt.DIM + "exports/" + r.dir().getFileName()
                    + (r.skipped() > 0 ? " " + CbFmt.FAINT + "(" + r.skipped() + " had no texture)" : ""));
            return 1;
        }

        Path file = BlockExporter.exportAll(format, blocks);
        if (file == null) { BulkResult.record(src, "Export failed — write error."); Chat.error(src, "Export failed — write error."); return 0; }
        BulkResult.record(src, "Exported " + blocks.size() + " block(s) → exports/" + file.getFileName()); // X3
        Chat.success(src, "Exported " + CbFmt.VALUE + blocks.size() + CbFmt.RESET + " block(s) → " + CbFmt.DIM + "exports/"
                + file.getFileName().toString());
        return 1;
    }

    static boolean isExportFormat(String s) {
        return switch (s) {
            case "json", "txt", "csv", "md", "html", "yaml", "yml", "png" -> true;
            default -> false;
        };
    }
}
