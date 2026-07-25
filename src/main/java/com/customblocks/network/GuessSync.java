/**
 * GuessSync.java — Group 30 (Guess Mode). v2 command redesign (2026-07-05).
 *
 * Responsibility: build a {@link GuessModePayload} from {@link GuessModeStore} and push it to clients.
 * Mirrors {@link HudSync}: {@link #sendTo} for a single joining player, {@link #broadcast} to everyone
 * after any /cb guess change (NO-REJOIN — the centered pose + the holder's blinding must update live).
 *
 * The store keeps a player's flagged blocks as ids; the client renders/matches by SLOT INDEX, so each id
 * is resolved to its slot index here (via SlotManager) and only the indices are sent. A deleted/unknown id
 * resolves to nothing and is dropped; a player left with no resolvable blocks and no all-mode is omitted
 * entirely (so they're neither posed nor blinded).
 *
 * v3 Phase 1: the disguise LOOK is customizable. Each flagged slot carries an optional look SLOT (the block
 * whose baked picture is shown instead), and a global default look slot rides at the top. Both are resolved
 * from ids to slots here (deleted look → -1). The client fallback is per-slot look → default → bundled "?"
 * cube (the "?" stays hardcoded client-side, never sent). The blank NAME "???" is still hardcoded client-side.
 *
 * §H Placed Mask Mode rides this same route (Group doc: "masked positions and mode state sync to clients
 * through the existing GuessSync route rather than a second parallel channel"), but on its own compact
 * binary payload and with per-VIEWER filtering: {@link com.customblocks.core.PlacedMaskStore#feedFor} drops
 * a runner's own placements before the packet is built, so the runner keeps seeing the truth without any
 * client-side trust. Placements/breaks send one-position DELTAS ({@link #placedMaskAdd}/{@link #placedMaskDel});
 * only a join or a mode toggle costs a full re-send ({@link #broadcastPlacedMask}).
 *
 * Depends on: GuessModeStore, PlacedMaskStore, SlotManager, GuessModePayload, PlacedMaskPayload, Gson
 * Called by:  CustomBlocksMod (on join), GuessCommands + PlacedMaskCommands (after mutations), SlotBlock
 *             (per placement / break).
 */
package com.customblocks.network;

import com.customblocks.core.GuessModeStore;
import com.customblocks.core.GuessPoseStore;
import com.customblocks.core.GuessShowcaseStore;
import com.customblocks.core.PlacedMaskStore;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.network.payloads.GuessModePayload;
import com.customblocks.network.payloads.PlacedMaskPayload;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;

public final class GuessSync {

    private GuessSync() {} // static-only

    /** Send the current guess-mode set to one player's client. */
    public static void sendTo(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new GuessModePayload(buildJson()));
        sendPlacedMask(player);   // §H — plus the placed-mask picture this viewer must see
    }

    // ── §H Placed Mask Mode ──────────────────────────────────────────────────

    /** Send one player their full placed-mask picture (their own placements already filtered out). */
    public static void sendPlacedMask(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new PlacedMaskPayload(
                PlacedMaskPayload.OP_FULL, PlacedMaskStore.feedFor(player.getUuid())));
    }

    /** Rebuild every online player's placed-mask picture (join, /cb guess placed on/off). */
    public static void broadcastPlacedMask(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) sendPlacedMask(p);
    }

    /** One block just became masked: tell everyone EXCEPT the runner, who must keep seeing the real block. */
    public static void placedMaskAdd(MinecraftServer server, UUID runner, World world, BlockPos pos) {
        delta(server, runner, PlacedMaskPayload.OP_ADD, world, pos);
    }

    /** One masked block is gone: tell everyone (the runner never had it, and simply ignores an unknown pos). */
    public static void placedMaskDel(MinecraftServer server, World world, BlockPos pos) {
        delta(server, null, PlacedMaskPayload.OP_DEL, world, pos);
    }

    private static void delta(MinecraftServer server, UUID skip, byte op, World world, BlockPos pos) {
        if (server == null || world == null || pos == null) return;
        PlacedMaskPayload payload = PlacedMaskPayload.one(op, PlacedMaskStore.dimId(world), pos.asLong());
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (skip != null && skip.equals(p.getUuid())) continue;
            ServerPlayNetworking.send(p, payload);
        }
    }

    /** Broadcast the current guess-mode set to EVERY online player (NO-REJOIN live push). */
    public static void broadcast(MinecraftServer server) {
        if (server == null) return;
        GuessModePayload payload = new GuessModePayload(buildJson());
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    /**
     * Build the client feed: {@code { "def": <defaultLookSlot|-1>, "pose": {rp,ry,rr,lp,ly,lr}, "p": { uuid:
     * { all, looks:{ flaggedSlot: lookSlot|-1 } } } }} for every ACTIVE guess-mode player. Flagged block ids →
     * slot indices; each flagged slot maps to its per-round look slot (or -1 = use default). "def" carries the
     * global default look slot; "pose" carries the shared centered guess pose (six arm angles, radians · G30-4).
     */
    private static String buildJson() {
        JsonObject root = new JsonObject();
        root.addProperty("def", lookSlot(GuessModeStore.defaultLook()));
        root.add("pose", GuessPoseStore.toJson());   // G30-4 slice 1 — the shared centered guess pose (radians)
        root.add("showcase", GuessShowcaseStore.toJson()); // G30-8b — the shared showcase display tuning
        JsonObject players = new JsonObject();
        for (Map.Entry<UUID, GuessModeStore.Entry> e : GuessModeStore.activeEntries().entrySet()) {
            GuessModeStore.Entry v = e.getValue();
            JsonObject looks = new JsonObject();
            for (Map.Entry<String, String> le : v.looks.entrySet()) {
                SlotData d = SlotManager.getById(le.getKey());
                if (d == null) continue;                                     // flagged block deleted → drop it
                looks.addProperty(Integer.toString(d.index()), lookSlot(le.getValue()));
            }
            // Drop a player whose only flags were deleted/unknown ids and who isn't in all-mode.
            if (!v.all && looks.size() == 0) continue;
            JsonObject o = new JsonObject();
            o.addProperty("all", v.all);
            o.add("looks", looks);
            players.add(e.getKey().toString(), o);
        }
        root.add("p", players);
        return root.toString();
    }

    /** Resolve a look block id to its slot index, or -1 when null/deleted (client falls back). */
    private static int lookSlot(String id) {
        if (id == null) return -1;
        SlotData d = SlotManager.getById(id);
        return d == null ? -1 : d.index();
    }
}
