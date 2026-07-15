/**
 * TomatoRegistry.java — Group 32 (Explosive Tomato) Phase A ("Fly").
 *
 * Registers the Explosive Tomato ENTITY TYPE and its item under {@code customblocks:explosive_tomato},
 * and hands the item to the vanilla dispenser as a projectile.
 *
 * This is the mod's FIRST custom entity — nothing else in CustomBlocks touches Registries.ENTITY_TYPE.
 * Two consequences worth knowing before editing this file:
 *   • A projectile whose EntityType has no renderer registered CRASHES the client the moment one spawns.
 *     The renderer is registered in CustomBlocksClient (FlyingItemEntityRenderer) — keep them in step.
 *   • trackingTickInterval is 2, not the snowball's 4: a tomato can be RIDDEN, and a rider's camera on a
 *     4-tick-synced vehicle judders badly.
 *
 * Phase A registers no sounds, no explosion and no config — those land in Phase B ("Boom").
 *
 * Depends on: TomatoEntity, TomatoItem, CustomBlocksMod
 * Called by:  CustomBlocksMod.onInitialize (register), CustomBlocksMod.registerToolsTab (ITEM in the tab),
 *             CustomBlocksClient (TOMATO, to bind the renderer)
 */
package com.customblocks.tomato;

import com.customblocks.CustomBlocksMod;
import net.minecraft.block.DispenserBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class TomatoRegistry {

    private TomatoRegistry() {} // static-only

    /** Registry path for both the entity type and the item. */
    public static final String ID = "explosive_tomato";

    public static EntityType<TomatoEntity> TOMATO;
    public static TomatoItem ITEM;

    /** Register the entity type, the item, and the dispenser behaviour. Call once, from onInitialize. */
    public static void register() {
        Identifier id = Identifier.of(CustomBlocksMod.MOD_ID, ID);

        TOMATO = Registry.register(Registries.ENTITY_TYPE, id,
                EntityType.Builder.<TomatoEntity>create(TomatoEntity::new, SpawnGroup.MISC)
                        .dimensions(0.55f, 0.55f)
                        .passengerAttachments(0.55f) // the rider sits on top of the fruit, not inside it
                        .maxTrackingRange(8)
                        .trackingTickInterval(2)
                        .build(id.toString()));

        ITEM = Registry.register(Registries.ITEM, id, new TomatoItem(new Item.Settings().maxCount(64)));

        // A6 — dispensers fire it like a cannon. Velocity/spread come from TomatoItem.getProjectileSettings().
        DispenserBlock.registerProjectileBehavior(ITEM);

        CustomBlocksMod.LOGGER.info("[CustomBlocks] G32 Phase A: registered the Explosive Tomato entity + item.");
    }
}
