/**
 * HudWidgetRenderer.java — GROUP 03. CLIENT-SIDE ONLY.
 *
 * Draws the 2 widgets into the shared CustomBlocks overlay layer (the one HudRenderMixin owns, which is
 * also where Group 04's chat flourishes render — we never touch vanilla chat or the vanilla HUD).
 *
 * Everything here is DISPLAY-ONLY. There is no click dispatch, no hover, no hit-testing — by design, and
 * not for lack of effort: Minecraft grabs the mouse while you are playing, so no cursor exists to click a
 * HUD widget with. Each widget's action is the command it maps to (see HudWidgetType.action()).
 *
 * Geometry and colour come straight from HudWidgetType. There is no per-player layout: the store, the
 * Widgets tab and the colour picker were cut on 2026-07-14 (ADR-017).
 *
 * Depends on: HudWidgetType (geometry + colour), WidgetSignals (state), CbTheme (colours).
 * Called by:  CbOverlay, from HudRenderMixin.
 */
package com.customblocks.client.hud.widget;

import com.customblocks.client.gui.CbTheme;
import com.customblocks.client.hud.HudField.Anchor;
import com.customblocks.client.hud.HudFieldType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;

@Environment(EnvType.CLIENT)
public final class HudWidgetRenderer {

    private static final int PAD = 3;

    /** Cap the pulse maths to a sane tick source shared by every animated widget. */
    private static long nowMs() { return System.currentTimeMillis(); }

    private HudWidgetRenderer() {} // static-only

    public static void render(DrawContext ctx, MinecraftClient mc, HudFieldType.Ctx look) {
        TextRenderer tr = mc.textRenderer;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        // ACTIVE_TOOL and its drawActiveTool are DELETED (G03-TOOLCHIP, 2026-07-15) — the chip rendered a
        // persistent tool MODE, not "is holding the tool", so it never cleared. Two widgets left.
        drawMacroBanner(ctx, tr, sw, sh);
        drawPadlock(ctx, sw, sh, look);
    }

    // ── widgets ──────────────────────────────────────────────────────────────

    /**
     * Pulses hard between red and white while recording, and cannot be missed — that is the requirement,
     * because the failure it prevents is building for ten minutes without realising you're still recording.
     */
    private static void drawMacroBanner(DrawContext ctx, TextRenderer tr, int sw, int sh) {
        if (!WidgetSignals.macroRecording) return;
        HudWidgetType t = HudWidgetType.MACRO_BANNER;
        boolean flash = (nowMs() / 400L) % 2 == 0;           // ~2.5 Hz — assertive, not a strobe
        int col = flash ? CbTheme.ACCENT : CbTheme.TEXT;
        String name = WidgetSignals.macroName;
        String text = "● REC" + (name == null || name.isEmpty() ? "" : "  " + name)
                + "   /cb macro stop";
        int tw = tr.getWidth(text);
        int[] p = pos(t, tw + PAD * 2, tr.fontHeight + PAD * 2, sw, sh);
        panel(ctx, p[0], p[1], tw + PAD * 2, tr.fontHeight + PAD * 2, col);
        ctx.drawTextWithShadow(tr, text, p[0] + PAD, p[1] + PAD, col);
    }

    /**
     * The 7×9 padlock sprite, 10px BELOW the crosshair and horizontally centred — never on it (locked
     * 2026-07-15). Aim must stay clear, so the offset from screen centre is deliberately non-zero. Only
     * drawn while the crosshair is actually on a locked custom block.
     *
     * DRAWN, not typed. This was the 🔒 font glyph, which is a missing-glyph risk on any resource pack whose
     * font lacks it (the same reason Group 27's Bulk grid draws its own — see G27 T9): a padlock that
     * silently becomes a tofu box is worse than no padlock. Every pixel below is a fill() in CB red, with a
     * highlight column down the lit (left) side and a darker right/bottom edge so it reads as a solid object
     * rather than a flat blob. The sprite carries no owner identity, so LockManager stays the flat set it is.
     */
    private static void drawPadlock(DrawContext ctx, int sw, int sh, HudFieldType.Ctx look) {
        if (look == null || !look.hasBlock() || !WidgetSignals.isLocked(look.id())) return;
        HudWidgetType t = HudWidgetType.LOCKED_PADLOCK;
        int x = sw / 2 + t.offsetX() - PADLOCK_W / 2;
        int y = sh / 2 + t.offsetY();
        sprite(ctx, x, y, PADLOCK);
    }

    // ── the padlock sprite ───────────────────────────────────────────────────

    private static final int PADLOCK_W = 7;

    /** Lit-side highlight, base CB red, shadowed edge, and the keyhole. */
    private static final int LOCK_HI   = 0xFFFF6B6B;
    private static final int LOCK_BASE = CbTheme.ACCENT;      // 0xFFFF0000
    private static final int LOCK_DARK = CbTheme.ACCENT_DIM;  // 0xFF7A0000
    private static final int LOCK_HOLE = 0xFF3A0000;

    /**
     * 7 wide × 9 tall. Rows 0–2 are the shackle (an open arch, so it reads as a padlock at this size and
     * not as a die), rows 3–8 the body, with the keyhole punched down the middle.
     *   H = highlight · B = base · D = dark edge · K = keyhole · '.' = transparent
     */
    private static final String[] PADLOCK = {
            "..BBB..",
            ".H...D.",
            ".H...D.",
            "HBBBBBD",
            "HBBBBBD",
            "HBBKBBD",
            "HBBKBBD",
            "HBBBBBD",
            "DDDDDDD",
    };

    /** Blit a char-map sprite one pixel at a time. Tiny by design — this is a 63-pixel image. */
    private static void sprite(DrawContext ctx, int x, int y, String[] rows) {
        for (int ry = 0; ry < rows.length; ry++) {
            String row = rows[ry];
            for (int rx = 0; rx < row.length(); rx++) {
                int col = switch (row.charAt(rx)) {
                    case 'H' -> LOCK_HI;
                    case 'B' -> LOCK_BASE;
                    case 'D' -> LOCK_DARK;
                    case 'K' -> LOCK_HOLE;
                    default  -> 0; // transparent
                };
                if (col != 0) ctx.fill(x + rx, y + ry, x + rx + 1, y + ry + 1, col);
            }
        }
    }

    // ── shared drawing ───────────────────────────────────────────────────────

    /** Black panel + a hairline of the widget's colour — the mod-wide red/black/lime look, reused as-is. */
    private static void panel(DrawContext ctx, int x, int y, int w, int h, int accent) {
        ctx.fill(x, y, x + w, y + h, CbTheme.CARD);
        ctx.fill(x, y, x + w, y + 1, accent);
    }

    /** Resolve a widget's anchor + offset into a screen position. Mirrors the Group 27 brick geometry. */
    private static int[] pos(HudWidgetType t, int width, int height, int sw, int sh) {
        Anchor a = t.anchor();
        int x = switch (a) {
            case TR, BR  -> sw - t.offsetX() - width;
            case CENTER  -> sw / 2 + t.offsetX() - width / 2;
            default      -> t.offsetX();
        };
        int y = switch (a) {
            case BL, BR  -> sh - t.offsetY() - height;
            case CENTER  -> sh / 2 + t.offsetY() - height / 2;
            default      -> t.offsetY();
        };
        return new int[] { x, y };
    }
}
