/**
 * RetextureAllCommands.java
 *
 * Responsibility: the /cb retextureall bulk re-render — re-bake EVERY assigned block at a chosen
 * texture size from its stored source image, off the server thread, rebuilding the pack ONCE at the
 * end (§7: no per-block pack churn). Split out of CreationCommands (§9.3 handler 400-line gate) so the
 * block lifecycle (create/rename/dupe/retexture) and this batch op each stay small + focused.
 *
 * Before it overwrites anything it takes a full safety backup (textures + sources + slots.json) via
 * BackupManager, so `/cb backup restore` can bring every block back if the new size looks worse. If
 * that backup can't be made, the whole op is ABORTED and nothing is touched — we never overwrite the
 * only copy of a block's pixels without a restore point.
 *
 * Triggered by the TextureSizeMenu → RetextureConfirmMenu "Yes" confirm; also usable directly. Blocks
 * with no stored source (made before the source store, or Arabic/video) are LEFT AS-IS when the new size
 * is the same or bigger — upscaling baked pixels only adds blur with no new detail — and are re-baked
 * only when going to a SMALLER size (a real downscale). Animated (GIF) blocks are
 * RE-BAKED too: their saved original is re-decoded at the new cell size and the grid is rebuilt, while
 * the clip's edited speed / loop / smoothing / trim are preserved (Group 14 ADR-014 Step 3 — one
 * command upgrades static AND animated blocks, so GIFs no longer need recreating by hand). A GIF with
 * no saved original (legacy/video) is left as-is — it can't be re-baked without losing quality.
 *
 * Registered into the /cb tree by CommandRegistrar. All mutations go through SlotManager / TextureStore.
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.AnimData;
import com.customblocks.core.BackupManager;
import com.customblocks.core.IncidentRecorder;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.image.AnimationDecoder;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.image.ImageProcessor;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public final class RetextureAllCommands {

    private RetextureAllCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // /cb retextureall [16-512] — re-render every existing block at the given size (default:
        // the current textureSize). Triggered by the TextureSizeMenu "Yes" confirm; usable directly.
        root.then(CommandManager.literal("retextureall")
                .executes(ctx -> retextureAll(ctx.getSource().getServer(), CustomBlocksConfig.textureSize, ctx.getSource()))
                .then(CommandManager.argument("px", IntegerArgumentType.integer(16, 512))
                        .suggests((c, b) -> { b.suggest(16); b.suggest(32); b.suggest(64); b.suggest(128); b.suggest(256); b.suggest(512); return b.buildFuture(); })
                        .executes(ctx -> retextureAll(ctx.getSource().getServer(),
                                IntegerArgumentType.getInteger(ctx, "px"), ctx.getSource()))));
    }

    /**
     * Re-render EVERY assigned block at {@code newSize} from its stored source image, off the
     * server thread, then rebuild the pack ONCE at the end (CLAUDE.md §7: no per-block pack churn).
     * Blocks with no stored source (made before the source store, or Arabic/video) are upscaled
     * from their baked texture instead — no new detail, but they still match the new size. Drives
     * the TextureSizeMenu → RetextureConfirmMenu "Yes" button.
     */
    public static int retextureAll(MinecraftServer server, int newSize, ServerCommandSource src) {
        if (server == null) return 0;
        // Snapshot indices on the caller thread; copy the bg config so the worker reads a stable view.
        java.util.List<Integer> indices = new java.util.ArrayList<>();
        for (SlotData d : SlotManager.assignedSlots()) indices.add(d.index());
        final String mode = CustomBlocksConfig.backgroundMode;
        final int tol = CustomBlocksConfig.backgroundTolerance;
        // Flush slots now (server thread) so the pre-change backup copies a current slots.json, then
        // pick the backup's name. The heavy file copy itself runs off-thread, inside the worker below.
        SlotManager.saveAll();
        final String backupName = BackupManager.timestampName("pre-retextureall");
        final int blockCount = indices.size();
        Chat.info(src, "Retexturing " + indices.size() + " block(s) to " + CbFmt.VALUE + newSize + "px" + CbFmt.RESET + "…");

        Thread worker = new Thread(() -> {
            // Safety net FIRST: snapshot textures + sources + slots.json before overwriting a single
            // file, so /cb backup restore can bring everything back. Heavy copy is fine off-thread (same
            // idiom as AutoBackup). If it fails, ABORT — never overwrite the only copy without a restore point.
            try {
                BackupManager.save(backupName, blockCount, BackupManager.Kind.SAFETY, "pre-retextureall", null);
            } catch (Exception e) {
                IncidentRecorder.record("Retexture-all aborted: pre-change safety backup failed",
                        null, src.getName(), e);
                server.execute(() -> Chat.error(src, "Stopped — couldn't make a safety backup first, so "
                        + CbFmt.BOLD + "nothing was changed" + CbFmt.RESET + ". Your blocks are untouched. (Check the log, then try again.)"));
                return;
            }
            int rerendered = 0, downscaled = 0, kept = 0, skipped = 0, animated = 0;
            // GIF re-bakes: the strip PNG is written here (disk I/O is safe off the server thread), but
            // the AnimData swap is a synchronized slot mutation that must run on the server thread — so
            // collect the re-decoded frame data per slot and apply it in the final server.execute below.
            java.util.Map<Integer, AnimRebake> animUpdates = new java.util.HashMap<>();
            for (int index : indices) {
                try {
                    // Group 14 ADR-014 Step 3 — re-bake animated blocks too: re-decode the saved ORIGINAL
                    // gif at the new cell size and rebuild the grid. A GIF with no saved original (legacy/
                    // video) can't be re-baked without quality loss, so it's left untouched.
                    if (SlotManager.animFor(index).isAnimated()) {
                        byte[] gif = TextureStore.loadSource(index);
                        if (gif == null || gif.length == 0) { animated++; continue; }
                        int cell = Math.min(AnimationDecoder.OFFATLAS_MAX_SIZE, Math.max(64, newSize));
                        AnimationDecoder.Decoded dec = AnimationDecoder.decode(gif, cell);
                        if (dec == null || dec.frameCount() <= 1) { animated++; continue; }
                        TextureStore.save(index, dec.stripPng());
                        animUpdates.put(index, new AnimRebake(dec.frameCount(), dec.frameTimes(),
                                dec.frameTimesMs(), dec.transparency()));
                        continue;
                    }
                    byte[] raw = TextureStore.loadSource(index);
                    if (raw != null && raw.length > 0) {
                        byte[] cleaned = BackgroundRemover.apply(raw, mode, tol);
                        byte[] png = ImageProcessor.toBlockPng(cleaned, newSize);
                        png = BackgroundRemover.snapBackgroundBlack(png, mode, tol);
                        TextureStore.save(index, png);
                        rerendered++;
                    } else {
                        // No saved original. The only pixels we have are the already-baked texture, so
                        // UPscaling it would just add blur with zero new detail — that's the bug the owner
                        // hit. So: leave it as-is when the new size is >= its current size (no blur), and
                        // only re-bake when going SMALLER (a real downscale, no blur, fewer texels).
                        byte[] baked = TextureStore.load(index);
                        if (baked == null || baked.length == 0) {
                            skipped++;
                        } else if (newSize >= ImageProcessor.pngWidth(baked)) {
                            kept++; // would only upscale → keep the sharper current texture untouched
                        } else {
                            TextureStore.save(index, ImageProcessor.toBlockPng(baked, newSize));
                            downscaled++;
                        }
                    }
                } catch (Exception e) {
                    skipped++;
                }
            }
            final int fr = rerendered, fd = downscaled, fk = kept, fs = skipped, fa = animated;
            server.execute(() -> {
                // Apply each re-baked GIF's AnimData on the server thread (synchronized + persisted),
                // preserving the clip's edited speed / loop / smoothing / trim; only the frame data is
                // refreshed from the new decode. withTrim() re-clamps if a long clip kept fewer frames.
                int reanimated = 0;
                for (var e : animUpdates.entrySet()) {
                    SlotData d = SlotManager.getBySlot("slot_" + e.getKey());
                    if (d == null || !d.isAnimated()) continue;
                    AnimRebake nu = e.getValue();
                    AnimData old = d.anim();
                    AnimData rebuilt = AnimData.ofDecoded(nu.frameCount(), nu.times(), nu.ms(), nu.transparency());
                    if (old.isUniform()) rebuilt = rebuilt.withUniform(old.uniformTicks());
                    rebuilt = rebuilt.withLoopMode(old.loopMode())
                            .withInterpolate(old.interpolate())
                            .withTrim(old.trimStart(), old.trimEnd());
                    SlotManager.setAnim(d.customId(), rebuilt);
                    reanimated++;
                }
                ResourcePackServer.updatePack(); // ONE rebuild after the whole batch (§7)
                if (fs > 0) {
                    IncidentRecorder.record("Retexture-all to " + newSize + "px skipped " + fs
                            + " slot(s) (no source/texture or decode error)", null, src.getName(), null);
                }
                String animNote = "";
                if (reanimated > 0) animNote += " " + CbFmt.VALUE + reanimated + CbFmt.RESET + " GIF(s) re-baked.";
                if (fa > 0) animNote += " " + CbFmt.DIM + fa + CbFmt.RESET + " GIF(s) had no saved original — left as-is.";
                String keptNote = (fk > 0)
                        ? " " + CbFmt.DIM + fk + CbFmt.RESET + " left as-is (no saved original — upscaling would only blur them)." : "";
                Chat.success(src, "Retexture complete — " + CbFmt.OK + fr + CbFmt.RESET + " re-rendered from original, " + CbFmt.VALUE + fd
                        + CbFmt.RESET + " resized smaller, " + CbFmt.DIM + fs + CbFmt.RESET + " skipped." + keptNote + animNote + " "
                        + (CustomBlocksConfig.silentPack
                        ? "Blocks update in a moment." : "Accept the resource-pack prompt to see them.")
                        + " " + CbFmt.DIM + "Don't like it? " + CbFmt.BODY + "/cb backup restore " + backupName + CbFmt.DIM + " puts every block back.");
            });
        }, "CustomBlocks-RetextureAll");
        worker.setDaemon(true);
        worker.start();
        return 1;
    }

    /**
     * Lightweight carrier of a re-decoded GIF's frame data from the worker thread to the server thread.
     * The strip PNG is already written to disk during the worker loop, so only the plain numbers travel
     * here — keeping the big byte[] out of the map so a large batch doesn't pile up in memory.
     */
    private record AnimRebake(int frameCount, java.util.List<Integer> times,
                              java.util.List<Integer> ms, boolean transparency) {}
}
