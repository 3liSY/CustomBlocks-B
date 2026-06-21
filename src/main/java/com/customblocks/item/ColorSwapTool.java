/**
 * ColorSwapTool.java
 *
 * Marks a tool whose right-click swaps a PLACED custom block to an already-existing colour
 * variant — the Squares (fixed Green/Yellow/Red/Black via ShapeToolItem, and the custom-hex
 * one via CustomColorToolItem). It lets the client-side ClientSwapPredictor paint the swap
 * instantly without knowing each tool's concrete type or how it derives its colour key.
 *
 * Triangles do NOT implement this: they CREATE a variant + rebuild the pack, so there is
 * nothing the client can predict for them.
 *
 * Implemented by: ShapeToolItem (Square only), CustomColorToolItem (Square only)
 * Called by:      client/ClientSwapPredictor
 */
package com.customblocks.item;

import net.minecraft.item.ItemStack;

public interface ColorSwapTool {

    /**
     * The colour key this stack would swap a clicked block to — one of "red"/"yellow"/"green"/
     * "black" or a custom "hex_rrggbb" — or {@code null} when this stack does not swap (a
     * Triangle, or a custom tool carrying no colour). Pure and side-free so the client may call
     * it; it must match the key the server uses in ColorVariantService.swapPlaced.
     */
    String swapColourKey(ItemStack stack);
}
