/**
 * TempRetextureCommands.java — GROUP_14 §S TEMPORARY (remove when the logo re-source job is done).
 *
 * /cb tempretexture all          — bake every assistant-sourced HD logo (ResourceLogos.LIST) onto its base
 *                                  block in ONE off-thread pass, full safety backup first, pack rebuilt once.
 * /cb tempretexture undo <id>    — restore that one block to its TRUE original (per-block snapshot).
 * /cb tempretexture undo all     — restore every block the batch touched.
 * /cb tempretexture clear        — drop the snapshots once the job is signed off.
 *
 * The batch-apply twin of /cb sourcewall: the wall shows the re-source backlog, this fills it from the
 * sourced list, the owner reviews and undoes any miss. Only STATIC blocks are touched (animated GIFs are
 * skipped) so undo is exactly texture+source+link. This whole command + ResourceLogos + TempRetextureBackup
 * are deleted at job close (closeout task), same as SourceWallCommands.
 *
 * Depends on: ResourceLogos, TempRetextureBackup, SlotManager, TextureStore, BackgroundRemover/ImageProcessor/
 *             ImageDownloader, BackupManager, ResourcePackServer, Chat. Called by: CommandRegistrar (TEMP).
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.BackupManager;
import com.customblocks.core.IncidentRecorder;
import com.customblocks.core.ResourceLogos;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TempRetextureBackup;
import com.customblocks.core.TextureStore;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.image.ImageDownloader;
import com.customblocks.image.ImageProcessor;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.ArrayList;
import java.util.List;

public final class TempRetextureCommands {

    private TempRetextureCommands() {} // static-only

    /** One unit of work: a base block index + id + the sourced logo URL to bake onto it. */
    private record Job(int index, String id, String url) {}

    /** Wikimedia rate-limits bursts (HTTP 429). Space the downloads out and retry with backoff so a
     *  many-logo batch doesn't lose most of its fetches to throttling. */
    private static final long THROTTLE_MS = 500L; // gap before each download
    private static final int  MAX_TRIES   = 4;    // 1 try + 3 backoff retries (1s, 2s, 3s)

    /** Download with retry: on any failure (429 throttle, transient 5xx, timeout) wait, then try again. */
    private static byte[] downloadWithRetry(String url) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= MAX_TRIES; attempt++) {
            try {
                return ImageDownloader.download(url);
            } catch (Exception e) {
                last = e;
                if (attempt < MAX_TRIES) Thread.sleep(1000L * attempt); // 1s → 2s → 3s
            }
        }
        throw last;
    }

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("tempretexture")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("all").executes(ctx -> all(ctx.getSource())))
                .then(CommandManager.literal("clear").executes(ctx -> clear(ctx.getSource())))
                .then(CommandManager.literal("undo")
                        .executes(ctx -> {
                            Chat.error(ctx.getSource(), "Use " + CbFmt.BODY + "/cb tempretexture undo <id>" + CbFmt.RESET + " for one block, or " + CbFmt.BODY + "/cb tempretexture undo all" + CbFmt.RESET + ".");
                            return 0;
                        })
                        .then(CommandManager.literal("all").executes(ctx -> undoAll(ctx.getSource())))
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .suggests(BlockSuggestions.IDS)
                                .executes(ctx -> undoOne(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))));
    }

    // ── all ──────────────────────────────────────────────────────────────────────────────────────────

    private static int all(ServerCommandSource src) {
        MinecraftServer server = src.getServer();
        if (server == null) return 0;
        if (ResourceLogos.LIST.isEmpty()) {
            Chat.error(src, "No sourced logos are bundled in this jar yet — nothing to apply.");
            return 0;
        }
        // Resolve the sourced list against THIS server's blocks on the server thread. A logo whose id has
        // no block here is skipped (the owner may not have made it); animated blocks are left alone so undo
        // stays a clean texture+source swap.
        List<Job> jobs = new ArrayList<>();
        int missing = 0, animated = 0;
        for (ResourceLogos.Logo logo : ResourceLogos.LIST) {
            SlotData d = SlotManager.getById(logo.id());
            if (d == null) { missing++; continue; }
            if (d.isAnimated()) { animated++; continue; }
            jobs.add(new Job(d.index(), logo.id(), logo.url()));
        }
        if (jobs.isEmpty()) {
            Chat.error(src, "None of the " + ResourceLogos.LIST.size() + " sourced logos match a block here ("
                    + missing + " missing, " + animated + " animated). Run /cb sourcewall to see the real ids.");
            return 0;
        }

        SlotManager.saveAll(); // flush so the safety backup copies a current slots.json
        final String backupName = BackupManager.timestampName("pre-tempretexture");
        final int blockCount = SlotManager.assignedSlots().size();
        final String mode = CustomBlocksConfig.backgroundMode;
        final int size = CustomBlocksConfig.textureSize;
        final int fMissing = missing, fAnimated = animated;
        Chat.info(src, "Applying " + CbFmt.VALUE + jobs.size() + CbFmt.RESET + " sourced logo(s)…");

        Thread worker = new Thread(() -> {
            // Safety net FIRST — never overwrite the only copy of a block's pixels without a restore point.
            try {
                BackupManager.save(backupName, blockCount, BackupManager.Kind.SAFETY, "pre-tempretexture", null);
            } catch (Exception e) {
                IncidentRecorder.record("tempretexture aborted: pre-change safety backup failed", null, src.getName(), e);
                server.execute(() -> Chat.error(src, "Stopped — couldn't make a safety backup first, so "
                        + CbFmt.BOLD + "nothing was changed" + CbFmt.RESET + ". Your blocks are untouched. (Check the log, then try again.)"));
                return;
            }
            int ok = 0;
            List<String> failed = new ArrayList<>();
            for (Job j : jobs) {
                try {
                    TempRetextureBackup.snapshot(j.index()); // capture the TRUE original BEFORE overwriting (first time only)
                    Thread.sleep(THROTTLE_MS); // Wikimedia throttles bursts (HTTP 429) — pace the downloads
                    byte[] raw = downloadWithRetry(j.url());
                    byte[] cleaned = BackgroundRemover.apply(raw, mode);
                    byte[] png = ImageProcessor.toBlockPng(cleaned, size);
                    png = BackgroundRemover.snapBackgroundBlack(png, mode);
                    TextureStore.save(j.index(), png);
                    TextureStore.saveSource(j.index(), raw); // keep the HD source so /cb retextureall can re-bake later
                    TextureStore.saveUrl(j.index(), j.url());
                    ok++;
                } catch (Exception e) {
                    failed.add(j.id());
                }
            }
            final int fOk = ok;
            final List<String> fFailed = failed;
            server.execute(() -> {
                ResourcePackServer.updatePack(); // ONE rebuild after the whole batch (§7)
                if (!fFailed.isEmpty()) {
                    IncidentRecorder.record("tempretexture: " + fFailed.size() + " logo(s) failed to download/bake: "
                            + String.join(", ", fFailed), null, src.getName(), null);
                }
                String fail = fFailed.isEmpty() ? "" : " " + CbFmt.BAD + fFailed.size() + CbFmt.RESET + " failed (" + String.join(", ", fFailed) + ")";
                String skip = (fMissing > 0 || fAnimated > 0)
                        ? " " + CbFmt.DIM + "(skipped " + fMissing + " not-on-server, " + fAnimated + " animated)" : "";
                Chat.success(src, "Logos applied — " + CbFmt.OK + fOk + CbFmt.RESET + " block(s)" + fail + skip + ". "
                        + (CustomBlocksConfig.silentPack ? "They update in a moment." : "Accept the resource-pack prompt to see them.")
                        + " " + CbFmt.DIM + "Don't like one? " + CbFmt.BODY + "/cb tempretexture undo <id>" + CbFmt.DIM + " · all back: " + CbFmt.BODY + "/cb tempretexture undo all" + CbFmt.DIM + " · or " + CbFmt.BODY + "/cb backup restore " + backupName);
            });
        }, "CustomBlocks-TempRetexture");
        worker.setDaemon(true);
        worker.start();
        return 1;
    }

    // ── undo ─────────────────────────────────────────────────────────────────────────────────────────

    private static int undoOne(ServerCommandSource src, String id) {
        SlotData d = SlotManager.getById(id);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        if (!TempRetextureBackup.has(d.index())) {
            Chat.error(src, "\"" + id + "\" has no tempretexture backup — /cb tempretexture all didn't change it.");
            return 0;
        }
        if (!TempRetextureBackup.restore(d.index())) {
            Chat.error(src, "Couldn't restore \"" + id + "\" — check the log.");
            return 0;
        }
        ResourcePackServer.updatePack();
        Chat.success(src, "Restored \"" + id + "\" to its original. "
                + (CustomBlocksConfig.silentPack ? "It updates in a moment." : "Accept the resource-pack prompt."));
        return 1;
    }

    private static int undoAll(ServerCommandSource src) {
        List<Integer> indices = TempRetextureBackup.snapshotted();
        if (indices.isEmpty()) {
            Chat.error(src, "No tempretexture backups to undo.");
            return 0;
        }
        int restored = 0;
        for (int index : indices) if (TempRetextureBackup.restore(index)) restored++;
        ResourcePackServer.updatePack();
        Chat.success(src, "Restored " + CbFmt.OK + restored + CbFmt.RESET + " of " + indices.size() + " block(s) to their originals. "
                + (CustomBlocksConfig.silentPack ? "They update in a moment." : "Accept the resource-pack prompt."));
        return 1;
    }

    // ── clear ────────────────────────────────────────────────────────────────────────────────────────

    private static int clear(ServerCommandSource src) {
        int n = TempRetextureBackup.clearAll();
        Chat.success(src, "Cleared " + CbFmt.BODY + n + CbFmt.RESET + " tempretexture backup(s). (Run this only once the logo job is signed off — "
                + "undo stops working for them after.)");
        return 1;
    }
}
