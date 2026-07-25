/**
 * DeletionService.java — G06-14 slice 2.
 *
 * The ONE shared delete rail. Every delete path — /cb delete, the red Deleter, bulk delete and the
 * broken-blocks GUI cleanup (slice 3) — funnels through here so deletion behaves identically
 * everywhere: no more drifted paths leaving purple blocks (the old bug). Single deletes call
 * {@link #delete}; batch deletes loop {@link #deleteCore} then do the shared steps once.
 *
 * What one delete does:
 *   1. snapshot the texture (before SlotManager.delete frees it) so /cb undo can restore the pixels,
 *   2. remove the slot definition (SlotManager.delete — also captures it into the Trash),
 *   3. clean up an orphaned note, rebuild the pack, record the undo step,
 *   4. resync every client's slot cache so the identity clears live, not on rejoin (G05-1),
 *   5. remember the index's id/name (MarkerResolver) and turn every placed copy into a
 *      "Deleted: <name>" marker via DeletedPlacementSweeper — instant for loaded chunks, on
 *      chunk-load for far ones. This REPLACES the old shared (Removed) swap.
 *
 * Callers do their own lock check + chat message first (the wording differs per path).
 *
 * {@link #healSourceFromTrash} is the same rail's undo half: it puts a deleted block's ORIGINAL picture
 * back when /cb undo revives it, since the undo stack only ever carried the baked pixels (G06 §D2).
 *
 * Depends on: SlotManager, TextureStore, TrashManager, BlockNotesManager, ResourcePackServer, UndoManager,
 *             HudSync, MarkerResolver, DeletedPlacementSweeper, SlotData
 * Called by:  DeleteCommands.deleteCore, SlotBlock.cbDelete (the Deleter),
 *             BulkCommands.applyDelete + BrokenConfirmMenu.doDeleteSelected (batch, via deleteCore),
 *             HistoryCommands (healSourceFromTrash, on undo of a delete)
 */
package com.customblocks.core;

import com.customblocks.block.DeletedPlacementSweeper;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public final class DeletionService {

    private DeletionService() {} // static-only

    /**
     * Delete one custom block (definition + placed copies). Caller must have already confirmed the
     * block isn't locked. {@code actor} is the undo owner (null for console → not undoable).
     */
    public static void delete(MinecraftServer server, SlotData before, UUID actor) {
        if (server == null || before == null) return;
        byte[] texture = deleteCore(server, before);      // the per-block work (+ live marker sweep)
        ResourcePackServer.updatePack();                  // free the slot's texture from the pack
        UndoManager.recordDelete(actor, before, texture); // one undo step
        HudSync.broadcast(server);                        // clear the deleted identity live (G05-1)
    }

    /**
     * One block's deletion CORE — everything EXCEPT the batch-shared steps (pack rebuild, undo
     * record, client resync), which the caller does ONCE for the whole batch. Returns the texture
     * snapshot so the caller can build its undo Op. Caller must have lock-checked already.
     *
     * Used by {@link #delete} (single) and by the bulk + broken-cleanup batch deletes (slice 3),
     * so every path frees the slot AND swaps placed copies to "Deleted: &lt;name&gt;" markers —
     * no more purple blocks left in the world.
     */
    public static byte[] deleteCore(MinecraftServer server, SlotData before) {
        if (server == null || before == null) return null;
        String id = before.customId();
        byte[] texture = TextureStore.load(before.index()); // snapshot BEFORE delete frees it
        SlotManager.delete(id);
        BlockNotesManager.onBlockDeleted(id); // drop an orphaned note if any
        // Remember the name so the sweep can label the markers, then swap every placed copy to a
        // "Deleted: <name>" marker (loaded chunks now, far ones when their chunk loads).
        MarkerResolver.put(before.index(), id, before.displayName());
        DeletedPlacementSweeper.onDeleted(server, before.index());
        return texture;
    }

    /**
     * G06 §D2 — the inverse of what a delete does to the SOURCE files: put an undone delete's original
     * picture (and the link it came from) back on disk.
     *
     * <p>A delete erases {@code slot_N.src} / {@code slot_N.url}, but {@link #deleteCore} copies both into
     * the trash first, so the trash entry is the only surviving record of them. The undo stack carries the
     * baked pixels alone — which is why an undone delete used to come back looking perfect while
     * {@code /cb setbg}, {@code /cb resize} and {@code /cb variants} all refused it with "no stored picture
     * to re-bake". Only fills gaps: an existing source is never overwritten. Silent no-op when the block
     * genuinely had no source (Arabic glyphs, video frames) or the best-effort trash capture failed.
     */
    public static void healSourceFromTrash(SlotData before) {
        if (before == null) return;
        if (!TextureStore.hasSource(before.index())) {
            byte[] source = TrashManager.sourceFor(before.customId(), before.index());
            if (source != null) TextureStore.saveSource(before.index(), source);
        }
        if (TextureStore.loadUrl(before.index()) == null) {
            String url = TrashManager.urlFor(before.customId(), before.index());
            if (url != null) TextureStore.saveUrl(before.index(), url);
        }
    }
}
