/**
 * StairConnection.java — Group 08 §J (corner connection).
 *
 * The vanilla stair-corner rule (net.minecraft.block.StairsBlock#getStairShape) ported to the mod's
 * data-only design: a stair's {@link StairShape} is NOT a block-state (the 2026-07-20 OOM revert) but is
 * recomputed from its neighbours and stored on {@link AnimSlotBlockEntity}. Two stairs meeting at an L
 * (same half, perpendicular facings) auto-form an inner/outer corner so the step has no gap.
 *
 * The algorithm is byte-faithful to vanilla:
 *   - FRONT neighbour (the block the tall side faces, pos.offset(facing)) with the SAME half and a
 *     perpendicular facing → OUTER corner, unless a matching stair already sits opposite the corner.
 *   - BACK neighbour (pos.offset(facing.opposite)) under the same rule → INNER corner.
 *   - The connecting neighbour facing {@code facing.rotateYCounterclockwise()} → LEFT, else RIGHT.
 * A neighbour "is a stair" iff it's a {@link SlotBlock} whose current shape is {@code stairs}; its facing
 * (null placement → NORTH) and half come off its BlockEntity. Vanilla reads only the neighbour's FACING +
 * HALF (never its shape), so there is no recursion and the recompute is idempotent.
 *
 * Depends on: SlotBlock.resolveShape + BlockShapes.isDirectional (the "is a stair" gate),
 *             AnimSlotBlockEntity (neighbour facing/half + the shape write).
 * Called by:  DirectionalPlacement.stamp (initial shape on placement, both sides),
 *             SlotBlock.neighborUpdate (recompute when a neighbour changes, server side).
 */
package com.customblocks.block;

import net.minecraft.block.enums.BlockHalf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class StairConnection {

    private StairConnection() {} // static-only

    private static final Direction[] HORIZONTAL = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };

    /** A neighbour stair's orientation (facing never null here — a null placement is read as NORTH). */
    private record Neighbor(Direction facing, BlockHalf half) {}

    /**
     * Refresh this stair AND its four horizontal neighbours. Used on placement: the block is set into the
     * world (firing the neighbours' block updates) BEFORE onPlaced stamps this BE's facing, so a neighbour
     * that recomputed on that early update saw this block as an unstamped (NORTH) stair. Re-refreshing the
     * neighbours here — after the facing is stamped — settles them on the correct corner.
     */
    public static void refreshWithNeighbors(World world, BlockPos pos) {
        refresh(world, pos);
        for (Direction d : HORIZONTAL) refresh(world, pos.offset(d));
    }

    /**
     * Recompute the stair shape at {@code pos} from its neighbours and write it to the BlockEntity.
     * Safe on any block (no-op unless it's a stamped directional stair). Works on both sides: the server
     * write syncs the BE to clients, a client write re-meshes the section (placement prediction).
     */
    public static void refresh(World world, BlockPos pos) {
        if (world == null) return;
        if (!(world.getBlockState(pos).getBlock() instanceof SlotBlock sb)) return;
        if (!BlockShapes.isDirectional(SlotBlock.resolveShape(sb.getSlotIndex(), sb.getSlotKey()))) return;
        if (!(world.getBlockEntity(pos) instanceof AnimSlotBlockEntity be)) return;
        Direction facing = be.placeFacing();
        if (facing == null) return; // old / unstamped placement stays STRAIGHT (base) until replaced
        be.applyStairShape(compute(world, pos, facing, be.placeHalf()));
    }

    /** The vanilla getStairShape decision, in the mod's frame. Package-visible for the outline path. */
    public static StairShape compute(BlockView world, BlockPos pos, Direction facing, BlockHalf half) {
        // FRONT neighbour → OUTER corner.
        Neighbor front = neighborStair(world, pos.offset(facing));
        if (front != null && front.half() == half && front.facing().getAxis() != facing.getAxis()
                && isDifferentOrientation(world, pos, facing, half, front.facing().getOpposite())) {
            StairShape s = front.facing() == facing.rotateYCounterclockwise() ? StairShape.OUTER_LEFT : StairShape.OUTER_RIGHT;
            return topSwap(s, half);
        }
        // BACK neighbour → INNER corner.
        Neighbor back = neighborStair(world, pos.offset(facing.getOpposite()));
        if (back != null && back.half() == half && back.facing().getAxis() != facing.getAxis()
                && isDifferentOrientation(world, pos, facing, half, back.facing())) {
            StairShape s = back.facing() == facing.rotateYCounterclockwise() ? StairShape.INNER_LEFT : StairShape.INNER_RIGHT;
            return topSwap(s, half);
        }
        return StairShape.STRAIGHT;
    }

    /**
     * F1 fix (2026-07-22): the corner above is classified in the base NORTH/bottom frame, but a TOP
     * (upside-down) half is rendered by {@link BlockShapes#orient} as x:180 + y:180 = 180° about Z —
     * which preserves Z (front/back = inner vs outer) and mirrors only X (left↔right). So an asymmetric
     * corner wedge (INNER_LEFT = SW quadrant vs INNER_RIGHT = SE) lands on the wrong side for a top half.
     * Swap LEFT↔RIGHT of the chosen corner when the half is TOP; STRAIGHT is left/right symmetric and
     * never reaches here. Collision reads the same stored StairShape through the same orient, so the
     * hitbox follows the visual for free.
     */
    private static StairShape topSwap(StairShape s, BlockHalf half) {
        if (half != BlockHalf.TOP) return s;
        return switch (s) {
            case INNER_LEFT  -> StairShape.INNER_RIGHT;
            case INNER_RIGHT -> StairShape.INNER_LEFT;
            case OUTER_LEFT  -> StairShape.OUTER_RIGHT;
            case OUTER_RIGHT -> StairShape.OUTER_LEFT;
            case STRAIGHT    -> StairShape.STRAIGHT;
        };
    }

    /** Vanilla guard: true when the block in {@code dir} is NOT a stair matching this one's facing+half
     *  (so a corner is only formed when nothing already fills it). */
    private static boolean isDifferentOrientation(BlockView world, BlockPos pos, Direction facing, BlockHalf half, Direction dir) {
        Neighbor n = neighborStair(world, pos.offset(dir));
        return n == null || n.facing() != facing || n.half() != half;
    }

    /** The neighbour's stair orientation, or null when it isn't a stairs-shaped custom block. */
    @Nullable
    private static Neighbor neighborStair(BlockView world, BlockPos npos) {
        if (!(world.getBlockState(npos).getBlock() instanceof SlotBlock nb)) return null;
        if (!BlockShapes.isDirectional(SlotBlock.resolveShape(nb.getSlotIndex(), nb.getSlotKey()))) return null;
        if (!(world.getBlockEntity(npos) instanceof AnimSlotBlockEntity be)) return null;
        Direction f = be.placeFacing();
        return new Neighbor(f == null ? Direction.NORTH : f, be.placeHalf());
    }
}
