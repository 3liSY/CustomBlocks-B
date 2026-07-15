/**
 * NoteCommands.java
 *
 * Responsibility: the /cb lore command family (Group 18 REVAMP v2). "lore" is primary; "note" is kept as
 * an alias so the old habit still works. All lore editing is OP-only (decision R12), gated in-handler so
 * non-ops get a clear message.
 *
 *   /cb lore <id>               → open the Lore GUI            (alias: /cb note <id>)
 *   /cb lore <id> <text...>     → quick-add: append one line   (alias: /cb note <id> <text...>)
 *   /cb lore <id> clear         → wipe all lines, turn it Off  (alias: /cb note <id> clear)
 *   /cb lore import <id> <code> → pull shared lore from the vault (confirm before overwrite)
 *
 * Share itself is the GUI's Share button → {@link #share}, which uploads off-thread and posts a code.
 *
 * Depends on: BlockNotesManager, SlotManager, GuiRouter/Nav, Chat, BlockSuggestions,
 *             CloudVaultClient, BulkConfirm, HudSync
 * Called by:  CommandRegistrar, NotesMenu (Share button)
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.CustomBlocksConfig;
import com.customblocks.cloud.CloudVaultClient;
import com.customblocks.cloud.VaultHistory;
import com.customblocks.command.Chat;
import com.customblocks.core.BlockNotesManager;
import com.customblocks.core.NoteData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.customblocks.network.HudSync;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class NoteCommands {

    private NoteCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(tree(CommandManager.literal("lore")));  // primary
        root.then(tree(CommandManager.literal("note")));  // alias (old habit still works)
    }

    /** Build the shared subtree under a given literal so "lore" and "note" behave identically. */
    private static LiteralArgumentBuilder<ServerCommandSource> tree(LiteralArgumentBuilder<ServerCommandSource> node) {
        return node
                // import <id> <code>  (literal "import" wins over the greedy id word)
                .then(CommandManager.literal("import")
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .suggests(BlockSuggestions.IDS)
                                .then(CommandManager.argument("code", StringArgumentType.word())
                                        .executes(ctx -> importNote(ctx,
                                                StringArgumentType.getString(ctx, "id"),
                                                StringArgumentType.getString(ctx, "code"))))))
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> open(ctx, StringArgumentType.getString(ctx, "id")))
                        // "clear" literal takes priority over the greedy text arg
                        .then(CommandManager.literal("clear")
                                .executes(ctx -> clear(ctx, StringArgumentType.getString(ctx, "id"))))
                        .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> quickAdd(ctx,
                                        StringArgumentType.getString(ctx, "id"),
                                        StringArgumentType.getString(ctx, "text")))));
    }

    /** OP gate (decision R12) — returns true (and messages) when the source is NOT allowed. */
    private static boolean denyNonOp(ServerCommandSource src) {
        if (src.hasPermissionLevel(2)) return false;
        Chat.error(src, "Only operators can edit block lore.");
        return true;
    }

    private static int open(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        if (denyNonOp(src)) return 0;
        if (SlotManager.getById(id) == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) {
            Chat.error(src, "Open lore as a player, not from the console.");
            return 0;
        }
        GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.NOTES, id));
        return 1;
    }

    /** Quick-add (decision R11): append one line; enables the lore on the first line. */
    private static int quickAdd(CommandContext<ServerCommandSource> ctx, String id, String text) {
        ServerCommandSource src = ctx.getSource();
        if (denyNonOp(src)) return 0;
        if (SlotManager.getById(id) == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        BlockNotesManager.addLine(id, text);
        resync(src);
        Chat.success(src, "Added a lore line to " + CbFmt.BODY + id);
        Chat.info(src, "Tip: use " + CbFmt.BODY + "/cb lore " + id + CbFmt.DIM + " for the full editor (edit, delete, On/Off, Share).");
        return 1;
    }

    private static int clear(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        if (denyNonOp(src)) return 0;
        if (!BlockNotesManager.clearNote(id)) { Chat.info(src, "No lore set on \"" + id + "\""); return 1; }
        resync(src);                       // cleared lines must vanish from the item
        Chat.success(src, "Lore cleared from " + CbFmt.BODY + id);
        return 1;
    }

    // ── Share (called by the Lore GUI's Share button) ───────────────────────────

    /** Upload {@code id}'s lore to the vault off-thread, then post a share code in chat (decision R9). */
    public static void share(ServerPlayerEntity p, String id) {
        ServerCommandSource src = p.getCommandSource();
        if (!p.hasPermissionLevel(2)) { Chat.error(src, "Only operators can share block lore."); return; }
        if (SlotManager.getById(id) == null) { Chat.error(src, "There's no block called \"" + id + "\"."); return; }
        if (!BlockNotesManager.hasNote(id)) { Chat.info(src, "That block has no lore to share yet."); return; }
        if (!CustomBlocksConfig.cloudShareEnabled) {
            Chat.error(src, "Cloud sharing is disabled. Enable it in /cb config (cloudShareEnabled).");
            return;
        }
        if (!CloudVaultClient.isConfigured()) {
            Chat.error(src, "The cloud vault isn't set up yet. Put your worker URL in config.json as "
                    + "\"vaultEndpoint\", then /cb reload.");
            return;
        }
        MinecraftServer server = p.getServer();
        if (server == null) return;
        String json = BlockNotesManager.exportJson(id);
        Chat.info(src, "Sharing the lore on " + CbFmt.BODY + id + CbFmt.DIM + "…");
        new Thread(() -> {
            String code = CloudVaultClient.uploadNote(json);
            server.execute(() -> {
                if (code == null) {
                    Chat.error(src, "Share failed — check vaultEndpoint and that the worker is reachable.");
                } else {
                    VaultHistory.record("note", code, id, src);
                    MutableText msg = Text.literal(CbFmt.OK + "Shared lore on " + CbFmt.VALUE + id + CbFmt.OK + " — code: " + CbFmt.VALUE + code + "  ")
                            .append(Chat.shareButton(code))
                            .append(Text.literal("  " + CbFmt.DIM + "Import with " + CbFmt.BODY + "/cb lore import <id> " + code));
                    Chat.line(src, msg);
                }
            });
        }, "cb-lore-share").start();
    }

    // ── /cb lore import <id> <code>  (vault download — off-thread, confirm before overwrite) ──

    private static int importNote(CommandContext<ServerCommandSource> ctx, String id, String code) {
        ServerCommandSource src = ctx.getSource();
        if (denyNonOp(src)) return 0;
        if (SlotManager.getById(id) == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        if (!CustomBlocksConfig.cloudShareEnabled) {
            Chat.error(src, "Cloud sharing is disabled. Enable it in /cb config (cloudShareEnabled).");
            return 0;
        }
        if (!CloudVaultClient.isConfigured()) {
            Chat.error(src, "The cloud vault isn't set up yet. Put your worker URL in config.json as "
                    + "\"vaultEndpoint\", then /cb reload.");
            return 0;
        }
        MinecraftServer server = src.getServer();
        if (server == null) return 0;
        Chat.info(src, "Downloading lore code \"" + code + "\"…");
        new Thread(() -> {
            String json = CloudVaultClient.downloadNote(code);
            NoteData incoming = (json == null) ? null : BlockNotesManager.importJson(json);
            server.execute(() -> {
                if (incoming == null || incoming.isEmpty()) {
                    Chat.error(src, "Download failed — bad code, empty lore, or the vault is unreachable.");
                    return;
                }
                if (BlockNotesManager.hasNote(id)) {                  // R9/D7 — confirm before overwrite
                    BulkConfirm.request(src, () -> commitImport(src, id, incoming),
                            "overwrite the lore on " + id);
                    Chat.info(src, CbFmt.VALUE + id + CbFmt.DIM + " already has lore. Run " + CbFmt.BODY + "/cb confirm" + CbFmt.DIM + " to overwrite, "
                            + CbFmt.BODY + "/cb cancel" + CbFmt.DIM + " to keep it.");
                } else {
                    commitImport(src, id, incoming);
                }
            });
        }, "cb-lore-import").start();
        return 1;
    }

    /** Apply downloaded lore onto {@code id} and re-sync the item hover. */
    private static void commitImport(ServerCommandSource src, String id, NoteData incoming) {
        BlockNotesManager.update(id, incoming);
        resync(src);
        Chat.success(src, "Lore imported onto " + CbFmt.BODY + id);
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    /** Re-broadcast the slot index so changed lore shows/hides live for EVERY player (was actor-only). */
    private static void resync(ServerCommandSource src) {
        HudSync.broadcast(src.getServer()); // NO-REJOIN: lore change shows live for all players
    }

}
