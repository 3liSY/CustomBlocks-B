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
import com.customblocks.core.OnboardingManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.item.ToolItems;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import com.customblocks.network.payloads.ChatPrefillPayload;
import com.customblocks.network.payloads.HudStatePayload;
import com.customblocks.network.payloads.HudSyncPayload;
import com.customblocks.network.payloads.OpenGuiPayload;
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

    @Override
    public void onInitialize() {
        LOGGER.info("[CustomBlocks] Initializing CustomBlocks v1.0.0 (Phase 10+)");

        CustomBlocksConfig.load();
        int maxSlots = CustomBlocksConfig.maxSlots;

        // Register server→client payloads (Phase 10/11)
        PayloadTypeRegistry.playS2C().register(OpenGuiPayload.ID,   OpenGuiPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HudSyncPayload.ID,   HudSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HudStatePayload.ID,  HudStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatPrefillPayload.ID, ChatPrefillPayload.CODEC); // Group 04
        PayloadTypeRegistry.playS2C().register(SilentPackPayload.ID, SilentPackPayload.CODEC);   // Group 05

        SlotManager.registerAll(maxSlots);
        SlotManager.loadAll();
        ToolItems.registerAll();
        registerCreativeTab();
        registerToolsTab();
        CommandRegistrar.register();

        // Resource-pack HTTP server: start with the world, rebuild the pack, stop on shutdown.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ResourcePackServer.setServer(server);
            ResourcePackServer.start();
            ResourcePackServer.updatePack();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> SlotManager.saveAll());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ResourcePackServer.stop());

        // On player join: tell the client our silent-pack preference FIRST (so the pack
        // push below is auto-accepted with no dialog), then send pack, HUD index, welcome.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                    handler.player, new SilentPackPayload(CustomBlocksConfig.silentPack));
            ResourcePackServer.sendToPlayer(handler.player);
            HudSync.sendTo(handler.player);
            OnboardingManager.onPlayerJoin(handler.player);
        });

        LOGGER.info("[CustomBlocks] Registered {} slot blocks (slot_0 to slot_{}).",
                maxSlots, maxSlots - 1);
        LOGGER.info("[CustomBlocks] Loaded {} saved custom block(s).", SlotManager.usedSlots());
        LOGGER.info("[CustomBlocks] Hello World — mod loaded successfully.");
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
}
