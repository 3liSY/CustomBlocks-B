/**
 * CustomToolItem.java
 *
 * Responsibility: Shared base for the mod's hand tools (Phase 7). It resolves the targeted
 * block to a SlotBlock + its SlotData and runs the tool's work SERVER-side only, then lets
 * subclasses implement act().
 *
 * Pitfall guard (Bible §9.6 "client-side skip"): we do NOT early-return on world.isClient —
 * that caused the old noticeable delay. Instead the client path returns SUCCESS (the arm
 * swings instantly) and only the ServerPlayerEntity branch performs the actual mutation.
 *
 * Depends on: SlotBlock, SlotData, SlotManager
 * Called by:  the game, on right-click with a tool; subclasses LuminaBrush / Chisel / Deleter.
 */
package com.customblocks.item;

import com.customblocks.block.SlotBlock;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class CustomToolItem extends Item {

    protected CustomToolItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        if (!(world.getBlockState(pos).getBlock() instanceof SlotBlock slot)) {
            return ActionResult.PASS; // not one of our blocks — behave like an empty hand
        }
        // Client returns SUCCESS so the arm swings with no delay; the server does the work.
        if (!(ctx.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        SlotData d = SlotManager.getBySlot(slot.getSlotKey());
        if (d == null) {
            return ActionResult.SUCCESS; // an unassigned slot block; nothing to edit
        }
        act(player, world, pos, d);
        return ActionResult.SUCCESS;
    }

    /** Perform the tool's effect on the targeted custom block (server side only). */
    protected abstract void act(ServerPlayerEntity player, World world, BlockPos pos, SlotData d);
}
