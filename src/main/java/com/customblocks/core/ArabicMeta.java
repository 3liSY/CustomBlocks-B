/**
 * ArabicMeta.java — Group 13 / G13-25 checkpoint 1: the optional Arabic identity of one slot.
 *
 * Responsibility: Immutable record saying WHICH letter/number a slot is. A slot whose SlotData
 * carries a non-null ArabicMeta IS an auto-join letter or number block:
 *   glyphId  — the glyph's art base: a letter's ArabicGlyphs.artBase (e.g. "jeem") or a
 *              number's ArabicArt idBase (e.g. "a0" / "e0").
 *   form     — the baked contextual form, ArabicJoining.ISOLATED..FINAL (0..3). Numbers never
 *              grow connecting bars, so theirs is always ISOLATED.
 *   colorKey — the bundled colour name ("black"/"red"/"green"/"yellow") or a custom-hex key.
 * null ArabicMeta = a normal custom block (the default for every non-Arabic slot).
 *
 * Sibling form-slots are NOT stored here (drift risk if one is deleted/recreated independently,
 * spec §6.1) — they are found on demand by scanning SlotManager.assignedSlots() for entries whose
 * ArabicMeta shares the same glyphId + colorKey.
 *
 * Data-only design (locked after the reverted CP1 partition): there is NO ArabicSlotBlock
 * subclass and NO FACING blockstate — an Arabic slot is a plain SlotBlock whose SlotData carries
 * this record. Orientation lives in the shared slot BlockEntity, not the blockstate.
 *
 * Depends on: (none — pure data; the form constants live in arabic/ArabicJoining)
 * Called by:  SlotData (optional field), SlotDataStore (persistence), ArabicSlotBootstrap
 *             (creation), and the G13-25 join/sibling services (later checkpoints)
 */
package com.customblocks.core;

public record ArabicMeta(String glyphId, int form, String colorKey) {

    /** Normalize so lookups never see null/blank parts (form clamped to the 4 ADR-005 forms). */
    public ArabicMeta {
        glyphId = glyphId == null ? "" : glyphId;
        form = Math.max(0, Math.min(3, form));
        colorKey = (colorKey == null || colorKey.isBlank()) ? "black" : colorKey;
    }

    /** True when this meta actually names a glyph (guards a corrupt/blank on-disk entry). */
    public boolean isValid() {
        return !glyphId.isBlank();
    }
}
