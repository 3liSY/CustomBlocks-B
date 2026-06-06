/**
 * HudConfig.java
 *
 * Responsibility: Shared configuration for the CustomBlocks HUD overlay —
 * position, visibility, and display style. Reads from CustomBlocksConfig.hudEnabled;
 * can be toggled at runtime via /cb config hud.
 *
 * Depends on: CustomBlocksConfig
 * Called by: HudRenderer, ConfigCommands
 */
package com.customblocks.client;

import com.customblocks.CustomBlocksConfig;

public final class HudConfig {

    private HudConfig() {}

    /** X position of the HUD overlay (pixels from left edge). */
    public static int x = 6;

    /** Y position of the HUD overlay (pixels from top edge). */
    public static int y = 6;

    /** Whether the HUD overlay is currently visible. Mirrors CustomBlocksConfig.hudEnabled at startup. */
    public static boolean visible = true;

    /** Sync the runtime flag from config (call after config reload). */
    public static void syncFromConfig() {
        visible = CustomBlocksConfig.hudEnabled;
    }
}
