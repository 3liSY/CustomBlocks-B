/**
 * CbActionBar.java — Group 27 §G27.7 §A4 dockable / hideable / smaller action bar. CLIENT-ONLY.
 *
 * Responsibility: the shared bottom action bar reworked into a movable panel that behaves like the §B
 * colour panel — drag its grip to dock it to the bottom, left or right edge (live snap), a corner toggle
 * hides it down to a thin sliver, and the buttons are smaller to free screen space. Dock side + hidden
 * state are remembered per screen via {@link com.customblocks.client.gui.CbScreenPrefs}. Buttons are
 * custom-drawn (not vanilla widgets) so the host routes mouse input through this component, exactly like
 * the colour panel. A {@link #flashPrimary} helper gives the green "saved" pulse over the primary button.
 *
 * Depends on: CbScreenPrefs, DrawContext.
 * Called by: ArabicPreviewScreen (and later the other Group 27 frame screens).
 */
package com.customblocks.client.gui.panel;

import com.customblocks.client.gui.CbScreenPrefs;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class CbActionBar {

    /** One bar button. {@code primary} draws green + gets the save flash. */
    public record Item(String label, Runnable action, boolean primary) {}

    private static final int GOLD = 0xFF_FF_AA_00, BAR_BG = 0xF0101010;
    private static final int BTN_BG = 0xFF333333, BTN_HOVER = 0xFF555555, PRIMARY = 0xFF2E8B2E;
    private static final int BTN_H = 16, PAD = 4, GAP = 3, GRIP = 12, TOGGLE = 14;
    private static final int DOCK_BOTTOM = 0, DOCK_LEFT = 1, DOCK_RIGHT = 2;

    private final String screenId;
    private final List<Item> items;
    private final CbScreenPrefs prefs = CbScreenPrefs.get();

    private int dock, primaryIndex = -1;
    private boolean hidden, dragging;
    private int screenW, screenH;
    private long flashEnd;

    // Layout cached each render (used by mouse handlers in the same frame).
    private int bx, by, bw, bh;
    private int[] ix = new int[0], iy = new int[0], iw = new int[0];
    private int gripX, gripY, gripW, gripH, togX, togY;

    public CbActionBar(String screenId, List<Item> items) {
        this.screenId = screenId;
        this.items = items;
        for (int i = 0; i < items.size(); i++) if (items.get(i).primary()) { primaryIndex = i; break; }
    }

    public void init(int screenW, int screenH) {
        this.screenW = screenW; this.screenH = screenH;
        CbScreenPrefs.Bar b = prefs.bar(screenId);
        dock = b.dock; hidden = b.hidden;
        ix = new int[items.size()]; iy = new int[items.size()]; iw = new int[items.size()];
    }

    public void flashPrimary() { flashEnd = System.currentTimeMillis() + 600; }

    public boolean isPointInside(double mx, double my) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    // ── Render ───────────────────────────────────────────────────────────────
    public void render(DrawContext ctx, int screenW, int screenH, TextRenderer tr, int mx, int my) {
        this.screenW = screenW; this.screenH = screenH;
        if (hidden) { renderSliver(ctx, tr); return; }

        int maxW = 34;
        for (Item it : items) maxW = Math.max(maxW, tr.getWidth(it.label()) + 10);
        int n = items.size();
        boolean horizontal = dock == DOCK_BOTTOM;

        if (horizontal) {
            bw = GRIP + n * maxW + (n - 1) * GAP + 4 + TOGGLE;
            bh = BTN_H + 2 * PAD;
            bx = Math.max(2, (screenW - bw) / 2);
            by = screenH - bh - 2;
            gripX = bx + 2; gripY = by + 4; gripW = 7; gripH = bh - 8;
            int cx = bx + GRIP;
            for (int i = 0; i < n; i++) { ix[i] = cx; iy[i] = by + PAD; iw[i] = maxW; cx += maxW + GAP; }
            togX = bx + bw - TOGGLE; togY = by + PAD;
        } else {
            bw = maxW + 2 * PAD;
            bh = GRIP + n * (BTN_H + GAP) + TOGGLE;
            bx = dock == DOCK_LEFT ? 2 : screenW - bw - 2;
            by = Math.max(46, (screenH - bh) / 2);
            gripX = bx + (bw - 7) / 2; gripY = by + 3; gripW = 7; gripH = 7;
            int cy = by + GRIP;
            for (int i = 0; i < n; i++) { ix[i] = bx + PAD; iy[i] = cy; iw[i] = maxW; cy += BTN_H + GAP; }
            togX = bx + (bw - TOGGLE) / 2; togY = by + bh - TOGGLE + 2;
        }

        // Strip background + gold edge.
        ctx.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, GOLD);
        ctx.fill(bx, by, bx + bw, by + bh, BAR_BG);
        // Drag grip (dots).
        for (int i = 0; i < 3; i++) {
            if (horizontal) ctx.fill(gripX + 1, gripY + i * 3, gripX + 6, gripY + 1 + i * 3, 0xFFBBBBBB);
            else ctx.fill(gripX, gripY + i * 2, gripX + 7, gripY + 1 + i * 2, 0xFFBBBBBB);
        }
        // Buttons.
        for (int i = 0; i < n; i++) {
            Item it = items.get(i);
            boolean hover = mx >= ix[i] && mx < ix[i] + iw[i] && my >= iy[i] && my < iy[i] + BTN_H;
            int bg = it.primary() ? PRIMARY : (hover ? BTN_HOVER : BTN_BG);
            ctx.fill(ix[i], iy[i], ix[i] + iw[i], iy[i] + BTN_H, bg);
            ctx.drawCenteredTextWithShadow(tr, Text.literal("§f" + it.label()), ix[i] + iw[i] / 2, iy[i] + 4, 0xFFFFFFFF);
        }
        // Save flash over the primary button.
        if (primaryIndex >= 0 && System.currentTimeMillis() < flashEnd) {
            ctx.fill(ix[primaryIndex], iy[primaryIndex], ix[primaryIndex] + iw[primaryIndex], iy[primaryIndex] + BTN_H, 0x6600FF44);
        }
        // Hide toggle.
        ctx.fill(togX, togY, togX + TOGGLE - 2, togY + 12, 0xFF000000);
        ctx.drawTextWithShadow(tr, Text.literal("§e_"), togX + 3, togY + 2, 0xFFFFFFFF);
    }

    private void renderSliver(DrawContext ctx, TextRenderer tr) {
        // A thin tab on the docked edge with a "show" arrow.
        if (dock == DOCK_BOTTOM) { bx = screenW / 2 - 14; by = screenH - 12; bw = 28; bh = 12; }
        else if (dock == DOCK_LEFT) { bx = 2; by = screenH / 2 - 14; bw = 12; bh = 28; }
        else { bx = screenW - 14; by = screenH / 2 - 14; bw = 12; bh = 28; }
        ctx.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, GOLD);
        ctx.fill(bx, by, bx + bw, by + bh, BAR_BG);
        String arrow = dock == DOCK_BOTTOM ? "^" : (dock == DOCK_LEFT ? ">" : "<");
        ctx.drawCenteredTextWithShadow(tr, Text.literal("§e" + arrow), bx + bw / 2, by + bh / 2 - 4, 0xFFFFFFFF);
        togX = bx; togY = by; // whole tab toggles
    }

    // ── Input ────────────────────────────────────────────────────────────────
    public boolean mouseClicked(double mx, double my, int button) {
        if (hidden) {
            if (mx >= bx && mx < bx + bw && my >= by && my < by + bh) { setHidden(false); return true; }
            return false;
        }
        if (!isPointInside(mx, my)) return false;
        if (mx >= togX && mx < togX + TOGGLE && my >= togY && my < togY + 12) { setHidden(true); return true; }
        // Grip → start dragging to re-dock.
        if (mx >= gripX - 2 && mx < gripX + gripW + 2 && my >= gripY - 2 && my < gripY + gripH + 2) {
            dragging = true; return true;
        }
        for (int i = 0; i < items.size(); i++) {
            if (mx >= ix[i] && mx < ix[i] + iw[i] && my >= iy[i] && my < iy[i] + BTN_H) {
                items.get(i).action().run();
                return true;
            }
        }
        return true; // clicked the strip background → swallow
    }

    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!dragging) return false;
        int nd = dock;
        if (my > screenH * 0.66) nd = DOCK_BOTTOM;
        else if (mx < screenW * 0.33) nd = DOCK_LEFT;
        else if (mx > screenW * 0.66) nd = DOCK_RIGHT;
        dock = nd;
        return true;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        if (!dragging) return false;
        dragging = false;
        prefs.saveBar(screenId, dock, hidden);
        return true;
    }

    private void setHidden(boolean h) { hidden = h; prefs.saveBar(screenId, dock, hidden); }
}
