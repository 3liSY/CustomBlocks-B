/**
 * ArabicLetterRetirement.java
 *
 * Responsibility: Group 13 / Build B. Permanently retire the 144 OLD static Arabic letter
 * blocks (36 letters x 4 colours) now that auto-join is the only letter system. Numbers
 * (Eastern A0-A9 + Western E0-E9, 80 blocks) are NEVER matched, so they survive untouched.
 *
 *   - Boot migration (idempotent): delete every existing arabic_<letter>_<colour> slot, free its
 *     index, delete its texture, and record the index in RetiredSlots. A no-op once they're gone.
 *   - World cleanup: as chunks load we SCAN the freshly-loaded chunk's sections (a direct,
 *     non-blocking read) and queue any placed copy of a retired letter (a slot_N block whose
 *     index is retired). The actual air-swap is done on the next world tick, NOT inside the
 *     chunk-load event: touching the chunk manager (world.getBlockState / getChunkBlocking)
 *     during a chunk-load callback can park the server thread on a chunk that isn't FULL yet
 *     and deadlock spawn-area prep. By tick time the chunk is loaded + ticking, so the swap is
 *     safe and never blocks. Old placements vanish, and a reused slot can never show a wrong
 *     block (the index is reserved from reuse until last resort).
 *
 * Depends on: ArabicArt, SlotManager, RetiredSlots, SlotBlock, Fabric ServerChunkEvents +
 *             ServerTickEvents
 * Called by:  CustomBlocksMod.onInitialize (once, right after the bundled-art import)
 */
package com.customblocks.arabic;

import com.customblocks.block.SlotBlock;
import com.customblocks.core.RetiredSlots;
import com.customblocks.core.SlotManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ArabicLetterRetirement {

    private static final Logger LOG = LoggerFactory.getLogger("CustomBlocks/Arabic");

    /** Chunks already swept this session (key: dimension + chunk-long) so we never rescan one. */
    private static final Set<String> SWEPT = Collections.synchronizedSet(new HashSet<>());

    /** Per-world queue of retired-letter positions awaiting an air-swap on the next world tick. */
    private static final Map<RegistryKey<World>, Queue<BlockPos>> PENDING = new ConcurrentHashMap<>();

    /** Max air-swaps performed per world per tick, so a huge backlog can never hitch the server. */
    private static final int CLEAN_BUDGET_PER_TICK = 256;

    private ArabicLetterRetirement() {}

    /** Run once on boot: retire the static letters, then arm the placed-copy air cleanup. */
    public static void init() {
        int removed = retireStaticLetters();
        if (removed > 0) {
            LOG.info("[CustomBlocks/Arabic] Build B: retired {} static letter block(s); {} slot(s) reclaimed.",
                    removed, removed);
        }
        // Scan loaded chunks for placed copies (cheap, non-blocking) ...
        ServerChunkEvents.CHUNK_LOAD.register(ArabicLetterRetirement::onChunkLoad);
        // ... and air-swap them on the world tick, off the chunk-load critical path (no deadlock).
        ServerTickEvents.END_WORLD_TICK.register(ArabicLetterRetirement::onWorldTick);
    }

    /** Delete every existing static letter slot in one batch. Idempotent: a no-op once all gone. */
    private static int retireStaticLetters() {
        List<String> letterIds = new ArrayList<>();
        for (ArabicArt.Glyph g : ArabicArt.ALL) {
            if (g.group() != ArabicArt.Group.LETTER) continue; // numbers stay
            for (String color : ArabicArt.COLORS) {
                String id = ArabicArt.blockId(g, color);
                if (SlotManager.getById(id) != null) letterIds.add(id);
            }
        }
        if (letterIds.isEmpty()) return 0;
        return SlotManager.retireSlots(letterIds).size();
    }

    /** On chunk load, find placed copies of retired letters and queue them for a tick-time air-swap. */
    private static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        if (RetiredSlots.isEmpty()) return;
        String key = world.getRegistryKey().getValue() + ":" + chunk.getPos().toLong();
        if (!SWEPT.add(key)) return; // already swept this chunk this session

        List<BlockPos> hits = new ArrayList<>();
        ChunkSection[] sections = chunk.getSectionArray();
        int startX = chunk.getPos().getStartX();
        int startZ = chunk.getPos().getStartZ();
        int bottomY = chunk.getBottomY();
        for (int si = 0; si < sections.length; si++) {
            ChunkSection section = sections[si];
            if (section == null || section.isEmpty()) continue;
            int sectionBottom = bottomY + (si << 4);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (state.getBlock() instanceof SlotBlock sb && RetiredSlots.contains(sb.getSlotIndex())) {
                            hits.add(new BlockPos(startX + x, sectionBottom + y, startZ + z));
                        }
                    }
                }
            }
        }
        if (hits.isEmpty()) return;
        // Defer the swap to the world tick — doing it here (world.getBlockState/setBlockState)
        // can park the server thread on a not-yet-FULL chunk and deadlock spawn-area prep.
        PENDING.computeIfAbsent(world.getRegistryKey(), k -> new ConcurrentLinkedQueue<>()).addAll(hits);
    }

    /**
     * Each world tick, drain a budgeted slice of queued positions and air-swap any that are still a
     * retired letter. Uses the non-blocking getWorldChunk (null when not loaded) so it never parks the
     * server thread; positions whose chunk isn't ready are re-queued for a later tick.
     */
    private static void onWorldTick(ServerWorld world) {
        Queue<BlockPos> queue = PENDING.get(world.getRegistryKey());
        if (queue == null || queue.isEmpty()) return;

        int budget = CLEAN_BUDGET_PER_TICK;
        int cleaned = 0;
        List<BlockPos> requeue = new ArrayList<>();
        BlockPos pos;
        while (budget-- > 0 && (pos = queue.poll()) != null) {
            WorldChunk wc = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
            if (wc == null) { requeue.add(pos); continue; } // chunk not ready yet — try again later
            BlockState s = wc.getBlockState(pos);
            if (s.getBlock() instanceof SlotBlock sb && RetiredSlots.contains(sb.getSlotIndex())) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                cleaned++;
            }
        }
        if (!requeue.isEmpty()) queue.addAll(requeue);
        if (cleaned > 0) {
            LOG.info("[CustomBlocks/Arabic] Build B: air-cleaned {} placed static letter(s).", cleaned);
        }
    }
}
