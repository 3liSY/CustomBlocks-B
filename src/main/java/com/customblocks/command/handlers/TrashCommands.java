/**
 * TrashCommands.java — Group 09, Slice 4 (deleted-block trash browser).
 *
 *   /cb deletedblocks  (alias /cb trash) — open the trash browser GUI for a player; console prints a count.
 *
 * Restore is orchestrated here (the GUI's per-entry "Restore" button calls guiRestore): it recreates the
 * block through the SAME tested SlotManager.create + setters that /cb create / dupe use, writes the saved
 * texture + source back, removes the trash entry, then rebuilds + pushes the pack. Pin / delete-permanently
 * are pure data ops the GUI does directly against TrashManager.
 *
 * Depends on: TrashManager, SlotManager, SlotData, TextureStore, ResourcePackServer, GuiRouter, Nav, Chat.
 * Called by:  CommandRegistrar; TrashEntryMenu (guiRestore).
 */
package com.customblocks.command.handlers;

import com.customblocks.block.DeletedPlacementSweeper;
import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.DeletedSlots;
import com.customblocks.core.MarkerResolver;
import com.customblocks.core.MarkerTombstones;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.TrashManager;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class TrashCommands {

    private TrashCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("deletedblocks").executes(TrashCommands::open));
        root.then(CommandManager.literal("trash").executes(TrashCommands::open)); // friendly alias
    }

    private static int open(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, MenuKey.of(Dest.TRASH_LIST));
            return 1;
        }
        Chat.info(src, "Deleted blocks in the trash: " + TrashManager.list().size()
                + " (open in-game with /cb deletedblocks to restore them).");
        return 1;
    }

    /** Restore a trashed block from the GUI (recreate it, re-apply texture, rebuild the pack). */
    public static void guiRestore(ServerPlayerEntity player, String entryId) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        server.execute(() -> doRestore(player, entryId));
    }

    private static void doRestore(ServerPlayerEntity player, String entryId) {
        ServerCommandSource src = player.getCommandSource();
        MinecraftServer server = player.getServer(); // non-null: guiRestore guards it before scheduling
        TrashManager.TrashEntry e = TrashManager.get(entryId);
        if (e == null) {
            Chat.error(src, "That deleted block is no longer in the trash.");
            return;
        }
        if (SlotManager.hasId(e.customId())) {
            Chat.error(src, "A block named \"" + e.customId() + "\" already exists. "
                    + "Rename or delete that one first, then restore.");
            return;
        }

        // G06-14 slice 4: reuse the block's ORIGINAL, still-reserved slot index when we can — the same
        // thing /cb undo of a delete does (restoreSnapshot). That way far placed copies still wearing
        // slot_N revert to the real block automatically, no slot number leaks, and no fresh index is
        // burned. Fall back to a fresh slot for legacy entries (slotIndex -1) or if the old index was
        // somehow re-used.
        int slot = e.slotIndex();
        boolean reuse = slot >= 0 && DeletedSlots.contains(slot) && SlotManager.getBySlot("slot_" + slot) == null;
        int index;
        if (reuse) {
            SlotData restored = new SlotData(slot, e.customId(), e.displayName(),
                    e.glow(), e.hardness(), e.soundType(), e.noCollision(), e.category(), e.shape());
            SlotManager.restoreSnapshot(restored); // puts it back at slot_N + un-reserves the index
            index = slot;
        } else {
            SlotData created = SlotManager.create(e.customId(), e.displayName());
            if (created == null) {
                Chat.error(src, "Couldn't restore \"" + e.customId() + "\" — no free slot (the block pool is full).");
                return;
            }
            // Re-apply every saved attribute (mirrors SlotManager.dupe).
            SlotManager.setGlow(e.customId(), e.glow());
            SlotManager.setHardness(e.customId(), e.hardness());
            SlotManager.setSoundType(e.customId(), e.soundType());
            SlotManager.setNoCollision(e.customId(), e.noCollision());
            SlotManager.setCategory(e.customId(), e.category());
            SlotManager.setShape(e.customId(), e.shape());
            index = created.index();
        }

        // Put the texture + original source image back into the (re)used slot.
        byte[] tex = TrashManager.textureBytes(entryId);
        if (tex != null) TextureStore.save(index, tex);
        byte[] source = TrashManager.sourceBytes(entryId);
        if (source != null) TextureStore.saveSource(index, source);

        // A restore un-tombstones the id (it can heal again) and clears its stale marker label; the block
        // is live again, so its "Deleted: <name>" markers heal back into it (loaded now, far on chunk-load).
        MarkerTombstones.remove(e.customId());
        MarkerResolver.forget(index);
        DeletedPlacementSweeper.resolveMarkersNear(server);

        // Drop the trash entry and rebuild/push the pack so the texture shows.
        TrashManager.purge(entryId);
        ResourcePackServer.updatePack();
        ResourcePackServer.syncToAll();
        HudSync.broadcast(server); // restored identity shows live for everyone, no rejoin
        Chat.success(src, "Restored \"" + e.customId() + "\" from the trash"
                + (reuse ? " " + CbFmt.DIM + "(slot " + slot + ")" : "")
                + (tex == null ? " " + CbFmt.DIM + "(no saved texture — it'll look untextured until retextured)." : "."));
        GuiRouter.back(player); // pop the entry screen → refreshed trash list
    }

    /**
     * Empty (permanent delete) from the GUI — G06-14 slice 5. Frees the reserved slot number for reuse and
     * tombstones the block's markers so a later same-name create can't accidentally revive them:
     *   1. purge the trash data,
     *   2. release the reserved slot index (nextFreeSlotIndex may reuse it again),
     *   3. tombstone the id (its markers become generic, un-healable "(Deleted)") + drop the label memory,
     *   4. re-resolve loaded markers now so nearby copies go generic live; far ones on chunk-load.
     * Returns false if the entry was already gone.
     */
    public static boolean guiPurge(ServerPlayerEntity player, String entryId) {
        TrashManager.TrashEntry e = TrashManager.get(entryId);
        boolean ok = TrashManager.purge(entryId);
        if (e != null) {
            if (e.slotIndex() >= 0) DeletedSlots.remove(e.slotIndex()); // free the number for reuse
            MarkerTombstones.add(e.customId()); // its markers may never heal again
            MarkerResolver.forget(e.slotIndex());
            MinecraftServer server = player.getServer();
            if (server != null) DeletedPlacementSweeper.resolveMarkersNear(server);
        }
        return ok;
    }
}
