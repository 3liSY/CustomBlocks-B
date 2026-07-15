/**
 * ArabicClientView.java — Group 13 / G13-25 CP3b. CLIENT-ONLY.
 *
 * Responsibility: the client-side metadata adapter behind ArabicSlotJoinFlow.CLIENT_VIEW. On a
 * REMOTE session (dedicated server / LAN guest) this JVM's SlotManager holds its OWN stale
 * slots.json (the G05 lesson), so the join-flow prediction reads the server-synced
 * ClientSlotCache instead — the "ar" tuple ("glyph/form/colour") HudSync ships per Arabic slot.
 * Singleplayer / LAN host never consult this view (their in-process SlotManager is live truth).
 *
 * Also hosts the render-side letter test (isLetterSlot) AnimSlotBER uses to keep Arabic LETTERS
 * full-bright even before their facing arrives — the old join system was always full-bright, and
 * the world-lit fallback read as "dim/grey letters" (MP finding 2026-07-03).
 *
 * Depends on: ClientSlotCache ("ar" field), SlotManager (SP/LAN-host path), ArabicGlyphs
 *             (letter-vs-number test), ArabicSlotJoinFlow.ClientView (the seam it implements)
 * Called by:  CustomBlocksClient (install), ArabicSlotJoinFlow (via CLIENT_VIEW), AnimSlotBER
 */
package com.customblocks.client;

import com.customblocks.arabic.ArabicGlyphs;
import com.customblocks.arabic.ArabicSlotJoinFlow;
import com.customblocks.block.SlotBlock;
import com.customblocks.core.ArabicMeta;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class ArabicClientView implements ArabicSlotJoinFlow.ClientView {

    private ArabicClientView() {} // install() only

    /** Install the seam (client entrypoint, once). */
    public static void install() {
        ArabicSlotJoinFlow.CLIENT_VIEW = new ArabicClientView();
    }

    @Override
    @Nullable
    public ArabicMeta arabicMeta(int slotIndex) {
        return parse(cacheTuple(slotIndex));
    }

    @Override
    @Nullable
    public Integer indexForId(String customId) {
        return ClientSlotCache.indexForId(customId);
    }

    @Override
    public int glowFor(int slotIndex) {
        ClientSlotCache.Entry e = ClientSlotCache.getEntry(slotIndex);
        return (e == null) ? 0 : e.glow();
    }

    /** True when the slot is an Arabic LETTER (numbers excluded — they have no glyph char).
     *  Session-aware: remote reads the synced cache, SP / LAN host the in-process SlotManager. */
    public static boolean isLetterSlot(int slotIndex) {
        ArabicMeta meta;
        if (SlotBlock.CLIENT_REMOTE_SESSION) {
            meta = parse(cacheTuple(slotIndex));
        } else {
            SlotBlock b = SlotManager.blockAt(slotIndex);
            SlotData d = (b == null) ? null : SlotManager.getBySlot(b.getSlotKey());
            meta = (d == null || !d.isArabic()) ? null : d.arabic();
        }
        return meta != null && ArabicGlyphs.charForName(meta.glyphId()).isPresent();
    }

    /** The synced "ar" tuple for a slot, or null (normal block / cache miss). */
    @Nullable
    private static String cacheTuple(int slotIndex) {
        ClientSlotCache.Entry e = ClientSlotCache.getEntry(slotIndex);
        String ar = (e == null) ? null : e.arabic();
        return (ar == null || ar.isEmpty()) ? null : ar;
    }

    /** "glyph/form/colour" → ArabicMeta, or null on an absent/corrupt tuple. */
    @Nullable
    private static ArabicMeta parse(@Nullable String tuple) {
        if (tuple == null) return null;
        String[] p = tuple.split("/", 3);
        if (p.length < 3) return null;
        int form;
        try {
            form = Integer.parseInt(p[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        return new ArabicMeta(p[0], form, p[2]);
    }
}
