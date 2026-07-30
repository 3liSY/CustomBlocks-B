/**
 * CategoryHubView.java — Group 27 Category Hub, all rendering + hit-rect geometry. CLIENT-ONLY.
 *
 * Immediate-mode drawing extracted from {@link CategoryHubScreen} so the screen stays under the 500-line
 * size gate (CLAUDE.md §5.1) and has room for the v6 rebuild. Every frame this draws the bar, the left
 * category list, and the selected-category detail pane, and while doing so it records the on-screen
 * rectangle of every clickable element into public fields ({@code rRename}, {@code rowRects}, …). The
 * screen reads those same-frame rects back in its mouse handlers to route clicks — so this class owns
 * geometry, the screen owns state + input + actions.
 *
 * Layout constants live here as the single source of truth: the screen static-imports the few it needs
 * (LX/LW/BAR_H/Y_*) so the text fields it positions in init() can never drift from the labels drawn here.
 * Scroll offsets are owned here too (clamped during render, nudged via scrollList/scrollBlocks).
 *
 * Depends on: CategoryHubModel, CategoryHubDragDrop, ClientSlotCache, CbTheme.
 * Called by: client/gui/CategoryHubScreen (render + hit-rect read-back).
 */
package com.customblocks.client.gui;

import com.customblocks.client.ClientSlotCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
final class CategoryHubView {

    static final int BAR_H = 38, LX = 8, LW = 186, ROW_H = 22, BROW_H = 15;
    // Bottom-left New-category input (field drawn by the screen, the compact + button drawn here).
    static final int NEW_Y_OFF = 46, NEW_BTN_W = 48;
    private static final int CARD = 0xFF141414, CARD_SEL = CbTheme.SEL_FILL, EDGE = CbTheme.ACCENT;
    private static final int BTN = 0xFF2A2A2A, BTN_HOV = 0xFF444444, BTN_OFF = 0xFF181818, BTN_ON = CbTheme.SEL_FILL;
    // Overview dashboard: cyan live values (#39E0C8) + the mockup's marker-box reds/panel.
    private static final int CYAN = 0x39E0C8, RED_DIM = 0xFF7A0000, RED_DEEP = 0xFF2A0000, PANEL = 0xFF0A0A0D;

    // Detail-pane vertical layout (relative to BAR_H) — fixed gaps so labels never overlap controls.
    // The description section used to sit between OPTIONS and BLOCKS; descriptions were removed
    // root-and-branch (G11 2026-07-28), so BLOCKS takes back those 40px of detail-pane height.
    static final int Y_HEADER = 10, Y_RENAME_L = 26, Y_RENAME = 34, Y_ORG_L = 56, Y_ORG = 64,
            Y_COL_L = 88, Y_COL = 96, Y_HEX = 112, Y_OPT_L = 134, Y_OPT = 142, Y_BLK = 164;

    // Rects recomputed every render, read by CategoryHubScreen.mouseClicked in the same frame.
    final List<int[]> rowRects = new ArrayList<>();
    final List<String> rowKeys = new ArrayList<>();
    final List<int[]> blockRects = new ArrayList<>();
    final List<String> blockIds = new ArrayList<>();
    int[] rRename, rMerge, rDelete, rMove, rDefault, rSort, rLock, rUnlock, rNew, rClear, rSetHex;
    int[][] swatchRects = new int[CategoryHubModel.TAGS.length][4];

    // Scroll offsets owned here (clamped during render, nudged by the screen's scroll handler).
    private int listScroll, blockScroll;

    // Per-frame render context, set at the top of render() so the draw helpers can stay param-light.
    private CategoryHubScreen screen;
    private TextRenderer tr;
    private int width, height;

    void scrollList(int step) { listScroll = Math.max(0, listScroll + step); }
    void scrollBlocks(int step) { blockScroll = Math.max(0, blockScroll + step); }
    void resetBlockScroll() { blockScroll = 0; }

