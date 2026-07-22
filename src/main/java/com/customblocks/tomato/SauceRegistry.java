/**
 * SauceRegistry.java — Group 32 (Explosive Tomato) Phase D ("Mess").
 *
 * Registers the {@code customblocks:tomato_sauce} block. Deliberately registers NO BlockItem: sauce is spawned
 * only by a blast (see {@link SauceManager#splatter}), never placed, crafted, or given — so there is nothing to
 * put in a creative tab and no dupe surface.
 *
 * The Settings here carry the two gameplay knobs that need no code: slipperiness (0.995, slipperier than ice's
 * 0.98) and pistonBehavior NORMAL (pushed whole like a normal block, NOT broken like a snow layer).
 *
 * Depends on: SauceBlock, CustomBlocksMod
 * Called by:  CustomBlocksMod.onInitialize (register)
 */
package com.customblocks.tomato;

import com.customblocks.CustomBlocksMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class SauceRegistry {

    private SauceRegistry() {} // static-only

    public static final String ID = "tomato_sauce";
    public static SauceBlock SAUCE;

    /** Register the sauce block (no item). Call once from onInitialize. */
    public static void register() {
        Identifier id = Identifier.of(CustomBlocksMod.MOD_ID, ID);
        SAUCE = new SauceBlock(AbstractBlock.Settings.create()
                .mapColor(MapColor.RED)
                .strength(0.1f)
                .slipperiness(0.995f)                 // slipperier than ice (0.98) — owner-locked
                .sounds(BlockSoundGroup.HONEY)        // wet, sticky footfall
                .nonOpaque()                          // partial-height box: don't cull neighbours
                .replaceable()                        // a later splat can overwrite it; it drops nothing
                .pistonBehavior(PistonBehavior.NORMAL)); // pushed whole, NOT snow-style destroy
        Registry.register(Registries.BLOCK, id, SAUCE);
        CustomBlocksMod.LOGGER.info("[CustomBlocks] G32 Phase D: registered the tomato_sauce block.");
    }
}
