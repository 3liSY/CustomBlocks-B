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
 * Depends on: SlotManager, TextureStore, BlockNotesManager, ResourcePackServer, UndoManager,
 *             HudSync, MarkerResolver, DeletedPlacementSweeper, SlotData
 * Called by:  DeleteCommands.deleteCore, SlotBlock.cbDelete (the Deleter),
 *             BulkCommands.applyDelete + BrokenConfirmMenu.doDeleteSelected (batch, via deleteCore)
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
}
