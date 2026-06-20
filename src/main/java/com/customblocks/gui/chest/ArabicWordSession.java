/**
 * ArabicWordSession.java — per-player state for the Arabic word maker flow (Group 13 Area 2).
 *
 * Carries the pending build (display name, id, text, chosen bg + letter colours) across the
 * anvil → choice → Color Studio steps, since a MenuKey only holds one String arg. Also tracks
 * whether a throwaway "Render preview" block currently exists so it can be cleaned up.
 *
 * Two modes:
 *   • word     — making a real word block (text != null). Create → ArabicMaker.finalizeWord.
 *   • defaults — editing the saved default colours (text == null, defaultsMode = true).
 *                Save → writes CustomBlocksConfig.arabicDefault*.
 *
 * Depends on: nothing (plain holder). Called by: ArabicMaker, ColorStudioMenu, ArabicHubMenu.
 */
package com.customblocks.gui.chest;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArabicWordSession {

    private static final Map<UUID, ArabicWordSession> SESSIONS = new ConcurrentHashMap<>();

    public String name;            // display name (word mode)
    public String id;              // block id (word mode)
    public String text;            // the Arabic/Latin text to render (null in defaults mode)
    public int    bgArgb;          // chosen background colour (ARGB, opaque)
    public int    letterArgb;      // chosen letter colour (ARGB, opaque); outline stays black
    public boolean defaultsMode;   // true = editing saved defaults, not making a block
    public boolean previewActive;  // true = a throwaway preview slot currently exists

    private ArabicWordSession() {}

    public static ArabicWordSession start(UUID player) {
        ArabicWordSession s = new ArabicWordSession();
        SESSIONS.put(player, s);
        return s;
    }

    public static ArabicWordSession get(UUID player) { return SESSIONS.get(player); }

    public static void clear(UUID player) { SESSIONS.remove(player); }
}
