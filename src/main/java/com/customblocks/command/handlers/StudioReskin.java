/**
 * StudioReskin.java — Group 14 "edit everything": re-skin an EXISTING block from a new image url.
 *
 * The Block Creation Studio is a full editor, so "Save changes" can swap a block's picture/GIF, not just
 * its settings. This is the texture side of that: download + decode the new image OFF the server thread,
 * and only on success hop back to the server thread to overwrite the slot's texture, fix its animated flag,
 * and rebuild the pack ONCE. A broken/non-image link leaves the block untouched (mirrors
 * CreationCommands.createWithTexture). The slot INDEX never moves, so already-placed blocks (the registry
 * slot_N blocks) keep their new look automatically.
 *
 * Handles the animated↔static transition:
 *   • animated source → save the vertical strip + a FRESH AnimData (the new clip's own frame timing);
 *   • static source   → bake a square texture and CLEAR any prior animation (no "animated flag, still image"
 *                       mismatch). {@code bgArgb} (null = none) fills behind the image's transparent pixels.
 *
 * Undo (Group 14 Bucket 1): the whole "Save changes" — the new picture/anim AND the settings the caller
 * applied before this ran — is recorded as ONE reversible RETEXTURE op, mirroring /cb retexture. The caller
 * passes the PRE-edit slot snapshot + its old pixels ({@code beforeSlot}/{@code beforeTex}); a null snapshot
 * (e.g. after a rename, whose id move owns its own history) skips the recording. /cb undo restores the old
 * slot (incl. AnimData + settings) + old pixels; /cb redo re-applies the re-skin.
 *
 * Depends on: ImageDownloader, AnimationDecoder, BackgroundRemover, ImageProcessor, TextureStore,
 *             SlotManager, AnimData, UndoManager, ResourcePackServer, HudSync, Chat, IncidentRecorder,
 *             CustomBlocksConfig.
 * Called by:  CreationStudioBridge.saveFromStudio (when "Save changes" carries a new url).
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.AnimData;
import com.customblocks.core.IncidentRecorder;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.UndoManager;
import com.customblocks.image.AnimationDecoder;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.image.ImageDownloader;
import com.customblocks.image.ImageProcessor;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class StudioReskin {

    private StudioReskin() {} // static-only

    /**
     * Re-skin the block {@code id} (slot {@code index}) with the image at {@code url}, off the server thread.
     * Settings/rename are applied by the caller BEFORE this runs; this only swaps the texture + anim flag and
     * does the single pack rebuild. {@code bgArgb} = null means "no background fill". {@code beforeSlot} +
     * {@code beforeTex} are the pre-edit snapshot/pixels the success path records for undo (null = no undo).
     */
    public static void apply(ServerPlayerEntity player, ServerCommandSource src, String id, int index,
                             String url, Integer bgArgb, SlotData beforeSlot, byte[] beforeTex) {
        MinecraftServer server = src.getServer();
        Chat.info(src, "Fetching the new image for \"" + id + "\"…");

        Thread worker = new Thread(() -> {
            try {
                byte[] raw = ImageDownloader.download(url);
                // Animated source → strip + fresh AnimData. (decode returns null/1-frame → fall through to static.)
                if (AnimationDecoder.isAnimated(raw)) {
                    // Off-atlas grid → decode at the full requested cell size (decoder caps only for its memory
                    // budget). No 256 atlas cap, no frame-dropping — same as create.
                    int cellSize = Math.min(AnimationDecoder.OFFATLAS_MAX_SIZE, Math.max(64, CustomBlocksConfig.textureSize));
                    AnimationDecoder.Decoded dec = AnimationDecoder.decode(raw, cellSize);
                    if (dec != null && dec.frameCount() > 1) {
                        server.execute(() -> finishAnimated(player, src, id, index, raw, dec, beforeSlot, beforeTex, url));
                        return;
                    }
                }
                // Static source → bake a square block texture (with optional background fill).
                byte[] cleaned = BackgroundRemover.apply(raw, CustomBlocksConfig.backgroundMode,
                        CustomBlocksConfig.backgroundTolerance);
                byte[] png = ImageProcessor.toBlockPng(cleaned, CustomBlocksConfig.textureSize);
                png = bgArgb != null ? ImageProcessor.fillBackground(png, bgArgb)
                        : BackgroundRemover.snapBackgroundBlack(png, CustomBlocksConfig.backgroundMode,
                          CustomBlocksConfig.backgroundTolerance);
                final byte[] finalPng = png;
                server.execute(() -> finishStatic(player, src, id, index, raw, finalPng, beforeSlot, beforeTex, url));
            } catch (Exception e) {
                String code = IncidentRecorder.record("Studio re-skin failed for \"" + id + "\" (url: " + url + ")",
                        id, src.getName(), e);
                server.execute(() -> Chat.incidentError(src, "Couldn't get an image from that URL, so the picture was NOT changed (other settings were saved).", code));
            }
        }, "CustomBlocks-StudioReskin");
        worker.setDaemon(true);
        worker.start();
    }

    /** Server thread: store the new strip + source, set a fresh animation, record undo, rebuild once. */
    private static void finishAnimated(ServerPlayerEntity player, ServerCommandSource src, String id, int index,
                                       byte[] raw, AnimationDecoder.Decoded dec, SlotData beforeSlot, byte[] beforeTex,
                                       String url) {
        TextureStore.save(index, dec.stripPng());
        TextureStore.saveSource(index, raw); // keep the GIF/WebP for later re-decode
        TextureStore.saveUrl(index, url);    // remember the link for the studio
        SlotManager.setAnim(id, AnimData.ofDecoded(dec.frameCount(), dec.frameTimes(), dec.frameTimesMs(), dec.transparency()));
        // Record AFTER the slot carries its new AnimData (so the redo snapshot round-trips), like applyTexture.
        UndoManager.recordRetexture(player.getUuid(), beforeSlot, SlotManager.getById(id), beforeTex, dec.stripPng());
        finish(player, src, id, "Updated \"" + id + "\" — now animated (" + dec.frameCount() + " frames).");
        if (dec.warning() != null) Chat.info(src, dec.warning());
    }

    /** Server thread: store the new texture + source, drop any prior animation, record undo, rebuild once. */
    private static void finishStatic(ServerPlayerEntity player, ServerCommandSource src, String id, int index,
                                     byte[] raw, byte[] png, SlotData beforeSlot, byte[] beforeTex, String url) {
        TextureStore.save(index, png);
        TextureStore.saveSource(index, raw);
        TextureStore.saveUrl(index, url); // remember the link for the studio
        SlotData cur = SlotManager.getById(id);
        if (cur != null && cur.isAnimated()) SlotManager.setAnim(id, AnimData.NONE); // still image → no animation
        UndoManager.recordRetexture(player.getUuid(), beforeSlot, SlotManager.getById(id), beforeTex, png);
        finish(player, src, id, "Updated the picture on \"" + id + "\".");
    }

    private static void finish(ServerPlayerEntity player, ServerCommandSource src, String id, String okMsg) {
        ResourcePackServer.updatePack();
        Chat.success(src, okMsg + (CustomBlocksConfig.silentPack ? "" : " Accept the pack prompt to see it."));
        HudSync.broadcast(player.getServer()); // NO-REJOIN: reskin shows live for all players (was actor-only)
    }
}
