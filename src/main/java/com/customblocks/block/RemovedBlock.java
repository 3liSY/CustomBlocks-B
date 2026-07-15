/**
 * RemovedBlock.java
 *
 * Responsibility: ONE shared placeholder block (customblocks:removed) that a deleted custom block's
 * placed copies are swapped to (G06-2 / G06-3 / G05-2 "improved Option 2", decided 2026-06-26). It is
 * deliberately NOT a {@link SlotBlock} — it is tied to no slot index, so a leftover can never inherit
 * an old OR a future block's skin / name (the slot-recycling corruption). Grey, named "(Removed)" (lang
 * key), drops nothing, breaks instantly. The swap itself is done by {@link DeletedPlacementSweeper}.
 *
 * Depends on: CustomBlocksMod (MOD_ID, LOGGER)
 * Called by:  CustomBlocksMod.onInitialize (register), DeletedPlacementSweeper (reads INSTANCE)
 */
package com.customblocks.block;

import com.customblocks.CustomBlocksMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class RemovedBlock extends Block {

    /** The single shared instance, set by {@link #register()} during mod init. */
    public static RemovedBlock INSTANCE;

    public RemovedBlock(Settings settings) {
        super(settings);
    }

    /** Register the block + a (tab-less) BlockItem under customblocks:removed. Call once from init. */
    public static void register() {
        Identifier id = Identifier.of(CustomBlocksMod.MOD_ID, "removed");
        // strength(0) = instant break; dropsNothing() = no drop. A plain opaque cube — its look comes
        // from the shipped static asset (assets/customblocks/.../removed.*), never the generated pack.
        INSTANCE = new RemovedBlock(AbstractBlock.Settings.create().strength(0.0f).dropsNothing());
        Registry.register(Registries.BLOCK, id, INSTANCE);
        // A BlockItem so the block has a name for hover/F3; intentionally NOT added to any creative tab.
        Registry.register(Registries.ITEM, id, new BlockItem(INSTANCE, new Item.Settings()));
        CustomBlocksMod.LOGGER.info("[CustomBlocks] Registered shared '(Removed)' placeholder block.");
    }
}
