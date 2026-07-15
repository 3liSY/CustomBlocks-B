/**
 * ScreenTestBlockEntityRenderer.java — Group 14 "own-texture" MOCKUP. CLIENT-ONLY.
 *
 * Draws the test block's six faces using a LIVE in-memory texture (NativeImageBackedTexture)
 * registered straight into the TextureManager — never the resource pack, never the block atlas, and
 * (crucially) with NO mipmap. This is the same path Minecraft maps use, which is why maps stay sharp
 * at any distance. It is the candidate fix for the "muffled / pixelated" custom blocks: a block can
 * carry a FULL-resolution picture and pick its own filtering, instead of being squeezed into the
 * shared block atlas (small = blocky, big = atlas-overflow muffle).
 *
 * The picture set comes from {@link ScreenTestImages}: high-res sharpness cards, an animated card,
 * and (best-effort) the player's OWN created blocks at full source resolution for a direct A/B
 * against the same block placed normally. Right-click the block to cycle through the gallery; the
 * change is instant (no pack rebuild, no "reloading resources").
 *
 * Each (example, frame) is uploaded + cached once; animated examples advance by world time. Faces are
 * drawn no-cull so winding never hides one. The face/vertex emit code is unchanged from the proven
 * Group 13 spike — only the texture SOURCE changed.
 *
 * Depends on: ScreenTestBlockEntity, ScreenTestImages
 * Called by:  CustomBlocksClient (BlockEntityRendererFactories.register)
 */
package com.customblocks.client.render;

import com.customblocks.CustomBlocksMod;
import com.customblocks.block.ScreenTestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScreenTestBlockEntityRenderer implements BlockEntityRenderer<ScreenTestBlockEntity> {

    /** "exampleIndex_frameIndex" → uploaded dynamic-texture id. Built + registered once, then reused. */
    private static final Map<String, Identifier> CACHE = new HashMap<>();

    /** Ticks each animated frame shows for (~10 fps) so motion is visible but not frantic. */
    private static final int TICKS_PER_FRAME = 2;

    public ScreenTestBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(ScreenTestBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vcp, int light, int overlay) {
        List<ScreenTestImages.Example> gallery = ScreenTestImages.get();
        if (gallery.isEmpty()) return;

        int idx = Math.floorMod(be.variant(), gallery.size());
        ScreenTestImages.Example ex = gallery.get(idx);

        int frame = 0;
        if (ex.frames.size() > 1) {
            long time = be.getWorld() != null ? be.getWorld().getTime() : 0L;
            frame = (int) ((time / TICKS_PER_FRAME) % ex.frames.size());
        }

        Identifier tex = upload(idx, frame, ex);
        if (tex == null) return;

        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
        MatrixStack.Entry e = matrices.peek();
        Matrix4f m = e.getPositionMatrix();

        // Slightly inflate past the 0..1 cube so the live picture sits just outside the base model
        // (no z-fighting with the stone base).
        float a = -0.002f, b = 1.002f;

        face(vc, m, e, light, a, b, a, b, b, a, b, a, a, a, a, a, 0, 0, -1); // north (z-)
        face(vc, m, e, light, b, b, b, a, b, b, a, a, b, b, a, b, 0, 0, 1);  // south (z+)
        face(vc, m, e, light, a, b, b, a, b, a, a, a, a, a, a, b, -1, 0, 0); // west (x-)
        face(vc, m, e, light, b, b, a, b, b, b, b, a, b, b, a, a, 1, 0, 0);  // east (x+)
        face(vc, m, e, light, a, b, b, b, b, b, b, b, a, a, b, a, 0, 1, 0);  // up (y+)
        face(vc, m, e, light, a, a, a, b, a, a, b, a, b, a, a, b, 0, -1, 0); // down (y-)
    }

    /** Upload (or fetch from cache) the texture for one example frame, with its chosen filter. */
    private static Identifier upload(int exampleIdx, int frame, ScreenTestImages.Example ex) {
        String key = exampleIdx + "_" + frame;
        Identifier cached = CACHE.get(key);
        if (cached != null) return cached;
        try {
            NativeImageBackedTexture t = new NativeImageBackedTexture(ex.frames.get(frame));
            t.setFilter(ex.smooth, false); // smooth = linear (photos); else nearest (pixel-exact). mipmap OFF either way.
            Identifier id = Identifier.of(CustomBlocksMod.MOD_ID, "screen_test_dyn_" + key);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, t);
            CACHE.put(key, id);
            return id;
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Screen-test upload failed for {}: {}", key, e.toString());
            return null;
        }
    }

    /** Emit one quad (4 corners + outward normal), full-white tint, UV mapped 0..1. */
    private static void face(VertexConsumer vc, Matrix4f m, MatrixStack.Entry e, int light,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float nx, float ny, float nz) {
        vert(vc, m, e, light, x1, y1, z1, 0f, 0f, nx, ny, nz);
        vert(vc, m, e, light, x2, y2, z2, 1f, 0f, nx, ny, nz);
        vert(vc, m, e, light, x3, y3, z3, 1f, 1f, nx, ny, nz);
        vert(vc, m, e, light, x4, y4, z4, 0f, 1f, nx, ny, nz);
    }

    private static void vert(VertexConsumer vc, Matrix4f m, MatrixStack.Entry e, int light,
                             float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        vc.vertex(m, x, y, z)
          .color(255, 255, 255, 255)
          .texture(u, v)
          .overlay(OverlayTexture.DEFAULT_UV)
          .light(light)
          .normal(e, nx, ny, nz);
    }
}
