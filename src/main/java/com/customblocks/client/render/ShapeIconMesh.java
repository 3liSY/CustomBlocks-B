/**
 * ShapeIconMesh.java — Group 08 §B (shaped item icon). CLIENT-ONLY.
 *
 * Draws a slot's ITEM icon (hotbar, inventory, hand, dropped item, item frame) in the slot's CURRENT
 * shape, from the very same {@link BlockShapes} boxes the placed block's mesh and its hitbox come from
 * ({@link SlotShapeMesh#boxesFor}). One geometry source for all three, so a stair icon can never drift
 * from the stair you place or the stair you walk into.
 *
 * Why this exists: §B made the resource pack shape-BLIND (a static slot always ships the full-cube model),
 * which is what makes `/cb setshape` reload-free — but it also left every shaped block with a CUBE icon.
 * Drawing the shape here fixes the icon on the CLIENT, so no pack byte depends on the shape and the
 * reload-free property is kept intact.
 *
 * Reach: a plain shaped slot (no §E painted faces, no G06 §G rotations) routes its icon through
 * {@code builtin/entity} → {@link SlotItemRenderer} → here. A painted/rotated slot keeps its atlas cube
 * item model and never reaches this class — its icon stays a painted cube (the remaining §B trade; making
 * it shaped too would need the icon model to depend on the shape, i.e. a pack push per `/cb setshape`).
 *
 * Depends on: BlockShapes / SlotShapeMesh (the boxes), AnimSlotBER (the shared vertex format).
 * Called by:  SlotItemRenderer (static off-atlas icon path).
 */
package com.customblocks.client.render;

