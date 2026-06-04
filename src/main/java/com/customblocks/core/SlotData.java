/**
 * SlotData.java
 *
 * Responsibility: Immutable snapshot of one slot's state (design rule #1, FR-01-3).
 * Implemented as a record so it is immutable by construction. Every "change" returns
 * a NEW SlotData via update()/withX() — never mutate in place.
 *
 * Fields grow per-phase (Bible §7). Phase 1 has the identity fields only; textures,
 * glow, hardness, sounds, shapes, category, etc. are added in their phases.
 */
package com.customblocks.core;

public record SlotData(int index, String customId, String displayName) {

    /** The registry slot key for this index, e.g. "slot_42". */
    public String slotKey() {
        return "slot_" + index;
    }

    /** Return a copy with a new id + display name (the canonical .update() builder). */
    public SlotData update(String newCustomId, String newDisplayName) {
        return new SlotData(index, newCustomId, newDisplayName);
    }

    public SlotData withCustomId(String newCustomId) {
        return new SlotData(index, newCustomId, displayName);
    }

    public SlotData withDisplayName(String newDisplayName) {
        return new SlotData(index, customId, newDisplayName);
    }
}
