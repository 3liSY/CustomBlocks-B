/**
 * RoundConfig.java — Group 31 (BuzzerGame) Phase 1 items 5-6.
 *
 * The tunable rules for one host session's rounds, held inside its {@link BuzzerSession}.
 * Kept in its own small class (well under the 300-line *Config cap) so BuzzerSession stays focused on the
 * state machine. Defaults match the design spec: Precision Stop format, 5.00s target, a 3-2-1 countdown
 * on, and DISQUALIFY on a Reaction Race false start.
 *
 * Depends on: FalseStartRule
 * Called by:  BuzzerSession (owns + reads), BuzzerGameCommands (config verbs)
 */
package com.customblocks.buzzergame;

import net.minecraft.nbt.NbtCompound;

import java.util.Locale;

public final class RoundConfig {

    /** false = Precision Stop (count up to a target), true = Reaction Race (3-2-1-GO, fastest wins). */
    public boolean reactionRace = false;
    /** Precision Stop target, in ticks (20 ticks = 1.00s). Default 100 = 5.00s. */
    public int targetTicks = 100;
    /** Whether a 3-2-1 countdown plays before the round (the GO gate for Reaction Race). */
    public boolean countdownEnabled = true;
    /** Countdown length in seconds when enabled. */
    public int countdownSecs = 3;
    /** What a Reaction Race pre-GO buzz does. */
    public FalseStartRule falseStart = FalseStartRule.DISQUALIFY;

    /** One-line human summary for the panel readout. */
    public String summary() {
        String fmt = String.format(Locale.ROOT, "%.2fs", targetTicks / 20.0);
        return (reactionRace ? "Reaction Race" : "Precision Stop")
                + " | target " + fmt
                + " | countdown " + (countdownEnabled ? countdownSecs + "s" : "off")
                + " | false-start " + falseStart.label();
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.putBoolean("reactionRace", reactionRace);
        nbt.putInt("targetTicks", targetTicks);
        nbt.putBoolean("countdownEnabled", countdownEnabled);
        nbt.putInt("countdownSecs", countdownSecs);
        nbt.putString("falseStart", falseStart.name());
    }

    public void readNbt(NbtCompound nbt) {
        reactionRace = nbt.getBoolean("reactionRace");
        if (nbt.contains("targetTicks")) targetTicks = Math.max(1, nbt.getInt("targetTicks"));
        countdownEnabled = !nbt.contains("countdownEnabled") || nbt.getBoolean("countdownEnabled");
        if (nbt.contains("countdownSecs")) countdownSecs = Math.max(1, nbt.getInt("countdownSecs"));
        FalseStartRule r = FalseStartRule.fromToken(nbt.getString("falseStart"));
        if (r == null) {
            try { r = FalseStartRule.valueOf(nbt.getString("falseStart")); } catch (IllegalArgumentException ignored) {}
        }
        if (r != null) falseStart = r;
    }
}
