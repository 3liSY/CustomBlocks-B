/**
 * DeletedMarkerRegistry.java — G06-14 slice 1.
 *
 * Registers the identity-carrying Deleted marker block, its BlockItem, and its BlockEntityType under
 * customblocks:deleted_marker. Kept separate from SlotManager / RemovedBlock (the old (Removed) system
 * this replaces, removed in slice 5). Call register() once from CustomBlocksMod.onInitialize; the
 * floating-tag renderer is registered client-side in CustomBlocksClient.
 *
 * strength(0) = instant break; dropsNothing() = no drop. A plain opaque grey cube — its look comes from
 * the shipped static asset (assets/customblocks/.../deleted_marker.*), never the generated pack.
 *
 * Depends on: DeletedMarkerBlock, DeletedMarkerBlockEntity, DeletedMarkerItem
 * Called by:  CustomBlocksMod.onInitialize (register), CustomBlocksClient (BLOCK_ENTITY for the BER)
 */
package com.customblocks.block;

import com.customblocks.CustomBlocksMod;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class DeletedMarkerRegistry {

    private DeletedMarkerRegistry() {}

    public static DeletedMarkerBlock BLOCK;
    public static DeletedMarkerItem ITEM;
    public static BlockEntityType<DeletedMarkerBlockEntity> BLOCK_ENTITY;

    public static void register() {
        Identifier id = Identifier.of(CustomBlocksMod.MOD_ID, "deleted_marker");

        BLOCK = new DeletedMarkerBlock(AbstractBlock.Settings.create().strength(0.0f).dropsNothing());
        Registry.register(Registries.BLOCK, id, BLOCK);

        ITEM = new DeletedMarkerItem(BLOCK, new Item.Settings());
        Registry.register(Registries.ITEM, id, ITEM);

        BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id,
                FabricBlockEntityTypeBuilder.create(DeletedMarkerBlockEntity::new, BLOCK).build());

        CustomBlocksMod.LOGGER.info("[CustomBlocks] G06-14 slice 1: registered 'deleted_marker' block + BlockEntity.");
    }
}
