/**
 * BlockShapes.java — the single source of truth for every block shape's geometry (Group 08).
 *
 * One place defines each shape's box(es) in pixel coordinates (0..16); BOTH the live collision/
 * outline {@link VoxelShape} (read by SlotBlock) and the resource-pack model elements (written by
 * ServerPackGenerator) derive from the same boxes, so the shape you walk into always matches the
 * shape you see. "full" and "cross" are special: full → vanilla full cube + cube_all model; cross →
 * an X billboard (vanilla cross model, walk-through like a plant).
 *
 * Depends on: vanilla VoxelShapes only.
 * Called by:  SlotBlock (shapes), ServerPackGenerator (model elements), ShapeCommands (list/validate).
 */
package com.customblocks.block;

import net.minecraft.block.enums.BlockHalf;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockShapes {

    private BlockShapes() {} // static-only

    public static final String DEFAULT = "full";

    /** Shape name → one-line description, in the order shown by /cb shapelist + the editor. */
    private static final Map<String, String> SHAPES = new LinkedHashMap<>();
    static {
        SHAPES.put("full",        "Default full block (1×1×1)");
        SHAPES.put("slab_bottom", "Bottom half-slab");
        SHAPES.put("slab_top",    "Top half-slab");
        SHAPES.put("carpet",      "1/16-height ground layer");
        SHAPES.put("thin",        "Thin vertical panel (no frame)");
        SHAPES.put("pane",        "Thin crossed panels (pane/post)");
        SHAPES.put("wall",        "Wall post");
        SHAPES.put("pillar",      "Tall thin pillar");
        SHAPES.put("stairs",      "Stair shape");
        SHAPES.put("cross",       "X cross (like a flower)");
    }

    /** All shape names, in display order. */
    public static String[] names() { return SHAPES.keySet().toArray(new String[0]); }

    public static boolean isValid(String shape) { return shape != null && SHAPES.containsKey(shape); }

    public static String description(String shape) { return SHAPES.getOrDefault(shape, "Unknown shape"); }

    public static boolean isFull(String shape)  { return shape == null || "full".equals(shape); }
    public static boolean isCross(String shape) { return "cross".equals(shape); }

    /**
     * Box geometry (pixel coords 0..16) for a shape. Each row is {x1,y1,z1,x2,y2,z2}. Returns null
     * for "full" and "cross" (those use vanilla models, not generated elements).
     */
    public static int[][] boxes(String shape) {
        return switch (shape == null ? "full" : shape) {
            case "slab_bottom" -> new int[][]{{0, 0, 0, 16, 8, 16}};
            case "slab_top"    -> new int[][]{{0, 8, 0, 16, 16, 16}};
            case "carpet"      -> new int[][]{{0, 0, 0, 16, 1, 16}};
            case "thin"        -> new int[][]{{0, 0, 7, 16, 16, 9}};
            case "pane"        -> new int[][]{{0, 0, 7, 16, 16, 9}, {7, 0, 0, 9, 16, 16}};
            case "wall"        -> new int[][]{{4, 0, 4, 12, 16, 12}};
            case "pillar"      -> new int[][]{{6, 0, 6, 10, 16, 10}};
            case "stairs"      -> stairBoxes(StairShape.STRAIGHT); // straight = the base stair model
            default            -> null; // full / cross
        };
    }

    /** Per-name VoxelShape cache — blocks now use dynamicBounds (no built-in shape cache), so these are
     *  queried live every collision/outline check; build each shape's VoxelShape once and reuse it. */
    private static final Map<String, VoxelShape> OUTLINE_CACHE = new ConcurrentHashMap<>();

    /** The outline (selection) shape: full cube for full, a small box for cross, else the boxes. */
    public static VoxelShape outline(String shape) {
        return OUTLINE_CACHE.computeIfAbsent(shape == null ? "full" : shape, BlockShapes::buildOutline);
    }

    private static VoxelShape buildOutline(String shape) {
        if (isFull(shape)) return VoxelShapes.fullCube();
        if (isCross(shape)) return VoxelShapes.cuboid(2 / 16d, 0, 2 / 16d, 14 / 16d, 16 / 16d, 14 / 16d);
        return union(boxes(shape));
    }

    /** The collision shape: empty for cross (walk-through, like a plant), else same as the outline. */
    public static VoxelShape collision(String shape) {
        if (isCross(shape)) return VoxelShapes.empty();
        return outline(shape);
    }

    // ── G08 §J — directional placement (facing + top/bottom half) ────────────────────────────────
    // Only asymmetric shapes actually rotate with placement; every other shape looks identical from
    // any horizontal angle, so it ignores facing/half and keeps its base geometry (== the pre-§J
    // behavior, so §A's non-stairs hitboxes are byte-unchanged). The rotation applied here EXACTLY
    // mirrors the blockstate model rotation ServerPackGenerator emits (x:180 for a top half, then
    // y:90·k for the facing), so a placed block's collision always matches what the player sees.

    /** True for shapes whose placement facing/half rotate the geometry + model (only stairs today). */
    public static boolean isDirectional(String shape) { return "stairs".equals(shape); }

    /** Outline for a directional placement: base (NORTH/bottom) shape rotated to {@code facing}+{@code half}. */
    public static VoxelShape outline(String shape, Direction facing, BlockHalf half) {
        VoxelShape base = outline(shape);
        return isDirectional(shape) ? orient(base, facing, half) : base;
    }

    /** Collision for a directional placement (empty for cross; else the oriented outline). */
    public static VoxelShape collision(String shape, Direction facing, BlockHalf half) {
        if (isCross(shape)) return VoxelShapes.empty();
        return outline(shape, facing, half);
    }

    // ── G08 §J corners — per-shape base geometry (NORTH/bottom identity, rotated by orient) ──────
    // Each corner's boxes are authored in the mod's NORTH-facing/bottom frame, derived from the vanilla
    // stair/inner/outer models rotated into that frame (verified element-for-element against vanilla's
    // inner_stairs.json). orient() then rotates the union to the placed facing+half, so hitbox == the
    // baked corner model the client draws (same math). Top-layer (y 8..16) quadrants per shape:
    //   straight    NW+NE (north half)   inner_left NW+NE+SW   inner_right NW+NE+SE
    //   outer_left  NW                   outer_right NE
    public static int[][] stairBoxes(StairShape s) {
        int[] slab  = {0, 0, 0, 16, 8, 16};       // bottom half-slab (shared by every shape)
        int[] north = {0, 8, 0, 16, 16, 8};       // north-half top = the straight tall step
        return switch (s == null ? StairShape.STRAIGHT : s) {
            case STRAIGHT    -> new int[][]{ slab, north };
            case INNER_LEFT  -> new int[][]{ slab, north, {0, 8, 8,  8, 16, 16} }; // + SW quadrant
            case INNER_RIGHT -> new int[][]{ slab, north, {8, 8, 8, 16, 16, 16} }; // + SE quadrant
            case OUTER_LEFT  -> new int[][]{ slab, {0, 8, 0,  8, 16, 8} };         // NW quadrant only
            case OUTER_RIGHT -> new int[][]{ slab, {8, 8, 0, 16, 16, 8} };         // NE quadrant only
        };
    }

    /** Per-shape base VoxelShape cache (NORTH/bottom), built once; orient() rotates it per placement. */
    private static final Map<StairShape, VoxelShape> STAIR_BASE_CACHE = new ConcurrentHashMap<>();

    private static VoxelShape stairBase(StairShape s) {
        return STAIR_BASE_CACHE.computeIfAbsent(s == null ? StairShape.STRAIGHT : s, k -> union(stairBoxes(k)));
    }

    /** Outline (selection) for a stair placement + its corner shape, rotated to facing+half. */
    public static VoxelShape stairOutline(StairShape s, Direction facing, BlockHalf half) {
        return orient(stairBase(s), facing, half);
    }

    /** Collision for a stair placement — stairs are solid, so this is the same as the outline. */
    public static VoxelShape stairCollision(StairShape s, Direction facing, BlockHalf half) {
        return stairOutline(s, facing, half);
    }

    /** Apply the top-half flip then y:90·k (facing) to a base shape, so collision == visual.
     *  NORTH/bottom is identity (base shape unchanged).
     *
     *  J8 fix (2026-07-21): vanilla's stair base faces EAST (+x) — the one horizontal axis x:180 does NOT
     *  move — so vanilla can flip a top half with x:180 alone. This mod's base faces NORTH (−z), the axis
     *  x:180 DOES flip, so x:180 by itself also spun every upside-down stair 180° in yaw: wrong facing on
     *  placement, and corners forming on the wrong side of the turn. The top half therefore applies x:180
     *  PLUS y:180 (net: 180° about Z) — the exact meaning of vanilla's x:180 in a NORTH-identity frame.
     *  {@link #topExtraSteps} is the single definition of that "+y:180", shared by every orient path. */
    private static VoxelShape orient(VoxelShape base, Direction facing, BlockHalf half) {
        VoxelShape s = base;
        if (half == BlockHalf.TOP) s = rotX180(s);
        int k = (facingSteps(facing) + topExtraSteps(half)) & 3;
        for (int r = 0; r < k; r++) s = rotY90cw(s);
        return s;
    }

    /** Extra 90° CW Y-steps a TOP half adds on top of x:180 — see the J8 note on {@link #orient}. */
    public static int topExtraSteps(BlockHalf half) { return half == BlockHalf.TOP ? 2 : 0; }

    // ── Shared orient primitives (§J BlockEntity redesign) ───────────────────────────────────────
    // The client renderer (DirectionalSlotModel) rotates the baked stair MESH per placement to match
    // the hitbox. To guarantee the drawn geometry and the collision box can NEVER disagree, the mesh
    // uses the SAME coordinate formulas as the VoxelShape rotations above — point form of rotX180 /
    // rotY90cw — instead of re-deriving a matrix. Change one, both move together.

    /** Facing → count of 90° CW Y-rotations. NORTH=0, EAST=1, SOUTH=2, WEST=3 (vertical dirs → 0). */
    public static int facingSteps(Direction facing) {
        return switch (facing == null ? Direction.NORTH : facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0; // NORTH (and the never-used vertical dirs)
        };
    }

    /**
     * Rotate a unit-cube point (each coord in 0..1) by the SAME transform {@link #orient} applies to
     * collision — x:180 for a top half, then y:90·k for the facing — writing {x,y,z} into {@code out}
     * (length ≥ 3). Point form of rotX180 (x,1-y,1-z) and rotY90cw (1-z,y,x); both fix the block centre.
     */
    public static void orientPoint(float x, float y, float z, Direction facing, BlockHalf half, float[] out) {
        if (half == BlockHalf.TOP) { y = 1f - y; z = 1f - z; }        // rotX180: (x, 1-y, 1-z)
        int k = (facingSteps(facing) + topExtraSteps(half)) & 3;      // J8: a TOP half also turns y:180
        for (int r = 0; r < k; r++) { float nx = 1f - z; z = x; x = nx; } // rotY90cw: (1-z, y, x)
        out[0] = x; out[1] = y; out[2] = z;
    }

    /**
     * Rotate a face/normal {@link Direction} by the same orientation (linear part only — no centre
     * offset): x:180 negates Y,Z; y:90·k CW maps (dx,dy,dz)→(-dz,dy,dx). Used to keep the drawn quad's
     * cull/nominal face and normal correct after {@link #orientPoint} moves its vertices.
     */
    public static Direction orientDir(Direction d, Direction facing, BlockHalf half) {
        int dx = d.getOffsetX(), dy = d.getOffsetY(), dz = d.getOffsetZ();
        if (half == BlockHalf.TOP) { dy = -dy; dz = -dz; }            // rotX180 linear part
        int k = (facingSteps(facing) + topExtraSteps(half)) & 3;      // J8: a TOP half also turns y:180
        for (int r = 0; r < k; r++) { int nx = -dz; dz = dx; dx = nx; } // rotY90cw linear part
        return Direction.fromVector(dx, dy, dz);
    }

    /** 90° clockwise about Y viewed from above — matches vanilla blockstate {@code "y": 90}. */
    private static VoxelShape rotY90cw(VoxelShape shape) {
        VoxelShape[] acc = { VoxelShapes.empty() };
        shape.forEachBox((x1, y1, z1, x2, y2, z2) ->
                acc[0] = VoxelShapes.union(acc[0], VoxelShapes.cuboid(1 - z2, y1, x1, 1 - z1, y2, x2)));
        return acc[0];
    }

    /** 180° about X (upside-down) — matches vanilla blockstate {@code "x": 180}. */
    private static VoxelShape rotX180(VoxelShape shape) {
        VoxelShape[] acc = { VoxelShapes.empty() };
        shape.forEachBox((x1, y1, z1, x2, y2, z2) ->
                acc[0] = VoxelShapes.union(acc[0], VoxelShapes.cuboid(x1, 1 - y2, 1 - z2, x2, 1 - y1, 1 - z1)));
        return acc[0];
    }

    private static VoxelShape union(int[][] boxes) {
        if (boxes == null || boxes.length == 0) return VoxelShapes.fullCube();
        VoxelShape out = cuboid(boxes[0]);
        for (int i = 1; i < boxes.length; i++) out = VoxelShapes.union(out, cuboid(boxes[i]));
        return out;
    }

    private static VoxelShape cuboid(int[] b) {
        return VoxelShapes.cuboid(b[0] / 16d, b[1] / 16d, b[2] / 16d, b[3] / 16d, b[4] / 16d, b[5] / 16d);
    }
}
