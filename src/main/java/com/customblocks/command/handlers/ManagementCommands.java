/**
 * ManagementCommands.java
 *
 * Responsibility: Block management commands — lock/unlock and favorites. These are organizational
 * tools: locking prevents modification; favorites let each player bookmark blocks. Notes moved to
 * NoteCommands (Group 18); the draft/publish staging system was scrapped (SWEEP_INDEX §A).
 * Registered into the /cb tree by CommandRegistrar.
 *
 * Depends on: LockManager, FavoritesManager, UndoManager, Chat, BlockSuggestions
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.core.WidgetSync;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.FavoritesManager;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotManager;
import com.customblocks.core.UndoManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;

public final class ManagementCommands {

    private ManagementCommands() {}

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // ── Lock ──────────────────────────────────────────────────────────────
        root.then(CommandManager.literal("lock")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> lockBlock(ctx, StringArgumentType.getString(ctx, "id")))));

        root.then(CommandManager.literal("unlock")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> unlockBlock(ctx, StringArgumentType.getString(ctx, "id")))));

        root.then(CommandManager.literal("locked").executes(ManagementCommands::listLocked));

        // Notes → NoteCommands (Group 18). Draft/publish staging → scrapped (SWEEP_INDEX §A).

        // ── Favorites ─────────────────────────────────────────────────────────
        root.then(CommandManager.literal("fav")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> toggleFav(ctx, StringArgumentType.getString(ctx, "id")))));

        root.then(CommandManager.literal("favs").executes(ManagementCommands::listFavs));
    }

    // ── Lock handlers ─────────────────────────────────────────────────────────

    /**
     * G04-UNDO-DIALECT (2026-07-15): the success line used to read "Locked <id> — use /cb unlock <id> to
     * edit it again", which is almost word-for-word the LOCK ERROR (Chat.lockedError) — so a successful
     * lock and a refused edit looked like the same message. It now says what the lock DOES, quotes the id
     * like every other line, and carries ↩ Undo. Both the lock and the unlock are recorded as a FLAG op,
     * so that chip undoes THE LOCK — not some older unrelated edit it used to reach past.
     */
    private static int lockBlock(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        if (SlotManager.getById(id) == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        if (!LockManager.lock(id)) { Chat.info(src, "\"" + id + "\" is already locked."); return 1; }
        WidgetSync.pushAll(src.getServer());   // G03: the padlock is a server-wide fact
        UndoManager.recordFlag(BulkConfirm.actor(src), new UndoManager.Flag(id, "lock", true, null), "lock");
        Chat.successWith(src, "Locked \"" + id + "\" — nobody can edit it until it's unlocked.", Chat.undoButton());
        return 1;
    }

    private static int unlockBlock(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        if (!LockManager.unlock(id)) { Chat.info(src, "\"" + id + "\" is not locked."); return 1; }
        WidgetSync.pushAll(src.getServer());   // G03: the padlock is a server-wide fact
        UndoManager.recordFlag(BulkConfirm.actor(src), new UndoManager.Flag(id, "lock", false, null), "unlock");
        Chat.successWith(src, "Unlocked \"" + id + "\" — it can be edited again.", Chat.undoButton());
        return 1;
    }

    private static int listLocked(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        List<String> ids = LockManager.list();
        if (ids.isEmpty()) { Chat.info(src, "No locked blocks"); return 1; }
        Chat.raw(src, CbFmt.BAD + ids.size() + " locked block(s):");
        for (String id : ids) {
            MutableText line = Text.literal(CbFmt.DIM + " - " + CbFmt.BODY + id + " ")
                    .append(Chat.runButton("[unlock]", "/cb unlock " + id, "Unlock " + id));
            Chat.raw(src, line);
        }
        return 1;
    }

    // ── Favorite handlers ─────────────────────────────────────────────────────

    private static int toggleFav(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            Chat.error(src, "Favorites are per-player — run as a player, not console");
            return 0;
        }
        if (SlotManager.getById(id) == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        UUID uuid = player.getUuid();
        if (FavoritesManager.isFavorite(uuid, id)) {
            FavoritesManager.remove(uuid, id);
            WidgetSync.push(player);
            // The Flag carries the OWNER uuid, not the clicker's — undo must un-do it on THIS player's list.
            UndoManager.recordFlag(BulkConfirm.actor(src), new UndoManager.Flag(id, "favorite", false, uuid), "unfavorite");
            Chat.successWith(src, "Removed \"" + id + "\" from your favorites.", Chat.undoButton());
        } else {
            FavoritesManager.add(uuid, id);
            WidgetSync.push(player);
            UndoManager.recordFlag(BulkConfirm.actor(src), new UndoManager.Flag(id, "favorite", true, uuid), "favorite");
            Chat.successWith(src, "Added \"" + id + "\" to your favorites ★", Chat.undoButton());
        }
        return 1;
    }

    private static int listFavs(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            Chat.error(src, "Favorites are per-player — run as a player, not console");
            return 0;
        }
        List<String> ids = FavoritesManager.list(player.getUuid());
        if (ids.isEmpty()) {
            Chat.raw(src, CbFmt.DIM + "No favorites yet. Use " + CbFmt.BODY + "/cb fav <id>" + CbFmt.DIM + " to bookmark a block.");
            return 1;
        }
        Chat.raw(src, CbFmt.VALUE + ids.size() + " favorite(s):");
        for (String id : ids) {
            MutableText line = Text.literal(CbFmt.DIM + " - " + CbFmt.BODY + id + " ")
                    .append(Chat.runButton("[give]", "/cb give " + id, "Give " + id))
                    .append(Text.literal(" "))
                    .append(Chat.runButton("[unfav]", "/cb fav " + id, "Remove from favorites"));
            Chat.raw(src, line);
        }
        return 1;
    }

    // ── Clickable helpers ─────────────────────────────────────────────────────

}
