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
 * host session, and warns nearby players — same contract as breaking a buzzer.
 *
 * Depends on: TimerDisplayBlockEntity, TimerDisplayVisual, BuzzerGameWand, BuzzerSession, BuzzerSessionManager, RoundBroadcast
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
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.util.shape.VoxelShapes;
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
        // I4/I5 (rev 2026-07-19): the outline now HUGS the visible stand — the same base/leg/screen union used
        // for collision, tracking whole + per-part scale via shapeFor(). Returning empty (the prior pass) left
        // the visible stand with NO highlight box at all, so aiming read as "the hitbox doesn't match" and the
        // break felt slow (no wireframe + no crack overlay on the invisible block). The per-part INTERACTION
        // entities still own wand select/resize/rotate/link routing; this box is only what you see + mine.
        return shapeFor(world, pos, false);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeFor(world, pos, true); // small base column so the stand still sits on the ground
    }

    /**
     * Click/collision box that HUGS the actual rendered stand: a union of the base, leg and screen columns,
     * each tracking whole + per-part scale and the stacking lift via {@link TimerDisplayVisual#partBandsPx}
     * (I4). The old box was one fat hollow cuboid, full-width over the whole height, so the crosshair caught
     * empty space around the thin leg and the highlight never matched what you see — which also made the
     * invisible stand read as a "slow/bugged" break (I5). Collision returns the base column only.
     */
    private static VoxelShape shapeFor(BlockView world, BlockPos pos, boolean collisionOnly) {
        float whole = 1f, baseS = 1f, legS = 1f, screenS = 1f;
        if (world.getBlockEntity(pos) instanceof TimerDisplayBlockEntity be) {
            whole = be.getScale();
            baseS = be.partScale(TimerPart.BASE);
            legS = be.partScale(TimerPart.LEG);
            screenS = be.partScale(TimerPart.SCREEN);
        }
        double eff = TimerDisplayVisual.renderedScale(whole);
        double sb = eff * baseS, sl = eff * legS, ss = eff * screenS;
        double[] bands = TimerDisplayVisual.partBandsPx(whole, baseS, legS, screenS);
        double baseTop = bands[0], legTop = bands[1], screenBot = bands[2], screenTop = bands[3];

        // Each column mirrors its model slice, scaled about centre 8: base X 2–14 / Z 3–13, leg X 6.5–9.5 /
        // Z 7–9.5, screen X 2–14 / Z ~7.18–9.32 (see the timer_display_* models).
        VoxelShape base = box(8 - 6 * sb, 0, 8 - 5 * sb, 8 + 6 * sb, baseTop, 8 + 5 * sb);
        if (collisionOnly) return base;
        VoxelShape leg = box(8 - 1.5 * sl, baseTop, 8 - 1.0 * sl, 8 + 1.5 * sl, legTop, 8 + 1.5 * sl);
        VoxelShape screen = box(8 - 6 * ss, screenBot, 8 - 0.82 * ss, 8 + 6 * ss, screenTop, 8 + 1.32 * ss);
        return VoxelShapes.union(base, leg, screen);
    }

    /** A cuboid clamped to the block cell (createCuboidShape needs 0–16 px, min ≤ max). */
    private static VoxelShape box(double x0, double y0, double z0, double x1, double y1, double z1) {
        return Block.createCuboidShape(
                clampCell(Math.min(x0, x1)), clampCell(Math.min(y0, y1)), clampCell(Math.min(z0, z1)),
                clampCell(Math.max(x0, x1)), clampCell(Math.max(y0, y1)), clampCell(Math.max(z0, z1)));
    }

    private static double clampCell(double v) { return Math.max(0.0, Math.min(16.0, v)); }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TimerDisplayBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        return validateTicker(type, BuzzerGameRegistry.TIMER_DISPLAY_ENTITY, TimerDisplayBlockEntity::serverTick);
    }

    /**
     * Spawn the stand's display entities the moment the block is placed (G31 §C) instead of on the first
     * block-entity tick, so there is no one-tick flicker/gap between the block appearing and its visual.
     */
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (world instanceof ServerWorld serverWorld
                && world.getBlockEntity(pos) instanceof TimerDisplayBlockEntity display) {
            display.ensureSpawnedOnPlace(serverWorld, state);
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // The block has no outline (I4): right-clicks land on the part INTERACTION entities, routed by the wand
        // through UseEntityCallback. This only runs if something targets the block directly (rare) — a harmless
        // link-status fallback.
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer
                && world.getBlockEntity(pos) instanceof TimerDisplayBlockEntity display) {
            Chat.tool(serverPlayer, display.isLinked()
                    ? "Timer stand — linked."
                    : "Timer stand — not linked. Use the BuzzerGame wand (Link mode).");
        }
        return ActionResult.SUCCESS;
    }

    /**
     * On break: despawn the display entities, then auto-unlink from its host session and warn nearby
     * players. Must read this block's BlockEntity BEFORE super (it's removed on block change).
     */
    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient
                && world.getBlockEntity(pos) instanceof TimerDisplayBlockEntity display
                && world instanceof ServerWorld serverWorld) {
            display.despawnVisual(serverWorld);
            if (display.isLinked()) {
                BuzzerSession session = BuzzerSessionManager.bySession(display.getSessionId());
                if (session != null) {
                    session.unlinkDisplay(display.getDisplayId());
                    RoundBroadcast.warnNear(serverWorld, pos, "A linked timer stand was removed.");
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
