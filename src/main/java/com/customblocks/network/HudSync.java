/**
 * HudSync.java
 *
 * Responsibility: Build and send a HudSyncPayload to a specific player.
 * Centralises the JSON-building logic (using Gson for proper escaping) so command
 * handlers can re-sync the client cache after create/delete/rename/dupe without
 * duplicating the payload construction code.
 *
 * Why not manual string-building: display names can contain backslashes or quotes
 * that would silently corrupt the hand-rolled JSON and leave the cache empty.
 *
 * Depends on: SlotManager, HudSyncPayload, Gson
 * Called by: CustomBlocksMod (on join), CreationCommands (after mutations)
 */
package com.customblocks.network;

import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.network.payloads.HudSyncPayload;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class HudSync {

    private HudSync() {}

    /** Send the current slot index to a player's client (populates ClientSlotCache). */
    public static void sendTo(ServerPlayerEntity player) {
        JsonObject root = new JsonObject();
        for (SlotData d : SlotManager.assignedSlots()) {
            // NUL separator MUST match ClientSlotCache.populate()'s indexOf('\u0000') split.
            root.addProperty(String.valueOf(d.index()), d.customId() + '\u0000' + d.displayName());
        }
        ServerPlayNetworking.send(player, new HudSyncPayload(root.toString()));
    }
}
