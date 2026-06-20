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
 * No undo is recorded — texture changes aren't undoable anywhere in the mod (cf. /cb retexture), and
 * recording one here would revert the anim flag without reverting the texture (a render mismatch).
 *
 * Depends on: ImageDownloader, AnimationDecoder, BackgroundRemover, ImageProcessor, TextureStore,
 *             SlotManager, AnimData, ResourcePackServer, HudSync, Chat, IncidentRecorder, CustomBlocksConfig.
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
     * does the single pack rebuild. {@code bgArgb} = null means "no background fill".
     */
    public static void apply(ServerPlayerEntity player, ServerCommandSource src, String id, int index,
                             String url, Integer bgArgb) {
        MinecraftServer server = src.getServer();
        Chat.info(src, "Fetching the new image for \"" + id + "\"…");

        Thread worker = new Thread(() -> {
            try {
                byte[] raw = ImageDownloader.download(url);
                // Animated source → strip + fresh AnimData. (decode returns null/1-frame → fall through to static.)
                if (AnimationDecoder.isAnimated(raw)) {
                    // Atlas strip → cap at 256 so a 512 config can't overflow the block atlas (the muffle).
                    int atlasSize = Math.min(AnimationDecoder.ATLAS_MAX_SIZE, CustomBlocksConfig.textureSize);
                    AnimationDecoder.Decoded dec = AnimationDecoder.decode(raw, atlasSize);
                    if (dec != null && dec.frameCount() > 1) {
                        server.execute(() -> finishAnimated(player, src, id, index, raw, dec));
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
                server.execute(() -> finishStatic(player, src, id, index, raw, finalPng));
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                IncidentRecorder.record("Studio re-skin failed for \"" + id + "\" (by "
                        + src.getName() + ", url: " + url + ")", e);
                server.execute(() -> Chat.error(src,
                        "Couldn't get an image from that URL, so the picture was NOT changed (other settings were saved). " + msg));
            }
        }, "CustomBlocks-StudioReskin");
        worker.setDaemon(true);
        worker.start();
    }

    /** Server thread: store the new strip + source, set a fresh animation, rebuild once. */
    private static void finishAnimated(ServerPlayerEntity player, ServerCommandSource src, String id, int index,
                                       byte[] raw, AnimationDecoder.Decoded dec) {
        TextureStore.save(index, dec.stripPng());
        TextureStore.saveSource(index, raw); // keep the GIF/WebP for later re-decode
        SlotManager.setAnim(id, AnimData.ofDecoded(dec.frameCount(), dec.frameTimes(), dec.transparency()));
        finish(player, src, id, "Updated \"" + id + "\" — now animated (" + dec.frameCount() + " frames).");
        if (dec.warning() != null) Chat.info(src, dec.warning());
    }

    /** Server thread: store the new texture + source, drop any prior animation, rebuild once. */
    private static void finishStatic(ServerPlayerEntity player, ServerCommandSource src, String id, int index,
                                     byte[] raw, byte[] png) {
        TextureStore.save(index, png);
        TextureStore.saveSource(index, raw);
        SlotData cur = SlotManager.getById(id);
        if (cur != null && cur.isAnimated()) SlotManager.setAnim(id, AnimData.NONE); // still image → no animation
        finish(player, src, id, "Updated the picture on \"" + id + "\".");
    }

    private static void finish(ServerPlayerEntity player, ServerCommandSource src, String id, String okMsg) {
        ResourcePackServer.updatePack();
        Chat.success(src, okMsg + (CustomBlocksConfig.silentPack ? "" : " Accept the pack prompt to see it."));
        HudSync.sendTo(player);
    }
}
