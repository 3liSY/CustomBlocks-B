/**
 * DirectionalSlotModel.java — Group 08 §J (placement rotation) + §B (reload-free shape draw). CLIENT-ONLY.
 *
 * The one wrap around a static slot's baked model. It does two jobs during a chunk rebuild, both batched
 * into the section mesh (never a per-frame {@code BlockEntityRenderer} draw, which craters FPS in bulk —
 * the GT 730 risk this design exists to avoid):
 *
 *   §B — draws the slot's CURRENT shape by emitting {@link BlockShapes} boxes through {@link SlotShapeMesh},
 *        instead of relying on the resource pack to have baked a model for that shape. A shape change is
 *        therefore a data change plus a re-mesh: no pack rebuild, no reload prompt. The pack always emits
 *        the FULL-cube model for a static slot, so {@code shape=full} is a literal pass-through to the
 *        wrapped model (byte-identical to pre-§B rendering) and the wrapped quads double as the sprite
 *        source for every other shape — which is how §E per-face overrides carry across a shape change.
 *   §J — rotates that geometry to the placement's facing/half (and its computed corner shape, for stairs).
 *
 * Geometry and collision are the same boxes through the same {@link BlockShapes#orientPoint} math, so the
 * block you see and the block you walk into cannot disagree, for any shape.
 *
 * Depends on: BlockShapes (geometry + orient), SlotShapeMesh (emit), SlotGeometryData (synced shape + rotations),
 *             SlotOrientation (BE render data).
 * Called by:  SlotModelPlugin (wraps the baked model), the Fabric chunk mesher (emitBlockQuads).
 */
package com.customblocks.client.render;

import com.customblocks.block.BlockShapes;
import com.customblocks.block.SlotGeometryData;
import com.customblocks.block.SlotOrientation;
import com.customblocks.block.StairShape;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import java.util.List;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class DirectionalSlotModel extends ForwardingBakedModel {

    private final int slot;
    private final String slotKey;

    /** The wrapped full cube's sprite per face, indexed by {@link Direction#getId()}. Sampled once on
     *  first use — the wrapped model is re-baked on every resource reload, so it can never go stale. */
    private volatile Sprite[] faceSprites;

    public DirectionalSlotModel(BakedModel wrapped, int slot) {
        this.wrapped = wrapped;
        this.slot = slot;
        this.slotKey = "slot_" + slot;
    }

    /** Must be false, or the renderer takes the vanilla fast path and never calls emitBlockQuads. */
    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos,
                               Supplier<Random> randomSupplier, RenderContext context) {
        String shape = SlotGeometryData.resolveShape(slot, slotKey);

        // §B pass-through: a full cube IS the wrapped model. Nothing is re-emitted, so the ~99% of blocks
        // that are full cubes render exactly as they did before §B — same quads, same everything.
        if (BlockShapes.isFull(shape)) {
            super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
            return;
        }

        Sprite[] sprites = sprites(state, randomSupplier.get());
        int packedRot = SlotGeometryData.resolveFaceRot(slot);

        if (BlockShapes.isCross(shape)) {
            // A cross is a billboard: it has no facing/half and no corner, so it never rotates.
            SlotShapeMesh.emitCross(context.getEmitter(), sprites[Direction.NORTH.getId()]);
            return;
        }

        // §J — a directional shape (stairs) reads its placement off the BlockEntity render data. Anything
        // else is orientation-free and emits in the base frame.
        Object data = (blockView instanceof FabricBlockView fbv) ? fbv.getBlockEntityRenderData(pos) : null;
        SlotOrientation o = (data instanceof SlotOrientation so) ? so : null;
        boolean directional = BlockShapes.isDirectional(shape);
        StairShape stairShape = (o == null || o.shape() == null) ? StairShape.STRAIGHT : o.shape();
        int[][] boxes = SlotShapeMesh.boxesFor(shape, stairShape);

        if (directional && o != null && !o.isBase()) {
            final Direction facing = o.facing();
            final BlockHalf half = o.half();
            context.pushTransform(q -> rotate(q, facing, half));
            SlotShapeMesh.emitBoxes(context.getEmitter(), boxes, sprites, packedRot);
            context.popTransform();
        } else {
            SlotShapeMesh.emitBoxes(context.getEmitter(), boxes, sprites, packedRot);
        }
    }

    /**
     * The six face sprites of the wrapped full-cube model. Each face's quad already carries the sprite the
     * pack resolved for it, so a §E per-face override needs no lookup here. A face with no quad (an odd or
     * partial model) falls back to the particle sprite, so a shape can never render untextured.
     */
    private Sprite[] sprites(BlockState state, Random random) {
        Sprite[] cached = faceSprites;
        if (cached != null) return cached;
        Sprite[] out = new Sprite[6];
        Sprite particle = wrapped.getParticleSprite();
        for (Direction d : Direction.values()) {
            List<BakedQuad> quads = wrapped.getQuads(state, d, random);
            if (quads.isEmpty()) quads = wrapped.getQuads(state, null, random);
            out[d.getId()] = quads.isEmpty() ? particle : quads.get(0).getSprite();
            if (out[d.getId()] == null) out[d.getId()] = particle;
        }
        faceSprites = out;
        return out;
    }

    /** Move each vertex + its face/normal by the shared orient math; keep every other quad property intact. */
    private static boolean rotate(MutableQuadView q, Direction facing, BlockHalf half) {
        float[] p = new float[3];
        for (int i = 0; i < 4; i++) {
            BlockShapes.orientPoint(q.x(i), q.y(i), q.z(i), facing, half, p);
            q.pos(i, p[0], p[1], p[2]);
            if (q.hasNormal(i)) {
                float[] n = { q.normalX(i), q.normalY(i), q.normalZ(i) };
                rotateVec(n, facing, half);
                q.normal(i, n[0], n[1], n[2]);
            }
        }
        // Cull face controls chunk-boundary occlusion; setting it also updates the nominal (light) face.
        Direction cull = q.cullFace();
        if (cull != null) {
            q.cullFace(BlockShapes.orientDir(cull, facing, half));
        } else {
            Direction nom = q.nominalFace();
            if (nom != null) q.nominalFace(BlockShapes.orientDir(nom, facing, half));
        }
        return true;
    }

    /** Linear part of the orient transform on a float vector (matches BlockShapes.orientDir exactly). */
    private static void rotateVec(float[] v, Direction facing, BlockHalf half) {
        if (half == BlockHalf.TOP) { v[1] = -v[1]; v[2] = -v[2]; }        // rotX180 linear
        int k = (BlockShapes.facingSteps(facing) + BlockShapes.topExtraSteps(half)) & 3; // J8: TOP also y:180
        for (int r = 0; r < k; r++) { float nx = -v[2]; v[2] = v[0]; v[0] = nx; } // rotY90cw linear
    }
}
