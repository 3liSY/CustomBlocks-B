/**
 * HistoryCommands.java
 *
 * Responsibility: The history subcommands — `/cb undo` and `/cb redo`. Each is per-player
 * (FR-06-2): it reads that player's own stack from UndoManager and applies the reverse
 * (undo) or forward (redo) change through SlotManager's non-recording restore primitives.
 * Registered into the /cb tree by CommandRegistrar. Stays under 400 lines (§9.3).
 *
 * UndoManager stores only immutable SlotData snapshots; all state mutation and the
 * resource-pack / lighting refresh live here, so UndoManager stays free of server types.
 */
package com.customblocks.command.handlers;

import com.customblocks.block.SlotLighting;
import com.customblocks.command.Chat;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.UndoManager;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class HistoryCommands {

    private HistoryCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("undo").executes(HistoryCommands::undo));
        root.then(CommandManager.literal("redo").executes(HistoryCommands::redo));
    }

    private static int undo(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            Chat.error(src, "Run /cb undo as a player");
            return 0;
        }
        UUID who = player.getUuid();
        UndoManager.Op op = UndoManager.undo(who);
        if (op == null) {
            Chat.info(src, "Nothing to undo");
            return 0;
        }
        applyInverse(src, op);
        Chat.success(src, "Undid " + describe(op) + " §7(" + UndoManager.undoSize(who) + " left)");
        return 1;
    }

    private static int redo(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            Chat.error(src, "Run /cb redo as a player");
            return 0;
        }
        UUID who = player.getUuid();
        UndoManager.Op op = UndoManager.redo(who);
        if (op == null) {
            Chat.info(src, "Nothing to redo");
            return 0;
        }
        applyForward(src, op);
        Chat.success(src, "Redid " + describe(op) + " §7(" + UndoManager.redoSize(who) + " left)");
        return 1;
    }

    /** Reverse an op (undo direction). */
    private static void applyInverse(ServerCommandSource src, UndoManager.Op op) {
        switch (op.kind()) {
            case CREATE -> { // block was created → remove it again
                SlotManager.removeSilently(op.after().customId());
                ResourcePackServer.updatePack();
            }
            case DELETE -> { // block was deleted → bring it back (data + texture)
                SlotManager.restoreSnapshot(op.before());
                if (op.texture() != null) TextureStore.save(op.before().index(), op.texture());
                ResourcePackServer.updatePack();
            }
            case MODIFY -> restoreMeta(src, op.before());
        }
    }

    /** Re-apply an op (redo direction). */
    private static void applyForward(ServerCommandSource src, UndoManager.Op op) {
        switch (op.kind()) {
            case CREATE -> { // re-create the block
                SlotManager.restoreSnapshot(op.after());
                ResourcePackServer.updatePack();
            }
            case DELETE -> { // re-delete the block
                SlotManager.removeSilently(op.before().customId());
                ResourcePackServer.updatePack();
            }
            case MODIFY -> restoreMeta(src, op.after());
        }
    }

    /** Restore a metadata snapshot, then refresh placed-block lighting (glow may differ). */
    private static void restoreMeta(ServerCommandSource src, SlotData d) {
        SlotManager.restoreSnapshot(d);
        SlotLighting.applyToPlaced(src.getServer(), d.index(), d.glow());
    }

    private static String describe(UndoManager.Op op) {
        SlotData d = op.before() != null ? op.before() : op.after();
        String id = d != null ? d.customId() : "?";
        return op.label() + " " + id;
    }
}
