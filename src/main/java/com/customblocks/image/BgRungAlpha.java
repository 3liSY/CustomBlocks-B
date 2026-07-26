/**
 * BgRungAlpha.java — cascade rung 1: the file's own authored alpha channel (G10 §H).
 *
 * <p>The most certain rung in the cascade, because it is not an estimate at all: the person who made
 * the picture already recorded which pixels are background. Rung 1 declines only when the file has
 * nothing to say — no alpha, or an alpha channel that is one constant value everywhere (a fully
 * opaque or fully transparent sheet carries no authored information to read).
 *
 * <p>Rung 1 also owns the {@code tRNS} trap (§H requirement). Java's PNG reader silently DISCARDS a
 * {@code tRNS} chunk on greyscale and truecolour PNGs, so the decoded image comes back fully opaque
 * even though the file plainly declared a transparent colour. Concluding "no alpha" from that decode
 * would be wrong. On those two colour types {@code tRNS} names a single fully-transparent COLOUR,
 * which is exactly what rung 2 consumes, so the fact is not lost — it is handed to the rung that can
 * use it as a known key. That is why {@link #trnsKey} reads the raw bytes rather than the decode.
 *
 * Depends on: BackgroundRemover.OPAQUE_THRESHOLD (the project's one alpha cutoff).
 * Called by:  image/BgCascade.
 */
package com.customblocks.image;

final class BgRungAlpha {

    private BgRungAlpha() {} // static-only

    /** PNG colour type 0 — greyscale. A {@code tRNS} chunk here names one transparent grey level. */
    private static final int COLOUR_TYPE_GREY = 0;
    /** PNG colour type 2 — truecolour. A {@code tRNS} chunk here names one transparent RGB triple. */
    private static final int COLOUR_TYPE_RGB = 2;

    /**
     * Rung 1's proposed mask, or {@code null} when the file's alpha carries no information.
     * A pixel is background where its alpha falls below the project's opacity cutoff.
     */
    static boolean[][] mask(int[] px, int w, int h) {
        // Cheap pass first: a constant alpha channel is the common case (every JPEG, every opaque PNG),
        // and finding out costs one scan with no allocation. Only a file that actually varies pays for
        // the mask array — which on a large picture is megabytes.
        final int first = px[0] >>> 24;
        boolean varies = false;
        for (int i = 0; i < px.length; i++) {
            if ((px[i] >>> 24) != first) { varies = true; break; }
        }
        if (!varies) return null; // a single constant alpha is not authored intent

        boolean[][] mask = new boolean[w][h];
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                mask[x][y] = ((px[row + x] >>> 24) & 0xFF) < BackgroundRemover.OPAQUE_THRESHOLD;
            }
        }
        return mask;
    }

    /**
     * The opaque RGB a PNG's {@code tRNS} chunk declares fully transparent, or {@code null} when the
     * bytes are not a PNG, carry no {@code tRNS}, or are of a colour type whose transparency Java
     * decodes correctly on its own (indexed palettes and true alpha channels).
     *
     * <p>Walks the chunk list directly: 8-byte signature, then repeating
     * {@code length(4) type(4) data(length) crc(4)}. Only IHDR's colour type and tRNS's data are read.
     */
    static Integer trnsKey(byte[] raw) {
        if (raw == null || raw.length < 8) return null;
        if ((raw[0] & 0xFF) != 137 || raw[1] != 'P' || raw[2] != 'N' || raw[3] != 'G') return null;

        int colourType = -1;
        int p = 8;
        while (p + 8 <= raw.length) {
            int len = be32(raw, p);
            if (len < 0 || p + 12L + len > raw.length) return null; // truncated or absurd chunk
            int dataAt = p + 8;
            if (isType(raw, p + 4, 'I', 'H', 'D', 'R')) {
                if (len < 10) return null;
                colourType = raw[dataAt + 9] & 0xFF; // IHDR: width(4) height(4) bitDepth(1) colourType(1)
            } else if (isType(raw, p + 4, 't', 'R', 'N', 'S')) {
                // Both cases store 16-bit samples; the high byte is the value at bit depth 8.
                if (colourType == COLOUR_TYPE_GREY && len >= 2) {
                    int g = raw[dataAt] & 0xFF;
                    return 0xFF000000 | (g << 16) | (g << 8) | g;
                }
                if (colourType == COLOUR_TYPE_RGB && len >= 6) {
                    int r = raw[dataAt] & 0xFF;
                    int g = raw[dataAt + 2] & 0xFF;
                    int b = raw[dataAt + 4] & 0xFF;
                    return 0xFF000000 | (r << 16) | (g << 8) | b;
                }
                return null; // indexed or alpha-channel PNG — Java decodes those correctly
            } else if (isType(raw, p + 4, 'I', 'D', 'A', 'T')) {
                return null; // tRNS must precede IDAT; past here there is none
            }
            p = dataAt + len + 4;
        }
        return null;
    }

    private static boolean isType(byte[] b, int at, char c0, char c1, char c2, char c3) {
        return b[at] == c0 && b[at + 1] == c1 && b[at + 2] == c2 && b[at + 3] == c3;
    }

    private static int be32(byte[] b, int at) {
        return ((b[at] & 0xFF) << 24) | ((b[at + 1] & 0xFF) << 16)
                | ((b[at + 2] & 0xFF) << 8) | (b[at + 3] & 0xFF);
    }
}
