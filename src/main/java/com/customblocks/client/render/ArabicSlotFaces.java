/**
 * ArabicSlotFaces.java — Group 13 / G13-25 CP2. CLIENT-ONLY.
 *
 * Facing-aware cube draw for a placed REAL Arabic letter slot, ported 1:1 from the proven
 * ArabicLetterBlockEntityRenderer geometry (TG §E/§F confirmed):
 *   - the whole local frame is rotated so +Z lands on the glyph facing (the word-axis layout that
 *     tiles seamlessly), then front + left + right + top + bottom draw the block's OWN tile;
 *   - the BACK face draws LAST, on its own buffer, showing the mirror PARTNER's tile (readable
 *     back: the 180° turn alone keeps a glyph readable — like turning a sign around — so NO U-flip;
 *     the left/right reading order is handled by the partner-slot swap in ArabicSlotJoinFlow);
 *   - all faces sit FLUSH at z = 1.0 (the G13-19 v2 gap fix: 0.999 inset leaked a hairline seam at
 *     every edge, an overhang drew stray lines — exactly on the boundary seals the cube);
 *   - the glyph draws FULL-BRIGHT (the v2 fix: never dims grey when facing into a solid block);
 *   - buffer discipline: the provider keeps ONE RenderLayer building at a time, so all own-tile
 *     faces finish before the partner buffer is fetched, and we never switch back ("Not building!").
 *
 * Depends on: (vanilla render API only)
 * Called by:  AnimSlotBER.render (placed Arabic letter branch)
 */
package com.customblocks.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public final class ArabicSlotFaces {

    private ArabicSlotFaces() {} // static-only

    /** Draw the full letter cube: own tile on 5 faces oriented to {@code facing}, partner tile on
     *  the back. {@code backTex} may equal {@code frontTex} (lone letter / own mirror). */
    public static void draw(MatrixStack matrices, VertexConsumerProvider vcp,
                            Direction facing, Identifier frontTex, Identifier backTex) {
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE; // full-bright glyph, always
        matrices.push();
        // Orient local +Z onto the block's facing so the front face keeps the word-axis layout.
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));
        matrices.translate(-0.5, -0.5, -0.5);
        VertexConsumer own = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(frontTex));
        face(matrices, own, light, RotationAxis.POSITIVE_Y, 0f);    // front  (+Z)
        face(matrices, own, light, RotationAxis.POSITIVE_Y, 90f);   // left   (-X)
        face(matrices, own, light, RotationAxis.POSITIVE_Y, -90f);  // right  (+X)
        face(matrices, own, light, RotationAxis.POSITIVE_X, -90f);  // top    (+Y)
        face(matrices, own, light, RotationAxis.POSITIVE_X, 90f);   // bottom (-Y)
        // Back face LAST on the partner buffer — never switch back to the own buffer afterwards.
        VertexConsumer back = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(backTex));
        face(matrices, back, light, RotationAxis.POSITIVE_Y, 180f); // back (-Z) — turned, NOT mirrored
        matrices.pop();
    }

    /** Rotate the canonical south quad onto one face (about the cube centre) and draw it flush. */
    private static void face(MatrixStack matrices, VertexConsumer vc, int light,
                             RotationAxis axis, float degrees) {
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(axis.rotationDegrees(degrees));
        matrices.translate(-0.5, -0.5, -0.5);
        MatrixStack.Entry e = matrices.peek();
        Matrix4f m = e.getPositionMatrix();
        float z = 1.0f; // flush with the block boundary — no seam, no overhang (G13-19 v2)
        vert(vc, m, e, light, 0f, 1f, z, 0f, 0f); // top-left
        vert(vc, m, e, light, 1f, 1f, z, 1f, 0f); // top-right
        vert(vc, m, e, light, 1f, 0f, z, 1f, 1f); // bottom-right
        vert(vc, m, e, light, 0f, 0f, z, 0f, 1f); // bottom-left
        matrices.pop();
    }

    private static void vert(VertexConsumer vc, Matrix4f m, MatrixStack.Entry e, int light,
                             float x, float y, float z, float u, float v) {
        vc.vertex(m, x, y, z)
          .color(255, 255, 255, 255)
          .texture(u, v)
          .overlay(OverlayTexture.DEFAULT_UV)
          .light(light)
          .normal(e, 0f, 0f, 1f);
    }
}