    // ── render ───────────────────────────────────────────────────────────────
    void render(DrawContext ctx, int mx, int my, CategoryHubScreen screen, TextRenderer tr, int width, int height) {
        this.screen = screen; this.tr = tr; this.width = width; this.height = height;
        ctx.fill(0, 0, width, height, 0xFF000000);
        List<CategoryHubModel.Row> rows = CategoryHubModel.rows(screen.searchText());

        ctx.fill(0, 0, width, BAR_H, CbTheme.BAR_BG);
        ctx.fill(0, BAR_H - 1, width, BAR_H, EDGE);
        ctx.drawTextWithShadow(tr, CbTheme.title("Category Hub"), 10, 8, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, Text.literal("§7" + rows.size() + " shown · §f"
                + CategoryHubModel.totalBlocks() + "§7 blocks"), 10, 22, 0xFFFFFFFF);

        renderList(ctx, mx, my, rows);
        if (screen.sel() != null) renderDetail(ctx, mx, my); else renderOverview(ctx);

        ctx.drawTextWithShadow(tr,
                Text.literal("§8click a category · drag a block onto one, or pick + Move · ? for help"), 8, height - 14, 0xFFFFFFFF);
    }

    private void renderList(DrawContext ctx, int mx, int my, List<CategoryHubModel.Row> rows) {
        rowRects.clear(); rowKeys.clear();
        String sel = screen.sel();
        CategoryHubDragDrop drag = screen.drag();
        int top = BAR_H + 30, bottom = height - 52;
        ctx.drawTextWithShadow(tr, CbTheme.red("CATEGORIES"), LX, BAR_H + 14, 0xFFFFFFFF);
        ctx.fill(LX - 1, top - 1, LX + LW + 1, bottom + 1, 0xFF2A2A2A);
        ctx.fill(LX, top, LX + LW, bottom, 0xFF0C0C0C);
        int visible = (bottom - top) / ROW_H;
        listScroll = clamp(listScroll, 0, Math.max(0, rows.size() - visible));
        for (int i = 0; i < visible && i + listScroll < rows.size(); i++) {
            CategoryHubModel.Row r = rows.get(i + listScroll);
            int y = top + i * ROW_H;
            boolean isSel = r.key().equals(sel);
            boolean hover = in(mx, my, LX, y, LW, ROW_H);
            ctx.fill(LX, y, LX + LW, y + ROW_H - 1, isSel ? CARD_SEL : (hover ? 0xFF1E1E1E : CARD));
            if (isSel) ctx.fill(LX, y, LX + 2, y + ROW_H - 1, EDGE);
            ctx.fill(LX + 6, y + 6, LX + 18, y + 18, 0xFF000000);
            ctx.fill(LX + 7, y + 7, LX + 17, y + 17, r.colorArgb());
            ctx.drawTextWithShadow(tr,
                    fitText(CategoryHubModel.coloredName(r.name(), r.colorTag(), r.colorHex()), LW - 66), LX + 24, y + 3, 0xFFFFFFFF);
            ctx.drawTextWithShadow(tr, Text.literal("§8" + r.count() + " blk"), LX + 24, y + 12, 0xFFFFFFFF);
            if (r.isDefault()) ctx.drawTextWithShadow(tr, Text.literal("§a★"), LX + LW - 12, y + 7, 0xFFFFFFFF);
            // L10 drag-drop overlays: eased lime border while hovered, fading pulse + "+1" bump after a drop.
            drag.renderRowOverlay(ctx, LX, y, LW, ROW_H - 1, r.key(), CbTheme.ACCENT);
            drag.renderBump(ctx, tr, LX + 52, y + 12, r.key());
            rowRects.add(new int[]{LX, y, LW, ROW_H - 1}); rowKeys.add(r.key());
        }
        // New-category: a compact "+ New" button beside the newField the screen draws at bottom-left.
        rNew = new int[]{LX + LW - NEW_BTN_W, height - NEW_Y_OFF, NEW_BTN_W, 16};
        drawBtn(ctx, mx, my, "+ New", rNew, !CategoryHubModel.normalizeId(screen.newText()).isEmpty(), false);
    }

