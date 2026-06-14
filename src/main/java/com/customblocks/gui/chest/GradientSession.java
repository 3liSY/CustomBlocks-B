/**
 * GradientSession.java — Group 10 (Gradient Builder).
 *
 * Responsibility: per-player working state for the Gradient Picker GUI — two endpoint colours
 * (hex or block id) and the step count. Same pattern as BgStudioSession.
 *
 * Depends on: nothing.
 * Called by:  gui/chest/GradientPickerMenu.
 */
package com.customblocks.gui.chest;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GradientSession {

    private GradientSession() {} // static-only

    /** A player's current Gradient Builder selection. */
    public static final class State {
        /** Colour A — hex string ("#RRGGBB") or null if not yet picked. */
        public String colorA;
        /** Colour B — hex string ("#RRGGBB") or null if not yet picked. */
        public String colorB;
        /** If non-null, Colour A came from this block (display only). */
        public String blockA;
        /** If non-null, Colour B came from this block (display only). */
        public String blockB;
        public int steps;
        State() { this.steps = 4; }
    }

    private static final Map<UUID, State> SESSIONS = new ConcurrentHashMap<>();

    public static State get(UUID player) {
        return SESSIONS.computeIfAbsent(player, k -> new State());
    }

    public static void setColorA(UUID player, String hex, String blockId) {
        State s = get(player);
        s.colorA = hex;
        s.blockA = blockId;
    }

    public static void setColorB(UUID player, String hex, String blockId) {
        State s = get(player);
        s.colorB = hex;
        s.blockB = blockId;
    }

    public static void setSteps(UUID player, int steps) {
        get(player).steps = Math.max(1, Math.min(16, steps));
    }

    public static void clear(UUID player) { SESSIONS.remove(player); }
}
