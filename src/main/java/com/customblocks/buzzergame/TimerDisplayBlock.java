/**
 * TimerDisplayBlock.java — Group 31 (BuzzerGame) item 1 (physical stand display, static version).
 *
 * The compact timer stand the host places to show the round clock (replaces the old multiblock-wall idea
 * — design lock 2026-07-07). The block itself is INVISIBLE: all of its look is server-side display
 * entities spawned by {@link TimerDisplayBlockEntity} (stand model + two digit panels), which keeps the
 * visual freely scalable/rotatable for the later resize + rotate items. This block is just the anchor,
 * hitbox, facing, and interaction routing.
 *
 * Right-click behaviour: BuzzerGame wand in hand → dispatched by the wand's mode (link this stand, or
 * resize it — see {@link BuzzerGameWand}); empty hand → a one-line link-status hint. Placing it faces the
 * screen toward the player (sign-style). Breaking it despawns its display entities, auto-unlinks from its
 * panel's session, and warns nearby hosts — same contract as breaking a buzzer.
 *
 * Depends on: TimerDisplayBlockEntity, TimerDisplayVisual, BuzzerGameWand, AdminPanelBlockEntity, RoundBroadcast
 * Called by:  BuzzerGameRegistry.register(), the game (placement / right-click / tick / break)
 */
package com.customblocks.buzzergame;

import com.customblocks.command.Chat;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class TimerDisplayBlock extends BlockWithEntity {

    /** Which way the screen faces (set from the player on placement, sign-style). */
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public static final MapCodec<TimerDisplayBlock> CODEC = createCodec(TimerDisplayBlock::new);

    public TimerDisplayBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, net.minecraft.util.math.Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE; // the visual is the display entities, not this block
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeFor(world, pos, 16.0); // highlight box = the whole stand (grows/shrinks with it)
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeFor(world, pos, 3.0); // collision = just the base (don't snag the screen)
    }

    /** A cuboid click/collision box that tracks the stand's rendered size, capped to the block cell. */
    private static VoxelShape shapeFor(BlockView world, BlockPos pos, double baseHeight) {
        float scale = world.getBlockEntity(pos) instanceof TimerDisplayBlockEntity be
                ? TimerDisplayVisual.renderedScale(be.getScale()) : 1.0f;
        double half = Math.min(8.0, 6.0 * scale); // half width/depth about centre 8, capped to the cell
        double height = Math.min(16.0, baseHeight * scale);
        return Block.createCuboidShape(8.0 - half, 0.0, 8.0 - half, 8.0 + half, height, 8.0 + half);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TimerDisplayBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        return validateTicker(type, BuzzerGameRegistry.TIMER_DISPLAY_ENTITY, TimerDisplayBlockEntity::serverTick);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.SUCCESS;
        }
        TimerDisplayBlockEntity display = world.getBlockEntity(pos) instanceof TimerDisplayBlockEntity d ? d : null;

        // BuzzerGame wand in hand → dispatch by the wand's current mode (link / resize / …).
        if (BuzzerGameWand.isWand(serverPlayer.getMainHandStack())) {
            BuzzerGameWand.onStandClicked(serverPlayer, world, display);
            return ActionResult.SUCCESS;
        }
        if (display != null) {
            String msg = display.isLinked()
                    ? "Timer stand — linked to a panel."
                    : "Timer stand — not linked. Use the BuzzerGame wand (Link mode): click the panel, then this stand.";
            Chat.tool(serverPlayer, msg);
        }
        return ActionResult.SUCCESS;
    }

    /**
     * On break: despawn the display entities, then auto-unlink from the panel's session and warn nearby
     * hosts. Must read this block's BlockEntity BEFORE super (it's removed on block change).
     */
    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient
                && world.getBlockEntity(pos) instanceof TimerDisplayBlockEntity display
                && world instanceof ServerWorld serverWorld) {
            display.despawnVisual(serverWorld);
            if (display.isLinked() && display.getPanelPos() != null
                    && world.getBlockEntity(display.getPanelPos()) instanceof AdminPanelBlockEntity panel
                    && display.getSessionId() != null
                    && display.getSessionId().equals(panel.session().sessionId())) {
                panel.session().unlinkScreen(display.getDisplayId());
                panel.markDirty();
                RoundBroadcast.warnNear(serverWorld, display.getPanelPos(), "A linked timer stand was removed.");
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
