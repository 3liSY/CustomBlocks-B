/**
 * TimerHitbox.java — Group 31 (BuzzerGame) I4/I5 (per-part interaction hitboxes).
 *
 * The timer stand's block is INVISIBLE and its parts float/tilt/scale outside the block cell, so a clamped
 * 0–16px voxel could never match them (I4) and breaking read as slow/bugged (I5). Instead each part (base /
 * leg / screen) gets a server-side INTERACTION entity sized to the rendered band and positioned at its
 * bottom-centre, re-fit on every resize/rotate. The entity carries command tags (which vanilla persists) with
 * the owning stand's block pos + which part, so a click routes back to the right block even after a reload.
 * Boxes may overlap; MC's entity pick makes the nearest-crosshair part win.
 *
 * {@link BuzzerGameWand#onPartInteract} handles the clicks (wired to Fabric's Use/AttackEntityCallback in
 * {@link com.customblocks.CustomBlocksMod}); {@link com.customblocks.command.handlers.BuzzerGameCommands}
 * raycasts these entities to find "the stand you're aiming at" for the size/textcolor/textsize commands.
 *
 * Depends on: TimerDisplayVisual (band geometry: partBandsPx / renderedScale + the Render state), TimerPart
 * Called by:  TimerDisplayVisual (spawn/update/despawn), BuzzerGameWand, BuzzerGameCommands
 */
package com.customblocks.buzzergame;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.InteractionEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class TimerHitbox {

    private TimerHitbox() {} // static-only

    // Per-part INTERACTION hitbox full widths (block units at scale 1); generous + may overlap neighbours —
    // the nearest-crosshair part wins. Base ≈ footprint, leg widened so a thin leg is still clickable, screen a
    // square that also catches the tilted glass + the text floating just in front of it.
    private static final double HIT_BASE_W   = 0.85;
    private static final double HIT_LEG_W    = 0.5;
    private static final double HIT_SCREEN_W = 1.0;

    /** Command tag marking a stand-part hitbox; two more carry the owning stand's pos + part so a click routes
     *  back to the right block after a reload (vanilla persists entity command tags). */
    public static final String HIT_TAG  = "cb_timer_hit";
    private static final String HIT_POS  = "cb_timer_pos:";
    private static final String HIT_PART = "cb_timer_part:";

    /** Spawn one per-part hitbox tracking the rendered part; returns its UUID to persist. */
    public static UUID spawn(ServerWorld world, BlockPos pos, TimerDisplayVisual.Render r, TimerPart part) {
        InteractionEntity e = new InteractionEntity(EntityType.INTERACTION, world);
        e.readNbt(nbt(pos, r, part));
        world.spawnEntity(e);
        return e.getUuid();
    }

    /** Re-fit an existing per-part hitbox to the current render (position + size). */
    public static void push(ServerWorld world, @Nullable UUID id, BlockPos pos, TimerDisplayVisual.Render r, TimerPart part) {
        if (id == null || !(world.getEntity(id) instanceof InteractionEntity e)) return;
        NbtCompound n = nbt(pos, r, part);
        n.putUuid("UUID", id);
        e.readNbt(n);
    }

    private static NbtCompound nbt(BlockPos pos, TimerDisplayVisual.Render r, TimerPart part) {
        double[] bands = TimerDisplayVisual.partBandsPx(r.wholeScale(), r.baseScale(), r.legScale(), r.screenScale());
        double baseTop = bands[0] / 16.0, legTop = bands[1] / 16.0, screenBot = bands[2] / 16.0, screenTop = bands[3] / 16.0;
        double eff = TimerDisplayVisual.renderedScale(r.wholeScale());
        double yLo, yHi, width;
        switch (part) {
            case BASE -> { yLo = 0.0;       yHi = baseTop;   width = HIT_BASE_W   * eff * r.baseScale(); }
            case LEG  -> { yLo = baseTop;   yHi = legTop;    width = HIT_LEG_W    * eff * r.legScale(); }
            default   -> { yLo = screenBot; yHi = screenTop; width = HIT_SCREEN_W * eff * r.screenScale(); }
        }
        NbtCompound n = new NbtCompound();
        NbtList p = new NbtList();
        p.add(NbtDouble.of(pos.getX() + 0.5));
        p.add(NbtDouble.of(pos.getY() + yLo));
        p.add(NbtDouble.of(pos.getZ() + 0.5));
        n.put("Pos", p);
        n.putFloat("width", (float) Math.max(0.05, width));
        n.putFloat("height", (float) Math.max(0.05, yHi - yLo));
        n.putByte("response", (byte) 1); // register clicks (arm-swing + attack/interact)
        n.put("Tags", tags(pos, part));
        return n;
    }

    private static NbtList tags(BlockPos pos, TimerPart part) {
        NbtList tags = new NbtList();
        tags.add(NbtString.of(HIT_TAG));
        tags.add(NbtString.of(HIT_POS + pos.getX() + "," + pos.getY() + "," + pos.getZ()));
        tags.add(NbtString.of(HIT_PART + part.name()));
        return tags;
    }

    /** True if the entity is a stand-part hitbox (carries {@link #HIT_TAG}). */
    public static boolean isHitEntity(@Nullable Entity e) {
        return e != null && e.getCommandTags().contains(HIT_TAG);
    }

    /** The owning stand's block position encoded in a hitbox entity's tags, or null. */
    public static @Nullable BlockPos hitPos(Entity e) {
        for (String t : e.getCommandTags()) {
            if (t.startsWith(HIT_POS)) {
                String[] xyz = t.substring(HIT_POS.length()).split(",");
                if (xyz.length == 3) {
                    try {
                        return new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
                    } catch (NumberFormatException ignored) { }
                }
            }
        }
        return null;
    }

    /** The part a hitbox entity represents, or null. */
    public static @Nullable TimerPart hitPart(Entity e) {
        for (String t : e.getCommandTags()) {
            if (t.startsWith(HIT_PART)) {
                try {
                    return TimerPart.valueOf(t.substring(HIT_PART.length()));
                } catch (IllegalArgumentException ignored) { }
            }
        }
        return null;
    }
}
