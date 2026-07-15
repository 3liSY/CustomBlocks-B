/**
 * GuessShowcaseBlock.java — Group 30 (Guess Mode) · G30-8b Showcase.
 *
 * A floating end-crystal-style display piece spawned by {@code /cb guess showcase spawn [blockid]}. The block
 * itself is INVISIBLE (BlockWithEntity's default render type) — everything the player sees is drawn by
 * {@link com.customblocks.client.render.GuessShowcaseBER}: a spinning picture core + an orbiting outer layer,
 * bobbing, sized/glowing/particled per the shared {@link com.customblocks.core.GuessShowcaseStore} tuning.
 *
 * It's a pure display: NO collision (players walk through it), a small outline box so it can be aimed at.
 * Removal = an OP shift-right-clicks it (this class) or {@code /cb guess showcase delete}. A non-sneaking use,
 * or a non-op, gets a hint instead of deleting, so a Showcase can't be dismissed by accident.
 *
 * Depends on: GuessShowcaseBlockEntity, GuessShowcaseRegistry.
 * Called by:  GuessShowcaseRegistry.register(), GuessShowcaseCommands (placement), the BER (client render).
 */
package com.customblocks.block;

import com.customblocks.command.Chat;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class GuessShowcaseBlock extends BlockWithEntity {

    public static final MapCodec<GuessShowcaseBlock> CODEC = createCodec(GuessShowcaseBlock::new);

    /** A small centred box just so the display can be aimed at for shift-right-click removal. */
    private static final VoxelShape OUTLINE = Block.createCuboidShape(3.0, 3.0, 3.0, 13.0, 13.0, 13.0);

    public GuessShowcaseBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GuessShowcaseBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE;
    }

    /** No collision — the display floats and players walk through it. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return net.minecraft.util.shape.VoxelShapes.empty();
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity op)) return ActionResult.SUCCESS;

        if (!op.hasPermissionLevel(2)) {
            Chat.toolError(op, "This is an op-only Guess showcase.");
            return ActionResult.SUCCESS;
        }
        if (op.isSneaking()) {
            world.removeBlock(pos, false);
            Chat.toolSuccess(op, "Showcase removed.");
            return ActionResult.SUCCESS;
        }
        Chat.tool(op, "Sneak + right-click to remove this Showcase (or /cb guess showcase delete).");
        return ActionResult.SUCCESS;
    }
}
