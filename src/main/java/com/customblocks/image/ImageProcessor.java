/**
 * ImageProcessor.java
 *
 * Responsibility: Decode arbitrary image bytes and produce a square PNG suitable for a
 * block texture — aspect ratio preserved, centered, with transparent padding, scaled with a
 * high-quality Lanczos-3 resample (ImageResampler) plus a light sharpen when enlarging, so a small
 * source picture looks far less mushy than the old bicubic stretch (Group 14 §5c, "Option B").
 *
 * Phase 4: core resize. BackgroundRemover (3 modes) + ColorReplacer come next.
 *
 * Called by: the retexture command.
 */
package com.customblocks.image;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

public final class ImageProcessor {

    private ImageProcessor() {} // static-only

    /** Sharpen strength after an ENLARGE. Owner-approved from the tools/render_preview sheets, 2026-07-25. */
    private static final float ENLARGE_SHARPEN = 0.60f;
    /** Sharpen strength after a SHRINK — lighter, because a down-scale is already the crisper direction. */
    private static final float SHRINK_SHARPEN = 0.35f;
    /** How far a sharpened pixel may pass its 3×3 neighbourhood, as a fraction of that neighbourhood's
     *  contrast. Flat colour has no contrast, so it gets no allowance and cannot grow a halo. */
    private static final float SHARPEN_OVERSHOOT = 0.20f;

    /**
     * Produce a {@code size}x{@code size} ARGB PNG from arbitrary image bytes.
     * {@code size} should be a power of two (16/32/64/128) for clean mipmapping.
     */
    public static byte[] toBlockPng(byte[] input, int size) throws Exception {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(input));
        if (src == null) {
            throw new Exception("Could not read that image — the data isn't a recognized image "
                    + "(supported: PNG, JPG, GIF, WebP). The link may point to a web page, not a direct image.");
        }
        int sw = src.getWidth();
        int sh = src.getHeight();
        double scale = Math.min((double) size / sw, (double) size / sh);
        int dw = Math.max(1, (int) Math.round(sw * scale));
        int dh = Math.max(1, (int) Math.round(sh * scale));

        // High-quality Lanczos resample (premultiplied alpha → no transparent-edge halo) followed by a
        // light sharpen, in BOTH directions (G10 §G, owner 2026-07-25 — "soft when a 447px picture is
        // stretched up AND when a 954×1484 one is shrunk down"). Shrinking used to get no sharpen at all,
        // and the enlarge sharpen was mostly cancelled by a hard clamp, so both baked mushy. Down-scales
        // take the lighter amount because Lanczos is already crisper there and over-sharpening a shrink
        // just aliases. A picture that is ALREADY the block size is never sharpened — no resize ran.
        BufferedImage scaled = ImageResampler.resize(src, dw, dh);
        boolean resized = dw != sw || dh != sh;
        if (resized) {
            float amount = scale > 1.0 ? ENLARGE_SHARPEN : SHRINK_SHARPEN;
            scaled = ImageResampler.unsharpMask(scaled, amount, SHARPEN_OVERSHOOT);
        }

        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        int dx = (size - dw) / 2;
        int dy = (size - dh) / 2;
        g.drawImage(scaled, dx, dy, null); // 1:1 paste of the already-sized image — no rescale here
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(out, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * Read just an image's pixel dimensions from its header (no full decode), as {@code {width, height}},
     * or {@code null} if the bytes aren't a readable image.
     */
    public static int[] dimensions(byte[] input) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
            if (iis == null) return null;
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                return new int[]{ reader.getWidth(0), reader.getHeight(0) };
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            return null;
        }
    }

    // G10 §G (owner, 2026-07-25): smallSourceNote() is gone — the "that picture is only N×Npx … it was
    // enlarged, so fine details may look soft" chat line was removed as noise. Enlarging still happens
    // in toBlockPng exactly as before; only the notice went.

    /**
     * Width in pixels of a PNG, read straight from its IHDR header — no full decode. Our baked block
     * textures are always square PNGs, so this is the cheapest way to learn a slot's current texture
     * size (used by /cb retextureall to skip a pointless upscale). Returns 0 if {@code png} isn't a
     * readable PNG, so callers should treat 0 as "unknown — don't touch it".
     */
    public static int pngWidth(byte[] png) {
        // PNG = 8-byte signature, then the IHDR chunk: 4-byte length, "IHDR", 4-byte width (big-endian).
        if (png == null || png.length < 24) return 0;
        if ((png[0] & 0xFF) != 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') return 0;
        return ((png[16] & 0xFF) << 24) | ((png[17] & 0xFF) << 16)
                | ((png[18] & 0xFF) << 8) | (png[19] & 0xFF);
    }

    /**
     * Height in pixels of a PNG, read straight from its IHDR header — no full decode. Companion to
     * {@link #pngWidth(byte[])}; the height word sits at byte offset 20 (big-endian). Returns 0 when
     * {@code png} isn't a readable PNG, so callers treat 0 as "unknown — don't touch it".
     */
    public static int pngHeight(byte[] png) {
        if (png == null || png.length < 24) return 0;
        if ((png[0] & 0xFF) != 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') return 0;
        return ((png[20] & 0xFF) << 24) | ((png[21] & 0xFF) << 16)
                | ((png[22] & 0xFF) << 8) | (png[23] & 0xFF);
    }

    /**
     * Composite a PNG over an opaque {@code bgArgb} background of the same size: every transparent or
     * semi-transparent pixel takes (some of) the background colour. Used by the Block Creation Studio's
     * "background colour" so a logo with transparent areas sits on a solid backdrop instead of replacing
     * the image. {@code bgArgb} should be fully opaque (0xFFrrggbb).
     */
    public static byte[] fillBackground(byte[] png, int bgArgb) throws Exception {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(png));
        if (src == null) throw new Exception("Could not read the texture to apply a background colour.");
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setColor(new java.awt.Color(0xFF000000 | (bgArgb & 0xFFFFFF), true));
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(out, "PNG", baos);
        return baos.toByteArray();
    }
}
