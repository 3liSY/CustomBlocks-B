/**
 * ShapeState.java — Group 08 §B.
 *
 * The block-state enum that lets a placed {@link SlotBlock} select its pre-baked shape MODEL
 * variant natively, so /cb setshape changes the visible model through an ordinary block-state
 * update instead of pushing a resource-pack reload. One value per shape in {@link BlockShapes}.
 *
 * IMPORTANT: these values must stay in lock-step with {@link BlockShapes#names()} — the pack's
 * per-slot blockstate JSON keys a variant on each {@code shape=<asString()>}. {@link #fromName}
 * maps a stored shape string back; anything unknown falls back to {@link #FULL} (safe default).
 *
 * NOTE: shape stays the source-of-truth on {@code SlotData} — collision, outline, and persistence
 * still read it there (unchanged). This property ONLY drives which baked model renders, so a bad
 * variant can never affect hitboxes or saved data.
 */
package com.customblocks.block;

import net.minecraft.util.StringIdentifiable;

public enum ShapeState implements StringIdentifiable {
    FULL("full"),
    SLAB_BOTTOM("slab_bottom"),
    SLAB_TOP("slab_top"),
    CARPET("carpet"),
    THIN("thin"),
    PANE("pane"),
    WALL("wall"),
    PILLAR("pillar"),
    STAIRS("stairs"),
    CROSS("cross");

    private final String id;

    ShapeState(String id) { this.id = id; }

    @Override
    public String asString() { return id; }

    /** Map a stored shape name (SlotData) to its state value; unknown/null → FULL. */
    public static ShapeState fromName(String shape) {
        if (shape != null) {
            for (ShapeState v : values()) if (v.id.equals(shape)) return v;
        }
        return FULL;
    }
}
