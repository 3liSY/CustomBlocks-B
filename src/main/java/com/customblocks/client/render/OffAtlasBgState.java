/**
 * OffAtlasBgState.java — Group 14 / Phase 1c Step 2b. CLIENT-ONLY.
 *
 * Client-side flag for the off-atlas background mode: false (default) = transparent/letterbox pixels are
 * flattened onto solid BLACK; true = they stay see-through. Read by StaticFrameCache / AnimFrameCache when
 * they build a slot texture (whether to call OffAtlasImage.compositeOverBlack).
 *
 * The server pushes the value via TransparentBgPayload on join + on /cb config transparent. Default is
 * FALSE (black) so a block is never accidentally see-through before the server speaks; reset to false on
 * disconnect so another server's preference never bleeds across (mirrors SilentPackState).
 *
 * Changing the value clears both off-atlas caches so every placed block + hand/inventory icon re-builds in
 * the new mode the next frame.
 *
 * Depends on: AnimFrameCache, StaticFrameCache (cache clear on change)
 * Called by: CustomBlocksClient (set on payload / disconnect), StaticFrameCache + AnimFrameCache (read)
 */
package com.customblocks.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class OffAtlasBgState {

    private static volatile boolean transparent = false;

    private OffAtlasBgState() {} // static-only

    public static boolean isTransparent() { return transparent; }

    /** Set the mode; if it actually changed, drop the off-atlas caches so textures rebuild in that mode. */
    public static void set(boolean value) {
        if (value == transparent) return;
        transparent = value;
        AnimFrameCache.clear();
        StaticFrameCache.clear();
    }
}
