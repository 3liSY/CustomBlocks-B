/**
 * SauceBlock.java — Group 32 (Explosive Tomato) Phase D ("Mess").
 *
 * The flat-ground sauce puddle: a real 8-layer block (snow-style {@link net.minecraft.state.property.Properties#LAYERS}
 * 1–8), NOT one of the 1028 slots and NOT a placeable/craftable item — it is spawned only by a blast via
 * {@link SauceManager}. All the block-local gameplay lives here; the lifecycle (drying, rain-wash, lava burn-off,
 * the 50-per-chunk FIFO cap) is owned by {@link SauceManager} so this class stays stateless per-block:
 *   • Slipperiness (0.995, slipperier than ice) comes free from the block Settings — no code here.
 *   • Piston push moves the whole block (Settings pistonBehavior NORMAL, NOT snow's DESTROY).
 *   • Fall damage: absorbed in proportion to depth — layer/8 of it (owner-locked 2026-07-17); 8 layers = full
 *     negation, a 1-layer splash barely helps.
 *   • Eating: plain empty-hand right-click slurps one layer for soup-tier hunger/saturation (owner-locked
 *     2026-07-17 — sneak is no longer required).
 *
 * Depends on: SauceManager (lifecycle + cap), SauceRegistry (the Settings that carry slipperiness/piston)
 * Called by:  the game (collision / land / use), SauceManager (placement reads LAYERS)
 */
package com.customblocks.tomato;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public final class SauceBlock extends Block {

    /** Depth of the puddle, 1–8, reusing vanilla's snow LAYERS property so the blockstate reads {@code layers=N}. */
    public static final IntProperty LAYERS = Properties.LAYERS;
    public static final int MAX_LAYERS = 8;

    /** One collision/outline box per layer count: height = layers × 2px, so 8 layers is a full cube. */
    private static final VoxelShape[] SHAPE_BY_LAYERS = new VoxelShape[MAX_LAYERS + 1];
    static {
        SHAPE_BY_LAYERS[0] = VoxelShapes.empty();
        for (int i = 1; i <= MAX_LAYERS; i++) SHAPE_BY_LAYERS[i] = Block.createCuboidShape(0, 0, 0, 16, i * 2, 16);
    }

    public SauceBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(LAYERS, 1));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE_BY_LAYERS[state.get(LAYERS)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE_BY_LAYERS[state.get(LAYERS)];
    }

    /**
     * Fall damage: absorbed in proportion to puddle depth (owner-locked 2026-07-17, was a hard ≥5-layer cutoff).
     * The damage multiplier is {@code 1 − layers/8}, so 1 layer lets ~88% through (≈12% cushioned), and 8 layers
     * = a full cube = 0× = total negation. Declared public (widening the parent) so it holds regardless of the
     * 1.20.5+ access refactor.
     */
    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        float multiplier = 1.0f - state.get(LAYERS) / (float) MAX_LAYERS;
        entity.handleFallDamage(fallDistance, multiplier, world.getDamageSources().fall());
    }

    /**
     * Eat a puddle (owner-locked 2026-07-17): plain empty-hand right-click (this {@code onUse} overload is the
     * empty-hand path — a held item routes to {@code onUseWithItem} instead). Sneak is NOT required, there is no
     * cooldown, and it works even at full hunger. Every click removes exactly ONE layer; one slurp fills the
     * entire hunger bar AND heals the player to their normal maximum health — {@code add(20, 1.0f)} saturates the
     * bar and {@code setHealth(getMaxHealth())} tops health off, both clamped so it never overheals.
     */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        player.getHungerManager().add(20, 1.0f);        // fill the whole hunger bar + full saturation (clamped at 20)
        player.setHealth(player.getMaxHealth());        // heal to normal max; setHealth clamps → never overheals
        int layers = state.get(LAYERS);
        if (layers <= 1) world.removeBlock(pos, false);
        else world.setBlockState(pos, state.with(LAYERS, layers - 1)); // exactly one layer per click
        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 0.7f, 1.4f); // quick slurp
        return ActionResult.SUCCESS;
    }
}
