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

import com.customblocks.CustomBlocksConfig;
import com.customblocks.CustomBlocksMod;
import com.customblocks.client.capture.CaptureOverlayActions;
import com.customblocks.client.capture.CaptureOverlayManager;
import com.customblocks.client.gui.ArabicPreviewScreen;
import com.customblocks.client.gui.BlockCreationStudioScreen;
import com.customblocks.client.gui.EscMenuButtons;
import com.customblocks.client.gui.EyedropScreen;
import com.customblocks.client.gui.HudEditorScreen;
import com.customblocks.client.gui.RecolorSliderScreen;
import com.customblocks.client.gui.ShapeEditorScreen;
import com.customblocks.client.gui.VaultConflictScreen;
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
import com.customblocks.network.payloads.VaultConflictPayload;
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

        // G05-5 — restore the weak-GPU friend's per-server low-res choice (used by the pack sync on join).
        com.customblocks.client.lowres.LowResState.load();

        // Group 29 - local Shorts framing overlay (capture-invisible native window + fallback).
        CaptureOverlayManager.init();

        // Group 26 / FIX D — on a DEDICATED server the client's SlotManager is empty, so a custom
        // block's name (item in hand/inventory + placed-block name) would read "Custom Block".
        // Install the client name seam so SlotBlock falls back to the synced ClientSlotCache.
        // Common code can't import a client class, so the resolver is wired here (ADR-009 pattern).
        com.customblocks.block.SlotBlock.CLIENT_NAME_RESOLVER = idx -> {
            ClientSlotCache.Entry e = ClientSlotCache.getEntry(idx);
            return e == null ? null : e.name();
        };

        // Group 18 (REVAMP v2) — same seam for the block lore: the active lines are synced into
        // ClientSlotCache (HudSync "lore" field); SlotItem.appendTooltip reads them back here.
        com.customblocks.block.SlotBlock.CLIENT_LORE_RESOLVER = idx -> {
            ClientSlotCache.Entry e = ClientSlotCache.getEntry(idx);
            return (e == null || e.lore() == null) ? java.util.List.of() : e.lore();
        };

        // G08 — same seam for the block SHAPE: on a dedicated server the client's SlotManager is empty, so
        // the outline/collision box fell back to a full cube even when the block was set to carpet/slab/etc.
        // The synced shape is in ClientSlotCache ("shape" field); SlotBlock.resolveShape reads it back here.
        com.customblocks.block.SlotBlock.CLIENT_SHAPE_RESOLVER = idx -> {
            ClientSlotCache.Entry e = ClientSlotCache.getEntry(idx);
            return e == null ? null : e.shape();
        };

        // S4 — same seam for the block SOUND: footstep/break/place sounds play client-side, but on a dedicated
        // server the client's SlotManager is empty so getSoundGroup fell back to stone (single /cb setsound OR
        // bulk/setall sound never applied). The synced sound is in ClientSlotCache ("sound"); resolveSound reads it.
        com.customblocks.block.SlotBlock.CLIENT_SOUND_RESOLVER = idx -> {
            ClientSlotCache.Entry e = ClientSlotCache.getEntry(idx);
            return e == null ? null : e.sound();
        };

        // Group 30 (Guess Mode) — a flagged holder's OWN client shows "???" for any block flagged for them
        // (a specific id, or every custom block in all-mode), in every menu/tooltip/hotbar. Local only: other
        // clients never install this, so they keep seeing the real name. Server JVM leaves it null.
        com.customblocks.block.SlotBlock.CLIENT_NAME_DISGUISE = (stack, idx) ->
                ClientGuessState.localDisguisesSlot(idx) ? ClientGuessState.BLANK_NAME : null;

        // G13-25 CP3b — same seam for the Arabic join-flow prediction: on a remote session the
        // flow reads the synced "ar" tuples through this view instead of the stale local SlotManager.
        ArabicClientView.install();

        // G05-1 / M2 — on a REMOTE server (dedicated, or a LAN guest) this client's SlotManager holds
        // its OWN stale block list, not the server's, so item names/lore must read the synced
        // ClientSlotCache, not the local SlotManager. Flag it on join; an integrated host (singleplayer
        // or LAN host) keeps its live in-process SlotManager (flag stays false). Reset on disconnect.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                com.customblocks.block.SlotBlock.CLIENT_REMOTE_SESSION = !client.isIntegratedServerRunning());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                com.customblocks.block.SlotBlock.CLIENT_REMOTE_SESSION = false);

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
                        // data = "<id>|<texture url>" (see ImageToolCommands.recolor)
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
                    case RECORD_OVERLAY -> CaptureOverlayActions.handle(context.client(), data);
                    case CATEGORY_HUB   -> context.client().setScreen(new com.customblocks.client.gui.CategoryHubScreen());
                    case GUESS_SETTINGS -> context.client().setScreen(new com.customblocks.client.gui.GuessSettingsScreen());
                    case BUZZER_PANEL   -> {
                        // Refresh the open admin panel in place, or open it fresh (Group 31 item 4).
                        if (context.client().currentScreen instanceof com.customblocks.client.gui.BuzzerPanelScreen s) s.refresh(data);
                        else context.client().setScreen(new com.customblocks.client.gui.BuzzerPanelScreen(data));
                    }
                    case BULK_WORKBENCH -> {
                        // Group 07 §G07-3: refresh the open Workbench in place (an Apply must never close it),
                        // or open it fresh. data = the BulkSnapshot JSON.
                        // §G07-4 MP live-refresh: an Apply broadcasts a blank-tab snapshot to EVERY online
                        // player. A player who has the Hub closed must not have it popped open by someone
                        // else's op, so only a named tab (a real open request) opens it fresh; a blank tab that
                        // arrives while the Hub is closed is a bystander broadcast and is ignored.
                        if (context.client().currentScreen instanceof com.customblocks.client.gui.BulkWorkbenchScreen s) s.refresh(data);
                        else if (com.customblocks.client.gui.BulkWorkbenchScreen.wantsOpen(data))
                            context.client().setScreen(new com.customblocks.client.gui.BulkWorkbenchScreen(data));
                    }
                    case BACKUP_SCREEN  -> {
                        // Group 09 §G09-A4: refresh the open Backup Screen in place (an action must never
                        // close it), or open it fresh. data = BackupManager.screenJson().
                        if (context.client().currentScreen instanceof com.customblocks.client.gui.BackupScreen s) s.refresh(data);
                        else context.client().setScreen(new com.customblocks.client.gui.BackupScreen(data));
                    }
                    case SETALL_SCREEN  -> // Group 07: the dedicated Set All Screen (/cb setall, no args).
                        context.client().setScreen(new com.customblocks.client.gui.SetAllScreen());
                    default             -> context.client().setScreen(new MainMenuScreen());
                }
            });
        });

        // StudioEditPayload → open the Block Creation Studio on an existing block (edit mode, Group 14 Phase 2)
        ClientPlayNetworking.registerGlobalReceiver(StudioEditPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(
                        new BlockCreationStudioScreen(payload.index(), payload.id(), payload.name(), payload.attrs(), payload.url()))));

        // VaultConflictPayload → open the Cloud Vault conflict screen (Group 20 §S2): a download whose id
        // clashes locally opens BOTH blocks as side-by-side cubes so the player picks how to resolve it.
        ClientPlayNetworking.registerGlobalReceiver(VaultConflictPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(
                        new VaultConflictScreen(payload.code(), payload.incomingId(), payload.meta(), payload.preview()))));

        // HudSyncPayload → populate ClientSlotCache
        ClientPlayNetworking.registerGlobalReceiver(HudSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientSlotCache.populate(payload.indexJson())));

        // Group 30 — GuessModePayload → who's in guess mode (body pose) + the local player's own blinding.
        // Reset on disconnect so another server's session never bleeds through.
        ClientPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.GuessModePayload.ID, (payload, context) ->
                        context.client().execute(() -> ClientGuessState.populate(payload.json())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientGuessState.clear());

        // ChatPrefillPayload → open the chat input with the command already typed (Group 04,
        // sent when a command is clicked in the /cb help chest GUI).
        ClientPlayNetworking.registerGlobalReceiver(ChatPrefillPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(
                        new net.minecraft.client.gui.screen.ChatScreen(payload.text()))));

        // ClearLogsPayload → wipe only this client's [CB] chat lines (Group 04 §G04-4). Chat history is
        // client-side, so the server can only ask; CbChatMirror does the surgery. Must run on the
        // client thread — hence client.execute.
        CbChatMirror.register();
        ClientPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.ClearLogsPayload.ID, (payload, context) ->
                        context.client().execute(CbChatMirror::clearCbLines));

        // WidgetSyncPayload → the one signal feeding the 3 HUD widgets (Group 03). The server is
        // authoritative for every value in it; the client only draws.
        ClientPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.WidgetSyncPayload.ID, (payload, context) ->
                        context.client().execute(() ->
                                com.customblocks.client.hud.widget.WidgetSignals.apply(payload.json())));
        // Drop it all on disconnect so one server's locks never bleed into the next.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                com.customblocks.client.hud.widget.WidgetSignals.reset());

        // SilentPackPayload → set the silent-pack flag this server wants (Group 05). Reset to
        // false on disconnect so other servers' packs are never auto-accepted by us.
        ClientPlayNetworking.registerGlobalReceiver(SilentPackPayload.ID, (payload, context) ->
                context.client().execute(() -> SilentPackState.set(payload.silent())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SilentPackState.set(false));

        // TransparentBgPayload → off-atlas background mode (Group 14 Phase 1c Step 2b). Setting it rebuilds
        // the off-atlas caches. Reset to false (black) on disconnect so another server's choice never bleeds.
        ClientPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.TransparentBgPayload.ID, (payload, context) ->
                        context.client().execute(() ->
                                com.customblocks.client.render.OffAtlasBgState.set(payload.transparent())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                com.customblocks.client.render.OffAtlasBgState.set(false));

        // ColorHexSyncPayload → set this server's variant hexes (Group 06 / M3 hex, G06-C). The
        // Square/Triangle tool name is rendered client-side from these, so without the push a dedicated
        // client showed its own default "[#EE3333]". Reset to the shipped defaults on disconnect so
        // another server's (or singleplayer's) hexes never bleed across.
        ClientPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.ColorHexSyncPayload.ID, (payload, context) ->
                        context.client().execute(() -> {
                            CustomBlocksConfig.triangleRedHex    = payload.red();
                            CustomBlocksConfig.triangleYellowHex = payload.yellow();
                            CustomBlocksConfig.triangleGreenHex  = payload.green();
                            CustomBlocksConfig.triangleBlackHex  = payload.black();
                        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CustomBlocksConfig.triangleRedHex    = CustomBlocksConfig.TRIANGLE_RED_DEFAULT;
            CustomBlocksConfig.triangleYellowHex = CustomBlocksConfig.TRIANGLE_YELLOW_DEFAULT;
            CustomBlocksConfig.triangleGreenHex  = CustomBlocksConfig.TRIANGLE_GREEN_DEFAULT;
            CustomBlocksConfig.triangleBlackHex  = CustomBlocksConfig.TRIANGLE_BLACK_DEFAULT;
        });

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

        // VersionHandshakePayload → §K auto-update (Group 20). Compare our version to the server's;
        // older → update screen (or warn if the server disabled it), newer → warn only. One-shot per
        // connection; reset on disconnect so the next server can prompt again.
        ClientPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.VersionHandshakePayload.ID, (payload, context) ->
                        com.customblocks.client.update.UpdateController.onHandshake(context.client(), payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                com.customblocks.client.update.UpdateController.reset());

        // Group 05 remote fix: on a DEDICATED server the modded client can't rebuild from its own
        // stale slot data, so the server streams the pack files. Receive manifest → request lacking
        // files → buffer chunks → write → silent reload. (Integrated host still uses RegenPack above.)
        ClientPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.PackManifestPayload.ID, (payload, context) ->
                        com.customblocks.client.packsync.ClientPackReceiver.onManifest(context.client(), payload.gz()));
        ClientPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.PackFilePayload.ID, (payload, context) ->
                        com.customblocks.client.packsync.ClientPackReceiver.onFile(
                                context.client(), payload.path(), payload.index(), payload.count(), payload.data()));
        ClientPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.PackDonePayload.ID, (payload, context) ->
                        com.customblocks.client.packsync.ClientPackReceiver.onDone(context.client(), payload.hash()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                com.customblocks.client.packsync.ClientPackReceiver.reset());

        // HudStatePayload → update + persist HUD visibility so the renderer reflects the toggle
        ClientPlayNetworking.registerGlobalReceiver(HudStatePayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    HudConfig.visible = payload.enabled();
                    HudConfig.save();
                }));

        // HUD look-at hover sound: edge-triggered each client tick (Group 27 §G27.4).
        ClientTickEvents.END_CLIENT_TICK.register(HudHoverSound::tick);

        // Group 14 — placed ANIMATED blocks render OFF the block atlas (their own grid texture: ALL frames,
        // full speed, crisp — no atlas muffle and no frame-dropping to fit a strip). AnimSlotBER paints them;
        // static slots render nothing here and keep their normal atlas model.
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
                com.customblocks.block.AnimSlotRegistry.BLOCK_ENTITY,
                com.customblocks.client.render.AnimSlotBER::new);

        // Animated ITEM icons also render off-atlas (a frame grid can't sit on the atlas). The pack gives every
        // animated slot a builtin/entity item model, which routes its icon to this one shared renderer; static
        // slots keep their atlas item model, so it draws nothing for them.
        com.customblocks.client.render.SlotItemRenderer animItemRenderer =
                new com.customblocks.client.render.SlotItemRenderer();
        com.customblocks.block.SlotBlock[] slotBlocks = com.customblocks.core.SlotManager.allBlocks();
        if (slotBlocks != null) {
            for (int i = 0; i < slotBlocks.length; i++) {
                com.customblocks.block.SlotBlock.SlotItem it = com.customblocks.core.SlotManager.itemAt(i);
                if (it != null) net.fabricmc.fabric.api.client.rendering.v1
                        .BuiltinItemRendererRegistry.INSTANCE.register(it, animItemRenderer);
            }
        }

        // G13-25 CP5: the old arabic_letter renderers are gone — Arabic letters are plain slot
        // blocks now, drawn by AnimSlotBER above (ArabicSlotFaces path).

        // G06-14 slice 1 — draw the always-visible "Deleted: <name>" floating tag above a Deleted marker.
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
                com.customblocks.block.DeletedMarkerRegistry.BLOCK_ENTITY,
                com.customblocks.client.render.DeletedMarkerBER::new);

        // Group 30 · G30-8b — the floating end-crystal-style Showcase display (spinning picture core + orbiting
        // outer layer + bob), tuned by the shared GuessShowcaseStore, showing the placed BE's chosen slot.
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
                com.customblocks.block.GuessShowcaseRegistry.BLOCK_ENTITY,
                com.customblocks.client.render.GuessShowcaseBER::new);

        // Group 30 · G30 §R (R2) — the "?"-textured break/dig debris particle factory (bound to the
        // mystery_break sprite). Spawned by the §R particle mixin when a flagged holder breaks a disguised block.
        net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry.getInstance().register(
                com.customblocks.particle.MysteryParticles.MYSTERY_BREAK,
                com.customblocks.client.render.MysteryBreakParticle.Factory::new);

        // Group 32 — the Explosive Tomato's renderer. MANDATORY, not cosmetic: an EntityType with no renderer
        // hard-crashes the client the instant one spawns. G32-TEXTURE (2026-07-15) swapped FlyingItemEntityRenderer
        // (which draws the extruded ITEM MODEL — a red slab with visible edges) for a camera-facing billboard of
        // the 256 entity texture.
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                com.customblocks.tomato.TomatoRegistry.TOMATO,
                com.customblocks.client.render.TomatoEntityRenderer::new);

        // Group 06 — instant colour-Square swaps: paint the predicted variant on the client the same
        // tick as the click; the server still does the authoritative swap (ADR-009).
        ClientSwapPredictor.register();

        // Register the three CustomBlocks key bindings (toggle HUD / menu / HUD editor).
        CbKeybinds.register();

        // G05-5 — client-side "/cblowres [256|128|off]" so a non-op weak-GPU friend can shrink the pack.
        // Own root (NOT a /cb subcommand): a client child under "cb" would make Fabric's client dispatcher
        // shadow the whole /cb tree and reject every server /cb command client-side (see LowResClientCommand).
        com.customblocks.client.command.LowResClientCommand.register();

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
