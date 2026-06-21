/**
 * HudBgShapes.java — GROUP 27 §G27.14 (HUD shape backgrounds). CLIENT-SIDE ONLY.
 *
 * Responsibility: the per-brick background-shape draw helpers used by HudRenderer.drawBackground.
 * Pure geometry on a DrawContext, drawn in the brick's local space (origin 0,0, size boxW×boxH,
 * already translated + scaled by the caller). Split out of HudRenderer so that file stays under the
 * 500-line gate (CLAUDE.md §5.1). DrawContext.fill only does axis-aligned rects, so rounded corners
 * are built from per-row horizontal spans.
 *
 * Shapes: PILL (rounded capsule + thin left accent stripe), GLOW_BOX (flat fill + accent border +
 * brighter top glow strip). BOX (flat rect) and PLAIN (nothing) are handled inline in HudRenderer.
 *
 * Depends on: nothing (DrawContext only).
 * Called by: HudRenderer.drawBackground.
 */
package com.customblocks.client.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public final class HudBgShapes {

    private HudBgShapes() {}

    /** Rounded capsule fill + a thin accent stripe down the left. */
    public static void pill(DrawContext ctx, int w, int h, int argb, int accentRgb) {
        int r = Math.min(h / 2, w / 2);                 // full capsule = corner radius half-height
        roundedRect(ctx, w, h, r, argb);
        // Accent stripe: short vertical bar just inside the rounded left edge.
        int sx = Math.max(2, r / 2);
        int top = Math.max(2, h / 5);
        ctx.fill(sx, top, sx + 2, h - top, 0xFF000000 | (accentRgb & 0xFFFFFF));
    }

    /** Flat inset fill + a 1px accent border on all sides + a brighter accent glow strip on top. */
    public static void glowBox(DrawContext ctx, int w, int h, int argb, int accentRgb) {
        int acc  = 0xFF000000 | (accentRgb & 0xFFFFFF);
        int glow = (0x66 << 24) | (accentRgb & 0xFFFFFF);
        ctx.fill(0, 0, w, h, argb);                     // base fill
        ctx.fill(1, 1, w - 1, 3, glow);                 // top glow strip
        ctx.fill(0, 0, w, 1, acc);                      // top border
        ctx.fill(0, h - 1, w, h, acc);                  // bottom border
        ctx.fill(0, 0, 1, h, acc);                      // left border
        ctx.fill(w - 1, 0, w, h, acc);                  // right border
    }

    /** Axis-aligned rounded rectangle filled with {@code argb}, built from per-row spans. */
    public static void roundedRect(DrawContext ctx, int w, int h, int r, int argb) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) { ctx.fill(0, 0, w, h, argb); return; }
        for (int y = 0; y < h; y++) {
            int dy = Math.max(r - y, r - (h - 1 - y));  // >0 inside a top/bottom corner band
            int inset = 0;
            if (dy > 0) inset = (int) Math.round(r - Math.sqrt((double) r * r - (double) dy * dy));
            ctx.fill(inset, y, w - inset, y + 1, argb);
        }
    }
}
