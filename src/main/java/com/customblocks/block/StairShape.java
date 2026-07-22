/**
 * StairShape.java — Group 08 §J (corner connection).
 *
 * The vanilla stair "shape" state, carried data-only on {@link AnimSlotBlockEntity} (never a block-state
 * — the 2026-07-20 OOM revert). A stair recomputes this from its neighbours whenever an adjacent block
 * changes ({@link StairConnection}) so two stairs meeting at an L auto-form a corner with no gap, exactly
 * like vanilla:
 *   STRAIGHT     — the plain stair (tall step on the facing side).
 *   INNER_LEFT / INNER_RIGHT — ¾-top L (wraps INTO the corner), formed with a perpendicular stair behind.
 *   OUTER_LEFT / OUTER_RIGHT — ¼-top corner (wraps OUT of the corner), formed with a perpendicular stair in front.
 *
 * "left"/"right" follow the vanilla rule: the connecting neighbour faces {@code facing.rotateYCounterclockwise()}
 * → LEFT, else RIGHT. The base geometry for each shape ({@link BlockShapes#stairBoxes}) is authored in the
 * mod's NORTH/bottom identity frame; the same facing/half {@link BlockShapes#orient} rotates it to placement.
 *
 * The pack bakes one model per corner ({@code slot_N} + {@code slot_N<modelSuffix>}); the client picks the
 * baked model for the current shape and rotates it (DirectionalSlotModel), so hitbox == visual for corners too.
 *
 * Depends on: nothing (pure enum).
 * Called by:  BlockShapes (geometry), StairConnection (compute), AnimSlotBlockEntity (storage),
 *             FaceModelBuilder / ServerPackGenerator (model emit), DirectionalSlotModel (render pick).
 */
package com.customblocks.block;

public enum StairShape {
    STRAIGHT(""),
    INNER_LEFT("_inner_left"),
    INNER_RIGHT("_inner_right"),
    OUTER_LEFT("_outer_left"),
    OUTER_RIGHT("_outer_right");

    private final String modelSuffix;

    StairShape(String modelSuffix) { this.modelSuffix = modelSuffix; }

    /** "" for straight, else "_inner_left" etc. — appended to a slot's block-model key for its corner model. */
    public String modelSuffix() { return modelSuffix; }

    public boolean isStraight() { return this == STRAIGHT; }

    /** The 4 corner shapes only (STRAIGHT is the base model {@code slot_N}, not a suffixed corner model). */
    public static final StairShape[] CORNERS = { INNER_LEFT, INNER_RIGHT, OUTER_LEFT, OUTER_RIGHT };

    /** Parse a stored name back to a shape (NBT), defaulting to STRAIGHT for null/unknown. */
    public static StairShape byName(String name) {
        if (name == null) return STRAIGHT;
        try { return valueOf(name); } catch (IllegalArgumentException e) { return STRAIGHT; }
    }
}
