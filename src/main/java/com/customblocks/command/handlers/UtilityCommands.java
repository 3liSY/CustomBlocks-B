/**
 * UtilityCommands.java
 *
 * Responsibility: Non-mutating / utility subcommands — list, give, reload, export.
 * Registered into the /cb tree by CommandRegistrar. Stays under 400 lines (§9.3).
 *
 * `list` ends with clickable [.json] / [.txt] buttons that run `/cb export`, which
 * writes the block list to config/customblocks/exports/ and offers a copy-path button.
 */
package com.customblocks.command.handlers;

import com.customblocks.block.SlotBlock;
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
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
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

        root.then(CommandManager.literal("give")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> give(ctx, StringArgumentType.getString(ctx, "id")))));

        root.then(CommandManager.literal("reload")
                .executes(UtilityCommands::reload));

        root.then(CommandManager.literal("search")
                .executes(UtilityCommands::searchGui)
                .then(CommandManager.argument("query", StringArgumentType.greedyString())
                        .executes(ctx -> search(ctx, StringArgumentType.getString(ctx, "query")))));

        root.then(CommandManager.literal("categories")
                .executes(UtilityCommands::categories));

        root.then(CommandManager.literal("export")
                .executes(UtilityCommands::exportMenu)
                .then(CommandManager.literal("json").executes(ctx -> export(ctx, "json")))
                .then(CommandManager.literal("txt").executes(ctx -> export(ctx, "txt")))
                .then(CommandManager.literal("csv").executes(ctx -> export(ctx, "csv")))
                .then(CommandManager.literal("md").executes(ctx -> export(ctx, "md")))
                .then(CommandManager.literal("html").executes(ctx -> export(ctx, "html")))
                .then(CommandManager.literal("yaml").executes(ctx -> export(ctx, "yaml")))
                .then(CommandManager.literal("png").executes(UtilityCommands::exportPngAll))
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

    private static int list(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Collection<SlotData> all = SlotManager.assignedSlots();
        if (all.isEmpty()) {
            Chat.info(src, "No custom blocks yet. Make one with /cb create <id>");
            return 1;
        }
        src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§e" + all.size() + " custom block(s):"), false);
        for (SlotData d : all) {
            src.sendFeedback(() -> Text.literal(
                    "§7 - §f" + d.customId() + categoryTag(d) + statusTags(d.customId())
                            + " §7(slot " + d.index() + ", \"" + d.displayName() + "\")"), false);
        }
        // Clickable export options.
        MutableText exportLine = Text.literal("§7Export list: ")
                .append(runButton("[.json]", "/cb export json", "Export all blocks to a .json file"))
                .append(Text.literal(" "))
                .append(runButton("[.txt]", "/cb export txt", "Export all blocks to a .txt file"));
        src.sendFeedback(() -> exportLine, false);
        return 1;
    }

    private static int give(CommandContext<ServerCommandSource> ctx, String id) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.getById(id);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
        if (item == null) {
            Chat.error(src, "The item for \"" + id + "\" couldn't be found — try /cb reload, "
                    + "and report this if it keeps happening.");
            return 0;
        }
        ServerPlayerEntity player = src.getPlayerOrThrow();
        player.getInventory().insertStack(new ItemStack(item));
        Chat.success(src, "Gave you 1 × " + d.displayName() + ".");
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
            Chat.info(src, "No blocks match '" + query + "'");
            return 1;
        }
        src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§e" + hits.size() + " match(es) for \"" + query + "\":"), false);
        for (SlotData d : hits) {
            MutableText line = Text.literal("§7 - §f" + d.customId() + categoryTag(d) + statusTags(d.customId()) + " ")
                    .append(runButton("[give]", "/cb give " + d.customId(), "Give " + d.customId()));
            src.sendFeedback(() -> line, false);
        }
        return 1;
    }

    private static int categories(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Set<String> cats = SlotManager.categories();
        if (cats.isEmpty()) {
            Chat.info(src, "No categories yet. Set one with /cb setcategory <id> <name>");
            return 1;
        }
        src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§e" + cats.size() + " categor(ies):"), false);
        for (String c : cats) {
            int count = SlotManager.byCategory(c).size();
            MutableText line = Text.literal("§7 - §f" + c + " §7(" + count + ") ")
                    .append(runButton("[list]", "/cb search " + c, "Show blocks in " + c));
            src.sendFeedback(() -> line, false);
        }
        return 1;
    }

    /** A small grey "[category]" tag for list output, or "" when uncategorized. */
    private static String categoryTag(SlotData d) {
        return d.category().isEmpty() ? "" : " §8[" + d.category() + "]";
    }

    /** Lock/draft status tags for list output — e.g. " §c[locked] §8[draft]". */
    private static String statusTags(String id) {
        StringBuilder sb = new StringBuilder();
        if (LockManager.isLocked(id)) sb.append(" §c[locked]");
        if (DraftManager.isDraft(id))  sb.append(" §8[draft]");
        return sb.toString();
    }

    /** /cb export (no args) — shows bulk export options */
    private static int exportMenu(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Collection<SlotData> all = SlotManager.assignedSlots();
        if (all.isEmpty()) {
            Chat.info(src, "Nothing to export yet — make a block with /cb create <id>");
            return 1;
        }
        MutableText msg = Text.literal(Chat.PREFIX + "§fExport §e" + all.size() + "§f block(s): ")
                .append(runButton("[.json]", "/cb export json", "Bulk export all blocks to JSON"))
                .append(Text.literal(" "))
                .append(runButton("[.txt]", "/cb export txt", "Bulk export all blocks to TXT"))
                .append(Text.literal(" "))
                .append(runButton("[.csv]", "/cb export csv", "Bulk export to CSV (open in a spreadsheet)"))
                .append(Text.literal(" "))
                .append(runButton("[.md]", "/cb export md", "Bulk export to a Markdown table"))
                .append(Text.literal(" "))
                .append(runButton("[.html]", "/cb export html", "Bulk export to a viewable HTML table"))
                .append(Text.literal(" "))
                .append(runButton("[.png]", "/cb export png", "Save every block's texture as a .png image"))
                .append(Text.literal(" "))
                .append(runButton("[to Vault]", "/cb export vault", "Upload all blocks to the Vault"))
                .append(Text.literal("  §7Per-block: §f/cb export <id>"));
        src.sendFeedback(() -> msg, false);
        return 1;
    }

    /** /cb export json|txt */
    private static int export(CommandContext<ServerCommandSource> ctx, String format) {
        ServerCommandSource src = ctx.getSource();
        Collection<SlotData> all = SlotManager.assignedSlots();
        if (all.isEmpty()) {
            Chat.info(src, "Nothing to export yet — make a block with /cb create <id>");
            return 1;
        }
        Path file = BlockExporter.exportAll(format, all);
        if (file == null) {
            Chat.error(src, "Export failed — write error");
            return 0;
        }
        String name = file.getFileName().toString();
        MutableText msg = Text.literal(Chat.PREFIX + "§fExported §e" + all.size() + "§f block(s) → §7" + name + " ")
                .append(runButton("[to Vault]", "/cb export vault", "Upload all blocks to the Vault"));
        src.sendFeedback(() -> msg, false);
        return 1;
    }

    /** /cb export png — write every block's baked texture PNG into exports/textures-&lt;stamp&gt;/ */
    private static int exportPngAll(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Collection<SlotData> all = SlotManager.assignedSlots();
        if (all.isEmpty()) { Chat.info(src, "Nothing to export yet — make a block with /cb create <id>"); return 1; }
        BlockExporter.PngBatch r = BlockExporter.exportAllPng(all);
        if (r == null) { Chat.error(src, "Export failed — write error"); return 0; }
        Chat.success(src, "Exported §e" + r.written() + "§r texture PNG(s) → §7exports/" + r.dir().getFileName()
                + (r.skipped() > 0 ? " §8(" + r.skipped() + " had no texture)" : ""));
        return 1;
    }

    /** /cb export &lt;id&gt; png — write that block's texture to exports/&lt;id&gt;.png */
    private static int exportOnePng(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.getById(id);
        if (d == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        Path file = BlockExporter.exportPng(d);
        if (file == null) { Chat.error(src, "No texture to export for \"" + id + "\"."); return 0; }
        Chat.success(src, "Saved §e" + id + "§r texture → §7exports/" + id + ".png");
        return 1;
    }

    /** /cb export vault — bulk vault upload (Phase 14 stub) */
    private static int exportVaultAll(CommandContext<ServerCommandSource> ctx) {
        Chat.info(ctx.getSource(), "Vault sync is coming in Phase 14. Stay tuned!");
        return 1;
    }

    /** /cb export <id> — shows per-block export options */
    private static int exportOneMenu(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.getById(id);
        if (d == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        MutableText msg = Text.literal(Chat.PREFIX + "§fExport §e" + id + "§f: ")
                .append(runButton("[to Config]", "/cb export " + id + " config", "Save " + id + ".json to exports folder"))
                .append(Text.literal(" "))
                .append(runButton("[.png]", "/cb export " + id + " png", "Save " + id + ".png (the block texture image)"))
                .append(Text.literal(" "))
                .append(runButton("[to Vault]", "/cb export " + id + " vault", "Upload to Block Vault"))
                .append(Text.literal(" "))
                .append(runButton("[Download]", "/cb export " + id + " download", "Get a download link for this block"));
        src.sendFeedback(() -> msg, false);
        return 1;
    }

    /** /cb export <id> config — saves to exports/<id>.json */
    private static int exportOneConfig(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.getById(id);
        if (d == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        Path file = BlockExporter.exportOne(d);
        if (file == null) { Chat.error(src, "Export failed — write error"); return 0; }
        Chat.success(src, "Saved §e" + id + "§r → §7exports/" + id + ".json");
        return 1;
    }

    /** /cb export <id> vault — upload to vault (Phase 14 stub) */
    private static int exportOneVault(CommandContext<ServerCommandSource> ctx, String id) {
        if (SlotManager.getById(id) == null) { Chat.error(ctx.getSource(), "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        Chat.info(ctx.getSource(), "Vault sync is coming in Phase 14. Stay tuned!");
        return 1;
    }

    /** /cb export <id> download — saves to config then serves a link via the HTTP server */
    private static int exportOneDownload(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.getById(id);
        if (d == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        Path file = BlockExporter.exportOne(d);
        if (file == null) { Chat.error(src, "Export failed — write error"); return 0; }
        String url = ResourcePackServer.getExportUrl(id);
        MutableText msg = Text.literal(Chat.PREFIX + "§fDownload §e" + id + "§f: ")
                .append(openUrlButton("[open link]", url, url));
        src.sendFeedback(() -> msg, false);
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
            IncidentRecorder.record("Import failed for " + f + " file(s) in " + folder + " (by "
                    + src.getName() + "): " + String.join(", ", result.failed()));
            Chat.error(src, f + " file(s) couldn't be imported: " + String.join(", ", result.failed())
                    + ". Check they are valid CustomBlocks export JSONs.");
        }
        return c > 0 ? 1 : 0;
    }

    // ── Clickable chat helpers ───────────────────────────────────────────────

    private static MutableText runButton(String label, String command, String hover) {
        return Text.literal(label).styled(s -> s
                .withColor(Formatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }

    private static MutableText openUrlButton(String label, String url, String hover) {
        return Text.literal(label).styled(s -> s
                .withColor(Formatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }
}
