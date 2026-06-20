/**
 * ArabicLabels.java — Group 13 / O6. COMMON (server + client).
 *
 * The live, in-memory form-word labels for the connected Arabic join forms: initial / medial / final
 * (isolated has none). This is what {@link ArabicNaming} reads when it builds a display name or a
 * virtual id, so changing a label re-labels every held / placed / stored block instantly with no
 * resource-pack reload (ADR-006: names are computed at display time, never baked).
 *
 * Source of truth flow:
 *   - Server: {@code CustomBlocksConfig.arabicForm*} (persisted JSON) → pushed in here at load and on
 *     every /cb config change, then broadcast to clients via ArabicLabelsPayload.
 *   - Client: ArabicLabelsPayload receiver sets these on join / change; reset to the shipped defaults
 *     on disconnect so another server's labels never bleed across.
 *
 * Defaults match the shipped config defaults so naming works even before the first sync.
 *
 * Depends on: (none — plain statics so both sides and the give command can read it)
 * Called by:  CustomBlocksConfig.load / ArabicFormCommands (server set), CustomBlocksClient (client
 *             set/reset), ArabicNaming (read).
 */
package com.customblocks.arabic;

public final class ArabicLabels {

    /** Shipped defaults — also the disconnect-reset values. */
    public static final String DEFAULT_INI = "Ini";
    public static final String DEFAULT_MID = "Mid";
    public static final String DEFAULT_FIN = "Fin";

    private static volatile String ini = DEFAULT_INI;
    private static volatile String mid = DEFAULT_MID;
    private static volatile String fin = DEFAULT_FIN;

    private ArabicLabels() {} // static-only

    public static String ini() { return ini; }
    public static String mid() { return mid; }
    public static String fin() { return fin; }

    /** Replace all three labels (a null/blank falls back to that label's default). */
    public static void set(String newIni, String newMid, String newFin) {
        ini = clean(newIni, DEFAULT_INI);
        mid = clean(newMid, DEFAULT_MID);
        fin = clean(newFin, DEFAULT_FIN);
    }

    /** Reset to the shipped defaults (used on client disconnect). */
    public static void resetDefaults() { set(DEFAULT_INI, DEFAULT_MID, DEFAULT_FIN); }

    private static String clean(String v, String fallback) {
        return (v == null || v.trim().isEmpty()) ? fallback : v.trim();
    }
}
