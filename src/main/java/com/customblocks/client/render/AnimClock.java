/**
 * AnimClock.java — Group 14 / ADR-014 Step 2. CLIENT-ONLY.
 *
 * A monotonic MILLISECOND clock for off-atlas animation playback. The old renderer keyed off the integer
 * world tick ({@code world.getTime()}, 20/sec), which capped every clip at 20 fps and rounded sub-50ms frame
 * times away — so a ~30 fps GIF played ~50% too slow. This clock integrates REAL wall-clock time instead, so a
 * clip runs at its true source fps, and it FREEZES while the game is paused (singleplayer Esc menu) so a paused
 * world doesn't keep animating.
 *
 * Read once per render frame by {@link AnimSlotBER} (placed block) and {@link SlotItemRenderer} (item icon).
 * It accumulates in nanoseconds (then reports ms) so being read several times within one frame — once per
 * visible block/item — can't stall it the way integer-ms deltas under 1ms would. Single client render thread,
 * so plain statics are fine.
 *
 * Depends on: MinecraftClient (pause state).
 * Called by:  AnimSlotBER, SlotItemRenderer.
 */
package com.customblocks.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class AnimClock {

    private AnimClock() {} // static-only

    private static long accumNanos = 0L;
    private static long lastNanos = 0L;

    /** The current animation time in milliseconds — advances with real time, holds while the game is paused. */
    public static long nowMs() {
        long n = System.nanoTime();
        if (lastNanos != 0L) {
            MinecraftClient mc = MinecraftClient.getInstance();
            boolean paused = mc != null && mc.isPaused();
            if (!paused) accumNanos += (n - lastNanos);
        }
        lastNanos = n;
        return accumNanos / 1_000_000L;
    }
}
