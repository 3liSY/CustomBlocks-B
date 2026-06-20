/**
 * SlotData.java
 *
 * Responsibility: Immutable snapshot of one slot's state (design rule #1, FR-01-3).
 * Implemented as a record so it is immutable by construction. Every "change" returns
 * a NEW SlotData via update()/withX() — never mutate in place.
 *
 * Fields grow per-phase (Bible §7). Phase 1 had the identity fields only; glow, hardness,
 * sound, and noCollision are Phase 6; category is Phase 8. Shapes etc. arrive in their phases.
 */
package com.customblocks.core;

public record SlotData(int index, String customId, String displayName,
                       int glow, float hardness, String soundType, boolean noCollision,
                       String category, String shape, AnimData anim) {

    /** Vanilla stone hardness — the default break resistance for a new block. */
    public static final float DEFAULT_HARDNESS = 1.5f;
    /** Default block sound group key (see SlotBlock.getSoundGroup). */
    public static final String DEFAULT_SOUND = "stone";
    /** Default category — empty means "uncategorized". */
    public static final String DEFAULT_CATEGORY = "";
    /** Default block shape — a full 1×1×1 cube (see BlockShapes). */
    public static final String DEFAULT_SHAPE = "full";

    /** Normalize a null/blank shape and a null anim so callers never see null. */
    public SlotData {
        shape = (shape == null || shape.isBlank()) ? DEFAULT_SHAPE : shape;
        anim = anim == null ? AnimData.NONE : anim;
    }

    /** Back-compat (pre-anim): same fields as before, no animation. */
    public SlotData(int index, String customId, String displayName,
                    int glow, float hardness, String soundType, boolean noCollision,
                    String category, String shape) {
        this(index, customId, displayName, glow, hardness, soundType, noCollision, category, shape, AnimData.NONE);
    }

    /** Back-compat (pre-shape): same fields as before, shape defaults to full. */
    public SlotData(int index, String customId, String displayName,
                    int glow, float hardness, String soundType, boolean noCollision, String category) {
        this(index, customId, displayName, glow, hardness, soundType, noCollision, category, DEFAULT_SHAPE, AnimData.NONE);
    }

    /** Back-compat: a new block starts with no glow, default hardness/sound, solid, uncategorized, full. */
    public SlotData(int index, String customId, String displayName) {
        this(index, customId, displayName, 0, DEFAULT_HARDNESS, DEFAULT_SOUND, false, DEFAULT_CATEGORY, DEFAULT_SHAPE, AnimData.NONE);
    }

    /** Back-compat: glow specified, default hardness + sound, solid, uncategorized, full. */
    public SlotData(int index, String customId, String displayName, int glow) {
        this(index, customId, displayName, glow, DEFAULT_HARDNESS, DEFAULT_SOUND, false, DEFAULT_CATEGORY, DEFAULT_SHAPE, AnimData.NONE);
    }

    /** True when this slot is an animated block (a multi-frame strip with a sidecar .mcmeta). */
    public boolean isAnimated() {
        return anim != null && anim.isAnimated();
    }

    /** The registry slot key for this index, e.g. "slot_42". */
    public String slotKey() {
        return "slot_" + index;
    }

    /** Return a copy with a new id + display name (the canonical .update() builder). */
    public SlotData update(String newCustomId, String newDisplayName) {
        return new SlotData(index, newCustomId, newDisplayName, glow, hardness, soundType, noCollision, category, shape, anim);
    }

    public SlotData withCustomId(String newCustomId) {
        return new SlotData(index, newCustomId, displayName, glow, hardness, soundType, noCollision, category, shape, anim);
    }

    public SlotData withDisplayName(String newDisplayName) {
        return new SlotData(index, customId, newDisplayName, glow, hardness, soundType, noCollision, category, shape, anim);
    }

    /** Return a copy with a new light level, clamped to the valid 0..15 range. */
    public SlotData withGlow(int newGlow) {
        return new SlotData(index, customId, displayName, Math.max(0, Math.min(15, newGlow)), hardness, soundType, noCollision, category, shape, anim);
    }

    /** Return a copy with a new break hardness (negative = unbreakable, 0 = instant break). */
    public SlotData withHardness(float newHardness) {
        return new SlotData(index, customId, displayName, glow, newHardness, soundType, noCollision, category, shape, anim);
    }

    /** Return a copy with a new sound group key (see SlotBlock.getSoundGroup). */
    public SlotData withSoundType(String newSoundType) {
        return new SlotData(index, customId, displayName, glow, hardness, newSoundType, noCollision, category, shape, anim);
    }

    /** Return a copy with collision toggled (true = passable/walk-through, false = solid). */
    public SlotData withNoCollision(boolean newNoCollision) {
        return new SlotData(index, customId, displayName, glow, hardness, soundType, newNoCollision, category, shape, anim);
    }

    /** Return a copy in a new category ("" = uncategorized). */
    public SlotData withCategory(String newCategory) {
        return new SlotData(index, customId, displayName, glow, hardness, soundType, noCollision,
                newCategory == null ? DEFAULT_CATEGORY : newCategory, shape, anim);
    }

    /** Return a copy with a new shape (null/blank → full; see BlockShapes for valid names). */
    public SlotData withShape(String newShape) {
        return new SlotData(index, customId, displayName, glow, hardness, soundType, noCollision, category,
                newShape == null || newShape.isBlank() ? DEFAULT_SHAPE : newShape, anim);
    }

    /** Return a copy with new animation state (AnimData.NONE = make it a plain static block). */
    public SlotData withAnim(AnimData newAnim) {
        return new SlotData(index, customId, displayName, glow, hardness, soundType, noCollision, category, shape,
                newAnim == null ? AnimData.NONE : newAnim);
    }
}
