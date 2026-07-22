/**
 * WheelRing.java - Group 34 (Wheel of Fortune) v2 items A/B/F.
 *
 * The wheel's ring: the geometry every other class measures against, the per-spin sample of prizes, and the
 * two display-entity layers that make up the wheel face —
 *   - {@link #SLICES} alternating coloured carnival WEDGES (concrete blocks squashed into thin radial bars),
 *     spawned once when the wheel is placed and never touched again;
 *   - {@link #SLICES} prize ICONS drawn in their inventory look on top of the wedges, RE-ROLLED on every spin
 *     from a fresh random sample of the cached pool (design lock 2026-07-23).
 *
 * Geometry note (the whole package depends on it): the wheel is VERTICAL and turned to face the placer, so
 * its plane is spanned by world-up and one horizontal axis. Everything is expressed in the display entities'
 * own local frame at yaw {@code faceYaw}: local +X is {@link #axisX} (the in-plane horizontal), local +Y is
 * world up, local +Z is {@link #normal} (out of the wheel, toward the viewer). That is why a wedge can be
 * aimed radially with a single rotation about local Z — see {@link WheelDisplayVisual#quatZ}.
 *
 * Depends on: WheelPool (the cached prize list), WheelDisplayVisual (shared nbt builders)
 * Called by:  WheelBlockEntity (build on place, reroll per spin), WheelDisplayVisual (geometry)
 */
