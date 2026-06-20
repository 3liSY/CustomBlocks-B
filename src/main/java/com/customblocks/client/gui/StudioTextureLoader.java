/**
 * StudioTextureLoader.java
 *
 * Responsibility: Fetch + decode a Block Creation Studio preview off the UI thread, using the SAME path
 * as the /cb create command — {@link ImageDownloader} (browser User-Agent so CDNs like Wikimedia don't
 * 403, plus Tenor/Giphy share-page resolution). Returns EVERY frame as a cube grid plus the animation's
 * plain numbers ({@link AnimData}), so the studio can play a live preview and edit speed/loop/trim
 * (Group 14 Phase 2). Keeps the network/decode out of the screen class (§9.3 size + §5.6 standalone).
 *
 * It also reads an EXISTING animated block's frame-strip back from the active resource pack (edit mode),
 * so opening a block to edit it shows its real frames without re-downloading.
 *
 * Depends on: ImageDownloader, AnimationDecoder, AnimData, PreviewCube, MinecraftClient (pack read).
 * Called by:  BlockCreationStudioScreen (loadTexture + edit-load, on a daemon thread).
 */
package com.customblocks.client.gui;

import com.customblocks.core.AnimData;
import com.customblocks.image.AnimationDecoder;
import com.customblocks.image.ImageDownloader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class StudioTextureLoader {

    private StudioTextureLoader() {} // static-only

    private static final Logger LOG = LoggerFactory.getLogger("CustomBlocks/AI");

    /** Decode size for preview frames; they downsample to PreviewCube.GRID anyway, so this need not match
     *  the server's texture size — keeping it fixed decouples the client preview from server config. */
    private static final int PREVIEW_SIZE = 128;

    /** Preview result: every frame as a cube grid, the animation numbers, and whether it's animated. */
    public record Result(int[][] frames, AnimData anim, boolean animated) {}

    /** Download + decode {@code url} for the preview cube. Returns null on any failure (→ "URL failed"). */
    public static Result load(String url) {
        return load(url, 0);
    }

    /**
     * Same as {@link #load(String)} but with a custom fetch timeout (seconds). {@code timeoutSeconds > 0}
     * marks the AI preview path: it uses the longer-timeout downloader AND logs WHY a fetch failed (a
     * timeout, 404, or no-internet all used to surface as the same silent grey "couldn't generate" badge).
     */
    public static Result load(String url, int timeoutSeconds) {
        try {
            byte[] raw = timeoutSeconds > 0
                    ? ImageDownloader.download(url, timeoutSeconds)
                    : ImageDownloader.download(url);
            if (AnimationDecoder.isAnimated(raw)) {
                AnimationDecoder.Decoded dec = AnimationDecoder.decode(raw, PREVIEW_SIZE);
                if (dec != null && dec.frameCount() > 1) {
                    int[][] frames = splitStrip(read(dec.stripPng()), dec.frameCount());
                    if (frames != null) {
                        AnimData anim = AnimData.ofDecoded(dec.frameCount(), dec.frameTimes(), dec.transparency());
                        return new Result(frames, anim, true);
                    }
                }
                // decode failed → fall through to a static first-frame preview
            }
            BufferedImage img = read(raw); // first frame for a static preview
            if (img == null) return null;
            return new Result(new int[][]{PreviewCube.downsample(img)}, AnimData.NONE, false);
        } catch (Exception e) {
            if (timeoutSeconds > 0) // AI path: surface the real reason instead of a silent grey badge
                LOG.warn("[CustomBlocks] AI texture fetch failed for {} : {}", url, e.toString());
            return null;
        }
    }

    /**
     * Read an existing block's frame grids back from the active resource pack (edit mode). For an
     * animated block this is the stored vertical strip split into {@code frameCount} squares; for a
     * static block it's the single texture. Returns null if the pack texture isn't available.
     */
    public static int[][] loadFromPack(int index, int frameCount) {
        try {
            Identifier id = Identifier.of("customblocks", "textures/block/slot_" + index + ".png");
            Optional<Resource> res = MinecraftClient.getInstance().getResourceManager().getResource(id);
            if (res.isEmpty()) return null;
            try (InputStream in = res.get().getInputStream()) {
                BufferedImage img = ImageIO.read(in);
                if (img == null) return null;
                int n = Math.max(1, frameCount);
                return n <= 1 ? new int[][]{PreviewCube.downsample(img)} : splitStrip(img, n);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /** Split a vertical frame-strip (w wide, w·N tall) into N downsampled cube grids. */
    private static int[][] splitStrip(BufferedImage strip, int frameCount) {
        if (strip == null || frameCount <= 0) return null;
        int w = strip.getWidth();
        int fh = strip.getHeight() / frameCount;
        if (w <= 0 || fh <= 0) return null;
        int[][] grids = new int[frameCount][];
        for (int i = 0; i < frameCount; i++)
            grids[i] = PreviewCube.downsample(strip.getSubimage(0, i * fh, w, fh));
        return grids;
    }

    private static BufferedImage read(byte[] bytes) throws Exception {
        return bytes == null ? null : ImageIO.read(new ByteArrayInputStream(bytes));
    }
}
