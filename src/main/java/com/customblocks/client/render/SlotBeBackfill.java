/**
 * SlotBeBackfill.java — Group 14 / Phase 1c Step 1. CLIENT-ONLY.
 *
 * Off-atlas placed blocks are painted ONLY by AnimSlotBER, which (like every BlockEntityRenderer)
 * draws a block only if it carries a client BlockEntity. A SlotBlock placed by an OLDER jar — before
 * Phase 1b made SlotBlock a BlockEntityProvider — has no saved BlockEntity, and the client render
 * region looks one up with CreationType.CHECK (never creates), so the BER never fires. With the
 * invisible off-atlas pack model that means nothing draws it → the block goes invisible.
 *
 * This backfills that gap: on every client chunk load, walk the loaded sections and force-create the
 * client BlockEntity for any SlotBlock position lacking one (CreationType.IMMEDIATE), so AnimSlotBER
 * can draw old and new placements alike. Idempotent (a position that already has a BE is returned
 * as-is), client-only (no disk write, no NBT, no sync), and fires before the section's first render
 * build, so the BE is present when the render region is captured. Cheap: a section is fully scanned
 * only when its palette actually contains a SlotBlock.
 *
 * Depends on: SlotBlock (the block to detect), AnimSlotBlockEntity (created via SlotBlock.createBlockEntity)
 * Called by:  CustomBlocksClient.onInitializeClient (ClientChunkEvents.CHUNK_LOAD registration)
 */
package com.customblocks.client.render;

import com.customblocks.block.SlotBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

@Environment(EnvType.CLIENT)
public final class SlotBeBackfill {

    private SlotBeBackfill() {} // static-only

    /** Arm the backfill: run once per client chunk load. */
    public static void register() {
        ClientChunkEvents.CHUNK_LOAD.register(SlotBeBackfill::onChunkLoad);
    }

    private static void onChunkLoad(ClientWorld world, WorldChunk chunk) {
        ChunkSection[] sections = chunk.getSectionArray();
        int startX = chunk.getPos().getStartX();
        int startZ = chunk.getPos().getStartZ();
        int bottomY = chunk.getBottomY();
        for (int si = 0; si < sections.length; si++) {
            ChunkSection sec = sections[si];
            if (sec == null || sec.isEmpty()) continue;
            // Cheap palette gate: skip the full scan unless this section actually holds a SlotBlock.
            if (!sec.getBlockStateContainer().hasAny(st -> st.getBlock() instanceof SlotBlock)) continue;
            int sectionBottom = bottomY + (si << 4);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState st = sec.getBlockState(x, y, z);
                        if (st.getBlock() instanceof SlotBlock) {
                            BlockPos pos = new BlockPos(startX + x, sectionBottom + y, startZ + z);
                            // IMMEDIATE = create+attach the client BE if missing (no-op if already there).
                            chunk.getBlockEntity(pos, WorldChunk.CreationType.IMMEDIATE);
                        }
                    }
                }
            }
        }
    }
}
