/**
 * LowResScaler.java — GROUP 05 §E / G05-5 (per-client low-res texture mode). CLIENT-SIDE ONLY.
 *
 * Responsibility: downscale one pack PNG so neither side exceeds the chosen size, preserving aspect
 * ratio. Aspect-preserving matters: a static cube_all texture (512x512) becomes size x size — the
 * atlas-overflow fix — while an animated frame GRID or a legacy vertical STRIP scales uniformly, so
 * the per-frame geometry the client derives from cols + image width stays valid. Only PNGs under our
 * textures/ dir are touched; models/blockstates/mcmeta/pack.mcmeta pass through untouched.
 *
 * Pure Java (ImageIO + ImageResampler) so it behaves identically on any client. Never throws into the
 * sync loop — on any decode/encode problem it returns the original bytes so the pack still applies.
 *
 * Depends on: ImageResampler (Lanczos-3 resize, anti-ringing), javax.imageio.
 * Called by:  ClientPackReceiver (on the low-res worker thread, per synced file).
 */
package com.customblocks.client.lowres;

import com.customblocks.CustomBlocksMod;
import com.customblocks.image.ImageResampler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Environment(EnvType.CLIENT)
public final class LowResScaler {

    private LowResScaler() {} // static-only

    /** Only PNG textures under our pack's textures/ dir are shrunk (models/json/mcmeta untouched). */
    public static boolean isShrinkable(String path) {
        if (path == null) return false;
        String p = path.replace('\\', '/').toLowerCase();
        return p.endsWith(".png") && p.contains("/textures/");
    }

    /**
     * Downscale {@code png} so neither side exceeds {@code maxSize}, keeping aspect ratio. Returns the
     * ORIGINAL bytes when the image already fits (both sides <= maxSize) or can't be decoded/encoded.
     */
    public static byte[] shrink(byte[] png, int maxSize) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(png));
            if (src == null) return png; // not a decodable image — leave it alone
            int w = src.getWidth(), h = src.getHeight();
            if (w <= maxSize && h <= maxSize) return png; // already small enough
            double s = (double) maxSize / Math.max(w, h);
            int dw = Math.max(1, (int) Math.round(w * s));
            int dh = Math.max(1, (int) Math.round(h * s));
            BufferedImage out = ImageResampler.resize(toArgb(src), dw, dh);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(out, "png", bos);
            return bos.toByteArray();
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] low-res shrink failed; keeping full-size PNG.", e);
            return png;
        }
    }

    /** ImageResampler reads via getRGB; force TYPE_INT_ARGB so indexed/gray PNGs resample correctly. */
    private static BufferedImage toArgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) return src;
        BufferedImage argb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return argb;
    }
}
