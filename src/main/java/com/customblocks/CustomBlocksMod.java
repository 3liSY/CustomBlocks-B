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
import com.customblocks.network.ResourcePackServer;
import com.customblocks.network.payloads.HudSyncPayload;
import com.customblocks.network.payloads.OpenGuiPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
        PayloadTypeRegistry.playS2C().register(OpenGuiPayload.ID,  OpenGuiPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HudSyncPayload.ID,  HudSyncPayload.CODEC);

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

        // On player join: send pack, send HUD index, fire onboarding welcome.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ResourcePackServer.sendToPlayer(handler.player);
            // Build and send HUD sync index (slotIndex → "customId displayName")
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (var d : SlotManager.assignedSlots()) {
                if (!first) sb.append(',');
                sb.append('"').append(d.index()).append("\":\"")
                  .append(d.customId()).append(' ').append(d.displayName()).append('"');
                first = false;
            }
            sb.append('}');
            ServerPlayNetworking.send(handler.player, new HudSyncPayload(sb.toString()));
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

    /** Register the tools tab listing the mod's hand tools. */
    private static void registerToolsTab() {
        Registry.register(Registries.ITEM_GROUP, CUSTOM_TOOLS_TAB,
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemGroup.customblocks.tools"))
                        .icon(() -> new ItemStack(ToolItems.LUMINA_BRUSH))
                        .entries((displayContext, entries) -> {
                            entries.add(ToolItems.LUMINA_BRUSH);
                            entries.add(ToolItems.CHISEL);
                            entries.add(ToolItems.DELETER);
                        })
                        .build());
    }
}
