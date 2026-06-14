/**
 * ReIdCommands.java
 *
 * Responsibility: The `/cb reid` subcommand — change a block's custom id while keeping its
 * slot index (so the texture and any placed blocks are untouched). Three entry points:
 *   /cb reid                → pick-a-block GUI (ReIdMenu)
 *   /cb reid <id>           → anvil prompt for the new id (ReIdMenu.openAnvil)
 *   /cb reid <id> <newId>   → direct change
 * The id-key migration and cross-manager bookkeeping live in SlotManager.reId; this handler
 * validates, records the undo step, and reports. The GUI paths delegate back to the direct
 * form, so all the rules live in one place. Registered by CommandRegistrar. Split out of
 * CreationCommands (which is already near the 400-line handler gate, §9.3).
 *
 * Depends on: SlotManager/SlotData, LockManager, UndoManager, HudSync, Chat, BlockSuggestions,
 *             GuiRouter/Nav/ReIdMenu (GUI entry points)
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.UndoManager;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import com.customblocks.gui.chest.ReIdMenu;
import com.customblocks.network.HudSync;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class ReIdCommands {

    private ReIdCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // /cb reid                  → pick-a-block GUI (player only)
        // /cb reid <id>             → anvil prompt for the new id (player only)
        // /cb reid <id> <newId>     → direct; both ids are word()s (same charset as create)
        root.then(CommandManager.literal("reid")
                .executes(ReIdCommands::openPicker)
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> openAnvil(ctx, StringArgumentType.getString(ctx, "id")))
                        .then(CommandManager.argument("newId", StringArgumentType.word())
                                .executes(ctx -> reid(ctx,
                                        StringArgumentType.getString(ctx, "id"),
                                        StringArgumentType.getString(ctx, "newId"))))));
    }

    /** No-arg: open the pick-a-block re-id menu (chest GUI). Player only. */
    private static int openPicker(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) {
            Chat.error(src, "Open the re-id picker as a player, or use /cb reid <id> <newId>.");
            return 0;
        }
        GuiRouter.openFresh(p, MenuKey.of(Dest.REID));
        return 1;
    }

    /** Single-arg: open the anvil to type a new id for an existing, unlocked block. Player only. */
    private static int openAnvil(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) {
            Chat.error(src, "Open the re-id prompt as a player, or use /cb reid <id> <newId>.");
            return 0;
        }
        if (SlotManager.getById(id) == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        if (LockManager.isLocked(id)) {
            Chat.error(src, "\"" + id + "\" is locked. Use /cb unlock " + id + " to edit it.");
            return 0;
        }
        ReIdMenu.openAnvil(p, id, () -> {}); // from the command there is no menu to return to
        return 1;
    }

    private static int reid(CommandContext<ServerCommandSource> ctx, String oldId, String newId) {
        ServerCommandSource src = ctx.getSource();

        SlotData before = SlotManager.getById(oldId);
        if (before == null) {
            Chat.error(src, "There's no block called \"" + oldId + "\". Check /cb list for the right id.");
            return 0;
        }
        if (LockManager.isLocked(oldId)) {
            Chat.error(src, "\"" + oldId + "\" is locked. Use /cb unlock " + oldId + " to edit it.");
            return 0;
        }
        if (newId.equals(oldId)) {
            Chat.info(src, "\"" + newId + "\" is already its id — nothing to change.");
            return 1;
        }
        if (SlotManager.hasId(newId)) {
            Chat.error(src, "\"" + newId + "\" is already taken by another block. Pick a different id.");
            return 0;
        }

        SlotData after = SlotManager.reId(oldId, newId);
        if (after == null) {
            Chat.error(src, "Couldn't change the id \"" + oldId + "\" to \"" + newId + "\".");
            return 0;
        }
        UndoManager.recordReid(actor(src), before, after);
        Chat.success(src, "Changed id \"" + oldId + "\" to \"" + newId + "\". Undo with /cb undo.");
        if (src.getEntity() instanceof ServerPlayerEntity p) HudSync.sendTo(p);
        return 1;
    }

    /** The acting player's UUID, or null for console/command-block (those aren't undoable). */
    private static UUID actor(ServerCommandSource src) {
        return src.getEntity() instanceof ServerPlayerEntity p ? p.getUuid() : null;
    }
}