    /** Hub overview (nothing selected): centered stats dashboard in the mockup's "Red Ops" language. */
    private void renderOverview(DrawContext ctx) {
        int rx = LX + LW + 10, r2 = width - 8, cx = (rx + r2) / 2;
        int cats = ClientSlotCache.categories().size();
        int blocks = CategoryHubModel.totalBlocks();
        String def = ClientSlotCache.defaultCategory();
        String defName = (def == null || def.isEmpty()) ? "—" : CategoryHubModel.titleCase(def);
        int loose = CategoryHubModel.uncategorizedCount();

        int paneTop = BAR_H, paneBot = height - 20, blockH = 130;
        int y = Math.max(paneTop + 8, paneTop + (paneBot - paneTop - blockH) / 2);

        // Marker box: red-dim frame · panel interior · red-deep inner square (mockup .empty .mk).
        ctx.fill(cx - 20, y, cx + 20, y + 40, RED_DIM);
        ctx.fill(cx - 18, y + 2, cx + 18, y + 38, PANEL);
        ctx.fill(cx - 8, y + 12, cx + 8, y + 28, RED_DEEP);
        y += 50;

        ctx.drawCenteredTextWithShadow(tr, Text.literal("§lSELECT A CATEGORY"), cx, y, 0xFFFFFFFF);
        y += 16;

        // Four stat pills, adaptive width so they fit narrow right panes.
        int gap = 8, n = 4, paneW = r2 - rx;
        int pw = Math.max(44, Math.min(96, (paneW - (n - 1) * gap) / n));
        int rowW = n * pw + (n - 1) * gap, sxp = cx - rowW / 2;
        statPill(ctx, sxp,                     y, pw, String.valueOf(cats),   "CATEGORIES");
        statPill(ctx, sxp + (pw + gap),        y, pw, String.valueOf(blocks), "BLOCKS");
        statPill(ctx, sxp + 2 * (pw + gap),    y, pw, defName,                "DEFAULT");
        statPill(ctx, sxp + 3 * (pw + gap),    y, pw, String.valueOf(loose),  "LOOSE");
        y += 44;

        ctx.drawCenteredTextWithShadow(tr, Text.literal("§7Pick one on the left to manage its blocks & settings."), cx, y, 0xFFFFFFFF);
        y += 11;
        ctx.drawCenteredTextWithShadow(tr, Text.literal("§8Or type a name bottom-left and press §7+ New§8 to start one."), cx, y, 0xFFFFFFFF);
    }

