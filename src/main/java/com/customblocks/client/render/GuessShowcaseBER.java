/**
 * GuessShowcaseBER.java — Group 30 (Guess Mode) · G30-8b Showcase. CLIENT-ONLY.
 *
 * Draws the floating end-crystal-style display for a placed {@link com.customblocks.block.GuessShowcaseBlock}:
 * a SINGLE spinning picture core (the shown slot's baked texture, or the bundled "?"), turntable-spinning on Y
 * at the shared core speed, with a gentle vertical bob. (The old translucent outer "cage" layer + end-rod
 * particles were removed — G30 §S: S1/S7; the Glow toggle was scrapped entirely — G30 §S: S6.) The look/feel
 * (core speed, size) is the ONE shared tuning from {@link ClientGuessState} (fed by GuessShowcaseStore); the
 * shown slot is per-display, read off the BlockEntity. The core is drawn with the normal world-lit cutout layer.
 * Wall-clock timing (AnimClock) so it spins at true speed regardless of tps.
 *
 * {@link #renderCore} is shared with the Guess Settings Showcase tab's in-screen live preview (G30 §S: S13).
 *
 * Depends on: GuessShowcaseBlockEntity (shown slot), ClientGuessState (shared tuning), GuessDisguise
 *   (look→texture), AnimClock (wall-clock ms).
 * Called by:  CustomBlocksClient (BlockEntityRendererFactories.register), GuessSettingsScreen (preview).
 */
package com.customblocks.client.render;

import com.customblocks.block.GuessShowcaseBlockEntity;
import com.customblocks.client.ClientGuessState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class GuessShowcaseBER implements BlockEntityRenderer<GuessShowcaseBlockEntity> {

    /** Base fraction of a block the core cube spans before the shared size multiplier. */
    private static final float BASE_SCALE = 0.55f;

    public GuessShowcaseBER(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(GuessShowcaseBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vcp, int light, int overlay) {
        Identifier tex = GuessDisguise.textureForLook(be.shownSlot()); // shown picture, or the bundled "?"

        float inner = ClientGuessState.showcaseInner();
        float size  = ClientGuessState.showcaseSize();

        long ms = AnimClock.nowMs();
        float t = (ms % 3_600_000L) / 1000f;         // seconds, wrapped (no float blow-up)
        float yaw = (t * inner) % 360f;
        float bob = (float) Math.sin(t * 1.6f) * 0.06f * size;   // gentle float

        matrices.push();
        matrices.translate(0.5, 0.5 + bob, 0.5); // origin → block centre (plus the bob)
        renderCore(matrices, vcp, tex, light, overlay, size, yaw);
        matrices.pop();
    }

    /**
     * Draw the spinning picture core at the current matrix origin: scaled by {@code size}, spun {@code yaw}° on Y,
     * with the normal world-lit cutout cube. Shared by the world BER (above) and the Showcase tab's in-screen
     * live preview (GuessSettingsScreen, S13).
     */
    public static void renderCore(MatrixStack matrices, VertexConsumerProvider vcp, Identifier tex,
                                  int light, int overlay, float size, float yaw) {
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex)); // normal world-lit

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        float scale = BASE_SCALE * size;
        matrices.scale(scale, scale, scale);
        matrices.translate(-0.5, -0.5, -0.5); // centre the unit cube on the origin
        drawCube(matrices, vc, light, overlay);
        matrices.pop();
    }

    // ── Cube draw (six faces of the unit cube). Full texture on every face (UV 0..1). ──────────────────────
    private static void drawCube(MatrixStack matrices, VertexConsumer vc, int light, int overlay) {
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_Y, 0f);   // front  (+Z)
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_Y, 90f);  // left   (-X)
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_Y, -90f); // right  (+X)
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_Y, 180f); // back   (-Z)
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_X, -90f); // top    (+Y)
        face(matrices, vc, light, overlay, RotationAxis.POSITIVE_X, 90f);  // bottom (-Y)
    }

    private static void face(MatrixStack matrices, VertexConsumer vc, int light, int overlay,
                             RotationAxis axis, float degrees) {
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(axis.rotationDegrees(degrees));
        matrices.translate(-0.5, -0.5, -0.5);
        MatrixStack.Entry e = matrices.peek();
        Matrix4f m = e.getPositionMatrix();
        float z = 1.0f;
        vert(vc, m, e, light, overlay, 0f, 1f, z, 0f, 0f);
        vert(vc, m, e, light, overlay, 1f, 1f, z, 1f, 0f);
        vert(vc, m, e, light, overlay, 1f, 0f, z, 1f, 1f);
        vert(vc, m, e, light, overlay, 0f, 0f, z, 0f, 1f);
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
