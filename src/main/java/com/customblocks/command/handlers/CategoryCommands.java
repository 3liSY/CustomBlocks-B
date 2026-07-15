/**
 * CategoryCommands.java — the unified /cb category command (Group 11 overhaul, 2nd pass).
 *
 * One base command with sub-actions (the old scattered /cb renamecategory, mergecategory,
 * categorydesc, givecategory, exportcategory, sharecategory, importcategory are GONE — folded
 * in here):
 *
 *   list (opens the Category Hub) · edit · info · rename · merge · delete · color · desc · icon ·
 *   sort · lock|unlock · give · export · share · import  (one arg each, see the builder below).
 *
 * Sync logic lives in core/CategoryService (shared with CategoryEditMenu). Player/threaded
 * ops (give, export, share, import) stay here. Under the 400-line handler gate.
 *
 * Depends on: CategoryService, SlotManager, SlotBlock, BlockExporter, ResourcePackServer,
 *             Chat, CloudVaultClient, GuiRouter, Nav
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.core.IncidentRecorder;

import com.customblocks.command.CbFmt;
import com.customblocks.CustomBlocksConfig;
import com.customblocks.block.SlotBlock;
import com.customblocks.cloud.CloudVaultClient;
import com.customblocks.cloud.VaultHistory;
import com.customblocks.command.Chat;
import com.customblocks.core.BlockExporter;
import com.customblocks.core.CategoryService;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.GuiMode;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.customblocks.network.ResourcePackServer;
import com.customblocks.network.payloads.OpenGuiPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
            // Group 27: open the full red+black Category Hub client screen (replaces the old chest browser).
            ServerPlayNetworking.send(p, new OpenGuiPayload(GuiMode.CATEGORY_HUB.id, ""));
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
        for (String line : CategoryService.info(category)) Chat.raw(src, Text.literal(line));
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
        if (overflow > 0) msg.append(" " + CbFmt.VALUE).append(overflow).append(" didn't fit (inventory full).");
        if (missing > 0)  msg.append(" " + CbFmt.FAINT).append(missing).append(" had no item — try /cb reload.");
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
        MutableText msg = Text.literal(CbFmt.BODY + "Exported " + CbFmt.VALUE + blocks.size()
                        + CbFmt.BODY + " block(s) of " + CbFmt.VALUE + cat + CbFmt.BODY + " → " + CbFmt.DIM + zip.getFileName() + "  ")
                .append(Text.literal(CbFmt.VALUE + "[download]").styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, ResourcePackServer.getZipUrl(zip.getFileName().toString()))).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(CbFmt.DIM + "Open in browser to download")))));
        Chat.line(src, msg);
        return 1;
    }

    // ── /cb category share <cat>  (vault upload — off-thread) ────────────────────

    private static int shareCategory(CommandContext<ServerCommandSource> ctx, String category) {
        ServerCommandSource src = ctx.getSource();
        String cat = category.trim().toLowerCase(Locale.ROOT);
        if (!CustomBlocksConfig.cloudShareEnabled) { Chat.error(src, "Cloud sharing is disabled. Enable it in /cb config (cloudShareEnabled)."); return 0; }
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
                        VaultHistory.record("category", code, cat, src);
                        MutableText msg = Text.literal(CbFmt.OK + "Shared " + CbFmt.VALUE + cat + CbFmt.OK + " — code: " + CbFmt.VALUE + code + "  ")
                                .append(Chat.shareButton(code))
                                .append(Text.literal("  " + CbFmt.DIM + "Import with " + CbFmt.BODY + "/cb category import " + code));
                        Chat.line(src, msg);
                    }
                });
            } catch (Exception ex) {
                String code = IncidentRecorder.record("Category vault share failed for \"" + cat + "\"", null, src.getName(), ex);
                server.execute(() -> Chat.incidentError(src, "Couldn't share that category — the vault didn't answer. Check your connection and try again.", code));
            }
        }, "cb-vault-share").start();
        return 1;
    }

    // ── /cb category import <code>  (vault download — off-thread) ────────────────

    private static int importCategory(CommandContext<ServerCommandSource> ctx, String code) {
        ServerCommandSource src = ctx.getSource();
        if (!CustomBlocksConfig.cloudShareEnabled) { Chat.error(src, "Cloud sharing is disabled. Enable it in /cb config (cloudShareEnabled)."); return 0; }
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
                String incidentCode = IncidentRecorder.record("Category vault import failed (code: " + code + ")", null, src.getName(), ex);
                server.execute(() -> Chat.incidentError(src, "Couldn't import that code — it may be wrong, expired, or the vault is unreachable.", incidentCode));
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
        MutableText msg = Text.literal(CbFmt.DIM + "Looks like category " + CbFmt.VALUE + cat + CbFmt.DIM + ".  ")
                .append(Chat.runButton(CbFmt.OK + "[Add]", set, "Set " + d.customId() + " → " + cat))
                .append(Text.literal(" "))
                .append(Chat.suggestButton(CbFmt.VALUE + "[Edit]", set, "Pick a different category"));
        Chat.line(src, msg);
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private static List<SlotData> sortedByIndex(List<SlotData> in) {
        List<SlotData> out = new ArrayList<>(in);
        out.sort(Comparator.comparingInt(SlotData::index));
        return out;
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
