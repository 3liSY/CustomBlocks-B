/**
 * WheelBlock.java - Group 34 (Wheel of Fortune) v2 item A (the hidden centre anchor).
 *
 * The block a wheel item places. It is INVISIBLE and has no collision (design lock 2026-07-23: "no visible
 * centre block") — it exists only to own the {@link WheelBlockEntity} and to mark where the wheel is built.
 * Placing it turns the wheel to face the placer and spawns the whole thing; removing it takes every display
 * entity with it.
 *
 * Because the wheel body is display entities, a normal crosshair never touches the anchor: clicks arrive on
 * the INTERACTION surfaces {@link WheelDisplayVisual} spawns, and {@link #onWheelInteract} routes them back
 * here (right-click anywhere on the wheel or the arrow spins it; left-click the centre removes it). The
 * {@link #onUse} path stays wired as the direct fallback for anyone standing inside the anchor cell.
 *
 * Depends on: WheelBlockEntity, WheelBlockRegistry, WheelDisplayVisual, Chat
 * Called by:  WheelBlockRegistry.register(), the game (place / use / tick / break), CustomBlocksMod (entity clicks)
 */
package com.customblocks.wheel;

import com.customblocks.command.Chat;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class WheelBlock extends BlockWithEntity {

    public static final MapCodec<WheelBlock> CODEC = createCodec(WheelBlock::new);

    /** A small centred box so the anchor can still be aimed at directly if the click surfaces are ever gone. */
    private static final VoxelShape OUTLINE = Block.createCuboidShape(6.0, 6.0, 6.0, 10.0, 10.0, 10.0);

    public WheelBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new WheelBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE;
    }

    /** No collision — the anchor sits inside the wheel and nobody should bump into an invisible cube. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        return validateTicker(type, WheelBlockRegistry.BLOCK_ENTITY, WheelBlockEntity::serverTick);
    }

    /** Face the wheel at the placer and build it immediately (no one-tick flicker). Singleton: any wheel
     *  already standing is taken down first, so the server can never end up with two. */
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!(world instanceof ServerWorld serverWorld)) return;
        boolean replaced = WheelBlockRegistry.removeOther(serverWorld, pos);
        if (world.getBlockEntity(pos) instanceof WheelBlockEntity wheel) {
            wheel.faceTowards(placer != null ? placer.getYaw() : 0f);
            wheel.ensureBuilt(serverWorld);
        }
        if (placer instanceof ServerPlayerEntity player) {
            Chat.tool(player, replaced ? "Wheel moved here (one wheel per server)." : "Wheel placed. Right-click it to spin.");
        }
    }

    /** Direct right-click on the anchor cell — the fallback spin trigger. */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (world instanceof ServerWorld serverWorld && player instanceof ServerPlayerEntity serverPlayer) {
            spin(serverWorld, pos, serverPlayer);
        }
        return ActionResult.SUCCESS;
    }

    /**
     * On break: take every display entity down. The BlockEntity must be read BEFORE super, which removes it.
     */
    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld serverWorld
                && world.getBlockEntity(pos) instanceof WheelBlockEntity wheel) {
            wheel.despawnAndClear(serverWorld);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    // ------------------------------------------------------------------ click surfaces (INTERACTION entities)

    /**
     * Route a click on one of the wheel's INTERACTION surfaces (wired to Fabric's Use/AttackEntityCallback in
     * {@link com.customblocks.CustomBlocksMod}). Right-click anywhere = spin; left-click the centre = remove
     * the wheel, which is how it comes down since the anchor block itself is unaimable.
     */
    public static ActionResult onWheelInteract(PlayerEntity player, World world, Entity entity, boolean attack) {
        if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
        if (!(world instanceof ServerWorld serverWorld) || !WheelDisplayVisual.isHit(entity)) return ActionResult.PASS;
        BlockPos anchor = WheelDisplayVisual.hitAnchor(entity);
        if (anchor == null || !(world.getBlockEntity(anchor) instanceof WheelBlockEntity wheel)) return ActionResult.PASS;

        if (!attack) {
            spin(serverWorld, anchor, serverPlayer);
            return ActionResult.SUCCESS;
        }
        if (WheelDisplayVisual.hitPart(entity) != WheelDisplayVisual.Part.CENTER) {
            Chat.tool(serverPlayer, "Hit the centre arrow to take the wheel down.");
            return ActionResult.SUCCESS;
        }
        wheel.despawnAndClear(serverWorld);
        serverWorld.removeBlock(anchor, false);
        if (!serverPlayer.isCreative()) serverPlayer.getInventory().insertStack(new ItemStack(WheelBlockRegistry.ITEM));
        Chat.toolSuccess(serverPlayer, "Wheel removed.");
        return ActionResult.SUCCESS;
    }

    /** Start a spin, or say why not. Shared by the block click and the display-entity click. */
    private static void spin(ServerWorld world, BlockPos anchor, ServerPlayerEntity player) {
        if (!(world.getBlockEntity(anchor) instanceof WheelBlockEntity wheel)) return;
        if (wheel.startSpin(world)) Chat.tool(player, "Spinning the wheel...");
        else Chat.tool(player, "The wheel is already spinning.");
    }
}
