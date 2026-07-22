/**
 * DeletedPlacementSweeper.java
 *
 * Responsibility: turn every placed copy of a deleted custom block (a {@link SlotBlock} whose index is
 * in {@link DeletedSlots}) into a "Deleted: <name>" marker ({@link DeletedMarkerBlock}, identity in its
 * BlockEntity) — G06-14 slice 2, replacing the old shared {@link RemovedBlock} "(Removed)" swap. Mirrors
 * the proven {@link com.customblocks.arabic.ArabicLetterRetirement} cleanup, but swaps to a labelled
 * marker instead of AIR, so a deleted block's placement is an obvious, named tombstone rather than a
 * purple/broken block or a vanish. The label comes from {@link MarkerResolver} (remembered at delete).
 *
 * Three fully-budgeted stages run on the world tick so none can hitch the server:
 *   - SCAN: chunk positions queued for scanning (seeded by {@link #onDeleted} around online players for
 *     an instant, no-rejoin refresh) are read a budget at a time; each loaded chunk is scanned for
 *     matching placements, whose positions go to the SWAP stage.
 *   - SWAP: queued positions are turned into Deleted markers a budget at a time (NOTIFY_ALL → clients
 *     update live). Positions whose chunk isn't loaded yet are re-queued for a later tick.
 *   - MARKER (G06-14 slices 4-5): existing Deleted markers are re-resolved — HEALED back into their real
 *     block when a live block of the same customId exists again (Restore, even after a restart), or
 *     TOMBSTONED to a generic "(Deleted)" marker when their block was Emptied ({@link com.customblocks.core.MarkerTombstones}).
 *     Marker positions are gathered by iterating a chunk's BLOCK ENTITIES only ({@link #queueMarkersInChunk})
 *     — sparse and cheap, NOT a full-block scan — so this can run on every chunk load without the old
 *     "forever scanner" tax (reason #2 the old (Removed) system was scrapped).
 * Far / unloaded copies are caught the moment their chunk loads ({@link #onChunkLoad}). All access is
 * on the server thread; the concurrent collections are belt-and-suspenders (mirrors the Arabic code).
 *
 * Depends on: SlotBlock, DeletedMarkerBlock (+BE), MarkerResolver, MarkerTombstones, DeletedSlots,
 *             SlotManager, SlotData, Fabric ServerChunkEvents + ServerTickEvents
 * Called by:  CustomBlocksMod.onInitialize (init), DeletionService (onDeleted),
 *             TrashCommands (resolveMarkersNear on Restore / Empty)
 */
package com.customblocks.block;

