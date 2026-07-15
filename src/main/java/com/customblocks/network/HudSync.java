/**
 * HudSync.java
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ NO-REJOIN PRINCIPLE (project rule, owner 2026-06-27)                          │
 * │                                                                              │
 * │ Anything we build must try its hardest to take effect LIVE — the player      │
 * │ must never have to leave and rejoin (or restart the world) to see a change.  │
 * │ Whenever the server changes a block's identity, texture, shape, or a placed  │
 * │ copy, it must PUSH that change to every affected client in the same action.  │
 * │                                                                              │
 * │ The three live-push rails (always update the matching one):                  │
 * │   • identity / name / HUD ....... HudSync.broadcast (THIS class)             │
 * │   • texture / model / shape ..... ResourcePackServer.updatePack             │
 * │   • a placed block in the world . world.setBlockState(.., NOTIFY_ALL)        │
 * │     (+ BlockEntity.sync() for marker labels etc.)                            │
 * │                                                                              │
 * │ Grep the tag  NO-REJOIN  to find every spot that owes a live push, and any   │
 * │ KNOWN GAP still waiting on one. If you add a new mutation, add its push too. │
 * └─────────────────────────────────────────────────────────────────────────────┘
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

import com.customblocks.core.ArabicMeta;
import com.customblocks.core.BlockNotesManager;
import com.customblocks.core.CategoryMetadataStore;
import com.customblocks.core.DefaultCategoryStore;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.network.payloads.HudSyncPayload;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class HudSync {

    private HudSync() {}

    /**
     * Send the current slot index to a player's client (populates ClientSlotCache).
     * {@code pass} = noCollision (walk-through). ClientSlotCache.populate() parses these keys.
     */
    public static void sendTo(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new HudSyncPayload(buildIndexJson()));
    }

    /**
     * Broadcast the current slot index to EVERY online player (Group 05 / G05-1 resync fix).
     * Build the JSON once, reuse the payload for all sends. Use this after any create/delete/
     * variant edit so every client's ClientSlotCache learns the new identity without a rejoin —
     * tool item paths skipped HudSync entirely and command paths only synced the actor, which is
     * why a freshly made slot showed no HUD until the player rejoined.
     */
    public static void broadcast(MinecraftServer server) {
        if (server == null) return;
        HudSyncPayload payload = new HudSyncPayload(buildIndexJson());
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    /** Build the structured slot-index JSON (shared by {@link #sendTo} and {@link #broadcast}). */
    private static String buildIndexJson() {
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
            // G13-25 CP3b — the Arabic identity tuple ("glyph/form/colour"), so a REMOTE client
            // (stale local SlotManager, G05 lesson) can predict the join flow off the synced cache.
            // Absent for every normal block.
            if (d.isArabic()) {
                ArabicMeta am = d.arabic();
                s.addProperty("ar", am.glyphId() + "/" + am.form() + "/" + am.colorKey());
            }
            // Group 18 (REVAMP v2) — the active lore lines (enabled + non-empty), for on-item hover.
            java.util.List<String> lore = BlockNotesManager.activeLore(d.customId());
            if (!lore.isEmpty()) {
                JsonArray arr = new JsonArray();
                for (String ln : lore) arr.add(ln);
                s.add("lore", arr);
            }
            root.add(String.valueOf(d.index()), s);
        }
        // Category metadata for the studio's Category tab (Group 27 §G27.6). Underscore-prefixed keys
        // are NOT slots — ClientSlotCache skips them when reading the index.
        // §G27 L11 "categories are real on creation": union block-membership with CategoryMetadataStore's
        // known set so an explicitly-created category with 0 blocks still gets listed on the client.
        java.util.Set<String> allCats = new java.util.TreeSet<>(SlotManager.categories());
        allCats.addAll(CategoryMetadataStore.knownCategories());
        JsonObject meta = new JsonObject();
        JsonObject hexes = new JsonObject(); // §G27 Category Hub: per-category custom "#RRGGBB" name tint
        JsonObject descs = new JsonObject(); // §G27 Category Hub: per-category description
        JsonObject sorts = new JsonObject(); // §G27 Category Hub: per-category sort order (non-default only)
        JsonArray cats = new JsonArray();    // every known category key, including 0-block ones
        for (String cat : allCats) {
            cats.add(cat);
            String tag = CategoryMetadataStore.getColorTag(cat);
            if (tag != null && !tag.isEmpty()) meta.addProperty(cat, tag);
            String hex = CategoryMetadataStore.getColorHex(cat);
            if (hex != null && !hex.isEmpty()) hexes.addProperty(cat, hex);
            String d = CategoryMetadataStore.getDescription(cat);
            if (d != null && !d.isEmpty()) descs.addProperty(cat, d);
            String so = CategoryMetadataStore.getSortOrder(cat);
            if (so != null && !so.isEmpty() && !"alpha".equals(so)) sorts.addProperty(cat, so);
        }
        root.add("_meta", meta);
        root.add("_hex", hexes);
        root.add("_desc", descs);
        root.add("_sort", sorts);
        root.add("_categories", cats);
        root.addProperty("_default", DefaultCategoryStore.get());
        return root.toString();
    }
}
