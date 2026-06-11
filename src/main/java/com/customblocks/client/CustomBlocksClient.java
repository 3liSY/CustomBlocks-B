/**
 * CustomBlocksClient.java
 *
 * Responsibility: Client mod entrypoint (Fabric ClientModInitializer).
 * Registers payload receivers, loads the persisted HUD config, and injects the
 * CustomBlocks buttons into the vanilla pause/ESC menu.
 *
 * Phase 10/11 + Group 03 additions:
 *   - OpenGuiPayload  → opens the correct GUI screen based on mode (incl. HUD_EDITOR)
 *   - HudSyncPayload  → populates ClientSlotCache for the HUD renderer
 *   - HudStatePayload → toggles + persists HUD visibility
 *   - HudConfig.load() restores saved HUD position/scale/color/opacity on init
 *   - ScreenEvents.AFTER_INIT → inject ESC-menu buttons (EscMenuButtons)
 *
 * Depends on: CustomBlocksMod (logger), gui screens, ClientSlotCache, HudConfig,
 *             HudEditorScreen, EscMenuButtons
 * Called by: Fabric loader via the "client" entrypoint in fabric.mod.json
 */
package com.customblocks.client;

import com.customblocks.CustomBlocksMod;
import com.customblocks.client.gui.EscMenuButtons;
import com.customblocks.client.gui.HudEditorScreen;
import com.customblocks.gui.GuiMode;
import com.customblocks.gui.screens.ArabicBrowserScreen;
import com.customblocks.gui.screens.BlockEditorScreen;
import com.customblocks.gui.screens.ConfigScreen;
import com.customblocks.gui.screens.MacroListScreen;
import com.customblocks.gui.screens.MainMenuScreen;
import com.customblocks.network.payloads.ChatPrefillPayload;
import com.customblocks.network.payloads.HudStatePayload;
import com.customblocks.network.payloads.HudSyncPayload;
import com.customblocks.network.payloads.OpenGuiPayload;
import com.customblocks.network.payloads.SilentPackPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

@Environment(EnvType.CLIENT)
public class CustomBlocksClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CustomBlocksMod.LOGGER.info("[CustomBlocks] Client initializing (Phase 10/11 + Group 03 — GUI + HUD).");

        // Restore persisted HUD settings (position / scale / color / opacity / visibility).
        HudConfig.load();

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
                    case HUD_EDITOR     -> context.client().setScreen(new HudEditorScreen());
                    default             -> context.client().setScreen(new MainMenuScreen());
                }
            });
        });

        // HudSyncPayload → populate ClientSlotCache
        ClientPlayNetworking.registerGlobalReceiver(HudSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientSlotCache.populate(payload.indexJson())));

        // ChatPrefillPayload → open the chat input with the command already typed (Group 04,
        // sent when a command is clicked in the /cb help chest GUI).
        ClientPlayNetworking.registerGlobalReceiver(ChatPrefillPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(
                        new net.minecraft.client.gui.screen.ChatScreen(payload.text()))));

        // SilentPackPayload → set the silent-pack flag this server wants (Group 05). Reset to
        // false on disconnect so other servers' packs are never auto-accepted by us.
        ClientPlayNetworking.registerGlobalReceiver(SilentPackPayload.ID, (payload, context) ->
                context.client().execute(() -> SilentPackState.set(payload.silent())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SilentPackState.set(false));

        // HudStatePayload → update + persist HUD visibility so the renderer reflects the toggle
        ClientPlayNetworking.registerGlobalReceiver(HudStatePayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    HudConfig.visible = payload.enabled();
                    HudConfig.save();
                }));

        // Inject the two CustomBlocks buttons into the vanilla pause/ESC menu.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                EscMenuButtons.onScreenInit(client, screen));

        // Customcolor tools: the icon texture is white; tint it with the stack's NBT colour
        // so every pair visibly wears its hex (recycled old-project approach).
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register(
                (stack, tintIndex) -> {
                    if (tintIndex != 0) return -1;
                    int rgb = com.customblocks.item.CustomColorToolItem.rgbOf(stack);
                    return (rgb < 0 ? 0xFFFFFF : rgb) | 0xFF000000;
                },
                com.customblocks.item.ToolItems.CUSTOM_SQUARE,
                com.customblocks.item.ToolItems.CUSTOM_TRIANGLE);
    }
}
