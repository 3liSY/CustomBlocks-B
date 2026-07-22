/**
 * FaceCommands.java
 *
 * Responsibility: The per-face paint commands (Group 06 / M4):
 *   /cb setface <id> <face> <url>    — download an image and put it on ONE face of the block
 *                                      (same clean-up pipeline as /cb retexture; the other
 *                                      faces keep showing the base texture)
 *   /cb clearface <id> <face|all>    — remove face override(s), back to the base texture
 *   /cb clearallfaces <id>           — alias of "clearface <id> all" (the Group 08 spec name)
 * The Rainbow Rectangle's right-click pre-fills "/cb setface <id> <face> " in chat, so the
 * player only pastes the URL (ChatPrefillPayload).
 *
 * G08 §D cleanup (2026-07-20): the old duplicate `/cb paintface` literal (an identical alias of
 * setface) was removed — setface is the one Group 08 spec name. No behavior change.
 *
 * Depends on: SlotManager/SlotData, TextureStore (saveFace/deleteFace), LockManager,
 *             ImageDownloader, BackgroundRemover, ImageProcessor, ResourcePackServer,
 *             IncidentRecorder, BlockSuggestions, Chat
 * Called by:  CommandRegistrar; item/RainbowRectangleItem (via the chat prefill)
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.IncidentRecorder;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.image.ImageDownloader;
import com.customblocks.image.ImageProcessor;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Locale;

public final class FaceCommands {

    private FaceCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // setface — the one Group 08 spec name for painting a single face (the old duplicate
        // `paintface` alias was removed in the G08 §D cleanup).
        root.then(CommandManager.literal("setface")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .then(CommandManager.argument("face", StringArgumentType.word())
                                .suggests((c, b) -> { for (String f : TextureStore.FACES) b.suggest(f); return b.buildFuture(); })
                                .then(CommandManager.argument("url", StringArgumentType.greedyString())
                                        .executes(ctx -> setFace(ctx,
                                                StringArgumentType.getString(ctx, "id"),
                                                StringArgumentType.getString(ctx, "face"),
                                                StringArgumentType.getString(ctx, "url")))))));

        root.then(CommandManager.literal("clearface")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .then(CommandManager.argument("face", StringArgumentType.word())
                                .suggests((c, b) -> { for (String f : TextureStore.FACES) b.suggest(f); b.suggest("all"); return b.buildFuture(); })
                                .executes(ctx -> clearFace(ctx,
                                        StringArgumentType.getString(ctx, "id"),
                                        StringArgumentType.getString(ctx, "face"))))));

        // clearallfaces — Group 08 spec name; identical to "clearface <id> all".
        root.then(CommandManager.literal("clearallfaces")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> clearFace(ctx,
                                StringArgumentType.getString(ctx, "id"), "all"))));
    }

    private static boolean validFace(String face) {
        for (String f : TextureStore.FACES) if (f.equals(face)) return true;
        return false;
    }

    private static int setFace(CommandContext<ServerCommandSource> ctx, String id, String faceRaw, String url) {
        ServerCommandSource src = ctx.getSource();
        String face = faceRaw.toLowerCase(Locale.ROOT);
        if (!validFace(face)) {
            Chat.error(src, "Pick a face: down, up, north, south, west, or east.");
            return 0;
        }
        SlotData d = SlotManager.getById(id);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        if (LockManager.isLocked(id)) {
            Chat.lockedError(src, id);
            return 0;
        }
        applyFaceTexture(src, id, d.index(), face, url);
        return 1;
    }

    /**
     * Download + clean + resize on a worker thread (the exact /cb retexture pipeline), save as
     * ONE face's override, then hop back for the single pack rebuild — mirrors applyTexture.
     */
    private static void applyFaceTexture(ServerCommandSource src, String id, int index, String face, String url) {
        MinecraftServer server = src.getServer();
        Chat.info(src, "Downloading the " + face + " face for \"" + id + "\"…");
        Thread worker = new Thread(() -> {
            try {
                byte[] raw = ImageDownloader.download(url);
                byte[] cleaned = BackgroundRemover.apply(raw, CustomBlocksConfig.backgroundMode,
                        CustomBlocksConfig.backgroundTolerance);
                byte[] png = ImageProcessor.toBlockPng(cleaned, CustomBlocksConfig.textureSize);
                png = BackgroundRemover.snapBackgroundBlack(png, CustomBlocksConfig.backgroundMode,
                        CustomBlocksConfig.backgroundTolerance);
                TextureStore.saveFace(index, face, png);
                server.execute(() -> {
                    ResourcePackServer.updatePack();
                    Chat.success(src, "Painted the " + CbFmt.BODY + face + CbFmt.OK + " face of \"" + id + "\". "
                            + CbFmt.DIM + "(/cb clearface " + id + " " + face + " undoes it.)");
                });
            } catch (Exception e) {
                String code = IncidentRecorder.record("Face paint failed for \"" + id + "\" " + face
                        + " (url: " + url + ")", id, src.getName(), e);
                server.execute(() -> Chat.incidentError(src, "Couldn't get a texture from that URL.", code));
            }
        }, "CustomBlocks-FacePaint");
        worker.setDaemon(true);
        worker.start();
    }

    private static int clearFace(CommandContext<ServerCommandSource> ctx, String id, String faceRaw) {
        ServerCommandSource src = ctx.getSource();
        String face = faceRaw.toLowerCase(Locale.ROOT);
        SlotData d = SlotManager.getById(id);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        if (LockManager.isLocked(id)) {
            Chat.lockedError(src, id);
            return 0;
        }
        int removed = 0;
        if ("all".equals(face)) {
            for (String f : TextureStore.FACES) if (TextureStore.deleteFace(d.index(), f)) removed++;
        } else if (validFace(face)) {
            if (TextureStore.deleteFace(d.index(), face)) removed++;
        } else {
            Chat.error(src, "Pick a face (down/up/north/south/west/east) or \"all\".");
            return 0;
        }
        if (removed == 0) {
            Chat.info(src, "\"" + id + "\" has no painted " + ("all".equals(face) ? "faces" : face + " face") + ".");
            return 1;
        }
        ResourcePackServer.updatePack();
        Chat.success(src, "Cleared " + removed + " painted face(s) on \"" + id + "\" — back to the base texture.");
        return 1;
    }
}
