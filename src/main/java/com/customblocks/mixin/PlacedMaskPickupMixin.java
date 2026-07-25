/**
 * PlacedMaskPickupMixin.java — Group 30 (Guess Mode) · §H Placed Mask Mode. COMMON (both sides).
 *
 * The drop a masked block rolls carries a runner tag ({@link com.customblocks.core.PlacedMaskTag}) so the
 * item lying on the floor still renders as the bundled "?". §H's boundary is explicit that HELD and INVENTORY
 * items are never disguised, so the tag has to die the moment the item stops being a thing in the world —
 * and this is that moment: {@code PlayerInventory.insertStack(int, ItemStack)}, which every pickup path
 * (collide-with-drop, {@code insertStack(stack)}, /give) funnels into.
 *
 * It strips on RETURN, and only on a successful insert, which is what keeps the two edge cases honest:
 *   • an item the player could NOT take (inventory full) never touches the inventory, so nothing is stripped
 *     and the drop keeps its mask instead of quietly revealing itself;
 *   • a PARTIAL pickup strips the copies that landed in the inventory and leaves the remainder on the ground
 *     still masked — the ground stack is the one the loop never sees.
 * The stripped inventory copy is byte-identical to a normal one, so it stacks and trades like any other.
 *
 * Registered as a COMMON mixin in customblocks.mixins.json (inventories exist on both sides).
 */
package com.customblocks.mixin;

import com.customblocks.core.PlacedMaskTag;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class PlacedMaskPickupMixin {

    @Inject(method = "insertStack(ILnet/minecraft/item/ItemStack;)Z", at = @At("RETURN"))
    private void customblocks$placedMaskUnmaskOnPickup(int slot, ItemStack stack,
                                                       CallbackInfoReturnable<Boolean> cir) {
        // Nothing entered the inventory → leave the drop on the floor exactly as it was, mask included.
        // (The incoming stack cannot be re-checked here: a full pickup empties it, so the tag is gone from
        // it and only the inventory copies still carry one. Hence the sweep, which self-gates per stack.)
        if (!cir.getReturnValue()) return;
        PlayerInventory self = (PlayerInventory) (Object) this;
        for (int i = 0; i < self.size(); i++) PlacedMaskTag.strip(self.getStack(i));
    }
}
