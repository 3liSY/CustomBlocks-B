/**
 * CbHelpOverlay.java — shared [?] shortcut popup for Group 27 cube screens. CLIENT-ONLY.
 *
 * G27.7 §A6 ([?] revamp): replaces the per-screen copy-pasted renderHelp (a fixed ~320px box, so the
 * long shortcut lines overflowed; AND any click dismissed it). This is one organised, auto-sized popup:
 * a header bar with the title + a red [X] close button, then key/description rows grouped under VIEW /
 * EDIT headings. While it is open every click is swallowed — ONLY the red [X] (or Esc) closes it, so a
 * stray click no longer makes it vanish.
 *
 * Stateful: ONE instance per screen — holds open/closed + the [X] hit-box (recomputed each render).
 * Built once from the screen's title + its shortcut groups.
 * Depends on: DrawContext / TextRenderer.
 * Called by: ArabicPreviewScreen, RecolorSliderScreen, ShapeEditorScreen (G27 cube screens).
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class CbHelpOverlay {

    private static final int GOLD     = 0xFF_FF_AA_00;
    private static final int PANEL_BG = 0xF2_16_16_1A; // near-opaque so the world behind doesn't bleed through
    private static final int HEAD_BG  = 0xFF_23_23_2A;
    private static final int X_BG     = 0xFF_C0_2A_2A; // red close button
    private static final int X_BG_HOV = 0xFF_E5_3C_3C;
    private static final int PAD = 11, ROW_H = 12, HEAD_H = 22, COLGAP = 12, X_SIZE = 14;

    /** One shortcut line: a key / combo and what it does. */
    public record Row(String keys, String desc) {}
    /** A titled group of rows (e.g. "VIEW", "EDIT"). */
    public record Group(String heading, List<Row> rows) {}

    private final String title;
    private final List<Group> groups;
    private boolean open;
    // red [X] hit-box, recomputed every render.
    private int xx0, xy0, xx1, xy1;

    public CbHelpOverlay(String title, List<Group> groups) {
        this.title = title;
        this.groups = groups;
    }

    public boolean isOpen() { return open; }
    public void toggle()    { open = !open; }
    public void open()      { open = true; }
    public void close()     { open = false; }

    /**
     * Call first in the screen's mouseClicked. Returns true if the click was consumed (overlay open) —
     * the screen must then stop. Closes ONLY when the red [X] is hit; every other click is swallowed.
     */
    public boolean mouseClicked(double mx, double my) {
        if (!open) return false;
        if (mx >= xx0 && mx <= xx1 && my >= xy0 && my <= xy1) open = false;
        return true;
    }

    public void render(DrawContext ctx, int sw, int sh, TextRenderer tr, int mx, int my) {
        if (!open) return;

        // ── measure: widest key column + widest description column, total row count ──
        int keyW = 0, descW = 0, rows = 0;
        for (Group g : groups) {
            rows += 1 + g.rows().size();                 // heading row + its rows
            for (Row r : g.rows()) {
                keyW = Math.max(keyW, tr.getWidth(r.keys()));
                descW = Math.max(descW, tr.getWidth(r.desc()));
            }
        }
        int contentW = PAD + keyW + COLGAP + descW + PAD;
        int titleW   = PAD + tr.getWidth(title + " - Shortcuts") + 10 + X_SIZE + 4;
        int pw = Math.max(240, Math.max(contentW, titleW));
        int ph = HEAD_H + PAD + rows * ROW_H + 6 + ROW_H + PAD; // body + footer line
        int x0 = (sw - pw) / 2, y0 = (sh - ph) / 2;

        // ── panel + gold border ──
        ctx.fill(x0 - 1, y0 - 1, x0 + pw + 1, y0 + ph + 1, GOLD);
        ctx.fill(x0, y0, x0 + pw, y0 + ph, PANEL_BG);

        // ── header bar (title + red [X]) ──
        ctx.fill(x0, y0, x0 + pw, y0 + HEAD_H, HEAD_BG);
        ctx.fill(x0, y0 + HEAD_H - 1, x0 + pw, y0 + HEAD_H, GOLD);
        ctx.drawTextWithShadow(tr, Text.literal("§6§l" + title + " §7- §fShortcuts"), x0 + PAD, y0 + 7, 0xFFFFFFFF);
        xx1 = x0 + pw - 4; xx0 = xx1 - X_SIZE; xy0 = y0 + 4; xy1 = xy0 + X_SIZE;
        boolean hov = mx >= xx0 && mx <= xx1 && my >= xy0 && my <= xy1;
        ctx.fill(xx0, xy0, xx1, xy1, hov ? X_BG_HOV : X_BG);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("§f§lX"), (xx0 + xx1) / 2, xy0 + 3, 0xFFFFFFFF);

        // ── body: grouped key / description rows ──
        int kx = x0 + PAD, dx = x0 + PAD + keyW + COLGAP, y = y0 + HEAD_H + PAD;
        for (Group g : groups) {
            ctx.drawTextWithShadow(tr, Text.literal("§e§l" + g.heading()), kx, y, 0xFFFFFFFF);
            y += ROW_H;
            for (Row r : g.rows()) {
                ctx.drawTextWithShadow(tr, Text.literal("§b" + r.keys()), kx + 6, y, 0xFFFFFFFF);
                ctx.drawTextWithShadow(tr, Text.literal("§7" + r.desc()), dx, y, 0xFFFFFFFF);
                y += ROW_H;
            }
        }
        ctx.drawCenteredTextWithShadow(tr, Text.literal("§8press the red X or Esc to close"),
                x0 + pw / 2, y0 + ph - 12, 0xFFFFFFFF);
    }
}
