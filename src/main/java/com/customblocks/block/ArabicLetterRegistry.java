/**
 * ArabicLetterRegistry.java — Group 13 / Pass 4 (real feature).
 *
 * Registers the single joinable letter block, its BlockItem, and its BlockEntityType under
 * customblocks:arabic_letter. Kept separate from SlotManager and the bundled ArabicBlockRegistry so
 * the join feature touches none of the 1028 SlotBlocks or the 224 static letter blocks (ADR-005).
 * Call register() once from CustomBlocksMod.onInitialize; the renderer is registered client-side in
 * CustomBlocksClient. The item carries no fixed letter — the give command stamps the letter onto
 * each stack (ArabicLetterBlock.stackFor), so it is not added to a creative tab.
 *
 * Depends on: ArabicLetterBlock, ArabicLetterBlockEntity
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

public final class ArabicLetterRegistry {

    private ArabicLetterRegistry() {}

    public static Block BLOCK;
    public static BlockItem ITEM;
    public static BlockEntityType<ArabicLetterBlockEntity> BLOCK_ENTITY;

    public static void register() {
        Identifier id = Identifier.of(CustomBlocksMod.MOD_ID, "arabic_letter");

        BLOCK = new ArabicLetterBlock(AbstractBlock.Settings.create().strength(1.0f));
        Registry.register(Registries.BLOCK, id, BLOCK);

        // ArabicLetterItem (not a plain BlockItem) so the stack name is computed live from NBT (O6).
        ITEM = new ArabicLetterItem(BLOCK, new Item.Settings());
        Registry.register(Registries.ITEM, id, ITEM);

        BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id,
                FabricBlockEntityTypeBuilder.create(ArabicLetterBlockEntity::new, BLOCK).build());

        CustomBlocksMod.LOGGER.info("[CustomBlocks] Registered Group 13 joinable letter block 'arabic_letter'.");
    }
}
