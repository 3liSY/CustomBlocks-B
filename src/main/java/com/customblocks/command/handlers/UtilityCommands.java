/**
 * UtilityCommands.java
 *
 * Responsibility: Non-mutating / utility subcommands — list, give, reload, export.
 * Registered into the /cb tree by CommandRegistrar. Stays under 400 lines (§9.3).
 *
 * `list` ends with clickable [.json] / [.txt] buttons that run `/cb export`, which writes the block
 * list into config/customblocks/cloud_exports/ — the one artifact folder (G12, 2026-07-30).
 *
 * Export is admin-only and every route answers through {@link ExportReport}, so all of them state the
 * same four facts: count, file name, size, folder. Console gets that identical plain text (TG12 A8).
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.BlockExporter;
import com.customblocks.core.DraftManager;
import com.customblocks.core.IncidentRecorder;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class UtilityCommands {

    private UtilityCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("list").executes(UtilityCommands::list));

        // give lives in GiveCommands (Group 17 slice 2) — split out to keep this file under the cap.

        root.then(CommandManager.literal("reload")
                .executes(UtilityCommands::reload));

        root.then(CommandManager.literal("search")
                .executes(UtilityCommands::searchGui)
                .then(CommandManager.argument("query", StringArgumentType.greedyString())
                        .executes(ctx -> search(ctx, StringArgumentType.getString(ctx, "query")))));

        root.then(CommandManager.literal("categories")
                .executes(UtilityCommands::categories));

        // Export is ADMIN-ONLY (G12, 2026-07-30): it writes server files in bulk. This node carried NO
        // .requires at all, so any player could dump every block definition to disk. Gating the parent
        // literal covers every child route (json/txt/…/zip/vault/<id>/…) in one place.
        root.then(CommandManager.literal("export")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(UtilityCommands::exportMenu)
                .then(CommandManager.literal("json").executes(ctx -> export(ctx, "json")))
                .then(CommandManager.literal("txt").executes(ctx -> export(ctx, "txt")))
                .then(CommandManager.literal("csv").executes(ctx -> export(ctx, "csv")))
                .then(CommandManager.literal("md").executes(ctx -> export(ctx, "md")))
                .then(CommandManager.literal("html").executes(ctx -> export(ctx, "html")))
                .then(CommandManager.literal("yaml").executes(ctx -> export(ctx, "yaml")))
                .then(CommandManager.literal("png").executes(UtilityCommands::exportPngAll))
                .then(CommandManager.literal("zip").executes(UtilityCommands::exportAllZip))
                .then(CommandManager.literal("vault").executes(UtilityCommands::exportVaultAll))
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> exportOneMenu(ctx, StringArgumentType.getString(ctx, "id")))
                        .then(CommandManager.literal("config")
                                .executes(ctx -> exportOneConfig(ctx, StringArgumentType.getString(ctx, "id"))))
                        .then(CommandManager.literal("png")
                                .executes(ctx -> exportOnePng(ctx, StringArgumentType.getString(ctx, "id"))))
                        .then(CommandManager.literal("vault")
                                .executes(ctx -> exportOneVault(ctx, StringArgumentType.getString(ctx, "id"))))
                        .then(CommandManager.literal("download")
                                .executes(ctx -> exportOneDownload(ctx, StringArgumentType.getString(ctx, "id"))))));

        root.then(CommandManager.literal("importfolder")
                .executes(ctx -> importFolderCmd(ctx, null))
                .then(CommandManager.argument("path", StringArgumentType.greedyString())
                        .executes(ctx -> importFolderCmd(ctx, StringArgumentType.getString(ctx, "path")))));
    }

    /**
     * /cb list — a player gets the Bulk Workbench on its Browse tab (§G07-3); the server console, which has
     * no screen to open, still gets the chat list it always got. Scripts driving /cb list from console are
     * unaffected.
     */
    private static int list(CommandContext<ServerCommandSource> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayerEntity) {
            return BulkSnapshot.openFor(ctx.getSource(), BulkSnapshot.TAB_BROWSE);
        }
        return chatList(ctx);
    }

    private static int chatList(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Collection<SlotData> all = SlotManager.assignedSlots();
        if (all.isEmpty()) {
            Chat.info(src, "No custom blocks yet. Make one with /cb create <id>");
            return 1;
        }
        Chat.raw(src, CbFmt.VALUE + all.size() + " custom block(s):");
        for (SlotData d : all) {
            Chat.raw(src, Text.literal(
                    CbFmt.DIM + " - " + CbFmt.BODY + d.customId() + categoryTag(d) + statusTags(d.customId())
                            + " " + CbFmt.DIM + "(slot " + d.index() + ", \"" + d.displayName() + "\")"));
        }
        // Clickable export options.
        MutableText exportLine = Text.literal(CbFmt.DIM + "Export list: ")
                .append(Chat.runButton("[.json]", "/cb export json", "Export all blocks to a .json file"))
                .append(Text.literal(" "))
                .append(Chat.runButton("[.txt]", "/cb export txt", "Export all blocks to a .txt file"));
        Chat.raw(src, exportLine);
        return 1;
    }

    private static int reload(CommandContext<ServerCommandSource> ctx) {
        SlotManager.reload();
        Chat.success(ctx.getSource(), "Reloaded " + SlotManager.usedSlots() + " block(s) from disk.");
        return 1;
    }

    private static int searchGui(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.SEARCH));
            return 1;
        }
        Chat.info(src, "Usage: /cb search <query>");
        return 1;
    }

    private static int search(CommandContext<ServerCommandSource> ctx, String query) {
        ServerCommandSource src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.SEARCH, query));
            return 1;
        }
        List<SlotData> hits = SlotManager.search(query);
        if (hits.isEmpty()) {
            Chat.info(src, "No blocks match \"" + query + "\"");
            return 1;
        }
        Chat.raw(src, CbFmt.VALUE + hits.size() + " match(es) for \"" + query + "\":");
        for (SlotData d : hits) {
            MutableText line = Text.literal(CbFmt.DIM + " - " + CbFmt.BODY + d.customId() + categoryTag(d) + statusTags(d.customId()) + " ")
                    .append(Chat.runButton("[give]", "/cb give " + d.customId(), "Give " + d.customId()));
            Chat.raw(src, line);
        }
        return 1;
    }

    private static int categories(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        // In-game: open the category browser chest GUI (Group 11). Console: text list below.
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.CATEGORY_LIST));
            return 1;
        }
        Set<String> cats = SlotManager.categories();
        if (cats.isEmpty()) {
            Chat.info(src, "No categories yet. Set one with /cb setcategory <id> <name>");
            return 1;
        }
        Chat.raw(src, CbFmt.VALUE + cats.size() + " categor(ies):");
        for (String c : cats) {
            int count = SlotManager.byCategory(c).size();
            MutableText line = Text.literal(CbFmt.DIM + " - " + CbFmt.BODY + c + " " + CbFmt.DIM + "(" + count + ") ")
                    .append(Chat.runButton("[list]", "/cb search " + c, "Show blocks in " + c));
            Chat.raw(src, line);
        }
        return 1;
    }

    /** A small grey "[category]" tag for list output, or "" when uncategorized. */
    private static String categoryTag(SlotData d) {
        return d.category().isEmpty() ? "" : " " + CbFmt.FAINT + "[" + d.category() + "]";
    }

    /** Lock/draft status tags for list output — e.g. " §c[locked] §8[draft]". */
    private static String statusTags(String id) {
        StringBuilder sb = new StringBuilder();
        if (LockManager.isLocked(id)) sb.append(" " + CbFmt.BAD + "[locked]");
        if (DraftManager.isDraft(id))  sb.append(" " + CbFmt.FAINT + "[draft]");
        return sb.toString();
    }

    /** /cb export (no args) — players get the dashboard GUI, console gets text */
    private static int exportMenu(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Collection<SlotData> all = SlotManager.assignedSlots();
        if (all.isEmpty()) return ExportReport.nothingToExport(src);
        // Players get the chest GUI dashboard
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.EXPORT_DASHBOARD));
            return 1;
        }
        // Console gets text output (same as before)
        MutableText msg = Text.literal(CbFmt.BODY + "Export " + CbFmt.VALUE + all.size() + CbFmt.BODY + " block(s): ")
                .append(Chat.runButton("[.json]", "/cb export json", "Bulk export all blocks to JSON"))
                .append(Text.literal(" "))
                .append(Chat.runButton("[.txt]", "/cb export txt", "Bulk export all blocks to TXT"))
                .append(Text.literal(" "))
                .append(Chat.runButton("[.csv]", "/cb export csv", "Bulk export to CSV (open in a spreadsheet)"))
                .append(Text.literal(" "))
                .append(Chat.runButton("[.md]", "/cb export md", "Bulk export to a Markdown table"))
                .append(Text.literal(" "))
                .append(Chat.runButton("[.html]", "/cb export html", "Bulk export to a viewable HTML table"))
                .append(Text.literal(" "))
                .append(Chat.runButton("[.png]", "/cb export png", "Save every block's texture as a .png image"))
                .append(Text.literal(" "))
                .append(Chat.runButton("[Vault share]", "/cb export vault", "Share one block with /cb export <id> vault"))
                .append(Text.literal("  " + CbFmt.DIM + "Per-block: " + CbFmt.BODY + "/cb export <id>"));
        Chat.line(src, msg);
        return 1;
    }

    /** /cb export json|txt|csv|md|html|yaml — the bulk block-list file. */
    private static int export(CommandContext<ServerCommandSource> ctx, String format) {
        ServerCommandSource src = ctx.getSource();
        Collection<SlotData> all = SlotManager.assignedSlots();
        if (all.isEmpty()) return ExportReport.nothingToExport(src);
        Path file = BlockExporter.exportAll(format, all);
        if (file == null) return ExportReport.failed(src, format + " file");
        return ExportReport.wrote(src, all.size(), "block", file);
    }

    /** /cb export png — write every block's baked texture PNG into cloud_exports/textures-&lt;stamp&gt;/ */
    private static int exportPngAll(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Collection<SlotData> all = SlotManager.assignedSlots();
        if (all.isEmpty()) return ExportReport.nothingToExport(src);
        BlockExporter.PngBatch r = BlockExporter.exportAllPng(all);
        if (r == null) return ExportReport.failed(src, "texture folder");
        // A folder, not a single file, so it reports the folder + how many landed rather than a size.
        Chat.success(src, "Exported " + r.written() + " texture PNG" + (r.written() == 1 ? "" : "s")
                + (r.skipped() > 0 ? " (" + r.skipped() + " had no texture)" : ""));
        Chat.raw(src, Text.literal(CbFmt.DIM + "Folder: " + CbFmt.BODY + BlockExporter.FOLDER_LABEL
                + r.dir().getFileName() + "/"));
        return 1;
    }

    /** /cb export &lt;id&gt; png — write that block's texture to cloud_exports/&lt;id&gt;.png + a download link */
    private static int exportOnePng(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.getById(id);
        if (d == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        Path file = BlockExporter.exportPng(d);
        if (file == null) { Chat.error(src, "No texture to export for \"" + id + "\"."); return 0; }
        String url = ResourcePackServer.getPngUrl(id);
        // The [download] link is G20 §L's route, awaiting its own owner decision — chip, wording and
        // behaviour are carried over untouched; only the report around it is G12's.
        return ExportReport.wroteOne(src, d.customId(), file, openUrlButton("[download]", url, url));
    }

    /** /cb export zip — bundle every block (json + png) into one cloud_exports/all-&lt;stamp&gt;.zip + a download link */
    private static int exportAllZip(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Collection<SlotData> all = SlotManager.assignedSlots();
        if (all.isEmpty()) return ExportReport.nothingToExport(src);
        Path zip = BlockExporter.exportAllZip(all);
        if (zip == null) return ExportReport.failed(src, "ZIP");
        String url = ResourcePackServer.getZipUrl(zip.getFileName().toString());
        return ExportReport.wrote(src, all.size(), "block", zip, openUrlButton("[download]", url, url));
    }

    /** /cb export vault — bulk vault upload is not built; point players to the one-block share path. */
    private static int exportVaultAll(CommandContext<ServerCommandSource> ctx) {
        Chat.info(ctx.getSource(), "Bulk Vault upload is not built yet. Share one block with /cb export <id> vault or /cb vault upload <id>.");
        return 1;
    }

    /** /cb export <id> — shows per-block export options */
    private static int exportOneMenu(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.getById(id);
        if (d == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        MutableText msg = Text.literal(CbFmt.BODY + "Export " + CbFmt.VALUE + id + CbFmt.BODY + ": ")
                .append(Chat.runButton("[to Config]", "/cb export " + id + " config", "Save " + id + ".json to exports folder"))
                .append(Text.literal(" "))
                .append(Chat.runButton("[.png]", "/cb export " + id + " png", "Save " + id + ".png (the block texture image)"))
                .append(Text.literal(" "))
                .append(Chat.runButton("[to Vault]", "/cb export " + id + " vault", "Upload to Block Vault"))
                .append(Text.literal(" "))
                .append(Chat.runButton("[Download]", "/cb export " + id + " download", "Get a download link for this block"));
        Chat.line(src, msg);
        return 1;
    }

    /** /cb export &lt;id&gt; config — saves the block's definition to cloud_exports/&lt;id&gt;.json */
    private static int exportOneConfig(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.getById(id);
        if (d == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        Path file = BlockExporter.exportOne(d);
        if (file == null) return ExportReport.failed(src, "block file");
        // Said "exports/<id>.json" before — a folder it never wrote to. It has always been cloud_exports/.
        return ExportReport.wroteOne(src, d.customId(), file);
    }

    /** /cb export <id> vault — upload one block to the cloud vault. */
    private static int exportOneVault(CommandContext<ServerCommandSource> ctx, String id) {
        return CloudCommands.uploadBlock(ctx.getSource(), id);
    }

    /** /cb export <id> download — saves to config then serves a link via the HTTP server */
    private static int exportOneDownload(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.getById(id);
        if (d == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        Path file = BlockExporter.exportOne(d);
        if (file == null) return ExportReport.failed(src, "block file");
        String url = ResourcePackServer.getExportUrl(id);
        MutableText msg = Text.literal(CbFmt.BODY + "Download " + CbFmt.VALUE + id + CbFmt.BODY + ": ")
                .append(openUrlButton("[open link]", url, url));
        Chat.line(src, msg);
        return 1;
    }

    private static int importFolderCmd(CommandContext<ServerCommandSource> ctx, String pathStr) {
        ServerCommandSource src = ctx.getSource();
        Path folder = pathStr == null
                ? Path.of("config/customblocks/exports")
                : Path.of(pathStr);
        BlockExporter.ImportResult result = BlockExporter.importFolder(folder);
        int c = result.created().size(), s = result.skipped().size(), f = result.failed().size();
        if (c == 0 && s == 0 && f == 0) {
            Chat.info(src, "No importable block JSONs found in: " + folder);
            return 1;
        }
        if (c > 0) {
            ResourcePackServer.updatePack();
            Chat.success(src, "Imported " + c + " block(s): " + String.join(", ", result.created()) + ".");
        }
        if (s > 0) Chat.info(src, "Skipped " + s + " that already exist: " + String.join(", ", result.skipped()));
        if (f > 0) {
            // Major-error routing (Group 04): import failures also land in the incidents log.
            IncidentRecorder.record("Import failed for " + f + " file(s) in " + folder + ": "
                    + String.join(", ", result.failed()), null, src.getName(), null);
            Chat.error(src, f + " file(s) couldn't be imported: " + String.join(", ", result.failed())
                    + ". Check they are valid CustomBlocks export JSONs.");
        }
        return c > 0 ? 1 : 0;
    }

    // ── Clickable chat helpers ───────────────────────────────────────────────


    private static MutableText openUrlButton(String label, String url, String hover) {
        return Text.literal(label).styled(s -> s
                .withColor(Formatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }
}
