/**
 * BulkDraw.java — Group 07 (Bulk Operations Hub). CLIENT-ONLY.
 *
 * The immediate-mode drawing primitives the Hub's view classes share: a themed button, a text clipper, a
 * vertical scrollbar (X4), a hand-drawn pixel padlock (B11 — MC's font has no supplementary-plane 🔒), a
 * category chip (A1), a live block-item icon, first-word capitalization, hit-testing, and a scissor helper so
 * long text is clipped by a panel edge (X2 "no ellipsis, no overlap") instead of an "…".
 *
 * Rects are {@code int[]{x, y, w, h}} — recomputed every frame by the view that draws them, read back the
 * same frame by the screen's mouse handlers.
 *
 * Depends on: CbTheme, ClientSlotCache, CustomBlocksMod (MOD_ID), Registries.
 * Called by: BulkWorkbenchView, BulkOpsView, BulkWorkbenchScreen.
 */
package com.customblocks.client.gui;

import com.customblocks.CustomBlocksMod;
import com.customblocks.client.ClientSlotCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
final class BulkDraw {

    private BulkDraw() {} // static-only

    static final int BAR_H = 38;
    static final int ROW_H = 20;   // taller than the old 15 so a 16px block icon + a chip fit without overlap

    static final int PANEL   = 0xFF0C0C0C;
    static final int PANEL_E = 0xFF2A2A2A;
    static final int ROW     = 0xFF121212;
    static final int ROW_HOV = 0xFF1E1E1E;
    static final int ROW_SEL = CbTheme.SEL_FILL;

    private static final int BTN = 0xFF2A2A2A, BTN_HOV = 0xFF444444, BTN_OFF = 0xFF181818;
    private static final int BTN_DANGER = 0xFF5A0000, BTN_DANGER_HOV = 0xFF8A0000;

    /** A themed button. {@code active} paints the selected fill, {@code danger} the destructive red. */
    static void btn(DrawContext ctx, TextRenderer tr, int mx, int my, String label, int[] r,
                    boolean enabled, boolean active, boolean danger) {
        if (r == null) return;
        boolean hover = enabled && in(mx, my, r);
        int bg = !enabled ? BTN_OFF
                : danger ? (hover ? BTN_DANGER_HOV : BTN_DANGER)
                : active ? CbTheme.SEL_FILL
                : (hover ? BTN_HOV : BTN);
        ctx.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, active || danger ? CbTheme.ACCENT : 0xFF000000);
        ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], bg);
        ctx.drawCenteredTextWithShadow(tr, Text.literal((enabled ? "§f" : "§8") + fit(tr, label, r[2] - 6)),
                r[0] + r[2] / 2, r[1] + (r[3] - 8) / 2, 0xFFFFFFFF);
    }

    static void btn(DrawContext ctx, TextRenderer tr, int mx, int my, String label, int[] r, boolean enabled) {
        btn(ctx, tr, mx, my, label, r, enabled, false, false);
    }

    /** A bordered, empty panel — the backing for every list and pane. */
    static void panel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, PANEL_E);
        ctx.fill(x, y, x + w, y + h, PANEL);
    }

    /** Clip {@code s} to {@code maxPx}, appending an ellipsis when it doesn't fit. Buttons only — NOT ids. */
    static String fit(TextRenderer tr, String s, int maxPx) {
        if (s == null) s = "";
        if (tr.getWidth(s) <= maxPx) return s;
        while (!s.isEmpty() && tr.getWidth(s + "…") > maxPx) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    /** Capitalize the first letter (X2/label rule: "sel all page" → "Sel all page"), leaving § codes alone. */
    static String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        int i = 0;
        while (i + 1 < s.length() && s.charAt(i) == '§') i += 2; // skip a leading colour code
        if (i >= s.length()) return s;
        return s.substring(0, i) + Character.toUpperCase(s.charAt(i)) + s.substring(i + 1);
    }

    /**
     * A vertical scrollbar on the right edge of a list (X4). Draws nothing when everything fits. Returns the
     * thumb rect so the screen can drag it; a null return means "no bar this frame".
     */
    static int[] vscroll(DrawContext ctx, int rx, int top, int h, int total, int visible, int scroll) {
        if (total <= visible || h <= 0) return null;
        ctx.fill(rx, top, rx + 3, top + h, 0xFF1A1A1A);
        int thumbH = Math.max(12, h * visible / total);
        int maxScroll = Math.max(1, total - visible);
        int thumbY = top + (h - thumbH) * Math.min(scroll, maxScroll) / maxScroll;
        ctx.fill(rx, thumbY, rx + 3, thumbY + thumbH, CbTheme.ACCENT);
        // A2: return a wider grab band than the 3px bar so the thumb is easy to catch (the screen drags it).
        return new int[]{rx - 4, top, 8, h};
    }

    /** Hand-drawn 7×9 pixel padlock (B11) — a real sprite, since 🔒 is a missing-glyph box in MC's font. */
    static void padlock(DrawContext ctx, int x, int y) {
        int dark = 0xFF000000, metal = CbTheme.ACCENT;
        ctx.fill(x + 1, y,     x + 6, y + 1, dark); // shackle top bar
        ctx.fill(x + 1, y + 1, x + 2, y + 4, dark); // shackle left arm
        ctx.fill(x + 5, y + 1, x + 6, y + 4, dark); // shackle right arm
        ctx.fill(x,     y + 3, x + 7, y + 9, dark); // body border
        ctx.fill(x + 1, y + 4, x + 6, y + 8, metal); // body fill
        ctx.fill(x + 3, y + 5, x + 4, y + 7, dark); // keyhole
    }

    /** A small red category chip (A1). Returns its drawn width so the caller can advance past it. */
    static int chip(DrawContext ctx, TextRenderer tr, int x, int y, String text) {
        String t = text == null ? "" : text;
        int w = tr.getWidth(t) + 6;
        ctx.fill(x, y, x + w, y + 9, CbTheme.SEL_FILL);
        ctx.fill(x, y, x + w, y + 1, CbTheme.ACCENT);
        ctx.fill(x, y + 8, x + w, y + 9, CbTheme.ACCENT);
        ctx.drawTextWithShadow(tr, Text.literal("§f" + t), x + 3, y + 1, 0xFFFFFFFF);
        return w;
    }

    /** Draw the live block's item icon (its baked texture) for {@code id} at 16×16, or nothing if unknown. */
    static void blockIcon(DrawContext ctx, String id, int x, int y) {
        Integer idx = ClientSlotCache.indexForId(id);
        if (idx == null) return;
        Item item = Registries.ITEM.get(Identifier.of(CustomBlocksMod.MOD_ID, "slot_" + idx));
        if (item == Items.AIR) return;
        ctx.drawItem(new ItemStack(item), x, y);
    }

    /**
     * Draw the block's item icon scaled up (creative-inventory scale, §G27.22 grid). {@code scale} multiplies
     * the base 16px — e.g. 2.5f ≈ 40px. Positioned so its top-left lands at (x,y). Falls back to nothing if the
     * id is unknown; a scale that doesn't take (item render immediate) just shows 16px, never crashes.
     */
    static void blockIconLarge(DrawContext ctx, String id, int x, int y, float scale) {
        Integer idx = ClientSlotCache.indexForId(id);
        if (idx == null) return;
        Item item = Registries.ITEM.get(Identifier.of(CustomBlocksMod.MOD_ID, "slot_" + idx));
        if (item == Items.AIR) return;
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        ctx.drawItem(new ItemStack(item), 0, 0);
        m.pop();
    }

    static boolean in(double mx, double my, int[] r) {
        return r != null && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }

    static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
