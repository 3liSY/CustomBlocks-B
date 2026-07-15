/**
 * ScreenTestRegistry.java — Group 13 / Pass 4 PROOF SPIKE (throwaway).
 *
 * Registers the single test block, its item, and its BlockEntityType under brand-new ids
 * (customblocks:screen_test). Kept separate from SlotManager so the spike touches none of the
 * 1028 production blocks. Call register() once from CustomBlocksMod.onInitialize (server/common),
 * then register the renderer client-side in CustomBlocksClient.
 *
 * Depends on: ScreenTestBlock, ScreenTestBlockEntity
 * Called by:  CustomBlocksMod.onInitialize (register), CustomBlocksClient (BLOCK_ENTITY for the BER)
 */
package com.customblocks.block;

import com.customblocks.CustomBlocksMod;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ScreenTestRegistry {

    private ScreenTestRegistry() {}

    public static Block BLOCK;
    public static BlockItem ITEM;
    public static BlockEntityType<ScreenTestBlockEntity> BLOCK_ENTITY;

    public static void register() {
        Identifier id = Identifier.of(CustomBlocksMod.MOD_ID, "screen_test");

        BLOCK = new ScreenTestBlock(AbstractBlock.Settings.create().strength(1.0f).nonOpaque());
        Registry.register(Registries.BLOCK, id, BLOCK);

        ITEM = new BlockItem(BLOCK, new Item.Settings());
        Registry.register(Registries.ITEM, id, ITEM);

        BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id,
                FabricBlockEntityTypeBuilder.create(ScreenTestBlockEntity::new, BLOCK).build());

        CustomBlocksMod.LOGGER.info("[CustomBlocks] Registered Group 13 proof spike block 'screen_test'.");
    }
}
