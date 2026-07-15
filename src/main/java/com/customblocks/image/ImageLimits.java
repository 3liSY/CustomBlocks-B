/**
 * ImageLimits.java
 *
 * Responsibility: Shared safety limits for user-provided images before they enter expensive
 * decode/recolour rails. Keeps command code small and gives friendly errors instead of letting a
 * very large source image push the server into an OOM or long CPU stall.
 */
package com.customblocks.image;

public final class ImageLimits {

    /** Colour-family commands bake four textures from one source, so they use a tighter cap. */
    public static final int COLOR_FAMILY_MAX_BYTES = 12 * 1024 * 1024;
    private static final int COLOR_FAMILY_TIMEOUT_SECONDS = 20;
    private static final int COLOR_FAMILY_MAX_EDGE = 4096;
    private static final long COLOR_FAMILY_MAX_PIXELS = 12_000_000L;

    private ImageLimits() {} // static-only

    /** Download and validate a static source for /cb colorvariants. */
    public static byte[] downloadColorFamilySource(String url) throws Exception {
        byte[] raw = ImageDownloader.download(url, COLOR_FAMILY_TIMEOUT_SECONDS, COLOR_FAMILY_MAX_BYTES);
        requireColorFamilySource(raw);
        return raw;
    }

    private static void requireColorFamilySource(byte[] raw) throws Exception {
        int[] dim = ImageProcessor.dimensions(raw);
        if (dim == null) {
            throw new Exception("Could not read that image. Use a direct PNG, JPG, GIF, or WebP image link.");
        }
        int w = dim[0], h = dim[1];
        long pixels = (long) w * h;
        if (w > COLOR_FAMILY_MAX_EDGE || h > COLOR_FAMILY_MAX_EDGE || pixels > COLOR_FAMILY_MAX_PIXELS) {
            throw new Exception("That image is too large for a colour family (" + w + "x" + h
                    + "). Use an image up to " + COLOR_FAMILY_MAX_EDGE + "px on a side.");
        }
    }
}
