/**
 * CreationCommands.java
 *
 * Responsibility: The block lifecycle subcommands — create, delete, rename, dupe,
 * retexture. Registered into the /cb tree by CommandRegistrar. All mutations go through
 * SlotManager (the single source of truth). Stays under 400 lines (§9.3).
 *
 * retexture downloads + processes the image on a background thread (never blocks the
 * server tick), then hops back to the server thread to rebuild + push the resource pack.
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.BlockNotesManager;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.UndoManager;
import com.customblocks.image.ImageDownloader;
import com.customblocks.image.ImageProcessor;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class CreationCommands {

    private CreationCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // Old-project format the players already know:
        //   /cb create <id>                  → block, name = id, untextured
        //   /cb create <id> <name>           → block with a display name, untextured
        //   /cb create <id> <name> <url>     → block + texture in one command
        // Name is a quoted string so a multi-word name + URL can coexist
        //   (single-word names need no quotes, e.g. /cb create heart Heart https://...).
        root.then(CommandManager.literal("create")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .executes(ctx -> create(ctx, id(ctx), null, null))
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .executes(ctx -> create(ctx, id(ctx), StringArgumentType.getString(ctx, "name"), null))
                                .then(CommandManager.argument("url", StringArgumentType.greedyString())
                                        .executes(ctx -> create(ctx, id(ctx),
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "url").trim()))))));

        root.then(CommandManager.literal("delete")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> delete(ctx, id(ctx)))));

        root.then(CommandManager.literal("rename")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> rename(ctx, id(ctx), StringArgumentType.getString(ctx, "name"))))));

        root.then(CommandManager.literal("dupe")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .then(CommandManager.argument("newId", StringArgumentType.word())
                                .executes(ctx -> dupe(ctx, id(ctx), StringArgumentType.getString(ctx, "newId"))))));

        root.then(CommandManager.literal("retexture")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .then(CommandManager.argument("url", StringArgumentType.greedyString())
                                .executes(ctx -> retexture(ctx, id(ctx), StringArgumentType.getString(ctx, "url"))))));
    }

    private static String id(CommandContext<ServerCommandSource> ctx) {
        return StringArgumentType.getString(ctx, "id");
    }

    /** The acting player's UUID, or null for console/command-block (those aren't undoable). */
    private static UUID actor(ServerCommandSource src) {
        return src.getEntity() instanceof ServerPlayerEntity p ? p.getUuid() : null;
    }

    private static int create(CommandContext<ServerCommandSource> ctx, String id, String name, String url) {
        ServerCommandSource src = ctx.getSource();
        // Validate the URL BEFORE creating, so a bad link never leaves a half-made block.
        if (url != null && !url.isBlank() && !ImageDownloader.isHttpUrl(url)) {
            Chat.error(src, "Not a web link — use a direct http/https image URL. Not created");
            return 0;
        }
        SlotData d = SlotManager.create(id, name);
        if (d == null) {
            Chat.error(src, "Can't create '" + id + "' — id exists or no free slots");
            return 0;
        }
        UndoManager.recordCreate(actor(src), d);
        Chat.success(src, "Created " + id + " (slot " + d.index() + ")");
        if (url != null && !url.isBlank()) {
            applyTexture(src, id, d.index(), url); // one-shot create + texture
        }
        return 1;
    }

    private static int delete(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        // Snapshot the block + its texture BEFORE deleting, so undo can fully restore it.
        SlotData before = SlotManager.getById(id);
        if (before == null) {
            Chat.error(src, "No block '" + id + "'");
            return 0;
        }
        if (LockManager.isLocked(id)) {
            Chat.error(src, "'" + id + "' is locked — /cb unlock " + id + " first");
            return 0;
        }
        byte[] texture = TextureStore.load(before.index());
        SlotManager.delete(id);
        BlockNotesManager.onBlockDeleted(id); // clean up orphaned note if any
        ResourcePackServer.updatePack(); // free the slot's texture from the pack
        UndoManager.recordDelete(actor(src), before, texture);
        Chat.success(src, "Deleted " + id);
        return 1;
    }

    private static int rename(CommandContext<ServerCommandSource> ctx, String id, String name) {
        ServerCommandSource src = ctx.getSource();
        SlotData before = SlotManager.getById(id);
        if (before == null) {
            Chat.error(src, "No block '" + id + "'");
            return 0;
        }
        if (LockManager.isLocked(id)) {
            Chat.error(src, "'" + id + "' is locked — /cb unlock " + id + " first");
            return 0;
        }
        SlotData d = SlotManager.rename(id, name);
        if (d == null) {
            Chat.error(src, "No block '" + id + "'");
            return 0;
        }
        UndoManager.recordModify(actor(src), before, d, "rename");
        Chat.success(src, "Renamed " + id + " → \"" + name + "\"");
        return 1;
    }

    private static int dupe(CommandContext<ServerCommandSource> ctx, String id, String newId) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.dupe(id, newId);
        if (d == null) {
            Chat.error(src, "Can't duplicate — check the ids and that a free slot exists");
            return 0;
        }
        UndoManager.recordCreate(actor(src), d); // dupe makes a new block → undo = delete it
        Chat.success(src, "Duplicated " + id + " → " + newId + " (slot " + d.index() + ")");
        return 1;
    }

    private static int retexture(CommandContext<ServerCommandSource> ctx, String id, String url) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.getById(id);
        if (d == null) {
            Chat.error(src, "No block '" + id + "'");
            return 0;
        }
        if (LockManager.isLocked(id)) {
            Chat.error(src, "'" + id + "' is locked — /cb unlock " + id + " first");
            return 0;
        }
        applyTexture(src, id, d.index(), url);
        return 1;
    }

    /**
     * Download + decode the image off the server thread, then hop back to the server
     * thread to rebuild and push the resource pack. Shared by create-with-url and retexture.
     */
    private static void applyTexture(ServerCommandSource src, String id, int index, String url) {
        MinecraftServer server = src.getServer();
        Chat.info(src, "Downloading texture for " + id + "…");

        Thread worker = new Thread(() -> {
            try {
                byte[] raw = ImageDownloader.download(url);
                byte[] png = ImageProcessor.toBlockPng(raw, CustomBlocksConfig.textureSize);
                TextureStore.save(index, png);
                server.execute(() -> {
                    ResourcePackServer.updatePack();
                    Chat.success(src, "Textured " + id + " — accept the pack prompt");
                });
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                server.execute(() -> Chat.error(src, "Retexture failed: " + msg));
            }
        }, "CustomBlocks-Retexture");
        worker.setDaemon(true);
        worker.start();
    }
}
