/**
 * AnimSlotBER.java — Group 14 / Phase 1b. CLIENT-ONLY.
 *
 * BlockEntityRenderer for placed custom blocks. For an ANIMATED slot it draws all six faces straight from
 * the slot's own off-atlas texture ({@link AnimFrameCache}), showing the current frame by shifting the V
 * coordinate over the vertical strip (vanilla's own animation trick) — with mipmaps OFF, so the placed
 * block is CRISP instead of being muffled by the block atlas. For a STATIC slot it draws nothing and the
 * block's normal atlas model renders it as before.
 *
 * The placed animated block's pack model is transparent (ServerPackGenerator emits an empty/barrier-style
 * model for animated slots), so only this renderer paints it — no atlas cube underneath, no double-draw.
 * Inventory/hand keep the atlas-animated icon via the decoupled item model.
 *
 * Modeled on ArabicLetterBlockEntityRenderer (the proven in-codebase off-atlas cube draw): faces sit at
 * z = 0.999 (just inside the surface, no edge overhang) and use getEntityCutoutNoCull so a transparent GIF
 * shows through correctly.
 *
 * Depends on: AnimSlotBlockEntity (slot index), AnimFrameCache (texture + frame timing).
 * Called by:  CustomBlocksClient (BlockEntityRendererFactories.register).
 */
package com.customblocks.client.render;

import com.customblocks.block.AnimSlotBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class AnimSlotBER implements BlockEntityRenderer<AnimSlotBlockEntity> {

    public AnimSlotBER(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(AnimSlotBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vcp, int light, int overlay) {
        int slot = be.slotIndex();
        if (slot < 0) return;
        AnimFrameCache.Slot s = AnimFrameCache.get(slot);
        if (s == null) return; // static slot — its normal atlas model already rendered it

        long worldTime = be.getWorld() != null ? be.getWorld().getTime() : 0L;
        int frame = s.currentStripIndex(worldTime, tickDelta);
        float vTop = (float) frame / s.frameCount;
        float vBot = (float) (frame + 1) / s.frameCount;

        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(s.textureId));
        // All six faces of the unit cube show the current frame (same texture, one buffer).
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_Y, 0f,   vTop, vBot); // front  (+Z)
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_Y, 90f,  vTop, vBot); // left   (-X)
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_Y, -90f, vTop, vBot); // right  (+X)
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_Y, 180f, vTop, vBot); // back   (-Z)
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_X, -90f, vTop, vBot); // top    (+Y)
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_X, 90f,  vTop, vBot); // bottom (-Y)
    }

    /** Rotate the canonical south quad onto one face (about the cube centre) and draw the frame band. */
    private static void face(MatrixStack matrices, VertexConsumer vc, int light, int overlay,
                             RotationAxis axis, float degrees, float vTop, float vBot) {
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(axis.rotationDegrees(degrees));
        matrices.translate(-0.5, -0.5, -0.5);
        MatrixStack.Entry e = matrices.peek();
        Matrix4f m = e.getPositionMatrix();
        float z = 0.999f; // just inside the surface — no edge overhang lines
        vert(vc, m, e, light, overlay, 0f, 1f, z, 0f, vTop); // top-left
        vert(vc, m, e, light, overlay, 1f, 1f, z, 1f, vTop); // top-right
        vert(vc, m, e, light, overlay, 1f, 0f, z, 1f, vBot); // bottom-right
        vert(vc, m, e, light, overlay, 0f, 0f, z, 0f, vBot); // bottom-left
        matrices.pop();
    }

    private static void vert(VertexConsumer vc, Matrix4f m, MatrixStack.Entry e, int light, int overlay,
                             float x, float y, float z, float u, float v) {
        vc.vertex(m, x, y, z)
          .color(255, 255, 255, 255)
          .texture(u, v)
          .overlay(overlay == 0 ? OverlayTexture.DEFAULT_UV : overlay)
          .light(light)
          .normal(e, 0f, 0f, 1f);
    }
}
