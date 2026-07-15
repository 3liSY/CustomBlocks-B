/**
 * WidgetSync.java — GROUP 03. SERVER-SIDE.
 *
 * Builds and pushes the one signal packet the 2 HUD widgets render from. The server owns every fact in
 * it (CLAUDE.md §5.8) — the client draws, and decides nothing.
 *
 * Push, don't poll: every value here changes rarely and at a moment the server already knows about (a
 * macro starts, a block is locked). So callers fire {@link #push} at those moments and the HUD is simply
 * correct, instead of the client asking "anything new?" sixty times a second.
 *
 * The incident ping, the favourites strip and the op-forced widget set were all cut on 2026-07-14
 * (ADR-017), so nothing here reads IncidentRecorder or FavoritesManager any more. IncidentRecorder is
 * untouched and still records — it just never reaches the HUD.
 *
 * G03-TOOLCHIP (2026-07-15): the "tool" property is GONE, and with it the ACTIVE_TOOL widget. It sent
 * OmniToolState.getMode(id) — the player's PERSISTENT tool mode, not "is holding the tool" — so once you
 * set a mode the bottom-left chip rendered forever. The widget was deleted outright (owner: "unnecessary
 * and needs to be wiped"), which IS the fix; there is nothing left to gate.
 *
 * Depends on: LockManager, MacroManager.
 * Called by:  the mutation sites above + player join; received by WidgetSignals on the client.
 */
package com.customblocks.core;

import com.customblocks.network.payloads.WidgetSyncPayload;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class WidgetSync {

    private static final Gson GSON = new Gson();

    private WidgetSync() {} // static-only

    /** Send the current widget state to one player. */
    public static void push(ServerPlayerEntity player) {
        if (player == null) return;
        ServerPlayNetworking.send(player, new WidgetSyncPayload(build(player)));
    }

    /** Send to everyone — used when a server-wide fact changed (a lock). */
    public static void pushAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) push(p);
    }

    private static String build(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        JsonObject o = new JsonObject();

        o.addProperty("macro", MacroManager.isRecording(id));
        o.addProperty("macroName", "");   // the name isn't exposed while recording; the banner reads fine without it

        JsonArray locked = new JsonArray();
        for (String lid : LockManager.list()) locked.add(lid);
        o.add("locked", locked);

        return GSON.toJson(o);
    }
}
