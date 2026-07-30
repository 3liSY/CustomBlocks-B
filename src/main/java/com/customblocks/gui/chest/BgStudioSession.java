/**
 * BgStudioSession.java — Group 10 (Background Studio).
 *
 * Responsibility: per-player working selection for the Background Studio — which block is being
 * worked on, whether removal is on, and the fill colour (what the removed background becomes —
 * defaults to black for parity). Opening the GUI seeds it from the server default; clicking a mode
 * tile updates it; Apply reads it.
 *
 * G10 §H: there is no strength here any more. The per-block tolerance override and the global default
 * are both deleted, so a session carries only the mode and the fill.
 *
 * Depends on: CustomBlocksConfig (default mode), BackgroundRemover (mode normalisation),
 *             ColorLibrary (fill colour resolution).
 * Called by:  gui/chest/BgStudioMenu.
 */
package com.customblocks.gui.chest;

import com.customblocks.CustomBlocksConfig;
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
        /** Hex colour the removed background is painted (default black). */
        public String fillColor;
        State(String id, String mode, String fillColor) {
            this.id = id; this.mode = mode; this.fillColor = fillColor;
        }
    }

    private static final Map<UUID, State> SESSIONS = new ConcurrentHashMap<>();

    /** Get (creating from the server default if needed) the session for {@code player}, retargeted to
     *  {@code id}. Retargeting keeps the chosen mode and fill — there is no per-block state to reload. */
    public static State get(UUID player, String id) {
        State s = SESSIONS.computeIfAbsent(player, k -> new State(id, defaultMode(), DEFAULT_FILL));
        if (id != null && !id.equals(s.id)) s.id = id;
        return s;
    }

    public static void setMode(UUID player, String id, String mode) {
        State s = get(player, id);
        s.mode = BackgroundRemover.requireMode(mode);
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
        String m = BackgroundRemover.requireMode(CustomBlocksConfig.backgroundMode);
        return BackgroundRemover.NONE.equals(m) ? BackgroundRemover.AUTO : m; // a studio default of "off" is pointless
    }
}
