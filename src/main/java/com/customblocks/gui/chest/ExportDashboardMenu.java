/**
 * ExportDashboardMenu.java — unified Export Dashboard chest GUI (Group 11 overhaul).
 *
 * /cb export (player, no args) opens this GUI instead of the old chat-based text output.
 * A dynamic same-GUI flow with two phases:
 *   Phase 1 (scope):  tiles for Per Block, Per Category, All Blocks, Bulk Choose.
 *   Phase 2 (format): every non-"All" scope shows the SAME seven format tiles (json, txt,
 *                      csv, md, html, yaml, png) via formatTiles(), routed through
 *                      /cb bulkexport <scope> <format>. A "← Scopes" tile returns to phase 1.
 *
 * "Bulk Choose" opens the block list (Dest.BLOCK_LIST) in listPickForExport mode; ticking
 * blocks there and confirming returns here as the "selection" phase.
 *
 * The MenuKey arg encodes the phase: "" = scope selection, "block" = block picker,
 * "category" = category picker, "all" = format choices for all, "selection" = format
 * choices for the hand-picked set, "block:<id>" = formats for one block,
 * "cat:<name>" = formats for one category.
 *
 * Depends on: ChestMenu, Icons, Layout, GuiFx, GuiRouter, Nav,
 *             SlotManager, SlotData, SlotBlock, BlockExporter, ListSelection
 * Called by:  GuiRouter (Dest.EXPORT_DASHBOARD), UtilityCommands (/cb export)
 */
package com.customblocks.gui.chest;

