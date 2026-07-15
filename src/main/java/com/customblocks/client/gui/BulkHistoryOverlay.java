/**
 * BulkHistoryOverlay.java — Group 07 §G07-4 (in-screen history panel + toast). CLIENT-ONLY.
 *
 * Split out of {@link BulkWorkbenchScreen} to hold the ≤500-line cap. Owns the undo/redo history panel and the
 * bottom-right undo/redo toast: the stack labels + toast text arrive in the snapshot (parsed here), it renders
 * both, and its {@link #click} routes clicks — the History toggle, a step-row to jump-back/forward N (sends a
 * counted UNDO/REDO), or a toast dismiss. The chrome draws the History button using {@link #undoSize()} /
 * {@link #histOpen()}, so the toggle rect is passed into {@link #click}.
 *
 * Depends on: BulkDraw, BulkWorkbenchView (geometry), CbTheme, CbUiSounds, BulkActionPayload, Gson.
 * Called by: BulkWorkbenchScreen (parse / render / click) and BulkWorkbenchView (undoSize / histOpen).
 */
package com.customblocks.client.gui;

import com.customblocks.network.payloads.BulkActionPayload;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

import static com.customblocks.client.gui.BulkWorkbenchView.CONTENT_Y;
import static com.customblocks.client.gui.BulkWorkbenchView.FOOT_H;
import static com.customblocks.client.gui.BulkWorkbenchView.contentBottom;

@Environment(EnvType.CLIENT)
final class BulkHistoryOverlay {

    private String toastMsg;
    private long toastAt;
    private List<String> undoHist = List.of();
    private List<String> redoHist = List.of();
    private boolean open;

    private final List<int[]> histRects = new ArrayList<>();
    private final List<Integer> histJumps = new ArrayList<>();   // +N = undo N, -N = redo N
    private int[] rToast;
    private int[] rBox, rHistClose;   // §G27.22 centered popup box + its ✖ close

    int undoSize() { return undoHist.size(); }
    boolean histOpen() { return open; }

    /** Pull the toast + undo/redo stacks from a fresh snapshot. */
    void parse(JsonObject root) {
        if (root.has("toast")) {
            String ts = root.get("toast").getAsString();
            if (!ts.isBlank()) { toastMsg = ts; toastAt = System.currentTimeMillis(); }
        }
        undoHist = strList(root, "undo");
        redoHist = strList(root, "redo");
    }

    private static List<String> strList(JsonObject root, String key) {
        List<String> out = new ArrayList<>();
        if (root.has(key) && root.get(key).isJsonArray())
            for (JsonElement e : root.getAsJsonArray(key)) if (e.isJsonPrimitive()) out.add(e.getAsString());
        return out;
    }

    // ── render (called topmost, after the tab content + modals) ────────────────
    void render(DrawContext ctx, TextRenderer tr, int mx, int my, int width, int height) {
        if (open) renderHistory(ctx, tr, mx, my, width, height);
        renderToast(ctx, tr, width, height);
    }

    private void renderToast(DrawContext ctx, TextRenderer tr, int width, int height) {
        if (toastMsg == null) return;
        if (System.currentTimeMillis() - toastAt > 3200) { toastMsg = null; rToast = null; return; }
        int w = tr.getWidth(toastMsg) + 20, h = 20;
        int x = width - w - 10, y = height - FOOT_H - h - 8;
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, CbTheme.ACCENT);
        ctx.fill(x, y, x + w, y + h, 0xFF0C0C0C);
        ctx.drawTextWithShadow(tr, Text.literal("§a" + toastMsg), x + 10, y + 6, 0xFFFFFFFF);
        rToast = new int[]{x, y, w, h};
    }

    private void renderHistory(DrawContext ctx, TextRenderer tr, int mx, int my, int width, int height) {
        histRects.clear(); histJumps.clear();
        // §G27.22 owner override to OPAQUE_MODALS: dimmed backdrop (screen faintly visible), the box stays opaque.
        ctx.fill(0, 0, width, height, 0x99000000);
        int pw = 244;
        int contentH = 46 + (undoHist.size() + redoHist.size() + 1) * 13;
        int ph = BulkDraw.clamp(contentH, 80, height - 60);
        int px = (width - pw) / 2, py = (height - ph) / 2;
        rBox = new int[]{px, py, pw, ph};
        ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, CbTheme.ACCENT);
        ctx.fill(px, py, px + pw, py + ph, 0xFF0C0C0C);   // opaque box (occlusion still holds — only the backdrop is dim)

        int y = py + 6;
        ctx.drawTextWithShadow(tr, CbTheme.red("History"), px + 8, y, 0xFFFFFFFF);
        rHistClose = new int[]{px + pw - 18, py + 4, 14, 14};
        BulkDraw.btn(ctx, tr, mx, my, "✖", rHistClose, true);
        y += 16;

        int listBottom = py + ph - 6;
        ctx.enableScissor(px, y, px + pw, listBottom);
        for (int i = redoHist.size() - 1; i >= 0; i--) y = row(ctx, tr, mx, my, px, pw, y, "§8" + redoHist.get(i), -(i + 1));
        ctx.fill(px + 8, y + 5, px + pw - 8, y + 6, CbTheme.ACCENT);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("§7— now —"), px + pw / 2, y, 0xFFFFFFFF);
        y += 13;
        for (int i = 0; i < undoHist.size(); i++) y = row(ctx, tr, mx, my, px, pw, y, "§f↩ " + undoHist.get(i), i + 1);
        if (undoHist.isEmpty() && redoHist.isEmpty())
            ctx.drawTextWithShadow(tr, Text.literal("§8(no changes yet this session)"), px + 12, y, 0xFFFFFFFF);
        ctx.disableScissor();
    }

    private int row(DrawContext ctx, TextRenderer tr, int mx, int my, int px, int pw, int y, String label, int jump) {
        int[] r = {px + 6, y, pw - 12, 12};
        if (BulkDraw.in(mx, my, r)) ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], BulkDraw.ROW_HOV);
        ctx.drawTextWithShadow(tr, Text.literal(label), px + 10, y + 2, 0xFFFFFFFF);
        histRects.add(r); histJumps.add(jump);
        return y + 13;
    }

    /** Handle a click on the toast, the History toggle button, or a history step. Returns true if consumed. */
    boolean click(double mx, double my, int[] rHistoryBtn) {
        if (toastMsg != null && BulkDraw.in(mx, my, rToast)) { toastMsg = null; return true; }
        if (BulkDraw.in(mx, my, rHistoryBtn)) { CbUiSounds.click(); open = !open; return true; }
        if (open) {
            if (BulkDraw.in(mx, my, rHistClose)) { CbUiSounds.click(); open = false; return true; }
            for (int i = 0; i < histRects.size(); i++) {
                if (BulkDraw.in(mx, my, histRects.get(i))) {
                    int jump = histJumps.get(i);
                    ClientPlayNetworking.send(new BulkActionPayload(jump > 0 ? BulkActionPayload.UNDO : BulkActionPayload.REDO,
                            "", String.valueOf(Math.abs(jump)), "", ""));
                    CbUiSounds.click();
                    return true;
                }
            }
            // The popup is modal over a dimmed backdrop — swallow every click while open; one outside the box closes it.
            if (!BulkDraw.in(mx, my, rBox)) open = false;
            return true;
        }
        return false;
    }
}
