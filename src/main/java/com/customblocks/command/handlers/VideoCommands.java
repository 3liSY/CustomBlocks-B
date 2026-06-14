/**
 * VideoCommands.java
 *
 * Responsibility: /cb video subcommands — list MP4 files and extract a specific
 * frame as the texture for a custom block.
 * Video files must be placed in config/customblocks/videos/.
 * Extraction runs on a daemon thread (never blocks the server tick).
 *
 * Depends on: VideoDecoder, TextureStore, ResourcePackServer, SlotManager, Chat
 * Called by: CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.network.ResourcePackServer;
import com.customblocks.video.VideoDecoder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.io.File;
import java.nio.file.Path;

public final class VideoCommands {

    private VideoCommands() {}

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        var video = CommandManager.literal("video");

        // /cb video list — list .mp4 files in the videos folder
        video.executes(ctx -> listVideos(ctx.getSource()));
        video.then(CommandManager.literal("list")
                .executes(ctx -> listVideos(ctx.getSource())));

        // /cb video extract <file> <id> <frame>
        video.then(CommandManager.literal("extract")
                .then(CommandManager.argument("file", StringArgumentType.word())
                        .suggests((c, b) -> {
                            File[] files = videosDir(c.getSource().getServer())
                                    .listFiles((f, n) -> n.endsWith(".mp4"));
                            if (files != null) {
                                String typed = b.getRemaining().toLowerCase(java.util.Locale.ROOT);
                                for (File f : files) {
                                    String name = f.getName().substring(0, f.getName().length() - 4);
                                    if (name.toLowerCase(java.util.Locale.ROOT).startsWith(typed)) b.suggest(name);
                                }
                            }
                            return b.buildFuture();
                        })
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .suggests(BlockSuggestions.IDS)
                                .then(CommandManager.argument("frame", IntegerArgumentType.integer(0))
                                        .executes(ctx -> extract(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "file"),
                                                StringArgumentType.getString(ctx, "id"),
                                                IntegerArgumentType.getInteger(ctx, "frame")))))));

        root.then(video);
    }

    private static int listVideos(ServerCommandSource src) {
        File dir = videosDir(src.getServer());
        dir.mkdirs();
        File[] files = dir.listFiles((f, n) -> n.endsWith(".mp4"));
        if (files == null || files.length == 0) {
            Chat.info(src, "No .mp4 files found in config/customblocks/videos/");
            Chat.info(src, "Place MP4 files there, then use: /cb video extract <file> <id> <frame>");
            return 1;
        }
        Chat.info(src, "Videos in config/customblocks/videos/ (" + files.length + "):");
        for (File f : files) {
            src.sendMessage(net.minecraft.text.Text.literal(
                    "  §e" + f.getName().replace(".mp4", "") + "§7  (" + (f.length() / 1024) + " KB)"));
        }
        return 1;
    }

    private static int extract(ServerCommandSource src, String filename, String id, int frame) {
        SlotData d = SlotManager.getById(id);
        if (d == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        if (LockManager.isLocked(id)) {
            Chat.error(src, "'" + id + "' is locked — /cb unlock " + id + " first");
            return 0;
        }
        File videoFile = videoFile(src.getServer(), filename);
        if (!videoFile.exists()) {
            Chat.error(src, "File not found: config/customblocks/videos/" + filename + ".mp4");
            return 0;
        }
        MinecraftServer server = src.getServer();
        int index = d.index();
        Chat.info(src, "Extracting frame " + frame + " from " + filename + "…");
        Thread worker = new Thread(() -> {
            try {
                byte[] png = VideoDecoder.extractFrameAsPng(videoFile, frame);
                TextureStore.save(index, png);
                server.execute(() -> {
                    ResourcePackServer.updatePack();
                    Chat.success(src, "Frame " + frame + " applied to " + id + " — accept the pack prompt");
                });
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                server.execute(() -> Chat.error(src, "Video extract failed: " + msg));
            }
        }, "CustomBlocks-VideoExtract");
        worker.setDaemon(true);
        worker.start();
        return 1;
    }

    private static File videosDir(MinecraftServer server) {
        return Path.of("config/customblocks/videos").toFile();
    }

    private static File videoFile(MinecraftServer server, String name) {
        String safe = name.replaceAll("[^a-zA-Z0-9_\\-]", "");
        return videosDir(server).toPath().resolve(safe + ".mp4").toFile();
    }
}
