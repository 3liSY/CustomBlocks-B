/**
 * HudEditorOverlays.java — GROUP 27 §G27.4 (Lego HUD Builder). CLIENT-SIDE ONLY.
 *
 * Responsibility: the two pure-drawing overlays for the HUD editor — the [?] shortcut help
 * panel and the "Discard changes?" cancel-confirm dialog. Drawing only; the editor owns the
 * open/close state and the confirm button hit-test.
 *
 * Depends on: nothing (DrawContext + TextRenderer).
 * Called by: HudEditorScreen.render().
 */
package com.customblocks.client.gui.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class HudEditorOverlays {

    private HudEditorOverlays() {}

    private static final int GOLD = 0xFFFFAA00;

    private static final String[] HELP_LINES = {
            "§7Drag a brick to place it · magnetic snap · Shift = free move",
            "§7Arrow keys nudge 1px · Shift+arrow 10px",
            "§7⠿ drag = reorder · 👁 show/hide · ⚙ inspector · ✕ delete",
            "§7[+ Add brick] palette · [Preset ▾] browse/save/share",
            "§8Ctrl+Z undo · Ctrl+Y redo · Ctrl+C copy · Ctrl+V paste",
            "§8Tab collapse panel · Enter save · Esc cancel · ? this help",
    };

    public static void drawHelp(DrawContext ctx, TextRenderer tr, int screenW, int screenH) {
        int pw = 360, ph = HELP_LINES.length * 14 + 34;
        int x = (screenW - pw) / 2, y = (screenH - ph) / 2;
        ctx.fill(x - 1, y - 1, x + pw + 1, y + ph + 1, GOLD);
        ctx.fill(x, y, x + pw, y + ph, 0xF0101010);
        ctx.drawTextWithShadow(tr, Text.literal("§6§lHUD Editor — Shortcuts"), x + 10, y + 8, 0xFFFFFFFF);
        for (int i = 0; i < HELP_LINES.length; i++)
            ctx.drawText(tr, Text.literal(HELP_LINES[i]), x + 10, y + 26 + i * 14, 0xFFFFFFFF, false);
        ctx.drawText(tr, Text.literal("§8click anywhere to close"), x + 10, y + ph - 12, 0xFFFFFFFF, false);
    }

    /** Outline a brick's bounds {x,y,w,h} in the preview (cyan while dragging, else faint white). */
    public static void drawSelection(DrawContext ctx, int[] b, boolean dragging, int cyan) {
        if (b == null) return;
        int c = dragging ? cyan : 0xFFFFFFAA;
        ctx.fill(b[0] - 1, b[1] - 1, b[0] + b[2] + 1, b[1], c);
        ctx.fill(b[0] - 1, b[1] + b[3], b[0] + b[2] + 1, b[1] + b[3] + 1, c);
        ctx.fill(b[0] - 1, b[1], b[0], b[1] + b[3], c);
        ctx.fill(b[0] + b[2], b[1], b[0] + b[2] + 1, b[1] + b[3], c);
    }

    public static void drawConfirm(DrawContext ctx, TextRenderer tr, int screenW, int screenH) {
        int cx = screenW / 2, cy = screenH / 2, pw = 220, ph = 80;
        ctx.fill(cx - pw / 2 - 1, cy - ph / 2 - 1, cx + pw / 2 + 1, cy + ph / 2 + 1, GOLD);
        ctx.fill(cx - pw / 2, cy - ph / 2, cx + pw / 2, cy + ph / 2, 0xFF1A1A1A);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("§fDiscard changes?"), cx, cy - 24, 0xFFFFFFFF);
        ctx.fill(cx - 104, cy + 6, cx - 8, cy + 26, 0xFFAA3333);
        ctx.fill(cx + 8, cy + 6, cx + 104, cy + 26, 0xFF333333);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("§cYes, discard"), cx - 56, cy + 11, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("Keep editing"), cx + 56, cy + 11, 0xFFFFFFFF);
    }
}
