/**
 * CategoryCommands.java — the unified /cb category command (Group 11 overhaul, 2nd pass).
 *
 * One base command with sub-actions (the old scattered /cb renamecategory, mergecategory,
 * categorydesc, givecategory, exportcategory, sharecategory, importcategory are GONE — folded
 * in here):
 *
 *   list (opens the Category Hub) · edit · rename · combine · open · color · icon ·
 *   sort · lock|unlock · give · export.
 *
 * Category descriptions are GONE (G11 2026-07-28) — the `desc` verb, the stored field and every
 * surface that printed it were removed rather than reworded.
 *
 * The membership verbs (create / set / remove / delete / filter / info) are in
 * CategoryMemberCommands and the network pair (share / import) in CategoryVaultCommands; all three
 * files hang their verbs onto the SAME `category` node, so /cb category stays one tree while each
 * file stays under the 400-line handler gate (§9.3).
 *
 * Sync logic lives in core/CategoryService (shared with CategoryEditMenu).
 *
 * Every category-name argument is a GREEDY string in last position and is never quoted
 * (G11 2026-07-26) — see {@link #catArg} and {@link CategorySuggest}.
 *
 * Depends on: CategoryService, SlotManager, SlotBlock, BlockExporter, ResourcePackServer,
 *             Chat, GuiRouter, Nav
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.block.SlotBlock;
import com.customblocks.command.Chat;
import com.customblocks.core.BlockExporter;
import com.customblocks.core.CategoryFilters;
import com.customblocks.core.CategoryMembershipStore;
import com.customblocks.core.CategoryMetadataStore;
import com.customblocks.core.CategoryReport;
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
        LiteralArgumentBuilder<ServerCommandSource> cat = CommandManager.literal("category")
                .executes(ctx -> openList(ctx.getSource()))

                // Bare `list` still opens the Category Hub for a player; with a mode it prints the
                // ordered text listing instead — the Hub has no filter UI of its own yet (G27 owns it).
                .then(CommandManager.literal("list")
                        .executes(ctx -> openList(ctx.getSource()))
                        .then(CommandManager.argument("mode", StringArgumentType.word())
                                .suggests((c, b) -> CategorySuggest.words(b, CategoryService.categoryModes()))
                                .executes(ctx -> listFiltered(ctx.getSource(), str(ctx, "mode")))))

                .then(CommandManager.literal("edit")
                        .then(catArg("cat").executes(ctx -> openEdit(ctx.getSource(), str(ctx, "cat")))))

                // `rename <old> into <new>` — same connector grammar as `combine` (G11 2026-07-28).
                // Two word-arguments could never take a multi-word name: only ONE argument may be
                // greedy, so "Arabic Numbers" was untypeable on either side. One greedy spec split
                // on the required `into` lets both names be multi-word and unquoted.
                .then(CommandManager.literal("rename")
                        .then(CommandManager.argument("spec", StringArgumentType.greedyString())
                                .suggests(CategorySuggest::rename)
                                .executes(ctx -> rename(ctx.getSource(), str(ctx, "spec")))))

                // `combine <a> into <b>` — the only fold-one-category-into-another verb (G11
                // 2026-07-26). `merge` is retired: `merge a b` never said which one survived. Both
                // names are greedy and unquoted, so the whole thing is ONE argument split on the
                // required `into` connector.
                .then(CommandManager.literal("combine")
                        .then(CommandManager.argument("spec", StringArgumentType.greedyString())
                                .suggests(CategorySuggest::combine)
                                .executes(ctx -> combine(ctx.getSource(), str(ctx, "spec")))))

                // The click target behind every category name printed in chat (G11 C12).
                .then(CommandManager.literal("open")
                        .then(catArg("cat").executes(ctx -> openHub(ctx.getSource(), str(ctx, "cat")))))

                // Value first, name last (G11 2026-07-26): the name is the greedy argument on every
                // verb, so "Arabic Numbers" can be typed bare instead of quoted.
                .then(CommandManager.literal("color")
                        .then(CommandManager.argument("color", StringArgumentType.word())
                                .suggests(CategorySuggest::colors)
                                .then(catArg("cat")
                                        .executes(ctx -> report(ctx.getSource(),
                                                CategoryService.setColor(str(ctx, "cat"), str(ctx, "color")))))))

                .then(CommandManager.literal("icon")
                        .then(CommandManager.argument("block", StringArgumentType.word())
                                .suggests(BlockSuggestions.IDS)
                                .then(catArg("cat")
                                        .executes(ctx -> report(ctx.getSource(),
                                                CategoryService.setIcon(str(ctx, "cat"), str(ctx, "block")))))))

                .then(CommandManager.literal("sort")
                        .then(CommandManager.argument("mode", StringArgumentType.word())
                                .suggests((c, b) -> CategorySuggest.words(b, List.of(CategoryFilters.ALPHA, "custom")))
                                .then(catArg("cat")
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
                        .then(catArg("cat").executes(ctx -> exportCategory(ctx, str(ctx, "cat")))));

        // create / set / remove / delete / filter / info and the vault pair live next door
        // (§9.3 line gate), hung onto this same node so it stays ONE /cb category tree.
        CategoryMemberCommands.register(cat);
        CategoryVaultCommands.register(cat);
        root.then(cat);
    }

    /** A single greedy category-name argument with category suggestions. */
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<ServerCommandSource, String> catArg(String name) {
        return CommandManager.argument(name, StringArgumentType.greedyString())
                .suggests(CategorySuggest::categories);
    }

    /** How a category is written in chat: its name AS TYPED, never quoted (G11 2026-07-26). */
    static String shown(String category) {
        return CategoryMetadataStore.getDisplayName(category);
    }

    /**
     * Turn a CategoryService.Outcome into chat feedback + a Brigadier result code.
     *
     * Every category line goes through {@link CategoryChat}, which tints the category name with its
     * own colour tag, makes it click/hoverable, and hangs the outcome's follow-up chip off the tail
     * (G11 C11-C13).
     */
    static int report(ServerCommandSource src, CategoryService.Outcome o) {
        return CategoryChat.report(src, o);
    }

    /** `/cb category rename <old> into <new>` — split the one greedy argument on its connector. */
    private static int rename(ServerCommandSource src, String spec) {
        int at = CategorySuggest.connectorAt(spec);
        if (at < 0) {
            Chat.error(src, "Say what it becomes: /cb category rename <category> into <new name>.");
            return 0;
        }
        String from = spec.substring(0, at).trim();
        String to = spec.substring(at + CategorySuggest.CONNECTOR.length()).trim();
        return report(src, CategoryService.rename(from, to));
    }

    /** `/cb category combine <a> into <b>` — split the one greedy argument on its connector. */
    private static int combine(ServerCommandSource src, String spec) {
        int at = CategorySuggest.connectorAt(spec);
        if (at < 0) {
            Chat.error(src, "Say which way round: /cb category combine <category> into <category>.");
            return 0;
        }
        String from = spec.substring(0, at).trim();
        String to = spec.substring(at + CategorySuggest.CONNECTOR.length()).trim();
        return report(src, CategoryService.combine(BulkConfirm.actor(src), from, to));
    }

    /** `/cb category open <cat>` — the Hub, focused on one category (the chat click target). */
    private static int openHub(ServerCommandSource src, String category) {
        String cat = CategoryMembershipStore.key(category);
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) {
            Chat.error(src, "The Category Hub is in-game only. Try /cb category info " + shown(cat) + ".");
            return 0;
        }
        ServerPlayNetworking.send(p, new OpenGuiPayload(GuiMode.CATEGORY_HUB.id, cat));
        return 1;
    }

    // ── list / edit / info ──────────────────────────────────────────────────────

    private static int openList(ServerCommandSource src) {
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            // Group 27: open the full red+black Category Hub client screen (replaces the old chest browser).
            ServerPlayNetworking.send(p, new OpenGuiPayload(GuiMode.CATEGORY_HUB.id, ""));
            return 1;
        }
        return listFiltered(src, ""); // console gets the text listing, alphabetical by default
    }

    /** The category listing in a named order (G11 filter — the category half of the mode set). */
    private static int listFiltered(ServerCommandSource src, String mode) {
        List<String> lines = CategoryReport.filterCategories(mode);
        if (lines == null) {
            Chat.error(src, "Unknown order \"" + mode + "\". Categories can be listed by: "
                    + String.join(", ", CategoryService.categoryModes()) + ".");
            return 0;
        }
        for (String line : lines) Chat.raw(src, CategoryChat.decorate(line));
        return 1;
    }

    private static int openEdit(ServerCommandSource src, String category) {
        String cat = CategoryMembershipStore.key(category);
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) {
            Chat.error(src, "The category editor is in-game only. Try /cb category info " + shown(cat) + ".");
            return 0;
        }
        GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.CATEGORY_EDIT, cat));
        return 1;
    }

    // ── /cb category give <cat> ─────────────────────────────────────────────────

    private static int giveCategory(CommandContext<ServerCommandSource> ctx, String category) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            Chat.error(src, "Only a player can receive items.");
            return 0;
        }
        String cat = CategoryMembershipStore.key(category);
        List<SlotData> blocks = sortedByIndex(CategoryMembershipStore.blocksIn(cat));
        if (blocks.isEmpty()) {
            Chat.error(src, "No blocks in category " + shown(cat) + ". See /cb categories.");
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
        String cat = CategoryMembershipStore.key(category);
        List<SlotData> blocks = sortedByIndex(CategoryMembershipStore.blocksIn(cat));
        if (blocks.isEmpty()) {
            Chat.error(src, "No blocks in category " + shown(cat) + ". See /cb categories.");
            return 0;
        }
        Path zip = BlockExporter.exportCategoryZip(cat, blocks);
        if (zip == null) return ExportReport.failed(src, "ZIP");
        // The [download] chip is G20 §L's route — carried over exactly as it was, wording included.
        MutableText download = Text.literal(CbFmt.VALUE + "[download]").styled(s -> s
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, ResourcePackServer.getZipUrl(zip.getFileName().toString())))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(CbFmt.DIM + "Open in browser to download"))));
        // Every block in the ZIP records ALL its categories, not just this one (schema v2) — the payload
        // is honest about where else they live, even though the selection is this one category.
        return ExportReport.headline(src, "Exported " + blocks.size() + " block"
                + (blocks.size() == 1 ? "" : "s") + " of " + shown(cat), zip, download);
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




}
