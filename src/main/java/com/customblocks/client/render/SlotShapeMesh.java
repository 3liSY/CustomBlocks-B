/**
 * SlotShapeMesh.java — Group 08 §B (reload-free shape draw). CLIENT-ONLY.
 *
 * Emits a slot's CURRENT shape as chunk-mesh geometry at bake time, so changing a shape never has to
 * re-emit a model into the resource pack (and never prompts a reload). The boxes come from
 * {@link BlockShapes} — the exact same coordinates the collision/outline VoxelShape is built from — so
 * "hitbox == visual" holds for every shape by construction, the way §J already guarantees it for stairs.
 *
 * Sprites are NOT resolved by Identifier. They are sampled off the wrapped model's own baked quads (the
 * pack always emits the FULL-cube model for a static slot under §B), which means a face's §E per-face
 * override arrives already resolved, with no texture-name guessing and no second lookup table.
 *
 * UVs use {@code BAKE_LOCK_UV}: the UV is derived from the vertex positions, which is exactly vanilla's
 * auto-UV for a model element — so a slab/pillar/pane samples the same part of the texture the pack-baked
 * model used to. G06 §G quarter-turns ride on the same call through the BAKE_ROTATE_* flag bits.
 *
 * Depends on: BlockShapes (geometry), FaceRotations (quarter-turns, via SlotBlock.resolveFaceRot).
 * Called by:  DirectionalSlotModel (the wrap that owns the per-slot state).
 */
package com.customblocks.client.render;

import com.customblocks.block.BlockShapes;
import com.customblocks.core.FaceRotations;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
public final class SlotShapeMesh {

    private SlotShapeMesh() {} // static-only

    /** Emit every box of {@code boxes} (pixel coords 0..16) as 6 faces each. */
    public static void emitBoxes(QuadEmitter e, int[][] boxes, Sprite[] faceSprites, int packedRot) {
        if (boxes == null) return;
        for (int[] b : boxes) {
            for (Direction d : Direction.values()) {
                Sprite sprite = faceSprites[d.getId()];
                if (sprite == null) continue;
                square(e, d, b);
                e.color(-1, -1, -1, -1);
                e.spriteBake(sprite, MutableQuadView.BAKE_LOCK_UV | FaceRotations.unpack(packedRot, d.getId()));
                e.emit();
            }
        }
    }

    /**
     * One box face through {@link QuadEmitter#square}, which owns the vertex winding AND sets the cull
     * face (a face sitting flat on the block boundary culls; an interior face does not). The left/bottom/
     * right/top/depth values below are that method's face-local frame, and depth is always measured inward
     * from the face being drawn.
     *
     * Every call MUST stay canonical — {@code left < right} and {@code bottom < top}. square() already does
     * the UP / EAST / SOUTH mirroring itself (that is what its {@code 1 - left} / {@code 1 - top} lines are),
     * so the mirror belongs in the VALUE only, never in the pair ORDER. Passing a mirrored pair in the old,
     * un-swapped order cancels square()'s own flip and reverses the winding, and a backwards quad is
     * back-face culled — which is exactly how UP/NORTH/EAST went invisible and made every non-full shape
     * look see-through from above and from the north/east (G08 §B regression, 2026-07-21).
     */
    private static void square(QuadEmitter e, Direction d, int[] b) {
        float x0 = b[0] / 16f, y0 = b[1] / 16f, z0 = b[2] / 16f;
        float x1 = b[3] / 16f, y1 = b[4] / 16f, z1 = b[5] / 16f;
        switch (d) {
            case DOWN  -> e.square(d, x0,       z0,       x1,       z1,       y0);
            case UP    -> e.square(d, x0,       1f - z1,  x1,       1f - z0,  1f - y1);
            case WEST  -> e.square(d, z0,       y0,       z1,       y1,       x0);
            case EAST  -> e.square(d, 1f - z1,  y0,       1f - z0,  y1,       1f - x1);
            case NORTH -> e.square(d, 1f - x1,  y0,       1f - x0,  y1,       z0);
            case SOUTH -> e.square(d, x0,       y0,       x1,       y1,       1f - z1);
        }
    }

    /**
     * The "cross" shape — two diagonal planes, each drawn from both sides, matching vanilla's
     * {@code block/cross} billboard (a flower/plant look). UVs are set explicitly instead of locked,
     * because a diagonal quad has no axis-aligned face to project onto.
     */
    public static void emitCross(QuadEmitter e, Sprite sprite) {
        if (sprite == null) return;
        // Plane A runs NW→SE, plane B runs NE→SW; the second pair of each is the back face.
        plane(e, sprite, 0f, 0f, 1f, 1f);
        plane(e, sprite, 1f, 1f, 0f, 0f);
        plane(e, sprite, 1f, 0f, 0f, 1f);
        plane(e, sprite, 0f, 1f, 1f, 0f);
    }

    /** One vertical billboard quad from (ax,az) to (bx,bz), full block height, textured edge-to-edge. */
    private static void plane(QuadEmitter e, Sprite sprite, float ax, float az, float bx, float bz) {
        e.pos(0, ax, 1f, az).uv(0, 0f, 0f);
        e.pos(1, ax, 0f, az).uv(1, 0f, 1f);
        e.pos(2, bx, 0f, bz).uv(2, 1f, 1f);
        e.pos(3, bx, 1f, bz).uv(3, 1f, 0f);
        e.color(-1, -1, -1, -1);
        e.cullFace(null);           // a billboard never sits flat on a block face
        e.nominalFace(Direction.UP); // light it from above, like vanilla's cross
        e.spriteBake(sprite, 0);
        e.emit();
    }

    /** The boxes to draw for a non-full, non-cross shape. Stairs resolve through their corner shape. */
    public static int[][] boxesFor(String shape, com.customblocks.block.StairShape stairShape) {
        if (BlockShapes.isDirectional(shape)) return BlockShapes.stairBoxes(stairShape);
        return BlockShapes.boxes(shape);
    }
}
