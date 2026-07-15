/**
 * GameMode.java — Group 31 (BuzzerGame) Phase 1 item 2.
 *
 * The v1 game formats and the minimum linked-buzzer count each needs to start:
 *   SOLO  — 1 buzzer.
 *   DUEL  — exactly 2 (min 2; a 3rd+ is ignored for scoring in DUEL).
 *   PARTY — 2 or more.
 * TEAM is deferred (design lock) — buzzer→team assignment is designed but the mode itself is out of v1,
 * so it is intentionally NOT an enum value yet.
 *
 * Called by: PanelSession (start validation), the mode command + admin panel readout.
 */
package com.customblocks.buzzergame;

public enum GameMode {
    SOLO("Solo", 1),
    DUEL("Duel", 2),
    PARTY("Party", 2);

    private final String label;
    private final int minBuzzers;

    GameMode(String label, int minBuzzers) {
        this.label = label;
        this.minBuzzers = minBuzzers;
    }

    public String label() {
        return label;
    }

    /** Fewest linked buzzers this mode needs before a round may start. */
    public int minBuzzers() {
        return minBuzzers;
    }

    /** Parse a command token ("solo"/"duel"/"party") to a mode, or null if unknown. */
    public static GameMode fromToken(String token) {
        if (token == null) return null;
        return switch (token.trim().toLowerCase()) {
            case "solo" -> SOLO;
            case "duel" -> DUEL;
            case "party" -> PARTY;
            default -> null;
        };
    }
}
