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

import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class AnimSlotBlockEntity extends BlockEntity implements RenderDataBlockEntity {

    private final int slotIndex;

    /** Glyph facing of a placed Arabic letter (see class header), null for every other block. */
    @Nullable
    private Direction arabicFacing = null;
    /** Slot whose tile the back face draws (mirror partner), -1 = own tile. */
    private int arabicBackSlot = -1;

    /**
     * G08 §J — per-placement orientation of a DIRECTIONAL shape (stairs today). Stored here instead of
     * a block-state property: LIGHT×SHAPE×FACING×HALF over ~3100 slots OOM'd registration (2026-07-20),
     * so per-placement rotation lives on the BE and syncs via the normal update packet — zero block-states.
     * Null facing / bottom half = the base orientation, written to NBT only when non-default so every
     * normal (non-directional) placement stays exactly as free as before. {@link #getRenderData} hands
     * these to the client model, which rotates the baked mesh to match the hitbox (BlockShapes.orient).
     */
    @Nullable
    private Direction placeFacing = null;
    private BlockHalf placeHalf = BlockHalf.BOTTOM;

    /**
     * G08 §J corners — the stair's connection shape, recomputed from its neighbours by
     * {@link com.customblocks.block.StairConnection} on placement + on every neighbour change. STRAIGHT
     * for a plain stair (and every non-stair block); a corner value selects the baked corner mesh the
     * client draws. Written to NBT only when non-straight, so normal blocks stay byte-free.
     */
    private StairShape placeShape = StairShape.STRAIGHT;

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

    /** G08 §J — placement facing of a directional shape, or null (non-directional / old placement). */
    @Nullable
    public Direction placeFacing() { return placeFacing; }

    /** G08 §J — placement half (TOP = upside-down), BOTTOM by default. */
    public BlockHalf placeHalf() { return placeHalf; }

    /** G08 §J corners — the stair's current connection shape (STRAIGHT for a plain stair). */
    public StairShape placeShape() { return placeShape; }

    /**
     * G08 §J corners — set the recomputed stair shape (from {@link StairConnection}). Server-side it syncs
     * the BE to trackers; a predicting client re-meshes its own section. No-op when unchanged. Returns true
     * when it actually changed.
     */
    public boolean applyStairShape(StairShape shape) {
        if (shape == null) shape = StairShape.STRAIGHT;
        if (shape == placeShape) return false;
        placeShape = shape;
        markDirty();
        if (world instanceof ServerWorld sw) sw.getChunkManager().markForUpdate(pos); // sync BE to clients
        else scheduleRerender();                                                      // client prediction
        return true;
    }

    /**
     * G08 §J — stamp a directional placement's orientation. Called on the SERVER (authoritative, from
     * SlotBlock.onPlaced) and on a predicting CLIENT (instant local feedback). Server syncs the BE to
     * trackers; a client re-meshes its own section so the rotated mesh shows immediately. No-op when the
     * orientation is unchanged.
     */
    public void applyPlacement(@Nullable Direction facing, BlockHalf half) {
        if (half == null) half = BlockHalf.BOTTOM;
        if (facing == placeFacing && half == placeHalf) return;
        placeFacing = facing;
        placeHalf = half;
        markDirty();
        if (world instanceof ServerWorld sw) sw.getChunkManager().markForUpdate(pos); // sync BE to clients
        else scheduleRerender();                                                      // client prediction
    }

    /**
     * Render data for the off-thread chunk mesher (Fabric block-view API). Returns an immutable
     * {@link SlotOrientation} ONLY for a rotated directional placement; null for every base/normal block
     * so the mesher does no extra work and the model wrapper passes those straight through. Reading the
     * BlockEntity directly on the mesh thread would be unsafe — this snapshot is the safe hand-off.
     */
    @Override
    @Nullable
    public Object getRenderData() {
        if (placeFacing == null && placeHalf != BlockHalf.TOP && placeShape == StairShape.STRAIGHT) return null;
        return new SlotOrientation(placeFacing, placeHalf, placeShape);
    }

    /** Client-side: schedule a section re-mesh so a facing change is picked up by the batched model. */
    private void scheduleRerender() {
        if (world != null && world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
        }
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
        // G08 §J — a directional placement writes its orientation only when non-default (facing set or
        // top half), so normal blocks add zero bytes.
        if (placeFacing != null) nbt.putString("placeFacing", placeFacing.getName());
        if (placeHalf == BlockHalf.TOP) nbt.putBoolean("placeTop", true);
        if (placeShape != StairShape.STRAIGHT) nbt.putString("placeShape", placeShape.name());
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        arabicFacing = nbt.contains("arabicFacing") ? Direction.byName(nbt.getString("arabicFacing")) : null;
        if (arabicFacing != null && arabicFacing.getAxis().isVertical()) arabicFacing = null; // corrupt guard
        arabicBackSlot = nbt.contains("arabicBackSlot") ? nbt.getInt("arabicBackSlot") : -1;

        // G08 §J — restore placement orientation; guard against a vertical/corrupt facing.
        Direction newFacing = nbt.contains("placeFacing") ? Direction.byName(nbt.getString("placeFacing")) : null;
        if (newFacing != null && newFacing.getAxis().isVertical()) newFacing = null;
        BlockHalf newHalf = nbt.getBoolean("placeTop") ? BlockHalf.TOP : BlockHalf.BOTTOM;
        StairShape newShape = nbt.contains("placeShape") ? StairShape.byName(nbt.getString("placeShape")) : StairShape.STRAIGHT;
        boolean changed = newFacing != placeFacing || newHalf != placeHalf || newShape != placeShape;
        placeFacing = newFacing;
        placeHalf = newHalf;
        placeShape = newShape;
        // When the authoritative BE update packet lands on a client, re-mesh so the new rotation shows.
        if (changed) scheduleRerender();
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
