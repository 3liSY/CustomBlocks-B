/**
 * CbPopupPicker.java — GROUP 27 UI kit. Shared "pick from a list" popup. CLIENT-SIDE ONLY.
 *
 * A small red+black modal list: a red header with a close [X], a scrollable list of choices (label,
 * optional swatch colour, optional count), and — when a non-empty "new name" candidate is supplied —
 * a lime "+ Create '<name>'" row at the bottom. While open it swallows every click except a row / the
 * [X]. One instance per screen; the owning screen calls open(...), then render()/mouseClicked() first
 * in its own render/mouseClicked, and stops when this reports the click consumed.
 *
 * Opaque scrim uses the same immediate-flush trick as CbHelpOverlay so no background TEXT bleeds through.
 *
 * Depends on: DrawContext / TextRenderer, CbTheme. Called by: CategoryHubScreen (merge / move target).
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public final class CbPopupPicker {

    /** One choice: the value passed back on pick, a display label, a swatch colour (0 = none), a count (<0 = hide). */
    public record Item(String value, String label, int swatchArgb, int count) {}

    private static final int EDGE = CbTheme.ACCENT;
    private static final int PANEL_BG = 0xFF000000, HEAD_BG = 0xFF1A1A1A;
    private static final int ROW_H = 15, HEAD_H = 20, PAD = 6, MAX_ROWS = 9, X_SIZE = 14;

    private boolean open;
    private String title = "";
    private String newCandidate = ""; // normalised id for the "+ Create" row; "" = no create row
    private String newLabel = "";     // pretty label for the create row
    private final List<Item> items = new ArrayList<>();
    private Consumer<String> onChoose = v -> {};
    private int scroll;

    // Hit-boxes recomputed every render.
    private int px, py, pw, ph;
    private int xx0, xy0, xx1, xy1;
    private final List<int[]> rowRects = new ArrayList<>();
    private final List<String> rowValues = new ArrayList<>();

    public boolean isOpen() { return open; }
    public void close()     { open = false; }

    /** Open the picker. {@code newCandidate}/{@code newLabel} blank = no create row. */
    public void open(String title, List<Item> items, String newCandidate, String newLabel, Consumer<String> onChoose) {
        this.title = title;
        this.items.clear();
        this.items.addAll(items);
        this.newCandidate = newCandidate == null ? "" : newCandidate;
        this.newLabel = newLabel == null ? "" : newLabel;
        this.onChoose = onChoose == null ? v -> {} : onChoose;
        this.scroll = 0;
        this.open = true;
    }

    /** Call first in the screen's mouseClicked. Returns true when consumed (picker is modal). */
    public boolean mouseClicked(double mx, double my) {
        if (!open) return false;
        if (mx >= xx0 && mx <= xx1 && my >= xy0 && my <= xy1) { open = false; return true; }
        for (int i = 0; i < rowRects.size(); i++) {
            int[] r = rowRects.get(i);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                String v = rowValues.get(i);
                open = false;
                onChoose.accept(v);
                return true;
            }
        }
        return true; // swallow every other click while open
    }

    public boolean mouseScrolled(double vAmt) {
        if (!open) return false;
        scroll = Math.max(0, Math.min(maxScroll(), scroll + (int) -Math.signum(vAmt)));
        return true;
    }

    private boolean hasCreate() { return !newCandidate.isEmpty(); }
    private int totalRows() { return items.size() + (hasCreate() ? 1 : 0); }
    private int maxScroll() { return Math.max(0, totalRows() - MAX_ROWS); }

    public void render(DrawContext ctx, int sw, int sh, TextRenderer tr, int mx, int my) {
        if (!open) return;
        rowRects.clear(); rowValues.clear();

        int visible = Math.min(MAX_ROWS, totalRows());
        pw = 240;
        ph = HEAD_H + PAD + visible * ROW_H + PAD;
        px = (sw - pw) / 2; py = (sh - ph) / 2;

        // Opaque scrim, drawn IMMEDIATE (see CbImmediateFill) so background text can't bleed through it — §K7.
        CbImmediateFill.fill(ctx, 0, 0, sw, sh, 0xCC000000);
        ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, EDGE);
        ctx.fill(px, py, px + pw, py + ph, PANEL_BG);

        // Header + close [X].
        ctx.fill(px, py, px + pw, py + HEAD_H, HEAD_BG);
        ctx.fill(px, py + HEAD_H - 1, px + pw, py + HEAD_H, EDGE);
        ctx.drawTextWithShadow(tr, CbTheme.red(title), px + PAD, py + 6, 0xFFFFFFFF);
        xx1 = px + pw - 4; xx0 = xx1 - X_SIZE; xy0 = py + 3; xy1 = xy0 + X_SIZE;
        boolean hov = mx >= xx0 && mx <= xx1 && my >= xy0 && my <= xy1;
        ctx.fill(xx0, xy0, xx1, xy1, hov ? 0xFFE53C3C : 0xFFC02A2A);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("§f§lX"), (xx0 + xx1) / 2, xy0 + 3, 0xFFFFFFFF);

        // Rows.
        int y = py + HEAD_H + PAD;
        for (int i = 0; i < visible; i++) {
            int idx = i + scroll;
            int rx = px + PAD, rw = pw - PAD * 2, ry = y + i * ROW_H;
            boolean rowHov = mx >= rx && mx < rx + rw && my >= ry && my < ry + ROW_H - 1;
            boolean create = hasCreate() && idx == items.size();
            ctx.fill(rx, ry, rx + rw, ry + ROW_H - 1, rowHov ? 0xFF241015 : 0xFF121212);
            if (create) {
                ctx.drawTextWithShadow(tr, Text.literal("§a+ Create '" + newLabel + "'"), rx + 5, ry + 3, 0xFFFFFFFF);
                rowRects.add(new int[]{rx, ry, rw, ROW_H - 1}); rowValues.add(newCandidate);
            } else if (idx < items.size()) {
                Item it = items.get(idx);
                if (it.swatchArgb() != 0) {
                    ctx.fill(rx + 4, ry + 3, rx + 12, ry + 11, 0xFF000000);
                    ctx.fill(rx + 5, ry + 4, rx + 11, ry + 10, it.swatchArgb());
                }
                ctx.drawTextWithShadow(tr, Text.literal("§f" + it.label()), rx + 16, ry + 3, 0xFFFFFFFF);
                if (it.count() >= 0)
                    ctx.drawTextWithShadow(tr, Text.literal("§8" + it.count()), rx + rw - 24, ry + 3, 0xFFFFFFFF);
                rowRects.add(new int[]{rx, ry, rw, ROW_H - 1}); rowValues.add(it.value());
            }
        }
        if (maxScroll() > 0)
            ctx.drawTextWithShadow(tr, Text.literal("§8scroll ▲▼"), px + pw - 52, py + ph - 10, 0xFFFFFFFF);
    }
}
