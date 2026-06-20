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
 * Group 10 additions:
 *   - OpenGuiPayload RECOLOR_SLIDER → live recolour slider (data = "<id>|<texture url>")
 *   - OpenGuiPayload EYEDROP        → screen eyedrop
 *
 * Depends on: CustomBlocksMod (logger), gui screens, ClientSlotCache, HudConfig,
 *             HudEditorScreen, EscMenuButtons
 * Called by: Fabric loader via the "client" entrypoint in fabric.mod.json
 */
package com.customblocks.client;

import com.customblocks.CustomBlocksMod;
import com.customblocks.client.gui.ArabicPreviewScreen;
import com.customblocks.client.gui.BlockCreationStudioScreen;
import com.customblocks.client.gui.EscMenuButtons;
import com.customblocks.client.gui.EyedropScreen;
import com.customblocks.client.gui.HudEditorScreen;
import com.customblocks.client.gui.RecolorSliderScreen;
import com.customblocks.client.gui.ShapeEditorScreen;
import com.customblocks.client.hud.HudHoverSound;
import com.customblocks.gui.GuiMode;
import com.customblocks.gui.screens.ArabicBrowserScreen;
import com.customblocks.gui.screens.BlockEditorScreen;
import com.customblocks.gui.screens.ConfigScreen;
import com.customblocks.gui.screens.MacroListScreen;
import com.customblocks.gui.screens.MainMenuScreen;
import com.customblocks.arabic.ArabicLabels;
import com.customblocks.network.payloads.ArabicLabelsPayload;
import com.customblocks.network.payloads.ChatPrefillPayload;
import com.customblocks.network.payloads.HudStatePayload;
import com.customblocks.network.payloads.HudSyncPayload;
import com.customblocks.network.payloads.OpenGuiPayload;
import com.customblocks.network.payloads.RegenPackPayload;
import com.customblocks.network.payloads.SilentPackPayload;
import com.customblocks.network.payloads.StudioEditPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
                    case RECOLOR_SLIDER -> {
                        // data = "<id>|<texture url>" (see ImageToolCommands.livecolor)
                        int sep = data.indexOf('|');
                        String rid = sep >= 0 ? data.substring(0, sep) : data;
                        String url = sep >= 0 ? data.substring(sep + 1) : "";
                        context.client().setScreen(new RecolorSliderScreen(rid, url));
                    }
                    case EYEDROP        -> context.client().setScreen(new EyedropScreen());
                    case ARABIC_PREVIEW -> openArabicPreview(context.client(), data);
                    case SHAPE_EDITOR   -> {
                        // data = "<id>|<texture url>|<current shape>"
                        String[] p = data.split("\\|", 3);
                        context.client().setScreen(new ShapeEditorScreen(
                                p.length > 0 ? p[0] : "",
                                p.length > 1 ? p[1] : "",
                                p.length > 2 ? p[2] : "full"));
                    }
                    case CREATE_STUDIO  -> {
                        // data = "ai:<prompt>" → open on the AI tab (Group 15); otherwise the normal studio.
                        if (data != null && data.startsWith("ai:"))
                            context.client().setScreen(new BlockCreationStudioScreen(data.substring(3)));
                        else
                            context.client().setScreen(new BlockCreationStudioScreen());
                    }
                    default             -> context.client().setScreen(new MainMenuScreen());
                }
            });
        });

        // StudioEditPayload → open the Block Creation Studio on an existing block (edit mode, Group 14 Phase 2)
        ClientPlayNetworking.registerGlobalReceiver(StudioEditPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(
                        new BlockCreationStudioScreen(payload.index(), payload.id(), payload.name(), payload.attrs()))));

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

        // ArabicLabelsPayload → set this server's live join form labels (Group 13 / O6). Reset to the
        // shipped defaults on disconnect so another server's labels never bleed across.
        ClientPlayNetworking.registerGlobalReceiver(ArabicLabelsPayload.ID, (payload, context) ->
                context.client().execute(() -> ArabicLabels.set(payload.ini(), payload.mid(), payload.fin())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ArabicLabels.resetDefaults());

        // RegenPackPayload → modded clients build the pack locally + silently reload (Group 05).
        // The integrated single-player server's HTTP push is ignored by a modded client, so the
        // server signals us to generate from the live slot data instead of downloading.
        ClientPlayNetworking.registerGlobalReceiver(RegenPackPayload.ID, (payload, context) ->
                ResourcePackGenerator.regenerate(context.client(), payload.hash()));

        // HudStatePayload → update + persist HUD visibility so the renderer reflects the toggle
        ClientPlayNetworking.registerGlobalReceiver(HudStatePayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    HudConfig.visible = payload.enabled();
                    HudConfig.save();
                }));

        // HUD look-at hover sound: edge-triggered each client tick (Group 27 §G27.4).
        ClientTickEvents.END_CLIENT_TICK.register(HudHoverSound::tick);

        // Group 14 / Phase 1b — draw placed ANIMATED blocks off-atlas (crisp, no mipmap muffle).
        // Static slots render nothing here and keep their normal atlas model.
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
                com.customblocks.block.AnimSlotRegistry.BLOCK_ENTITY,
                com.customblocks.client.render.AnimSlotBER::new);

        // Group 13 / Pass 4 (real feature) — draw joinable Arabic letters from live in-memory textures.
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
                com.customblocks.block.ArabicLetterRegistry.BLOCK_ENTITY,
                com.customblocks.client.render.ArabicLetterBlockEntityRenderer::new);

        // Group 13 / O6 — draw the join letter's ITEM icon as the same glyph cube (fixes the black icon).
        net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(
                com.customblocks.block.ArabicLetterRegistry.ITEM,
                new com.customblocks.client.render.ArabicLetterItemRenderer());

        // Register the three CustomBlocks key bindings (toggle HUD / menu / HUD editor).
        CbKeybinds.register();

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

    /**
     * Open (or refresh in place) the live Arabic preview screen. Data = "id|texUrl|letterHex|bgHex|text".
     * If a preview screen is already open, refresh it so a colour change keeps the current rotation.
     */
    private static void openArabicPreview(net.minecraft.client.MinecraftClient client, String data) {
        String[] p = data.split("\\|", 5);
        String id  = p.length > 0 ? p[0] : "";
        String url = p.length > 1 ? p[1] : "";
        int letter = argbFromHex(p.length > 2 ? p[2] : "#FFFFFF");
        int bg     = argbFromHex(p.length > 3 ? p[3] : "#0A0A0A");
        String txt = p.length > 4 ? p[4] : "";
        if (client.currentScreen instanceof ArabicPreviewScreen sc) sc.refresh(id, url, letter, bg, txt);
        else client.setScreen(new ArabicPreviewScreen(id, url, letter, bg, txt));
    }

    private static int argbFromHex(String hex) {
        try { return 0xFF000000 | Integer.parseInt(hex.replace("#", ""), 16); }
        catch (Exception e) { return 0xFF000000; }
    }
}
