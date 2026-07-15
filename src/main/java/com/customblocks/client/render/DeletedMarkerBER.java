/**
 * DeletedMarkerBER.java — G06-14 slice 1. CLIENT-ONLY.
 *
 * Draws the always-visible floating tag above a Deleted marker: "Deleted: <name>" in RED, billboarded
 * toward the camera (the same way a name tag faces you). The block itself still renders its normal grey
 * + ✖ cube from its model/atlas — this renderer only adds the hovering label. Reads the name from the
 * marker's synced BlockEntity, so it updates with no resource pack / reload.
 *
 * Depends on: DeletedMarkerBlockEntity
 * Called by:  CustomBlocksClient (BlockEntityRendererFactories.register)
 */
package com.customblocks.client.render;

import com.customblocks.block.DeletedMarkerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Matrix4f;

public class DeletedMarkerBER implements BlockEntityRenderer<DeletedMarkerBlockEntity> {

    /** MC red (Formatting.RED) ARGB — the fallback colour passed to draw() (the Text is also RED-styled). */
    private static final int RED = 0xFFFF5555;

    private final TextRenderer textRenderer;

    public DeletedMarkerBER(BlockEntityRendererFactory.Context ctx) {
        this.textRenderer = ctx.getTextRenderer();
    }

    @Override
    public void render(DeletedMarkerBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vcp, int light, int overlay) {
        String label = be.label();
        if (label == null || label.isEmpty()) return;
        Text text = Text.literal(label).formatted(Formatting.RED);

        MinecraftClient client = MinecraftClient.getInstance();
        matrices.push();
        matrices.translate(0.5, 1.25, 0.5);                               // hover above the block centre
        matrices.multiply(client.getEntityRenderDispatcher().getRotation()); // billboard toward camera
        matrices.scale(-0.025f, -0.025f, 0.025f);                         // name-tag scale (flip X/Y)
        Matrix4f m = matrices.peek().getPositionMatrix();
        float x = -textRenderer.getWidth(text) / 2.0f;                    // centre horizontally
        int bgAlpha = (int) (client.options.getTextBackgroundOpacity(0.25f) * 255.0f) << 24;
        textRenderer.draw(text, x, 0f, RED, false, m, vcp,
                TextRenderer.TextLayerType.NORMAL, bgAlpha, light);
        matrices.pop();
    }
}
