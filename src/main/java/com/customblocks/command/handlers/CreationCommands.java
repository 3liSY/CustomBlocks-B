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
import com.customblocks.ai.AiTextureGenerator;
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
import java.util.function.Consumer;

public final class CreationCommands {

    private CreationCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // Old-project format the players already know:
        //   /cb create <id>                  → block, name = id, untextured
        //   /cb create <id> <name>           → block with a display name, untextured
        //   /cb create <id> <name> <url>     → block + texture in one command
        // Name is a quoted string so a multi-word name + URL can coexist
        //   (single-word names need no quotes, e.g. /cb create heart Heart https://...).
        // Group 27 §G27.6 — bare /cb create (no args) opens the Block Creation Studio (CreationStudioBridge).
        root.then(CommandManager.literal("create")
                .executes(ctx -> CreationStudioBridge.openStudio(ctx.getSource()))
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
                        .suggests((c, b) -> { b.suggest(16); b.suggest(32); b.suggest(64); b.suggest(128); b.suggest(256); b.suggest(512); return b.buildFuture(); })
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
        return doCreate(ctx.getSource(), id, name, url);
    }

    /** The single block-creation rail, shared by the /cb create CLI and the studio (CreationStudioBridge, §G27.6). */
    static int doCreate(ServerCommandSource src, String id, String name, String url) {
        return doCreate(src, id, name, url, null, null);
    }

    /** Same rail + optional {@code postApply} (run before the pack rebuild) and {@code bgArgb} studio background (null = none). */
    static int doCreate(ServerCommandSource src, String id, String name, String url,
                        Consumer<SlotData> postApply, Integer bgArgb) {
        // With a URL: download + decode FIRST, and only create the block if that succeeds — a broken
        // or non-image link must NOT leave behind an empty, untextured block (developer-reported bug).
        if (url != null && !url.isBlank()) {
            if (!ImageDownloader.isHttpUrl(url)) {
                Chat.error(src, "That doesn't look like a web link, so the block was not created. "
                        + "Use a direct http/https image URL (right-click the image → Copy Image Address).");
                return 0;
            }
            if (SlotManager.hasId(id)) {
                Chat.error(src, "Couldn't create \"" + id + "\" — that id is already taken. Try a different id.");
                return 0;
            }
            createWithTexture(src, id, name, url, postApply, bgArgb);
            return 1;
        }

        // No URL: create immediately (nothing can fail to download).
        SlotData d = SlotManager.create(id, name);
        if (d == null) {
            Chat.error(src, "Couldn't create \"" + id + "\" — that id is already taken or every slot "
                    + "is in use. Try a different id, or check /cb list.");
            return 0;
        }
        UndoManager.recordCreate(actor(src), d);
        if (postApply != null) { postApply.accept(d); ResourcePackServer.updatePack(); } // studio shape/attrs
        Chat.success(src, name == null
                ? "Block \"" + id + "\" created successfully."
                : "Block \"" + id + "\" (\"" + name + "\") created successfully.");
        CategoryCommands.suggestOnCreate(src, d); // Group 11: one-click category hint (opt-out via config)
        syncHud(src);
        return 1;
    }

