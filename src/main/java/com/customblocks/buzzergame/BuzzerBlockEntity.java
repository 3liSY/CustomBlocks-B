/**
 * BuzzerBlockEntity.java — Group 31 (BuzzerGame) Phase 1 items 1 + 3.
 *
 * Per-buzzer identity + its link back to an admin panel. Holds a stable {@code buzzerId} (assigned on
 * placement) plus, once linked with the wand, the {@code panelPos} of its admin panel and the
 * {@code sessionId} that panel had at link time. A press looks up the panel at {@code panelPos} and only
 * routes into it when the session id still matches (so a broken/replaced panel cleanly falls back to the
 * standalone demo press). Positions/ids are the buzzer's only knowledge — the PanelSession stays the
 * single source of truth for the round (design lock).
 *
 * Depends on: BuzzerGameRegistry (BlockEntityType)
 * Called by:  BuzzerBlock (createBlockEntity, press routing, break-unlink), BuzzerGameWand (link), NBT
 */
package com.customblocks.buzzergame;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class BuzzerBlockEntity extends BlockEntity {

    /** Stable id for this buzzer, assigned on placement. Links + per-round results key off it. */
    private UUID buzzerId = UUID.randomUUID();

    /** The linked admin panel's position, or null if this buzzer isn't linked. */
    private @Nullable BlockPos panelPos;
    /** The linked session's id (guards against a replaced panel at the same spot). */
    private @Nullable UUID sessionId;

    public BuzzerBlockEntity(BlockPos pos, BlockState state) {
        super(BuzzerGameRegistry.BLOCK_ENTITY, pos, state);
    }

    public UUID getBuzzerId() {
        return buzzerId;
    }

    public @Nullable BlockPos getPanelPos() {
        return panelPos;
    }

    public @Nullable UUID getSessionId() {
        return sessionId;
    }

    public boolean isLinked() {
        return panelPos != null && sessionId != null;
    }

    /** Bind this buzzer to a panel's session (called by the link wand). */
    public void link(BlockPos panelPos, UUID sessionId) {
        this.panelPos = panelPos == null ? null : panelPos.toImmutable();
        this.sessionId = sessionId;
        markDirty();
    }

    /** Forget the panel link (called when a link goes stale). */
    public void clearLink() {
        this.panelPos = null;
        this.sessionId = null;
        markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putUuid("buzzerId", buzzerId);
        if (panelPos != null) nbt.putLong("panelPos", panelPos.asLong());
        if (sessionId != null) nbt.putUuid("sessionId", sessionId);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        if (nbt.containsUuid("buzzerId")) {
            buzzerId = nbt.getUuid("buzzerId");
        }
        panelPos = nbt.contains("panelPos") ? BlockPos.fromLong(nbt.getLong("panelPos")) : null;
        sessionId = nbt.containsUuid("sessionId") ? nbt.getUuid("sessionId") : null;
    }

    /** Ship the id to clients on chunk load (harmless now; future phases read it client-side). */
    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup lookup) {
        return createNbt(lookup);
    }
}
