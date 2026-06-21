/**
 * SlotItemRenderer.java — Group 14 / Phase 1c. CLIENT-ONLY.
 *
 * Draws an off-atlas block's ITEM icon (inventory, hotbar, hand, dropped item, item frame) as the same
 * crisp off-atlas cube the placed block uses — so no custom block touches the block atlas for its visual
 * (owner: "no atlas anymore, forever"). Reads the slot index off the held BlockItem and reuses
 * {@link AnimSlotBER#drawCube}: a STATIC slot draws its full {@link StaticFrameCache} texture; an ANIMATED
 * slot (Phase 1c Step 3) draws the current frame band from {@link AnimFrameCache} — killing the last atlas
 * muffle (the animated icon used to animate via the atlas).
 *
 * This one renderer is registered for EVERY slot item, but it only paints off-atlas slots: ServerPackGenerator
 * gives static AND animated off-atlas slots a {@code builtin/entity} item model (which is what routes an item
 * to a DynamicItemRenderer); shaped / per-face items keep their atlas item model and both caches return null,
 * so this draws nothing for them.
 *
 * Depends on: SlotBlock (slot index off the BlockItem), StaticFrameCache, AnimFrameCache, AnimSlotBER (drawCube).
 * Called by:  CustomBlocksClient (BuiltinItemRendererRegistry.register).
 */
package com.customblocks.client.render;

import com.customblocks.block.SlotBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class SlotItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                       VertexConsumerProvider vcp, int light, int overlay) {
        if (!(stack.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof SlotBlock sb)) return;
        int slot = sb.getSlotIndex();

        // Static off-atlas slot → its full crisp texture on all six faces.
        Identifier tex = StaticFrameCache.get(slot);
        if (tex != null) {
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
            // The builtin/entity item model's display transforms already posed the matrices; draw into that space.
            AnimSlotBER.drawCube(matrices, vc, light, overlay, 0f, 1f);
            return;
        }

        // Animated off-atlas slot (Step 3) → current frame band from its strip (kills the atlas icon muffle).
        AnimFrameCache.Slot s = AnimFrameCache.get(slot);
        if (s == null) return; // not off-atlas (shaped / per-face) — its normal atlas item model drew it
        MinecraftClient mc = MinecraftClient.getInstance();
        long worldTime = (mc != null && mc.world != null) ? mc.world.getTime() : 0L;
        int frame = s.currentStripIndex(worldTime, 0f);
        float vTop = (float) frame / s.frameCount;
        float vBot = (float) (frame + 1) / s.frameCount;
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(s.textureId));
        AnimSlotBER.drawCube(matrices, vc, light, overlay, vTop, vBot);
    }
}
