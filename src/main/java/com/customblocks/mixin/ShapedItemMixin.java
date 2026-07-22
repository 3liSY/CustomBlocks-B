/**
 * ShapedItemMixin.java — Group 08 §B (shaped item icon, pack-INDEPENDENT). CLIENT-SIDE ONLY.
 *
 * Draws a slot item's icon in the slot's CURRENT shape on EVERY item-render context — hotbar, inventory
 * GUI, first/third-person hand, dropped item, item frame — without depending on the pack routing the item
 * to {@code builtin/entity}. See {@link com.customblocks.client.render.ShapedItemIcon} for the full why:
 * the original shaped icon lived in a Fabric DynamicItemRenderer (SlotItemRenderer) that only fires for a
 * {@code builtin/entity} model, which the SERVER authors on a dedicated server — so a shaped block showed
 * a FULL CUBE in hand/inventory there while the placed block still showed the shape.
 *
 * This is the exact same pack-independence fix Group 30 applied to the guess disguise
 * ({@link ItemDisguiseMixin}): hook the ONE method every item-render context funnels into —
 * {@code ItemRenderer.renderItem(stack, mode, leftHanded, matrices, vcp, light, overlay, model)} — at HEAD,
 * draw the shape ourselves (from client-synced shape data), and cancel the vanilla draw. Because it cancels
 * BEFORE the model renders, the DynamicItemRenderer never runs for a shaped item, so there is no double draw
 * with SlotItemRenderer; non-shaped items (full cube, cross, painted/rotated, animated, guess) are left
 * untouched and render normally.
 *
 * Registered as a client mixin in customblocks.mixins.json.
 */
package com.customblocks.mixin;

import com.customblocks.client.render.ShapedItemIcon;
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
public abstract class ShapedItemMixin {

    @Inject(
        method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
        at = @At("HEAD"), cancellable = true)
    private void customblocks$shapedItemIcon(ItemStack stack, ModelTransformationMode mode, boolean leftHanded,
                                             MatrixStack matrices, VertexConsumerProvider vcp, int light,
                                             int overlay, BakedModel model, CallbackInfo ci) {
        if (ShapedItemIcon.tryRender(stack, mode, leftHanded, matrices, vcp, light, overlay, model)) {
            ci.cancel();
        }
    }
}