    /** One overview stat: bordered card + red spine + big cyan value + grey label. */
    private void statPill(DrawContext ctx, int px, int py, int pw, String value, String label) {
        int ph = 30;
        ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF242429);
        ctx.fill(px, py, px + pw, py + ph, 0xFF0C0C0F);
        ctx.fill(px, py, px + 2, py + ph, EDGE);
        ctx.drawCenteredTextWithShadow(tr, Text.literal(fit(value, pw - 8)).setStyle(
                net.minecraft.text.Style.EMPTY.withColor(net.minecraft.text.TextColor.fromRgb(CYAN)).withBold(true)),
                px + pw / 2, py + 5, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("§8" + fit(label, pw - 8)), px + pw / 2, py + 18, 0xFFFFFFFF);
    }

    private void renderDetail(DrawContext ctx, int mx, int my) {
        String sel = screen.sel();            // non-null here (render guards on screen.sel() != null)
        String pickedBlock = screen.pickedBlock();
        boolean confirmDelete = screen.confirmDelete();
        int rx = LX + LW + 10, rw = width - rx - 8;
        boolean real = !CategoryHubModel.UNCATEGORIZED.equals(sel);
        String key = sel;
        String tag = ClientSlotCache.colorTag(key), hex = ClientSlotCache.colorHex(key);

        // Header.
        ctx.drawTextWithShadow(tr,
                fitText(CategoryHubModel.coloredName(CategoryHubModel.titleCase(sel), tag, hex).formatted(net.minecraft.util.Formatting.BOLD), rw - 90), rx, BAR_H + Y_HEADER, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, Text.literal("§7" + CategoryHubModel.blocks(sel).size() + " blocks"), rx + rw - 60, BAR_H + Y_HEADER, 0xFFFFFFFF);

        // RENAME: pre-filled name box + Rename + Delete.
        ctx.drawTextWithShadow(tr, Text.literal(real ? "§8Name (edit, then Rename)" : "§8Uncategorized bucket"), rx, BAR_H + Y_RENAME_L, 0xFFFFFFFF);
        rRename = new int[]{rx + rw - 144, BAR_H + Y_RENAME, 70, 16};
        rDelete = new int[]{rx + rw - 70, BAR_H + Y_RENAME, 70, 16};
        drawBtn(ctx, mx, my, "Rename", rRename, real, false);
        drawBtn(ctx, mx, my, confirmDelete ? "§cConfirm?" : "Delete", rDelete, real, confirmDelete);

        // ORGANISE: Merge / Move open the popup picker.
        ctx.drawTextWithShadow(tr, Text.literal("§8Organise (pick a target — no typing)"), rx, BAR_H + Y_ORG_L, 0xFFFFFFFF);
        int hw = (rw - 6) / 2;
        rMerge = new int[]{rx, BAR_H + Y_ORG, hw, 18};
        rMove  = new int[]{rx + hw + 6, BAR_H + Y_ORG, rw - hw - 6, 18};
        drawBtn(ctx, mx, my, real ? "Merge into… ▾" : "Empty into… ▾", rMerge, real || CategoryHubModel.blocks(sel).size() > 0, false);
        drawBtn(ctx, mx, my, pickedBlock == null ? "Move picked block ▾" : "Move '" + fit(pickedBlock, 48) + "' ▾", rMove, pickedBlock != null, false);

        // COLOUR: swatches + custom hex.
        ctx.drawTextWithShadow(tr, Text.literal("§8Colour"), rx, BAR_H + Y_COL_L, 0xFFFFFFFF);
        int sx = rx, sy = BAR_H + Y_COL;
        for (int i = 0; i < CategoryHubModel.TAGS.length; i++) {
            int cx = sx + i * 15;
            swatchRects[i] = new int[]{cx, sy, 12, 12};
            boolean on = hex.isEmpty() && CategoryHubModel.TAGS[i].equals(tag);
            ctx.fill(cx - 1, sy - 1, cx + 13, sy + 13, on ? 0xFFFFFFFF : (in(mx, my, cx, sy, 12, 12) ? 0xFFAAAAAA : 0xFF000000));
            ctx.fill(cx, sy, cx + 12, sy + 12, CategoryHubModel.ARGB[i]);
        }
        rClear = new int[]{sx + CategoryHubModel.TAGS.length * 15 + 4, sy, 16, 12};
        drawBtn(ctx, mx, my, "✖", rClear, real, false);
        // hexField drawn by super.render at Y_HEX; label + Set button beside it.
        rSetHex = new int[]{rx + 82, BAR_H + Y_HEX - 1, 44, 16};
        drawBtn(ctx, mx, my, "Set #", rSetHex, real, false);
        if (!hex.isEmpty()) {
            ctx.fill(rx + 130, BAR_H + Y_HEX, rx + 144, BAR_H + Y_HEX + 14, 0xFF000000);
            ctx.fill(rx + 131, BAR_H + Y_HEX + 1, rx + 143, BAR_H + Y_HEX + 13, CategoryHubModel.argbForHex(hex));
            ctx.drawTextWithShadow(tr, Text.literal("§7" + hex), rx + 148, BAR_H + Y_HEX + 3, 0xFFFFFFFF);
        }

        // OPTIONS: default toggle / sort / lock / unlock.
        ctx.drawTextWithShadow(tr, Text.literal("§8Options"), rx, BAR_H + Y_OPT_L, 0xFFFFFFFF);
        boolean isDef = key.equalsIgnoreCase(ClientSlotCache.defaultCategory());
        boolean custom = "custom".equalsIgnoreCase(ClientSlotCache.sortOrder(key));
        int qw = (rw - 12) / 4, yo = BAR_H + Y_OPT;
        rDefault = new int[]{rx, yo, qw, 18};
        rSort    = new int[]{rx + qw + 4, yo, qw, 18};
        rLock    = new int[]{rx + 2 * (qw + 4), yo, qw, 18};
        rUnlock  = new int[]{rx + 3 * (qw + 4), yo, rw - 3 * (qw + 4), 18};
        drawBtn(ctx, mx, my, isDef ? "★ Default" : "Set default", rDefault, real, isDef);
        drawBtn(ctx, mx, my, custom ? "Sort: Custom" : "Sort: A–Z", rSort, real, false);
        drawBtn(ctx, mx, my, "Lock all", rLock, real, false);
        drawBtn(ctx, mx, my, "Unlock all", rUnlock, real, false);

        // BLOCKS (+ a search box that filters the list below, L11 "search inside a category").
        ctx.drawTextWithShadow(tr, CbTheme.red("BLOCKS"), rx, BAR_H + Y_BLK, 0xFFFFFFFF);
        renderBlocks(ctx, mx, my, rx, BAR_H + Y_BLK + 12, rw, height - (BAR_H + Y_BLK + 12) - 30);
    }

    private void renderBlocks(DrawContext ctx, int mx, int my, int rx, int top, int rw, int h) {
        blockRects.clear(); blockIds.clear();
        String sel = screen.sel();
        String pickedBlock = screen.pickedBlock();
        String q = screen.blockSearchText().trim().toLowerCase(java.util.Locale.ROOT);
        List<ClientSlotCache.Entry> all = CategoryHubModel.blocks(sel);
        List<ClientSlotCache.Entry> blocks = q.isEmpty() ? all : all.stream()
                .filter(e -> e.name().toLowerCase(java.util.Locale.ROOT).contains(q) || e.id().toLowerCase(java.util.Locale.ROOT).contains(q))
                .toList();
        ctx.fill(rx - 1, top - 1, rx + rw + 1, top + h + 1, 0xFF2A2A2A);
        ctx.fill(rx, top, rx + rw, top + h, 0xFF0C0C0C);
        if (blocks.isEmpty()) {
            ctx.drawTextWithShadow(tr, Text.literal(all.isEmpty()
                    ? "§8(no blocks — Move one here to fill it)" : "§8(no blocks match \"" + q + "\")"), rx + 6, top + 6, 0xFFFFFFFF);
            return;
        }
        int visible = h / BROW_H;
        blockScroll = clamp(blockScroll, 0, Math.max(0, blocks.size() - visible));
        for (int i = 0; i < visible && i + blockScroll < blocks.size(); i++) {
            ClientSlotCache.Entry e = blocks.get(i + blockScroll);
            int y = top + i * BROW_H;
            boolean picked = e.id().equals(pickedBlock);
            boolean hover = in(mx, my, rx, y, rw, BROW_H);
            ctx.fill(rx, y, rx + rw, y + BROW_H - 1, picked ? CARD_SEL : (hover ? 0xFF1E1E1E : 0xFF121212));
            if (picked) ctx.fill(rx, y, rx + 2, y + BROW_H - 1, EDGE);
            ctx.drawTextWithShadow(tr, Text.literal((picked ? "§f" : "§7") + fit(e.name(), rw - 90)), rx + 6, y + 3, 0xFFFFFFFF);
            ctx.drawTextWithShadow(tr, Text.literal("§8" + fit(e.id(), 70)), rx + rw - 78, y + 3, 0xFFFFFFFF);
            blockRects.add(new int[]{rx, y, rw, BROW_H - 1}); blockIds.add(e.id());
        }
    }

    // ── draw helpers ───────────────────────────────────────────────────────────
    private void drawBtn(DrawContext ctx, int mx, int my, String label, int[] r, boolean enabled, boolean active) {
        boolean hover = enabled && in(mx, my, r);
        int bg = !enabled ? BTN_OFF : (active ? BTN_ON : (hover ? BTN_HOV : BTN));
        ctx.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, active ? EDGE : 0xFF000000);
        ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], bg);
        ctx.drawCenteredTextWithShadow(tr, Text.literal((enabled ? "§f" : "§8") + fit(label, r[2] - 6)),
                r[0] + r[2] / 2, r[1] + (r[3] - 8) / 2, 0xFFFFFFFF);
    }

    private String fit(String s, int maxPx) {
        if (s == null) s = "";
        if (tr.getWidth(s) <= maxPx) return s;
        while (!s.isEmpty() && tr.getWidth(s + "…") > maxPx) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    /** Trim a coloured Text to fit maxPx, keeping its style (adds a plain ellipsis when clipped). */
    private Text fitText(net.minecraft.text.MutableText t, int maxPx) {
        String plain = t.getString();
        if (tr.getWidth(plain) <= maxPx) return t;
        return Text.literal(fit(plain, maxPx)).setStyle(t.getStyle());
    }

    private static boolean in(double mx, double my, int[] r) { return r != null && in(mx, my, r[0], r[1], r[2], r[3]); }
    private static boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
