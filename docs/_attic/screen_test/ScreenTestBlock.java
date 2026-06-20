/**
 * ScreenTestBlock.java — Group 13 / Pass 4 PROOF SPIKE (throwaway, not the real feature).
 *
 * A single isolated test block that proves we can give a WORLD block its own live, in-memory
 * texture that bypasses the resource pack entirely — so changing its picture causes NO pack
 * rebuild, NO resource reload, NO prompt, NO connection-reset risk. This is the make-or-break
 * mechanism for auto-join Arabic words (each joined letter needs its own slice with zero reload).
 *
 * How it proves it: the block carries a BlockEntity holding one int (`variant`). Right-click
 * cycles the variant on the server, which syncs to the client; the client renderer
 * (ScreenTestBlockEntityRenderer) draws a different in-memory bitmap for each variant. The
 * picture changes instantly with no "reloading resources" hitch. If that holds in-game, the
 * real feature swaps the procedural bitmap for the sliced ArabicWordRenderer output.
 *
 * Deliberately self-contained: a brand-new id (customblocks:screen_test), static bundled assets,
 * touches none of the 1028 SlotBlocks or the pack generator (Royal Directive §4 / §8).
 *
 * Depends on: ScreenTestBlockEntity, ScreenTestRegistry
 * Called by:  ScreenTestRegistry.register() (registration), the game (placement / use / render)
 */
package com.customblocks.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ScreenTestBlock extends Block implements BlockEntityProvider {

    public ScreenTestBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ScreenTestBlockEntity(pos, state);
    }

    /** Right-click → cycle the live texture. Server-authoritative; the change syncs to clients. */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof ScreenTestBlockEntity be) {
            be.cycle();
            // Action-bar message so the tester can confirm the swap happened with no reload screen.
            player.sendMessage(Text.literal("§b[Screen Test] §fpicture → variant " + be.variant()
                    + " §7(watch: NO reloading / freeze)"), true);
        }
        return ActionResult.SUCCESS;
    }
}
