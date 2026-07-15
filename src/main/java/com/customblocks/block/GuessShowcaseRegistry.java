/**
 * GuessShowcaseRegistry.java — Group 30 (Guess Mode) · G30-8b Showcase.
 *
 * Registers the {@code customblocks:guess_showcase} display block and its BlockEntityType. There is NO
 * BlockItem and NO creative-tab entry on purpose — a Showcase is only ever placed by the op command
 * ({@code /cb guess showcase spawn}), never carried as an item. Mirrors {@link DeletedMarkerRegistry} /
 * BuzzerGameRegistry; call {@link #register()} once from CustomBlocksMod.onInitialize.
 *
 * Depends on: GuessShowcaseBlock, GuessShowcaseBlockEntity, CustomBlocksMod.
 * Called by:  CustomBlocksMod.onInitialize; the client BER factory registers against {@link #BLOCK_ENTITY}.
 */
package com.customblocks.block;

import com.customblocks.CustomBlocksMod;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class GuessShowcaseRegistry {

    private GuessShowcaseRegistry() {} // static-only

    public static GuessShowcaseBlock BLOCK;
    public static BlockEntityType<GuessShowcaseBlockEntity> BLOCK_ENTITY;

    /** Register the Showcase display block + its BlockEntityType (main namespace, no item). */
    public static void register() {
        Identifier id = Identifier.of(CustomBlocksMod.MOD_ID, "guess_showcase");
        BLOCK = new GuessShowcaseBlock(AbstractBlock.Settings.create()
                .mapColor(MapColor.PURPLE)
                .strength(0.5f)
                .sounds(BlockSoundGroup.AMETHYST_BLOCK)
                .nonOpaque()
                .noCollision());
        Registry.register(Registries.BLOCK, id, BLOCK);
        BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id,
                FabricBlockEntityTypeBuilder.create(GuessShowcaseBlockEntity::new, BLOCK).build());
        CustomBlocksMod.LOGGER.info("[CustomBlocks] G30-8b: registered 'guess_showcase' display block + BlockEntity.");
    }
}
