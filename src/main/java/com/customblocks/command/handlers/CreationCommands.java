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

import com.customblocks.command.CbFmt;
import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.AnimData;
import com.customblocks.core.IncidentRecorder;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.UndoManager;
import com.customblocks.core.onboarding.AchievementManager;
import com.customblocks.core.onboarding.FirstUseHints;
import com.customblocks.ai.AiTextureGenerator;
import com.customblocks.image.AnimationDecoder;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.image.CheckerboardDetector;
import com.customblocks.image.ImageDownloader;
import com.customblocks.image.ImageProcessor;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
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

    /** G10-6: shown when a "transparent" source was really a flattened preview (checkerboard baked in) → cleaned to black. */
    private static final String FLAT_CHECKER_NOTE =
            CbFmt.VALUE + "Heads-up:" + CbFmt.RESET + " that was a flattened " + CbFmt.ITALIC + "preview" + CbFmt.RESET + " image (transparency checkerboard baked in), so the "
            + "background was cleaned to black. For a crisper result, grab the site's real " + CbFmt.OK + "transparent PNG" + CbFmt.RESET + " download.";

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

        // delete lives in DeleteCommands (Group 17 slice 3) — split out for /cb delete # + size cap.

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

        // /cb retextureall lives in RetextureAllCommands (split out for the §9.3 handler 400-line gate).
    }

    private static String id(CommandContext<ServerCommandSource> ctx) {
        return StringArgumentType.getString(ctx, "id");
    }

    /** The acting player's UUID, or null for console/command-block (those aren't undoable). */
    private static UUID actor(ServerCommandSource src) {
        return src.getEntity() instanceof ServerPlayerEntity p ? p.getUuid() : null;
    }

    /** Re-sync the slot cache for EVERY online client so a created/renamed block lands without a
     *  rejoin (G05-1). Was actor-only, which left other players stale on a server. */
    private static void syncHud(ServerCommandSource src) {
        HudSync.broadcast(src.getServer());
    }

    /** Group 23: count a successful create toward achievements + fire the one-time create hint (no-op for console). */
    private static void onCreated(ServerCommandSource src, String id) {
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            AchievementManager.recordBlockCreated(p);
            FirstUseHints.onFirstCreate(p, id);
        }
    }

    /** Group 23: count a successful retexture toward the first_texture achievement (no-op for console). */
    private static void onTextured(ServerCommandSource src) {
        if (src.getEntity() instanceof ServerPlayerEntity p) AchievementManager.recordTextureApplied(p);
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
        Chat.successWith(src, name == null
                        ? "Block \"" + id + "\" created successfully."
                        : "Block \"" + id + "\" (\"" + name + "\") created successfully.",
                Chat.editButton(id)); // G04-VIEW: ⊙ View deleted — a create line carries ✎ Edit only
        CategoryCommands.suggestOnCreate(src, d); // Group 11: one-click category hint (opt-out via config)
        onCreated(src, id); // Group 23: achievement + first-create hint
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

        // Group 05 B3: count this image op so a burst collapses into ONE reload (run endOp after updatePack).
        final Runnable endOp = ResourcePackServer.beginImageOp();
        Thread worker = new Thread(() -> {
            try {
                // AI/Pollinations links generate on the fly (slow first hit) → use the longer timeout + retry.
                byte[] raw = AiTextureGenerator.isAiUrl(url)
                        ? ImageDownloader.download(url, AiTextureGenerator.FETCH_TIMEOUT_SECONDS)
                        : ImageDownloader.download(url);
                // Group 14 — if the download is an actually-animated GIF/WebP, build an animated block
                // (vertical strip + .mcmeta) and stop here. Returns false → fall through to static.
                if (AnimCommands.maybeCreateAnimated(src, id, name, raw, url, server, postApply)) { endOp.run(); return; }
                byte[] cleaned = BackgroundRemover.apply(raw, CustomBlocksConfig.backgroundMode,
                        CustomBlocksConfig.backgroundTolerance);
                byte[] png = ImageProcessor.toBlockPng(cleaned, CustomBlocksConfig.textureSize);
                // Studio "background" colour fills behind the image's transparent pixels (else snap-to-black).
                png = bgArgb != null ? ImageProcessor.fillBackground(png, bgArgb)
                        : BackgroundRemover.snapBackgroundBlack(png, CustomBlocksConfig.backgroundMode, CustomBlocksConfig.backgroundTolerance);
                final byte[] finalPng = png;
                // §5c heads-up if the picture is smaller than the block size (computed off-thread, shown below).
                final String smallNote = ImageProcessor.smallSourceNote(raw, CustomBlocksConfig.textureSize);
                final boolean flatChecker = CheckerboardDetector.isFlattened(raw); // G10-6: flattened-preview source?
                server.execute(() -> {
                    try {
                        // Re-check on the server thread — the id could have been taken while downloading.
                        SlotData d = SlotManager.create(id, name);
                        if (d == null) {
                            Chat.error(src, "Couldn't create \"" + id + "\" — the id was taken or every slot "
                                    + "is in use. Nothing was created.");
                            return;
                        }
                        TextureStore.save(d.index(), finalPng);
                        TextureStore.saveSource(d.index(), raw);
                        TextureStore.saveUrl(d.index(), url); // remember the link so the studio can show it later
                        UndoManager.recordCreate(actor(src), d);
                        if (postApply != null) postApply.accept(d); // studio shape/attrs before the rebuild
                        ResourcePackServer.updatePack();
                        if (smallNote != null) Chat.info(src, smallNote); // §5c small-source heads-up
                        if (flatChecker) Chat.info(src, FLAT_CHECKER_NOTE); // G10-6 heads-up
                        Chat.successWith(src, "Block \"" + id + "\"" + (name == null ? "" : " (\"" + name + "\")")
                                        + " created" + (CustomBlocksConfig.silentPack
                                        ? " — it'll show in a moment."
                                        : " — accept the resource pack prompt to see it."),
                                Chat.editButton(id)); // G04-VIEW: ⊙ View deleted — ✎ Edit only
                        onCreated(src, id); // Group 23: achievement + first-create hint
                        syncHud(src);
                    } finally { endOp.run(); } // Group 05 B3: drop the count AFTER updatePack committed
                });
            } catch (Exception e) {
                String code = IncidentRecorder.record("Create-with-texture failed for \"" + id + "\" (url: " + url + ")",
                        id, src.getName(), e);
                server.execute(() -> Chat.incidentError(src, "Couldn't get an image from that URL, so the block was NOT created.", code));
                endOp.run(); // Group 05 B3: download failed — release the hold
            }
        }, "CustomBlocks-CreateTexture");
        worker.setDaemon(true);
        worker.start();
    }

    private static int rename(CommandContext<ServerCommandSource> ctx, String id, String name) {
        ServerCommandSource src = ctx.getSource();
        SlotData before = SlotManager.getById(id);
        if (before == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        if (LockManager.isLocked(id)) {
            Chat.lockedError(src, id);
            return 0;
        }
        SlotData d = SlotManager.rename(id, name);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        UndoManager.recordModify(actor(src), before, d, "rename");
        Chat.successWith(src, "Renamed \"" + id + "\" to \"" + name + "\".",
                Chat.editButton(id), Chat.undoButton());
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
        Chat.successWith(src, "Duplicated \"" + id + "\" into a new block \"" + newId + "\".",
                Chat.editButton(newId), Chat.undoButton());
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
            Chat.lockedError(src, id);
            return 0;
        }
        applyTexture(src, id, d.index(), url);
        return 1;
    }

    /**
     * Record a completed /cb retexture so /cb undo can revert it. {@code before} is the pre-edit slot
     * snapshot (carries the old AnimData) and {@code beforeTex} its old pixels; the after-slot is read
     * live (now carries the new AnimData) and {@code afterTex} is the new pixels — so undo/redo restore
     * BOTH the pixels and the animated/static flag. Skips when nothing changed or there's no actor.
     */
    private static void recordRetexture(ServerCommandSource src, SlotData before, byte[] beforeTex,
                                        String id, byte[] afterTex) {
        UndoManager.recordRetexture(actor(src), before, SlotManager.getById(id), beforeTex, afterTex);
    }

    /**
     * Download + decode the image off the server thread, then hop back to the server
     * thread to rebuild and push the resource pack. Only caller is /cb retexture.
     *
     * Group 14 Step 1 — handles the animated↔static transition the same way the studio does
     * (StudioReskin): an animated GIF/WebP retextures to an animated block (off-atlas grid strip
     * + fresh AnimData), and a still image clears any prior animation so a once-animated block
     * doesn't keep a dead anim flag. Previously every URL was baked as a still (Bug A).
     *
     * Failures go to the triggering player AND the incidents log so admins can review
     * errors they didn't see live (Group 04 major-error routing).
     */
    private static void applyTexture(ServerCommandSource src, String id, int index, String url) {
        MinecraftServer server = src.getServer();
        Chat.info(src, "Downloading texture for \"" + id + "\"…");

        // Undo snapshot (captured on the server thread BEFORE the async worker overwrites anything): the
        // pre-edit slot (carries the old AnimData) + old pixels, so /cb undo restores BOTH pixels and the
        // animated/static flag — not just the pixels (which would leave a stale anim flag, the old Bug A).
        SlotData beforeSlot = SlotManager.getById(id);
        byte[] beforeTex = TextureStore.load(index);

        // Group 05 B3: count this image op so a burst collapses into ONE reload (run endOp after updatePack).
        final Runnable endOp = ResourcePackServer.beginImageOp();
        Thread worker = new Thread(() -> {
            try {
                // AI/Pollinations links generate on the fly (slow first hit) → use the longer timeout + retry.
                byte[] raw = AiTextureGenerator.isAiUrl(url)
                        ? ImageDownloader.download(url, AiTextureGenerator.FETCH_TIMEOUT_SECONDS)
                        : ImageDownloader.download(url);
                // Animated source → strip + fresh AnimData. (decode null/1-frame → fall through to static.)
                if (AnimationDecoder.isAnimated(raw)) {
                    // Off-atlas grid → decode at the full requested cell size (decoder caps only for its
                    // memory budget). No 256 atlas cap, no frame-dropping — same as create + studio.
                    int cellSize = Math.min(AnimationDecoder.OFFATLAS_MAX_SIZE, Math.max(64, CustomBlocksConfig.textureSize));
                    AnimationDecoder.Decoded dec = AnimationDecoder.decode(raw, cellSize);
                    if (dec != null && dec.frameCount() > 1) {
                        server.execute(() -> {
                            try {
                                TextureStore.save(index, dec.stripPng());
                                TextureStore.saveSource(index, raw); // keep the GIF/WebP for later re-decode
                                TextureStore.saveUrl(index, url);    // remember the link for the studio
                                SlotManager.setAnim(id, AnimData.ofDecoded(dec.frameCount(), dec.frameTimes(), dec.frameTimesMs(), dec.transparency()));
                                recordRetexture(src, beforeSlot, beforeTex, id, dec.stripPng());
                                ResourcePackServer.updatePack();
                                Chat.success(src, "Texture applied to \"" + id + "\" — now animated ("
                                        + dec.frameCount() + " frames)." + (CustomBlocksConfig.silentPack
                                        ? " It'll show in a moment." : " Accept the resource pack prompt to see it."));
                                if (dec.warning() != null) Chat.info(src, dec.warning());
                                onTextured(src); // Group 23: first_texture achievement
                            } finally { endOp.run(); } // Group 05 B3: drop the count AFTER updatePack committed
                        });
                        return;
                    }
                }
                // Static source → bake a square block texture (M1: strip the background to opaque black
                // first, BEFORE the resize/pad so corner sampling reads the real background).
                byte[] cleaned = BackgroundRemover.apply(raw, CustomBlocksConfig.backgroundMode,
                        CustomBlocksConfig.backgroundTolerance);
                byte[] png = ImageProcessor.toBlockPng(cleaned, CustomBlocksConfig.textureSize);
                // Restore a true black after the resize blends the edges (no-op when off).
                png = BackgroundRemover.snapBackgroundBlack(png, CustomBlocksConfig.backgroundMode,
                        CustomBlocksConfig.backgroundTolerance);
                // §5c heads-up if the picture is smaller than the block size (computed off-thread, shown below).
                final String smallNote = ImageProcessor.smallSourceNote(raw, CustomBlocksConfig.textureSize);
                final boolean flatChecker = CheckerboardDetector.isFlattened(raw); // G10-6: flattened-preview source?
                TextureStore.save(index, png);
                // Keep the ORIGINAL image so the block can later be re-rendered at a different
                // texture size from real pixels (see the retexture-all NOTE on retexture()).
                TextureStore.saveSource(index, raw);
                TextureStore.saveUrl(index, url); // remember the link for the studio
                final byte[] afterTex = png; // effectively-final copy for the undo record below
                server.execute(() -> {
                    try {
                        SlotData cur = SlotManager.getById(id);
                        if (cur != null && cur.isAnimated()) SlotManager.setAnim(id, AnimData.NONE); // still image → no animation
                        recordRetexture(src, beforeSlot, beforeTex, id, afterTex);
                        ResourcePackServer.updatePack();
                        if (smallNote != null) Chat.info(src, smallNote); // §5c small-source heads-up
                        if (flatChecker) Chat.info(src, FLAT_CHECKER_NOTE); // G10-6 heads-up
                        Chat.success(src, CustomBlocksConfig.silentPack
                                ? "Texture applied to \"" + id + "\". It'll show on the block in a moment."
                                : "Texture applied to \"" + id + "\" — accept the resource pack prompt to see it.");
                        onTextured(src); // Group 23: first_texture achievement
                    } finally { endOp.run(); } // Group 05 B3: drop the count AFTER updatePack committed
                });
            } catch (Exception e) {
                String code = IncidentRecorder.record("Texture download failed for \"" + id + "\" (url: " + url + ")",
                        id, src.getName(), url, e);
                server.execute(() -> Chat.incidentError(src, "Couldn't get a texture from that URL.", code));
                endOp.run(); // Group 05 B3: download failed — release the hold
            }
        }, "CustomBlocks-Retexture");
        worker.setDaemon(true);
        worker.start();
    }

}
