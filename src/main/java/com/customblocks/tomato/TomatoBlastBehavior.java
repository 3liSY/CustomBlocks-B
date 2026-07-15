/**
 * TomatoBlastBehavior.java — Group 32 (Explosive Tomato) Phase B ("Boom").
 *
 * The block rules for the tomato's real vanilla explosion. Ordinary terrain craters and restores; two
 * families NEVER break, so there is no dupe surface and no bleed into other CB systems (owner-locked):
 *   • every CustomBlocks block — SlotBlock, guess showcase, buzzer, deleted markers, … — matched by its
 *     registry namespace, so a NEW CB block is blast-proof for free; and
 *   • every CONTAINER — chest / barrel / shulker / hopper / … — matched by an Inventory BlockEntity, so
 *     nothing spills and there is nothing to snapshot-dupe.
 * Everything else follows the default behaviour (normal blast resistance).
 *
 * Depends on: CustomBlocksMod (MOD_ID), the block/BE registries
 * Called by:  TomatoEntity.detonate (passed to World.createExplosion)
 */
package com.customblocks.tomato;

import com.customblocks.CustomBlocksMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;

public final class TomatoBlastBehavior extends ExplosionBehavior {

    /** Stateless — one shared instance is enough, and it keeps allocation off the detonation path. */
    public static final TomatoBlastBehavior INSTANCE = new TomatoBlastBehavior();

    private TomatoBlastBehavior() {}

    @Override
    public boolean canDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power) {
        if (isBlastProof(world, pos, state)) return false;
        return super.canDestroyBlock(explosion, world, pos, state, power);
    }

    /** True for any CustomBlocks block or any container — the two blast-proof families (owner-locked). */
    public static boolean isBlastProof(BlockView world, BlockPos pos, BlockState state) {
        if (state.isAir()) return false;
        if (Registries.BLOCK.getId(state.getBlock()).getNamespace().equals(CustomBlocksMod.MOD_ID)) return true;
        BlockEntity be = world.getBlockEntity(pos);
        return be instanceof Inventory; // chests / barrels / shulkers / hoppers / dispensers / …
    }
}
