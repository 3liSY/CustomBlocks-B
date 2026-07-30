/**
 * ArabicLetterRetirement.java
 *
 * Responsibility: Group 13 / Build B (+ G13-25 CP5). Permanently retire ALL the OLD static
 * Arabic art blocks — originally the 144 letters (36 x 4 colours, Build B), and since CP5 the
 * 80 numbers too (Eastern A0-A9 + Western E0-E9 x 4 colours): the real Arabic slot blocks
 * (ArabicSlotBootstrap) are the only letter/number system now.
 *
 *   - Boot migration (idempotent): delete every existing static-art slot, free its index, delete its
 *     texture, and record the index in RetiredSlots. A no-op once they're gone. The hit-list covers
 *     BOTH id generations: the prefixed arabic_<glyph>_<colour> shape AND the pre-prefix legacy
 *     shapes <glyph>_<colour> / num_<n>_<colour> (2026-07-26). The legacy numbers predate the
 *     arabic_ prefix, so the prefixed-only list never matched them and every boot reported success
 *     while leaving 82 orphans behind.
 *   - Safety: because this permanently deletes blocks, the batch takes a full G09 SAFETY backup
 *     immediately before it runs and ABORTS (changing nothing) if that snapshot fails — the same
 *     rule the other destructive batches follow (RetextureAllCommands, SetAllCommands).
 *   - World cleanup: as chunks load we SCAN the freshly-loaded chunk's sections (a direct,
 *     non-blocking read) and queue any placed copy of a retired letter (a slot_N block whose
 *     index is retired). The actual air-swap is done on the next world tick, NOT inside the
 *     chunk-load event: touching the chunk manager (world.getBlockState / getChunkBlocking)
 *     during a chunk-load callback can park the server thread on a chunk that isn't FULL yet
 *     and deadlock spawn-area prep. By tick time the chunk is loaded + ticking, so the swap is
 *     safe and never blocks. Old placements vanish, and a reused slot can never show a wrong
 *     block (the index is reserved from reuse until last resort).
 *
 * Depends on: ArabicArt, SlotManager, RetiredSlots, SlotBlock, BackupManager, Fabric
 *             ServerChunkEvents + ServerTickEvents
 * Called by:  CustomBlocksMod.onInitialize (once, right after the bundled-art import)
 */
package com.customblocks.arabic;

import com.customblocks.block.SlotBlock;
import com.customblocks.core.BackupManager;
import com.customblocks.core.IncidentRecorder;
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
import java.util.LinkedHashSet;
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

    /** Run once on boot: retire the static art blocks, then arm the placed-copy air cleanup. */
    public static void init() {
        int removed = retireStaticArt();
        if (removed > 0) {
            LOG.info("[CustomBlocks/Arabic] CP5: retired {} static Arabic art block(s); {} slot(s) reclaimed.",
                    removed, removed);
        }
        // Scan loaded chunks for placed copies (cheap, non-blocking) ...
        ServerChunkEvents.CHUNK_LOAD.register(ArabicLetterRetirement::onChunkLoad);
        // ... and air-swap them on the world tick, off the chunk-load critical path (no deadlock).
        ServerTickEvents.END_WORLD_TICK.register(ArabicLetterRetirement::onWorldTick);
    }

    /**
     * Pre-prefix legacy ids that carry no colour suffix, so no id SHAPE can describe them — a bare
     * glyph name is an ordinary word and matching it as a pattern would also delete a player-made
     * block that happens to share the name. Only ids proven orphaned in the owner's live slots.json
     * (2026-07-26) are listed here, and each is still existence-checked before deletion.
     */
    private static final String[] LEGACY_STRAY_IDS = {"sad"};

    /** Delete every existing static art slot (letters AND numbers, G13-25 CP5) in one batch.
     *  Idempotent: a no-op once all gone. Takes a G09 safety snapshot first and aborts if it fails. */
    private static int retireStaticArt() {
        List<String> artIds = collectStaticArtIds();
        if (artIds.isEmpty()) return 0;
        if (!snapshotBeforeRetire(artIds.size())) return 0; // no restore point -> change nothing
        return SlotManager.retireSlots(artIds).size();
    }

    /**
     * Every static-art id that actually exists right now, across both id generations:
     *   - arabic_<idBase>_<colour>   the prefixed shape (the 144 letters retired under this one);
     *   - <idBase>_<colour>          pre-prefix legacy, e.g. a0_black, ha_red;
     *   - <fileBase>_<colour>        pre-prefix legacy Western numbers, e.g. num_0_black;
     *   - {@link #LEGACY_STRAY_IDS}  named colourless strays.
     * The real Arabic slot blocks are arabic_<glyph>_<form-token>[_<colour>] where the form token is
     * iso/ini/mid/fin — never a colour — so no live block can be matched by any shape above.
     */
    private static List<String> collectStaticArtIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (ArabicArt.Glyph g : ArabicArt.ALL) {
            for (String color : ArabicArt.COLORS) {
                addIfPresent(ids, ArabicArt.blockId(g, color));
                addIfPresent(ids, g.idBase() + "_" + color);
                addIfPresent(ids, g.fileBase() + "_" + color);
            }
        }
        for (String stray : LEGACY_STRAY_IDS) addIfPresent(ids, stray);
        return new ArrayList<>(ids);
    }

    private static void addIfPresent(Set<String> out, String id) {
        if (SlotManager.getById(id) != null) out.add(id);
    }

    /**
     * Full G09 backup taken immediately before the retirement batch, so the whole deletion is one
     * undoable step (GROUP_10_COLOR_IMAGE.md 2026-07-24; confirmed to apply here 2026-07-26). Runs on
     * the mod-init thread — the server has not started, so the heavy copy cannot hitch a tick, and it
     * must finish before the batch anyway. Returns false when the snapshot failed, and the caller then
     * leaves every block untouched rather than deleting without a restore point.
     */
    private static boolean snapshotBeforeRetire(int doomed) {
        String name = BackupManager.timestampName("pre-arabic-retire");
        try {
            SlotManager.saveAll(); // snapshot must copy a current slots.json
            int blocks = SlotManager.assignedSlots().size();
            BackupManager.save(name, blocks, BackupManager.Kind.SAFETY, "pre-arabic-retire", null);
            LOG.info("[CustomBlocks/Arabic] Safety backup \"{}\" saved ({} block(s)) before retiring {} static art block(s).",
                    name, blocks, doomed);
            return true;
        } catch (Exception e) {
            IncidentRecorder.record("Arabic art retirement aborted: pre-change safety backup failed", e);
            LOG.error("[CustomBlocks/Arabic] Could not make a safety backup, so NOTHING was retired — your blocks are "
                    + "untouched. Fix the backup problem and restart to retry.", e);
            return false;
        }
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
