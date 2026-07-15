/**
 * WidgetSignals.java — GROUP 03. CLIENT-SIDE ONLY.
 *
 * The live state the 2 widgets render. Every value in here is pushed down by the server
 * (WidgetSyncPayload) — the client decides nothing, it only draws.
 *
 * Widgets are DISPLAY-ONLY: nothing in this class ever runs a command. It is a bag of facts.
 *
 * Depends on: Gson.
 * Called by: CustomBlocksClient (fills from WidgetSyncPayload), HudWidgetRenderer (reads).
 */
package com.customblocks.client.hud.widget;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.HashSet;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class WidgetSignals {

    private WidgetSignals() {} // static-only

    // G03-TOOLCHIP (2026-07-15): `toolMode` is deleted along with the ACTIVE_TOOL widget. Its comment used to
    // claim the server sent "" when the player wasn't holding the tool. It never did — WidgetSync sent the
    // player's REMEMBERED Omni-Tool mode, which is set once and never cleared, so the chip rendered forever.

    /** True while /cb macro record is running — the banner pulses for exactly this long. */
    public static volatile boolean macroRecording = false;
    public static volatile String macroName = "";

    /** Ids of every locked block. The padlock lights up when the crosshair's block id is in here. */
    private static volatile Set<String> lockedIds = Set.of();

    /** True if {@code id} is locked. */
    public static boolean isLocked(String id) {
        return id != null && !id.isEmpty() && lockedIds.contains(id);
    }

    /** Replace the whole signal set from the server's JSON. Unknown/absent fields keep their last value. */
    public static void apply(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            if (o.has("macro"))     macroRecording = o.get("macro").getAsBoolean();
            if (o.has("macroName")) macroName      = o.get("macroName").getAsString();

            if (o.has("locked")) {
                Set<String> next = new HashSet<>();
                for (JsonElement e : o.getAsJsonArray("locked")) next.add(e.getAsString());
                lockedIds = next;
            }
        } catch (Exception ignored) {
            // A malformed packet must never take the HUD down — keep the last good state.
        }
    }

    /** Forget everything on disconnect, so one server's state never bleeds into the next. */
    public static void reset() {
        macroRecording = false;
        macroName = "";
        lockedIds = Set.of();
    }
}
