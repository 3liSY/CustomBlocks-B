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
import com.customblocks.core.IncidentRecorder;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.UndoManager;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.image.ImageDownloader;
import com.customblocks.image.ImageProcessor;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

        // /cb retextureall [16-512] — re-render every existing block at the given size (default:
        // the current textureSize). Triggered by the TextureSizeMenu "Yes" confirm; usable directly.
        root.then(CommandManager.literal("retextureall")
                .executes(ctx -> retextureAll(ctx.getSource().getServer(), CustomBlocksConfig.textureSize, ctx.getSource()))
                .then(CommandManager.argument("px", IntegerArgumentType.integer(16, 512))
                        .executes(ctx -> retextureAll(ctx.getSource().getServer(),
                                IntegerArgumentType.getInteger(ctx, "px"), ctx.getSource()))));
    }

    private static String id(CommandContext<ServerCommandSource> ctx) {
        return StringArgumentType.getString(ctx, "id");
    }

    /** The acting player's UUID, or null for console/command-block (those aren't undoable). */
    private static UUID actor(ServerCommandSource src) {
        return src.getEntity() instanceof ServerPlayerEntity p ? p.getUuid() : null;
    }

    /** Re-sync the HUD cache for the acting player (no-op if command came from console). */
    private static void syncHud(ServerCommandSource src) {
        if (src.getEntity() instanceof ServerPlayerEntity p) HudSync.sendTo(p);
    }

    private static int create(CommandContext<ServerCommandSource> ctx, String id, String name, String url) {
        ServerCommandSource src = ctx.getSource();
        // Validate the URL BEFORE creating, so a bad link never leaves a half-made block.
        if (url != null && !url.isBlank() && !ImageDownloader.isHttpUrl(url)) {
            Chat.error(src, "That doesn't look like a web link, so the block was not created. "
                    + "Use a direct http/https image URL (right-click the image → Copy Image Address).");
            return 0;
        }
        SlotData d = SlotManager.create(id, name);
        if (d == null) {
            Chat.error(src, "Couldn't create \"" + id + "\" — that id is already taken or every slot "
                    + "is in use. Try a different id, or check /cb list.");
            return 0;
        }
        UndoManager.recordCreate(actor(src), d);
        Chat.success(src, name == null
                ? "Block \"" + id + "\" created successfully."
                : "Block \"" + id + "\" (\"" + name + "\") created successfully.");
        syncHud(src);
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
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        if (LockManager.isLocked(id)) {
            Chat.error(src, "\"" + id + "\" is locked. Use /cb unlock " + id + " to edit it.");
            return 0;
        }
        byte[] texture = TextureStore.load(before.index());
        SlotManager.delete(id);
        BlockNotesManager.onBlockDeleted(id); // clean up orphaned note if any
        ResourcePackServer.updatePack(); // free the slot's texture from the pack
        UndoManager.recordDelete(actor(src), before, texture);
        Chat.success(src, "Block \"" + id + "\" deleted. You can undo this with /cb undo.");
        syncHud(src);
        return 1;
    }

    private static int rename(CommandContext<ServerCommandSource> ctx, String id, String name) {
        ServerCommandSource src = ctx.getSource();
        SlotData before = SlotManager.getById(id);
        if (before == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        if (LockManager.isLocked(id)) {
            Chat.error(src, "\"" + id + "\" is locked. Use /cb unlock " + id + " to edit it.");
            return 0;
        }
        SlotData d = SlotManager.rename(id, name);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        UndoManager.recordModify(actor(src), before, d, "rename");
        Chat.success(src, "Renamed \"" + id + "\" to \"" + name + "\".");
        syncHud(src);
        return 1;
    }

    private static int dupe(CommandContext<ServerCommandSource> ctx, String id, String newId) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.dupe(id, newId);
        if (d == null) {
            Chat.error(src, "Couldn't duplicate \"" + id + "\". Make sure it exists, \"" + newId
                    + "\" isn't taken, and a free slot is available.");
            return 0;
        }
        UndoManager.recordCreate(actor(src), d); // dupe makes a new block → undo = delete it
        Chat.success(src, "Duplicated \"" + id + "\" into a new block \"" + newId + "\".");
        syncHud(src);
        return 1;
    }

    /* ============================================================================================
     * ║  NOTE — PLANNED FEATURE: "retexture existing blocks to the new size" (sub-sub-GUI)         ║
     * ║                                                                                            ║
     * ║  After the player picks a size in TextureSizeMenu, a 3-button confirm (Yes / Info / No)    ║
     * ║  should offer to re-render already-created blocks at the newly chosen textureSize.         ║
     * ║                                                                                            ║
     * ║  This is now BUILDABLE: applyTexture() stores each URL block's ORIGINAL image via          ║
     * ║  TextureStore.saveSource(). A "retexture all" job would, per assigned slot:                ║
     * ║     byte[] raw = TextureStore.loadSource(index);                                           ║
     * ║     if (raw == null)  → no source (Arabic glyph / video / made before this update):        ║
     * ║                         skip, or upscale the baked slot_N.png (no real detail gained).     ║
     * ║     else  → re-run BackgroundRemover.apply + ImageProcessor.toBlockPng(raw, newSize)       ║
     * ║            + snapBackgroundBlack, then TextureStore.save(index, png).                       ║
     * ║     Finally ONE ResourcePackServer.updatePack() AFTER the whole batch (not per block).     ║
     * ║                                                                                            ║
     * ║  CAUTIONS (CLAUDE.md §7): run off the server thread; debounce/one pack rebuild at the end; ║
     * ║  show the live count = SlotManager.usedSlots() in the Info button; sources are only        ║
     * ║  present for blocks created AFTER this update (older blocks → upscale-only).               ║
     * ║  Status: PARTIAL — source store DONE this session; the GUI + batch job are NOT built yet.  ║
     * ============================================================================================ */
    private static int retexture(CommandContext<ServerCommandSource> ctx, String id, String url) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.getById(id);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        if (LockManager.isLocked(id)) {
            Chat.error(src, "\"" + id + "\" is locked. Use /cb unlock " + id + " to edit it.");
            return 0;
        }
        applyTexture(src, id, d.index(), url);
        return 1;
    }

    /**
     * Download + decode the image off the server thread, then hop back to the server
     * thread to rebuild and push the resource pack. Shared by create-with-url and retexture.
     * Failures go to the triggering player AND the incidents log so admins can review
     * errors they didn't see live (Group 04 major-error routing).
     */
    private static void applyTexture(ServerCommandSource src, String id, int index, String url) {
        MinecraftServer server = src.getServer();
        Chat.info(src, "Downloading texture for \"" + id + "\"…");

        Thread worker = new Thread(() -> {
            try {
                byte[] raw = ImageDownloader.download(url);
                // M1: strip the background to opaque black first (no-op when mode is "none"),
                // BEFORE the resize/pad so corner sampling reads the real background.
                byte[] cleaned = BackgroundRemover.apply(raw, CustomBlocksConfig.backgroundMode,
                        CustomBlocksConfig.backgroundTolerance);
                byte[] png = ImageProcessor.toBlockPng(cleaned, CustomBlocksConfig.textureSize);
                // Restore a true black after the bicubic resize blends the edges (no-op when off).
                png = BackgroundRemover.snapBackgroundBlack(png, CustomBlocksConfig.backgroundMode,
                        CustomBlocksConfig.backgroundTolerance);
                TextureStore.save(index, png);
                // Keep the ORIGINAL image so the block can later be re-rendered at a different
                // texture size from real pixels (see the retexture-all NOTE on retexture()).
                TextureStore.saveSource(index, raw);
                server.execute(() -> {
                    ResourcePackServer.updatePack();
                    Chat.success(src, CustomBlocksConfig.silentPack
                            ? "Texture applied to \"" + id + "\". It'll show on the block in a moment."
                            : "Texture applied to \"" + id + "\" — accept the resource pack prompt to see it.");
                });
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                IncidentRecorder.record("Texture download failed for \"" + id + "\" (by "
                        + src.getName() + ", url: " + url + ")", e);
                server.execute(() -> Chat.error(src,
                        "Couldn't get a texture from that URL. " + msg));
            }
        }, "CustomBlocks-Retexture");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Re-render EVERY assigned block at {@code newSize} from its stored source image, off the
     * server thread, then rebuild the pack ONCE at the end (CLAUDE.md §7: no per-block pack churn).
     * Blocks with no stored source (made before the source store, or Arabic/video) are upscaled
     * from their baked texture instead — no new detail, but they still match the new size. Drives
     * the TextureSizeMenu → RetextureConfirmMenu "Yes" button. See the NOTE above retexture().
     */
    public static int retextureAll(MinecraftServer server, int newSize, ServerCommandSource src) {
        if (server == null) return 0;
        // Snapshot indices on the caller thread; copy the bg config so the worker reads a stable view.
        java.util.List<Integer> indices = new java.util.ArrayList<>();
        for (SlotData d : SlotManager.assignedSlots()) indices.add(d.index());
        final String mode = CustomBlocksConfig.backgroundMode;
        final int tol = CustomBlocksConfig.backgroundTolerance;
        Chat.info(src, "Retexturing " + indices.size() + " block(s) to §e" + newSize + "px§r…");

        Thread worker = new Thread(() -> {
            int rerendered = 0, upscaled = 0, skipped = 0;
            for (int index : indices) {
                try {
                    byte[] raw = TextureStore.loadSource(index);
                    if (raw != null && raw.length > 0) {
                        byte[] cleaned = BackgroundRemover.apply(raw, mode, tol);
                        byte[] png = ImageProcessor.toBlockPng(cleaned, newSize);
                        png = BackgroundRemover.snapBackgroundBlack(png, mode, tol);
                        TextureStore.save(index, png);
                        rerendered++;
                    } else {
                        byte[] baked = TextureStore.load(index); // no source → upscale existing pixels
                        if (baked != null && baked.length > 0) {
                            TextureStore.save(index, ImageProcessor.toBlockPng(baked, newSize));
                            upscaled++;
                        } else {
                            skipped++;
                        }
                    }
                } catch (Exception e) {
                    skipped++;
                }
            }
            final int fr = rerendered, fu = upscaled, fs = skipped;
            server.execute(() -> {
                ResourcePackServer.updatePack(); // ONE rebuild after the whole batch (§7)
                if (fs > 0) {
                    IncidentRecorder.record("Retexture-all to " + newSize + "px skipped " + fs
                            + " slot(s) (no source/texture or decode error)");
                }
                Chat.success(src, "Retexture complete — §a" + fr + "§r re-rendered, §e" + fu
                        + "§r upscaled, §7" + fs + "§r skipped. " + (CustomBlocksConfig.silentPack
                        ? "Blocks update in a moment." : "Accept the resource-pack prompt to see them."));
            });
        }, "CustomBlocks-RetextureAll");
        worker.setDaemon(true);
        worker.start();
        return 1;
    }
}
