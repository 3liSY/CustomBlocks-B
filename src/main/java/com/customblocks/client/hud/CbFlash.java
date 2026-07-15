/**
 * CbFlash.java — GROUP 04 §G04-4 "animated confirm". CLIENT-SIDE ONLY.
 *
 * When you click ✔ Yes on a destructive confirm, the action runs immediately — and a flash/shake plays
 * to sell it.
 *
 * The flourish plays HERE, in the CustomBlocks overlay layer, and NOT in the chat line, because a chat
 * line physically cannot animate: once a message is sent, vanilla exposes no way to re-render it. That is
 * the same limitation that killed the "live-updating chat line" idea. Rather than fight it (or worse,
 * reimplement vanilla chat, which is the one thing Group 04 forbids), the animation lives in the layer we
 * own, right next to chat.
 *
 * Depends on: CbTheme.
 * Called by:  CbOverlay (draw), CustomBlocksClient (trigger, on the confirm payload).
 */
package com.customblocks.client.hud;

import com.customblocks.client.gui.CbTheme;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public final class CbFlash {

    /** How long the whole flourish lasts. Long enough to register, short enough never to be in the way. */
    private static final long DURATION_MS = 450L;

    private static long startedAt = 0L;
    private static boolean destructive = false;

    private CbFlash() {} // static-only

    /**
     * Start the flourish.
     *
     * @param wasDestructive true for a delete/reset confirm (red shake), false for a plain success (lime pulse)
     */
    public static void play(boolean wasDestructive) {
        startedAt = System.currentTimeMillis();
        destructive = wasDestructive;
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (startedAt == 0L) return;
        long age = System.currentTimeMillis() - startedAt;
        if (age > DURATION_MS) { startedAt = 0L; return; }

        float t = 1f - (age / (float) DURATION_MS);          // 1 → 0
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        // A vignette rather than a full-screen wash: it reads as "that happened" without blinding anyone
        // or hiding the world at the exact moment they want to see the result.
        int alpha = (int) (110 * t) & 0xFF;
        int rgb = (destructive ? CbTheme.ACCENT : CbTheme.LIME) & 0x00FFFFFF;
        int col = (alpha << 24) | rgb;

        int band = Math.max(2, (int) (10 * t));
        ctx.fill(0, 0, sw, band, col);                        // top
        ctx.fill(0, sh - band, sw, sh, col);                  // bottom
        ctx.fill(0, 0, band, sh, col);                        // left
        ctx.fill(sw - band, 0, sw, sh, col);                  // right
    }
}
