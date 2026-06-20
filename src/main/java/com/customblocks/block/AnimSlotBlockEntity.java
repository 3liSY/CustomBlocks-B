/**
 * AnimSlotBlockEntity.java — Group 14 / Phase 1b.
 *
 * The (data-less) BlockEntity attached to every SlotBlock. Its only job is to give the client
 * a hook so the BlockEntityRenderer (AnimSlotBER, a later slice) can draw placed ANIMATED blocks
 * with their own off-atlas texture — crisp, no mipmap muffle. Static blocks render normally via
 * their atlas model; the renderer just skips them.
 *
 * It stores NO state: the slot index is read straight from the SlotBlock at this position, so there
 * is nothing to persist or sync (no writeNbt/readNbt, no update packet). One BlockEntityType backs
 * all 1028 slot blocks (built in AnimSlotRegistry).
 *
 * Depends on: AnimSlotRegistry (BlockEntityType), SlotBlock (slot index)
 * Called by:  SlotBlock.createBlockEntity (placement), AnimSlotBER (render — later slice)
 */
package com.customblocks.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class AnimSlotBlockEntity extends BlockEntity {

    private final int slotIndex;

    public AnimSlotBlockEntity(BlockPos pos, BlockState state) {
        super(AnimSlotRegistry.BLOCK_ENTITY, pos, state);
        this.slotIndex = (state.getBlock() instanceof SlotBlock sb) ? sb.getSlotIndex() : -1;
    }

    /** The slot this block draws from (-1 if somehow attached to a non-slot block). */
    public int slotIndex() { return slotIndex; }
}
