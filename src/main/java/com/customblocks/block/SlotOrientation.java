/**
 * SlotOrientation.java — Group 08 §J (BlockEntity redesign).
 *
 * Immutable per-placement orientation of a directional slot block (today: stairs), carried on
 * {@link AnimSlotBlockEntity} and handed to the client model as render data. Being an immutable
 * record of two enums, it is safe to read from the off-thread chunk mesher (the whole reason the
 * Fabric render-data path exists — see FabricBlockView#getBlockEntityRenderData).
 *
 * facing — the horizontal placement facing (NORTH/EAST/SOUTH/WEST).
 * half   — TOP (upside-down) or BOTTOM. Both feed BlockShapes.orient so the drawn mesh and the
 *          collision box rotate identically.
 * shape  — STRAIGHT or a corner (INNER/OUTER × LEFT/RIGHT), recomputed from neighbours by
 *          StairConnection. It selects WHICH baked mesh the client draws; facing/half then rotate it.
 *
 * Depends on: vanilla Direction + BlockHalf only.
 * Called by:  AnimSlotBlockEntity (getRenderData), DirectionalSlotModel (client render).
 */
package com.customblocks.block;

import net.minecraft.block.enums.BlockHalf;
import net.minecraft.util.math.Direction;

public record SlotOrientation(Direction facing, BlockHalf half, StairShape shape) {

    /** North + bottom + straight = the base (un-rotated, base-mesh) orientation → renderer pass-through. */
    public boolean isBase() {
        return (facing == null || facing == Direction.NORTH) && half != BlockHalf.TOP
                && (shape == null || shape == StairShape.STRAIGHT);
    }
}
