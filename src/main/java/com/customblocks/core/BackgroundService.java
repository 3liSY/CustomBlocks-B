/**
 * BackgroundService.java — G10 §C: the ONE re-bake rail for a block's stored background choice.
 *
 * Every surface that changes a background (command, Studio, bulk, Omni-Tool) calls
 * {@link #rebake} so a block ends up with the same pixels no matter which route set it — the
 * "without creating separate background systems" requirement in GROUP_10 §B.
 *
 * Rails, both reusing the existing image chain rather than a new one:
 *   black    — BackgroundRemover.apply → toBlockPng → snapBackgroundBlack. Byte-identical to the
 *              rail every block already bakes on today (CreationCommands, ColorFamilyOps.bakeBase),
 *              so setting a block to black is a no-op re-bake rather than a subtle re-render.
 *   #RRGGBB  — the same chain with the colour as the fill, which is what the colour-variant rail
 *              (ColorFamilyOps.recolourSet) already does.
 *
 * The re-bake reads the block's STORED SOURCE, not its baked texture, so repeated background
 * changes never compound (re-baking a baked texture would bake the old fill into the new one).
 * A block with no stored source cannot be re-baked honestly and is reported as skipped — the
 * spec's "no-source block is skipped with an honest warning instead of a guessed re-bake".
 *
 * Transparent is deliberately NOT handled here: slot blocks are not on the cutout render layer
 * yet (Group 14 owns that), so it would bake alpha the world cannot show. Callers gate on
 * {@link BackgroundValue#renderable} before reaching this class.
 *
 * Depends on: BackgroundValue, TextureStore, BackgroundRemover, ImageProcessor, CustomBlocksConfig.
 * Called by:  BackgroundCommands (single + scope routes).
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.image.ImageProcessor;

public final class BackgroundService {

    private BackgroundService() {} // static-only

    /** Why a re-bake did not happen, so the caller can report it honestly instead of guessing. */
    public enum Skip { NONE, NO_SOURCE, FAILED }

    /** One block's re-bake outcome: the new PNG (null when skipped) and why, if it was skipped. */
    public record Result(byte[] png, Skip skip) {
        public boolean ok() { return png != null; }
    }

    /**
     * Re-bake {@code index}'s texture onto {@code background}. Pure computation plus a texture read —
     * safe to run off the server thread; the caller saves the bytes and rebuilds the pack.
     */
    public static Result rebake(int index, String background) {
        byte[] raw = TextureStore.loadSource(index);
        if (raw == null || raw.length == 0) return new Result(null, Skip.NO_SOURCE);

        String mode = CustomBlocksConfig.backgroundMode;
        int tol = CustomBlocksConfig.backgroundTolerance;
        int size = CustomBlocksConfig.textureSize;
        try {
            if (BackgroundValue.BLACK.equals(background)) {
                // The existing default rail, unchanged — black must not re-render differently.
                byte[] cleaned = BackgroundRemover.apply(raw, mode, tol);
                byte[] png = ImageProcessor.toBlockPng(cleaned, size);
                return new Result(BackgroundRemover.snapBackgroundBlack(png, mode, tol), Skip.NONE);
            }
            int rgb = BackgroundValue.rgb(background);
            byte[] cleaned = BackgroundRemover.apply(raw, mode, tol, rgb);
            byte[] png = ImageProcessor.toBlockPng(cleaned, size);
            png = ImageProcessor.fillBackground(png, rgb); // fills the padding a non-square source leaves
            return new Result(BackgroundRemover.snapBackgroundColor(png, mode, tol, rgb), Skip.NONE);
        } catch (Exception e) {
            return new Result(null, Skip.FAILED);
        }
    }
}
