/**
 * TomatoEntityRenderer.java — Group 32 (Explosive Tomato). CLIENT-SIDE ONLY.
 *
 * Draws the flying / ridden tomato as a camera-facing billboard of
 * {@code textures/entity/explosive_tomato.png} (256×256).
 *
 * ── Why not FlyingItemEntityRenderer (which is what this replaces) ───────────────────────────────
 * That renderer draws the ITEM MODEL, and Minecraft extrudes a flat item sprite into a slab with real
 * thickness — so the tomato flew as a red tile with visible edges, and its silhouette changed depending on
 * where you stood. A billboard is a flat quad that always faces the camera: the tomato reads as a round
 * tomato from every angle, at the full 256, with no extruded rim.
 *
 * The texture is a flat front-view illustration with its shading baked in, NOT a UV unwrap — there is no
 * side, back or top face in it. That is exactly why this is a billboard and not a model: a cube or sphere
 * textured with this would smear the same front-view picture across every face. A modelled 3D tomato needs
 * an unwrapped texture that does not exist yet (open G32 art task).
 *
 * Depends on: TomatoEntity, CustomBlocksMod (MOD_ID)
 * Called by:  CustomBlocksClient (EntityRendererRegistry — keep it in step with TomatoRegistry.TOMATO)
 */
package com.customblocks.client.render;

import com.customblocks.CustomBlocksMod;
import com.customblocks.tomato.TomatoEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class TomatoEntityRenderer extends EntityRenderer<TomatoEntity> {

    private static final Identifier TEXTURE =
            Identifier.of(CustomBlocksMod.MOD_ID, "textures/entity/explosive_tomato.png");

    /** Edge length of the quad in blocks. The entity hitbox is 0.55 — the sprite reads best a touch larger. */
    private static final float SIZE = 0.7f;

    public TomatoEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(TomatoEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(TomatoEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vcp, int light) {
        matrices.push();
        // The whole billboard: rotate the quad by the CAMERA's rotation, so its face is always square to the
        // viewer. dispatcher.getRotation() is the same source the vanilla item renderer uses for this.
        matrices.multiply(dispatcher.getRotation());

        // §B fuse tell: for the ~0.5s before it blows, the tomato SWELLS and strobes WHITE like primed TNT —
        // the rider's visible bail cue. The fuse value is synced from the server (TomatoEntity.getFuse()).
        int fuse = entity.getFuse();
        float scale = SIZE;
        int overlay = OverlayTexture.DEFAULT_UV;
        if (fuse > 0) {
            float t = (TomatoEntity.FUSE_TICKS - fuse) / (float) TomatoEntity.FUSE_TICKS; // 0 → 1 across the fuse
            scale = SIZE * (1f + 0.5f * t);                                                // swell up to 1.5x
            if (fuse % 2 == 0) overlay = OverlayTexture.packUv(OverlayTexture.getU(0f), OverlayTexture.getV(true)); // white flash
        }
        matrices.scale(scale, scale, scale);

        MatrixStack.Entry e = matrices.peek();
        Matrix4f m = e.getPositionMatrix();
        // NoCull: the quad is one-sided geometry, and a billboard must never vanish when the maths puts the
        // camera a hair behind it.
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));

        // A unit quad centred on the entity's origin. (0,0) of the texture is its top-left.
        vert(vc, m, e, light, overlay, -0.5f,  0.5f, 0f, 1f); // bottom-left
        vert(vc, m, e, light, overlay,  0.5f,  0.5f, 1f, 1f); // bottom-right
        vert(vc, m, e, light, overlay,  0.5f, -0.5f, 1f, 0f); // top-right
        vert(vc, m, e, light, overlay, -0.5f, -0.5f, 0f, 0f); // top-left

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vcp, light);
    }

    /** Same vertex shape the mod's block-entity renderers use (see AnimSlotBER.vert). */
    private static void vert(VertexConsumer vc, Matrix4f m, MatrixStack.Entry e, int light, int overlay,
                             float x, float y, float u, float v) {
        vc.vertex(m, x, y, 0f)
          .color(255, 255, 255, 255)
          .texture(u, v)
          .overlay(overlay)
          .light(light)
          .normal(e, 0f, 0f, 1f);
    }
}
