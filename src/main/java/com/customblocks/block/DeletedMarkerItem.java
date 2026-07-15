/**
 * DeletedMarkerItem.java — G06-14 slice 1.
 *
 * The BlockItem for the deleted_marker block. Markers drop nothing, so this is only reachable via
 * creative / give. Overrides getName so the held item reads "Deleted Marker" in GREEN to match the
 * look-HUD label (owner choice 2026-06-27, recoloured red→green slice 2) instead of the plain lang
 * name. Not added to any creative tab.
 *
 * Depends on: DeletedMarkerBlock
 * Called by:  DeletedMarkerRegistry.register()
 */
package com.customblocks.block;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DeletedMarkerItem extends BlockItem {

    public DeletedMarkerItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public Text getName() {
        return Text.literal("Deleted Marker").formatted(Formatting.GREEN);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.literal("Deleted Marker").formatted(Formatting.GREEN);
    }
}
