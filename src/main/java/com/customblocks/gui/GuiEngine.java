/**
 * GuiEngine.java
 *
 * Responsibility: Static drawing utilities shared by all CustomBlocks GUI screens.
 * Wraps DrawContext calls so individual screens stay concise.
 * CLIENT-SIDE ONLY.
 *
 * Depends on: Minecraft DrawContext, TextRenderer, net.fabricmc.api.Environment
 * Called by: all screen classes in gui/screens/
 */
package com.customblocks.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class GuiEngine {

    private GuiEngine() {}

    // Colour palette
    public static final int COL_TITLE  = 0xFFFFAA00; // gold
    public static final int COL_BODY   = 0xFFFFFFFF; // white
    public static final int COL_DIM    = 0xFFAAAAAA; // grey
    public static final int COL_BG     = 0xCC000000; // semi-transparent black overlay
    public static final int COL_HEADER = 0xCC222222; // darker header band

    /** Full-screen dark overlay. */
    public static void drawBackground(DrawContext ctx, int w, int h) {
        ctx.fill(0, 0, w, h, COL_BG);
    }

    /** Header band across the top 30px. */
    public static void drawHeader(DrawContext ctx, int w) {
        ctx.fill(0, 0, w, 30, COL_HEADER);
    }

    /** Centred gold title text at y. */
    public static void drawTitle(DrawContext ctx, TextRenderer tr, Text title, int w, int y) {
        ctx.drawCenteredTextWithShadow(tr, title, w / 2, y, COL_TITLE);
    }

    /** 1-pixel horizontal separator. */
    public static void drawSeparator(DrawContext ctx, int x1, int x2, int y) {
        ctx.fill(x1, y, x2, y + 1, 0xFF555555);
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
