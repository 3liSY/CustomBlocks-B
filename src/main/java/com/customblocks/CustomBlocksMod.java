/**
 * CustomBlocksMod.java
 *
 * Responsibility: Main mod entrypoint (Fabric ModInitializer). The single place where
 * blocks, items, commands, and network payloads are registered at startup.
 *
 * Phase 2 (persistence + core commands): registers the slot pool, loads saved blocks
 * from disk, registers the /cb command tree, and saves on shutdown. The texture/network
 * pipeline (Phase 4-5) is wired in later.
 *
 * Depends on: CustomBlocksConfig, SlotManager, SlotBlock, CommandRegistrar
 * Called by:  Fabric loader via the "main" entrypoint in fabric.mod.json
 *
 * ADR Reference: ADR-001 (pre-registration instead of dynamic registry).
 */
package com.customblocks;

import com.customblocks.block.SlotBlock;
import com.customblocks.command.CommandRegistrar;
import com.customblocks.core.SlotManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
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

    /** The mod's creative inventory tab. */
    public static final RegistryKey<ItemGroup> CUSTOM_BLOCKS_TAB =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, "blocks"));

    @Override
    public void onInitialize() {
        LOGGER.info("[CustomBlocks] Initializing CustomBlocks v1.0.0 (Phase 2 — persistence + commands)");

        CustomBlocksConfig.load();
        int maxSlots = CustomBlocksConfig.maxSlots;

        SlotManager.registerAll(maxSlots);
        SlotManager.loadAll();          // restore saved blocks AFTER registration
        registerCreativeTab();
        CommandRegistrar.register();    // /customblock + /cb

        // Persist all slot data on shutdown (changes are also saved on each edit).
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> SlotManager.saveAll());

        LOGGER.info("[CustomBlocks] Registered {} slot blocks (slot_0 to slot_{}).",
                maxSlots, maxSlots - 1);
        LOGGER.info("[CustomBlocks] Loaded {} saved custom block(s).", SlotManager.usedSlots());
        LOGGER.info("[CustomBlocks] Hello World — mod loaded successfully.");
    }

    /** Register the creative tab and fill it with the slot items. */
    private static void registerCreativeTab() {
        Registry.register(Registries.ITEM_GROUP, CUSTOM_BLOCKS_TAB,
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemGroup.customblocks.blocks"))
                        .icon(() -> new ItemStack(Items.BOOKSHELF))
                        .entries((displayContext, entries) -> {
                            for (int i = 0; i < SlotManager.registeredCount(); i++) {
                                SlotBlock.SlotItem item = SlotManager.itemAt(i);
                                if (item != null) entries.add(item);
                            }
                        })
                        .build());
    }
}