import com.customblocks.block.SlotBlock;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ExportDashboardMenu {

    private ExportDashboardMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, String arg, int page) {
        String phase = (arg == null || arg.isEmpty()) ? "" : arg;

        if (phase.startsWith("block:"))     return buildBlockFormats(player, phase.substring(6));
        if (phase.startsWith("cat:"))       return buildCategoryFormats(player, phase.substring(4));
        if ("all".equals(phase))            return buildAllFormats(player);
        if ("selection".equals(phase))      return buildSelectionFormats(player);
        if ("block".equals(phase))          return buildBlockPicker(player, page);
        if ("category".equals(phase))       return buildCategoryPicker(player, page);
        return buildScopeSelection(player);
    }

    // ── Phase 1: Scope selection ─────────────────────────────────────────────

    private static ChestMenu buildScopeSelection(ServerPlayerEntity player) {
        int blockCount = SlotManager.assignedSlots().size();

        ChestMenu m = new ChestMenu("Export Dashboard", 3).fill();
        m.set(4, Icons.of(Items.CHEST, "§6§lExport Dashboard",
                "§7Choose what to export.",
                "§7" + blockCount + " block(s) registered."));

        // Per Block (slot 10)
        m.set(10, Icons.of(Items.GRASS_BLOCK, "§a§lPer Block",
                        "§7Export a single block.",
                        "§7Pick one from the list →"),
                (p, b, a) -> {
                    GuiFx.click(p);
                    GuiRouter.repage(p, MenuKey.of(Dest.EXPORT_DASHBOARD, "block").withPage(0));
                });

        // Per Category (slot 12)
        m.set(12, Icons.of(Items.BOOKSHELF, "§b§lPer Category",
                        "§7Export all blocks in a category.",
                        "§7Pick a category →"),
                (p, b, a) -> {
                    GuiFx.click(p);
                    GuiRouter.repage(p, MenuKey.of(Dest.EXPORT_DASHBOARD, "category").withPage(0));
                });

        // All Blocks (slot 14)
        m.set(14, Icons.of(Items.ENDER_CHEST, "§d§lAll Blocks",
                        "§7Export every block at once.",
                        "§7" + blockCount + " block(s)"),
                (p, b, a) -> {
                    GuiFx.click(p);
                    GuiRouter.repage(p, MenuKey.of(Dest.EXPORT_DASHBOARD, "all"));
                });

        // Bulk Choose (slot 16) — open the block list to hand-pick blocks, then export them.
        m.set(16, Icons.of(Items.BUNDLE, "§e§lBulk Choose",
                        "§7Hand-pick blocks to export.",
                        "§7Opens the block list — tick the",
                        "§7ones you want, then pick a format."),
                (p, b, a) -> {
                    GuiFx.click(p);
                    ListSelection.clear(p.getUuid());           // start the pick fresh
                    BulkSession.get(p.getUuid()).listPickForExport = true;
                    GuiRouter.navigate(p, MenuKey.of(Dest.BLOCK_LIST));
                });

        m.set(18, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    // ── Phase 1b: Block picker ───────────────────────────────────────────────

    private static ChestMenu buildBlockPicker(ServerPlayerEntity player, int page) {
        List<SlotData> all = new ArrayList<>(SlotManager.assignedSlots());
        all.sort(Comparator.comparingInt(SlotData::index));

        int per = Layout.PER_PAGE;
        int maxPage = all.isEmpty() ? 0 : (all.size() - 1) / per;
        int p = Math.max(0, Math.min(page, maxPage));

        ChestMenu m = new ChestMenu("Export — Pick a block", 6);
        if (all.isEmpty()) {
            m.set(22, Icons.of(Items.PAPER, "§7No blocks yet",
                    "§8Create one with /cb create <id>"));
        }

        int start = p * per;
        for (int i = 0; i < per; i++) {
            int gi = start + i;
            if (gi >= all.size()) break;
            SlotData d = all.get(gi);
            SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
            Item display = item != null ? item : Items.STONE;
            m.set(i, Icons.of(display, "§f" + d.displayName(),
                            "§7id: §f" + d.customId(),
                            "§aClick §7→ export options"),
                    (pl, b, a) -> {
                        GuiFx.click(pl);
                        GuiRouter.repage(pl, MenuKey.of(Dest.EXPORT_DASHBOARD, "block:" + d.customId()));
                    });
        }

        Layout.pagedFooter(m, p, maxPage, Dest.EXPORT_DASHBOARD, "block", all.size());
        // Override the back button to go to scope selection
        m.set(45, Icons.of(Items.ARROW, "§e← Scopes", "§8Back to scope selection"),
                (pl, b, a) -> {
                    GuiFx.click(pl);
                    GuiRouter.repage(pl, MenuKey.of(Dest.EXPORT_DASHBOARD, ""));
                });
        return m;
    }

    // ── Phase 1c: Category picker ────────────────────────────────────────────

    private static ChestMenu buildCategoryPicker(ServerPlayerEntity player, int page) {
        List<String> cats = new ArrayList<>(SlotManager.categories());

        int per = Layout.PER_PAGE;
        int maxPage = cats.isEmpty() ? 0 : (cats.size() - 1) / per;
        int pg = Math.max(0, Math.min(page, maxPage));

        ChestMenu m = new ChestMenu("Export — Pick a category", 6);
        if (cats.isEmpty()) {
            m.set(22, Icons.of(Items.PAPER, "§7No categories yet",
                    "§8Set one with /cb setcategory <id> <name>"));
        }

        int start = pg * per;
        for (int i = 0; i < per; i++) {
            int gi = start + i;
            if (gi >= cats.size()) break;
            String cat = cats.get(gi);
            int count = SlotManager.byCategory(cat).size();
            m.set(i, Icons.of(CategoryListMenu.iconFor(cat), "§e" + cat,
                            "§7" + count + " block(s)",
                            "§aClick §7→ export this category"),
                    (pl, b, a) -> {
                        GuiFx.click(pl);
                        GuiRouter.repage(pl, MenuKey.of(Dest.EXPORT_DASHBOARD, "cat:" + cat));
                    });
        }

        Layout.pagedFooter(m, pg, maxPage, Dest.EXPORT_DASHBOARD, "category", cats.size());
        m.set(45, Icons.of(Items.ARROW, "§e← Scopes", "§8Back to scope selection"),
                (pl, b, a) -> {
                    GuiFx.click(pl);
                    GuiRouter.repage(pl, MenuKey.of(Dest.EXPORT_DASHBOARD, ""));
                });
        return m;
    }

    // ── Phase 2: Format choices — single block ───────────────────────────────

    private static ChestMenu buildBlockFormats(ServerPlayerEntity player, String id) {
        SlotData d = SlotManager.getById(id);
        ChestMenu m = new ChestMenu("Export: " + id, 3).fill();

        if (d == null) {
            m.set(13, Icons.of(Items.BARRIER, "§cBlock not found", "§7\"" + id + "\" no longer exists."));
            m.set(18, Icons.of(Items.ARROW, "§e← Scopes"), (p, b, a) -> goBackToScopes(p));
            m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
            return m;
        }

        SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
        m.set(4, Icons.glint(item != null ? item : Items.STONE, "§f" + d.displayName(),
                "§7id: §f" + d.customId()));

        // Full, standardized format list for this one block.
        formatTiles(m, id);

        // Group 12: single-block downloads land in cloud_exports/ with a clickable [download] link.
        m.set(9, Icons.of(Items.WRITABLE_BOOK, "§a§lDownload JSON",
                        "§7Save as cloud_exports/" + id + ".json",
                        "§7and get a [download] link in chat."),
                (p, b, a) -> { GuiFx.apply(p); GuiRouter.runCommand(p, "export " + id + " download"); });
        m.set(15, Icons.of(Items.PAINTING, "§a§lDownload PNG",
                        "§7Save this block's texture as a .png",
                        "§7and get a [download] link in chat."),
                (p, b, a) -> { GuiFx.apply(p); GuiRouter.runCommand(p, "export " + id + " png"); });
        // Generate a tradeable Blueprint item carrying this block's recipe.
        m.set(16, Icons.of(Items.PAPER, "§b§lGenerate Blueprint",
                        "§7Get a Blueprint item of this block.",
                        "§7Hand it to a friend; they run",
                        "§7/cb importblock while holding it."),
                (p, b, a) -> { GuiFx.apply(p); GuiRouter.runCommand(p, "exportblock " + id); });

        m.set(18, Icons.of(Items.ARROW, "§e← Scopes", "§8Back to scope selection"),
                (p, b, a) -> goBackToScopes(p));
        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    // ── Phase 2: Format choices — category ───────────────────────────────────

    private static ChestMenu buildCategoryFormats(ServerPlayerEntity player, String cat) {
        int count = SlotManager.byCategory(cat).size();
        ChestMenu m = new ChestMenu("Export: " + cat, 3).fill();

        m.set(4, Icons.glint(CategoryListMenu.iconFor(cat), "§b" + cat,
                "§7" + count + " block(s)"));

        // Full, standardized format list for the whole category.
        formatTiles(m, "category:" + cat);

        // Category ZIP (slot 17) — the category-specific bundle, kept alongside the standard list.
        m.set(17, Icons.of(Items.CHEST, "§6§lCategory ZIP",
                        "§7Textures + JSON metadata in one ZIP,",
                        "§7saved to cloud_exports/."),
                (p, b, a) -> { GuiFx.apply(p); GuiRouter.runCommand(p, "category export " + cat); });

        m.set(18, Icons.of(Items.ARROW, "§e← Scopes", "§8Back to scope selection"),
                (p, b, a) -> goBackToScopes(p));
        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    // ── Phase 2: Format choices — all blocks ─────────────────────────────────

    private static ChestMenu buildAllFormats(ServerPlayerEntity player) {
        int count = SlotManager.assignedSlots().size();
        ChestMenu m = new ChestMenu("Export All (" + count + " blocks)", 3).fill();

        m.set(4, Icons.of(Items.ENDER_CHEST, "§d§lExport All",
                "§7" + count + " block(s)"));

        // JSON (slot 9)
        m.set(9, Icons.of(Items.WRITABLE_BOOK, "§a§l.json",
                        "§7Bulk export to JSON"),
                (p, b, a) -> { GuiFx.apply(p); GuiRouter.runCommand(p, "export json"); });

        // TXT (slot 10)
        m.set(10, Icons.of(Items.PAPER, "§e§l.txt",
                        "§7Bulk export to plain text"),
                (p, b, a) -> { GuiFx.apply(p); GuiRouter.runCommand(p, "export txt"); });

        // CSV (slot 11)
        m.set(11, Icons.of(Items.MAP, "§b§l.csv",
                        "§7Bulk export to CSV (spreadsheet)"),
                (p, b, a) -> { GuiFx.apply(p); GuiRouter.runCommand(p, "export csv"); });

        // Markdown (slot 12)
        m.set(12, Icons.of(Items.BOOK, "§f§l.md",
                        "§7Bulk export to Markdown table"),
                (p, b, a) -> { GuiFx.apply(p); GuiRouter.runCommand(p, "export md"); });

        // HTML (slot 13)
        m.set(13, Icons.of(Items.KNOWLEDGE_BOOK, "§6§l.html",
                        "§7Bulk export to viewable HTML"),
                (p, b, a) -> { GuiFx.apply(p); GuiRouter.runCommand(p, "export html"); });

        // YAML (slot 14)
        m.set(14, Icons.of(Items.FILLED_MAP, "§d§l.yaml",
                        "§7Bulk export to YAML"),
                (p, b, a) -> { GuiFx.apply(p); GuiRouter.runCommand(p, "export yaml"); });

        // PNG batch (slot 15)
        m.set(15, Icons.of(Items.PAINTING, "§a§lPNG Textures",
                        "§7Save every block's texture as .png"),
                (p, b, a) -> { GuiFx.apply(p); GuiRouter.runCommand(p, "export png"); });

        // Export All → one ZIP (json + png per block) with a download link (slot 16, Group 12).
        m.set(16, Icons.of(Items.CHEST, "§6§lExport All (ZIP)",
                        "§7Bundle every block (JSON + texture)",
                        "§7into one .zip + a [download] link."),
                (p, b, a) -> { GuiFx.apply(p); GuiRouter.runCommand(p, "export zip"); });

        // Vault (slot 17) — coming soon
        m.set(17, Icons.of(Items.ENDER_PEARL, "§8§lTo Vault §8(coming soon)",
                        "§8Vault Worker not deployed yet."),
                (p, b, a) -> GuiFx.deny(p));

        m.set(18, Icons.of(Items.ARROW, "§e← Scopes", "§8Back to scope selection"),
                (p, b, a) -> goBackToScopes(p));
        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    // ── Phase 2: Format choices — per selection ──────────────────────────────

    private static ChestMenu buildSelectionFormats(ServerPlayerEntity player) {
        int count = ListSelection.size(player.getUuid());
        ChestMenu m = new ChestMenu("Export Picked (" + count + " blocks)", 3).fill();

        if (count == 0) {
            m.set(13, Icons.of(Items.BARRIER, "§cNothing picked",
                    "§7Use Bulk Choose to tick some blocks first."));
            m.set(18, Icons.of(Items.ARROW, "§e← Scopes"), (p, b, a) -> goBackToScopes(p));
            m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
            return m;
        }

        m.set(4, Icons.of(Items.BUNDLE, "§e§lExport Picked",
                "§7" + count + " block(s) selected"));

        // Full, standardized format list for the hand-picked set (comma-joined ids as the filter).
        formatTiles(m, ListSelection.joined(player.getUuid()));

        m.set(18, Icons.of(Items.ARROW, "§e← Scopes", "§8Back to scope selection"),
                (p, b, a) -> goBackToScopes(p));
        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    private static void goBackToScopes(ServerPlayerEntity p) {
        GuiFx.click(p);
        GuiRouter.repage(p, MenuKey.of(Dest.EXPORT_DASHBOARD, ""));
    }

    /**
     * Lay out the standard seven export-format tiles (slots 9–15) that every scope shares,
     * each routed through /cb bulkexport &lt;scope&gt; &lt;format&gt; so any block set — one block,
     * a whole category (category:&lt;name&gt;) or a hand-picked id list — exports identically.
     */
    private static void formatTiles(ChestMenu m, String scope) {
        fmt(m, 9,  Items.WRITABLE_BOOK, "§a§l.json",  "Export to JSON",            scope, "json");
        fmt(m, 10, Items.PAPER,         "§e§l.txt",   "Export to plain text",      scope, "txt");
        fmt(m, 11, Items.MAP,           "§b§l.csv",   "Export to CSV (spreadsheet)", scope, "csv");
        fmt(m, 12, Items.BOOK,          "§f§l.md",    "Export to a Markdown table", scope, "md");
        fmt(m, 13, Items.KNOWLEDGE_BOOK,"§6§l.html",  "Export to viewable HTML",   scope, "html");
        fmt(m, 14, Items.FILLED_MAP,    "§d§l.yaml",  "Export to YAML",            scope, "yaml");
        fmt(m, 15, Items.PAINTING,      "§a§lPNG Textures", "Save each block's texture as .png", scope, "png");
    }

    private static void fmt(ChestMenu m, int slot, Item icon, String label, String desc,
                            String scope, String format) {
        m.set(slot, Icons.of(icon, label, "§7" + desc),
                (p, b, a) -> { GuiFx.apply(p); GuiRouter.runCommand(p, "bulkexport " + scope + " " + format); });
    }
}
