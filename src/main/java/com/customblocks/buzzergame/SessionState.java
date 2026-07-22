/**
 * SessionState.java — Group 31 (BuzzerGame) item D (solo-stopwatch rework, 2026-07-18).
 *
 * The solo-stopwatch lifecycle a {@link BuzzerSession} walks:
 *   IDLE  — no target; the screen shows a single idle "0.00" (الهدف hidden).
 *   ARMED — {@code start <value>} set a target; the screen shows الهدف + a resting النتيجة "0.00".
 *   RUNNING — buzzer press 1: النتيجة is climbing live (the clock only advances in this state).
 *   RESULTS — buzzer press 2: النتيجة is frozen at the stopped value (press 3 re-arms → ARMED).
 * COUNTDOWN and FINISHED are parked (multiplayer / reveal, see the deferred H pass) and unreachable
 * in the solo flow — kept only so the parked ranked-reveal code still compiles.
 *
 * Called by: BuzzerSession (current state), the timer stand digits + commands.
 */
package com.customblocks.buzzergame;

public enum SessionState {
    IDLE("Idle"),
    ARMED("Armed"),
    COUNTDOWN("Counting down"),
    RUNNING("Running"),
    RESULTS("Frozen"),
    FINISHED("Finished");

    private final String label;

    SessionState(String label) {
        this.label = label;
    }

    /** Human-readable label for chat / command readouts. */
    public String label() {
        return label;
    }
}
