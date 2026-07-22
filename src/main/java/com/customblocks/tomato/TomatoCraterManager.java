/**
 * TomatoCraterManager.java — Group 32 (Explosive Tomato) Phase B ("Boom").
 *
 * The anti-grief layer: the tomato craters like TNT, then the crater RESTORES after {@code tomatoRestoreSeconds}.
 * Owner-locked root-safety rework (2026-07-17): restore is now PERSISTED (survives restart, via
 * {@link TomatoCraterState}), BATCHED (a hard per-tick budget, never a whole crater in one tick), CHUNK-SAFE
 * (never force-loads a chunk — a block in an unloaded chunk simply waits), and ISOLATED (one bad position can't
 * stop the rest or crash the sweep).
 *
 * Correctness rules, all owner-locked:
 *   • FIRST-SNAPSHOT-WINS — a later blast never records a block an active crater already owns, or the later
 *     restore would write a crater back as "original" over the first crater's terrain and leave a permanent hole.
 *     A per-world in-memory OWNED set (rebuilt from the persisted craters on first access) enforces it.
 *   • FARM ORDER (E11) — each crater's queue is ordered supports-first / plants-last, so terrain/tilled soil/water
 *     return before the crops that sit on them. The full BlockState snapshot carries each crop's exact growth stage.
 *   • PLAYER BUILDS SURVIVE — at restore a position is refilled only where it is AIR, sauce, or a BLAST-CAUSED
 *     flowing fluid; sauce and flowing water can no longer block a refill (D1). Anything deliberately placed by a
 *     player — a solid block, a container, a source fluid — is left exactly where it is.
 *   • BLOCKS ONLY — entities (item frames, boats, …) are not tracked; the anti-grief promise covers blocks.
 *
 * All access is on the server thread (detonation + the END_SERVER_TICK sweep), so the plain collections are safe.
 *
 * Depends on: CustomBlocksConfig, TomatoCraterState, SauceManager (clearSauceForRestore), SauceRegistry, Fabric events
 * Called by:  CustomBlocksMod.onInitialize (init), TomatoEntity.detonate (snapshotBox + recordCrater)
 */
package com.customblocks.tomato;

