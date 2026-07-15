/**
 * CbSettingsOverlay.java — Group 27 §G27.8.B shared ⚙ Settings popup. CLIENT-SIDE ONLY.
 *
 * Responsibility: the one global Settings popup opened from the ⚙ button on every CB frame screen.
 * Replaces the cramped in-bar dim slider (§A2). All state lives in {@link CbScreenPrefs} (one shared,
 * global file): backdrop dim, hide-world, master sound + volume, default auto-spin, reduced motion,
 * confirm-before-discard, plus "reset bar positions" / "reset settings". Red+black frame (locked
 * palette 2026-07-04); full-screen scrim; clicks are swallowed while open; the red [X] or Esc closes.
 *
 * Stateful: ONE instance per screen — holds open/closed + hit boxes recomputed each render.
 * Depends on: CbScreenPrefs, CbTheme, CbUiSounds, DrawContext/TextRenderer.
 * Called by: ArabicPreviewScreen, RecolorSliderScreen, ShapeEditorScreen, HudEditorScreen.
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class CbSettingsOverlay {

    private static final int PW = 236, ROW_H = 18, HEAD_H = 22, PAD = 10, X_SIZE = 14;
    private static final int TRACK_W = 92;

    // Row ids (top to bottom).
    private static final int R_DIM = 0, R_WORLD = 1, R_SOUND = 2, R_VOL = 3,
            R_SPIN = 4, R_MOTION = 5, R_CONFIRM = 6, R_RESET = 7;
    private static final int ROWS = 8;

    private boolean open;
    private int px, py;                 // panel top-left, recomputed each render
    private int xx0, xy0, xx1, xy1;     // red [X] hit box
    private int dragRow = -1;           // slider row being dragged (-1 = none)

    public boolean isOpen() { return open; }
    public void toggle()    { open = !open; if (!open) endDrag(); }
    public void close()     { open = false; endDrag(); }

    private void endDrag() {
        if (dragRow >= 0) CbScreenPrefs.get().saveSettings(); // one write per drag, on release/close
        dragRow = -1;
    }

    // ── Render ───────────────────────────────────────────────────────────────
    public void render(DrawContext ctx, int sw, int sh, TextRenderer tr, int mx, int my) {
        if (!open) return;
        CbScreenPrefs p = CbScreenPrefs.get();
        int ph = HEAD_H + PAD + ROWS * ROW_H + PAD;
        px = (sw - PW) / 2; py = (sh - ph) / 2;

        ctx.fill(0, 0, sw, sh, 0xB0000000); // scrim — nothing behind bleeds through (§G27.8.0)
        ctx.fill(px - 1, py - 1, px + PW + 1, py + ph + 1, CbTheme.ACCENT);
        ctx.fill(px, py, px + PW, py + ph, CbTheme.PANEL_BG);

        // Header + red [X].
        ctx.fill(px, py, px + PW, py + HEAD_H, 0xFF1A1A1A);
        ctx.fill(px, py + HEAD_H - 1, px + PW, py + HEAD_H, CbTheme.ACCENT);
        ctx.drawTextWithShadow(tr, CbTheme.red("Settings"), px + PAD, py + 7, 0xFFFFFFFF);
        xx1 = px + PW - 4; xx0 = xx1 - X_SIZE; xy0 = py + 4; xy1 = xy0 + X_SIZE;
        boolean hov = mx >= xx0 && mx <= xx1 && my >= xy0 && my <= xy1;
        ctx.fill(xx0, xy0, xx1, xy1, hov ? 0xFFE53C3C : 0xFFC02A2A);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("§f§lX"), (xx0 + xx1) / 2, xy0 + 3, 0xFFFFFFFF);

        // Rows.
        slider(ctx, tr, R_DIM,   "Backdrop dim", p.dim01(), Math.round(p.dim01() * 100) + "%");
        toggle(ctx, tr, R_WORLD, "Hide world",   p.hideWorld);
        toggle(ctx, tr, R_SOUND, "UI sounds",    p.soundOn);
        slider(ctx, tr, R_VOL,   "Volume",       p.soundVolume / 100.0, p.soundVolume + "%");
        slider(ctx, tr, R_SPIN,  "Auto-spin",    p.spinDefault / 2.5, Math.round(p.spinDefault / 2.5 * 100) + "%");
        toggle(ctx, tr, R_MOTION, "Reduced motion", p.reducedMotion);
        toggle(ctx, tr, R_CONFIRM, "Confirm discard", p.confirmDiscard);

        // Reset row — two half-width buttons.
        int ry = rowY(R_RESET);
        int bw = (PW - 2 * PAD - 4) / 2;
        button(ctx, tr, px + PAD, ry, bw, "Reset bars", mx, my);
        button(ctx, tr, px + PAD + bw + 4, ry, bw, "Reset settings", mx, my);
    }

    private int rowY(int row) { return py + HEAD_H + PAD + row * ROW_H; }

    private void toggle(DrawContext ctx, TextRenderer tr, int row, String label, boolean on) {
        int y = rowY(row);
        ctx.drawTextWithShadow(tr, Text.literal("§7" + label), px + PAD, y + 3, 0xFFFFFFFF);
        int bx = px + PW - PAD - 34;
        ctx.fill(bx, y + 1, bx + 34, y + 13, on ? CbTheme.SEL_FILL : 0xFF333333);
        ctx.fill(bx, y + 1, bx + 34, y + 2, on ? CbTheme.ACCENT : 0xFF555555);
        ctx.drawCenteredTextWithShadow(tr, Text.literal(on ? "§cOn" : "§7Off"), bx + 17, y + 3, 0xFFFFFFFF);
    }

    private void slider(DrawContext ctx, TextRenderer tr, int row, String label, double v01, String chip) {
        int y = rowY(row);
        ctx.drawTextWithShadow(tr, Text.literal("§7" + label), px + PAD, y + 3, 0xFFFFFFFF);
        int tx = px + PW - PAD - TRACK_W - 30;
        ctx.fill(tx, y + 5, tx + TRACK_W, y + 8, 0xFF333333);
        int knob = tx + (int) Math.round(Math.max(0, Math.min(1, v01)) * TRACK_W);
        ctx.fill(knob - 2, y + 2, knob + 2, y + 11, CbTheme.ACCENT);
        ctx.drawTextWithShadow(tr, Text.literal("§f" + chip), tx + TRACK_W + 6, y + 3, 0xFFFFFFFF);
    }

    private void button(DrawContext ctx, TextRenderer tr, int x, int y, int w, String label, int mx, int my) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + 14;
        ctx.fill(x, y, x + w, y + 14, hov ? 0xFF555555 : 0xFF333333);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("§f" + label), x + w / 2, y + 3, 0xFFFFFFFF);
    }

    // ── Input (call first from the screen; returns true while open = consumed) ──
    public boolean mouseClicked(double mx, double my) {
        if (!open) return false;
        if (mx >= xx0 && mx <= xx1 && my >= xy0 && my <= xy1) { close(); CbUiSounds.click(); return true; }
        CbScreenPrefs p = CbScreenPrefs.get();
        int row = rowAt(my);
        switch (row) {
            case R_DIM, R_VOL, R_SPIN -> { dragRow = row; applySlider(row, mx); }
            case R_WORLD  -> { p.hideWorld = !p.hideWorld; p.saveSettings(); CbUiSounds.tick(); }
            case R_SOUND  -> { p.soundOn = !p.soundOn; p.saveSettings(); CbUiSounds.tick(); }
            case R_MOTION -> { p.reducedMotion = !p.reducedMotion; p.saveSettings(); CbUiSounds.tick(); }
            case R_CONFIRM -> { p.confirmDiscard = !p.confirmDiscard; p.saveSettings(); CbUiSounds.tick(); }
            case R_RESET  -> {
                int bw = (PW - 2 * PAD - 4) / 2;
                if (mx >= px + PAD && mx < px + PAD + bw) { p.resetBars(); CbUiSounds.click(); }
                else if (mx >= px + PAD + bw + 4 && mx < px + PW - PAD) { p.resetSettings(); CbUiSounds.click(); }
            }
            default -> { }
        }
        return true; // swallow everything while open
    }

    public boolean mouseDragged(double mx, double my) {
        if (!open || dragRow < 0) return open;
        applySlider(dragRow, mx);
        return true;
    }

    public boolean mouseReleased() {
        if (!open) return false;
        endDrag();
        return true;
    }

    /** Esc closes; all other keys are swallowed while open. */
    public boolean keyPressed(int key) {
        if (!open) return false;
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) close();
        return true;
    }

    private int rowAt(double my) {
        for (int r = 0; r < ROWS; r++) {
            int y = rowY(r);
            if (my >= y && my < y + ROW_H - 2) return r;
        }
        return -1;
    }

    private void applySlider(int row, double mx) {
        int tx = px + PW - PAD - TRACK_W - 30;
        double v = Math.max(0, Math.min(1, (mx - tx) / TRACK_W));
        CbScreenPrefs p = CbScreenPrefs.get();
        switch (row) {
            case R_DIM  -> p.dimAlpha = (int) Math.round(v * 255);
            case R_VOL  -> p.soundVolume = (int) Math.round(v * 100);
            case R_SPIN -> p.spinDefault = v * 2.5;
            default -> { }
        }
    }
}
