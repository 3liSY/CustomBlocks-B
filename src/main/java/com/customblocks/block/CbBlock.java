/**
 * CbBlock.java — G13-19 shared block contract (foundation).
 *
 * Responsibility: the common seam every tool/command/GUI routes through so it works the same on
 * every custom block. Since G13-25 CP5 there is ONE family — the slot-backed {@link SlotBlock}s
 * (Arabic letters/numbers are plain SlotBlocks too, flagged only by ArabicMeta on their SlotData);
 * the old per-instance arabic_letter NBT block this seam was built to reach is removed. The seam
 * stays: any future non-slot custom block gets tool support by implementing it.
 *
 * NOT a render merge — the two render systems stay separate (ADR-005). This is a shared RULES
 * contract only. It starts intentionally small (just {@link #cbDelete}) and grows one method per
 * migrated tool (recolor, attributes, isTransparent, …) so each addition ships with the feature
 * that uses it — never speculative.
 *
 * Depends on: (Minecraft world/pos/player only)
 * Called by:  item/DeleterItem (first migrated tool); later command/GUI tool paths.
 */
package com.customblocks.block;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface CbBlock {

    /**
     * Delete this custom block the way the Deleter (and /cb delete) should, SERVER-side, and tell
     * the player. {@link SlotBlock}: wipe the slot DEFINITION (texture/name/settings) via
     * SlotManager; the placed copy stays in the world as a broken/empty block (undoable via
     * /cb undo). Returns true when something was deleted; false when nothing happened (e.g. an
     * unassigned slot or a locked block — the implementation messages the player on refusal).
     */
    boolean cbDelete(ServerPlayerEntity player, World world, BlockPos pos);
}
