/**
 * ScreenTestBlockEntity.java — Group 13 / Pass 4 PROOF SPIKE (throwaway).
 *
 * Holds one int (`variant`) that selects which live in-memory picture the client draws. Right-click
 * cycles it on the server; the change is synced to clients via the standard block-entity update
 * packet (markForUpdate → toUpdatePacket). NO resource pack is involved, so no reload happens.
 *
 * Depends on: ScreenTestRegistry (BlockEntityType)
 * Called by:  ScreenTestBlock (createBlockEntity / onUse), the client renderer (variant())
 */
package com.customblocks.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class ScreenTestBlockEntity extends BlockEntity {

    /** Wrap point for the free-running variant counter (the CLIENT maps it onto its live gallery size). */
    private static final int WRAP = 1000;

    private int variant = 0;

    public ScreenTestBlockEntity(BlockPos pos, BlockState state) {
        super(ScreenTestRegistry.BLOCK_ENTITY, pos, state);
    }

    public int variant() { return variant; }

    /** Advance to the next picture and push the change to tracking clients (no pack, no reload). */
    public void cycle() {
        variant = (variant + 1) % WRAP;
        markDirty();
        if (world instanceof ServerWorld sw) sw.getChunkManager().markForUpdate(pos);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("variant", variant);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        variant = nbt.getInt("variant");
    }

    /** Sync the variant to the client when the chunk is first sent. */
    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup lookup) {
        return createNbt(lookup);
    }

    /** Sync the variant on every later change (markForUpdate uses this). */
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
