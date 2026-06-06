/**
 * ToolItems.java
 *
 * Responsibility: Register the mod's hand tools (Phase 7) into the item registry and expose
 * them so the creative tab can list them. Mirrors SlotManager's role for blocks: one place
 * that owns tool registration.
 *
 * Depends on: CustomBlocksMod (MOD_ID), the tool item classes
 * Called by:  CustomBlocksMod.onInitialize()
 */
package com.customblocks.item;

import com.customblocks.CustomBlocksMod;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ToolItems {

    private ToolItems() {} // static-only

    public static LuminaBrushItem LUMINA_BRUSH;
    public static ChiselItem CHISEL;
    public static DeleterItem DELETER;

    public static void registerAll() {
        LUMINA_BRUSH = register("lumina_brush", new LuminaBrushItem(new Item.Settings().maxCount(1)));
        CHISEL       = register("chisel",       new ChiselItem(new Item.Settings().maxCount(1)));
        DELETER      = register("deleter",      new DeleterItem(new Item.Settings().maxCount(1)));
    }

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(Registries.ITEM, Identifier.of(CustomBlocksMod.MOD_ID, name), item);
    }
}