package com.customblocks.wheel;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class WheelRing {

    private WheelRing() {} // static-only

    // ------------------------------------------------------------------ ring geometry (design locks)
    /** Visible prizes per spin. 100 is the cap the owner locked: the full ~1300 pool would overlap into mush
     *  on a 20-block ring and load a 137-mod server for nothing. Every item stays reachable via the reroll. */
    public static final int SLICES = 100;
    /** Outer rim radius — the wheel measures ~20 blocks across. */
    public static final double OUTER_R = 10.0;
    /** Inner edge of the coloured wedge band (the hollow middle is the arrow pivot + the win popup). */
    public static final double INNER_R = 6.0;
    /** Where the prize icons sit: the middle of the wedge band. */
    public static final double ICON_R = (OUTER_R + INNER_R) / 2.0;
    /** Degrees one slice spans. */
    public static final double SLICE_DEG = 360.0 / SLICES;

    /** Wedge bar: radial length (the band depth), tangential width (one slice's arc at {@link #ICON_R}), and
     *  a paper-thin depth so the wheel reads as a flat face. */
    private static final float WEDGE_LEN = (float) (OUTER_R - INNER_R);
    private static final float WEDGE_WIDTH = (float) (2 * Math.PI * ICON_R / SLICES);
    private static final float WEDGE_THICK = 0.08f;
    /** Neighbouring bars overlap near the inner edge (they are rectangles, not true pie sectors), so alternate
     *  slices sit a hair apart in depth — the overlap layers cleanly instead of z-fighting. */
    private static final double WEDGE_STAGGER = 0.012;
    /** Prize icon scale — just under one slice's arc width, so 100 icons ring the wheel without smearing. */
    private static final float ICON_SCALE = 0.55f;

    /** The carnival palette, cycled around the ring. 100 slices divides by 4, so the seam never repeats a colour. */
    private static final Item[] WEDGE_COLORS = {
            Items.RED_CONCRETE, Items.YELLOW_CONCRETE, Items.LIME_CONCRETE, Items.LIGHT_BLUE_CONCRETE };

    // ------------------------------------------------------------------ geometry helpers

    /** The wheel's centre point (the hidden anchor block's centre). */
    public static Vec3d center(BlockPos anchor) {
        return Vec3d.ofCenter(anchor);
    }

    /** Out of the wheel face, toward whoever placed it — the display entities' local +Z at {@code faceYaw}. */
    public static Vec3d normal(float faceYaw) {
        double r = Math.toRadians(faceYaw);
        return new Vec3d(-Math.sin(r), 0, Math.cos(r));
    }

    /** The in-plane horizontal axis — the display entities' local +X at {@code faceYaw} (wheel angle 0). */
    public static Vec3d axisX(float faceYaw) {
        double r = Math.toRadians(faceYaw);
        return new Vec3d(Math.cos(r), 0, Math.sin(r));
    }

    /** A point on the wheel: {@code radius} out at {@code angleDeg}, pushed {@code depth} toward the viewer. */
    public static Vec3d point(BlockPos anchor, float faceYaw, double radius, double angleDeg, double depth) {
        double a = Math.toRadians(angleDeg);
        return center(anchor)
                .add(axisX(faceYaw).multiply(radius * Math.cos(a)))
                .add(0, radius * Math.sin(a), 0)
                .add(normal(faceYaw).multiply(depth));
    }

    /** The centre angle of slice {@code i}. */
    public static double sliceCenterDeg(int i) {
        return i * SLICE_DEG;
    }

    /** The slice an angle points INTO — this is what turns the arrow's final angle into the winner (item C). */
    public static int sliceAt(double angleDeg) {
        double norm = ((angleDeg % 360.0) + 360.0) % 360.0;
        return (int) (Math.round(norm / SLICE_DEG) % SLICES);
    }

    // ------------------------------------------------------------------ per-spin sample (item B)

    /**
     * {@code count} DISTINCT prizes drawn at random from the cached pool, by partial Fisher-Yates so no item
     * repeats inside one spin. Called fresh on every spin — that is what keeps all ~1300 items reachable
     * despite the 100-icon cap.
     */
    public static List<Item> sample(Random random, int count) {
        List<Item> pool = WheelPool.items();
        int n = pool.size();
        int take = Math.min(count, n);
        int[] idx = new int[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        List<Item> out = new ArrayList<>(take);
        for (int i = 0; i < take; i++) {
            int j = i + random.nextInt(n - i);
            int swap = idx[i];
            idx[i] = idx[j];
            idx[j] = swap;
            out.add(pool.get(idx[i]));
        }
        return out;
    }

    // ------------------------------------------------------------------ wedge layer (spawned once)

    /** Spawn the alternating coloured wedge ring. Spawned on placement only; the colours never change. */
    public static List<UUID> spawnWedges(ServerWorld world, BlockPos anchor, float faceYaw) {
        List<UUID> out = new ArrayList<>(SLICES);
        for (int i = 0; i < SLICES; i++) {
            DisplayEntity.ItemDisplayEntity e = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
            e.readNbt(wedgeNbt(anchor, faceYaw, i));
            world.spawnEntity(e);
            out.add(e.getUuid());
        }
        return List.copyOf(out);
    }

    private static NbtCompound wedgeNbt(BlockPos anchor, float faceYaw, int slice) {
        double angle = sliceCenterDeg(slice);
        double depth = (slice % 2 == 0) ? WEDGE_STAGGER : -WEDGE_STAGGER;
        Vec3d p = point(anchor, faceYaw, ICON_R, angle, depth);
        NbtCompound n = WheelDisplayVisual.baseNbt(p, faceYaw, "fixed");
        NbtCompound t = WheelDisplayVisual.transform(WEDGE_LEN, WEDGE_WIDTH, WEDGE_THICK);
        t.put("left_rotation", WheelDisplayVisual.quatZ(angle)); // aim the long axis radially outward
        n.put("transformation", t);
        n.put("item", WheelDisplayVisual.itemNbt(WEDGE_COLORS[slice % WEDGE_COLORS.length]));
        n.putString("item_display", "none"); // raw block model — a full cube, squashed by the scale above
        return n;
    }

    // ------------------------------------------------------------------ icon layer (re-rolled per spin)

    /** Spawn the prize-icon ring showing {@code items} (one per slice). */
    public static List<UUID> spawnIcons(ServerWorld world, BlockPos anchor, float faceYaw, List<Item> items) {
        List<UUID> out = new ArrayList<>(SLICES);
        for (int i = 0; i < SLICES; i++) {
            DisplayEntity.ItemDisplayEntity e = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
            e.readNbt(iconNbt(anchor, faceYaw, i, itemAt(items, i)));
            world.spawnEntity(e);
            out.add(e.getUuid());
        }
        return List.copyOf(out);
    }

    /** Re-draw the whole icon ring with a fresh sample — one push per slice, once per spin. */
    public static void pushIcons(ServerWorld world, List<UUID> icons, BlockPos anchor, float faceYaw, List<Item> items) {
        for (int i = 0; i < icons.size(); i++) {
            UUID id = icons.get(i);
            if (id == null || !(world.getEntity(id) instanceof DisplayEntity.ItemDisplayEntity e)) continue;
            NbtCompound n = iconNbt(anchor, faceYaw, i, itemAt(items, i));
            n.putUuid("UUID", id);
            e.readNbt(n);
        }
    }

    private static NbtCompound iconNbt(BlockPos anchor, float faceYaw, int slice, Item item) {
        Vec3d p = point(anchor, faceYaw, ICON_R, sliceCenterDeg(slice), WheelDisplayVisual.DEPTH_ICON);
        NbtCompound n = WheelDisplayVisual.baseNbt(p, faceYaw, "fixed");
        n.put("transformation", WheelDisplayVisual.transform(ICON_SCALE, ICON_SCALE, ICON_SCALE));
        n.put("item", WheelDisplayVisual.itemNbt(item));
        // "gui" = the inventory look: blocks come out isometric like their item icon, flat items stay flat.
        // Icons are NOT rotated with their wedge — upright reads far better across a 20-block ring.
        n.putString("item_display", "gui");
        return n;
    }

    /** The prize on slice {@code i}, tolerating a short/stale list (a reload before the first spin). */
    private static Item itemAt(@Nullable List<Item> items, int i) {
        if (items == null || items.isEmpty()) return Items.STONE; // never a denylisted item on the ring (item F)
        return items.get(i % items.size());
    }
}
