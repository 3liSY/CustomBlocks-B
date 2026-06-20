/**
 * CbPanelHarmony.java — Group 27 §G27.7 §B3 harmony + tint/shade strip. CLIENT-ONLY.
 *
 * Responsibility: the "smart colour" suggestion block inside the floating panel — for the active
 * colour it offers harmony matches (complementary / triad / analogous) and a lighter→darker
 * tint-and-shade strip, all clickable. Pure presentation + hit-testing; the colour maths lives in
 * {@link com.customblocks.image.CbColorTools}. Kept separate so CbColorPanel stays under the
 * 500-line gate.
 *
 * Depends on: CbColorTools, DrawContext.
 * Called by: client/gui/panel/CbColorPanel.
 */
package com.customblocks.client.gui.panel;

import com.customblocks.image.CbColorTools;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
final class CbPanelHarmony {

    private static final int HS = 14, GAP = 2;

    /** Fixed vertical space the block occupies (label + matches row + tint/shade strip). */
    static final int HEIGHT = 41;

    // Last laid-out swatches (colour + hit-box), rebuilt every render() so clickedColor() matches.
    private int[] colors = new int[0];
    private int[] sx = new int[0], sy = new int[0];

    /** Draw the harmony matches + tint/shade strip; returns the total height consumed below {@code y}. */
    int render(DrawContext ctx, TextRenderer tr, int x, int y, int base, int mx, int my) {
        int b = base & 0xFFFFFF;
        int[] tri = CbColorTools.triad(b), ana = CbColorTools.analogous(b);
        int[] matches = { CbColorTools.complementary(b), tri[0], tri[1], ana[0], ana[1] };
        int[] strip   = CbColorTools.tintShadeStrip(b, 7, 0.6);

        colors = new int[matches.length + strip.length];
        sx = new int[colors.length];
        sy = new int[colors.length];

        ctx.drawTextWithShadow(tr, Text.literal("§7Harmony"), x, y, 0xFFFFFFFF);
        int rowY = y + 10;
        int i = 0;
        for (int c : matches) { drawSwatch(ctx, x + i * (HS + GAP), rowY, c, i, mx, my); i++; }

        int stripY = rowY + HS + 3;
        for (int j = 0; j < strip.length; j++) {
            drawSwatch(ctx, x + j * (HS + GAP), stripY, strip[j], matches.length + j, mx, my);
        }
        return (stripY + HS) - y;
    }

    private void drawSwatch(DrawContext ctx, int x, int y, int rgb, int idx, int mx, int my) {
        colors[idx] = rgb & 0xFFFFFF;
        sx[idx] = x; sy[idx] = y;
        boolean hover = mx >= x && mx < x + HS && my >= y && my < y + HS;
        ctx.fill(x - 1, y - 1, x + HS + 1, y + HS + 1, hover ? 0xFFFFFFFF : 0xFF000000);
        ctx.fill(x, y, x + HS, y + HS, 0xFF000000 | colors[idx]);
    }

    /** The colour of the swatch under the cursor, or -1. Uses the last render()'s layout. */
    int clickedColor(double mx, double my) {
        for (int i = 0; i < colors.length; i++) {
            if (mx >= sx[i] && mx < sx[i] + HS && my >= sy[i] && my < sy[i] + HS) return colors[i];
        }
        return -1;
    }
}
