/**
 * PlacedMaskTag.java — Group 30 (Guess Mode) · §H Placed Mask Mode. COMMON (both sides).
 *
 * Responsibility: the one marker that lets a DROPPED item keep the mask its block had. Everything else in
 * §H is decided by block POSITION, but the moment a masked block breaks the answer would walk out of the
 * mask as a normal item lying on the floor — so the server stamps the item the broken block rolled with the
 * runner's uuid, and the client's item-render seam draws the bundled "?" for anyone who is not that runner.
 *
 * Deliberately narrow:
 *   • it rides {@code minecraft:custom_data}, which vanilla never renders in a tooltip — the marker is
 *     invisible, it is not a second name/lore surface;
 *   • it is stripped the instant a player picks the item up ({@code ItemPickupUnmaskMixin}), so it never
 *     reaches an inventory. §H's boundary is explicit that held and inventory items are never disguised,
 *     and stripping also keeps the picked-up stack merging normally with the player's existing ones;
 *   • the render gate is still viewer identity — the runner who made the placement sees their own dropped
 *     item exactly as it is.
 *
 * Depends on: DataComponentTypes.CUSTOM_DATA / NbtComponent (vanilla, no registration needed).
 * Called by:  SlotBlock.getDroppedStacks (stamp), ItemDisguiseMixin (read), ItemPickupUnmaskMixin (strip).
 */
package com.customblocks.core;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public final class PlacedMaskTag {

    /** The custom_data key holding the masking runner's uuid string. */
    private static final String KEY = "cbPlacedMask";

    private PlacedMaskTag() {} // static-only

    /** Stamp a freshly rolled drop with the runner whose mask its block was under. */
    public static void stamp(ItemStack stack, String runnerUuid) {
        if (stack == null || stack.isEmpty() || runnerUuid == null || runnerUuid.isEmpty()) return;
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putString(KEY, runnerUuid));
    }

    /** The masking runner's uuid string on this stack, or null when it carries no mask. */
    public static String owner(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null || !data.contains(KEY)) return null;   // contains() first — no copy on the miss path
        return data.getNbt().getString(KEY);
    }

    /** Remove the marker (pickup), leaving the stack byte-identical to a normal one so it stacks again. */
    public static void strip(ItemStack stack) {
        if (owner(stack) == null) return;
        NbtCompound nbt = stack.get(DataComponentTypes.CUSTOM_DATA).copyNbt();
        nbt.remove(KEY);
        if (nbt.isEmpty()) stack.remove(DataComponentTypes.CUSTOM_DATA);
        else stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }
}
