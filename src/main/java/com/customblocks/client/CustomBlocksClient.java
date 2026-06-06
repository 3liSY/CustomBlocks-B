/**
 * CustomBlocksClient.java
 *
 * Responsibility: Client mod entrypoint (Fabric ClientModInitializer).
 * Registers payload receivers and initialises the client-side HUD config.
 *
 * Phase 10/11 additions:
 *   - OpenGuiPayload  → opens the correct GUI screen based on mode
 *   - HudSyncPayload  → populates ClientSlotCache for the HUD renderer
 *   - HudConfig.syncFromConfig() called on init
 *
 * Depends on: CustomBlocksMod (shared logger), all gui/screens, ClientSlotCache, HudConfig
 * Called by: Fabric loader via the "client" entrypoint in fabric.mod.json
 */
package com.customblocks.client;

import com.customblocks.CustomBlocksMod;
import com.customblocks.gui.GuiMode;
import com.customblocks.gui.screens.ArabicBrowserScreen;
import com.customblocks.gui.screens.BlockEditorScreen;
import com.customblocks.gui.screens.ConfigScreen;
import com.customblocks.gui.screens.MacroListScreen;
import com.customblocks.gui.screens.MainMenuScreen;
import com.customblocks.network.payloads.HudSyncPayload;
import com.customblocks.network.payloads.OpenGuiPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Environment(EnvType.CLIENT)
public class CustomBlocksClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CustomBlocksMod.LOGGER.info("[CustomBlocks] Client initializing (Phase 10/11 — GUI + HUD).");

        HudConfig.syncFromConfig();

        // OpenGuiPayload → open the right screen
        ClientPlayNetworking.registerGlobalReceiver(OpenGuiPayload.ID, (payload, context) -> {
            GuiMode mode = GuiMode.fromId(payload.mode());
            String data  = payload.data();
            context.client().execute(() -> {
                switch (mode) {
                    case MAIN_MENU      -> context.client().setScreen(new MainMenuScreen());
                    case BLOCK_EDITOR   -> context.client().setScreen(new BlockEditorScreen(data));
                    case CONFIG         -> context.client().setScreen(new ConfigScreen());
                    case MACRO_LIST     -> context.client().setScreen(new MacroListScreen());
                    case ARABIC_BROWSER -> context.client().setScreen(new ArabicBrowserScreen());
                    default             -> context.client().setScreen(new MainMenuScreen());
                }
            });
        });

        // HudSyncPayload → populate ClientSlotCache
        ClientPlayNetworking.registerGlobalReceiver(HudSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientSlotCache.populate(payload.indexJson())));
    }
}
