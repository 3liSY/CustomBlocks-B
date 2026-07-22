/**
 * ShieldSauceTintMixin.java — Group 32 (Explosive Tomato) Phase E, E6/E7. CLIENT-ONLY.
 *
 * Red-tints a SAUCED holder's shield (owner pre-approved mixin target: the shield renderer). We ride the
 * item-render entry point {@code ItemRenderer.renderItem(LivingEntity, ...)} — which carries the holder, unlike
 * the builtin shield renderer — and, only when the stack is a shield AND the holder is sauced
 * ({@code SaucedEntities}), swap the VertexConsumerProvider for a {@link TintVertexConsumerProvider}. Because the
 * tint multiplies colour rather than replacing the model, a banner shield keeps its pattern, just reddened (E7).
 * Every other item render is returned the original provider untouched — zero effect on anything but sauced shields.
 *
 * Injection point (Mixin Checkmark): {@code @ModifyVariable} on the {@code VertexConsumerProvider} parameter at
 * HEAD of {@code renderItem(LivingEntity;ItemStack;ModelTransformationMode;Z;MatrixStack;VertexConsumerProvider;World;III)V}.
 *
 * Depends on: ItemRenderer, SaucedEntities, TintVertexConsumerProvider
 * Called by:  Mixin framework (listed in the "client" array of customblocks.mixins.json)
 */
package com.customblocks.mixin;

import com.customblocks.client.SaucedEntities;
import com.customblocks.client.render.TintVertexConsumerProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(ItemRenderer.class)
public class ShieldSauceTintMixin {

    @ModifyVariable(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
            at = @At("HEAD"), argsOnly = true)
    private VertexConsumerProvider customblocks$sauceTint(VertexConsumerProvider vertexConsumers,
            LivingEntity entity, ItemStack stack, ModelTransformationMode mode, boolean leftHanded,
            MatrixStack matrices, VertexConsumerProvider originalConsumers, World world, int light, int overlay, int seed) {
        if (stack != null && stack.isOf(Items.SHIELD) && entity != null && SaucedEntities.isSauced(entity)) {
            return new TintVertexConsumerProvider(vertexConsumers, 1.0f, 0.32f, 0.28f);
        }
        return vertexConsumers;
    }
}
