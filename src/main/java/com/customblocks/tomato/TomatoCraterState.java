/**
 * TomatoCraterState.java — Group 32 (Explosive Tomato) Phase B ("Boom"), crater persistence.
 *
 * The per-world saved store of pending crater restores (owner-locked 2026-07-17, root safety): craters and their
 * progress now SURVIVE a server restart instead of living only in memory. It is a vanilla {@link PersistentState}
 * attached to each {@link ServerWorld}'s save, so it is written with the world and reloaded on startup.
 *
 * Each {@link Crater} is a countdown ({@code delayTicks}) before restore begins, then an ORDERED queue of blocks
 * drained front-first by {@link TomatoCraterManager} in small per-tick batches. The queue is ordered supports-first
 * / plants-last so farmland/soil/water return before the crops that sit on them (E11). Storing {@code delayTicks}
 * as a countdown (not an absolute server tick) means the timer keeps its meaning across a restart, where the tick
 * counter resets to zero.
 *
 * Deserialisation is defensive: a single unreadable block entry is skipped rather than crashing world load — a
 * lost restore is a hole, never a crash (root safety wins over completeness).
 *
 * Depends on: PersistentState, NbtHelper, the block registry lookup
 * Called by:  TomatoCraterManager (get / mutate / markDirty)
 */
package com.customblocks.tomato;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class TomatoCraterState extends PersistentState {

    static final String KEY = "customblocks_tomato_craters";

    /** One block to restore: where it goes and the exact original state (crops carry their growth stage). */
    record Entry(BlockPos pos, BlockState state) {}

    /** One pending crater: a countdown before restore starts, then an ordered queue drained front-first. */
    static final class Crater {
        long delayTicks;
        final Deque<Entry> queue = new ArrayDeque<>();
    }

    final List<Crater> craters = new ArrayList<>();

    static final Type<TomatoCraterState> TYPE = new Type<>(TomatoCraterState::new, TomatoCraterState::fromNbt, null);

    /** The saved crater store for {@code world}, created empty on first access. */
    static TomatoCraterState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, KEY);
    }

    private static TomatoCraterState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        TomatoCraterState s = new TomatoCraterState();
        RegistryWrapper.Impl<net.minecraft.block.Block> blocks = lookup.getWrapperOrThrow(RegistryKeys.BLOCK);
        NbtList list = nbt.getList("craters", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound cc = list.getCompound(i);
            Crater c = new Crater();
            c.delayTicks = cc.getLong("delay");
            NbtList blockList = cc.getList("blocks", NbtElement.COMPOUND_TYPE);
            for (int j = 0; j < blockList.size(); j++) {
                NbtCompound bc = blockList.getCompound(j);
                try { // isolate a bad entry — a lost restore is a hole, never a world-load crash
                    BlockPos pos = new BlockPos(bc.getInt("x"), bc.getInt("y"), bc.getInt("z"));
                    BlockState st = NbtHelper.toBlockState(blocks, bc.getCompound("state"));
                    if (!st.isAir()) c.queue.addLast(new Entry(pos, st));
                } catch (RuntimeException ignored) {}
            }
            if (!c.queue.isEmpty()) s.craters.add(c);
        }
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtList list = new NbtList();
        for (Crater c : craters) {
            NbtCompound cc = new NbtCompound();
            cc.putLong("delay", c.delayTicks);
            NbtList blockList = new NbtList();
            for (Entry e : c.queue) {
                NbtCompound bc = new NbtCompound();
                bc.putInt("x", e.pos().getX());
                bc.putInt("y", e.pos().getY());
                bc.putInt("z", e.pos().getZ());
                bc.put("state", NbtHelper.fromBlockState(e.state()));
                blockList.add(bc);
            }
            cc.put("blocks", blockList);
            list.add(cc);
        }
        nbt.put("craters", list);
        return nbt;
    }
}
