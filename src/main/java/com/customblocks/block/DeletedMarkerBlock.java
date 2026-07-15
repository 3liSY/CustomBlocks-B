/**
 * DeletedMarkerBlock.java — G06-14 (Unified Recycle-Bin deletion system) slice 1.
 *
 * The identity-carrying marker a deleted custom block's placed copies become. UNLIKE the old shared
 * {@link RemovedBlock}, this block has a {@link DeletedMarkerBlockEntity} holding the deleted block's
 * customId + displayName, so the tag reads "Deleted: <name>" and a later Restore can heal it back by
 * name (slices 4-5). It is NOT a {@link SlotBlock} → tied to no slot index, can never inherit a skin.
 *
 * Grey + faint ✖ static texture (assets/customblocks/.../deleted_marker.*, never the generated pack).
 * Instant break (strength 0), drops nothing — set in {@link DeletedMarkerRegistry#register()}.
 *
 * Depends on: DeletedMarkerBlockEntity
 * Called by:  DeletedMarkerRegistry.register() (registration), the game (placement / break / BE create)
 */
package com.customblocks.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class DeletedMarkerBlock extends Block implements BlockEntityProvider {

    public DeletedMarkerBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DeletedMarkerBlockEntity(pos, state);
    }
}
