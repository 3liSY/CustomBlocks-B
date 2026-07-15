/**
 * BuzzResult.java — Group 31 (BuzzerGame) Phase 1 items 5-6.
 *
 * One buzzer's frozen outcome for a round: the player's name, their stopped time in ticks, and flags
 * for whether it's a real timed press ({@code recorded}), carried a false-start penalty, or was
 * disqualified. The winner is picked from these at reveal time (closest-to-target for Precision Stop,
 * fastest reaction for Reaction Race) — the raw time is frozen on buzz but hidden until the host reveals.
 *
 * Depends on: (none)
 * Called by:  PanelSession (records on buzz, reads at reveal)
 */
package com.customblocks.buzzergame;

public record BuzzResult(String name, long ticks, boolean recorded, boolean falseStart, boolean dq) {

    /** A real timed press. {@code falseStart} = a penalty was already folded into {@code ticks}. */
    public static BuzzResult timed(String name, long ticks, boolean falseStart) {
        return new BuzzResult(name, ticks, true, falseStart, false);
    }

    /** A disqualified buzzer (Reaction Race false start under the DISQUALIFY rule). */
    public static BuzzResult disqualified(String name) {
        return new BuzzResult(name, 0L, false, false, true);
    }

    /** Resolved = this buzzer is done for the round (either timed or DQ'd). */
    public boolean resolved() {
        return recorded || dq;
    }
}
