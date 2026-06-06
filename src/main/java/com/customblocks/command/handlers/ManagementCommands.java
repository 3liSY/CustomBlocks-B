/**
 * ManagementCommands.java
 *
 * Responsibility: Block management commands — lock/unlock, notes, favorites, drafts.
 * These are organizational tools: locking prevents modification; notes annotate a block;
 * favorites let each player bookmark blocks; drafts mark blocks as work-in-progress.
 * Registered into the /cb tree by CommandRegistrar.
 *
 * Depends on: LockManager, BlockNotesManager, FavoritesManager, DraftManager, Chat, BlockSuggestions
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.BlockNotesManager;
import com.customblocks.core.DraftManager;
import com.customblocks.core.FavoritesManager;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotManager;
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

        // ── Notes ─────────────────────────────────────────────────────────────
        // /cb note <id>           → show note
        // /cb note <id> clear     → clear note  (literal takes priority over argument)
        // /cb note <id> <text...> → set note
        root.then(CommandManager.literal("note")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> showNote(ctx, StringArgumentType.getString(ctx, "id")))
                        .then(CommandManager.literal("clear")
                                .executes(ctx -> clearNote(ctx, StringArgumentType.getString(ctx, "id"))))
                        .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> setNote(ctx,
                                        StringArgumentType.getString(ctx, "id"),
                                        StringArgumentType.getString(ctx, "text"))))));

        // ── Favorites ─────────────────────────────────────────────────────────
        root.then(CommandManager.literal("fav")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> toggleFav(ctx, StringArgumentType.getString(ctx, "id")))));

        root.then(CommandManager.literal("favs").executes(ManagementCommands::listFavs));

        // ── Drafts ────────────────────────────────────────────────────────────
        root.then(CommandManager.literal("draft")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> markDraft(ctx, StringArgumentType.getString(ctx, "id")))));

        root.then(CommandManager.literal("publish")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> publishDraft(ctx, StringArgumentType.getString(ctx, "id")))));

        root.then(CommandManager.literal("drafts").executes(ManagementCommands::listDrafts));
    }

    // ── Lock handlers ─────────────────────────────────────────────────────────

    private static int lockBlock(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        if (SlotManager.getById(id) == null) { Chat.error(src, "No block '" + id + "'"); return 0; }
        if (!LockManager.lock(id)) { Chat.info(src, "'" + id + "' is already locked"); return 1; }
        Chat.success(src, "Locked §f" + id + "§r — use §f/cb unlock " + id + "§r to edit it again");
        return 1;
    }

    private static int unlockBlock(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        if (!LockManager.unlock(id)) { Chat.info(src, "'" + id + "' is not locked"); return 1; }
        Chat.success(src, "Unlocked §f" + id);
        return 1;
    }

    private static int listLocked(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        List<String> ids = LockManager.list();
        if (ids.isEmpty()) { Chat.info(src, "No locked blocks"); return 1; }
        src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§c" + ids.size() + " locked block(s):"), false);
        for (String id : ids) {
            MutableText line = Text.literal("§7 - §f" + id + " ")
                    .append(runButton("[unlock]", "/cb unlock " + id, "Unlock " + id));
            src.sendFeedback(() -> line, false);
        }
        return 1;
    }

    // ── Note handlers ─────────────────────────────────────────────────────────

    private static int showNote(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        if (SlotManager.getById(id) == null) { Chat.error(src, "No block '" + id + "'"); return 0; }
        String note = BlockNotesManager.getNote(id);
        if (note == null) {
            src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§7No note for §f" + id
                    + "§7. Set one: §f/cb note " + id + " <text>"), false);
        } else {
            src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§fNote on §e" + id + "§r: §7" + note), false);
        }
        return 1;
    }

    private static int setNote(CommandContext<ServerCommandSource> ctx, String id, String text) {
        ServerCommandSource src = ctx.getSource();
        if (SlotManager.getById(id) == null) { Chat.error(src, "No block '" + id + "'"); return 0; }
        BlockNotesManager.setNote(id, text);
        Chat.success(src, "Note on §f" + id + "§r saved");
        return 1;
    }

    private static int clearNote(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        if (!BlockNotesManager.clearNote(id)) { Chat.info(src, "No note set on '" + id + "'"); return 1; }
        Chat.success(src, "Note cleared from §f" + id);
        return 1;
    }

    // ── Favorite handlers ─────────────────────────────────────────────────────

    private static int toggleFav(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            Chat.error(src, "Favorites are per-player — run as a player, not console");
            return 0;
        }
        if (SlotManager.getById(id) == null) { Chat.error(src, "No block '" + id + "'"); return 0; }
        UUID uuid = player.getUuid();
        if (FavoritesManager.isFavorite(uuid, id)) {
            FavoritesManager.remove(uuid, id);
            Chat.info(src, "Removed §f" + id + "§7 from favorites");
        } else {
            FavoritesManager.add(uuid, id);
            Chat.success(src, "Added §f" + id + "§r to favorites ★");
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
            src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§7No favorites yet. Use §f/cb fav <id>§7 to bookmark a block."), false);
            return 1;
        }
        src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§e" + ids.size() + " favorite(s):"), false);
        for (String id : ids) {
            MutableText line = Text.literal("§7 - §f" + id + " ")
                    .append(runButton("[give]", "/cb give " + id, "Give " + id))
                    .append(Text.literal(" "))
                    .append(runButton("[unfav]", "/cb fav " + id, "Remove from favorites"));
            src.sendFeedback(() -> line, false);
        }
        return 1;
    }

    // ── Draft handlers ────────────────────────────────────────────────────────

    private static int markDraft(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        if (SlotManager.getById(id) == null) { Chat.error(src, "No block '" + id + "'"); return 0; }
        if (!DraftManager.markDraft(id)) { Chat.info(src, "'" + id + "' is already a draft"); return 1; }
        Chat.success(src, "§f" + id + "§r marked as draft. Publish with §f/cb publish " + id);
        return 1;
    }

    private static int publishDraft(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        if (!DraftManager.publish(id)) { Chat.info(src, "'" + id + "' is not a draft"); return 1; }
        Chat.success(src, "§f" + id + "§r published ✔");
        return 1;
    }

    private static int listDrafts(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        List<String> ids = DraftManager.list();
        if (ids.isEmpty()) { Chat.info(src, "No draft blocks"); return 1; }
        src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§7" + ids.size() + " draft(s):"), false);
        for (String id : ids) {
            MutableText line = Text.literal("§7 - §f" + id + " ")
                    .append(runButton("[publish]", "/cb publish " + id, "Publish " + id));
            src.sendFeedback(() -> line, false);
        }
        return 1;
    }

    // ── Clickable helpers ─────────────────────────────────────────────────────

    private static MutableText runButton(String label, String cmd, String hover) {
        return Text.literal(label).styled(s -> s
                .withColor(Formatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }
}