import com.customblocks.CustomBlocksConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TomatoCraterManager {

    private TomatoCraterManager() {} // static-only

    /** Root safety (owner-locked): the whole server restores at most this many blocks per tick, across all craters. */
    private static final int RESTORE_BUDGET_PER_TICK = 128;

    /** Positions owned by a pending crater, per world — the first-snapshot-wins guard, rebuilt from the save on demand. */
    private static final Map<RegistryKey<World>, Set<BlockPos>> OWNED = new HashMap<>();

    /** Wire the restore sweep + the server-stop cache clear. Call once from onInitialize. */
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(TomatoCraterManager::tick);
        // The craters themselves persist with the world; only the derived in-memory OWNED cache is dropped here.
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> OWNED.clear());
    }

    /**
     * Capture every non-air block in the {@code radius} cube around {@code centre} BEFORE the blast craters it.
     * The caller keeps only the subset the explosion actually destroys; the rest of this map is discarded.
     */
    public static Map<BlockPos, BlockState> snapshotBox(ServerWorld world, BlockPos centre, int radius) {
        Map<BlockPos, BlockState> pre = new HashMap<>();
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    m.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    BlockState s = world.getBlockState(m);
                    if (!s.isAir()) pre.put(m.toImmutable(), s);
                }
            }
        }
        return pre;
    }

    /**
     * Record a crater for restore. {@code originals} is {destroyed pos → its ORIGINAL state}. Positions already
     * owned by a pending crater are dropped (first-snapshot-wins). The kept positions are queued supports-first /
     * plants-last (E11) and persisted with the world; restore begins {@code tomatoRestoreSeconds} out.
     */
    public static void recordCrater(ServerWorld world, Map<BlockPos, BlockState> originals) {
        int seconds = CustomBlocksConfig.tomatoRestoreSeconds;
        if (seconds <= 0 || originals.isEmpty()) return;

        Set<BlockPos> owned = ownedFor(world);
        List<Map.Entry<BlockPos, BlockState>> supports = new ArrayList<>();
        List<Map.Entry<BlockPos, BlockState>> plants = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> e : originals.entrySet()) {
            if (!owned.add(e.getKey())) continue;                       // claim only positions nobody owns yet
            (isPlant(e.getValue()) ? plants : supports).add(e);
        }
        if (supports.isEmpty() && plants.isEmpty()) return;

        TomatoCraterState.Crater crater = new TomatoCraterState.Crater();
        crater.delayTicks = (long) seconds * 20L;
        for (Map.Entry<BlockPos, BlockState> e : supports) crater.queue.addLast(new TomatoCraterState.Entry(e.getKey(), e.getValue()));
        for (Map.Entry<BlockPos, BlockState> e : plants) crater.queue.addLast(new TomatoCraterState.Entry(e.getKey(), e.getValue()));

        TomatoCraterState state = TomatoCraterState.get(world);
        state.craters.add(crater);
        state.markDirty();
    }

    /**
     * END_SERVER_TICK: count each crater's timer down, then drain due craters in small batches sharing ONE global
     * per-tick budget so total restore work is hard-bounded no matter how many craters overlap. A block whose
     * chunk is unloaded is left untouched (never force-loaded); one that throws is skipped, never fatal.
     */
    private static void tick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            TomatoCraterState state = TomatoCraterState.get(world);
            if (state.craters.isEmpty()) continue;
            Set<BlockPos> owned = ownedFor(world);
            int budget = RESTORE_BUDGET_PER_TICK;
            boolean dirty = false;

            Iterator<TomatoCraterState.Crater> it = state.craters.iterator();
            while (it.hasNext() && budget > 0) {
                TomatoCraterState.Crater c = it.next();
                if (c.delayTicks > 0) { c.delayTicks--; dirty = true; continue; }
                while (budget > 0 && !c.queue.isEmpty()) {
                    TomatoCraterState.Entry e = c.queue.peekFirst();
                    if (!world.isChunkLoaded(e.pos().getX() >> 4, e.pos().getZ() >> 4)) break; // wait for natural load
                    try { restoreOne(world, e); } catch (RuntimeException ignored) {}           // isolate a bad position
                    c.queue.pollFirst();
                    owned.remove(e.pos());
                    budget--;
                    dirty = true;
                }
                if (c.queue.isEmpty()) { it.remove(); dirty = true; }
            }
            if (dirty) state.markDirty();
        }
    }

    /**
     * Refill one crater block with its original state — but only where the blast left AIR, sauce, or a blast-caused
     * flowing fluid. A puddle sitting on the spot is force-cleared first (untracked from SauceManager's FIFO, with a
     * splash fx); a solid player build, a container, or a source fluid is left exactly where it is (D1).
     */
    private static void restoreOne(ServerWorld world, TomatoCraterState.Entry e) {
        BlockState current = world.getBlockState(e.pos());
        if (current.isOf(SauceRegistry.SAUCE)) {
            SauceManager.clearSauceForRestore(world, e.pos());
        } else if (!current.isAir() && !isClearableFluid(current)) {
            return; // a player built / placed here → never overwrite it
        }
        world.setBlockState(e.pos(), e.state()); // NOTIFY_ALL default → clients see it live
    }

    /** A blast-caused flowing fluid (flowing water/lava) that a refill may overwrite; a source block is kept as player intent. */
    private static boolean isClearableFluid(BlockState state) {
        return state.getBlock() instanceof FluidBlock && !state.getFluidState().isStill();
    }

    /** Crops, saplings, flowers, grass, nether wart — anything that must restore AFTER its supporting block (E11). */
    private static boolean isPlant(BlockState state) {
        return state.getBlock() instanceof PlantBlock;
    }

    /** The first-snapshot-wins owned-position set for a world, rebuilt from the persisted craters on first access. */
    private static Set<BlockPos> ownedFor(ServerWorld world) {
        return OWNED.computeIfAbsent(world.getRegistryKey(), k -> {
            Set<BlockPos> set = new HashSet<>();
            for (TomatoCraterState.Crater c : TomatoCraterState.get(world).craters)
                for (TomatoCraterState.Entry e : c.queue) set.add(e.pos());
            return set;
        });
    }
}
