/**
 * StudioAnimPanel.java — Group 14 Phase 2 (Block Creation Studio, "Animation" tab). CLIENT-SIDE ONLY.
 *
 * The studio's animation editor, drawn into the section panel area (same pattern as StudioCategoryPanel).
 * It lights up only when the loaded texture is a real multi-frame clip. Rewritten 2026-06-20 for a CALM,
 * plain-language layout (owner: "bloated and confusing… think of an artist / a normal human"): no fps/ticks
 * jargon, no dense readouts — just airy labelled groups and a live playback bar:
 *   - Speed:  Slower · Normal · Faster  (Normal = the clip's own real speed),
 *   - Loop:   Forward · Bounce · Reverse,
 *   - Smooth motion: On / Off (frame blend),
 *   - Trim:   cut the start/end frames with steppers (Reset = all frames).
 * Each control returns a NEW immutable {@link AnimData} into {@link StudioState#anim} — the screen's
 * "Save changes" / "Create & Publish" carries those plain numbers to the server.
 *
 * It also owns the LIVE PREVIEW clock: {@link #currentFrame} picks which frame grid the cube should show
 * right now, honoring the trim range, loop order and per-frame timing — so the preview plays exactly like
 * the finished block will. All controls are custom-drawn with hit-rects (computed in render, read in
 * mouseClicked), matching the studio's other pickers.
 *
 * Depends on: DrawContext / TextRenderer, StudioState, AnimData. Called by: StudioSections (Animation case).
 */
package com.customblocks.client.gui;

