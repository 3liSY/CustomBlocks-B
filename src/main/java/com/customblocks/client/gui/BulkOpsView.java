/**
 * BulkOpsView.java — Group 07 (Bulk Operations Hub — Bulk Actions tab). CLIENT-ONLY.
 *
 * The §G27.22b layout (design LOCKED 2026-07-12): the ten ops live on the LEFT rail under a red "— ACTIONS —"
 * separator (below the two Hub tabs the chrome draws); the active op shows an op-header strip (icon + name +
 * description + inline controls) and an "affects N of T" line; the center is one row per TARGETED block with an
 * include checkbox, an OLD rotating cube in a fixed cell, name + old value, a red → , a NEW cube and the new
 * value in lime (or "will skip"/"removed"); a right RESULT-PREVIEW panel (companion {@link BulkResultPanel})
 * shows a before→after sample + SUMMARY + green bar. Right-click is disabled on this tab (Screen swallows it).
 *
 * Like {@link BulkWorkbenchView} this owns geometry and records a hit-rect for every clickable element into
 * public fields the screen reads the same frame; the layout methods are static so the screen can place its text
 * fields at the exact numbers this class draws labels at.
 *
 * Depends on: BulkDraw, BulkOpSpec, BulkWorkbenchModel, BulkOpText, BulkCube, BulkResultPanel, CbTheme.
 * Called by: BulkWorkbenchScreen (render + hit-rect read-back).
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

import static com.customblocks.client.gui.BulkWorkbenchView.CONTENT_X;
import static com.customblocks.client.gui.BulkWorkbenchView.CONTENT_Y;
import static com.customblocks.client.gui.BulkWorkbenchView.LX;
import static com.customblocks.client.gui.BulkWorkbenchView.contentBottom;
import static com.customblocks.client.gui.BulkWorkbenchView.contentRight;

@Environment(EnvType.CLIENT)
final class BulkOpsView {

    // ── geometry ───────────────────────────────────────────────────────────────
    static final int PANEL_W = 156;                                   // right RESULT-PREVIEW panel
    static int rowsRight(int width) { return contentRight(width) - PANEL_W - 6; }

    static final int HDR_H = 32, BROW_H = 24;                          // op-header height, action-row height
    static int listTop() { return CONTENT_Y + HDR_H + 12; }
    static int listBottom(int height) { return contentBottom(height); }

    // op-header inline control coords — the screen places its text fields at exactly these numbers.
    static int ctlY() { return CONTENT_Y + 8; }
    static int ctlX(int slot) { return CONTENT_X + 120 + slot * 104; }
    static int ctlW() { return 96; }

    // left op rail (below the two Hub tabs + the "— ACTIONS —" separator the chrome leaves room for).
    private static final int RAIL_TOP = BulkWorkbenchView.TAB_TOP
            + BulkWorkbenchView.TABS.length * (BulkWorkbenchView.TAB_H + BulkWorkbenchView.TAB_GAP) + 16;
    private static final int OP_BTN_H = 15, OP_GAP = 2;

    // ── hit rects (recomputed every render, read back the same frame) ────────────
    final int[][] railRects = new int[BulkOpSpec.OP_COUNT][];
    final List<int[]> rowRects = new ArrayList<>();       // include-checkbox cells
    final List<String> rowIds = new ArrayList<>();
    int[] rOptA, rOptB, rHue;                             // header cyclers + the Recolor hue slider
    int[] rUndo, rRedo, rApply;                          // footer
    int[] rListScroll;
    int[] rModalConfirm, rModalCancel, rResultOk;
    int curListTop;                                      // real list top this frame (read by mouseScrolled)

    private int listScroll;
    private final List<String> pageIds = new ArrayList<>();
    private int barTop, barH, barTotal, barVisible;      // A2 scrollbar-drag geometry
    private final BulkResultPanel resultPanel = new BulkResultPanel();

    void scrollList(int step) { listScroll = Math.max(0, listScroll + step); }
    void resetScrolls() { listScroll = 0; }
    List<String> pageIds() { return pageIds; }

    /** Drag the list scrollbar (A2): map mouse-Y over the track to a scroll offset. */
    void scrollToMouse(double my) {
        int max = Math.max(0, barTotal - barVisible);
        if (max == 0 || barH <= 0) return;
        int idx = (int) Math.round((my - barTop) / (double) barH * max);
        listScroll = BulkDraw.clamp(idx, 0, max);
    }

    // ── render ───────────────────────────────────────────────────────────────
    void render(DrawContext ctx, TextRenderer tr, int mx, int my, BulkWorkbenchScreen s, int width, int height) {
        rHue = null; rOptA = null; rOptB = null;
        renderRail(ctx, tr, mx, my, s);
        renderHeader(ctx, tr, mx, my, s, width);
        renderRows(ctx, tr, mx, my, s, width, height);
        resultPanel.render(ctx, tr, s, width, height);
        renderFoot(ctx, tr, mx, my, s, width, height);
    }

    // ── left op rail ───────────────────────────────────────────────────────────
    private void renderRail(DrawContext ctx, TextRenderer tr, int mx, int my, BulkWorkbenchScreen s) {
        ctx.drawTextWithShadow(tr, CbTheme.red("— ACTIONS —"), LX, RAIL_TOP - 12, 0xFFFFFFFF);
        for (int i = 0; i < BulkOpSpec.RAIL_ORDER.length; i++) {
            int op = BulkOpSpec.RAIL_ORDER[i];
            int x = LX, y = RAIL_TOP + i * (OP_BTN_H + OP_GAP);
            railRects[op] = new int[]{x, y, BulkWorkbenchView.RAIL_W, OP_BTN_H};
            railBtn(ctx, tr, mx, my, op, railRects[op], s.opIndex() == op);
        }
    }

    /** A left-aligned rail button: pixel op-icon + label; active = red, Delete always muted-red (danger cue). */
    private void railBtn(DrawContext ctx, TextRenderer tr, int mx, int my, int op, int[] r, boolean active) {
        boolean danger = op == BulkOpSpec.OP_DELETE;
        boolean hover = BulkDraw.in(mx, my, r);
        int bg = active ? CbTheme.SEL_FILL : danger ? 0xFF2A0A0A : hover ? 0xFF262626 : 0xFF161616;
        ctx.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, active || danger ? CbTheme.ACCENT : 0xFF000000);
        ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], bg);
        opIcon(ctx, op, r[0] + 3, r[1] + 4);
        String col = danger ? "§c" : active ? "§f" : "§7";
        ctx.drawTextWithShadow(tr, Text.literal(col + BulkOpSpec.RAIL[op]), r[0] + 15, r[1] + 4, 0xFFFFFFFF);
    }

    /** Tiny hand-drawn 8×7 pixel op-icon (B11 ruling — MC font lacks these codepoints). */
    private void opIcon(DrawContext ctx, int op, int x, int y) {
        int a = CbTheme.ACCENT, w = 0xFFFFFFFF, g = 0xFF9AA0A6, lime = CbTheme.LIME;
        switch (op) {
            case BulkOpSpec.OP_PROPERTY -> { ctx.fill(x, y + 5, x + 2, y + 7, w); ctx.fill(x + 2, y + 1, x + 6, y + 5, g); ctx.fill(x + 5, y, x + 7, y + 2, w); } // pencil
            case BulkOpSpec.OP_RECOLOR  -> { ctx.fill(x, y, x + 7, y + 7, g); ctx.fill(x + 1, y + 1, x + 3, y + 3, a); ctx.fill(x + 4, y + 3, x + 6, y + 5, lime); } // palette
            case BulkOpSpec.OP_RENAME   -> { ctx.fill(x, y, x + 7, y + 1, w); ctx.fill(x + 3, y, x + 4, y + 7, w); }   // "T"
            case BulkOpSpec.OP_CATEGORY -> { ctx.fill(x, y + 1, x + 3, y + 2, g); ctx.fill(x, y + 2, x + 7, y + 7, g); } // folder
            case BulkOpSpec.OP_DUPLICATE-> { ctx.fill(x, y, x + 5, y + 5, g); ctx.fill(x + 2, y + 2, x + 7, y + 7, w); } // two cards
            case BulkOpSpec.OP_REID     -> { ctx.fill(x + 1, y, x + 2, y + 7, w); ctx.fill(x + 4, y, x + 5, y + 7, w); ctx.fill(x, y + 2, x + 7, y + 3, w); ctx.fill(x, y + 4, x + 7, y + 5, w); } // "#"
            case BulkOpSpec.OP_LOCK     -> { ctx.fill(x + 1, y, x + 6, y + 1, w); ctx.fill(x + 1, y, x + 2, y + 3, w); ctx.fill(x + 5, y, x + 6, y + 3, w); ctx.fill(x, y + 3, x + 7, y + 7, a); } // padlock
            case BulkOpSpec.OP_FAVORITE -> { ctx.fill(x + 3, y, x + 4, y + 7, 0xFFFFD000); ctx.fill(x, y + 3, x + 7, y + 4, 0xFFFFD000); ctx.fill(x + 1, y + 1, x + 6, y + 6, 0xFFFFD000); } // star-ish
            case BulkOpSpec.OP_EXPORT   -> { ctx.fill(x + 3, y, x + 4, y + 5, lime); ctx.fill(x + 1, y + 2, x + 6, y + 3, lime); ctx.fill(x, y + 6, x + 7, y + 7, g); } // out-arrow
            case BulkOpSpec.OP_DELETE   -> { ctx.fill(x, y, x + 7, y + 1, 0xFFAA0000); ctx.fill(x + 1, y + 1, x + 6, y + 7, 0xFFAA0000); ctx.fill(x + 2, y + 2, x + 3, y + 6, 0xFF000000); ctx.fill(x + 4, y + 2, x + 5, y + 6, 0xFF000000); } // trash
            default -> ctx.fill(x, y, x + 7, y + 7, g);
        }
    }

    // ── op-header strip ─────────────────────────────────────────────────────────
    private void renderHeader(DrawContext ctx, TextRenderer tr, int mx, int my, BulkWorkbenchScreen s, int width) {
        int op = s.opIndex();
        opIcon(ctx, op, CONTENT_X, CONTENT_Y + 1);
        ctx.drawTextWithShadow(tr, CbTheme.red(BulkOpSpec.TITLE[op]), CONTENT_X + 12, CONTENT_Y, 0xFFFFFFFF);
        int descRight = ctlX(0) - 8;
        ctx.enableScissor(CONTENT_X, CONTENT_Y + 11, descRight, CONTENT_Y + 22);
        ctx.drawTextWithShadow(tr, Text.literal("§7" + BulkOpSpec.DESC[op]), CONTENT_X, CONTENT_Y + 12, 0xFFFFFFFF);
        ctx.disableScissor();

        renderControls(ctx, tr, mx, my, s, op, width);

        // "affects N of T · locked skipped" — N updates live with the checkboxes; scissored so it can never run
        // under the control column on the right (NO_TEXT_OVERLAP — clip by edge, the codebase's X2 pattern).
        int[] impact = BulkOpText.impact(s);
        int total = s.ticked().size();
        String line = "§faffects §a" + impact[0] + "§f of " + total
                + (impact[1] > 0 ? "  §8·§6 " + impact[1] + " locked skipped" : "")
                + (impact[2] > 0 ? "  §8·§c " + impact[2] + " problem" : "");
        int ay = CONTENT_Y + HDR_H - 8;
        ctx.enableScissor(CONTENT_X, ay, ctlX(0) - 8, ay + 10);
        ctx.drawTextWithShadow(tr, Text.literal(line), CONTENT_X, ay, 0xFFFFFFFF);
        ctx.disableScissor();
    }

    private void renderControls(DrawContext ctx, TextRenderer tr, int mx, int my, BulkWorkbenchScreen s, int op, int width) {
        switch (op) {
            case BulkOpSpec.OP_PROPERTY -> {
                rOptA = cycler(ctx, tr, mx, my, 0, "Setting", s.property());
                rOptB = cycler(ctx, tr, mx, my, 1, "Value", s.value());
            }
            case BulkOpSpec.OP_RECOLOR -> renderHueSlider(ctx, tr, mx, my, s, width);
            case BulkOpSpec.OP_RENAME -> {
                rOptA = cycler(ctx, tr, mx, my, 0, "Mode", s.textMode());
                // textA (+ textB for replace) are real fields drawn by the screen at ctlX(1)/ctlX(2).
                label(ctx, tr, 1, switch (s.textMode()) { case "prefix" -> "Add to start"; case "suffix" -> "Add to end"; default -> "Find"; });
                if ("replace".equals(s.textMode())) label(ctx, tr, 2, "Replace with");
                example(ctx, tr, s, width);
            }
            case BulkOpSpec.OP_CATEGORY -> label(ctx, tr, 1, "Category  §7(\"none\" clears)");
            case BulkOpSpec.OP_REID -> { label(ctx, tr, 1, "Pattern  §7{n} numbers"); example(ctx, tr, s, width); }
            case BulkOpSpec.OP_EXPORT -> rOptA = cycler(ctx, tr, mx, my, 0, "Format", s.exportFormat());
            case BulkOpSpec.OP_LOCK -> rOptA = cycler(ctx, tr, mx, my, 0, "Action", s.lockMode());
            case BulkOpSpec.OP_FAVORITE -> rOptA = cycler(ctx, tr, mx, my, 0, "Action", s.favMode());
            case BulkOpSpec.OP_DELETE -> ctx.drawTextWithShadow(tr,
                    Text.literal("§c⚠ removes the blocks and their placed copies — one Undo restores"), ctlX(0), ctlY() + 3, 0xFFFFFFFF);
            default -> { }
        }
    }

    private int[] cycler(DrawContext ctx, TextRenderer tr, int mx, int my, int slot, String label, String value) {
        int x = ctlX(slot), y = ctlY();
        ctx.drawTextWithShadow(tr, Text.literal("§8" + label), x, y - 9, 0xFFFFFFFF);
        int[] r = {x, y, ctlW(), 16};
        BulkDraw.btn(ctx, tr, mx, my, value + "  ▾", r, true);
        return r;
    }

    private void label(DrawContext ctx, TextRenderer tr, int slot, String text) {
        ctx.drawTextWithShadow(tr, Text.literal("§8" + text), ctlX(slot), ctlY() - 9, 0xFFFFFFFF);
    }

    /** Recolor hue slider + a result swatch (§G27.22b). Clicking/dragging the bar sets the hue. */
    private void renderHueSlider(DrawContext ctx, TextRenderer tr, int mx, int my, BulkWorkbenchScreen s, int width) {
        int x = ctlX(0), y = ctlY(), h = 12;
        int w = Math.max(60, Math.min(ctlW() + 60, rowsRight(width) - x - 56));  // keep the bar + swatch clear of the panel
        ctx.drawTextWithShadow(tr, Text.literal("§8Hue"), x, y - 9, 0xFFFFFFFF);
        // a coarse rainbow strip
        for (int i = 0; i < w; i++) ctx.fill(x + i, y, x + i + 1, y + h, 0xFF000000 | hsb(i * 360f / w));
        int hue = s.recolorHue();
        int tx = x + (int) (hue / 360f * w);
        ctx.fill(tx - 1, y - 2, tx + 1, y + h + 2, 0xFFFFFFFF);   // thumb
        rHue = new int[]{x, y - 2, w, h + 4};
        // result swatch + degrees
        ctx.fill(x + w + 8, y - 1, x + w + 28, y + h + 1, 0xFF000000 | hsb(hue));
        ctx.drawTextWithShadow(tr, Text.literal("§a" + hue + "°"), x + w + 32, y + 2, 0xFFFFFFFF);
    }

    private void example(DrawContext ctx, TextRenderer tr, BulkWorkbenchScreen s, int width) {
        String ex = BulkOpText.example(s);
        if (ex.isEmpty()) return;
        int y = ctlY() + 18;
        ctx.enableScissor(CONTENT_X, y, rowsRight(width), y + 10);
        ctx.drawTextWithShadow(tr, Text.literal(ex), ctlX(0), y, 0xFFFFFFFF);
        ctx.disableScissor();
    }

    // ── center rows ─────────────────────────────────────────────────────────────
    private void renderRows(DrawContext ctx, TextRenderer tr, int mx, int my, BulkWorkbenchScreen s, int width, int height) {
        rowRects.clear(); rowIds.clear(); pageIds.clear();
        int x = CONTENT_X, y = listTop(), w = rowsRight(width) - CONTENT_X, bottom = listBottom(height);
        curListTop = y;
        BulkDraw.panel(ctx, x, y, w, bottom - y);

        List<String> targets = new ArrayList<>(s.ticked());
        if (targets.isEmpty()) {
            ctx.drawTextWithShadow(tr, Text.literal("§8(tick blocks on the Blocks List tab, then pick an action)"), x + 6, y + 6, 0xFFFFFFFF);
            rListScroll = null;
            return;
        }
        int visible = Math.max(1, (bottom - y) / BROW_H);
        listScroll = BulkDraw.clamp(listScroll, 0, Math.max(0, targets.size() - visible));
        barTop = y; barH = bottom - y; barTotal = targets.size(); barVisible = visible;
        ctx.enableScissor(x, y, x + w, bottom);
        for (int i = 0; i < visible && i + listScroll < targets.size(); i++) {
            String id = targets.get(i + listScroll);
            int ry = y + i * BROW_H;
            drawActionRow(ctx, tr, mx, my, s, id, x, ry, w);
            rowRects.add(new int[]{x + 2, ry + 4, 14, BROW_H - 8});   // the include-checkbox hit cell
            rowIds.add(id);
            pageIds.add(id);
        }
        ctx.disableScissor();
        rListScroll = BulkDraw.vscroll(ctx, x + w - 4, y, bottom - y, targets.size(), visible, listScroll);
    }

    /** One targeted-block row: include ☑, OLD cube (fixed cell) + name/old-value → NEW cube + lime new-value. */
    private void drawActionRow(DrawContext ctx, TextRenderer tr, int mx, int my, BulkWorkbenchScreen s,
                               String id, int x, int y, int w) {
        ClientSlotCache.Entry e = BulkWorkbenchModel.byId(id);
        if (e == null) return;
        boolean included = !s.excluded().contains(id);
        boolean locked = s.locked().contains(id);
        boolean autoSkip = locked && BulkOpSpec.skipsLocked(s.opIndex());
        boolean dim = !included || autoSkip;
        BulkWorkbenchModel.PreviewRow p = included ? s.previewFor(id) : null;

        ctx.fill(x, y, x + w, y + BROW_H - 1, dim ? 0xFF0E0E0E : BulkDraw.ROW);
        // left stripe: red when included & active, grey when excluded/auto-skip.
        ctx.fill(x, y, x + 2, y + BROW_H - 1, dim ? 0xFF555555 : CbTheme.ACCENT);

        // include checkbox (or a padlock for the auto-skip locked row).
        int cbx = x + 3, cby = y + (BROW_H - 10) / 2;
        if (autoSkip) {
            BulkDraw.padlock(ctx, cbx, cby - 1);
        } else {
            ctx.fill(cbx, cby, cbx + 10, cby + 10, 0xFF000000);
            ctx.fill(cbx + 1, cby + 1, cbx + 9, cby + 9, included ? 0xFF1A1A1A : 0xFF0E0E0E);
            if (included) { ctx.drawTextWithShadow(tr, Text.literal("§a✓"), cbx + 1, cby + 1, 0xFFFFFFFF); }
        }

        // OLD cube in a fixed cell, then name + old value (two clipped lines).
        int oldCubeX = x + 28;
        s.cube().render(ctx, id, oldCubeX, y + BROW_H / 2, 9);
        int textX = oldCubeX + 14;
        int arrowX = x + (int) (w * 0.52);
        ctx.enableScissor(textX, y, arrowX - 6, y + BROW_H);
        ctx.drawTextWithShadow(tr, Text.literal((dim ? "§8" : "§f") + e.name()), textX, y + 3, 0xFFFFFFFF);
        String oldVal = p != null ? p.before() : (locked ? "" : e.id());
        ctx.drawTextWithShadow(tr, Text.literal("§8" + oldVal), textX, y + 13, 0xFFFFFFFF);
        ctx.disableScissor();

        // red arrow separator.
        ctx.drawTextWithShadow(tr, CbTheme.red("→"), arrowX, y + 8, 0xFFFFFFFF);

        // NEW cube (dimmed for Delete) + new value / skip / removed.
        int newCubeX = arrowX + 18;
        s.cube().render(ctx, id, newCubeX, y + BROW_H / 2, 9);
        int nvX = newCubeX + 14;
        ctx.enableScissor(nvX, y, x + w - 4, y + BROW_H);
        String newText;
        if (!included)            newText = "§8excluded";
        else if (p == null)       newText = "§8—";
        else if (p.skipped())     newText = "§6" + ("locked".equals(p.skipReason()) ? "will skip (locked)" : p.skipReason());
        else if (s.opIndex() == BulkOpSpec.OP_DELETE) newText = "§cremoved";
        else                      newText = "§a" + p.after();
        ctx.drawTextWithShadow(tr, Text.literal(newText), nvX, y + 3, 0xFFFFFFFF);
        if (included && p != null && !p.skipped() && s.opIndex() != BulkOpSpec.OP_DELETE)
            ctx.drawTextWithShadow(tr, Text.literal("§8after"), nvX, y + 13, 0xFFFFFFFF);
        ctx.disableScissor();
    }

    // ── footer: Undo / Redo (right of the chrome's History) + Execute on N ───────
    private void renderFoot(DrawContext ctx, TextRenderer tr, int mx, int my, BulkWorkbenchScreen s, int width, int height) {
        int footY = height - BulkWorkbenchView.FOOT_H;
        rUndo = new int[]{LX + 100, footY + 6, 44, 18};
        rRedo = new int[]{LX + 148, footY + 6, 44, 18};
        BulkDraw.btn(ctx, tr, mx, my, "↶ Undo", rUndo, true);
        BulkDraw.btn(ctx, tr, mx, my, "↷ Redo", rRedo, true);

        int n = BulkOpText.impact(s)[0];
        rApply = new int[]{width - 160, footY + 6, 152, 18};
        BulkDraw.btn(ctx, tr, mx, my, "⚡ Execute on " + n, rApply, s.canApply(), false, BulkOpSpec.destructive(s.opIndex()));
    }

    // ── confirm modal — opaque (OPAQUE_MODALS, no backdrop override) ─────────────
    void renderModal(DrawContext ctx, TextRenderer tr, int mx, int my, BulkWorkbenchScreen s, int width, int height) {
        ctx.fill(0, 0, width, height, CbTheme.DIALOG_BG);
        int w = Math.min(360, width - 40), h = Math.min(180, height - 60);
        int x = (width - w) / 2, y = (height - h) / 2;
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, CbTheme.ACCENT);
        ctx.fill(x, y, x + w, y + h, CbTheme.DIALOG_BG);

        int n = BulkOpText.impact(s)[0], skip = BulkOpText.impact(s)[1];
        boolean danger = BulkOpSpec.destructive(s.opIndex());
        ctx.drawCenteredTextWithShadow(tr, CbTheme.red(BulkOpSpec.confirmVerb(s.opIndex()) + " " + n
                + " block" + (n == 1 ? "" : "s") + "?"), x + w / 2, y + 14, 0xFFFFFFFF);
        String body = "Applies " + BulkOpSpec.RAIL[s.opIndex()] + " to " + n + " selected block" + (n == 1 ? "" : "s")
                + ". One Undo reverts." + (skip > 0 ? " " + skip + " locked block" + (skip == 1 ? "" : "s") + " skipped." : "");
        int ty = y + 34;
        for (net.minecraft.text.OrderedText ln : tr.wrapLines(Text.literal("§7" + body), w - 28)) {
            ctx.drawCenteredTextWithShadow(tr, ln, x + w / 2, ty, 0xFFFFFFFF); ty += 11;
        }
        ctx.drawCenteredTextWithShadow(tr, Text.literal("§8" + BulkOpText.summary(s)), x + w / 2, ty + 2, 0xFFFFFFFF);

        rModalCancel = new int[]{x + 8, y + h - 26, (w - 24) / 2, 18};
        rModalConfirm = new int[]{x + w / 2 + 4, y + h - 26, (w - 24) / 2, 18};
        BulkDraw.btn(ctx, tr, mx, my, "Cancel", rModalCancel, true);
        BulkDraw.btn(ctx, tr, mx, my, danger ? "Confirm delete" : "Confirm", rModalConfirm, true, !danger, danger);
    }

    // ── execute sweep — red L→R wipe + "applying c/N" count-up (§G27.22b) ─────────
    void renderSweep(DrawContext ctx, TextRenderer tr, BulkWorkbenchScreen s, int width, int height) {
        float prog = s.executeProgress();      // 0..1
        int n = s.executeCount();
        int x = CONTENT_X, y = listTop(), w = rowsRight(width) - CONTENT_X, bottom = listBottom(height);
        int sweepX = x + (int) (w * Math.min(1f, prog));
        ctx.fill(x, y, sweepX, bottom, 0x66FF0000);
        ctx.fill(sweepX - 2, y, sweepX, bottom, CbTheme.ACCENT);
        int c = Math.min(n, (int) Math.ceil(prog * n));
        String msg = prog >= 1f ? "§a✓ " + BulkOpSpec.RAIL[s.opIndex()] + " applied on " + n
                : "§fapplying " + c + "/" + n;
        ctx.drawCenteredTextWithShadow(tr, Text.literal(msg), x + w / 2, (y + bottom) / 2, 0xFFFFFFFF);
    }

    // ── result modal — X3 dual feedback ──────────────────────────────────────────
    void renderResultModal(DrawContext ctx, TextRenderer tr, String msg, int mx, int my, int width, int height) {
        ctx.fill(0, 0, width, height, CbTheme.DIALOG_BG);
        int w = Math.min(320, width - 40), h = Math.min(150, height - 60);
        int x = (width - w) / 2, y = (height - h) / 2;
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, CbTheme.ACCENT);
        ctx.fill(x, y, x + w, y + h, CbTheme.DIALOG_BG);
        ctx.drawCenteredTextWithShadow(tr, CbTheme.red("Result"), x + w / 2, y + 10, 0xFFFFFFFF);
        int ty = y + 30;
        for (net.minecraft.text.OrderedText ln : tr.wrapLines(Text.literal("§f" + msg), w - 24)) {
            if (ty > y + h - 30) break;
            ctx.drawCenteredTextWithShadow(tr, ln, x + w / 2, ty, 0xFFFFFFFF);
            ty += 11;
        }
        rResultOk = new int[]{x + w / 2 - 40, y + h - 26, 80, 18};
        BulkDraw.btn(ctx, tr, mx, my, "OK", rResultOk, true, true, false);
    }

    /** Pack a full-saturation, full-value HSB hue (degrees) to 0xRRGGBB. */
    static int hsb(float hueDeg) {
        float h = (hueDeg / 60f) % 6f;
        float f = h - (float) Math.floor(h);
        int v = 255, p = 0, q = (int) (255 * (1 - f)), t = (int) (255 * f);
        return switch ((int) h) {
            case 0 -> (v << 16) | (t << 8);
            case 1 -> (q << 16) | (v << 8);
            case 2 -> (v << 8) | t;
            case 3 -> (q << 8) | v;
            case 4 -> (t << 16) | v;
            default -> (v << 16) | q;
        };
    }
}
