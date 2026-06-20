/**
 * CategoryCommands.java — the unified /cb category command (Group 11 overhaul, 2nd pass).
 *
 * One base command with sub-actions (the old scattered /cb renamecategory, mergecategory,
 * categorydesc, givecategory, exportcategory, sharecategory, importcategory are GONE — folded
 * in here):
 *
 *   /cb category list                     — open the category browser GUI (console: text list)
 *   /cb category edit <cat>               — open the CategoryEditMenu for a category (in-game)
 *   /cb category info <cat>               — text summary (counts, locks, colour, sort, icon, desc)
 *   /cb category rename <old> <new>       — rename (updates all blocks + metadata)
 *   /cb category merge <source> <target>  — move all blocks into target, delete source
 *   /cb category delete <cat>             — uncategorize all blocks (blocks kept)
 *   /cb category color <cat> <color>      — tint the category name (green/aqua/red/none/…)
 *   /cb category desc <cat> <text…>       — set the description
 *   /cb category icon <cat> <blockId>     — set the category icon block
 *   /cb category sort <cat> alpha|custom  — set the block display order
 *   /cb category lock|unlock <cat>        — lock/unlock every block in the category
 *   /cb category give <cat>               — give one of every block
 *   /cb category export <cat>             — ZIP every block (textures + JSON)
 *   /cb category share <cat>              — upload to the vault, get a share code (off-thread)
 *   /cb category import <code>            — download a shared category by code (off-thread)
 *
 * Sync logic lives in core/CategoryService (shared with CategoryEditMenu). Player/threaded
 * ops (give, export, share, import) stay here. Under the 400-line handler gate.
 *
 * Depends on: CategoryService, SlotManager, SlotBlock, BlockExporter, ResourcePackServer,
 *             Chat, CloudVaultClient, GuiRouter, Nav
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.block.SlotBlock;
import com.customblocks.cloud.CloudVaultClient;
import com.customblocks.command.Chat;
import com.customblocks.core.BlockExporter;
import com.customblocks.core.CategoryService;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class CategoryCommands {

    private CategoryCommands() {} // static-only

    private static String str(CommandContext<ServerCommandSource> c, String n) {
        return StringArgumentType.getString(c, n);
    }

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("category")
                .executes(ctx -> openList(ctx.getSource()))

                .then(CommandManager.literal("list")
                        .executes(ctx -> openList(ctx.getSource())))

                .then(CommandManager.literal("edit")
                        .then(catArg("cat").executes(ctx -> openEdit(ctx.getSource(), str(ctx, "cat")))))

                .then(CommandManager.literal("info")
                        .then(catArg("cat").executes(ctx -> info(ctx.getSource(), str(ctx, "cat")))))

                .then(CommandManager.literal("rename")
                        .then(CommandManager.argument("old", StringArgumentType.word())
                                .suggests(CategoryCommands::suggestCategories)
                                .then(CommandManager.argument("new", StringArgumentType.word())
                                        .executes(ctx -> report(ctx.getSource(),
                                                CategoryService.rename(str(ctx, "old"), str(ctx, "new")))))))

                .then(CommandManager.literal("merge")
                        .then(CommandManager.argument("source", StringArgumentType.word())
                                .suggests(CategoryCommands::suggestCategories)
                                .then(CommandManager.argument("target", StringArgumentType.word())
                                        .suggests(CategoryCommands::suggestCategories)
                                        .executes(ctx -> report(ctx.getSource(),
                                                CategoryService.merge(str(ctx, "source"), str(ctx, "target")))))))

                .then(CommandManager.literal("delete")
                        .then(catArg("cat").executes(ctx -> report(ctx.getSource(),
                                CategoryService.delete(str(ctx, "cat"))))))

                .then(CommandManager.literal("color")
                        .then(CommandManager.argument("cat", StringArgumentType.word())
                                .suggests(CategoryCommands::suggestCategories)
                                .then(CommandManager.argument("color", StringArgumentType.word())
                                        .suggests(CategoryCommands::suggestColors)
                                        .executes(ctx -> report(ctx.getSource(),
                                                CategoryService.setColor(str(ctx, "cat"), str(ctx, "color")))))))

                .then(CommandManager.literal("desc")
                        .then(CommandManager.argument("cat", StringArgumentType.word())
                                .suggests(CategoryCommands::suggestCategories)
                                .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                        .executes(ctx -> report(ctx.getSource(),
                                                CategoryService.setDescription(str(ctx, "cat"), str(ctx, "text")))))))

                .then(CommandManager.literal("icon")
                        .then(CommandManager.argument("cat", StringArgumentType.word())
                                .suggests(CategoryCommands::suggestCategories)
                                .then(CommandManager.argument("block", StringArgumentType.word())
                                        .executes(ctx -> report(ctx.getSource(),
                                                CategoryService.setIcon(str(ctx, "cat"), str(ctx, "block")))))))

                .then(CommandManager.literal("sort")
                        .then(CommandManager.argument("cat", StringArgumentType.word())
                                .suggests(CategoryCommands::suggestCategories)
                                .then(CommandManager.argument("mode", StringArgumentType.word())
                                        .suggests((c, b) -> { b.suggest("alpha"); b.suggest("custom"); return b.buildFuture(); })
                                        .executes(ctx -> report(ctx.getSource(),
                                                CategoryService.setSort(str(ctx, "cat"), str(ctx, "mode")))))))

                .then(CommandManager.literal("lock")
                        .then(catArg("cat").executes(ctx -> report(ctx.getSource(),
                                CategoryService.lockAll(str(ctx, "cat"), true)))))

                .then(CommandManager.literal("unlock")
                        .then(catArg("cat").executes(ctx -> report(ctx.getSource(),
                                CategoryService.lockAll(str(ctx, "cat"), false)))))

                .then(CommandManager.literal("give")
                        .then(catArg("cat").executes(ctx -> giveCategory(ctx, str(ctx, "cat")))))

                .then(CommandManager.literal("export")
                        .then(catArg("cat").executes(ctx -> exportCategory(ctx, str(ctx, "cat")))))

                .then(CommandManager.literal("share")
                        .then(catArg("cat").executes(ctx -> shareCategory(ctx, str(ctx, "cat")))))

                .then(CommandManager.literal("import")
                        .then(CommandManager.argument("code", StringArgumentType.word())
                                .executes(ctx -> importCategory(ctx, str(ctx, "code"))))));
    }

    /** A single greedy category-name argument with category suggestions. */
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<ServerCommandSource, String> catArg(String name) {
        return CommandManager.argument(name, StringArgumentType.greedyString())
                .suggests(CategoryCommands::suggestCategories);
    }

    /** Turn a CategoryService.Outcome into chat feedback + a Brigadier result code. */
    private static int report(ServerCommandSource src, CategoryService.Outcome o) {
        if (o.ok()) Chat.success(src, o.msg()); else Chat.error(src, o.msg());
        return o.ok() ? 1 : 0;
    }

    // ── list / edit / info ──────────────────────────────────────────────────────

    private static int openList(ServerCommandSource src) {
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.CATEGORY_LIST));
            return 1;
        }
        List<String> cats = new ArrayList<>(SlotManager.categories());
        if (cats.isEmpty()) { Chat.info(src, "No categories yet. Use /cb setcategory <id> <name>."); return 1; }
        cats.sort(String::compareToIgnoreCase);
        StringBuilder sb = new StringBuilder("Categories (" + cats.size() + "): ");
        for (String c : cats) sb.append(c).append(" (").append(SlotManager.byCategory(c).size()).append(")  ");
        Chat.info(src, sb.toString().trim());
        return 1;
    }

    private static int openEdit(ServerCommandSource src, String category) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) {
            Chat.error(src, "The category editor is in-game only. Try /cb category info " + category + ".");
            return 0;
        }
        GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.CATEGORY_EDIT, category.trim().toLowerCase(Locale.ROOT)));
        return 1;
    }

    private static int info(ServerCommandSource src, String category) {
        for (String line : CategoryService.info(category)) src.sendFeedback(() -> Text.literal(line), false);
        return 1;
    }

    // ── /cb category give <cat> ─────────────────────────────────────────────────

    private static int giveCategory(CommandContext<ServerCommandSource> ctx, String category) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            Chat.error(src, "Only a player can receive items.");
            return 0;
        }
        String cat = category.trim().toLowerCase(Locale.ROOT);
        List<SlotData> blocks = sortedByIndex(SlotManager.byCategory(cat));
        if (blocks.isEmpty()) {
            Chat.error(src, "No blocks in category \"" + cat + "\". See /cb categories.");
            return 0;
        }
        List<String> gave = new ArrayList<>();
        int overflow = 0, missing = 0;
        for (SlotData d : blocks) {
            SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
            if (item == null) { missing++; continue; }
            ItemStack st = new ItemStack(item);
            player.getInventory().insertStack(st);
            if (st.isEmpty()) gave.add(d.customId()); else overflow++;
        }
        if (gave.isEmpty()) {
            Chat.error(src, "Couldn't give any — your inventory is full.");
            return 0;
        }
        StringBuilder msg = new StringBuilder("Gave " + gave.size() + " item"
                + (gave.size() == 1 ? "" : "s") + ": " + String.join(", ", gave) + ".");
        if (overflow > 0) msg.append(" §e").append(overflow).append(" didn't fit (inventory full).");
        if (missing > 0)  msg.append(" §8").append(missing).append(" had no item — try /cb reload.");
        Chat.success(src, msg.toString());
        return 1;
    }

    // ── /cb category export <cat> ───────────────────────────────────────────────

    private static int exportCategory(CommandContext<ServerCommandSource> ctx, String category) {
        ServerCommandSource src = ctx.getSource();
        String cat = category.trim().toLowerCase(Locale.ROOT);
        List<SlotData> blocks = sortedByIndex(SlotManager.byCategory(cat));
        if (blocks.isEmpty()) {
            Chat.error(src, "No blocks in category \"" + cat + "\". See /cb categories.");
            return 0;
        }
        Path zip = BlockExporter.exportCategoryZip(cat, blocks);
        if (zip == null) {
            Chat.error(src, "Export failed — couldn't write the ZIP.");
            return 0;
        }
        MutableText msg = Text.literal(Chat.PREFIX + "§fExported §e" + blocks.size()
                        + "§f block(s) of §b" + cat + "§f → §7" + zip.getFileName() + "  ")
                .append(Text.literal("§b[download]").styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, ResourcePackServer.getZipUrl(zip.getFileName().toString()))).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("§7Open in browser to download")))));
        src.sendFeedback(() -> msg, false);
        return 1;
    }

    // ── /cb category share <cat>  (vault upload — off-thread) ────────────────────

    private static int shareCategory(CommandContext<ServerCommandSource> ctx, String category) {
        ServerCommandSource src = ctx.getSource();
        String cat = category.trim().toLowerCase(Locale.ROOT);
        if (!CloudVaultClient.isConfigured()) {
            Chat.error(src, "The cloud vault isn't set up yet. Put your worker URL in config.json as "
                    + "\"vaultEndpoint\", then /cb reload.");
            return 0;
        }
        List<SlotData> blocks = sortedByIndex(SlotManager.byCategory(cat));
        if (blocks.isEmpty()) {
            Chat.error(src, "No blocks in category \"" + cat + "\". See /cb categories.");
            return 0;
        }
        MinecraftServer server = src.getServer();
        if (server == null) return 0;
        Chat.info(src, "Uploading category \"" + cat + "\" to the vault…");
        new Thread(() -> {
            try {
                Path zip = BlockExporter.exportCategoryZip(cat, blocks);
                if (zip == null) { server.execute(() -> Chat.error(src, "Couldn't build the category ZIP.")); return; }
                byte[] data = java.nio.file.Files.readAllBytes(zip);
                String code = CloudVaultClient.uploadCategory(cat, data);
                server.execute(() -> {
                    if (code == null) {
                        Chat.error(src, "Upload failed — check vaultEndpoint and that the worker is reachable.");
                    } else {
                        MutableText msg = Text.literal(Chat.PREFIX + "§aShared §b" + cat + "§a — code: §e" + code + "  ")
                                .append(copyButton("[copy code]", code))
                                .append(Text.literal("  §7Import with §f/cb category import " + code));
                        src.sendFeedback(() -> msg, false);
                    }
                });
            } catch (Exception ex) {
                server.execute(() -> Chat.error(src, "Share failed: " + ex.getMessage()));
            }
        }, "cb-vault-share").start();
        return 1;
    }

    // ── /cb category import <code>  (vault download — off-thread) ────────────────

    private static int importCategory(CommandContext<ServerCommandSource> ctx, String code) {
        ServerCommandSource src = ctx.getSource();
        if (!CloudVaultClient.isConfigured()) {
            Chat.error(src, "The cloud vault isn't set up yet. Put your worker URL in config.json as "
                    + "\"vaultEndpoint\", then /cb reload.");
            return 0;
        }
        MinecraftServer server = src.getServer();
        if (server == null) return 0;
        Chat.info(src, "Downloading category code \"" + code + "\"…");
        new Thread(() -> {
            try {
                byte[] zip = CloudVaultClient.downloadCategory(code);
                if (zip == null) { server.execute(() -> Chat.error(src, "Download failed — bad code, or the vault is unreachable.")); return; }
                BlockExporter.ImportResult r = BlockExporter.importCategoryZip(zip);
                server.execute(() -> {
                    int c = r.created().size(), s = r.skipped().size(), f = r.failed().size();
                    if (c > 0) {
                        ResourcePackServer.updatePack();
                        Chat.success(src, "Imported " + c + " block(s): " + String.join(", ", r.created()) + ".");
                    }
                    if (s > 0) Chat.info(src, "Skipped " + s + " already-present: " + String.join(", ", r.skipped()));
                    if (f > 0) Chat.error(src, f + " couldn't import: " + String.join(", ", r.failed()));
                    if (c == 0 && s == 0 && f == 0) Chat.info(src, "Nothing to import from that code.");
                });
            } catch (Exception ex) {
                server.execute(() -> Chat.error(src, "Import failed: " + ex.getMessage()));
            }
        }, "cb-vault-import").start();
        return 1;
    }

    /**
     * Create-time hint (Group 11): if auto-categorize is enabled and the new block is still
     * uncategorized, post a one-click "Suggested category" line. Never auto-applies — the player
     * chooses. No-op when disabled or no keyword matched.
     */
    public static void suggestOnCreate(ServerCommandSource src, SlotData d) {
        if (!com.customblocks.CustomBlocksConfig.autoCategorizeEnabled) return;
        if (d == null || !d.category().isEmpty()) return;
        String cat = com.customblocks.core.AutoCategorizeManager.suggest(d);
        if (cat.isEmpty()) return;
        String set = "/cb setcategory " + d.customId() + " " + cat;
        MutableText msg = Text.literal(Chat.PREFIX + "§7Looks like category §b" + cat + "§7.  ")
                .append(runButton("§a[Add]", set, "Set " + d.customId() + " → " + cat))
                .append(Text.literal(" "))
                .append(suggestButton("§e[Edit]", set, "Pick a different category"));
        src.sendFeedback(() -> msg, false);
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private static List<SlotData> sortedByIndex(List<SlotData> in) {
        List<SlotData> out = new ArrayList<>(in);
        out.sort(Comparator.comparingInt(SlotData::index));
        return out;
    }

    private static MutableText copyButton(String label, String value) {
        return Text.literal(label).styled(s -> s
                .withColor(Formatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.literal("§7Copy:\n§f" + value))));
    }

    private static MutableText runButton(String label, String command, String hover) {
        return Text.literal(label).styled(s -> s
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("§7" + hover))));
    }

    private static MutableText suggestButton(String label, String command, String hover) {
        return Text.literal(label).styled(s -> s
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("§7" + hover))));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>
    suggestCategories(CommandContext<ServerCommandSource> ctx,
                      com.mojang.brigadier.suggestion.SuggestionsBuilder b) {
        String typed = b.getRemaining().toLowerCase(Locale.ROOT);
        for (String c : SlotManager.categories()) {
            if (c.toLowerCase(Locale.ROOT).startsWith(typed)) b.suggest(c);
        }
        return b.buildFuture();
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>
    suggestColors(CommandContext<ServerCommandSource> ctx,
                  com.mojang.brigadier.suggestion.SuggestionsBuilder b) {
        String typed = b.getRemaining().toLowerCase(Locale.ROOT);
        for (String c : CategoryService.colorWords()) {
            if (c.startsWith(typed)) b.suggest(c);
        }
        return b.buildFuture();
    }
}
