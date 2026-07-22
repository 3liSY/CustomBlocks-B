/**
 * SauceScreenOverlay.java — Group 32 (Explosive Tomato) Phase E, E6/E8. CLIENT-ONLY.
 *
 * The screen sauce a point-blank blast paints (owner-locked 2026-07-17): a fresh RANDOM scattered layout that
 * covers ~90% of the screen — splashes, smears, gravity drips — with ~10% scattered clear holes so the view stays
 * partly readable. It is NOT a fixed edge border. It holds, then fully disappears after 10 seconds. Every hit
 * rolls a new layout ({@link #trigger}); the layout is generated ONCE per hit and stored, so it stays stable
 * frame-to-frame and only its alpha dries out — no per-frame flicker.
 *
 * Triggered by {@code SauceHitPayload} for both a direct hit (E8) and a blocked point-blank hit (E6, the blocker
 * is inside the blast radius). Purely local eye-candy — no gameplay, one "you got hit" signal from the server.
 *
 * Depends on: DrawContext, Util (wall-clock timer)
 * Called by: CustomBlocksClient (SauceHitPayload receiver → trigger), HudRenderMixin (render)
 */
package com.customblocks.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public final class SauceScreenOverlay {

    private SauceScreenOverlay() {}

    /** Sauce cell grid — a fine scatter grid across the whole screen (not an edge band). */
    private static final int COLS = 28;
    private static final int ROWS = 16;
    /** ~90% of cells are sauced; the rest are scattered clear holes (owner-locked readability). */
    private static final float COVERAGE = 0.90f;
    /** Approved sauce reds the cells pick from (deep / dark / brighter puree). */
    private static final int[] SAUCE_REDS = { 0xB41410, 0x8E0E0B, 0xC8331E, 0xA01712 };
    /** Fixed 10s lifetime, then fully gone (owner-locked). */
    private static final long DURATION_MS = 10_000L;

    private static long startMs = 0L;
    private static float peak = 0f;

    // Per-cell layout, regenerated per hit and held stable while it dries.
    private static final boolean[] covered = new boolean[COLS * ROWS];
    private static final int[] cellRgb = new int[COLS * ROWS];
    private static final float[] cellAlpha = new float[COLS * ROWS];
    private static final float[] cellInset = new float[COLS * ROWS];
    private static final int[] cellDrip = new int[COLS * ROWS]; // extra downward run, in cell fractions ×100

    /** A blast caught the local player: roll a fresh ~90% scattered layout and (re)start the 10s dry-out. */
    public static void trigger(float intensity) {
        float i = MathHelper.clamp(intensity, 0f, 1f);
        long now = Util.getMeasuringTimeMs();
        peak = Math.max(currentFactor(now), 0.55f + i * 0.45f); // closer hits start heavier; keep the stronger of overlapping hits
        startMs = now;
        Random rnd = Random.create();
        for (int c = 0; c < COLS * ROWS; c++) {
            boolean on = rnd.nextFloat() < COVERAGE;
            covered[c] = on;
            if (!on) continue;
            cellRgb[c] = SAUCE_REDS[rnd.nextInt(SAUCE_REDS.length)];
            cellAlpha[c] = 0.62f + rnd.nextFloat() * 0.38f;    // patchy opacity → smears + solid blobs
            cellInset[c] = rnd.nextFloat() * 0.35f;            // random shrink → irregular, non-grid edges
            cellDrip[c] = rnd.nextFloat() < 0.22f ? 40 + rnd.nextInt(160) : 0; // some cells hang a gravity drip
        }
    }

    /** True while the overlay is still drawing (used to also tint the local player's blocking shield). */
    public static boolean active() {
        return currentFactor(Util.getMeasuringTimeMs()) > 0.02f;
    }

    private static float currentFactor(long now) {
        long elapsed = now - startMs;
        if (elapsed < 0 || elapsed >= DURATION_MS) return 0f;
        float progress = elapsed / (float) DURATION_MS;
        // hold strong for the first ~60% of its life, then dry out to nothing — it "lasts 10s" then disappears.
        float dry = progress < 0.6f ? 1f : 1f - (progress - 0.6f) / 0.4f;
        return peak * dry;
    }

    public static void render(DrawContext context) {
        float f = currentFactor(Util.getMeasuringTimeMs());
        if (f <= 0.02f) return;
        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        float cw = w / (float) COLS;
        float ch = h / (float) ROWS;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int idx = row * COLS + col;
                if (!covered[idx]) continue;                       // scattered clear hole → readability
                int a = (int) (255 * cellAlpha[idx] * f);
                if (a <= 3) continue;
                int color = (a << 24) | (cellRgb[idx] & 0xFFFFFF);
                float ins = cellInset[idx] * cw * 0.5f;
                int x0 = (int) (col * cw + ins);
                int y0 = (int) (row * ch + ins);
                int x1 = (int) ((col + 1) * cw - ins);
                int y1 = (int) ((row + 1) * ch - ins);
                context.fill(x0, y0, x1, y1, color);
                if (cellDrip[idx] > 0) {                            // gravity drip hanging below the blob
                    int dripLen = (int) (cellDrip[idx] / 100f * ch * f);
                    int dw = Math.max(2, (int) (cw * 0.28f));
                    int dx = (x0 + x1) / 2 - dw / 2;
                    int da = (int) (a * 0.85f);
                    context.fill(dx, y1, dx + dw, y1 + dripLen, (da << 24) | (cellRgb[idx] & 0xFFFFFF));
                }
            }
        }
    }
}
