/**
 * SlotPools.java — the slot-pool free-index policy (moved out of SlotManager so that class stays
 * under the 500-line gate).
 *
 * G13-25 checkpoint 1 note (2026-07-03 correction): CP1 originally split the pool at a FIXED
 * compile-time boundary — indices 0..799 plain SlotBlock, 800.. ArabicSlotBlock — on the assumption
 * that normal custom blocks only ever occupy 0..799. That assumption was WRONG for a real world:
 * the owner's server runs maxSlots=2000 with ~1100 blocks already assigned, so live normal blocks
 * sit well past index 800. The fixed boundary (a) re-classed those live blocks as ArabicSlotBlock
 * (extra FACING state) and (b) capped new-block allocation at 800, so `/cb create` failed with
 * "every slot is in use" while ~900 slots were free. Both were regressions.
 *
 * The redesign is LOCKED as data-only (G13-25, 2026-07-03): there is no per-index class boundary
 * at all. Every index registers as a plain SlotBlock and the allocator scans the full pool —
 * byte-for-byte the pre-CP1 behaviour. An Arabic letter/number is a normal slot flagged by
 * ArabicMeta on its SlotData (allocated from this same pool by ArabicSlotBootstrap), so it can
 * never occupy an index differently from a normal block, on any world, client or server.
 *
 * Depends on: SlotBlock, DeletedSlots, RetiredSlots
 * Called by:  SlotManager (registerAll + create), CustomBlocksMod (boot maxSlots floor)
 */
package com.customblocks.core;

import com.customblocks.block.SlotBlock;
import net.minecraft.block.AbstractBlock;

import java.util.function.IntPredicate;

public final class SlotPools {

    private SlotPools() {} // static-only

    /** Pool size the mod ensures at boot (raise-only floor for older config files). Kept at 1448 so a
     *  fresh world has ample headroom; a hand-set higher value (e.g. the owner's 2000) is left alone. */
    public static final int REQUIRED_MAX_SLOTS = 1448;

    /** The block for one index. Every index is a plain SlotBlock — the G13-25 data-only design has
     *  no per-index class boundary (see the file header). */
    public static SlotBlock blockFor(int index, AbstractBlock.Settings settings) {
        return new SlotBlock(index, settings);
    }

    /**
     * Next free index a new block may claim, or -1 when the pool is full. Policy (G06-2/G06-3):
     * prefer a pristine, never-assigned index; a DELETED index is permanently retired — never reused
     * (its placements become the shared marker); a RETIRED index is air-cleaned, so it stays a
     * last-resort fallback. Scans the FULL pool 0..maxSlots-1.
     */
    public static int nextFreeNormalIndex(int maxSlots, IntPredicate assigned) {
        int retiredFallback = -1;
        for (int i = 0; i < maxSlots; i++) {
            if (assigned.test(i)) continue;
            if (DeletedSlots.contains(i)) continue; // permanently retired by a delete — never reuse
            if (RetiredSlots.contains(i)) { if (retiredFallback < 0) retiredFallback = i; continue; }
            return i;
        }
        if (retiredFallback >= 0) { RetiredSlots.remove(retiredFallback); return retiredFallback; }
        return -1;
    }

    /**
     * How many indices {@link #nextFreeNormalIndex} could still hand out — pristine ones plus the
     * retired last-resort fallbacks, minus every permanently-retired DELETED index.
     *
     * Lives here, beside the allocation policy it mirrors, so the two can never drift: a caller that
     * needed "slots left" would otherwise have to re-implement the DeletedSlots/RetiredSlots rules and
     * quietly disagree with the allocator. Read-only — claims nothing and un-retires nothing.
     *
     * Called by: Group 12 folder import (it must state slots-used and slots-left BEFORE it commits).
     */
    public static int freeCount(int maxSlots, IntPredicate assigned) {
        int free = 0;
        for (int i = 0; i < maxSlots; i++) {
            if (assigned.test(i)) continue;
            if (DeletedSlots.contains(i)) continue; // never reusable
            free++;                                 // pristine, or a retired fallback
        }
        return free;
    }
}
