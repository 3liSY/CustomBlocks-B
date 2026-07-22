/**
 * DirectionalPlacement.java — Group 08 §J (BlockEntity redesign).
 *
 * The per-placement facing/half plumbing for a directional slot shape (stairs today), kept out of
 * {@link SlotBlock} so that class stays under the no-monolith size limit and reads as one job each.
 *
 * Placement runs synchronously on one thread (getPlacementState → block set → createBlockEntity →
 * onPlaced), but only getPlacementState has the click context and only onPlaced has the BlockEntity.
 * {@link #capture} stashes the computed orientation in a thread-local keyed by the target position;
 * {@link #stamp} consumes it (rejecting any speculative/canceled call whose position doesn't match) and
 * writes it to the BlockEntity — server-side authoritatively, and on a predicting client for instant feedback.
 * {@link #at} resolves the orientation back for collision/outline (and, on the client, the model wrapper).
 *
 * Depends on: BlockShapes.isDirectional (the gate), AnimSlotBlockEntity (storage), SlotOrientation (result).
 * Called by:  SlotBlock.getPlacementState / onPlaced / getOutlineShape / getCollisionShape.
 */
package com.customblocks.block;

import net.minecraft.block.enums.BlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class DirectionalPlacement {

    private DirectionalPlacement() {} // static-only

    private record Pending(BlockPos pos, Direction facing, BlockHalf half) {}
    private static final ThreadLocal<Pending> PENDING = new ThreadLocal<>();

    /** getPlacementState hook — capture facing + clicked half (vanilla stair rules) for a directional shape. */
    public static void capture(ItemPlacementContext ctx, String shape) {
        if (!BlockShapes.isDirectional(shape)) return;
        Direction side = ctx.getSide();
        double relY = ctx.getHitPos().getY() - ctx.getBlockPos().getY();
        BlockHalf half = (side != Direction.DOWN && (side == Direction.UP || relY <= 0.5))
                ? BlockHalf.BOTTOM : BlockHalf.TOP;
        PENDING.set(new Pending(ctx.getBlockPos(), ctx.getHorizontalPlayerFacing(), half));
    }

    /** onPlaced hook — stamp the captured orientation on the slot BlockEntity (server + predicting client). */
    public static void stamp(World world, BlockPos pos, @Nullable LivingEntity placer, String shape) {
        if (!BlockShapes.isDirectional(shape)) return;
        Pending p = PENDING.get();
        PENDING.remove();
        boolean match = p != null && p.pos().equals(pos);
        Direction facing = match ? p.facing() : (placer != null ? placer.getHorizontalFacing() : Direction.NORTH);
        BlockHalf half = match ? p.half() : BlockHalf.BOTTOM;
        if (world.getBlockEntity(pos) instanceof AnimSlotBlockEntity be) be.applyPlacement(facing, half);
        // G08 §J corners — now that facing/half are stamped, compute the initial connection shape from
        // neighbours (both sides: server-authoritative + predicting client), and re-settle the neighbours,
        // whose earlier block-update fired before this BE was stamped.
        StairConnection.refreshWithNeighbors(world, pos);
    }

    /** The orientation stamped on the slot BE at {@code pos}, or null (no BE / not a slot / pre-§J placement). */
    @Nullable
    public static SlotOrientation at(BlockView world, BlockPos pos) {
        if (world != null && world.getBlockEntity(pos) instanceof AnimSlotBlockEntity be && be.placeFacing() != null) {
            return new SlotOrientation(be.placeFacing(), be.placeHalf(), be.placeShape());
        }
        return null;
    }
}
