/**
 * ArabicLetterBlockEntity.java — Group 13 / Pass 4 (real feature, graduated from ScreenTest spike).
 *
 * Holds the per-block data for one joinable Arabic letter:
 *   - letter : the Arabic letter char this block shows.
 *   - form   : its computed contextual form (0 isolated · 1 initial · 2 medial · 3 final), set by
 *              the bounded re-flow updater (ArabicJoinFlow) on place/break.
 * Facing/axis live on the block's FACING blockstate (set at placement), not here — the renderer
 * reads facing from the state and letter+form from this BlockEntity (ADR-005 live-texture path; no
 * resource pack, no reload). Changes sync to clients via the standard block-entity update packet.
 *
 * Depends on: ArabicLetterRegistry (BlockEntityType)
 * Called by:  ArabicLetterBlock (createBlockEntity / onPlaced), ArabicJoinFlow (setForm),
 *             the client renderer (letter()/form())
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

public class ArabicLetterBlockEntity extends BlockEntity {

    /** Empty until set on placement. Stored as a string so multi-unit chars stay intact. */
    private String letter = "";
    /** Contextual form index (ArabicJoining.ISOLATED..FINAL). */
    private int form = 0;
    /** Bundled colour name (black/red/green/yellow). Set on placement. */
    private String color = "black";
    /** Fixed form (0..3) for a searchable decoration variant, or -1 = auto-join (form follows neighbours). */
    private int lockedForm = -1;
    /** True when an auto-join letter neighbour sits beside this one (set by ArabicJoinFlow). Drives the
     *  stuck-isolated size fix: an ISOLATED letter that is attached is drawn at its natural height. */
    private boolean attached = false;
    /** C-full readable back (issue #6): the letter the BACK face shows — the mirror partner's letter in
     *  the word run (back of block k shows the front letter of block N-1-k). Empty = none (use own). */
    private String backLetter = "";
    /** Contextual form for the back face's letter (== the partner's front form). Set by ArabicJoinFlow. */
    private int backForm = 0;

    public ArabicLetterBlockEntity(BlockPos pos, BlockState state) {
        super(ArabicLetterRegistry.BLOCK_ENTITY, pos, state);
    }

    /** The Arabic letter char (or 0 if unset / empty). */
    public char letter() { return letter.isEmpty() ? 0 : letter.charAt(0); }

    public int form() { return form; }

    /** True when a letter neighbour sits beside this block (used by the renderer for the size fix). */
    public boolean attached() { return attached; }

    /** Letter the BACK face draws (mirror partner's letter), or 0 = none (renderer falls back to own). */
    public char backLetter() { return backLetter.isEmpty() ? 0 : backLetter.charAt(0); }

    /** Contextual form for the back face's letter (the partner's front form). */
    public int backForm() { return backForm; }

    /** Bundled colour name (black/red/green/yellow). */
    public String color() { return (color == null || color.isEmpty()) ? "black" : color; }

    /** Fixed form (0..3) for a searchable decoration variant, or -1 if this is an auto-join block. */
    public int lockedForm() { return lockedForm; }

    /** The form the renderer draws: the locked form when fixed, else the neighbour-computed form. */
    public int effectiveForm() { return lockedForm >= 0 ? lockedForm : form; }

    /** Set the bundled colour once at placement (server). */
    public void setColor(String c) {
        this.color = (c == null || c.isEmpty()) ? "black" : c;
        markDirty();
    }

    /** Set the fixed form (0..3) for a decoration variant, or -1 for auto-join. */
    public void setLockedForm(int f) {
        this.lockedForm = (f >= 0 && f <= 3) ? f : -1;
        markDirty();
    }

    /** Set the letter once at placement (server). Does not sync on its own — the flow does. */
    public void setLetter(char c) {
        this.letter = (c == 0) ? "" : String.valueOf(c);
        markDirty();
    }

    /**
     * Set the computed form. Returns true if it actually changed (so the updater only pushes a sync
     * packet when something moved). Caller pushes the update (markForUpdate) when true.
     */
    public boolean setForm(int newForm) {
        if (newForm == form) return false;
        form = newForm;
        markDirty();
        return true;
    }

    /**
     * Set whether this letter has a neighbour. Returns true if it changed (so the updater syncs even when
     * the form itself did not move — a non-connector gaining a neighbour keeps ISOLATED form but must sync).
     */
    public boolean setAttached(boolean a) {
        if (a == attached) return false;
        attached = a;
        markDirty();
        return true;
    }

    /** Set the back face's letter (mirror partner). Returns true if it changed (caller syncs). */
    public boolean setBackLetter(char c) {
        String s = (c == 0) ? "" : String.valueOf(c);
        if (s.equals(backLetter)) return false;
        backLetter = s;
        markDirty();
        return true;
    }

    /** Set the back face's form. Returns true if it changed (caller syncs). */
    public boolean setBackForm(int f) {
        if (f == backForm) return false;
        backForm = f;
        markDirty();
        return true;
    }

    /** Push the current form to tracking clients (no pack, no reload) — called by the flow. */
    public void sync() {
        if (world instanceof ServerWorld sw) sw.getChunkManager().markForUpdate(pos);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putString("letter", letter);
        nbt.putInt("form", form);
        nbt.putString("color", color);
        nbt.putInt("lockedForm", lockedForm);
        nbt.putBoolean("attached", attached);
        nbt.putString("backLetter", backLetter);
        nbt.putInt("backForm", backForm);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        letter = nbt.getString("letter");
        form = nbt.getInt("form");
        color = nbt.contains("color") ? nbt.getString("color") : "black";
        lockedForm = nbt.contains("lockedForm") ? nbt.getInt("lockedForm") : -1;
        attached = nbt.getBoolean("attached");
        backLetter = nbt.getString("backLetter");
        backForm = nbt.getInt("backForm");
    }

    /** Sync letter+form to the client when the chunk is first sent. */
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