    /**
     * Download + decode the image off the server thread, and create the block ONLY when that
     * succeeds. If the link is broken / rejected / not an image, nothing is created (the fix for
     * "a bad link still makes an empty block"). Mirrors applyTexture's pipeline, but the slot is
     * allocated last so a failure leaves the world untouched.
     */
    private static void createWithTexture(ServerCommandSource src, String id, String name, String url,
                                          Consumer<SlotData> postApply, Integer bgArgb) {
        MinecraftServer server = src.getServer();
        Chat.info(src, "Fetching the image for \"" + id + "\" before creating…");

        Thread worker = new Thread(() -> {
            try {
                // AI/Pollinations links generate on the fly (slow first hit) → use the longer timeout + retry.
                byte[] raw = AiTextureGenerator.isAiUrl(url)
                        ? ImageDownloader.download(url, AiTextureGenerator.FETCH_TIMEOUT_SECONDS)
                        : ImageDownloader.download(url);
                // Group 14 — if the download is an actually-animated GIF/WebP, build an animated block
                // (vertical strip + .mcmeta) and stop here. Returns false → fall through to static.
                if (AnimCommands.maybeCreateAnimated(src, id, name, raw, server)) return;
                byte[] cleaned = BackgroundRemover.apply(raw, CustomBlocksConfig.backgroundMode,
                        CustomBlocksConfig.backgroundTolerance);
                byte[] png = ImageProcessor.toBlockPng(cleaned, CustomBlocksConfig.textureSize);
                // Studio "background" colour fills behind the image's transparent pixels (else snap-to-black).
                png = bgArgb != null ? ImageProcessor.fillBackground(png, bgArgb)
                        : BackgroundRemover.snapBackgroundBlack(png, CustomBlocksConfig.backgroundMode, CustomBlocksConfig.backgroundTolerance);
                final byte[] finalPng = png;
                server.execute(() -> {
                    // Re-check on the server thread — the id could have been taken while downloading.
                    SlotData d = SlotManager.create(id, name);
                    if (d == null) {
                        Chat.error(src, "Couldn't create \"" + id + "\" — the id was taken or every slot "
                                + "is in use. Nothing was created.");
                        return;
                    }
                    TextureStore.save(d.index(), finalPng);
                    TextureStore.saveSource(d.index(), raw);
                    UndoManager.recordCreate(actor(src), d);
                    if (postApply != null) postApply.accept(d); // studio shape/attrs before the rebuild
                    ResourcePackServer.updatePack();
                    Chat.success(src, "Block \"" + id + "\"" + (name == null ? "" : " (\"" + name + "\")")
                            + " created" + (CustomBlocksConfig.silentPack
                            ? " — it'll show in a moment."
                            : " — accept the resource pack prompt to see it."));
                    syncHud(src);
                });
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                IncidentRecorder.record("Create-with-texture failed for \"" + id + "\" (by "
                        + src.getName() + ", url: " + url + ")", e);
                server.execute(() -> Chat.error(src,
                        "Couldn't get an image from that URL, so the block was NOT created. " + msg));
            }
        }, "CustomBlocks-CreateTexture");
        worker.setDaemon(true);
        worker.start();
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
                // AI/Pollinations links generate on the fly (slow first hit) → use the longer timeout + retry.
                byte[] raw = AiTextureGenerator.isAiUrl(url)
                        ? ImageDownloader.download(url, AiTextureGenerator.FETCH_TIMEOUT_SECONDS)
                        : ImageDownloader.download(url);
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
            int rerendered = 0, upscaled = 0, skipped = 0, animated = 0;
            for (int index : indices) {
                try {
                    // Group 14 — never run an animated strip through toBlockPng; it would crop the
                    // tall vertical strip into one square and destroy the animation. Leave it alone.
                    if (SlotManager.animFor(index).isAnimated()) { animated++; continue; }
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
            final int fr = rerendered, fu = upscaled, fs = skipped, fa = animated;
            server.execute(() -> {
                ResourcePackServer.updatePack(); // ONE rebuild after the whole batch (§7)
                if (fs > 0) {
                    IncidentRecorder.record("Retexture-all to " + newSize + "px skipped " + fs
                            + " slot(s) (no source/texture or decode error)");
                }
                String animNote = fa > 0 ? " §b" + fa + "§r animated left untouched." : "";
                Chat.success(src, "Retexture complete — §a" + fr + "§r re-rendered, §e" + fu
                        + "§r upscaled, §7" + fs + "§r skipped." + animNote + " " + (CustomBlocksConfig.silentPack
                        ? "Blocks update in a moment." : "Accept the resource-pack prompt to see them."));
            });
        }, "CustomBlocks-RetextureAll");
        worker.setDaemon(true);
        worker.start();
        return 1;
    }
}
