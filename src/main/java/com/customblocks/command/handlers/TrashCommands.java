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

import com.customblocks.command.Chat;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.TrashManager;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
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
        // Put the texture + original source image back into the new slot.
        byte[] tex = TrashManager.textureBytes(entryId);
        if (tex != null) TextureStore.save(created.index(), tex);
        byte[] source = TrashManager.sourceBytes(entryId);
        if (source != null) TextureStore.saveSource(created.index(), source);
        // Drop the trash entry and rebuild/push the pack so the texture shows.
        TrashManager.purge(entryId);
        ResourcePackServer.updatePack();
        ResourcePackServer.syncToAll();
        Chat.success(src, "Restored \"" + e.customId() + "\" from the trash"
                + (tex == null ? " §7(no saved texture — it'll look untextured until retextured)." : "."));
        GuiRouter.back(player); // pop the entry screen → refreshed trash list
    }
}
