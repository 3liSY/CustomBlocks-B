/**
 * GuessShowcaseBlockEntity.java — Group 30 (Guess Mode) · G30-8b Showcase.
 *
 * Per-display state for a placed Showcase: the ONE thing that varies per instance is {@code shownSlot} —
 * the slot index whose baked picture the display shows (or {@link #NO_SLOT} = the bundled "?" cube). The
 * look/feel knobs (spin, size) are NOT here — they're one shared value in
 * {@link com.customblocks.core.GuessShowcaseStore}, synced separately. The slot rides to clients via the
 * update packet + initial-chunk NBT so {@link com.customblocks.client.render.GuessShowcaseBER} can draw it.
 *
 * Depends on: GuessShowcaseRegistry (BlockEntityType).
 * Called by:  GuessShowcaseBlock (createBlockEntity), GuessShowcaseCommands (setShownSlot on spawn), NBT.
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

public final class GuessShowcaseBlockEntity extends BlockEntity {

    /** "No slot" sentinel — the display shows the bundled "?" cube (same fallback as the disguise system). */
    public static final int NO_SLOT = -1;

    private int shownSlot = NO_SLOT;

    public GuessShowcaseBlockEntity(BlockPos pos, BlockState state) {
        super(GuessShowcaseRegistry.BLOCK_ENTITY, pos, state);
    }

    public int shownSlot() { return shownSlot; }

    /** Set which slot's picture the display shows, persist, and push to clients. {@link #NO_SLOT} → "?". */
    public void setShownSlot(int slot) {
        this.shownSlot = slot;
        markDirty();
        if (world instanceof ServerWorld sw) sw.getChunkManager().markForUpdate(pos); // resend the BE nbt live
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("shownSlot", shownSlot);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        shownSlot = nbt.contains("shownSlot") ? nbt.getInt("shownSlot") : NO_SLOT;
    }

    /** Ship the shown slot to clients so the BER can draw the right picture (on chunk load + live updates). */
    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup lookup) {
        return createNbt(lookup);
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
