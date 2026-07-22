/**
 * BuzzerBlockEntity.java — Group 31 (BuzzerGame) item 1 (wand-owned session rebuild, 2026-07-18).
 *
 * Per-buzzer identity + its link to a host's wand session. Holds a stable {@code buzzerId} (assigned on
 * placement) plus, once linked with the wand, the {@code sessionId} of the host session it belongs to.
 * A press resolves that id through {@link BuzzerSessionManager}; if the session is gone (host logged off,
 * or the round was reset with a fresh id) the buzzer cleanly falls back to the standalone demo press.
 * The session — not the buzzer — knows this buzzer's world position, so it stays the single source of
 * truth for the round (design lock). {@code sessionId} is the buzzer's only link knowledge (no panel pos).
 *
 * Depends on: BuzzerGameRegistry (BlockEntityType), BuzzerSessionManager
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

    /** The host session this buzzer is linked to, or null if unlinked (resolved via BuzzerSessionManager). */
    private @Nullable UUID sessionId;

    public BuzzerBlockEntity(BlockPos pos, BlockState state) {
        super(BuzzerGameRegistry.BLOCK_ENTITY, pos, state);
    }

    public UUID getBuzzerId() {
        return buzzerId;
    }

    public @Nullable UUID getSessionId() {
        return sessionId;
    }

    public boolean isLinked() {
        return sessionId != null;
    }

    /** Bind this buzzer to a host's wand session (called by the link wand). */
    public void link(UUID sessionId) {
        this.sessionId = sessionId;
        markDirty();
    }

    /** Forget the session link (called on host logout, session reset, or break). */
    public void clearLink() {
        this.sessionId = null;
        markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putUuid("buzzerId", buzzerId);
        if (sessionId != null) nbt.putUuid("sessionId", sessionId);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        if (nbt.containsUuid("buzzerId")) {
            buzzerId = nbt.getUuid("buzzerId");
        }
        sessionId = nbt.containsUuid("sessionId") ? nbt.getUuid("sessionId") : null;
    }

    /** Ship the id to clients on chunk load (harmless now; future phases read it client-side). */
    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup lookup) {
        return createNbt(lookup);
    }
}
