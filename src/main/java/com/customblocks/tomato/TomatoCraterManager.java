/**
 * TomatoCraterManager.java — Group 32 (Explosive Tomato) Phase B ("Boom").
 *
 * The anti-grief layer: the tomato craters like TNT, then the crater RESTORES after
 * {@code tomatoRestoreSeconds}. In-memory only and lost on restart, intentionally (owner-locked) — a
 * server stop clears it so craters never leak across worlds.
 *
 * Three correctness rules, all owner-locked:
 *   • FIRST-SNAPSHOT-WINS — a later blast must never record a block an active crater already owns, or the
 *     later restore would write that block back as "original" over the first crater's terrain and leave a
 *     permanent hole. A per-world OWNED set enforces it: a position is claimed by exactly one crater.
 *   • NEVER OVERWRITE A PLAYER'S BUILD — at restore, a position is only refilled if it is still AIR (what
 *     the blast left). If a player has since built there, it is skipped.
 *   • BLOCKS ONLY — entities (item frames, boats, …) are not tracked; the anti-grief promise covers blocks.
 *
 * All access is on the server thread (detonation + the END_SERVER_TICK sweep), so the plain collections are
 * safe.
 *
 * Depends on: CustomBlocksConfig (tomatoRestoreSeconds), Fabric ServerTickEvents + ServerLifecycleEvents
 * Called by:  CustomBlocksMod.onInitialize (init), TomatoEntity.detonate (snapshotBox + recordCrater)
 */
package com.customblocks.tomato;

import com.customblocks.CustomBlocksConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
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

    /** One pending crater: the world it lives in, its captured original states, and the tick it restores at. */
    private record Crater(RegistryKey<World> worldKey, Map<BlockPos, BlockState> blocks, long restoreAtTick) {}

    /** Craters waiting to restore, in record order (server thread only). */
    private static final List<Crater> PENDING = new ArrayList<>();
    /** Positions owned by an ACTIVE crater, per world — the first-snapshot-wins guard. */
    private static final Map<RegistryKey<World>, Set<BlockPos>> OWNED = new HashMap<>();

    /** Wire the restore sweep + the world-stop clear. Call once from onInitialize. */
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(TomatoCraterManager::tick);
        // In-memory only (owner-locked): a stop clears everything so a crater never carries into the next world.
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { PENDING.clear(); OWNED.clear(); });
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
     * owned by an active crater are dropped here (first-snapshot-wins), so overlapping blasts never fight over a
     * block. Restore is scheduled {@code tomatoRestoreSeconds} out; 0 seconds means "never restore" (pure grief).
     */
    public static void recordCrater(ServerWorld world, Map<BlockPos, BlockState> originals) {
        int seconds = CustomBlocksConfig.tomatoRestoreSeconds;
        if (seconds <= 0 || originals.isEmpty()) return;

        RegistryKey<World> key = world.getRegistryKey();
        Set<BlockPos> owned = OWNED.computeIfAbsent(key, k -> new HashSet<>());
        Map<BlockPos, BlockState> mine = new HashMap<>();
        for (Map.Entry<BlockPos, BlockState> e : originals.entrySet()) {
            if (owned.add(e.getKey())) mine.put(e.getKey(), e.getValue()); // claim only positions nobody owns yet
        }
        if (mine.isEmpty()) return;

        long restoreAt = world.getServer().getTicks() + (long) seconds * 20L;
        PENDING.add(new Crater(key, mine, restoreAt));
    }

    /** END_SERVER_TICK: restore every crater whose timer has elapsed, and release the positions it owned. */
    private static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) return;
        long now = server.getTicks();
        Iterator<Crater> it = PENDING.iterator();
        while (it.hasNext()) {
            Crater c = it.next();
            if (now < c.restoreAtTick()) continue;
            ServerWorld world = server.getWorld(c.worldKey());
            if (world != null) restore(world, c.blocks());
            Set<BlockPos> owned = OWNED.get(c.worldKey());
            if (owned != null) owned.removeAll(c.blocks().keySet());
            it.remove();
        }
    }

    /** Refill each crater block with its original state — but only where the blast left AIR (never over a build). */
    private static void restore(ServerWorld world, Map<BlockPos, BlockState> blocks) {
        for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
            if (world.getBlockState(e.getKey()).isAir()) {
                world.setBlockState(e.getKey(), e.getValue()); // NOTIFY_ALL default → clients see it live
            }
        }
    }
}
