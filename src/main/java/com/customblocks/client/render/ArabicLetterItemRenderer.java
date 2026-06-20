/**
 * ArabicLetterItemRenderer.java — Group 13 / O6. CLIENT-ONLY.
 *
 * Draws the joinable letter's ITEM icon (inventory, hotbar, hand, dropped item, item frame) as the
 * same live-texture glyph cube the placed block uses — fixing the "all-black icon" bug, where the
 * item showed only the flat base model because the glyph is normally painted by the block-entity
 * renderer, which never runs for an item. Reads the letter / colour / form straight off the stack's
 * NBT and reuses {@link ArabicLetterBlockEntityRenderer#textureFor} (shared cache) +
 * {@link ArabicLetterBlockEntityRenderer#drawGlyphCube}. A held auto-join stack has no neighbours, so
 * it shows its isolated form; a fixed-form variant shows its locked form.
 *
 * Depends on: ArabicLetterBlock (NBT readers), ArabicJoining, ArabicLetterBlockEntityRenderer
 * Called by:  CustomBlocksClient (BuiltinItemRendererRegistry.register)
 */
package com.customblocks.client.render;

import com.customblocks.arabic.ArabicJoining;
import com.customblocks.block.ArabicLetterBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class ArabicLetterItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                       VertexConsumerProvider vcp, int light, int overlay) {
        char letter = ArabicLetterBlock.letterOf(stack);
        if (letter == 0) return;
        int locked = ArabicLetterBlock.lockedFormOf(stack);
        int form = (locked >= 0) ? locked : ArabicJoining.ISOLATED;
        // A held / hotbar stack has no neighbours → it shows its isolated (or locked) form art.
        Identifier tex = ArabicLetterBlockEntityRenderer.textureFor(letter, form, ArabicLetterBlock.colorOf(stack));
        if (tex == null) return;
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
        ArabicLetterBlockEntityRenderer.drawGlyphCube(matrices, vc, light);
    }
}
