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
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
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

        // Group 30 (Guess Mode) — a flagged holder sees a placed copy of a FLAGGED block as the disguise cube,
        // so a placed copy can't reveal the answer. Local to the holder (others see the real block). Every placed
        // SlotBlock already carries this BlockEntity (SlotBlock.createBlockEntity), atlas or not, so this fires
        // every frame regardless of block type. GuessDisguise.drawLook inflates the disguise cube ~1.2% so its
        // faces land just OUTSIDE a plain atlas block's baked faces (z=0/1) and win the depth test, opaquely
        // covering the real cube instead of z-fighting behind it — so no atlas/off-atlas gate is needed here.
        // The look picture itself (v3 Phase 1) comes from ClientGuessState.localLookSlot (override→default→"?").
        if (GuessDisguise.disguiseWorld(slot)) {
            GuessDisguise.drawLook(matrices, vcp, light, overlay,
                    com.customblocks.client.ClientGuessState.localLookSlot(slot));
            return;
        }

        AnimFrameCache.Slot s = AnimFrameCache.get(slot);
        if (s != null) {
            // Animated off-atlas. ADR-014 Step 3 slice 1: the slot uploads only the CURRENT frame to a
            // one-cell texture (VRAM bounded by what's on screen), so we draw the whole cell (UV 0..1).
            // ADR-014 Step 2 — millisecond wall-clock (not the 20-tps world tick) so it plays at true speed.
            Identifier tex = s.prepare(AnimClock.nowMs());
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
            drawCube(matrices, vc, light, overlay, 0f, 1f, 0f, 1f);
            return;
        }

        // Group 14 Phase 1c — STATIC off-atlas block: one crisp full-frame texture (no atlas mipmap muffle).
        // Returns null for an ordinary ATLAS static slot (non-full shape / per-face / pre-1c pack), whose
        // cube_all/shape model already drew it — so we paint nothing and never double-draw.
        Identifier staticTex = StaticFrameCache.get(slot);
        if (staticTex == null) return;

        // G13-25 CP2 — a placed Arabic LETTER carries a server-stamped facing on its BlockEntity:
        // draw facing-aware with the readable back (mirror partner's tile). Numbers and not-yet-
        // stamped placements have no facing and fall through to the plain six-face cube below.
        Direction arabicFacing = be.arabicFacing();
        if (arabicFacing != null) {
            Identifier backTex = null;
            int backSlot = be.arabicBackSlot();
            if (backSlot >= 0) backTex = StaticFrameCache.get(backSlot);
            if (backTex == null) backTex = staticTex; // partner not ready / own mirror → own tile
            ArabicSlotFaces.draw(matrices, vcp, arabicFacing, staticTex, backTex);
            return;
        }

        // G13-25 CP3b — an Arabic LETTER whose facing hasn't been stamped/predicted yet still
        // draws FULL-BRIGHT (the old join system always did; the world-lit fallback read as
        // "dim/grey letters" in the MP sweep). Numbers keep normal world lighting.
        int cubeLight = com.customblocks.client.ArabicClientView.isLetterSlot(slot)
                ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light;
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(staticTex));
        drawCube(matrices, vc, cubeLight, overlay, 0f, 1f, 0f, 1f); // full texture on all six faces
    }

    /**
     * Draw all six faces of the unit cube from {@code vc}'s texture, sampling the UV rectangle [u0,u1]×[v0,v1]
     * — one grid cell for an animated frame, or 0..1 / 0..1 for a static full texture. Shared by the placed
     * BER (this class) and the hand/inventory icon ({@link SlotItemRenderer}) so both draw the identical cube.
     */
    public static void drawCube(MatrixStack matrices, VertexConsumer vc, int light, int overlay,
                                float u0, float u1, float v0, float v1) {
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_Y, 0f,   u0, u1, v0, v1); // front  (+Z)
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_Y, 90f,  u0, u1, v0, v1); // left   (-X)
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_Y, -90f, u0, u1, v0, v1); // right  (+X)
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_Y, 180f, u0, u1, v0, v1); // back   (-Z)
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_X, -90f, u0, u1, v0, v1); // top    (+Y)
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_X, 90f,  u0, u1, v0, v1); // bottom (-Y)
    }

    /** Rotate the canonical south quad onto one face (about the cube centre) and draw the UV cell. */
    private static void face(MatrixStack matrices, VertexConsumer vc, int light, int overlay,
                             RotationAxis axis, float degrees, float u0, float u1, float v0, float v1) {
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(axis.rotationDegrees(degrees));
        matrices.translate(-0.5, -0.5, -0.5);
        MatrixStack.Entry e = matrices.peek();
        Matrix4f m = e.getPositionMatrix();
        float z = 0.999f; // just inside the surface — no edge overhang lines
        vert(vc, m, e, light, overlay, 0f, 1f, z, u0, v0); // top-left
        vert(vc, m, e, light, overlay, 1f, 1f, z, u1, v0); // top-right
        vert(vc, m, e, light, overlay, 1f, 0f, z, u1, v1); // bottom-right
        vert(vc, m, e, light, overlay, 0f, 0f, z, u0, v1); // bottom-left
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
