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

        // Group 30 (Guess Mode) — the flagged holder's OWN client shows the "?" mystery cube instead of the
        // real icon/hand for a flagged block, in EVERY mode (hotbar, inventory, own hand, other players'
        // hands, F5). A WATCHER isn't flagged, so GuessDisguise returns false and they see the real block.
        if (GuessDisguise.disguiseItem(stack)) {
            GuessDisguise.drawLook(matrices, vcp, light, overlay, GuessDisguise.lookSlotForStack(stack));
            return;
        }

        int slot = sb.getSlotIndex();

        // Animated off-atlas slot → current frame's grid cell (kills the atlas icon muffle). Checked first so
        // an animated grid (which is roughly square) is never mistaken for a static texture below.
        AnimFrameCache.Slot s = AnimFrameCache.get(slot);
        if (s != null) {
            // ADR-014 Step 3 slice 1: one-cell per-frame texture, drawn whole (UV 0..1). Same millisecond
            // wall-clock as the placed block so the icon plays at true speed (Step 2).
            Identifier tex = s.prepare(AnimClock.nowMs());
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
            // The builtin/entity item model's display transforms already posed the matrices; draw into that space.
            AnimSlotBER.drawCube(matrices, vc, light, overlay, 0f, 1f, 0f, 1f);
            return;
        }

        // Static off-atlas slot → its full crisp texture on all six faces.
        Identifier tex = StaticFrameCache.get(slot);
        if (tex == null) {
            // Group 30 (Guess Mode): a plain static full-cube slot now also gets a builtin/entity item
            // model (see ServerPackGenerator) so its item icon can be disguised — its WORLD block model is
            // still the ordinary atlas cube_all, so StaticFrameCache.get above returns null for it. Draw the
            // real slot_N.png cube ourselves (same look as the atlas icon it replaces). Returns null here for
            // shape/per-face slots (still their own atlas item model, never reach this renderer at all).
            tex = StaticFrameCache.getIconFallback(slot);
            if (tex == null) return;
        }
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
        AnimSlotBER.drawCube(matrices, vc, light, overlay, 0f, 1f, 0f, 1f);
    }
}
