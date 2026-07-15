/**
 * AdminPanelBlockEntity.java — Group 31 (BuzzerGame) Phase 1 item 2.
 *
 * Home + persistence for one {@link PanelSession} (the panel is the session root — design lock). The
 * session is created lazily on first access and serialized in the BE's NBT, so a round's state/mode/links
 * survive a chunk reload. Breaking the panel drops this BE and with it the whole session, which is the
 * intended "panel-break destroys session" behaviour; the item-3 link wand adds the buzzer/screen
 * auto-unlink warnings on top.
 *
 * While a round is live this BE ticks server-side: it advances the session clock (countdown / count-up)
 * and hands each beat to {@link RoundBroadcast} to show nearby players (the timer action-bar, 3-2-1-GO
 * titles, sound cues). It only persists on real state changes, not every clock tick.
 *
 * Depends on: BuzzerGameRegistry (BlockEntityType), PanelSession, RoundBeat, RoundBroadcast
 * Called by:  AdminPanelBlock (createBlockEntity, ticker, readout), BuzzerGameCommands (control), NBT load/save
 */
package com.customblocks.buzzergame;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class AdminPanelBlockEntity extends BlockEntity {

    private PanelSession session;

    public AdminPanelBlockEntity(BlockPos pos, BlockState state) {
        super(BuzzerGameRegistry.ADMIN_PANEL_ENTITY, pos, state);
    }

    /** The panel's session, created on first use. Mutate it, then call {@link #markDirty()} to persist. */
    public PanelSession session() {
        if (session == null) session = new PanelSession();
        return session;
    }

    /** Server ticker (wired by {@link AdminPanelBlock#getTicker}): drive + broadcast a live round. */
    public static void serverTick(World world, BlockPos pos, BlockState state, AdminPanelBlockEntity be) {
        if (world.isClient) return;
        PanelSession session = be.session();
        if (!session.isBroadcasting()) return;
        RoundBeat beat = session.tick();
        if (beat.stateChanged()) be.markDirty();
        RoundBroadcast.render((ServerWorld) world, pos, session, beat);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        NbtCompound sub = new NbtCompound();
        session().writeNbt(sub);
        nbt.put("session", sub);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        PanelSession s = new PanelSession();
        if (nbt.contains("session")) s.readNbt(nbt.getCompound("session"));
        this.session = s;
    }
}
