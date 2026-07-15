/**
 * BulkWorkbenchView.java — Group 07 (Bulk Operations Hub). CLIENT-ONLY.
 *
 * Chrome + the Blocks List tab (§G27.22, design LOCKED 2026-07-12). Draws the title bar (bold-red title, no
 * subtitle; "N blocks" grey + a separate green-bordered "N selected" pill), the LEFT tab rail (Blocks List ·
 * Bulk Actions — TABS_ON_LEFT rule), the always-visible NL command bar strip, and the footer (History button
 * bottom-LEFT). It records the on-screen rectangle of everything clickable into public fields that
 * {@link BulkWorkbenchScreen} reads back the same frame to route clicks.
 *
 * Blocks List tab (§G27.22): a full-width responsive GRID of ~30 tiles, each a slowly-rotating 3-D cube
 * (phase-locked via {@link BulkCube}) + the block's display name + its {@code id:} (id in lime). Filter chips
 * (All · ★ Favorites · Locked · Selected), a Sort ▾ dropdown (Name · ID · Newest · Color), a draggable
 * scrollbar + "showing X of N". Left-click ticks a tile (red border + tint + ✓ badge + subtle pulse);
 * right-click opens the block's info panel on the LEFT (fills the dead rail space); the footer's Select ▾ has
 * four options.
 *
 * Depends on: BulkDraw, BulkWorkbenchModel, BulkCube, ClientSlotCache, CbTheme.
 * Called by: BulkWorkbenchScreen (render + hit-rect read-back). drawRow is also reused by the Bulk Actions tab.
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

import static com.customblocks.client.gui.BulkDraw.BAR_H;
import static com.customblocks.client.gui.BulkDraw.ROW_H;

@Environment(EnvType.CLIENT)
final class BulkWorkbenchView {

    static final int LX = 8;
    static final int RAIL_W = 104;                // the LEFT tab rail (wide enough for "Bulk Actions")
    static final int TAB_TOP = BAR_H + 8, TAB_H = 22, TAB_GAP = 3;
    static final int CONTENT_X = LX + RAIL_W + 8;  // content starts to the right of the rail
    static final int NL_BAR_H = 30;                // always-visible natural-language command bar strip (§G07-4)
    static final int CONTENT_Y = BAR_H + 8 + NL_BAR_H;  // content sits below the title bar + the NL bar
    static final int FOOT_H = 30;

    // The NL command bar's input field (a Screen widget) + its preview live in the strip above the content.
    static int nlFieldX() { return CONTENT_X; }
    static int nlFieldY() { return BAR_H + 8; }
    static int nlFieldW(int width) { return Math.max(120, contentW(width) - 190); }

    // §G27.22 grid tile geometry: cube up top, name + id beneath (tuned for ~6 columns × 5 rows ≈ 30 tiles).
    private static final int GRID_PAD = 6, TILE_W = 72, TILE_H = 78, TILE_GAP = 6;
    private static final int CUBE_HALF = 15;                    // ~30px cube in the tile's top band
    private static final int CHIP_ROW_H = 16, SHOW_H = 12;      // filter-chip strip + "showing X of N" line

    /** Tab labels, indexed by BulkWorkbenchScreen.TAB_* (§G27.22: two tabs only — the Extra tab was cut). */
    static final String[] TABS = {"Blocks List", "Bulk Actions"};

    static int contentRight(int width) { return width - 8; }
    static int contentBottom(int height) { return height - FOOT_H - 8; }
    static int contentW(int width) { return contentRight(width) - CONTENT_X; }

    // Rects recomputed every render, read back by BulkWorkbenchScreen.mouseClicked the same frame.
    final List<int[]> rowRects = new ArrayList<>();   // grid tiles (kept the name — still the clickable cells)
    final List<String> rowIds = new ArrayList<>();
    int[][] rTabs = new int[TABS.length][];
    int[] rClose, rHistory;
    // Blocks List controls.
    int[] rChipAll, rChipFav, rChipLocked, rChipSel;                 // filter chips
    int[] rSort, rSortName, rSortId, rSortNewest, rSortColor;         // Sort ▾ + its 4 options
    int[] rSelAll, rSelScreen, rSelMatch, rSelLocked, rSelFav, rClearSel, rUseThese; // Select ▾ (4 opts) + Clear
    int[] rInfoClose, rInfoTick, rInfoEditor, rInfoPanel;             // left info panel (right-click a tile)
    int[] rListScroll;

    private int scroll;                              // first visible tile ROW
    private final List<String> pageIds = new ArrayList<>();      // currently-rendered tiles (Select "on screen")
    private final List<String> allMatching = new ArrayList<>();  // every tile matching filter+search (Select "matching")
    private String hoverId;                          // tile under the cursor, for the tooltip
    // Scrollbar drag geometry (A2), captured in renderGrid and read by scrollToMouse the same frame.
    private int barTop, barH, barTotal, barVisible;

    void scrollBy(int step) { scroll = Math.max(0, scroll + step); }
    void resetScroll() { scroll = 0; }
    List<String> pageIds() { return pageIds; }
    List<String> allMatchingIds() { return allMatching; }

    /** Drag the grid scrollbar (A2): map the mouse-Y over the track to a scroll offset (in tile rows). */
    void scrollToMouse(double my) {
        int max = Math.max(0, barTotal - barVisible);
        if (max == 0 || barH <= 0) return;
        int idx = (int) Math.round((my - barTop) / (double) barH * max);
        scroll = BulkDraw.clamp(idx, 0, max);
    }

    // ── chrome (drawn on every tab) ───────────────────────────────────────────

    void renderChrome(DrawContext ctx, TextRenderer tr, int mx, int my, BulkWorkbenchScreen s, int width, int height) {
        ctx.fill(0, 0, width, height, 0xFF000000);

        ctx.fill(0, 0, width, BAR_H, CbTheme.BAR_BG);
        ctx.fill(0, BAR_H - 1, width, BAR_H, CbTheme.ACCENT);
        ctx.drawTextWithShadow(tr, CbTheme.title("Bulk Operations Hub"), 10, 8, 0xFFFFFFFF);

        // "N blocks" (grey) then a separate green-bordered "N selected" pill — two distinct counts (§G27.22).
        String blocks = ClientSlotCache.entries().size() + " blocks";
        ctx.drawTextWithShadow(tr, Text.literal("§7" + blocks), 10, 22, 0xFFFFFFFF);
        int pillX = 10 + tr.getWidth(blocks) + 8;
        selectedPill(ctx, tr, pillX, 20, s.ticked().size());
        if (s.pickMode()) ctx.drawTextWithShadow(tr, Text.literal("§8· pick blocks"),
                pillX + tr.getWidth(s.ticked().size() + " selected") + 16, 22, 0xFFFFFFFF);

        // LEFT tab rail (X-rule TABS_ON_LEFT). Pick mode is a Blocks-List-only hand-off, so Bulk Actions locks.
        for (int i = 0; i < TABS.length; i++) {
            rTabs[i] = new int[]{LX, TAB_TOP + i * (TAB_H + TAB_GAP), RAIL_W, TAB_H};
            boolean usable = !s.pickMode() || i == BulkWorkbenchScreen.TAB_BROWSE;
            BulkDraw.btn(ctx, tr, mx, my, TABS[i], rTabs[i], usable, s.tab() == i, false);
        }

        int footY = height - FOOT_H;
        ctx.fill(0, footY, width, height, CbTheme.BAR_BG);
        ctx.fill(0, footY, width, footY + 1, CbTheme.ACCENT);
        rClose = new int[]{width - 30, 11, 22, 16};
        BulkDraw.btn(ctx, tr, mx, my, "✖", rClose, true);

        // History (§G07-4 / §G27.22) — now bottom-LEFT in the footer, shows the undo depth, opens the popup.
        rHistory = new int[]{LX, footY + 6, 92, 18};
        BulkDraw.btn(ctx, tr, mx, my, "History " + s.undoHistSize(), rHistory, true, s.histOpen(), false);

        // §G07-4 always-visible NL command bar: its input is a Screen widget (nlField*). The old "press Enter to
        // parse" helper is gone (§G27.22) — draw the live parse result only once there is one.
        String pv = s.nlPreview();
        if (pv != null && !pv.isBlank()) {
            int px = nlFieldX() + nlFieldW(width) + 8;
            ctx.drawTextWithShadow(tr, Text.literal(pv), px, nlFieldY() + 4, 0xFFFFFFFF);
        }
    }

    /** The green-bordered "N selected" pill beside the block count. */
    private void selectedPill(DrawContext ctx, TextRenderer tr, int x, int y, int n) {
        String t = n + " selected";
        int w = tr.getWidth(t) + 10;
        ctx.fill(x - 1, y - 1, x + w + 1, y + 13, CbTheme.LIME);
        ctx.fill(x, y, x + w, y + 12, 0xFF0C0C0C);
        ctx.drawTextWithShadow(tr, Text.literal((n > 0 ? "§a" : "§7") + t), x + 5, y + 2, 0xFFFFFFFF);
    }

    // ── Blocks List tab (§G27.22 grid) ─────────────────────────────────────────

    void renderBrowse(DrawContext ctx, TextRenderer tr, int mx, int my, BulkWorkbenchScreen s, int width, int height) {
        int gx = CONTENT_X, gw = contentW(width);
        int chipY = CONTENT_Y;
        renderChips(ctx, tr, mx, my, s, gx, chipY, width);

        int gy = CONTENT_Y + CHIP_ROW_H + 4;
        int gbottom = contentBottom(height);
        int gh = gbottom - gy - SHOW_H;
        List<ClientSlotCache.Entry> rows = renderGrid(ctx, tr, mx, my, s, gx, gy, gw, gh);

        // "showing X of N" under the grid.
        ctx.drawTextWithShadow(tr, Text.literal("§8showing " + pageIds.size() + " of " + rows.size()),
                gx + 2, gy + gh + 2, 0xFFFFFFFF);

        // Right-click detail = info panel on the LEFT (fills the dead rail space; opaque overlay).
        if (s.detailId() != null) renderInfoPanel(ctx, tr, mx, my, s, gy, gbottom);
        else { rInfoClose = null; rInfoEditor = null; rInfoTick = null; }

        renderFoot(ctx, tr, mx, my, s, width, height);

        // Sort dropdown overlays everything below the button (drawn last).
        if (s.sortOpen()) renderSortMenu(ctx, tr, mx, my, s);

        // Hover tooltip — only when nothing is overlaying the grid.
        if (hoverId != null && s.detailId() == null && !s.selAllOpen() && !s.sortOpen())
            drawTileTooltip(ctx, tr, s, hoverId, mx, my);
    }

    /** Filter-chip strip + the Sort ▾ button, above the grid. */
    private void renderChips(DrawContext ctx, TextRenderer tr, int mx, int my, BulkWorkbenchScreen s,
                             int gx, int y, int width) {
        String f = s.browseFilter();
        rChipAll    = chip(ctx, tr, mx, my, gx,          y, "All",         f.equals("all"));
        rChipFav    = chip(ctx, tr, mx, my, gx + 42,     y, "★ Favorites", f.equals("fav"));
        rChipLocked = chip(ctx, tr, mx, my, gx + 128,    y, "Locked",      f.equals("locked"));
        rChipSel    = chip(ctx, tr, mx, my, gx + 190,    y, "Selected",    f.equals("selected"));

        rSort = new int[]{contentRight(width) - 116, y, 116, CHIP_ROW_H - 2};
        BulkDraw.btn(ctx, tr, mx, my, "Sort: " + sortLabel(s.browseSort()) + " ▾", rSort, true, s.sortOpen(), false);
    }

    private int[] chip(DrawContext ctx, TextRenderer tr, int mx, int my, int x, int y, String label, boolean active) {
        int w = tr.getWidth(label.replace("§e", "")) + 12;
        int[] r = {x, y, w, CHIP_ROW_H - 2};
        BulkDraw.btn(ctx, tr, mx, my, label, r, true, active, false);
        return r;
    }

    private static String sortLabel(String key) {
        return switch (key) { case "id" -> "ID"; case "newest" -> "Newest"; case "color" -> "Color"; default -> "Name"; };
    }

    private void renderSortMenu(DrawContext ctx, TextRenderer tr, int mx, int my, BulkWorkbenchScreen s) {
        int x = rSort[0], y = rSort[1] + rSort[3] + 2, w = rSort[2], h = 16;
        ctx.fill(x - 1, y - 1, x + w + 1, y + 4 * h + 1, 0xFF000000);
        rSortName   = opt(ctx, tr, mx, my, x, y,             w, h, "Name",   s.browseSort().equals("name"));
        rSortId     = opt(ctx, tr, mx, my, x, y + h,         w, h, "ID",     s.browseSort().equals("id"));
        rSortNewest = opt(ctx, tr, mx, my, x, y + 2 * h,     w, h, "Newest", s.browseSort().equals("newest"));
        rSortColor  = opt(ctx, tr, mx, my, x, y + 3 * h,     w, h, "Color",  s.browseSort().equals("color"));
    }

    private int[] opt(DrawContext ctx, TextRenderer tr, int mx, int my, int x, int y, int w, int h, String label, boolean active) {
        int[] r = {x, y, w, h};
        BulkDraw.btn(ctx, tr, mx, my, label, r, true, active, false);
        return r;
    }

    /** Returns the full (filtered + sorted) row list so the caller can print "showing X of N". */
    private List<ClientSlotCache.Entry> renderGrid(DrawContext ctx, TextRenderer tr, int mx, int my,
                                                   BulkWorkbenchScreen s, int gx, int gy, int gw, int gh) {
        rowRects.clear(); rowIds.clear(); pageIds.clear(); allMatching.clear(); hoverId = null;
        List<ClientSlotCache.Entry> rows =
                BulkWorkbenchModel.visible(s.searchText(), BulkWorkbenchModel.Filter.all(), s.locked(), s.fav());
        rows = applyChip(rows, s);
        sortRows(rows, s);
        for (ClientSlotCache.Entry e : rows) allMatching.add(e.id());

        BulkDraw.panel(ctx, gx, gy, gw, gh);
        if (rows.isEmpty()) {
            ctx.drawTextWithShadow(tr, Text.literal(ClientSlotCache.entries().isEmpty()
                    ? "§8(no blocks yet — make one with /cb create)"
                    : "§8(nothing matches this filter / search)"), gx + 8, gy + 8, 0xFFFFFFFF);
            rListScroll = null;
            return rows;
        }

        int strideX = TILE_W + TILE_GAP, strideY = TILE_H + TILE_GAP;
        int cols = Math.max(1, (gw - GRID_PAD * 2 + TILE_GAP) / strideX);
        int rowsVisible = Math.max(1, (gh - GRID_PAD * 2 + TILE_GAP) / strideY);
        int totalRows = (rows.size() + cols - 1) / cols;
        scroll = BulkDraw.clamp(scroll, 0, Math.max(0, totalRows - rowsVisible));
        barTop = gy; barH = gh; barTotal = totalRows; barVisible = rowsVisible;  // A2 drag geometry (tile rows)

        int ox = gx + GRID_PAD, oy = gy + GRID_PAD;
        ctx.enableScissor(gx, gy, gx + gw, gy + gh);
        for (int r = 0; r < rowsVisible; r++) {
            for (int c = 0; c < cols; c++) {
                int listIdx = (r + scroll) * cols + c;
                if (listIdx >= rows.size()) break;
                ClientSlotCache.Entry e = rows.get(listIdx);
                int tx = ox + c * strideX, ty = oy + r * strideY;
                drawTile(ctx, tr, mx, my, s, e, tx, ty);
                rowRects.add(new int[]{tx, ty, TILE_W, TILE_H});
                rowIds.add(e.id());
                pageIds.add(e.id());
                if (BulkDraw.in(mx, my, tx, ty, TILE_W, TILE_H)) hoverId = e.id();
            }
        }
        ctx.disableScissor();
        rListScroll = BulkDraw.vscroll(ctx, gx + gw - 4, gy, gh, totalRows, rowsVisible, scroll);
        return rows;
    }

    /** Narrow by the active filter chip (§G27.22): All · Favorites · Locked · Selected. */
    private List<ClientSlotCache.Entry> applyChip(List<ClientSlotCache.Entry> rows, BulkWorkbenchScreen s) {
        String f = s.browseFilter();
        if (f.equals("all")) return rows;
        List<ClientSlotCache.Entry> out = new ArrayList<>();
        for (ClientSlotCache.Entry e : rows) {
            boolean keep = switch (f) {
                case "fav"      -> s.fav().contains(e.id());
                case "locked"   -> s.locked().contains(e.id());
                case "selected" -> s.ticked().contains(e.id());
                default         -> true;
            };
            if (keep) out.add(e);
        }
        return out;
    }

    /** Sort by the Sort ▾ choice: Name · ID · Newest (slot index desc) · Color (texture hue via BulkCube). */
    private void sortRows(List<ClientSlotCache.Entry> rows, BulkWorkbenchScreen s) {
        switch (s.browseSort()) {
            case "id" -> rows.sort((a, b) -> a.id().compareToIgnoreCase(b.id()));
            case "newest" -> rows.sort((a, b) -> Integer.compare(idx(b.id()), idx(a.id())));
            case "color" -> rows.sort((a, b) -> Float.compare(s.cube().hue(a.id()), s.cube().hue(b.id())));
            default -> rows.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        }
    }

    private static int idx(String id) {
        Integer i = ClientSlotCache.indexForId(id);
        return i == null ? -1 : i;
    }

    /** One grid tile: rotating cube, display name + id (lime), red border + ✓ + pulse when ticked, corner flags. */
    private void drawTile(DrawContext ctx, TextRenderer tr, int mx, int my, BulkWorkbenchScreen s,
                          ClientSlotCache.Entry e, int tx, int ty) {
        boolean tick = s.ticked().contains(e.id());
        boolean hover = BulkDraw.in(mx, my, tx, ty, TILE_W, TILE_H);
        ctx.fill(tx, ty, tx + TILE_W, ty + TILE_H, tick ? BulkDraw.ROW_SEL : (hover ? BulkDraw.ROW_HOV : BulkDraw.ROW));
        if (tick) {
            int a = CbTheme.ACCENT;
            ctx.fill(tx, ty, tx + TILE_W, ty + 2, a);
            ctx.fill(tx, ty + TILE_H - 2, tx + TILE_W, ty + TILE_H, a);
            ctx.fill(tx, ty, tx + 2, ty + TILE_H, a);
            ctx.fill(tx + TILE_W - 2, ty, tx + TILE_W, ty + TILE_H, a);
            // subtle phase-locked red pulse over the tile.
            int alpha = 0x18 + (int) (0x18 * (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 320.0)));
            ctx.fill(tx + 2, ty + 2, tx + TILE_W - 2, ty + TILE_H - 2, (alpha << 24) | 0x00FF0000);
        } else if (hover) {
            ctx.fill(tx, ty, tx + TILE_W, ty + 1, CbTheme.ACCENT_DIM);
        } else {
            ctx.fill(tx, ty, tx + TILE_W, ty + 1, BulkDraw.PANEL_E);
        }

        // §G27.22: the tile icon is a slowly-rotating 3-D cube, phase-locked to the shared clock (BulkCube).
        s.cube().render(ctx, e.id(), tx + TILE_W / 2, ty + 22, CUBE_HALF);

        // Name + id:LIME beneath the cube, each clipped to the tile width (never overlaps a neighbour, X2).
        ctx.drawCenteredTextWithShadow(tr, Text.literal((tick ? "§f" : "§7") + BulkDraw.fit(tr, e.name(), TILE_W - 6)),
                tx + TILE_W / 2, ty + 46, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("§8id:§a" + BulkDraw.fit(tr, e.id(), TILE_W - 6 - tr.getWidth("id:"))),
                tx + TILE_W / 2, ty + 58, 0xFFFFFFFF);

        if (tick) {   // ✓ badge (lime), top-right corner
            ctx.fill(tx + TILE_W - 13, ty + 3, tx + TILE_W - 3, ty + 13, 0xFF000000);
            ctx.drawTextWithShadow(tr, Text.literal("§a✓"), tx + TILE_W - 11, ty + 4, 0xFFFFFFFF);
        }
        if (s.locked().contains(e.id())) BulkDraw.padlock(ctx, tx + 3, ty + TILE_H - 12);
        if (s.fav().contains(e.id())) ctx.drawTextWithShadow(tr, Text.literal("§e★"), tx + 3, ty + 3, 0xFFFFFFFF);
    }

    private void drawTileTooltip(DrawContext ctx, TextRenderer tr, BulkWorkbenchScreen s, String id, int mx, int my) {
        ClientSlotCache.Entry e = BulkWorkbenchModel.byId(id);
        if (e == null) return;
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("§f" + e.name()));
        lines.add(Text.literal("§8id:§a" + e.id()));
        if (e.category() != null && !e.category().isEmpty()) lines.add(Text.literal("§8" + e.category()));
        String flags = (s.locked().contains(id) ? "§c locked " : "") + (s.fav().contains(id) ? "§e ★" : "");
        if (!flags.isBlank()) lines.add(Text.literal(flags.trim()));
        lines.add(Text.literal("§8left-click targets · right-click info"));
        ctx.drawTooltip(tr, lines, mx, my);
    }

    // ── Left info panel (right-click a tile) — §G27.22, fills the dead rail space ────────────────
    private void renderInfoPanel(DrawContext ctx, TextRenderer tr, int mx, int my, BulkWorkbenchScreen s,
                                 int top, int bottom) {
        ClientSlotCache.Entry e = BulkWorkbenchModel.byId(s.detailId());
        if (e == null) { rInfoClose = null; rInfoTick = null; rInfoEditor = null; return; }
        int px = LX, pw = RAIL_W + 80, ph = bottom - top;
        rInfoPanel = new int[]{px, top, pw + 2, ph};
        // Opaque backing (OPAQUE_MODALS — no bleed-through) + a right accent edge (panel-on-the-left look).
        ctx.fill(px, top, px + pw, top + ph, 0xFF0C0C0C);
        ctx.fill(px + pw, top, px + pw + 2, top + ph, CbTheme.ACCENT);

        rInfoClose = new int[]{px + pw - 18, top + 4, 14, 14};
        BulkDraw.btn(ctx, tr, mx, my, "✖", rInfoClose, true);

        // Large rotating hero cube.
        s.cube().render(ctx, e.id(), px + pw / 2, top + 34, 26);
        int y = top + 64;
        ctx.drawCenteredTextWithShadow(tr, CbTheme.red(BulkDraw.fit(tr, e.name(), pw - 12)), px + pw / 2, y, 0xFFFFFFFF); y += 14;
        infoLine(ctx, tr, px + 6, y, "id", e.id(), pw); y += 12;
        infoLine(ctx, tr, px + 6, y, "category", e.category().isEmpty() ? "(none)" : e.category(), pw); y += 12;
        infoLine(ctx, tr, px + 6, y, "glow", String.valueOf(e.glow()), pw); y += 12;
        infoLine(ctx, tr, px + 6, y, "hardness", BulkWorkbenchModel.trimFloat(e.hardness()), pw); y += 12;
        infoLine(ctx, tr, px + 6, y, "sound", e.sound(), pw); y += 16;

        // Live status line.
        String status = (s.ticked().contains(e.id()) ? "§aselected  " : "")
                + (s.locked().contains(e.id()) ? "§clocked  " : "")
                + (s.fav().contains(e.id()) ? "§e★ favorite" : "");
        ctx.drawTextWithShadow(tr, Text.literal(status.isBlank() ? "§8(no flags)" : status.trim()), px + 6, y, 0xFFFFFFFF);
        y += 16;

        boolean tick = s.ticked().contains(e.id());
        rInfoTick = new int[]{px + 6, y, pw - 12, 16};
        BulkDraw.btn(ctx, tr, mx, my, tick ? "Untarget ✓" : "Target", rInfoTick, true, tick, false);
        y += 20;
        rInfoEditor = new int[]{px + 6, y, pw - 12, 16};
        BulkDraw.btn(ctx, tr, mx, my, "Open editor", rInfoEditor, true);
    }

    private void infoLine(DrawContext ctx, TextRenderer tr, int x, int y, String key, String value, int pw) {
        ctx.drawTextWithShadow(tr, Text.literal("§8" + key), x, y, 0xFFFFFFFF);
        int kx = x + 56;
        ctx.enableScissor(kx, y, x + pw - 12, y + 10);
        ctx.drawTextWithShadow(tr, Text.literal("§f" + value), kx, y, 0xFFFFFFFF);
        ctx.disableScissor();
    }

    // ── Foot: Select ▾ (4 options) + Clear (+ pick "Use these") ─────────────────
    private void renderFoot(DrawContext ctx, TextRenderer tr, int mx, int my, BulkWorkbenchScreen s,
                            int width, int height) {
        int footY = height - FOOT_H;
        int x0 = LX + 100;   // clear the bottom-left History button
        rSelAll = new int[]{x0, footY + 6, 90, 18};
        rClearSel = new int[]{x0 + 94, footY + 6, 62, 18};
        BulkDraw.btn(ctx, tr, mx, my, "Select ▾", rSelAll, !allMatching.isEmpty(), s.selAllOpen(), false);
        BulkDraw.btn(ctx, tr, mx, my, "Clear selection", rClearSel, !s.ticked().isEmpty());

        if (s.pickMode()) {
            rUseThese = new int[]{x0 + 160, footY + 6, 110, 18};
            BulkDraw.btn(ctx, tr, mx, my, "Use these " + s.ticked().size(), rUseThese, !s.ticked().isEmpty(), true, false);
        } else {
            rUseThese = null;
            ctx.drawTextWithShadow(tr, Text.literal("§8left-click targets · right-click info"),
                    contentRight(width) - tr.getWidth("left-click targets · right-click info"), footY + 12, 0xFFFFFFFF);
        }

        // Dropdown opens UPWARD (foot is at the bottom); drawn last so it overlays the grid. Four options.
        if (s.selAllOpen()) {
            int ow = 190, oh = 18, base = footY - 4 * oh - 8;
            ctx.fill(x0 - 2, base - 2, x0 + ow + 2, footY - 2, 0xFF000000);
            int lockedN = countFlag(s, true), favN = countFlag(s, false);
            rSelScreen = optRow(ctx, tr, mx, my, x0, base,            ow, oh, "All on screen (" + pageIds.size() + ")");
            rSelMatch  = optRow(ctx, tr, mx, my, x0, base + oh,       ow, oh, "All matching search (" + allMatching.size() + ")");
            rSelLocked = optRow(ctx, tr, mx, my, x0, base + 2 * oh,   ow, oh, "Locked only (" + lockedN + ")");
            rSelFav    = optRow(ctx, tr, mx, my, x0, base + 3 * oh,   ow, oh, "Favorites (" + favN + ")");
        } else {
            rSelScreen = null; rSelMatch = null; rSelLocked = null; rSelFav = null;
        }
    }

    private int countFlag(BulkWorkbenchScreen s, boolean locked) {
        int n = 0;
        for (String id : allMatching) if ((locked ? s.locked() : s.fav()).contains(id)) n++;
        return n;
    }

    private int[] optRow(DrawContext ctx, TextRenderer tr, int mx, int my, int x, int y, int w, int h, String label) {
        int[] r = {x, y, w, h};
        BulkDraw.btn(ctx, tr, mx, my, label, r, true);
        return r;
    }

    /**
     * One list row, shared with the Bulk Actions tab. Icon · tick · FULL id (un-clipped, A1) · category chip ·
     * flags. {@code inlinePreview} adds the Bulk tab's "old → new" on the right for targeted rows.
     */
    void drawRow(DrawContext ctx, TextRenderer tr, int mx, int my, BulkWorkbenchScreen s,
                 ClientSlotCache.Entry e, int x, int y, int w, boolean inlinePreview) {
        boolean tick = s.ticked().contains(e.id());
        boolean detail = e.id().equals(s.detailId());
        boolean hover = BulkDraw.in(mx, my, x, y, w, ROW_H);
        ctx.fill(x, y, x + w, y + ROW_H - 1, tick || detail ? BulkDraw.ROW_SEL : (hover ? BulkDraw.ROW_HOV : BulkDraw.ROW));
        if (detail) ctx.fill(x, y, x + 2, y + ROW_H - 1, CbTheme.ACCENT);

        BulkDraw.blockIcon(ctx, e.id(), x + 3, y + 2);
        ctx.fill(x + 22, y + 6, x + 31, y + 15, 0xFF000000);
        ctx.fill(x + 23, y + 7, x + 30, y + 14, tick ? CbTheme.ACCENT : 0xFF1A1A1A);

        int flagsRight = x + w - 24;
        int bandRight = inlinePreview ? x + (int) (w * 0.44) : flagsRight;
        ctx.enableScissor(x + 34, y, bandRight, y + ROW_H);
        int cx = x + 34;
        ctx.drawTextWithShadow(tr, Text.literal((tick ? "§f" : "§7") + e.id()), cx, y + 6, 0xFFFFFFFF);
        cx += tr.getWidth(e.id()) + 6;
        if (e.category() != null && !e.category().isEmpty()) BulkDraw.chip(ctx, tr, cx, y + 5, e.category());
        ctx.disableScissor();

        int fx = flagsRight;
        if (s.locked().contains(e.id())) { BulkDraw.padlock(ctx, fx, y + 5); fx += 10; }
        if (s.fav().contains(e.id())) ctx.drawTextWithShadow(tr, Text.literal("§e★"), fx, y + 5, 0xFFFFFFFF);

        if (inlinePreview) {
            String text = BulkOpText.inlinePreview(s, e.id());
            int px = x + (int) (w * 0.46);
            ctx.enableScissor(px, y, x + w - 6, y + ROW_H);
            if (text != null) ctx.drawTextWithShadow(tr, Text.literal(text), px, y + 6, 0xFFFFFFFF);
            ctx.disableScissor();
        }
    }
}
