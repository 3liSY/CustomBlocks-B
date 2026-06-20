/**
 * CustomBlocksMod.java
 *
 * Responsibility: Main mod entrypoint (Fabric ModInitializer). Registers the slot pool,
 * loads saved blocks, registers the /cb command tree, runs the resource-pack HTTP server,
 * and pushes the pack to clients.
 *
 * Phase 4 (textures): the HTTP pack server starts with the world, rebuilds on every
 * retexture, and is sent to each player on join.
 *
 * Depends on: CustomBlocksConfig, SlotManager, SlotBlock, CommandRegistrar, ResourcePackServer
 * Called by:  Fabric loader via the "main" entrypoint in fabric.mod.json
 */
package com.customblocks;

import com.customblocks.block.SlotBlock;
import com.customblocks.command.CommandRegistrar;
import com.customblocks.core.AutoBackup;
import com.customblocks.core.OnboardingManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.item.ToolItems;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import com.customblocks.network.payloads.ArabicLabelsPayload;
import com.customblocks.network.payloads.ChatPrefillPayload;
import com.customblocks.network.payloads.HudStatePayload;
import com.customblocks.network.payloads.HudSyncPayload;
import com.customblocks.network.payloads.OpenGuiPayload;
import com.customblocks.network.payloads.GuiBackPayload;
import com.customblocks.network.payloads.RecolorApplyPayload;
import com.customblocks.network.payloads.RegenPackPayload;
import com.customblocks.network.payloads.SilentPackPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomBlocksMod implements ModInitializer {

    /** Mod id — matches fabric.mod.json and the assets/data namespace. */
    public static final String MOD_ID = "customblocks";

    /** Shared logger. All log lines are prefixed "[CustomBlocks]" per NFR-09. */
    public static final Logger LOGGER = LoggerFactory.getLogger("CustomBlocks");

    /** The mod's creative inventory tab for created blocks. */
    public static final RegistryKey<ItemGroup> CUSTOM_BLOCKS_TAB =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, "blocks"));

    /** The mod's creative inventory tab for the hand tools (Phase 7). */
    public static final RegistryKey<ItemGroup> CUSTOM_TOOLS_TAB =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, "tools"));

    /** Group 13: searchable Arabic auto-join letters (every letter x 4 forms x 4 colours). */
    public static final RegistryKey<ItemGroup> ARABIC_JOIN_TAB =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, "arabic_join"));

    @Override
    public void onInitialize() {
        LOGGER.info("[CustomBlocks] Initializing CustomBlocks v1.0.0 (Phase 10+)");

        CustomBlocksConfig.load();
        registerImageIOPlugins(); // Group 14 — let ImageIO find the bundled WebP plugin under Fabric's classloader
        // Group 13 / O6: seed the live Arabic form labels from config so server-side naming + the
        // join-send below are correct before any client connects.
        com.customblocks.arabic.ArabicLabels.set(CustomBlocksConfig.arabicFormIni,
                CustomBlocksConfig.arabicFormMid, CustomBlocksConfig.arabicFormFin);
        // Group 13 / Pass 1: extract JAR-bundled fonts (arabtype + Rockwell) before any text render.
        com.customblocks.arabic.FontAssets.extractAll();
        int maxSlots = CustomBlocksConfig.maxSlots;

        // Register server→client payloads (Phase 10/11)
        PayloadTypeRegistry.playS2C().register(OpenGuiPayload.ID,   OpenGuiPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HudSyncPayload.ID,   HudSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HudStatePayload.ID,  HudStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatPrefillPayload.ID, ChatPrefillPayload.CODEC); // Group 04
        PayloadTypeRegistry.playS2C().register(SilentPackPayload.ID, SilentPackPayload.CODEC);   // Group 05
        PayloadTypeRegistry.playS2C().register(RegenPackPayload.ID,  RegenPackPayload.CODEC);    // Group 05 — modded local regen
        PayloadTypeRegistry.playS2C().register(ArabicLabelsPayload.ID, ArabicLabelsPayload.CODEC); // Group 13 / O6
        PayloadTypeRegistry.playS2C().register(                                                    // Group 14 Phase 2 — studio edit-load
                com.customblocks.network.payloads.StudioEditPayload.ID,
                com.customblocks.network.payloads.StudioEditPayload.CODEC);
        // Group 10: client→server live-recolour Apply. Server bakes; client only previews.
        PayloadTypeRegistry.playC2S().register(RecolorApplyPayload.ID, RecolorApplyPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                RecolorApplyPayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() -> com.customblocks.core.ColorToolService.applyRecolor(
                            player, payload.id(), payload.hue(), payload.sat(), payload.light(),
                            payload.temp(), payload.contrast(), payload.shadowLift(), payload.highlightDrop(), payload.filter()));
                });

        // Group 10: client→server "go back" — cancelling a colour client screen reopens the menu
        // the player came from (Nav.current), instead of dropping them to the world.
        PayloadTypeRegistry.playC2S().register(GuiBackPayload.ID, GuiBackPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                GuiBackPayload.ID, (payload, context) -> {
                    var player = context.player();
                    com.customblocks.gui.chest.Nav.MenuKey prev =
                            com.customblocks.gui.chest.Nav.current(player.getUuid());
                    if (prev != null) com.customblocks.gui.chest.GuiRouter.render(player, prev);
                });

        // Group 13: client→server actions from the live Arabic preview screen — change the preview
        // colours (re-render pack-free) or Create the real block. Server stays authoritative.
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.ArabicPreviewPayload.ID,
                com.customblocks.network.payloads.ArabicPreviewPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.ArabicPreviewPayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() -> {
                        if (payload.action() == com.customblocks.network.payloads.ArabicPreviewPayload.ACTION_CREATE)
                            com.customblocks.arabic.ArabicMaker.finalizeFromPreview(player);
                        else
                            com.customblocks.arabic.ArabicMaker.updatePreviewColours(
                                    player, payload.letterArgb(), payload.bgArgb());
                    });
                });

        // Group 27 §G27.5: client→server Shape Editor save. Server applies via the /cb setshape rail.
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.ShapeEditorPayload.ID,
                com.customblocks.network.payloads.ShapeEditorPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.ShapeEditorPayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() -> com.customblocks.command.handlers.ShapeCommands
                            .applyFromEditor(player, payload.id(), payload.shape()));
                });

        // Group 27 §G27.6: client→server Block Creation Studio "Create & Publish". Server creates via
        // the same CreationCommands rail /cb create uses (validate id → SlotManager.create → texture).
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.CreateStudioPayload.ID,
                com.customblocks.network.payloads.CreateStudioPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.CreateStudioPayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() -> com.customblocks.command.handlers.CreationStudioBridge
                            .createFromStudio(player, payload.id(), payload.name(), payload.url(), payload.attrs()));
                });

        // Group 14 Phase 2: client→server studio "Save changes" (edit mode). Applies attrs + merges the
        // animation knobs onto the block's existing AnimData (per-frame timing preserved). Authoritative.
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.StudioSavePayload.ID,
                com.customblocks.network.payloads.StudioSavePayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.StudioSavePayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() -> com.customblocks.command.handlers.CreationStudioBridge
                            .saveFromStudio(player, payload.origId(), payload.id(), payload.name(),
                                    payload.url(), payload.attrs()));
                });

        // Group 27 §G27.6: client→server category management from the studio's Category tab
        // (rename / delete / colour / set-default). Applied through the existing category rails.
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.CategoryAdminPayload.ID,
                com.customblocks.network.payloads.CategoryAdminPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.CategoryAdminPayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() -> com.customblocks.command.handlers.CategoryAdminBridge
                            .handle(player, payload.op(), payload.cat(), payload.arg()));
                });

        SlotManager.registerAll(maxSlots);
        SlotManager.loadAll();
        // Group 26 / FIX A: clean legacy display names (underscores -> spaces) once on boot.
        // Idempotent — a no-op once every name is already clean.
        int cleaned = SlotManager.migrateDisplayNames();
        if (cleaned > 0) LOGGER.info("[CustomBlocks] Cleaned {} legacy display name(s) (underscores -> spaces).", cleaned);
        // Group 13 / Pass 2: ensure the 224 bundled Arabic art blocks exist (idempotent, no rebuild
        // here — the SERVER_STARTED handler below builds the pack once with them included).
        com.customblocks.arabic.ArabicBlockRegistry.importArt(false);
        // Group 13 / Build B: retire the 144 old static letter blocks (auto-join is the only letter
        // system now), reclaim their slots, and air-clean any placed copies as chunks load. Numbers
        // (A0-A9 + E0-E9) are never touched. Idempotent -- a no-op on later boots.
        com.customblocks.arabic.ArabicLetterRetirement.init();
        ToolItems.registerAll();
        // Group 14 / Phase 1b — attach a BlockEntity to every slot block so the own-texture world
        // renderer can draw placed animated blocks off-atlas (crisp, no mipmap muffle). Must run
        // AFTER SlotManager.registerAll() (the blocks must exist to build the BlockEntityType).
        com.customblocks.block.AnimSlotRegistry.register();
        // Group 13 / Pass 4 (real feature) — the dedicated joinable Arabic letter block.
        com.customblocks.block.ArabicLetterRegistry.register();
        registerCreativeTab();
        registerToolsTab();
        registerArabicJoinTab();
        CommandRegistrar.register();

        // Resource-pack HTTP server: start with the world, rebuild the pack, stop on shutdown.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ResourcePackServer.setServer(server);
            ResourcePackServer.start();
            ResourcePackServer.updatePack();
            AutoBackup.start(server); // Group 09 / Slice 3 — timed auto-backups + prune
        });
        // Stop the auto-backup timer first, THEN flush slots, so no auto-backup fires mid-shutdown.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> { AutoBackup.stop(); SlotManager.saveAll(); });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ResourcePackServer.stop());

        // On player join: tell the client our silent-pack preference FIRST (so the pack
        // push below is auto-accepted with no dialog), then send pack, HUD index, welcome.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                    handler.player, new SilentPackPayload(CustomBlocksConfig.silentPack));
            // Group 13 / O6: push the live Arabic form labels so join-block names match this server.
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(handler.player,
                    new ArabicLabelsPayload(CustomBlocksConfig.arabicFormIni,
                            CustomBlocksConfig.arabicFormMid, CustomBlocksConfig.arabicFormFin));
            ResourcePackServer.sendToPlayer(handler.player);
            HudSync.sendTo(handler.player);
            OnboardingManager.onPlayerJoin(handler.player);
        });
        // Drop the player's pack-send history so a later rejoin gets exactly one prompt again.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ResourcePackServer.forget(handler.player.getUuid()));

        LOGGER.info("[CustomBlocks] Registered {} slot blocks (slot_0 to slot_{}).",
                maxSlots, maxSlots - 1);
        LOGGER.info("[CustomBlocks] Loaded {} saved custom block(s).", SlotManager.usedSlots());
        LOGGER.info("[CustomBlocks] Hello World — mod loaded successfully.");
    }

    /**
     * Group 14 — ImageIO discovers its reader plugins (incl. the bundled TwelveMonkeys WebP reader)
     * by scanning the THREAD CONTEXT classloader. Under Fabric/Knot that isn't the mod's classloader,
     * so the JiJ'd WebP SPI wouldn't be found at first use. We scan once with the mod classloader set,
     * then restore the previous one. The registered SPIs persist in the global IIORegistry.
     */
    private static void registerImageIOPlugins() {
        ClassLoader prev = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(CustomBlocksMod.class.getClassLoader());
            javax.imageio.ImageIO.scanForPlugins();
            LOGGER.info("[CustomBlocks] ImageIO readers available: {}",
                    java.util.Arrays.toString(javax.imageio.ImageIO.getReaderFormatNames()));
        } catch (Throwable t) {
            LOGGER.warn("[CustomBlocks] ImageIO plugin scan failed (WebP may be unavailable).", t);
        } finally {
            Thread.currentThread().setContextClassLoader(prev);
        }
    }

    /** Register the creative tab, listing only the blocks that have actually been created. */
    private static void registerCreativeTab() {
        Registry.register(Registries.ITEM_GROUP, CUSTOM_BLOCKS_TAB,
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemGroup.customblocks.blocks"))
                        .icon(() -> new ItemStack(Items.BOOKSHELF))
                        .entries((displayContext, entries) -> {
                            for (SlotData d : SlotManager.assignedSlots()) {
                                SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
                                if (item != null) entries.add(item);
                            }
                        })
                        .build());
    }

    /**
     * Register the tools tab listing the mod's hand tools. Registered AFTER the blocks tab
     * (see onInitialize order) so it appears right after the CustomBlocks blocks tab — custom
     * creative groups render in registration order.
     */
    private static void registerToolsTab() {
        Registry.register(Registries.ITEM_GROUP, CUSTOM_TOOLS_TAB,
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemGroup.customblocks.tools"))
                        .icon(() -> new ItemStack(ToolItems.OMNI_TOOL))
                        .entries((displayContext, entries) -> {
                            // Group 06: the unified Omni-Tool replaces the separate Brush + Chisel.
                            entries.add(ToolItems.OMNI_TOOL);
                            entries.add(ToolItems.RAINBOW_RECTANGLE);
                            entries.add(ToolItems.DELETER);
                            // The eight colour/shape tools (Squares + Triangles).
                            for (var shape : ToolItems.SHAPES) entries.add(shape);
                        })
                        .build());
    }

    /**
     * Group 13: the searchable Arabic letters tab. ONE isolated tile per letter per colour (O6, dev
     * 2026-06-19) — every entry is the SAME single registered block (customblocks:arabic_letter) with
     * different custom-data, so this adds ZERO registrations and costs ZERO slots. BLACK letters
     * auto-join into words; coloured letters are decoration that always stay isolated. The old fixed
     * Ini/Mid/Fin decoration variants were dropped so every tile you grab behaves the obvious way.
     * Searchable by name, e.g. "jeem" or "black" in creative search.
     */
    private static void registerArabicJoinTab() {
        Registry.register(Registries.ITEM_GROUP, ARABIC_JOIN_TAB,
                FabricItemGroup.builder()
                        .displayName(Text.literal("Arabic Letters"))
                        .icon(() -> com.customblocks.block.ArabicLetterBlock.stackFor('\u062c', 1)) // black jeem
                        .entries((displayContext, entries) -> {
                            for (com.customblocks.arabic.ArabicArt.Glyph g : com.customblocks.arabic.ArabicArt.ALL) {
                                if (g.group() != com.customblocks.arabic.ArabicArt.Group.LETTER) continue;
                                java.util.Optional<Character> ch =
                                        com.customblocks.arabic.ArabicGlyphs.charForName(g.idBase());
                                if (ch.isEmpty()) continue;
                                char letter = ch.get();
                                for (String color : com.customblocks.arabic.ArabicArt.COLORS) {
                                    // One isolated tile per colour. Black auto-joins; colours stay isolated.
                                    entries.add(com.customblocks.block.ArabicLetterBlock.stackFor(letter, color, -1, 1));
                                }
                            }
                        })
                        .build());
    }
}
