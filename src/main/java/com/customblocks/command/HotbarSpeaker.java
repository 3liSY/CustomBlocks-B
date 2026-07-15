/**
 * HotbarSpeaker.java — the anti-strobe gate in front of every hotbar (action-bar) line (G04 §F).
 *
 * A tool swung fast fires the same feedback many times a second; with no gate the hotbar strobes.
 * The rule (owner-locked 2026-07-15): an IDENTICAL line inside 20 ticks (1.0s) of the last one is
 * swallowed; a DIFFERENT line always passes instantly. Dwell/fade is vanilla's own (~5s) — we never
 * reimplement it, we just decline to re-send the same text so the current line keeps dwelling.
 *
 * The actual send stays in {@link Chat#hotbar} (the one sanctioned {@code sendMessage(…, true)}), so
 * this class only makes the swallow decision: {@link #allow} returns whether the line reaches the bar.
 *
 * Used by: Chat only (package-private). Called on the server thread.
 */
package com.customblocks.command;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class HotbarSpeaker {

    private HotbarSpeaker() {} // static-only

    /** Identical-line swallow window: 20 ticks = 1.0s (owner-locked 2026-07-15). */
    private static final long SWALLOW_TICKS = 20L;

    /** The last hotbar line each player was shown, and the world tick it landed on. */
    private record Last(String text, long tick) {}
    private static final Map<UUID, Last> LAST = new ConcurrentHashMap<>();

    /**
     * True if {@code formatted} should actually reach {@code player}'s hotbar now — false if it is the
     * SAME line they were shown less than 20 ticks ago (swallow the repeat, keep the current dwell).
     * A different line always returns true and resets the window.
     */
    static boolean allow(ServerPlayerEntity player, String formatted) {
        long now = player.getWorld().getTime();
        Last last = LAST.get(player.getUuid());
        if (last != null && last.text().equals(formatted) && now - last.tick() < SWALLOW_TICKS) {
            return false;
        }
        LAST.put(player.getUuid(), new Last(formatted, now));
        return true;
    }
}
