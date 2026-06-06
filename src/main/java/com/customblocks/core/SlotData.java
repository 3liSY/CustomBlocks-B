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
                       String category) {

    /** Vanilla stone hardness — the default break resistance for a new block. */
    public static final float DEFAULT_HARDNESS = 1.5f;
    /** Default block sound group key (see SlotBlock.getSoundGroup). */
    public static final String DEFAULT_SOUND = "stone";
    /** Default category — empty means "uncategorized". */
    public static final String DEFAULT_CATEGORY = "";

    /** Back-compat: a new block starts with no glow, default hardness/sound, solid, uncategorized. */
    public SlotData(int index, String customId, String displayName) {
        this(index, customId, displayName, 0, DEFAULT_HARDNESS, DEFAULT_SOUND, false, DEFAULT_CATEGORY);
    }

    /** Back-compat: glow specified, default hardness + sound, solid, uncategorized. */
    public SlotData(int index, String customId, String displayName, int glow) {
        this(index, customId, displayName, glow, DEFAULT_HARDNESS, DEFAULT_SOUND, false, DEFAULT_CATEGORY);
    }

    /** The registry slot key for this index, e.g. "slot_42". */
    public String slotKey() {
        return "slot_" + index;
    }

    /** Return a copy with a new id + display name (the canonical .update() builder). */
    public SlotData update(String newCustomId, String newDisplayName) {
        return new SlotData(index, newCustomId, newDisplayName, glow, hardness, soundType, noCollision, category);
    }

    public SlotData withCustomId(String newCustomId) {
        return new SlotData(index, newCustomId, displayName, glow, hardness, soundType, noCollision, category);
    }

    public SlotData withDisplayName(String newDisplayName) {
        return new SlotData(index, customId, newDisplayName, glow, hardness, soundType, noCollision, category);
    }

    /** Return a copy with a new light level, clamped to the valid 0..15 range. */
    public SlotData withGlow(int newGlow) {
        return new SlotData(index, customId, displayName, Math.max(0, Math.min(15, newGlow)), hardness, soundType, noCollision, category);
    }

    /** Return a copy with a new break hardness (negative = unbreakable, 0 = instant break). */
    public SlotData withHardness(float newHardness) {
        return new SlotData(index, customId, displayName, glow, newHardness, soundType, noCollision, category);
    }

    /** Return a copy with a new sound group key (see SlotBlock.getSoundGroup). */
    public SlotData withSoundType(String newSoundType) {
        return new SlotData(index, customId, displayName, glow, hardness, newSoundType, noCollision, category);
    }

    /** Return a copy with collision toggled (true = passable/walk-through, false = solid). */
    public SlotData withNoCollision(boolean newNoCollision) {
        return new SlotData(index, customId, displayName, glow, hardness, soundType, newNoCollision, category);
    }

    /** Return a copy in a new category ("" = uncategorized). */
    public SlotData withCategory(String newCategory) {
        return new SlotData(index, customId, displayName, glow, hardness, soundType, noCollision,
                newCategory == null ? DEFAULT_CATEGORY : newCategory);
    }
}
