/**
 * AnimSlotRegistry.java — Group 14 / Phase 1b.
 *
 * Registers ONE BlockEntityType (customblocks:anim_slot) that backs ALL slot blocks, so each placed
 * slot block can carry an AnimSlotBlockEntity for the off-atlas world renderer. No new block or item
 * is registered here — the slot blocks already exist (SlotManager.registerAll); this only adds the
 * shared BlockEntityType over them.
 *
 * MUST be called AFTER SlotManager.registerAll() — the slot blocks must exist to build the type.
 *
 * Depends on: AnimSlotBlockEntity, SlotManager (the slot blocks), CustomBlocksMod (MOD_ID, LOGGER)
 * Called by:  CustomBlocksMod.onInitialize (register), CustomBlocksClient (BLOCK_ENTITY for the BER — later slice)
 */
package com.customblocks.block;

import com.customblocks.CustomBlocksMod;
import com.customblocks.core.SlotManager;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class AnimSlotRegistry {

    private AnimSlotRegistry() {}

    public static BlockEntityType<AnimSlotBlockEntity> BLOCK_ENTITY;

    public static void register() {
        SlotBlock[] blocks = SlotManager.allBlocks();
        if (blocks == null || blocks.length == 0) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] AnimSlotRegistry: no slot blocks — skipped (registerAll not run?).");
            return;
        }
        Identifier id = Identifier.of(CustomBlocksMod.MOD_ID, "anim_slot");
        // One type over every slot block (SlotBlock[] is a Block[] — covariant varargs).
        BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id,
                FabricBlockEntityTypeBuilder.create(AnimSlotBlockEntity::new, blocks).build());
        CustomBlocksMod.LOGGER.info("[CustomBlocks] Registered anim_slot BlockEntity over {} slot blocks (Group 14 Phase 1b).",
                blocks.length);
    }
}
