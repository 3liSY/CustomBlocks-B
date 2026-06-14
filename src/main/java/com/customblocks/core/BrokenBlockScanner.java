/**
 * BrokenBlockScanner.java — Group 09, Slice 5 (broken-block detection). READ-ONLY.
 *
 * Scans every assigned block and flags the ones that would render as the missing-texture (purple/black)
 * block: a SlotData exists but there is no baked texture file on disk ({@code !TextureStore.has(index)}).
 * For each, it records whether a saved SOURCE image exists — if so the block can be auto-fixed by
 * re-baking from that source (no network); if not, only a fresh /cb retexture can fix it.
 *
 * This class never mutates anything — it only reads SlotManager + TextureStore. The fix itself lives in
 * SafetyCommands. (World-scan for placed instances of missing blocks is intentionally NOT done here — it
 * would be an expensive full-world sweep; the registry/texture mismatch is the cheap, reliable signal.)
 *
 * Depends on: SlotManager, SlotData, TextureStore.
 * Called by:  SafetyCommands (/cb safety, /cb showbrokenblocks), BrokenBlocksMenu.
 */
package com.customblocks.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BrokenBlockScanner {

    private BrokenBlockScanner() {} // static-only

    /** One block with no baked texture. {@code hasSource} = a saved image exists, so it can be re-baked. */
    public record Broken(String customId, int index, boolean hasSource) {}

    /** All assigned blocks whose baked texture is missing, sorted by id. */
    public static List<Broken> scan() {
        List<Broken> out = new ArrayList<>();
        for (SlotData d : SlotManager.assignedSlots()) {
            if (!TextureStore.has(d.index())) {
                out.add(new Broken(d.customId(), d.index(), TextureStore.hasSource(d.index())));
            }
        }
        out.sort(Comparator.comparing(Broken::customId));
        return out;
    }

    /** How many assigned blocks are missing their baked texture. */
    public static int count() {
        int n = 0;
        for (SlotData d : SlotManager.assignedSlots()) {
            if (!TextureStore.has(d.index())) n++;
        }
        return n;
    }
}
