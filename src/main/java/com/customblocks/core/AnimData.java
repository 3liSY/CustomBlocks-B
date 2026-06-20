/**
 * AnimData.java
 *
 * Responsibility: Immutable snapshot of one slot's ANIMATION state (Group 14 Part A). Held as a
 * single field on {@link SlotData} so the slot record only grows by one. Every "change" returns a
 * NEW AnimData via withX() — never mutate in place (design rule #2, same contract as SlotData).
 *
 * Stores PLAIN NUMBERS only (frame count, original per-frame times in ticks, a uniform-speed
 * override, loop mode, interpolate, trim range, transparency) — NEVER a baked ".mcmeta" string. The
 * sidecar .mcmeta is regenerated deterministically at pack-build (ServerPackGenerator), designing out
 * the OLD project's bug where per-frame timing was lost on save. This class owns the frame-INDEX
 * playback order for Loop / Bounce / Reverse (no pixel duplication — only index references change).
 *
 * Speed model: {@code frameTimes} keeps the source's ORIGINAL per-frame timing forever, so "match
 * original" can always restore it. {@code uniformTicks} is the speed knob: 0 = play at the original
 * per-frame timing; > 0 = every frame plays for that many ticks (a flat speed the player chose).
 *
 * Depends on: nothing (pure data).
 * Called by:  SlotData (holds it), SlotDataStore (persists it), ServerPackGenerator (reads it to
 *             build the .mcmeta), AnimCommands / AnimationDecoder (produce + edit it).
 */
package com.customblocks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record AnimData(int frameCount, int uniformTicks, String loopMode,
                       boolean interpolate, int trimStart, int trimEnd,
                       boolean transparency, List<Integer> frameTimes) {

    /** Loop modes (frame-index ordering only — never duplicates pixels). */
    public static final String LOOP = "loop";
    public static final String BOUNCE = "bounce";
    public static final String REVERSE = "reverse";

    /** Ticks-per-frame used only when a frame has no known time at all (1 tick = 50ms). */
    public static final int FALLBACK_TICKS = 2;
    /** Smoothing (cross-fade between frames) defaults ON per the locked design. */
    public static final boolean DEFAULT_INTERPOLATE = true;

    /** The "not animated" sentinel — frameCount 0 so {@link #isAnimated()} is false. */
    public static final AnimData NONE =
            new AnimData(0, 0, LOOP, DEFAULT_INTERPOLATE, 0, -1, false, List.of());

    /** Normalize on construction so callers never see null/invalid state. */
    public AnimData {
        loopMode = normalizeLoop(loopMode);
        uniformTicks = Math.max(0, uniformTicks); // 0 = original per-frame timing
        frameTimes = frameTimes == null ? List.of() : List.copyOf(frameTimes);
    }

    /** Map any input to a valid loop mode (unknown → loop). */
    public static String normalizeLoop(String mode) {
        if (mode == null) return LOOP;
        String m = mode.trim().toLowerCase(java.util.Locale.ROOT);
        return (BOUNCE.equals(m) || REVERSE.equals(m)) ? m : LOOP;
    }

    /** True when this slot holds a real animation (more than one frame). */
    public boolean isAnimated() {
        return frameCount > 1;
    }

    /** True when a flat speed override is active (false = playing the source's own per-frame timing). */
    public boolean isUniform() {
        return uniformTicks > 0;
    }

    /** Build the fresh AnimData for a newly decoded clip: full range, original per-frame timing kept. */
    public static AnimData ofDecoded(int frameCount, List<Integer> frameTimes, boolean transparency) {
        return new AnimData(frameCount, 0, LOOP, DEFAULT_INTERPOLATE, 0, frameCount - 1, transparency, frameTimes);
    }

    // ── Immutable edits (the withX pattern) ───────────────────────────────────

    /** Flat speed: every frame plays for {@code ticks} (the source per-frame timing is kept for "original"). */
    public AnimData withUniform(int ticks) {
        return new AnimData(frameCount, Math.max(1, ticks), loopMode, interpolate,
                trimStart, trimEnd, transparency, frameTimes);
    }

    /** Drop the flat-speed override and play at the source's original per-frame timing again. */
    public AnimData withMatchOriginal() {
        return new AnimData(frameCount, 0, loopMode, interpolate,
                trimStart, trimEnd, transparency, frameTimes);
    }

    public AnimData withLoopMode(String mode) {
        return new AnimData(frameCount, uniformTicks, normalizeLoop(mode), interpolate,
                trimStart, trimEnd, transparency, frameTimes);
    }

    public AnimData withInterpolate(boolean smooth) {
        return new AnimData(frameCount, uniformTicks, loopMode, smooth,
                trimStart, trimEnd, transparency, frameTimes);
    }

    public AnimData withTransparency(boolean transparent) {
        return new AnimData(frameCount, uniformTicks, loopMode, interpolate,
                trimStart, trimEnd, transparent, frameTimes);
    }

    /** Trim to the inclusive index range [start, end], clamped to the real frame count. */
    public AnimData withTrim(int start, int end) {
        int s = clamp(start, 0, Math.max(0, frameCount - 1));
        int e = clamp(end, s, Math.max(0, frameCount - 1));
        return new AnimData(frameCount, uniformTicks, loopMode, interpolate,
                s, e, transparency, frameTimes);
    }

    // ── Playback (the single owner of Loop / Bounce / Reverse ordering) ───────

    /** Ticks this frame index displays for: the flat override, else its source time, else the fallback. */
    public int timeFor(int index) {
        if (uniformTicks > 0) return uniformTicks;
        if (frameTimes != null && index >= 0 && index < frameTimes.size()) {
            return Math.max(1, frameTimes.get(index));
        }
        return FALLBACK_TICKS;
    }

    /** The top-level {@code frametime} default written into the mcmeta (per-frame entries override it). */
    public int baseFrametime() {
        return uniformTicks > 0 ? uniformTicks : FALLBACK_TICKS;
    }

    /** A human "frames per second" for display: exact when uniform, average of the source times otherwise. */
    public double effectiveFps() {
        if (uniformTicks > 0) return 20.0 / uniformTicks;
        long sum = 0;
        int n = 0;
        if (frameTimes != null) for (int t : frameTimes) { sum += Math.max(1, t); n++; }
        if (n == 0 || sum == 0) return 20.0 / FALLBACK_TICKS;
        return 20.0 * n / sum;
    }

    /**
     * The ordered playback as {index, time} pairs, honoring the trim range and loop mode. Loop =
     * forward; Reverse = backward; Bounce = forward then back through the interior (no duplicated
     * endpoints, no duplicated pixels — Minecraft just re-references the same strip frames).
     */
    public List<int[]> playback() {
        int n = Math.max(1, frameCount);
        int s = clamp(trimStart, 0, n - 1);
        int e = trimEnd < 0 ? n - 1 : clamp(trimEnd, s, n - 1);

        List<Integer> order = new ArrayList<>();
        for (int i = s; i <= e; i++) order.add(i);
        if (REVERSE.equals(loopMode)) {
            Collections.reverse(order);
        } else if (BOUNCE.equals(loopMode)) {
            for (int i = e - 1; i > s; i--) order.add(i); // ping-pong back through the interior
        }

        List<int[]> out = new ArrayList<>(order.size());
        for (int idx : order) out.add(new int[]{idx, timeFor(idx)});
        return out;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
