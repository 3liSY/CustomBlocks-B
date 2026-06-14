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

import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.LinkedHashMap;
import java.util.Map;

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
            case "stairs"      -> new int[][]{{0, 0, 0, 16, 8, 16}, {0, 8, 8, 16, 16, 16}};
            default            -> null; // full / cross
        };
    }

    /** The outline (selection) shape: full cube for full, a small box for cross, else the boxes. */
    public static VoxelShape outline(String shape) {
        if (isFull(shape)) return VoxelShapes.fullCube();
        if (isCross(shape)) return VoxelShapes.cuboid(2 / 16d, 0, 2 / 16d, 14 / 16d, 16 / 16d, 14 / 16d);
        return union(boxes(shape));
    }

    /** The collision shape: empty for cross (walk-through, like a plant), else same as the outline. */
    public static VoxelShape collision(String shape) {
        if (isCross(shape)) return VoxelShapes.empty();
        return outline(shape);
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
