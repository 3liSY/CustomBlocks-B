/**
 * DeletedMarkerBlockEntity.java — G06-14 slice 1.
 *
 * Per-marker identity for one deleted custom block:
 *   - customId    : the deleted block's id (empty = a generic/emptied marker that never heals).
 *   - displayName : the human name, so the tag/HUD read "Deleted: <name>".
 * Set on the server when a delete creates the marker (slice 1: by the temp /cb spawnmarker test).
 * Synced to clients via the standard block-entity update packet so the floating tag (DeletedMarkerBER)
 * and the look-HUD (HudRenderer) can read it without a resource pack or reload.
 *
 * Depends on: DeletedMarkerRegistry (BlockEntityType)
 * Called by:  DeletedMarkerBlock (createBlockEntity), the delete rail / temp test command (setData),
 *             the client renderer + HudRenderer (label()/customId())
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

public class DeletedMarkerBlockEntity extends BlockEntity {

    /** The deleted block's id; empty for a generic/emptied marker (never heals back). */
    private String customId = "";
    /** The deleted block's human display name; drives the "Deleted: <name>" label. */
    private String displayName = "";

    public DeletedMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(DeletedMarkerRegistry.BLOCK_ENTITY, pos, state);
    }

    public String customId() { return customId; }
    public String displayName() { return displayName; }

    /** Label drawn on the tag / HUD: "Deleted: <name>", or generic "(Deleted)" when emptied (no name). */
    public String label() {
        return (displayName == null || displayName.isEmpty()) ? "(Deleted)" : "Deleted: " + displayName;
    }

    /** Set the marker's identity (server). Call {@link #sync()} after to push it to clients. */
    public void setData(String id, String name) {
        this.customId = (id == null) ? "" : id;
        this.displayName = (name == null) ? "" : name;
        markDirty();
    }

    /** Push the current identity to tracking clients (no pack, no reload). */
    public void sync() {
        if (world instanceof ServerWorld sw) sw.getChunkManager().markForUpdate(pos);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putString("customId", customId);
        nbt.putString("displayName", displayName);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        customId = nbt.getString("customId");
        displayName = nbt.getString("displayName");
    }

    /** Sync identity to the client when the chunk is first sent. */
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
