/**
 * CbColorPanel.java — Group 27 §G27.7 §B reusable floating colour panel. CLIENT-ONLY.
 *
 * Responsibility: one draggable, snap-to-edge, collapsible colour panel reused across every Group 27
 * colour screen (Arabic first, then recolor / bottom bar / HUD) so the feel is identical everywhere.
 * Holds the editable swatch grid, a recents row, a pinned favourites row, harmony + tint suggestions,
 * a contrast guard between two targets, fast keys 1–9, an in-panel hex entry, named saved palettes,
 * and the §B4 dropper/loupe. It is NOT a Screen — the host screen owns one and forwards render + input
 * to it. All persistence goes through {@link CbPaletteStore}; harmony maths through CbPanelHarmony.
 *
 * Depends on: CbPaletteStore, CbPanelHarmony, CbColorDropper, CbColorTools.
 * Called by: ArabicPreviewScreen (and later RecolorSliderScreen / the §A4 bar / HUD).
 */
package com.customblocks.client.gui.panel;

import com.customblocks.image.CbColorTools;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class CbColorPanel {

    /** A colour the panel can drive (e.g. Arabic letter / background). The host owns undo + network. */
    public interface Target {
        String label();
        int get();
        void apply(int rgb);
    }

    private static final int GOLD = 0xFF_FF_AA_00, PANEL_BG = 0xF0101010, HEAD_BG = 0xFF2A2A2A;
    private static final int PW = 124, PAD = 6, SW = 14, GAP = 2, PER_ROW = 7, HEAD_H = 14, SNAP = 16;

    // Hit kinds dispatched in mouseClicked.
    private static final int K_COLLAPSE = 0, K_TAB = 1, K_SWATCH = 2, K_FAV = 3, K_RECENT = 4, K_BTN = 5, K_SAVED = 6;
    private static final int BTN_HEX = 0, BTN_DROP = 1, BTN_FAV = 2, BTN_SAVE = 3, BTN_LOAD = 4;

    private record Hit(int kind, int data, int x, int y, int w, int h) {
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final String screenId;
    private final List<Target> targets;
    private final CbPaletteStore store = CbPaletteStore.get();
    private final CbPanelHarmony harmony = new CbPanelHarmony();
    private final CbColorDropper dropper = new CbColorDropper();
    private final List<Hit> hits = new ArrayList<>();

    private int x, y, panelH = HEAD_H, active;
    private boolean collapsed;
    private int screenW, screenH;

    // Drag: 0 none, 1 move panel, 2 reorder swatch.
    private int dragMode, reorderFrom = -1, reorderTo = -1, pressedSwatch = -1;
    private double dragOffX, dragOffY;
    private boolean dragMoved;

    // Inline text input: 0 none, 1 add-hex, 2 save-name, 3 edit-swatch.
    private int inputMode, editIndex = -1;
    private String inputBuf = "";
    private boolean loadOpen;

    // Harmony region (for routing clicks) + hover tooltip, set each render.
    private int harmX, harmY, harmW, harmH;
    private String hoverHex;

    public CbColorPanel(String screenId, List<Target> targets) {
        this.screenId = screenId;
        this.targets = targets;
    }

    public void init(int screenW, int screenH) {
        this.screenW = screenW; this.screenH = screenH;
        CbPaletteStore.Layout l = store.layout(screenId);
        collapsed = l.collapsed;
        if (l.x < 0) { x = screenW - PW - 8; y = 48; }   // first open → top-right under the title bar
        else { x = l.x; y = l.y; }
        clampToScreen();
    }

    public int x() { return x; }
    public int width() { return PW; }
    public boolean isDropperActive() { return dropper.isActive(); }

    /** Free the dropper's frozen frame when the host screen closes. */
    public void onClosed() { dropper.cancel(); }

    /** Call at the very top of the host render(), before drawing anything, for the §B4 dropper capture. */
    public void preRender(DrawContext ctx) { dropper.preRender(ctx); }

    public boolean isPointInside(double mx, double my) {
        return mx >= x && mx < x + PW && my >= y && my < y + panelH;
    }

    // ── Render ─────────────────────────────────────────────────────────────────
    public void render(DrawContext ctx, int screenW, int screenH, TextRenderer tr, int mx, int my) {
        this.screenW = screenW; this.screenH = screenH;
        hits.clear();
        hoverHex = null;
        panelH = computeHeight();

        // Outer frame + body fill (drawn first so sections paint on top).
        ctx.fill(x - 1, y - 1, x + PW + 1, y + panelH + 1, GOLD);
        ctx.fill(x, y, x + PW, y + panelH, PANEL_BG);

        // Header (drag handle + title + collapse toggle).
        ctx.fill(x, y, x + PW, y + HEAD_H - 1, HEAD_BG);
        ctx.fill(x, y + HEAD_H - 1, x + PW, y + HEAD_H, GOLD);
        for (int i = 0; i < 3; i++) ctx.fill(x + 4, y + 3 + i * 3, x + 11, y + 4 + i * 3, 0xFFBBBBBB); // grip
        ctx.drawTextWithShadow(tr, Text.literal("§6Colours"), x + 16, y + 3, 0xFFFFFFFF);
        int cbx = x + PW - 12;
        ctx.fill(cbx, y + 2, cbx + 9, y + 11, 0xFF000000);
        ctx.drawTextWithShadow(tr, Text.literal(collapsed ? "§a+" : "§e-"), cbx + 2, y + 2, 0xFFFFFFFF);
        hits.add(new Hit(K_COLLAPSE, 0, cbx, y + 2, 9, 9));

        if (collapsed) { finishOverlays(ctx, tr, mx, my); return; }

        int cy = y + HEAD_H + 2;
        cy = drawTabs(ctx, tr, cy, mx, my);
        cy = drawCurrentAndContrast(ctx, tr, cy);
        cy = drawFavourites(ctx, tr, cy, mx, my);
        cy = drawSwatches(ctx, tr, cy, mx, my);
        cy = drawRecents(ctx, tr, cy, mx, my);
        cy = drawHarmony(ctx, tr, cy, mx, my);
        cy = drawButtons(ctx, tr, cy, mx, my);
        if (inputMode != 0) drawInput(ctx, tr, cy);

        finishOverlays(ctx, tr, mx, my);
    }

    /** Total panel height for the current content — mirrors the section advances in render(). */
    private int computeHeight() {
        if (collapsed) return HEAD_H;
        int h = HEAD_H + 2;
        if (targets.size() >= 2) h += 14;                                   // tabs
        h += 16 + (targets.size() >= 2 ? 11 : 0) + 2;                       // current + contrast
        if (!store.favorites.isEmpty()) h += 10 + rows(store.favorites.size()) * (SW + GAP) + 2;
        h += 10 + Math.max(1, rows(store.swatches.size())) * (SW + GAP) + 2; // palette grid
        if (!store.recents.isEmpty()) h += 10 + rows(store.recents.size()) * (SW + GAP) + 2;
        h += CbPanelHarmony.HEIGHT + 4;
        h += 14;                                                            // buttons
        if (inputMode != 0) h += 14;
        return h + PAD;
    }

    private static int rows(int n) { return (n + PER_ROW - 1) / PER_ROW; }

    private void finishOverlays(DrawContext ctx, TextRenderer tr, int mx, int my) {
        if (loadOpen) drawLoadList(ctx, tr, mx, my);
        if (hoverHex != null) {                         // hover-a-swatch hex tooltip
            int w = tr.getWidth(hoverHex) + 6;
            ctx.fill(mx + 8, my - 2, mx + 8 + w, my + 10, 0xF0000000);
            ctx.drawTextWithShadow(tr, Text.literal(hoverHex), mx + 11, my, 0xFFFFFFFF);
        }
        dropper.render(ctx, tr, mx, my, screenW, screenH);
    }

    private int drawTabs(DrawContext ctx, TextRenderer tr, int cy, int mx, int my) {
        if (targets.size() < 2) return cy;
        int tw = (PW - 2 * PAD - GAP) / 2;
        for (int i = 0; i < 2; i++) {
            int tx = x + PAD + i * (tw + GAP);
            boolean sel = i == active;
            ctx.fill(tx, cy, tx + tw, cy + 12, sel ? GOLD : 0xFF333333);
            ctx.drawCenteredTextWithShadow(tr, Text.literal((sel ? "§0" : "§f") + targets.get(i).label()),
                    tx + tw / 2, cy + 2, 0xFFFFFFFF);
            hits.add(new Hit(K_TAB, i, tx, cy, tw, 12));
        }
        return cy + 14;
    }

    private int drawCurrentAndContrast(DrawContext ctx, TextRenderer tr, int cy) {
        int cur = cur();
        ctx.fill(x + PAD, cy, x + PAD + 16, cy + 14, 0xFF000000 | cur);
        ctx.drawTextWithShadow(tr, Text.literal(String.format(Locale.ROOT, "§f#%06X", cur)), x + PAD + 20, cy + 3, 0xFFFFFFFF);
        cy += 16;
        if (targets.size() >= 2) {
            double ratio = CbColorTools.contrastRatio(targets.get(0).get(), targets.get(1).get());
            ctx.drawTextWithShadow(tr, Text.literal(CbColorTools.contrastLabel(ratio)), x + PAD, cy, 0xFFFFFFFF);
            cy += 11;
        }
        return cy + 2;
    }

    private int drawFavourites(DrawContext ctx, TextRenderer tr, int cy, int mx, int my) {
        if (store.favorites.isEmpty()) return cy;
        ctx.drawTextWithShadow(tr, Text.literal("§eFav"), x + PAD, cy, 0xFFFFFFFF);
        cy += 10;
        cy = drawColorRow(ctx, tr, cy, store.favorites, K_FAV, mx, my, true);
        return cy + 2;
    }

    private int drawSwatches(DrawContext ctx, TextRenderer tr, int cy, int mx, int my) {
        ctx.drawTextWithShadow(tr, Text.literal("§7Palette"), x + PAD, cy, 0xFFFFFFFF);
        cy += 10;
        List<Integer> sw = store.swatches;
        for (int i = 0; i < sw.size(); i++) {
            int col = i % PER_ROW, row = i / PER_ROW;
            int sx = x + PAD + col * (SW + GAP), sy = cy + row * (SW + GAP);
            int rgb = sw.get(i) & 0xFFFFFF;
            boolean hover = mx >= sx && mx < sx + SW && my >= sy && my < sy + SW;
            boolean reord = dragMode == 2 && reorderFrom == i;
            ctx.fill(sx - 1, sy - 1, sx + SW + 1, sy + SW + 1, reord ? 0xFFFFFF55 : (hover ? 0xFFFFFFFF : 0xFF000000));
            ctx.fill(sx, sy, sx + SW, sy + SW, 0xFF000000 | rgb);
            if (hover) hoverHex = String.format(Locale.ROOT, "#%06X", rgb);
            hits.add(new Hit(K_SWATCH, i, sx, sy, SW, SW));
        }
        int rows = (sw.size() + PER_ROW - 1) / PER_ROW;
        return cy + Math.max(1, rows) * (SW + GAP) + 2;
    }

    private int drawRecents(DrawContext ctx, TextRenderer tr, int cy, int mx, int my) {
        if (store.recents.isEmpty()) return cy;
        ctx.drawTextWithShadow(tr, Text.literal("§8Recent"), x + PAD, cy, 0xFFFFFFFF);
        cy += 10;
        return drawColorRow(ctx, tr, cy, store.recents, K_RECENT, mx, my, false) + 2;
    }

    /** A wrapping row of colours (favourites / recents); records K_FAV/K_RECENT hits carrying the rgb. */
    private int drawColorRow(DrawContext ctx, TextRenderer tr, int cy, List<Integer> cols, int kind, int mx, int my, boolean star) {
        for (int i = 0; i < cols.size(); i++) {
            int col = i % PER_ROW, row = i / PER_ROW;
            int sx = x + PAD + col * (SW + GAP), sy = cy + row * (SW + GAP);
            int rgb = cols.get(i) & 0xFFFFFF;
            boolean hover = mx >= sx && mx < sx + SW && my >= sy && my < sy + SW;
            ctx.fill(sx - 1, sy - 1, sx + SW + 1, sy + SW + 1, hover ? 0xFFFFFFFF : 0xFF000000);
            ctx.fill(sx, sy, sx + SW, sy + SW, 0xFF000000 | rgb);
            if (hover) hoverHex = String.format(Locale.ROOT, "#%06X", rgb);
            hits.add(new Hit(kind, rgb, sx, sy, SW, SW));
        }
        int rows = (cols.size() + PER_ROW - 1) / PER_ROW;
        return cy + Math.max(1, rows) * (SW + GAP);
    }

    private int drawHarmony(DrawContext ctx, TextRenderer tr, int cy, int mx, int my) {
        harmX = x + PAD; harmY = cy; harmW = PW - 2 * PAD;
        harmH = harmony.render(ctx, tr, harmX, harmY, cur(), mx, my);
        return cy + harmH + 4;
    }

    private int drawButtons(DrawContext ctx, TextRenderer tr, int cy, int mx, int my) {
        String[] labels = { "+hex", "drop", "fav", "save", "load" };
        int n = labels.length, bw = (PW - 2 * PAD - (n - 1) * 2) / n;
        for (int i = 0; i < n; i++) {
            int bx = x + PAD + i * (bw + 2);
            boolean hover = mx >= bx && mx < bx + bw && my >= cy && my < cy + 12;
            boolean on = (i == BTN_DROP && dropper.isActive());
            ctx.fill(bx, cy, bx + bw, cy + 12, on ? GOLD : (hover ? 0xFF555555 : 0xFF333333));
            ctx.drawCenteredTextWithShadow(tr, Text.literal((on ? "§0" : "§f") + labels[i]), bx + bw / 2, cy + 2, 0xFFFFFFFF);
            hits.add(new Hit(K_BTN, i, bx, cy, bw, 12));
        }
        return cy + 14;
    }

    private int drawInput(DrawContext ctx, TextRenderer tr, int cy) {
        String prompt = inputMode == 2 ? "name:" : "hex:";
        ctx.fill(x + PAD, cy, x + PW - PAD, cy + 12, 0xFF000000);
        ctx.fill(x + PAD, cy, x + PW - PAD, cy + 1, GOLD);
        ctx.drawTextWithShadow(tr, Text.literal("§7" + prompt + " §f" + inputBuf + "§e_"), x + PAD + 2, cy + 2, 0xFFFFFFFF);
        return cy + 14;
    }

    private void drawLoadList(DrawContext ctx, TextRenderer tr, int mx, int my) {
        List<String> names = store.paletteNames();
        int lx = x, ly = y + panelH + 2, lw = PW;
        int rows = Math.max(1, names.size());
        ctx.fill(lx - 1, ly - 1, lx + lw + 1, ly + rows * 12 + 1, GOLD);
        ctx.fill(lx, ly, lx + lw, ly + rows * 12, PANEL_BG);
        if (names.isEmpty()) {
            ctx.drawTextWithShadow(tr, Text.literal("§8no saved palettes"), lx + 4, ly + 2, 0xFFFFFFFF);
            return;
        }
        for (int i = 0; i < names.size(); i++) {
            int ry = ly + i * 12;
            boolean hover = mx >= lx && mx < lx + lw && my >= ry && my < ry + 12;
            if (hover) ctx.fill(lx, ry, lx + lw, ry + 12, 0xFF444444);
            ctx.drawTextWithShadow(tr, Text.literal("§f" + names.get(i) + " §8(R-click del)"), lx + 4, ry + 2, 0xFFFFFFFF);
            hits.add(new Hit(K_SAVED, i, lx, ry, lw, 12));
        }
    }

    // ── Input ──────────────────────────────────────────────────────────────────
    public boolean mouseClicked(double mx, double my, int button) {
        if (dropper.isActive()) {                                   // dropper armed → click anywhere samples
            int rgb = dropper.sample((int) mx, (int) my);
            if (rgb >= 0) applyColor(rgb);
            return true;
        }
        if (loadOpen) {                                             // load list is modal-ish
            for (Hit h : hits) if (h.kind == K_SAVED && h.in(mx, my)) {
                String name = store.paletteNames().get(h.data);
                if (button == 1) store.deletePalette(name); else { store.loadPalette(name); loadOpen = false; }
                return true;
            }
            loadOpen = false; return true;
        }
        if (!isPointInside(mx, my)) {                               // outside → cancel input, let host handle
            if (inputMode != 0) { inputMode = 0; inputBuf = ""; }
            return false;
        }
        // Harmony region.
        if (mx >= harmX && mx < harmX + harmW && my >= harmY && my < harmY + harmH) {
            int c = harmony.clickedColor(mx, my);
            if (c >= 0) applyColor(c);
            return true;
        }
        for (Hit h : hits) {
            if (!h.in(mx, my)) continue;
            switch (h.kind) {
                case K_COLLAPSE -> { collapsed = !collapsed; persist(); return true; }
                case K_TAB      -> { active = h.data; return true; }
                case K_FAV      -> { if (button == 1) store.toggleFavorite(h.data); else applyColor(h.data); return true; }
                case K_RECENT   -> { applyColor(h.data); return true; }
                case K_BTN      -> { onButton(h.data); return true; }
                case K_SWATCH   -> {
                    if (button == 1) { startEdit(h.data); }         // right-click edits the swatch
                    else { pressedSwatch = h.data; dragMoved = false; }  // left-click applies on release (or drags)
                    return true;
                }
            }
        }
        // Header bar (not the collapse box) → start moving the panel.
        if (my < y + HEAD_H) { dragMode = 1; dragOffX = mx - x; dragOffY = my - y; dragMoved = false; }
        return true; // clicked panel body — swallow
    }

    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragMode == 1) {
            x = (int) (mx - dragOffX); y = (int) (my - dragOffY);
            clampToScreen(); dragMoved = true; return true;
        }
        if (pressedSwatch >= 0 && (Math.abs(dx) > 1 || Math.abs(dy) > 1 || dragMode == 2)) {
            dragMode = 2; reorderFrom = pressedSwatch;
            reorderTo = -1;
            for (Hit h : hits) if (h.kind == K_SWATCH && h.in(mx, my)) reorderTo = h.data;
            dragMoved = true; return true;
        }
        return false;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        boolean consumed = dragMode != 0 || pressedSwatch >= 0;
        if (dragMode == 1) { snapToEdges(); persist(); }
        else if (dragMode == 2) { if (reorderTo >= 0) store.moveSwatch(reorderFrom, reorderTo); }
        else if (pressedSwatch >= 0 && !dragMoved && pressedSwatch < store.swatches.size()) {
            applyColor(store.swatches.get(pressedSwatch));
        }
        dragMode = 0; reorderFrom = -1; reorderTo = -1; pressedSwatch = -1; dragMoved = false;
        return consumed;
    }

    public boolean keyPressed(int key, int scan, int mods) {
        if (dropper.isActive() && key == GLFW.GLFW_KEY_ESCAPE) { dropper.cancel(); return true; }
        if (inputMode != 0) {
            switch (key) {
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> commitInput();
                case GLFW.GLFW_KEY_ESCAPE -> { inputMode = 0; inputBuf = ""; }
                case GLFW.GLFW_KEY_BACKSPACE -> { if (!inputBuf.isEmpty()) inputBuf = inputBuf.substring(0, inputBuf.length() - 1); }
            }
            return true;
        }
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {       // fast keys → apply swatch 1–9
            int idx = key - GLFW.GLFW_KEY_1;
            if (idx < store.swatches.size()) { applyColor(store.swatches.get(idx)); return true; }
        }
        return false;
    }

    public boolean charTyped(char c, int mods) {
        if (inputMode == 0) return false;
        if (inputMode == 2) { if (c >= 32 && c < 127 && inputBuf.length() < 20) inputBuf += c; }
        else if (inputBuf.length() < 7 && (c == '#' || isHex(c))) inputBuf += Character.toUpperCase(c);
        return true;
    }

    // ── Actions ──────────────────────────────────────────────────────────────
    private void onButton(int id) {
        switch (id) {
            case BTN_HEX  -> { inputMode = 1; inputBuf = "#"; }
            case BTN_DROP -> dropper.activate();
            case BTN_FAV  -> store.toggleFavorite(cur());           // pin/unpin the active colour
            case BTN_SAVE -> { inputMode = 2; inputBuf = ""; }
            case BTN_LOAD -> loadOpen = !loadOpen;
        }
    }

    private void startEdit(int index) { inputMode = 3; editIndex = index; inputBuf = String.format(Locale.ROOT, "#%06X", store.swatches.get(index) & 0xFFFFFF); }

    private void commitInput() {
        int rgb = parseHex(inputBuf);
        switch (inputMode) {
            case 1 -> { if (rgb >= 0) store.addSwatch(rgb); }
            case 2 -> { if (!inputBuf.isBlank()) store.savePalette(inputBuf); }
            case 3 -> { if (rgb >= 0 && editIndex >= 0) store.setSwatch(editIndex, rgb); }
        }
        inputMode = 0; inputBuf = ""; editIndex = -1;
    }

    private void applyColor(int rgb) {
        if (targets.isEmpty()) return;
        targets.get(active).apply(rgb & 0xFFFFFF);
        store.pushRecent(rgb);
    }

    private int cur() { return targets.isEmpty() ? 0 : targets.get(active).get() & 0xFFFFFF; }

    private void clampToScreen() {
        x = Math.max(2, Math.min(x, screenW - PW - 2));
        y = Math.max(2, Math.min(y, screenH - HEAD_H - 2));
    }

    private void snapToEdges() {
        if (x < SNAP) x = 4;
        else if (x + PW > screenW - SNAP) x = screenW - PW - 4;
        if (y < SNAP + 44) y = 48;                                 // keep clear of the title bar
        else if (y + panelH > screenH - SNAP) y = screenH - panelH - 4;
    }

    private void persist() { store.saveLayout(screenId, x, y, collapsed); }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static int parseHex(String s) {
        try {
            String h = s.trim();
            if (h.startsWith("#")) h = h.substring(1);
            if (h.isEmpty()) return -1;
            return Integer.parseInt(h, 16) & 0xFFFFFF;
        } catch (Exception e) { return -1; }
    }
}
