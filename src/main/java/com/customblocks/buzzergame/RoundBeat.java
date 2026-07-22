/**
 * RoundBeat.java — Group 31 (BuzzerGame) Phase 1 items 5-6.
 *
 * A single "thing that just happened this tick" that the round clock hands back to the broadcaster:
 * an optional big on-screen title (e.g. "3", "2", "1", "GO!"), an optional subtitle, a sound cue, and a
 * {@code stateChanged} flag. Keeps {@link BuzzerSession} free of any world/packet code — it just describes
 * the beat; {@link RoundBroadcast} turns it into titles/sounds for nearby players.
 *
 * Depends on: (none)
 * Called by:  BuzzerSession (produces), RoundBroadcast (consumes)
 */
package com.customblocks.buzzergame;

public record RoundBeat(String title, String subtitle, Sound sound, boolean stateChanged) {

    /** Sound cue for a beat; mapped to a real SoundEvent by RoundBroadcast (keeps sounds out of state code). */
    public enum Sound { NONE, TICK, GO, WIN, REJECT }

    public static final RoundBeat NONE = new RoundBeat(null, null, Sound.NONE, false);

    /** A countdown number ticking (3, 2, 1…). */
    public static RoundBeat count(int n) {
        return new RoundBeat(String.valueOf(n), null, Sound.TICK, false);
    }

    /** The GO moment — countdown just ended, the clock starts. */
    public static RoundBeat go() {
        return new RoundBeat("GO!", null, Sound.GO, true);
    }
}
