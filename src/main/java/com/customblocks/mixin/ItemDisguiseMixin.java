/**
 * ItemDisguiseMixin.java — Group 30 (Guess Mode) — pack-INDEPENDENT item disguise. CLIENT-SIDE ONLY.
 *
 * The flagged holder must see the "?" mystery cube for a flagged custom block on EVERY item render — hotbar,
 * inventory GUI, first/third-person hand, dropped item, item frame. The earlier approach routed this through a
 * {@code builtin/entity} item model in the pack (SlotItemRenderer), but on a DEDICATED server the pack is
 * authored by the SERVER — so a client-only jar swap never got that model and the disguise silently no-op'd
 * (confirmed by the G30 diagnostic run: SlotItemRenderer never fired for the flagged plain block).
 *
 * This mixin removes that dependency: it hooks the ONE method every item-render context funnels into —
 * {@code ItemRenderer.renderItem(stack, mode, leftHanded, matrices, vcp, light, overlay, model)} — at HEAD.
 * When {@link GuessDisguise#disguiseItem} says the LOCAL player has this block flagged, it applies the block's
 * own display transform for the mode (so the "?" cube sits exactly where the real icon would), draws the
 * mystery cube, and cancels the real draw. It reproduces vanilla's own setup (push → apply model transform →
 * translate(-0.5) → draw → pop) so the disguised icon lands identically to a normal block item, in every mode.
 * A WATCHER (local player not flagged) fails the gate → the real item renders untouched.
 *
 * Pack-independent, so it works with only the CLIENT jar updated and never needs a server pack rebuild.
 *
 * Registered as a client mixin in customblocks.mixins.json.
 */
package com.customblocks.mixin;

import com.customblocks.client.render.GuessDisguise;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ItemRenderer.class)
public abstract class ItemDisguiseMixin {

    @Inject(
        method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
        at = @At("HEAD"), cancellable = true)
    private void customblocks$guessItemDisguise(ItemStack stack, ModelTransformationMode mode, boolean leftHanded,
                                                MatrixStack matrices, VertexConsumerProvider vcp, int light,
                                                int overlay, BakedModel model, CallbackInfo ci) {
        boolean guess = GuessDisguise.disguiseItem(stack);                     // §A — the flagged holder
        // §H — the item a MASKED block dropped, seen by anyone but its runner. Fixed to the bundled "?"
        // (§H never uses §B's look chain) and limited to the ground/frame modes, so a stack that still
        // carries the tag can never disguise a hotbar or inventory slot.
        boolean masked = !guess && GuessDisguise.maskedDrop(stack, mode);
        if (!guess && !masked) return;
        // Reproduce vanilla ItemRenderer.renderItem's own framing so the "?" lands exactly where the real icon
        // would in this mode (GUI rotation/scale, ground, hand, frame), then draw the cube instead of the item.
        matrices.push();
        model.getTransformation().getTransformation(mode).apply(leftHanded, matrices);
        matrices.translate(-0.5, -0.5, -0.5);
        GuessDisguise.drawLook(matrices, vcp, light, overlay, guess
                ? GuessDisguise.lookSlotForStack(stack)
                : com.customblocks.client.ClientGuessState.NO_LOOK);
        matrices.pop();
        ci.cancel();
    }
}
