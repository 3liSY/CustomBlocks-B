/**
 * SafetyCommands.java — Group 09, Slice 5 (broken-blocks report + safety check).
 *
 *   /cb showbrokenblocks  — open a GUI listing blocks whose baked texture is missing (renders purple).
 *   /cb safety            — a read-only data-safety summary (blocks, backups, auto-backup, trash, broken).
 *
 * The only mutating path is guiRebake (the GUI's auto-fix): it re-renders a broken block's texture from
 * its SAVED SOURCE image — deterministic, no network — reusing the exact pipeline that /cb retexture and
 * retexture-all use (BackgroundRemover → ImageProcessor → TextureStore → pack rebuild). Blocks with no
 * saved source can't be auto-fixed; the GUI points the player at /cb retexture instead.
 *
 * Depends on: BrokenBlockScanner, SlotManager, SlotData, TextureStore, ImageProcessor, BackgroundRemover,
 *             ResourcePackServer, BackupManager, TrashManager, CustomBlocksConfig, GuiRouter, Nav, Chat.
 * Called by:  CommandRegistrar; BrokenBlocksMenu (guiRebake).
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.BackupManager;
import com.customblocks.core.BrokenBlockScanner;
import com.customblocks.core.IncidentRecorder;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.TrashManager;
import com.customblocks.gui.chest.BrokenSelection;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.image.ImageProcessor;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class SafetyCommands {

    private SafetyCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("showbrokenblocks").executes(SafetyCommands::openBroken));
        root.then(CommandManager.literal("safety").executes(SafetyCommands::safety));
    }

    // ── /cb showbrokenblocks ──────────────────────────────────────────────────
    private static int openBroken(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, MenuKey.of(Dest.BROKEN_LIST));
            return 1;
        }
        Chat.info(src, "Broken blocks (missing texture): " + BrokenBlockScanner.count()
                + " (open in-game with /cb showbrokenblocks to fix them).");
        return 1;
    }

    // ── /cb safety ────────────────────────────────────────────────────────────
    private static int safety(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, MenuKey.of(Dest.SAFETY)); // advanced dashboard for players
            return 1;
        }
        return chatSafety(src); // console / command-block fallback
    }

    private static int chatSafety(ServerCommandSource src) {
        int used = SlotManager.usedSlots();
        int max = SlotManager.getMaxSlots();
        int backups = BackupManager.list().size();
        String newest = BackupManager.latestName();
        int trash = TrashManager.list().size();
        int broken = BrokenBlockScanner.count();
        int iv = CustomBlocksConfig.autoBackupInterval;

        Chat.info(src, "Data-safety check:");
        src.sendFeedback(() -> Text.literal("  §7Blocks: §f" + used + "§7/§f" + max), false);
        src.sendFeedback(() -> Text.literal("  §7Backups: §f" + backups
                + (newest != null ? " §8(newest: " + newest + ")" : " §8(none yet)")), false);
        src.sendFeedback(() -> Text.literal("  §7Auto-backup: " + (iv <= 0 ? "§cOFF"
                : "§aevery " + iv + " min §7(keep " + CustomBlocksConfig.autoBackupKeepCount + ")")), false);
        src.sendFeedback(() -> Text.literal("  §7Trash: §f" + trash + " §7deleted block(s)"), false);

        if (broken == 0) {
            src.sendFeedback(() -> Text.literal("  §7Broken blocks: §a0 ✔"), false);
        } else {
            Chat.line(src, Text.literal("  §7Broken blocks: §c" + broken + " §7— ")
                    .append(Chat.runButton("§e[open]", "/cb showbrokenblocks", "List and fix them")));
        }
        if (backups == 0) {
            Chat.line(src, Text.literal("§7Tip: make a backup — ")
                    .append(Chat.runButton("§a[/cb backup save]", "/cb backup save", "Save a backup now")));
        }
        return 1;
    }

    // ── GUI auto-fix: re-bake a broken block from its saved source image ───────
    public static void guiRebake(ServerPlayerEntity player, String id) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ServerCommandSource src = player.getCommandSource();
        SlotData d = SlotManager.getById(id);
        if (d == null) { Chat.error(src, "There's no block called \"" + id + "\" any more."); return; }
        byte[] raw = TextureStore.loadSource(d.index());
        if (raw == null || raw.length == 0) {
            Chat.error(src, "\"" + id + "\" has no saved image to rebuild from. Retexture it with "
                    + "/cb retexture " + id + " <url>.");
            return;
        }
        Chat.info(src, "Rebuilding texture for \"" + id + "\" from its saved image…");
        final int index = d.index();
        final String mode = CustomBlocksConfig.backgroundMode;
        final int tol = CustomBlocksConfig.backgroundTolerance;
        final int size = CustomBlocksConfig.textureSize;
        Thread worker = new Thread(() -> {
            try {
                byte[] cleaned = BackgroundRemover.apply(raw, mode, tol);
                byte[] png = ImageProcessor.toBlockPng(cleaned, size);
                png = BackgroundRemover.snapBackgroundBlack(png, mode, tol);
                TextureStore.save(index, png);
                server.execute(() -> {
                    ResourcePackServer.updatePack();
                    ResourcePackServer.syncToAll();
                    Chat.success(src, "Rebuilt the texture for \"" + id + "\" from its saved image.");
                    GuiRouter.render(player, MenuKey.of(Dest.BROKEN_LIST)); // refresh the report
                });
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                IncidentRecorder.record("Broken-block rebake failed for \"" + id + "\"", e);
                server.execute(() -> Chat.error(src, "Couldn't rebuild that texture. " + msg));
            }
        }, "CustomBlocks-RebakeFix");
        worker.setDaemon(true);
        worker.start();
    }

    /** Re-bake many broken blocks from their saved sources in ONE batch, then rebuild the pack once. */
    public static void guiRebakeMany(ServerPlayerEntity player, java.util.List<String> ids) {
        MinecraftServer server = player.getServer();
        if (server == null || ids == null || ids.isEmpty()) return;
        ServerCommandSource src = player.getCommandSource();
        // Snapshot indices on the caller (server) thread; bake off-thread.
        java.util.List<Integer> indices = new java.util.ArrayList<>();
        for (String id : ids) {
            SlotData d = SlotManager.getById(id);
            if (d != null) indices.add(d.index());
        }
        final String mode = CustomBlocksConfig.backgroundMode;
        final int tol = CustomBlocksConfig.backgroundTolerance;
        final int size = CustomBlocksConfig.textureSize;
        Chat.info(src, "Rebuilding up to " + indices.size() + " texture(s) from saved images…");
        Thread worker = new Thread(() -> {
            int fixed = 0, skipped = 0;
            for (int index : indices) {
                try {
                    byte[] raw = TextureStore.loadSource(index);
                    if (raw == null || raw.length == 0) { skipped++; continue; }
                    byte[] cleaned = BackgroundRemover.apply(raw, mode, tol);
                    byte[] png = ImageProcessor.toBlockPng(cleaned, size);
                    png = BackgroundRemover.snapBackgroundBlack(png, mode, tol);
                    TextureStore.save(index, png);
                    fixed++;
                } catch (Exception e) {
                    skipped++;
                }
            }
            final int ff = fixed, fs = skipped;
            server.execute(() -> {
                ResourcePackServer.updatePack(); // ONE rebuild after the whole batch (§7)
                ResourcePackServer.syncToAll();
                Chat.success(src, "Rebuilt " + ff + " texture(s)"
                        + (fs > 0 ? " §7(" + fs + " had no saved image — skipped)" : "") + ".");
                BrokenSelection.clear(player.getUuid());
                GuiRouter.render(player, MenuKey.of(Dest.BROKEN_LIST));
            });
        }, "CustomBlocks-RebakeMany");
        worker.setDaemon(true);
        worker.start();
    }
}
