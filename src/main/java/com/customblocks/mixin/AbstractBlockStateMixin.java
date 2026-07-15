/**
 * AbstractBlockStateMixin.java — Group 30 (Guess Mode) · G30 §R Total Blindness. CLIENT-SIDE ONLY.
 *
 * Hitbox leak plug: the flagged holder must not be able to identify a disguised block by the shape of its
 * SELECTION outline (a slab / stairs / shaped block would give itself away). When the LOCAL player has this
 * block flagged ({@link GuessDisguise#blinds}), the outline is forced to a plain full 1×1 cube — the same box
 * the "?" disguise cube fills. Outline only: the real COLLISION shape is left untouched (server-authoritative),
 * so there's no client/server movement desync (the deliberate scope call for §R slice 1).
 *
 * A WATCHER (local player not flagged) fails the gate → the real outline shows. Client-only mixin, so it never
 * loads on a dedicated server; on the integrated server it runs in the same JVM and the holder-local gate keeps
 * it correct.
 *
 * Registered as a client mixin in customblocks.mixins.json.
 */
package com.customblocks.mixin;

import com.customblocks.client.render.GuessDisguise;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockStateMixin {

    @Inject(
        method = "getOutlineShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;",
        at = @At("HEAD"), cancellable = true)
    private void customblocks$guessBlindOutline(BlockView world, BlockPos pos, ShapeContext context,
                                                CallbackInfoReturnable<VoxelShape> cir) {
        if (GuessDisguise.blinds((BlockState) (Object) this))
            cir.setReturnValue(VoxelShapes.fullCube());
    }
}
