/**
 * FaceCommands.java
 *
 * Responsibility: The per-face paint commands (Group 06 / M4):
 *   /cb paintface <id> <face> <url>  — download an image and put it on ONE face of the block
 *                                      (same clean-up pipeline as /cb retexture; the other
 *                                      faces keep showing the base texture)
 *   /cb clearface <id> <face|all>    — remove face override(s), back to the base texture
 * The Rainbow Rectangle's right-click pre-fills "/cb paintface <id> <face> " in chat, so the
 * player only pastes the URL (ChatPrefillPayload).
 *
 * Depends on: SlotManager/SlotData, TextureStore (saveFace/deleteFace), LockManager,
 *             ImageDownloader, BackgroundRemover, ImageProcessor, ResourcePackServer,
 *             IncidentRecorder, BlockSuggestions, Chat
 * Called by:  CommandRegistrar; item/RainbowRectangleItem (via the chat prefill)
 */
package com.customblocks.command.handlers;

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
        root.then(CommandManager.literal("paintface")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .then(CommandManager.argument("face", StringArgumentType.word())
                                .suggests((c, b) -> { for (String f : TextureStore.FACES) b.suggest(f); return b.buildFuture(); })
                                .then(CommandManager.argument("url", StringArgumentType.greedyString())
                                        .executes(ctx -> paintFace(ctx,
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
    }

    private static boolean validFace(String face) {
        for (String f : TextureStore.FACES) if (f.equals(face)) return true;
        return false;
    }

    private static int paintFace(CommandContext<ServerCommandSource> ctx, String id, String faceRaw, String url) {
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
            Chat.error(src, "\"" + id + "\" is locked. Use /cb unlock " + id + " to edit it.");
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
                    Chat.success(src, "Painted the §f" + face + "§a face of \"" + id + "\". "
                            + "§7(/cb clearface " + id + " " + face + " undoes it.)");
                });
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                IncidentRecorder.record("Face paint failed for \"" + id + "\" " + face
                        + " (by " + src.getName() + ", url: " + url + ")", e);
                server.execute(() -> Chat.error(src, "Couldn't get a texture from that URL. " + msg));
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
            Chat.error(src, "\"" + id + "\" is locked. Use /cb unlock " + id + " to edit it.");
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
