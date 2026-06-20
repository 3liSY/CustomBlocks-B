/**
 * CbColorDropper.java — Group 27 §G27.7 §B4 in-panel eyedrop + loupe. CLIENT-ONLY.
 *
 * Responsibility: an in-screen colour dropper the floating panel owns, so the player never leaves the
 * screen to sample a colour (unlike the standalone EyedropScreen). When armed it freezes the just-drawn
 * frame once (one framebuffer read-back — no per-frame capture, so it stays lag-free), then draws a loupe
 * magnifier that follows the cursor showing the exact pixels under it plus the hex; a click reports the
 * centre pixel's colour back to the panel. Samples MC's own framebuffer only (same as EyedropScreen).
 *
 * Flow: host screen calls {@link #preRender} at the very top of render() (captures the previous frame,
 * world + block, before this frame's UI is drawn); {@link #render} draws the loupe; the panel calls
 * {@link #sample} on click while {@link #isActive}.
 *
 * Depends on: MinecraftClient framebuffer, ScreenshotRecorder + NativeImage.
 * Called by: client/gui/panel/CbColorPanel.
 */
package com.customblocks.client.gui.panel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class CbColorDropper {

    private static final int GOLD = 0xFF_FF_AA_00;
    private static final int SRC_R = 6;     // sample radius in framebuffer px → (2R+1)² loupe grid
    private static final int CELL  = 7;      // on-screen size of each magnified source pixel

    private boolean active;
    private boolean pendingCapture;
    private NativeImage frozen;              // the frozen frame we sample from while active

    public boolean isActive() { return active; }

    /** Arm the dropper — the next preRender freezes the frame and the loupe goes live. */
    public void activate() {
        cancel();
        active = true;
        pendingCapture = true;
    }

    /** Disarm + free the frozen frame. */
    public void cancel() {
        active = false;
        pendingCapture = false;
        if (frozen != null) { try { frozen.close(); } catch (Exception ignored) {} frozen = null; }
    }

    /** Call at the very top of the host render(), before any of this frame's UI is drawn. */
    public void preRender(DrawContext ctx) {
        if (!pendingCapture) return;
        pendingCapture = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) { active = false; return; }
        try {
            frozen = ScreenshotRecorder.takeScreenshot(mc.getFramebuffer());
        } catch (Exception e) {
            frozen = null;
            active = false;
        }
    }

    /** Draw the magnifier loupe at the cursor while armed. */
    public void render(DrawContext ctx, TextRenderer tr, int mx, int my, int screenW, int screenH) {
        if (!active || frozen == null) return;
        int fx = toFb(mx), fy = toFb(my);

        int grid = (2 * SRC_R + 1);
        int box  = grid * CELL;
        // Keep the loupe on-screen: prefer down-right of the cursor, flip when near an edge.
        int lx = mx + 16; if (lx + box > screenW) lx = mx - 16 - box;
        int ly = my + 16; if (ly + box + 14 > screenH) ly = my - 16 - box - 14;

        ctx.fill(lx - 2, ly - 2, lx + box + 2, ly + box + 2, GOLD);
        for (int gy = 0; gy < grid; gy++) {
            for (int gx = 0; gx < grid; gx++) {
                int rgb = readRgb(fx - SRC_R + gx, fy - SRC_R + gy);
                int x = lx + gx * CELL, y = ly + gy * CELL;
                ctx.fill(x, y, x + CELL, y + CELL, 0xFF000000 | rgb);
            }
        }
        // Centre reticle over the pixel that a click will sample.
        int cx = lx + SRC_R * CELL, cy = ly + SRC_R * CELL;
        ctx.fill(cx - 1, cy - 1, cx + CELL + 1, cy, 0xFFFFFFFF);
        ctx.fill(cx - 1, cy + CELL, cx + CELL + 1, cy + CELL + 1, 0xFFFFFFFF);
        ctx.fill(cx - 1, cy, cx, cy + CELL, 0xFFFFFFFF);
        ctx.fill(cx + CELL, cy, cx + CELL + 1, cy + CELL, 0xFFFFFFFF);

        int center = readRgb(fx, fy);
        ctx.fill(lx - 2, ly + box + 2, lx + box + 2, ly + box + 14, 0xF0101010);
        ctx.drawTextWithShadow(tr, Text.literal(String.format(Locale.ROOT, "§f#%06X §7click·Esc", center)),
                lx, ly + box + 4, 0xFFFFFFFF);
    }

    /** Read the colour under the cursor + disarm. Returns -1 if unavailable. */
    public int sample(int mx, int my) {
        if (!active || frozen == null) { cancel(); return -1; }
        int rgb = readRgb(toFb(mx), toFb(my));
        cancel();
        return rgb;
    }

    private int toFb(int guiCoord) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double sf = mc != null ? mc.getWindow().getScaleFactor() : 1.0;
        return (int) Math.round(guiCoord * sf);
    }

    /** 0xRRGGBB at a framebuffer pixel (NativeImage packs little-endian ABGR), clamped to bounds. */
    private int readRgb(int fx, int fy) {
        fx = Math.max(0, Math.min(frozen.getWidth() - 1, fx));
        fy = Math.max(0, Math.min(frozen.getHeight() - 1, fy));
        int abgr = frozen.getColor(fx, fy);
        int r = abgr & 0xFF, g = (abgr >> 8) & 0xFF, b = (abgr >> 16) & 0xFF;
        return (r << 16) | (g << 8) | b;
    }
}
