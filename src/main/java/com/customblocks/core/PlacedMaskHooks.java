/**
 * PlacedMaskHooks.java — Group 30 (Guess Mode) · §H Placed Mask Mode. SERVER-side glue.
 *
 * Responsibility: the three world moments §H cares about, kept out of {@link com.customblocks.block.SlotBlock}
 * so that block stays a block:
 *   • {@link #onPlaced}      — a runner placed a CustomBlock → record it and mask it for everyone else;
 *   • {@link #onRemoved}     — a masked block stopped existing → release the record and unmask it live;
 *   • {@link #stampDrops}    — the drop rolled by a masked block carries the runner tag so the item on the
 *                              floor stays a "?" for watchers.
 *
 * The mask is decided ONCE, here, at placement time — never recomputed from block type at render time — which
 * is what makes it exactly reversible: toggling the mode off reveals precisely the blocks that were hidden.
 * Blocks placed before the toggle, blocks placed by anyone else, and every vanilla block are untouched
 * because they simply never got a record.
 *
 * Depends on: PlacedMaskStore, PlacedMaskTag, GuessSync, Chat.
 * Called by:  SlotBlock (onPlaced / onStateReplaced / getDroppedStacks).
 */
package com.customblocks.core;

import com.customblocks.command.Chat;
import com.customblocks.network.GuessSync;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlacedMaskHooks {

    /** How rarely the "your mask is full" line may repeat — placements can arrive many times a second. */
    private static final long CAP_NOTICE_MS = 5_000L;

    private static final Map<UUID, Long> CAP_NOTICED = new HashMap<>();

    private PlacedMaskHooks() {} // static-only

    /** A CustomBlock was just placed: record it when the placer is running the mode. */
    public static void onPlaced(World world, BlockPos pos, LivingEntity placer) {
        if (world == null || world.isClient || !(placer instanceof ServerPlayerEntity runner)) return;
        UUID id = runner.getUuid();
        if (!PlacedMaskStore.isRunning(id)) return;
        if (PlacedMaskStore.record(id, world, pos)) {
            GuessSync.placedMaskAdd(runner.getServer(), id, world, pos);
            return;
        }
        if (PlacedMaskStore.atCap(id)) capNotice(runner, id);
    }

    /** A block stopped existing: drop its record and reveal it for everyone, live. */
    public static void onRemoved(World world, BlockPos pos) {
        if (world == null || world.isClient) return;
        if (PlacedMaskStore.release(world, pos) == null) return;
        GuessSync.placedMaskDel(world.getServer(), world, pos);
    }

    /**
     * Stamp the runner tag onto the stacks a masked block just dropped, so the item entity keeps the mask
     * for watchers. The block itself is usually already gone by now — PlacedMaskStore remembers the position
     * it released one step earlier for exactly this call.
     */
    public static void stampDrops(LootContextParameterSet.Builder builder, List<ItemStack> drops) {
        if (builder == null || drops.isEmpty()) return;
        ServerWorld world = builder.getWorld();
        Vec3d origin = builder.getOptional(LootContextParameters.ORIGIN);
        if (world == null || origin == null) return;
        String owner = PlacedMaskStore.ownerForDrop(world, BlockPos.ofFloored(origin));
        if (owner == null) return;
        for (ItemStack stack : drops) PlacedMaskTag.stamp(stack, owner);
    }

    /** Group doc §H: reaching the cap "reports a clear message instead of silently dropping masks". */
    private static void capNotice(ServerPlayerEntity runner, UUID id) {
        long now = System.currentTimeMillis();
        Long last = CAP_NOTICED.get(id);
        if (last != null && now - last < CAP_NOTICE_MS) return;
        CAP_NOTICED.put(id, now);
        Chat.toolError(runner, "Placed Mask is full at " + PlacedMaskStore.MAX_POSITIONS
                + " blocks — new placements stay visible until you break some or toggle the mode off.");
    }

    /** Forget a runner's throttle so a fresh session warns again straight away. */
    public static void forget(UUID id) { CAP_NOTICED.remove(id); }
}
