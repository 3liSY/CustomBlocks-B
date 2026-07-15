/**
 * BulkFlagCommands.java
 *
 * Bulk lock / unlock / favorite / unfavorite (Group 07). These flip a per-block flag, and apply
 * immediately with no confirm guard. Kept separate from BulkCommands so each handler stays small
 * (§9.3); shares the hover-list formatting in BulkChat.
 *
 * G07-BULK-UNDO / G04-UNDO-DIALECT (2026-07-15): this file used to claim "no undo entry — the opposite
 * command IS the undo", and shipped a chip that ran /cb bulkunlock <filter>. That was a second undo
 * dialect: it recorded nothing, so /cb undo silently skipped these ops and reached an older unrelated
 * edit, and the "undo" chip was really a fresh forward command that re-applied to whatever the filter
 * matches NOW. Every op here now records ONE UndoManager batch of FLAG children, and the chip is the
 * same ↩ Undo → /cb undo that every other surface in the mod uses.
 *
 * Commands:
 *   /cb bulklock <filter>      /cb bulkunlock <filter>
 *   /cb bulkfavorite <filter>  /cb bulkunfavorite <filter>
 *
 * Filters resolved by BulkScope. Favorites are per-player, so those two need a player source. A locked block
 * is NOT skipped here — these ops are about the flags themselves. No-arg forms open the Bulk Workbench.
 *
 * Depends on: BulkScope, LockManager, FavoritesManager, UndoManager, BulkChat, Chat
 * Called by:  CommandRegistrar, BulkApply (the Screen)
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.BulkScope;
import com.customblocks.core.FavoritesManager;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.UndoManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BulkFlagCommands {

    private BulkFlagCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        flag(root, "bulklock",       "lock");
        flag(root, "bulkunlock",     "unlock");
        flag(root, "bulkfavorite",   "favorite");
        flag(root, "bulkunfavorite", "unfavorite");
    }

    private static void flag(LiteralArgumentBuilder<ServerCommandSource> root, String literal, String op) {
        // Lock/unlock share the Lock tab; favorite/unfavorite share the Favorite tab (§G07-4 A5a).
        String tabKey = (op.equals("favorite") || op.equals("unfavorite")) ? "favorite" : "lock";
        root.then(CommandManager.literal(literal)
                .executes(ctx -> BulkCommands.openOp(ctx.getSource(), tabKey))
                .then(CommandManager.argument("filter", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.FILTER_ONLY)
                        .executes(ctx -> run(ctx.getSource(), op, StringArgumentType.getString(ctx, "filter")))));
    }

    static int run(ServerCommandSource src, String op, String filter) {
        boolean favorite = op.equals("favorite") || op.equals("unfavorite");
        UUID player = src.getEntity() instanceof ServerPlayerEntity p ? p.getUuid() : null;
        if (favorite && player == null) {
            Chat.error(src, "Favorites are per-player — run this as a player.");
            return 0;
        }

        List<SlotData> blocks = BulkScope.resolve(filter, player);
        if (blocks.isEmpty()) { Chat.error(src, "No blocks matched: " + filter); return 0; }

        boolean on = op.equals("lock") || op.equals("favorite"); // what this op SETS the flag to
        String which = favorite ? "favorite" : "lock";
        List<String> changed = new ArrayList<>();
        List<UndoManager.Op> children = new ArrayList<>();
        for (SlotData d : blocks) {
            String id = d.customId();
            boolean did = switch (op) {
                case "lock"       -> LockManager.lock(id);
                case "unlock"     -> LockManager.unlock(id);
                case "favorite"   -> FavoritesManager.add(player, id);
                case "unfavorite" -> FavoritesManager.remove(player, id);
                default           -> false;
            };
            // Only a flag that ACTUALLY flipped is recorded — an already-locked block is not an undo step.
            if (did) {
                changed.add(id);
                children.add(UndoManager.flagOp(new UndoManager.Flag(id, which, on, player), op));
            }
        }
        if (changed.isEmpty()) {
            BulkResult.record(src, "Nothing to do — already " + pastTense(op) + "."); // X3
            Chat.info(src, "Nothing to do — those blocks were already " + pastTense(op) + ".");
            return 1;
        }

        // ONE undo entry for the whole batch, exactly like every other bulk op (G07.2 / G07-BULK-UNDO).
        UndoManager.recordBatch(BulkConfirm.actor(src), children, "bulk-" + op);
        String hover = CbFmt.DIM + capitalize(pastTense(op)) + " " + changed.size() + " block(s):\n" + CbFmt.BODY
                + BulkChat.columns(changed);
        MutableText msg = Text.literal(CbFmt.OK + capitalize(pastTense(op)) + " ")
                .append(Chat.hover(CbFmt.VALUE + CbFmt.UNDER + changed.size() + " block" + (changed.size() == 1 ? "" : "s") + CbFmt.RESET, hover))
                .append(Text.literal(" " + CbFmt.OK + "✔  "))
                .append(Chat.undoButton());
        BulkResult.record(src, capitalize(pastTense(op)) + " " + changed.size() + " block(s)."); // X3
        Chat.line(src, msg);
        return 1;
    }

    private static String pastTense(String op) {
        return switch (op) {
            case "lock"       -> "locked";
            case "unlock"     -> "unlocked";
            case "favorite"   -> "favorited";
            default           -> "unfavorited";
        };
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
