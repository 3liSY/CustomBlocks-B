/**
 * BgStudioSession.java — Group 10 (Background Studio).
 *
 * Responsibility: per-player working selection for the Background Studio — which block is being
 * worked on, the chosen removal mode, the tolerance, and the fill colour (what the removed
 * background becomes — defaults to black for parity). The chest menu (BgStudioMenu) and the
 * /cb tolerance command both read/write this so they stay in lock-step: opening the GUI seeds it
 * from the server defaults; clicking a mode tile or running /cb tolerance updates it; Apply reads it.
 *
 * Depends on: CustomBlocksConfig (default mode/tolerance), BackgroundRemover (mode normalisation),
 *             ColorLibrary (fill colour resolution).
 * Called by:  gui/chest/BgStudioMenu, command/handlers/ImageToolCommands.
 */
package com.customblocks.gui.chest;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.core.BlockToleranceStore;
import com.customblocks.core.ColorLibrary;
import com.customblocks.image.BackgroundRemover;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BgStudioSession {

    private BgStudioSession() {} // static-only

    private static final String DEFAULT_FILL = "#000000";

    /** A player's current Background Studio selection. */
    public static final class State {
        public String id;
        public String mode;
        public int tol;
        /** Hex colour the removed background is painted (default black). */
        public String fillColor;
        State(String id, String mode, int tol, String fillColor) {
            this.id = id; this.mode = mode; this.tol = tol; this.fillColor = fillColor;
        }
    }

    private static final Map<UUID, State> SESSIONS = new ConcurrentHashMap<>();

    /**
     * Get (creating from defaults if needed) the session for {@code player}, retargeted to {@code id}.
     * On a fresh session or when retargeting to a different block, the tolerance is seeded from that
     * block's saved per-block override if it has one, else the global default — so each block reopens
     * at its own remembered strength.
     */
    public static State get(UUID player, String id) {
        State s = SESSIONS.computeIfAbsent(player, k -> new State(id, defaultMode(), tolFor(id), DEFAULT_FILL));
        if (id != null && !id.equals(s.id)) { s.id = id; s.tol = tolFor(id); } // retarget, keep mode/fill
        return s;
    }

    /** A block's saved per-block tolerance override, or the global default if it has none. */
    private static int tolFor(String id) {
        return id == null ? defaultTol() : BlockToleranceStore.effective(id, defaultTol());
    }

    /** The session for {@code player} or null (no defaulting) — used by /cb tolerance before any GUI. */
    public static State peek(UUID player) { return SESSIONS.get(player); }

    public static void setMode(UUID player, String id, String mode) {
        State s = get(player, id);
        s.mode = BackgroundRemover.normalize(mode);
    }

    public static void setTol(UUID player, String id, int tol) {
        State s = get(player, id);
        s.tol = Math.max(0, Math.min(100, tol));
    }

    /** Set the fill colour (hex string from ColorLibrary.resolve, or raw "#RRGGBB"). */
    public static void setFillColor(UUID player, String id, String hex) {
        State s = get(player, id);
        s.fillColor = hex != null ? hex : DEFAULT_FILL;
    }

    /** Parse the State's fillColor hex to an 0xRRGGBB int, falling back to black on bad input. */
    public static int fillColorRgb(State s) {
        try {
            return Integer.parseInt(s.fillColor.replace("#", ""), 16) & 0xFFFFFF;
        } catch (Exception e) {
            return 0x000000;
        }
    }

    public static void clear(UUID player) { SESSIONS.remove(player); }

    private static String defaultMode() {
        String m = BackgroundRemover.normalize(CustomBlocksConfig.backgroundMode);
        return BackgroundRemover.NONE.equals(m) ? BackgroundRemover.EDGES : m; // a studio default of "none" is pointless
    }

    private static int defaultTol() {
        int t = CustomBlocksConfig.backgroundTolerance;
        return t > 0 ? Math.min(100, t) : 30;
    }
}
