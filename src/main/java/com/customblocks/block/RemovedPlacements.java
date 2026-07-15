/**
 * RemovedPlacements.java
 *
 * Responsibility: an in-memory journal of WHERE the {@link DeletedPlacementSweeper} turned a deleted
 * block's placed copies into {@link DeletedMarkerBlock} markers (G06-14 slice 2; was the shared
 * (Removed) block). A marker is its own block tied to no slot, so the swap erases the slot identity
 * (the anti-corruption point) — which means undo otherwise has no way to turn a marker back into the
 * real block. This journal closes that gap: the sweeper records each swapped position under the deleted
 * slot index, and {@code /cb undo} of a delete restores those exact positions back to their slot block.
 *
 * In-memory only ON PURPOSE: UndoManager history is itself in-memory and dropped on reload/restart, so a
 * DELETE op can only ever be undone within the same session — the journal just has to outlive the op, not
 * the JVM. A small index cap bounds memory; an evicted index's copies simply stay (Removed) (still safe).
 *
 * Depends on: SlotBlock, DeletedMarkerBlock, SlotManager (allBlocks + glowFor)
 * Called by:  DeletedPlacementSweeper (record on swap), HistoryCommands (restore on undo of a delete)
 */
package com.customblocks.block;

import com.customblocks.core.SlotManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RemovedPlacements {

    private static final Logger LOG = LoggerFactory.getLogger("CustomBlocks");

    /** Cap on tracked slot indices; past this the OLDEST is forgotten (its copies stay (Removed) — safe). */
    private static final int MAX_INDICES = 64;

    /** One swapped position: which world + where. */
    private record Spot(RegistryKey<World> world, BlockPos pos) {}

    /** Insertion-ordered so the oldest tracked index is the one evicted when over the cap. */
    private static final Map<Integer, List<Spot>> BY_INDEX = new LinkedHashMap<>();

    private RemovedPlacements() {} // static-only

    /** Record that a placed copy of slot {@code index} was just swapped to (Removed) at {@code pos}. */
    public static synchronized void record(int index, RegistryKey<World> world, BlockPos pos) {
        BY_INDEX.computeIfAbsent(index, k -> new ArrayList<>()).add(new Spot(world, pos.toImmutable()));
        while (BY_INDEX.size() > MAX_INDICES) {
            BY_INDEX.remove(BY_INDEX.keySet().iterator().next()); // evict eldest
        }
    }

    /**
     * Undo of a delete: turn every tracked (Removed) copy of {@code index} back into its slot block,
     * inheriting the slot's configured glow. Only positions that STILL hold a (Removed) block are
     * touched (so a since-replaced spot is left alone); the index's list is consumed either way.
     * Returns how many placements were restored.
     */
    public static synchronized int restore(MinecraftServer server, int index) {
        List<Spot> spots = BY_INDEX.remove(index);
        if (server == null || spots == null || spots.isEmpty()) return 0;
        SlotBlock[] all = SlotManager.allBlocks();
        if (all == null || index < 0 || index >= all.length || all[index] == null) return 0;
        BlockState restored = all[index].getDefaultState().with(SlotBlock.LIGHT, SlotManager.glowFor(index));
        int n = 0;
        for (Spot s : spots) {
            ServerWorld w = server.getWorld(s.world());
            if (w == null) continue;
            // G06-14 slice 2: copies are now Deleted markers (was (Removed)). Only turn a spot back if
            // it still holds a marker, so a since-replaced position is left alone.
            if (w.getBlockState(s.pos()).getBlock() instanceof DeletedMarkerBlock) {
                w.setBlockState(s.pos(), restored, Block.NOTIFY_ALL);
                n++;
            }
        }
        if (n > 0) LOG.info("[CustomBlocks] Undo restored {} Deleted-marker placement(s) back to slot {}.", n, index);
        return n;
    }

    /** Forget a slot's tracked positions (e.g. its undo step fell off the stack). */
    public static synchronized void clear(int index) {
        BY_INDEX.remove(index);
    }
}