import com.customblocks.client.gui.studio.StudioState;
import com.customblocks.core.AnimData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class StudioAnimPanel {

    private static final int GOLD = 0xFF_FF_AA_00;
    private static final int PANEL_W = 196;          // playback-bar width inside the section panel
    private static final double SPEED_STEP = 1.5;    // Slower/Faster multiply the current speed by this

    // Hit-rects {x,y,w,h}, filled in render(), read in mouseClicked().
    private int[][] speedRects;  // Slower / Normal / Faster
    private int[][] loopRects;   // Forward / Bounce / Reverse
    private int[][] smoothRects; // On / Off
    private int[][] trimRects;   // start− start+ end− end+ Reset

    public void render(DrawContext ctx, TextRenderer tr, int x, int y, StudioState st, int mx, int my) {
        speedRects = loopRects = smoothRects = trimRects = null;

        if (!st.isAnimated()) {
            ctx.drawTextWithShadow(tr, Text.literal("§6§lAnimation"), x, y, 0xFFFFFFFF);
            if (st.animLoading)
                ctx.drawTextWithShadow(tr, Text.literal("§8loading frames…"), x, y + 16, 0xFFFFFFFF);
            else {
                ctx.drawTextWithShadow(tr, Text.literal("§8This block isn't animated."), x, y + 16, 0xFFFFFFFF);
                ctx.drawTextWithShadow(tr, Text.literal("§8Load a GIF/WebP in the Texture tab"), x, y + 28, 0xFFFFFFFF);
                ctx.drawTextWithShadow(tr, Text.literal("§8to unlock these controls."), x, y + 40, 0xFFFFFFFF);
            }
            return;
        }

        AnimData a = st.anim;
        ctx.drawTextWithShadow(tr, Text.literal("§6§lAnimation"), x, y, 0xFFFFFFFF);

        // Speed — friendly, no numbers. "Normal" = the clip's own real timing.
        ctx.drawTextWithShadow(tr, Text.literal("§eSpeed"), x, y + 18, 0xFFFFFFFF);
        speedRects = new int[3][4];
        int bx = btn(ctx, tr, speedRects, 0, x, y + 30, "Slower", false, mx, my);
        bx = btn(ctx, tr, speedRects, 1, bx, y + 30, "Normal", !a.isUniform(), mx, my);
        btn(ctx, tr, speedRects, 2, bx, y + 30, "Faster", false, mx, my);

        // Loop — play order.
        ctx.drawTextWithShadow(tr, Text.literal("§eLoop"), x, y + 54, 0xFFFFFFFF);
        loopRects = new int[3][4];
        bx = btn(ctx, tr, loopRects, 0, x, y + 66, "Forward", AnimData.LOOP.equals(a.loopMode()), mx, my);
        bx = btn(ctx, tr, loopRects, 1, bx, y + 66, "Bounce", AnimData.BOUNCE.equals(a.loopMode()), mx, my);
        btn(ctx, tr, loopRects, 2, bx, y + 66, "Reverse", AnimData.REVERSE.equals(a.loopMode()), mx, my);

        // Smooth motion.
        ctx.drawTextWithShadow(tr, Text.literal("§eSmooth motion"), x, y + 90, 0xFFFFFFFF);
        smoothRects = new int[2][4];
        bx = btn(ctx, tr, smoothRects, 0, x, y + 102, "On",  a.interpolate(), mx, my);
        btn(ctx, tr, smoothRects, 1, bx, y + 102, "Off", !a.interpolate(), mx, my);

        // Trim — the only "advanced" control, kept compact at the bottom.
        ctx.drawTextWithShadow(tr, Text.literal("§eTrim §8(cut start/end)"), x, y + 126, 0xFFFFFFFF);
        trimRects = new int[5][4];
        int ty = y + 138;
        ctx.drawTextWithShadow(tr, Text.literal("§7Start"), x, ty + 5, 0xFFFFFFFF);
        bx = step(ctx, tr, trimRects, 0, x + 38, ty, "−", mx, my);
        bx = step(ctx, tr, trimRects, 1, bx, ty, "+", mx, my);
        ctx.drawTextWithShadow(tr, Text.literal("§7End"), bx + 6, ty + 5, 0xFFFFFFFF);
        bx = step(ctx, tr, trimRects, 2, bx + 28, ty, "−", mx, my);
        bx = step(ctx, tr, trimRects, 3, bx, ty, "+", mx, my);
        btn(ctx, tr, trimRects, 4, bx + 6, ty, "Reset", a.trimStart() == 0 && a.trimEnd() == a.frameCount() - 1, mx, my);

        // Live playback bar — a moving fill so you can see it play (no numbers).
        drawPlaybackBar(ctx, tr, x, y + 164, st);
    }

    /** Returns true if a control was clicked (and applied the immutable change to st.anim). */
    public boolean mouseClicked(double mx, double my, StudioState st) {
        if (!st.isAnimated()) return false;
        AnimData a = st.anim;

        if (speedRects != null) {
            if (in(speedRects[0], mx, my)) { st.anim = scaled(a, 1.0 / SPEED_STEP); return true; } // Slower
            if (in(speedRects[1], mx, my)) { st.anim = a.withMatchOriginal(); return true; }        // Normal
            if (in(speedRects[2], mx, my)) { st.anim = scaled(a, SPEED_STEP); return true; }         // Faster
        }
        if (loopRects != null) {
            if (in(loopRects[0], mx, my)) { st.anim = a.withLoopMode(AnimData.LOOP); return true; }
            if (in(loopRects[1], mx, my)) { st.anim = a.withLoopMode(AnimData.BOUNCE); return true; }
            if (in(loopRects[2], mx, my)) { st.anim = a.withLoopMode(AnimData.REVERSE); return true; }
        }
        if (smoothRects != null) {
            if (in(smoothRects[0], mx, my)) { st.anim = a.withInterpolate(true); return true; }
            if (in(smoothRects[1], mx, my)) { st.anim = a.withInterpolate(false); return true; }
        }
        if (trimRects != null) {
            int s = a.trimStart(), e = a.trimEnd(), last = a.frameCount() - 1;
            if (in(trimRects[0], mx, my)) { st.anim = a.withTrim(s - 1, e); return true; }
            if (in(trimRects[1], mx, my)) { st.anim = a.withTrim(s + 1, e); return true; }
            if (in(trimRects[2], mx, my)) { st.anim = a.withTrim(s, e - 1); return true; }
            if (in(trimRects[3], mx, my)) { st.anim = a.withTrim(s, e + 1); return true; }
            if (in(trimRects[4], mx, my)) { st.anim = a.withTrim(0, last); return true; }
        }
        return false;
    }

    /** Set a flat speed = the current effective fps scaled by {@code factor} (>1 faster, <1 slower). */
    private static AnimData scaled(AnimData a, double factor) {
        double fps = Math.max(0.25, a.effectiveFps() * factor);
        int ticks = (int) Math.round(20.0 / fps);
        return a.withUniform(Math.max(1, Math.min(100, ticks))); // 1..100 ticks = 20fps..0.2fps
    }

    /**
     * The frame grid the live preview should show RIGHT NOW — honors the trim range, loop ordering and
     * per-frame timing (so the preview plays exactly like the finished block). Frame-swap only (the
     * in-world block does the real smoothing/cross-fade via the .mcmeta). Returns the static grid when
     * not animated.
     */
    public int[] currentFrame(StudioState st) {
        int[][] f = st.frames;
        if (f == null || f.length == 0) return st.grid;
        if (f.length == 1 || st.anim == null) return f[0];
        int[] pos = playbackPos(st);
        if (pos[1] == 0) return f[0];
        List<int[]> pb = st.anim.playback();
        if (pb.isEmpty() || pos[0] >= pb.size()) return f[0];
        return frameAt(f, pb.get(pos[0])[0]);
    }

    /**
     * The live playback position as {stepIndex, stepCount}: which step of the loop-ordered playback is
     * showing right now, given wall-clock time and the per-frame timing. {0,0} when not playing (frames
     * not loaded yet / empty). Shared by {@link #currentFrame} and the playback bar so both stay in sync.
     */
    private static int[] playbackPos(StudioState st) {
        int[][] f = st.frames;
        if (f == null || f.length <= 1 || st.anim == null) return new int[]{0, 0};
        List<int[]> pb = st.anim.playback(); // ordered {frameIndex, ticks}
        if (pb.isEmpty()) return new int[]{0, 0};
        long totalMs = 0;
        for (int[] p : pb) totalMs += Math.max(1, p[1]) * 50L;
        if (totalMs <= 0) return new int[]{0, pb.size()};
        long t = System.currentTimeMillis() % totalMs, acc = 0;
        for (int i = 0; i < pb.size(); i++) {
            acc += Math.max(1, pb.get(i)[1]) * 50L;
            if (t < acc) return new int[]{i, pb.size()};
        }
        return new int[]{pb.size() - 1, pb.size()};
    }

    private static int[] frameAt(int[][] f, int idx) {
        return (idx >= 0 && idx < f.length) ? f[idx] : f[0];
    }

    /** A thin progress bar with a moving fill showing the preview playing (no numbers, just motion). */
    private void drawPlaybackBar(DrawContext ctx, TextRenderer tr, int x, int y, StudioState st) {
        int w = PANEL_W, h = 6;
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        ctx.fill(x, y, x + w, y + h, 0xFF1A1A1A);
        int[] pos = playbackPos(st);
        if (pos[1] > 0) {
            int fillW = (int) ((long) (pos[0] + 1) * w / pos[1]);
            ctx.fill(x, y, x + fillW, y + h, GOLD);
        } else {
            ctx.drawTextWithShadow(tr, Text.literal("§8loading…"), x, y, 0xFFFFFFFF);
        }
    }

    /** Draw a labelled toggle button, record its rect, return the x just past it. */
    private static int btn(DrawContext ctx, TextRenderer tr, int[][] rects, int i, int x, int y,
                           String label, boolean active, int mx, int my) {
        int w = tr.getWidth(label) + 8;
        rects[i] = new int[]{x, y, w, 14};
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + 14;
        ctx.fill(x - 1, y - 1, x + w + 1, y + 15, active ? GOLD : (hov ? 0xFFBBBBBB : 0xFF000000));
        ctx.fill(x, y, x + w, y + 14, active ? 0xFF3A2E00 : 0xFF1A1A1A);
        ctx.drawTextWithShadow(tr, Text.literal((active ? "§e" : "§f") + label), x + 4, y + 3, 0xFFFFFFFF);
        return x + w + 4;
    }

    /** Draw a small square stepper (− / +), record its rect, return the x just past it. */
    private static int step(DrawContext ctx, TextRenderer tr, int[][] rects, int i, int x, int y,
                            String sym, int mx, int my) {
        rects[i] = new int[]{x, y, 14, 14};
        boolean hov = mx >= x && mx < x + 14 && my >= y && my < y + 14;
        ctx.fill(x - 1, y - 1, x + 15, y + 15, hov ? GOLD : 0xFF000000);
        ctx.fill(x, y, x + 14, y + 14, 0xFF2A2A2A);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("§f" + sym), x + 7, y + 3, 0xFFFFFFFF);
        return x + 18;
    }

    private static boolean in(int[] r, double mx, double my) {
        return r != null && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }
}
