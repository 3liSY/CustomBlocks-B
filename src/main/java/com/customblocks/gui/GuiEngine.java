/**
 * GuiEngine.java
 *
 * Responsibility: Static drawing utilities shared by all CustomBlocks GUI screens.
 * Wraps DrawContext calls so individual screens stay concise. Restyled to the locked
 * red+black palette (Group 27 correction #2, 2026-07-04) — one edit here re-skins all
 * five gui/screens/* navigation screens at once.
 * CLIENT-SIDE ONLY.
 *
 * Depends on: Minecraft DrawContext, TextRenderer, CbTheme, net.fabricmc.api.Environment
 * Called by: all screen classes in gui/screens/
 */
package com.customblocks.gui;

import com.customblocks.client.gui.CbTheme;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class GuiEngine {

    private GuiEngine() {}

    // Colour palette — locked red+black (was gold-on-grey).
    public static final int COL_TITLE  = CbTheme.ACCENT;   // #FF0000 red
    public static final int COL_BODY   = 0xFFFFFFFF;       // white
    public static final int COL_DIM    = 0xFFAAAAAA;       // grey
    public static final int COL_BG     = 0xCC000000;       // semi-transparent black overlay
    public static final int COL_HEADER = CbTheme.BAR_BG;   // near-solid black header band

    /** Full-screen dark overlay. */
    public static void drawBackground(DrawContext ctx, int w, int h) {
        ctx.fill(0, 0, w, h, COL_BG);
    }

    /** Header band across the top 30px + the thin red brand line under it. */
    public static void drawHeader(DrawContext ctx, int w) {
        ctx.fill(0, 0, w, 30, COL_HEADER);
        ctx.fill(0, 29, w, 30, CbTheme.ACCENT);
    }

    /** Centred bold exact-red title text at y. */
    public static void drawTitle(DrawContext ctx, TextRenderer tr, Text title, int w, int y) {
        ctx.drawCenteredTextWithShadow(tr, CbTheme.red(title.getString()), w / 2, y, COL_BODY);
    }

    /** 1-pixel horizontal separator (dim red hairline). */
    public static void drawSeparator(DrawContext ctx, int x1, int x2, int y) {
        ctx.fill(x1, y, x2, y + 1, CbTheme.ACCENT_DIM);
    }

    /** Small section label in dim grey. */
    public static void drawLabel(DrawContext ctx, TextRenderer tr, String label, int x, int y) {
        ctx.drawTextWithShadow(tr, Text.literal("§7" + label), x, y, COL_DIM);
    }

    /** Filled rectangle helper. */
    public static void drawBox(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + h, color);
    }
}
