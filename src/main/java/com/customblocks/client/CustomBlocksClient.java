/**
 * CustomBlocksClient.java
 *
 * Responsibility: Client mod entrypoint (Fabric ClientModInitializer). The single
 * place where client-side payload receivers, screens, and the client texture cache
 * are registered.
 *
 * Phase 0 (foundation): only logs a client startup line.
 * Wired in incrementally:
 *   Phase 5  → payload receivers + client ResourcePackGenerator + TextureCache
 *   Phase 11 → HUD overlay + client-only screens
 *
 * Depends on: CustomBlocksMod (shared logger)
 * Called by:  Fabric loader via the "client" entrypoint in fabric.mod.json
 */
package com.customblocks.client;

import com.customblocks.CustomBlocksMod;
import net.fabricmc.api.ClientModInitializer;

public class CustomBlocksClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CustomBlocksMod.LOGGER.info("[CustomBlocks] Client initializing (Phase 0 — foundation).");
    }
}