import com.customblocks.block.BlockShapes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public final class ShapeIconMesh {

    private ShapeIconMesh() {} // static-only

    /**
     * Draw every box of {@code boxes} (pixel coords 0..16) as six faces in the item's unit-cube space —
     * the same space {@link AnimSlotBER#drawCube} draws in, so the display transforms the builtin/entity
     * item model already applied carry over unchanged.
     *
     * UVs are taken from the vertex positions the way vanilla's auto-UV does it for a model element (and
     * the way {@code BAKE_LOCK_UV} does it for the world mesh): a slab's side shows the texture's bottom
     * half, not a squashed whole texture.
     */
    public static void drawBoxes(MatrixStack matrices, VertexConsumer vc, int light, int overlay,
                                 int[][] boxes) {
        drawBoxes(matrices, vc, light, overlay, boxes, null);
    }

    /**
     * F3 (2026-07-22): the same boxes, but pre-rotated to a fixed presentation {@code facing} (bottom
     * half) through the shared {@link BlockShapes#orientPoint}/orientDir math — so a DIRECTIONAL stair
     * icon presents its step toward the viewer like a vanilla stair item, instead of the base NORTH
     * frame's tall back. Pass {@code null} for the plain base-frame draw (every non-directional shape).
     * ICON PATH ONLY — the placed mesh and the hitbox are untouched. The item layer is no-cull, so the
     * rotated winding never drops a face; the rotation only re-aims the presented step + its lighting.
     */
    public static void drawBoxes(MatrixStack matrices, VertexConsumer vc, int light, int overlay,
                                 int[][] boxes, Direction facing) {
        if (boxes == null) return;
        MatrixStack.Entry e = matrices.peek();
        Matrix4f m = e.getPositionMatrix();
        for (int[] b : boxes) {
            float x0 = b[0] / 16f, y0 = b[1] / 16f, z0 = b[2] / 16f;
            float x1 = b[3] / 16f, y1 = b[4] / 16f, z1 = b[5] / 16f;

            // DOWN (−Y) / UP (+Y): u from x, v from z.
            quad(vc, m, e, light, overlay, facing, 0f, -1f, 0f,
                    x0, y0, z0, x0, z0,  x1, y0, z0, x1, z0,  x1, y0, z1, x1, z1,  x0, y0, z1, x0, z1);
            quad(vc, m, e, light, overlay, facing, 0f, 1f, 0f,
                    x0, y1, z1, x0, z1,  x1, y1, z1, x1, z1,  x1, y1, z0, x1, z0,  x0, y1, z0, x0, z0);

            // NORTH (−Z) / SOUTH (+Z): u from x (north mirrored, like vanilla), v from y (top of the
            // block is the top of the texture, so v = 1 − y).
            quad(vc, m, e, light, overlay, facing, 0f, 0f, -1f,
                    x1, y1, z0, 1f - x1, 1f - y1,  x0, y1, z0, 1f - x0, 1f - y1,
                    x0, y0, z0, 1f - x0, 1f - y0,  x1, y0, z0, 1f - x1, 1f - y0);
            quad(vc, m, e, light, overlay, facing, 0f, 0f, 1f,
                    x0, y1, z1, x0, 1f - y1,  x1, y1, z1, x1, 1f - y1,
                    x1, y0, z1, x1, 1f - y0,  x0, y0, z1, x0, 1f - y0);

            // WEST (−X) / EAST (+X): u from z (east mirrored, like vanilla), v from y.
            quad(vc, m, e, light, overlay, facing, -1f, 0f, 0f,
                    x0, y1, z0, z0, 1f - y1,  x0, y1, z1, z1, 1f - y1,
                    x0, y0, z1, z1, 1f - y0,  x0, y0, z0, z0, 1f - y0);
            quad(vc, m, e, light, overlay, facing, 1f, 0f, 0f,
                    x1, y1, z1, 1f - z1, 1f - y1,  x1, y1, z0, 1f - z0, 1f - y1,
                    x1, y0, z0, 1f - z0, 1f - y0,  x1, y0, z1, 1f - z1, 1f - y0);
        }
    }

    /** One quad: four (x,y,z,u,v) vertices sharing one outward normal. When {@code facing} is non-null,
     *  every vertex + the normal is pre-rotated to that fixed presentation facing (bottom half) — F3. */
    private static void quad(VertexConsumer vc, Matrix4f m, MatrixStack.Entry e, int light, int overlay,
                             Direction facing,
                             float nx, float ny, float nz,
                             float ax, float ay, float az, float au, float av,
                             float bx, float by, float bz, float bu, float bv,
                             float cx, float cy, float cz, float cu, float cv,
                             float dx, float dy, float dz, float du, float dv) {
        vert(vc, m, e, light, overlay, facing, nx, ny, nz, ax, ay, az, au, av);
        vert(vc, m, e, light, overlay, facing, nx, ny, nz, bx, by, bz, bu, bv);
        vert(vc, m, e, light, overlay, facing, nx, ny, nz, cx, cy, cz, cu, cv);
        vert(vc, m, e, light, overlay, facing, nx, ny, nz, dx, dy, dz, du, dv);
    }

    /** Same vertex format {@link AnimSlotBER} writes, so both icons feed one render layer. When
     *  {@code facing} is non-null (F3), the vertex + normal are rotated to that fixed facing first. */
    private static void vert(VertexConsumer vc, Matrix4f m, MatrixStack.Entry e, int light, int overlay,
                             Direction facing,
                             float nx, float ny, float nz, float x, float y, float z, float u, float v) {
        if (facing != null) {
            float[] p = new float[3];
            BlockShapes.orientPoint(x, y, z, facing, BlockHalf.BOTTOM, p); // icon has no half → bottom
            x = p[0]; y = p[1]; z = p[2];
            float[] n = { nx, ny, nz };
            rotateNormal(n, facing);
            nx = n[0]; ny = n[1]; nz = n[2];
        }
        vc.vertex(m, x, y, z)
          .color(255, 255, 255, 255)
          .texture(u, v)
          .overlay(overlay == 0 ? OverlayTexture.DEFAULT_UV : overlay)
          .light(light)
          .normal(e, nx, ny, nz);
    }

    /** Linear part of the orient transform for a bottom half — only y:90·k for the facing, matching
     *  {@link BlockShapes#orientDir} (which is what the placed mesh's normals use). */
    private static void rotateNormal(float[] v, Direction facing) {
        int k = BlockShapes.facingSteps(facing) & 3;
        for (int r = 0; r < k; r++) { float nx = -v[2]; v[2] = v[0]; v[0] = nx; } // rotY90cw linear
    }
}
