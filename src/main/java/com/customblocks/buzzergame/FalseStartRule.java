/**
 * FalseStartRule.java — Group 31 (BuzzerGame) Phase 1 item 6 (Reveal / Reaction Race).
 *
 * What happens when a player buzzes DURING the Reaction Race countdown (before "GO"):
 *   DISQUALIFY — that buzzer is out for the round (default).
 *   PENALTY    — the buzzer stays in, but a fixed time penalty is added to its real reaction.
 *   IGNORE     — the early press is thrown away; the player just waits for GO.
 * Only meaningful for the Reaction Race format; Precision Stop simply ignores pre-GO presses.
 *
 * Depends on: (none)
 * Called by:  PanelSession (onBuzz), RoundConfig, BuzzerGameCommands (falsestart verb)
 */
package com.customblocks.buzzergame;

public enum FalseStartRule {
    DISQUALIFY("Disqualify"),
    PENALTY("Time penalty"),
    IGNORE("Ignore");

    private final String label;

    FalseStartRule(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Parse a command token (dq/disqualify, penalty, ignore) → rule, or null if unknown. */
    public static FalseStartRule fromToken(String token) {
        if (token == null) return null;
        return switch (token.toLowerCase()) {
            case "dq", "disqualify" -> DISQUALIFY;
            case "penalty", "pen" -> PENALTY;
            case "ignore", "none" -> IGNORE;
            default -> null;
        };
    }
}
