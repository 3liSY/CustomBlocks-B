/**
 * BulkResultPanel.java — Group 07 (Bulk Operations Hub — Bulk Actions tab). CLIENT-ONLY.
 *
 * The §G27.22b right RESULT-PREVIEW panel, split out of {@link BulkOpsView} to hold the ≤500-line cap. Header
 * "🔎 RESULT PREVIEW" (hand-drawn magnifier — B11), the first still-affected sample block, a before→after pair
 * of larger rotating cubes with captions, the {@code old → new} line, then a SUMMARY block (✓ change N / 🔒 skip
 * / of T selected) with a green proportion bar. Reads the Screen's live preview + impact; updates every frame,
 * so an op switch or a checkbox toggle re-renders it for free. No state, no click targets.
 *
 * Depends on: BulkDraw, BulkOpsView (geometry), BulkOpText, BulkWorkbenchModel, BulkCube, CbTheme.
 * Called by: BulkOpsView.render.
 */
package com.customblocks.client.gui;

import com.customblocks.client.ClientSlotCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import static com.customblocks.client.gui.BulkWorkbenchView.CONTENT_Y;
import static com.customblocks.client.gui.BulkWorkbenchView.contentBottom;
import static com.customblocks.client.gui.BulkWorkbenchView.contentRight;

@Environment(EnvType.CLIENT)
final class BulkResultPanel {

    void render(DrawContext ctx, TextRenderer tr, BulkWorkbenchScreen s, int width, int height) {
        int pw = BulkOpsView.PANEL_W;
        int px = contentRight(width) - pw, py = CONTENT_Y, ph = contentBottom(height) - py;
        BulkDraw.panel(ctx, px, py, pw, ph);

        magnifier(ctx, px + 6, py + 5);
        ctx.drawTextWithShadow(tr, CbTheme.red("RESULT PREVIEW"), px + 16, py + 5, 0xFFFFFFFF);
        ctx.fill(px + 6, py + 17, px + pw - 6, py + 18, CbTheme.ACCENT_DIM);

        // The first still-affected sample (skips locked/no-op rows) drives the before→after cubes.
        BulkWorkbenchModel.PreviewRow sample = null;
        for (BulkWorkbenchModel.PreviewRow r : s.preview()) if (!r.skipped()) { sample = r; break; }

        int y = py + 24;
        if (sample == null) {
            ctx.drawTextWithShadow(tr, Text.literal("§8(nothing to preview —"), px + 8, y, 0xFFFFFFFF);
            ctx.drawTextWithShadow(tr, Text.literal("§8 tick blocks + set an action)"), px + 8, y + 11, 0xFFFFFFFF);
        } else {
            ClientSlotCache.Entry e = BulkWorkbenchModel.byId(sample.id());
            String name = e != null ? e.name() : sample.id();
            ctx.drawCenteredTextWithShadow(tr, Text.literal("§f" + BulkDraw.fit(tr, name, pw - 12)), px + pw / 2, y, 0xFFFFFFFF);
            y += 14;
            int lx = px + pw / 4, rx = px + pw * 3 / 4;
            s.cube().render(ctx, sample.id(), lx, y + 16, 15);
            s.cube().render(ctx, sample.id(), rx, y + 16, 15);
            if (s.opIndex() == BulkOpSpec.OP_DELETE) ctx.fill(rx - 15, y + 1, rx + 15, y + 31, 0x99000000); // "gone" dim
            ctx.drawCenteredTextWithShadow(tr, Text.literal("§8before"), lx, y + 34, 0xFFFFFFFF);
            ctx.drawCenteredTextWithShadow(tr, Text.literal("§8after"), rx, y + 34, 0xFFFFFFFF);
            ctx.drawTextWithShadow(tr, CbTheme.red("→"), px + pw / 2 - 3, y + 12, 0xFFFFFFFF);
            y += 46;
            String line = "§8" + BulkDraw.fit(tr, sample.before(), 60) + " §7→ §a" + BulkDraw.fit(tr, sample.after(), 60);
            ctx.drawCenteredTextWithShadow(tr, Text.literal(line), px + pw / 2, y, 0xFFFFFFFF);
            y += 16;
        }

        // SUMMARY + green proportion bar, anchored near the bottom.
        int[] imp = BulkOpText.impact(s);
        int total = s.ticked().size();
        int sy = Math.max(y + 6, py + ph - 46);
        ctx.fill(px + 6, sy - 4, px + pw - 6, sy - 3, CbTheme.ACCENT_DIM);
        ctx.drawTextWithShadow(tr, Text.literal("§fSUMMARY"), px + 8, sy, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, Text.literal("§a✓ change " + imp[0]
                + (imp[1] > 0 ? "   §6skip " + imp[1] : "")), px + 8, sy + 11, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, Text.literal("§7of " + total + " selected"), px + 8, sy + 22, 0xFFFFFFFF);
        int barX = px + 8, barW = pw - 16, barY = sy + 33;
        ctx.fill(barX, barY, barX + barW, barY + 4, 0xFF1A1A1A);
        int fill = total == 0 ? 0 : barW * imp[0] / total;
        ctx.fill(barX, barY, barX + fill, barY + 4, CbTheme.LIME);
    }

    /** A tiny hand-drawn magnifier (🔎 is a missing glyph in MC's font — B11 ruling). */
    private void magnifier(DrawContext ctx, int x, int y) {
        int w = 0xFFFFFFFF;
        ctx.fill(x + 1, y, x + 5, y + 1, w);
        ctx.fill(x, y + 1, x + 1, y + 5, w);
        ctx.fill(x + 5, y + 1, x + 6, y + 5, w);
        ctx.fill(x + 1, y + 5, x + 5, y + 6, w);
        ctx.fill(x + 5, y + 5, x + 8, y + 8, w); // handle
    }
}
