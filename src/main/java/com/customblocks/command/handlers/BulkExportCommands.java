/**
 * BulkExportCommands.java
 *
 * Bulk export (Group 07): write just the matched subset of blocks to a timestamped JSON/TXT file
 * in config/customblocks/exports/, the filtered counterpart of /cb export json|txt (which always
 * dumps everything). Read-only — no mutation, no undo, no confirm guard. Reuses BlockExporter.
 *
 *   /cb bulkexport <filter> [json|txt|csv|md|html|yaml|png]   /cb bulkexport  (no args → dashboard)
 *
 * Default format is json; png writes each matched block's baked texture image. Kept in its own
 * handler (own domain; keeps BulkCommands under the gate).
 *
 * Depends on: BulkScope, BlockExporter, Chat
 * Called by:  CommandRegistrar, BulkActionMenu (applyExportFromGui)
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.BlockExporter;
import com.customblocks.core.BulkScope;
import com.customblocks.core.SlotData;
import com.customblocks.gui.chest.BulkSession;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class BulkExportCommands {

    private BulkExportCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("bulkexport")
                .executes(ctx -> openBuilder(ctx.getSource()))
                .then(CommandManager.argument("args", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.EXPORT_ARGS)
                        .executes(ctx -> bulkExport(ctx.getSource(), StringArgumentType.getString(ctx, "args")))));
    }

    private static int openBuilder(ServerCommandSource src) {
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            BulkSession s = BulkSession.get(p.getUuid());
            s.op = "export";
            s.resetSelection(); // fresh open → "None selected currently"
            com.customblocks.gui.chest.GuiFx.open(p);
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.BULK_SELECT));
            return 1;
        }
        Chat.error(src, "Open the bulk export builder in-game with /cb bulkexport.");
        return 0;
    }

    /** Close the dashboard, then export the assembled filter in the chosen format. */
    public static void applyExportFromGui(ServerPlayerEntity player, String filter, String format) {
        net.minecraft.server.MinecraftServer s = player.getServer();
        if (s == null) return;
        s.execute(() -> {
            player.closeHandledScreen();
            bulkExport(player.getCommandSource(), (filter + " " + format).trim());
        });
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
        if (blocks.isEmpty()) { Chat.error(src, "No blocks matched filter: " + scope); return 0; }

        if (format.equals("png")) {
            BlockExporter.PngBatch r = BlockExporter.exportAllPng(blocks);
            if (r == null) { Chat.error(src, "Export failed — write error."); return 0; }
            Chat.success(src, "Exported §e" + r.written() + "§r texture PNG(s) → §7exports/" + r.dir().getFileName()
                    + (r.skipped() > 0 ? " §8(" + r.skipped() + " had no texture)" : ""));
            return 1;
        }

        Path file = BlockExporter.exportAll(format, blocks);
        if (file == null) { Chat.error(src, "Export failed — write error."); return 0; }
        Chat.success(src, "Exported §e" + blocks.size() + "§r block(s) → §7exports/"
                + file.getFileName().toString());
        return 1;
    }

    private static boolean isExportFormat(String s) {
        return switch (s) {
            case "json", "txt", "csv", "md", "html", "yaml", "yml", "png" -> true;
            default -> false;
        };
    }
}
