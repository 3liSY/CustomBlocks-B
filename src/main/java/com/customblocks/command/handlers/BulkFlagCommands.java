/**
 * BulkFlagCommands.java
 *
 * Bulk lock / unlock / favorite / unfavorite (Group 07). These flip a per-block flag that is its
 * own inverse (bulkunlock reverses bulklock, etc.), so they apply immediately with no confirm
 * guard and no undo entry — the opposite command IS the undo. Kept separate from BulkCommands so
 * each handler stays small (§9.3); shares the hover-list formatting in BulkChat.
 *
 * Commands:
 *   /cb bulklock <filter>      /cb bulkunlock <filter>
 *   /cb bulkfavorite <filter>  /cb bulkunfavorite <filter>
 *
 * Filters resolved by BulkScope. Favorites are per-player, so those two need a player source.
 *
 * Depends on: BulkScope, LockManager, FavoritesManager, BulkChat, Chat
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.BulkScope;
import com.customblocks.core.FavoritesManager;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
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
        root.then(CommandManager.literal(literal)
                .executes(ctx -> openFlagBuilder(ctx.getSource(), op))
                .then(CommandManager.argument("filter", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.FILTER_ONLY)
                        .executes(ctx -> run(ctx.getSource(), op, StringArgumentType.getString(ctx, "filter")))));
    }

    /** No-arg /cb bulklock|unlock|favorite|unfavorite → open the two-step builder pre-set to it. */
    private static int openFlagBuilder(ServerCommandSource src, String runOp) {
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            boolean favorite = runOp.equals("favorite") || runOp.equals("unfavorite");
            com.customblocks.gui.chest.BulkSession.get(p.getUuid()).flagOn = runOp.equals("lock") || runOp.equals("favorite");
            return BulkCommands.openOpBuilder(src, favorite ? "favorite" : "lock");
        }
        Chat.error(src, "Open the bulk builder in-game with /cb bulkgui.");
        return 0;
    }

    /** Close the dashboard, then run a flag op assembled in the GUI (lock/unlock/favorite/unfavorite). */
    public static void applyFlagFromGui(ServerPlayerEntity player, String op, String filter) {
        net.minecraft.server.MinecraftServer s = player.getServer();
        if (s == null) return;
        s.execute(() -> {
            player.closeHandledScreen();
            run(player.getCommandSource(), op, filter);
        });
    }

    private static int run(ServerCommandSource src, String op, String filter) {
        boolean favorite = op.equals("favorite") || op.equals("unfavorite");
        UUID player = src.getEntity() instanceof ServerPlayerEntity p ? p.getUuid() : null;
        if (favorite && player == null) {
            Chat.error(src, "Favorites are per-player — run this as a player.");
            return 0;
        }

        List<SlotData> blocks = BulkScope.resolve(filter, player);
        if (blocks.isEmpty()) { Chat.error(src, "No blocks matched filter: " + filter); return 0; }

        List<String> changed = new ArrayList<>();
        for (SlotData d : blocks) {
            String id = d.customId();
            boolean did = switch (op) {
                case "lock"       -> LockManager.lock(id);
                case "unlock"     -> LockManager.unlock(id);
                case "favorite"   -> FavoritesManager.add(player, id);
                case "unfavorite" -> FavoritesManager.remove(player, id);
                default           -> false;
            };
            if (did) changed.add(id);
        }
        if (changed.isEmpty()) {
            Chat.info(src, "Nothing to do — those blocks were already " + pastTense(op) + ".");
            return 1;
        }

        String inverse = switch (op) {
            case "lock"       -> "/cb bulkunlock " + filter;
            case "unlock"     -> "/cb bulklock " + filter;
            case "favorite"   -> "/cb bulkunfavorite " + filter;
            default           -> "/cb bulkfavorite " + filter;
        };
        String hover = "§7" + capitalize(pastTense(op)) + " " + changed.size() + " block(s):\n§f"
                + BulkChat.columns(changed);
        MutableText msg = Text.literal("§a" + capitalize(pastTense(op)) + " ")
                .append(Chat.hover("§e§n" + changed.size() + " block" + (changed.size() == 1 ? "" : "s") + "§r", hover))
                .append(Text.literal(" §a✔  "))
                .append(Chat.runButton("§7[undo]", inverse, "§7Reverse this §8(" + inverse + ")"));
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