import com.customblocks.core.DeletedSlots;
import com.customblocks.core.MarkerResolver;
import com.customblocks.core.MarkerTombstones;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class DeletedPlacementSweeper {

    private static final Logger LOG = LoggerFactory.getLogger("CustomBlocks");

    /** Chunk longs queued for a scan (seeded around players on delete). Deduped per world. */
    private static final Map<RegistryKey<World>, Set<Long>> SCAN = new ConcurrentHashMap<>();
    /** Block positions queued for the slot_N → marker swap. */
    private static final Map<RegistryKey<World>, Queue<BlockPos>> SWAP = new ConcurrentHashMap<>();
    /** Marker positions queued for heal/tombstone re-resolution (G06-14 slices 4-5). */
    private static final Map<RegistryKey<World>, Queue<BlockPos>> MARKER = new ConcurrentHashMap<>();

    private static final int SCAN_BUDGET_PER_TICK = 64;   // chunks scanned per world per tick
    private static final int SWAP_BUDGET_PER_TICK = 256;  // blocks swapped per world per tick
    private static final int MARKER_BUDGET_PER_TICK = 256; // markers re-resolved per world per tick

    private DeletedPlacementSweeper() {}

    /** Arm the chunk-load scan + the tick-time scan/swap drains. Call once from init. */
    public static void init() {
        ServerChunkEvents.CHUNK_LOAD.register(DeletedPlacementSweeper::onChunkLoad);
        ServerTickEvents.END_WORLD_TICK.register(DeletedPlacementSweeper::onWorldTick);
    }

    /**
     * Instant, no-rejoin refresh after a delete: queue every loaded chunk around every online player
     * for a scan, so the tick handler swaps their placed copies of {@code index} to (Removed) live.
     * Far / unloaded copies are handled by {@link #onChunkLoad} when their chunk next loads.
     */
    public static void onDeleted(MinecraftServer server, int index) {
        if (server == null || !DeletedSlots.contains(index)) return;
        int r = Math.max(2, server.getPlayerManager().getViewDistance());
        for (ServerWorld world : server.getWorlds()) {
            Set<Long> set = SCAN.computeIfAbsent(world.getRegistryKey(), k -> ConcurrentHashMap.newKeySet());
            for (ServerPlayerEntity p : world.getPlayers()) {
                ChunkPos c = p.getChunkPos();
                for (int dx = -r; dx <= r; dx++)
                    for (int dz = -r; dz <= r; dz++)
                        set.add(ChunkPos.toLong(c.x + dx, c.z + dz));
            }
        }
    }

    /** Just-loaded chunk → scan it (only while blocks are deleted) for placed copies to mark, and always
     *  gather its Deleted markers (cheap BE-only pass) so far copies heal/tombstone the moment they load. */
    private static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        if (!DeletedSlots.isEmpty()) scanChunk(world, chunk);
        queueMarkersInChunk(world, chunk);
    }

    private static void onWorldTick(ServerWorld world) {
        RegistryKey<World> key = world.getRegistryKey();

        // Stage 1 — scan a budget of queued chunks (skip any not currently loaded).
        Set<Long> scan = SCAN.get(key);
        if (scan != null && !scan.isEmpty()) {
            int budget = SCAN_BUDGET_PER_TICK;
            Iterator<Long> it = scan.iterator();
            while (budget-- > 0 && it.hasNext()) {
                long cpos = it.next();
                it.remove();
                WorldChunk wc = world.getChunkManager().getWorldChunk(ChunkPos.getPackedX(cpos), ChunkPos.getPackedZ(cpos));
                if (wc != null) scanChunk(world, wc);
            }
        }

        // Stage 2 — swap a budget of queued positions to (Removed).
        Queue<BlockPos> swap = SWAP.get(key);
        if (swap != null && !swap.isEmpty()) {
            int budget = SWAP_BUDGET_PER_TICK;
            int swapped = 0;
            List<BlockPos> requeue = new ArrayList<>();
            BlockPos pos;
            while (budget-- > 0 && (pos = swap.poll()) != null) {
                WorldChunk wc = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
                if (wc == null) { requeue.add(pos); continue; } // chunk not ready — try later
                BlockState s = wc.getBlockState(pos);
                if (s.getBlock() instanceof SlotBlock sb && DeletedSlots.contains(sb.getSlotIndex())) {
                    int idx = sb.getSlotIndex();
                    // G06-C SAFEGUARD: a retired index whose slot is LIVE again is stale data — never
                    // tombstone a legitimately placed block. Heal the set here too (belt-and-suspenders
                    // to the boot reconcile) and leave the block untouched.
                    if (SlotManager.blockAt(idx) != null && SlotManager.getBySlot("slot_" + idx) != null) {
                        DeletedSlots.remove(idx);
                        continue;
                    }
                    // G06-14 slice 2: turn the placed copy into a "Deleted: <name>" marker (its own
                    // identity-carrying block), NOT the old shared (Removed) block. The label comes
                    // from MarkerResolver (remembered at delete time); empty → generic "(Deleted)".
                    world.setBlockState(pos, DeletedMarkerRegistry.BLOCK.getDefaultState(), Block.NOTIFY_ALL);
                    if (world.getBlockEntity(pos) instanceof DeletedMarkerBlockEntity be) {
                        be.setData(MarkerResolver.idFor(idx), MarkerResolver.nameFor(idx));
                        be.sync();
                    }
                    // Journal WHERE this copy was marked so /cb undo can turn it back (G06-2 undo completion).
                    RemovedPlacements.record(idx, key, pos);
                    swapped++;
                }
            }
            if (!requeue.isEmpty()) swap.addAll(requeue);
            if (swapped > 0) LOG.info("[CustomBlocks] Turned {} deleted-block placement(s) into Deleted markers.", swapped);
        }

        // Stage 3 — re-resolve a budget of queued markers: heal back into the real block (Restore) or
        // tombstone to a generic "(Deleted)" marker (Empty). Chunk-not-ready positions are re-queued.
        Queue<BlockPos> markers = MARKER.get(key);
        if (markers != null && !markers.isEmpty()) {
            int budget = MARKER_BUDGET_PER_TICK;
            List<BlockPos> requeue = new ArrayList<>();
            BlockPos pos;
            while (budget-- > 0 && (pos = markers.poll()) != null) {
                WorldChunk wc = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
                if (wc == null) { requeue.add(pos); continue; }
                resolveMarker(world, pos);
            }
            if (!requeue.isEmpty()) markers.addAll(requeue);
        }
    }

    /**
     * Re-resolve one Deleted marker (G06-14 slices 4-5):
     *   - a generic marker (no customId) is left alone;
     *   - a marker whose block was Emptied (in {@link MarkerTombstones}) is stripped to a generic,
     *     never-healing "(Deleted)" marker;
     *   - a marker whose block is LIVE again (same customId — Restore or a same-name re-create) is HEALED
     *     back into that block, inheriting the slot's configured glow;
     *   - otherwise (block still deleted / in the trash) it stays a "Deleted: &lt;name&gt;" marker.
     */
    private static void resolveMarker(ServerWorld world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof DeletedMarkerBlockEntity be)) return;
        String id = be.customId();
        if (id == null || id.isEmpty()) return;                 // already generic
        if (MarkerTombstones.contains(id)) {                    // Emptied → make it un-healable
            be.setData("", "");
            be.sync();
            return;
        }
        SlotData live = SlotManager.getById(id);                // Restored / re-created → heal it back
        if (live == null) return;                               // still deleted → leave the marker
        SlotBlock block = SlotManager.blockAt(live.index());
        if (block == null) return;
        world.setBlockState(pos,
                block.getDefaultState().with(SlotBlock.LIGHT, SlotManager.glowFor(live.index())),
                Block.NOTIFY_ALL);
    }

    /** Enqueue every Deleted marker in a loaded chunk for re-resolution (iterates BLOCK ENTITIES only). */
    private static void queueMarkersInChunk(ServerWorld world, WorldChunk chunk) {
        Map<BlockPos, BlockEntity> bes = chunk.getBlockEntities();
        if (bes.isEmpty()) return;
        List<BlockPos> hits = null;
        for (Map.Entry<BlockPos, BlockEntity> e : bes.entrySet()) {
            if (e.getValue() instanceof DeletedMarkerBlockEntity be && !be.customId().isEmpty()) {
                if (hits == null) hits = new ArrayList<>();
                hits.add(e.getKey().toImmutable());
            }
        }
        if (hits != null) {
            MARKER.computeIfAbsent(world.getRegistryKey(), k -> new ConcurrentLinkedQueue<>()).addAll(hits);
        }
    }

    /**
     * Instant, no-rejoin marker re-resolution after a Restore or Empty: gather every Deleted marker in the
     * loaded chunks around online players so the next few ticks heal (Restore) or tombstone (Empty) them.
     * Far / unloaded markers are handled by {@link #onChunkLoad} when their chunk next loads.
     */
    public static void resolveMarkersNear(MinecraftServer server) {
        if (server == null) return;
        int r = Math.max(2, server.getPlayerManager().getViewDistance());
        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity p : world.getPlayers()) {
                ChunkPos c = p.getChunkPos();
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        WorldChunk wc = world.getChunkManager().getWorldChunk(c.x + dx, c.z + dz);
                        if (wc != null) queueMarkersInChunk(world, wc);
                    }
                }
            }
        }
    }

    /**
     * G06-C SAFEGUARD: a placed copy of {@code index} should become a Deleted marker only when the
     * index is retired AND its slot is not a live block. If a live block currently owns the slot, an
     * index sitting in {@link DeletedSlots} is stale data (delete+restore that never un-retired, or a
     * legacy migration) — converting it would tombstone a legitimately placed block. The live-slot
     * check makes that impossible; the boot reconcile + swap-stage heal scrub the stale entry itself.
     */
    private static boolean shouldMark(int index) {
        return DeletedSlots.contains(index) && SlotManager.getBySlot("slot_" + index) == null;
    }

    /** Scan one loaded chunk's sections; queue every placed copy of a deleted index for the swap. */
    private static void scanChunk(ServerWorld world, WorldChunk chunk) {
        ChunkSection[] sections = chunk.getSectionArray();
        int startX = chunk.getPos().getStartX();
        int startZ = chunk.getPos().getStartZ();
        int bottomY = chunk.getBottomY();
        List<BlockPos> hits = new ArrayList<>();
        for (int si = 0; si < sections.length; si++) {
            ChunkSection section = sections[si];
            if (section == null || section.isEmpty()) continue;
            int sectionBottom = bottomY + (si << 4);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState st = section.getBlockState(x, y, z);
                        if (st.getBlock() instanceof SlotBlock sb && shouldMark(sb.getSlotIndex())) {
                            hits.add(new BlockPos(startX + x, sectionBottom + y, startZ + z));
                        }
                    }
                }
            }
        }
        if (!hits.isEmpty()) {
            SWAP.computeIfAbsent(world.getRegistryKey(), k -> new ConcurrentLinkedQueue<>()).addAll(hits);
        }
    }
}
