/**
 * AnimSlotBlockEntity.java — Group 14 / Phase 1b (+ G13-25 CP2 Arabic placement state).
 *
 * The BlockEntity attached to every SlotBlock. Two jobs:
 *   1. (Phase 1b) give the client a hook so AnimSlotBER can draw placed off-atlas blocks — for a
 *      normal block it stores NO state (the slot index is read from the SlotBlock at this position).
 *   2. (G13-25 CP2) for a placed ARABIC LETTER slot only, carry the per-placement join state the
 *      data-only design keeps OUT of the blockstate (no FACING property — that exact property forced
 *      the reverted CP1 partition):
 *        arabicFacing   — horizontal facing of the glyph (toward the reader); the word runs along
 *                         the horizontal axis PERPENDICULAR to it. Null for every non-letter.
 *        arabicBackSlot — slot index whose tile the BACK face draws (the word run's mirror partner,
 *                         set by ArabicSlotJoinFlow so the word reads correctly from behind), or
 *                         -1 = draw this block's own tile.
 *      Both fields are written by the SERVER (placement + join flow) and synced via the standard
 *      block-entity update packet; NBT is only written when set, so normal blocks stay ~free.
 *
 * Depends on: AnimSlotRegistry (BlockEntityType), SlotBlock (slot index)
 * Called by:  SlotBlock.createBlockEntity (placement), AnimSlotBER (render),
 *             arabic/ArabicSlotJoinFlow (facing + back-partner stamps)
 */
package com.customblocks.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class AnimSlotBlockEntity extends BlockEntity {

    private final int slotIndex;

    /** Glyph facing of a placed Arabic letter (see class header), null for every other block. */
    @Nullable
    private Direction arabicFacing = null;
    /** Slot whose tile the back face draws (mirror partner), -1 = own tile. */
    private int arabicBackSlot = -1;

    public AnimSlotBlockEntity(BlockPos pos, BlockState state) {
        super(AnimSlotRegistry.BLOCK_ENTITY, pos, state);
        this.slotIndex = (state.getBlock() instanceof SlotBlock sb) ? sb.getSlotIndex() : -1;
    }

    /** The slot this block draws from (-1 if somehow attached to a non-slot block). */
    public int slotIndex() { return slotIndex; }

    /** Facing of a placed Arabic letter, or null (non-letter / not yet stamped by the server). */
    @Nullable
    public Direction arabicFacing() { return arabicFacing; }

    /** Slot index the BACK face draws (mirror partner), or -1 = this block's own tile. */
    public int arabicBackSlot() { return arabicBackSlot; }

    /** Set the glyph facing. Returns true when it actually changed (caller syncs). */
    public boolean setArabicFacing(@Nullable Direction f) {
        if (f == arabicFacing) return false;
        arabicFacing = f;
        markDirty();
        return true;
    }

    /** Set the back face's mirror-partner slot (-1 = own). Returns true when changed (caller syncs). */
    public boolean setArabicBackSlot(int slot) {
        int v = Math.max(-1, slot);
        if (v == arabicBackSlot) return false;
        arabicBackSlot = v;
        markDirty();
        return true;
    }

    /** Push the current state to tracking clients — called by the join flow after its stamps. */
    public void sync() {
        if (world instanceof ServerWorld sw) sw.getChunkManager().markForUpdate(pos);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        // Only a placed Arabic letter writes anything — every other slot block stays stateless.
        if (arabicFacing != null) nbt.putString("arabicFacing", arabicFacing.getName());
        if (arabicBackSlot >= 0) nbt.putInt("arabicBackSlot", arabicBackSlot);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        arabicFacing = nbt.contains("arabicFacing") ? Direction.byName(nbt.getString("arabicFacing")) : null;
        if (arabicFacing != null && arabicFacing.getAxis().isVertical()) arabicFacing = null; // corrupt guard
        arabicBackSlot = nbt.contains("arabicBackSlot") ? nbt.getInt("arabicBackSlot") : -1;
    }

    /** Sync the Arabic placement state when the chunk is first sent to a client. */
    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup lookup) {
        return createNbt(lookup);
    }

    /** Sync on every later change (markForUpdate uses this). */
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
