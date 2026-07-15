/**
 * PanelGui.java — Group 31 item 4 (admin panel Screen helpers).
 *
 * The admin-Screen "cycle" buttons and the snapshot string, split out of {@link PanelSession} so that class
 * stays under the §9.3 500-line cap. Each method drives the session purely through its public API (the
 * IDLE-guarded setters + the public {@link RoundConfig} fields), so the state machine itself is untouched.
 *
 * Depends on: PanelSession, RoundConfig, GameMode, FalseStartRule, SessionState
 * Called by:  BuzzerPanelNet (button actions), AdminPanelBlock / BuzzerPanelNet (guiSnapshot)
 */
package com.customblocks.buzzergame;

public final class PanelGui {

    private PanelGui() {} // static-only

    /** Cycle the mode Solo → Duel → Party (IDLE only, via setMode's guard). */
    public static PanelSession.Result cycleMode(PanelSession s) {
        GameMode[] v = GameMode.values();
        return s.setMode(v[(s.mode().ordinal() + 1) % v.length]);
    }

    /** Toggle Precision Stop ↔ Reaction Race (IDLE only). */
    public static PanelSession.Result toggleFormat(PanelSession s) {
        return s.setGameFormat(!s.config().reactionRace);
    }

    /** Cycle the countdown length Off → 1 → 2 → 3 → 5 → 10 → Off (IDLE only). */
    public static PanelSession.Result cycleCountdown(PanelSession s) {
        int[] seq = {0, 1, 2, 3, 5, 10};
        int cur = s.config().countdownEnabled ? s.config().countdownSecs : 0;
        int idx = 0;
        for (int i = 0; i < seq.length; i++) if (seq[i] == cur) { idx = (i + 1) % seq.length; break; }
        return s.setCountdown(seq[idx]);
    }

    /** Cycle the false-start rule DQ → Penalty → Ignore (IDLE only). */
    public static PanelSession.Result cycleFalseStart(PanelSession s) {
        FalseStartRule[] v = FalseStartRule.values();
        return s.setFalseStart(v[(s.config().falseStart.ordinal() + 1) % v.length]);
    }

    /** Cycle the Precision target through the presets 3/5/10/15/30/60s (IDLE only). */
    public static PanelSession.Result cycleTargetPreset(PanelSession s) {
        double[] pres = {3, 5, 10, 15, 30, 60};
        int idx = 0;
        for (int i = 0; i < pres.length; i++) {
            if (Math.round(pres[i] * 20.0) == s.config().targetTicks) { idx = (i + 1) % pres.length; break; }
        }
        return s.setTargetSeconds(pres[idx]);
    }

    /**
     * Compact pipe-delimited snapshot the admin Screen renders. Fields:
     * {@code pos|state|mode|format|target|countdown|falseStart|buzzers|screens|canReveal|idle}.
     */
    public static String guiSnapshot(PanelSession s, long pos) {
        RoundConfig c = s.config();
        return pos
                + "|" + s.state().label()
                + "|" + s.mode().label()
                + "|" + (c.reactionRace ? "Reaction Race" : "Precision Stop")
                + "|" + PanelSession.fmtTicks(c.targetTicks)
                + "|" + (c.countdownEnabled ? c.countdownSecs + "s" : "Off")
                + "|" + c.falseStart.label()
                + "|" + s.buzzerCount()
                + "|" + s.screenCount()
                + "|" + (s.state() == SessionState.RESULTS ? 1 : 0)
                + "|" + (s.state() == SessionState.IDLE ? 1 : 0);
    }
}
