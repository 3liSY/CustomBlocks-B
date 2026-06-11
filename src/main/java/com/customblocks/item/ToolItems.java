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
    public static OmniToolItem OMNI_TOOL;
    public static RainbowRectangleItem RAINBOW_RECTANGLE;

    /** The eight colour/shape tools (Squares + Triangles), in creative-tab order. */
    public static final java.util.List<ShapeToolItem> SHAPES = new java.util.ArrayList<>();

    /** The custom-colour pair (/cb customcolor) — colour rides in NBT, so one id each. */
    public static CustomColorToolItem CUSTOM_SQUARE;
    public static CustomColorToolItem CUSTOM_TRIANGLE;

    public static void registerAll() {
        // Brush + Chisel stay registered (so any already-given copies still work), but the
        // Omni-Tool is the unified tool surfaced in the creative tab and the give shortcuts.
        LUMINA_BRUSH = register("lumina_brush", new LuminaBrushItem(new Item.Settings().maxCount(1)));
        CHISEL       = register("chisel",       new ChiselItem(new Item.Settings().maxCount(1)));
        DELETER      = register("deleter",      new DeleterItem(new Item.Settings().maxCount(1)));
        OMNI_TOOL    = register("omni_tool",    new OmniToolItem(new Item.Settings().maxCount(1)));
        RAINBOW_RECTANGLE = register("rainbow_rectangle",
                new RainbowRectangleItem(new Item.Settings().maxCount(1)));

        // Eight colour/shape tools — textures reused from the old project, code is new.
        registerShape("green",  "§a", "green_square",   "Green",  "Square");
        registerShape("green",  "§a", "green_triangle", "Green",  "Triangle");
        registerShape("yellow", "§e", "yellow_square",  "Yellow", "Square");
        registerShape("yellow", "§e", "yellow_triangle","Yellow", "Triangle");
        registerShape("red",    "§c", "red_square",     "Red",    "Square");
        registerShape("red",    "§c", "red_triangle",   "Red",    "Triangle");
        registerShape("black",  "§8", "black_square",   "Black",  "Square");
        registerShape("black",  "§8", "black_triangle", "Black",  "Triangle");

        // Custom-colour pair: any-hex tools given by /cb customcolor (Group 06).
        CUSTOM_SQUARE   = register("custom_square",   new CustomColorToolItem(new Item.Settings().maxCount(1), "Square"));
        CUSTOM_TRIANGLE = register("custom_triangle", new CustomColorToolItem(new Item.Settings().maxCount(1), "Triangle"));
    }

    private static void registerShape(String color, String code, String id, String colorName, String shape) {
        SHAPES.add(register(id, new ShapeToolItem(new Item.Settings().maxCount(1), colorName, code, shape)));
    }

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(Registries.ITEM, Identifier.of(CustomBlocksMod.MOD_ID, name), item);
    }
}
