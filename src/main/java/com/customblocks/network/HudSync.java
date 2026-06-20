/**
 * HudSync.java
 *
 * Responsibility: Build and send a HudSyncPayload to a specific player.
 * Centralises the JSON-building logic (using Gson for proper escaping) so command
 * handlers can re-sync the client cache after create/delete/rename/dupe without
 * duplicating the payload construction code.
 *
 * Group 27 §G27.4: each slot is now a structured JSON object (id, name + the sync-brick
 * fields category/glow/hardness/sound/shape/passable) instead of a delimited string. This
 * fixes the bug where a display name containing the separator split wrong and a one-word
 * name dropped the slot entirely (ClientSlotCache previously split on the first space).
 *
 * Depends on: SlotManager, HudSyncPayload, Gson
 * Called by: CustomBlocksMod (on join), CreationCommands (after mutations)
 */
package com.customblocks.network;

import com.customblocks.core.CategoryMetadataStore;
import com.customblocks.core.DefaultCategoryStore;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.network.payloads.HudSyncPayload;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class HudSync {

    private HudSync() {}

    /**
     * Send the current slot index to a player's client (populates ClientSlotCache).
     * {@code pass} = noCollision (walk-through). ClientSlotCache.populate() parses these keys.
     */
    public static void sendTo(ServerPlayerEntity player) {
        JsonObject root = new JsonObject();
        for (SlotData d : SlotManager.assignedSlots()) {
            JsonObject s = new JsonObject();
            s.addProperty("id",    d.customId());
            s.addProperty("name",  d.displayName());
            s.addProperty("cat",   d.category());
            s.addProperty("glow",  d.glow());
            s.addProperty("hard",  d.hardness());
            s.addProperty("sound", d.soundType());
            s.addProperty("shape", d.shape());
            s.addProperty("pass",  d.noCollision());
            root.add(String.valueOf(d.index()), s);
        }
        // Category metadata for the studio's Category tab (Group 27 §G27.6). Underscore-prefixed keys
        // are NOT slots — ClientSlotCache skips them when reading the index.
        JsonObject meta = new JsonObject();
        for (String cat : SlotManager.categories()) {
            String tag = CategoryMetadataStore.getColorTag(cat);
            if (tag != null && !tag.isEmpty()) meta.addProperty(cat, tag);
        }
        root.add("_meta", meta);
        root.addProperty("_default", DefaultCategoryStore.get());
        ServerPlayNetworking.send(player, new HudSyncPayload(root.toString()));
    }
}
