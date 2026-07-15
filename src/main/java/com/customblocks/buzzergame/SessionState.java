/**
 * SessionState.java — Group 31 (BuzzerGame) Phase 1 item 2.
 *
 * The lifecycle a {@link PanelSession} walks: IDLE (waiting) → COUNTDOWN (3-2-1) → RUNNING (timer live)
 * → RESULTS (times frozen, winner hidden until reveal) → FINISHED (round over). Phase 1 only proves the
 * transitions exist and persist; the countdown/timer/reveal that drive them for real arrive in Phase 2.
 *
 * Called by: PanelSession (current state), the admin panel readout + commands.
 */
package com.customblocks.buzzergame;

public enum SessionState {
    IDLE("Idle"),
    COUNTDOWN("Counting down"),
    RUNNING("Running"),
    RESULTS("Results (awaiting reveal)"),
    FINISHED("Finished");

    private final String label;

    SessionState(String label) {
        this.label = label;
    }

    /** Human-readable label for the admin panel readout. */
    public String label() {
        return label;
    }
}
