/**
 * WheelDisplayVisual.java - Group 34 (Wheel of Fortune) v2 items A/C/D.
 *
 * Every entity the wheel is made of, and the NBT builders the rest of the package shares. The wheel body has
 * no real blocks at all (design lock 2026-07-23): it is display entities spawned around a hidden centre
 * anchor and driven through {@link net.minecraft.entity.Entity#readNbt}, the same server-authoritative
 * technique {@link com.customblocks.buzzergame.TimerDisplayVisual} uses, so every player sees it with no
 * client mod.
 *
 * This class owns the CENTRE pieces and the click surface:
 *   - the Minecraft-style arrow (an ITEM_DISPLAY of {@code minecraft:arrow}) pivoting on the centre;
 *   - the win popup (giant slow-spinning ITEM_DISPLAY icon + glowing TEXT_DISPLAY name banner);
 *   - the INTERACTION hitboxes (one at the centre, a ring of them over the rim) that make a wall of display
 *     entities right-clickable at all.
 * {@link WheelRing} owns the wedge + icon ring and builds it from the same {@link #baseNbt} helpers.
 *
 * Every spawned entity carries the {@link #TAG} command tag so {@link #despawnAll} can sweep the area and
 * leave nothing behind even if a UUID handle was lost (design lock: breaking the centre removes ALL of it).
 *
 * Depends on: EntityType.ITEM_DISPLAY / TEXT_DISPLAY / INTERACTION, DisplayEntity, WheelRing (geometry)
 * Called by:  WheelBlockEntity (spawn on place, push each tick, despawn on break), WheelRing (nbt helpers)
 */
package com.customblocks.wheel;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.InteractionEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class WheelDisplayVisual {

    private WheelDisplayVisual() {} // static-only

    /** Command tag on EVERY wheel entity — the safety net {@link #despawnAll} sweeps by. */
    public static final String TAG = "cb_wheel";
    /** Tag carrying the owning anchor's block pos, so a click routes back to the right wheel after a reload. */
    private static final String TAG_POS = "cb_wheel_pos:";
    /** Tag naming which click surface an INTERACTION entity is (CENTRE = the arrow, RIM = the wheel face). */
    private static final String TAG_PART = "cb_wheel_part:";

    /** The two click surfaces; both spin the wheel, only CENTRE removes it on a left-click. */
    public enum Part { CENTER, RIM }

    // ------------------------------------------------------------------ tunable geometry
    /** How far in front of the wheel plane each layer sits (blocks) — wedges 0, then icons, arrow, popup. */
    static final double DEPTH_ICON = 0.18;
    private static final double DEPTH_ARROW = 0.40;
    private static final double DEPTH_POPUP = 1.10;
    /** Arrow model scale. The sprite is 1 block square with the shaft on its diagonal, so the tip reaches
     *  {@code scale * 0.707} blocks out — 10 puts it at r≈7.1, just short of the icons at r=8. */
    private static final double ARROW_SCALE = 10.0;
    /** The arrow sprite is drawn pointing at 45° (bottom-left feathers → top-right tip), so a slice at wheel
     *  angle {@code a} needs the model rotated by {@code a - 45}. */
    private static final double ARROW_SPRITE_DEG = 45.0;
    /** Popup icon centre / name banner, offset from the wheel centre along the wheel's up axis. */
    private static final double POPUP_ICON_UP = 1.1;
    private static final double POPUP_TEXT_DOWN = 2.2;
    static final float POPUP_ICON_SCALE = 3.2f;
    static final float POPUP_TEXT_SCALE = 1.1f;
    /** Brand lime — the glow the winner's name banner is outlined in. */
    private static final int POPUP_GLOW = 0x40FF00;
    /** Faint dark plate behind the banner so the name reads over any sky/wall. */
    private static final int POPUP_BG = 0x90000000;
    /** Click surfaces: one box on the centre pivot plus a ring of them over the rim (right-click either). */
    private static final int HIT_RING = 20;
    private static final float HIT_CENTER_SIZE = 4.0f;
    private static final float HIT_RIM_SIZE = 2.8f;
    /** Half-extent of the sweep {@link #despawnAll} clears — comfortably past the 10-block rim. */
    private static final double SWEEP = 26.0;

    /** Every entity one placed wheel owns. Wedge/icon lists are parallel to {@link WheelRing#SLICES}. */
    public record Handles(List<UUID> wedges, List<UUID> icons, @Nullable UUID arrow,
                          @Nullable UUID popupIcon, @Nullable UUID popupText, List<UUID> hits) {

        public static Handles empty() {
            return new Handles(List.of(), List.of(), null, null, null, List.of());
        }

        /** True once the wheel is fully built (a partial set means a rebuild is due). */
        public boolean complete() {
            return arrow != null && popupIcon != null && popupText != null
                    && wedges.size() == WheelRing.SLICES && icons.size() == WheelRing.SLICES
                    && hits.size() == HIT_RING + 1;
        }
    }

    // ------------------------------------------------------------------ arrow

    /** Spawn the centre arrow pointing at {@code angleDeg}; returns its UUID. */
    public static UUID spawnArrow(ServerWorld world, BlockPos anchor, float faceYaw, double angleDeg) {
        DisplayEntity.ItemDisplayEntity e = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
        e.readNbt(arrowNbt(anchor, faceYaw, angleDeg, 0));
        world.spawnEntity(e);
        return e.getUuid();
    }

    /**
     * Re-aim the arrow. {@code interpolate} smooths the step client-side over one tick so a fast spin reads as
     * motion instead of a 20 fps stutter; the final landing frame is pushed with 0 so it snaps exactly on slice.
     */
    public static void pushArrow(ServerWorld world, @Nullable UUID id, BlockPos anchor, float faceYaw,
                                 double angleDeg, int interpolate) {
        if (id == null || !(world.getEntity(id) instanceof DisplayEntity.ItemDisplayEntity e)) return;
        NbtCompound n = arrowNbt(anchor, faceYaw, angleDeg, interpolate);
        n.putUuid("UUID", id);
        e.readNbt(n);
    }

    private static NbtCompound arrowNbt(BlockPos anchor, float faceYaw, double angleDeg, int interpolate) {
        Vec3d p = WheelRing.center(anchor).add(WheelRing.normal(faceYaw).multiply(DEPTH_ARROW));
        NbtCompound n = baseNbt(p, faceYaw, "fixed");
        n.putInt("interpolation_duration", interpolate);
        NbtCompound t = transform((float) ARROW_SCALE, (float) ARROW_SCALE, (float) ARROW_SCALE);
        t.put("left_rotation", quatZ(angleDeg - ARROW_SPRITE_DEG));
        n.put("transformation", t);
        n.put("item", itemNbt(net.minecraft.item.Items.ARROW));
        n.putString("item_display", "none"); // raw model — the flat sprite lies in the wheel plane
        return n;
    }

    // ------------------------------------------------------------------ popup (item D)

    /** Spawn the (initially hidden) win popup: giant icon + name banner. Returns {icon, text} UUIDs. */
    public static UUID[] spawnPopup(ServerWorld world, BlockPos anchor, float faceYaw, Item item) {
        DisplayEntity.ItemDisplayEntity icon = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
        icon.readNbt(popupIconNbt(anchor, faceYaw, item, 0f, 0));
        world.spawnEntity(icon);
        DisplayEntity.TextDisplayEntity text = new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world);
        text.readNbt(popupTextNbt(world, anchor, faceYaw, item, 0f));
        world.spawnEntity(text);
        return new UUID[]{ icon.getUuid(), text.getUuid() };
    }

    /**
     * Re-push the popup. {@code scale} is the pop-in factor (0 hides it entirely, 1 = full size) and
     * {@code spinDeg} is the icon's slow 3D rotation; the banner never spins so the name stays readable.
     */
    public static void pushPopup(ServerWorld world, @Nullable UUID iconId, @Nullable UUID textId,
                                 BlockPos anchor, float faceYaw, Item item, float scale, double spinDeg) {
        if (iconId != null && world.getEntity(iconId) instanceof DisplayEntity.ItemDisplayEntity e) {
            NbtCompound n = popupIconNbt(anchor, faceYaw, item, scale, spinDeg);
            n.putUuid("UUID", iconId);
            e.readNbt(n);
        }
        if (textId != null && world.getEntity(textId) instanceof DisplayEntity.TextDisplayEntity e) {
            NbtCompound n = popupTextNbt(world, anchor, faceYaw, item, scale);
            n.putUuid("UUID", textId);
            e.readNbt(n);
        }
    }

    private static NbtCompound popupIconNbt(BlockPos anchor, float faceYaw, Item item, float scale, double spinDeg) {
        Vec3d p = WheelRing.center(anchor)
                .add(WheelRing.normal(faceYaw).multiply(DEPTH_POPUP))
                .add(0, POPUP_ICON_UP, 0);
        NbtCompound n = baseNbt(p, faceYaw, "center"); // billboard: keeps facing whoever walks around it (D4)
        n.putInt("interpolation_duration", 2);         // the spin/pop-in is pushed every 2 ticks — smooth it
        float s = POPUP_ICON_SCALE * scale;
        NbtCompound t = transform(s, s, s);
        t.put("left_rotation", quatY(spinDeg));        // slow 3D turn inside the camera-facing frame (D1)
        n.put("transformation", t);
        n.put("item", itemNbt(item));
        n.putString("item_display", "none");
        return n;
    }

    private static NbtCompound popupTextNbt(ServerWorld world, BlockPos anchor, float faceYaw, Item item, float scale) {
        Vec3d p = WheelRing.center(anchor)
                .add(WheelRing.normal(faceYaw).multiply(DEPTH_POPUP))
                .add(0, -POPUP_TEXT_DOWN, 0);
        NbtCompound n = baseNbt(p, faceYaw, "center");
        n.putInt("interpolation_duration", 2);
        float s = POPUP_TEXT_SCALE * scale;
        n.put("transformation", transform(s, s, s));
        n.putString("text", Text.Serialization.toJsonString(item.getName(), world.getRegistryManager()));
        n.putInt("line_width", 400);
        n.putByte("text_opacity", (byte) -1);
        n.putInt("background", POPUP_BG);
        n.putBoolean("default_background", false);
        n.putBoolean("shadow", true);
        n.putBoolean("see_through", false);
        n.putString("alignment", "center");
        n.putBoolean("Glowing", true);                 // the "glowing banner" — outlined in brand lime
        n.putInt("glow_color_override", POPUP_GLOW);
        return n;
    }

    // ------------------------------------------------------------------ click surfaces

    /**
     * Spawn the click surfaces: one box on the centre pivot (the arrow) plus a ring of {@link #HIT_RING} boxes
     * over the rim, so right-clicking the wheel ANYWHERE spins it. Order matters — index 0 is the centre.
     */
    public static List<UUID> spawnHits(ServerWorld world, BlockPos anchor, float faceYaw) {
        List<UUID> out = new ArrayList<>(HIT_RING + 1);
        out.add(spawnHit(world, anchor, WheelRing.center(anchor), HIT_CENTER_SIZE, Part.CENTER));
        for (int i = 0; i < HIT_RING; i++) {
            Vec3d p = WheelRing.point(anchor, faceYaw, WheelRing.ICON_R, i * 360.0 / HIT_RING, DEPTH_ICON);
            out.add(spawnHit(world, anchor, p, HIT_RIM_SIZE, Part.RIM));
        }
        return List.copyOf(out);
    }

    private static UUID spawnHit(ServerWorld world, BlockPos anchor, Vec3d at, float size, Part part) {
        InteractionEntity e = new InteractionEntity(EntityType.INTERACTION, world);
        NbtCompound n = new NbtCompound();
        n.put("Pos", doubles(at.x, at.y - size / 2.0, at.z)); // INTERACTION grows UP from Pos — centre the box
        n.putFloat("width", size);
        n.putFloat("height", size);
        n.putByte("response", (byte) 1);
        NbtList tags = tagList();
        tags.add(NbtString.of(TAG_POS + anchor.getX() + "," + anchor.getY() + "," + anchor.getZ()));
        tags.add(NbtString.of(TAG_PART + part.name()));
        n.put("Tags", tags);
        e.readNbt(n);
        world.spawnEntity(e);
        return e.getUuid();
    }

    /** True if the entity is one of a wheel's click surfaces. */
    public static boolean isHit(@Nullable Entity e) {
        return e instanceof InteractionEntity && e.getCommandTags().contains(TAG);
    }

    /** The anchor position encoded in a click surface's tags, or null. */
    public static @Nullable BlockPos hitAnchor(Entity e) {
        for (String t : e.getCommandTags()) {
            if (!t.startsWith(TAG_POS)) continue;
            String[] xyz = t.substring(TAG_POS.length()).split(",");
            if (xyz.length != 3) continue;
            try {
                return new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
            } catch (NumberFormatException ignored) { /* a hand-edited tag — treat as not-a-wheel */ }
        }
        return null;
    }

    /** Which surface was clicked, defaulting to RIM (the harmless one) if the tag is missing. */
    public static Part hitPart(Entity e) {
        for (String t : e.getCommandTags()) {
            if (t.startsWith(TAG_PART)) {
                try {
                    return Part.valueOf(t.substring(TAG_PART.length()));
                } catch (IllegalArgumentException ignored) { /* fall through to RIM */ }
            }
        }
        return Part.RIM;
    }

    // ------------------------------------------------------------------ teardown

    /**
     * Remove EVERY entity of one wheel: the known handles first, then a tag sweep of the whole area so a
     * handle lost to a {@code /kill}, a crash or an old build can never leave debris behind (item A3).
     */
    public static void despawnAll(ServerWorld world, Handles h, BlockPos anchor) {
        h.wedges().forEach(id -> discard(world, id));
        h.icons().forEach(id -> discard(world, id));
        h.hits().forEach(id -> discard(world, id));
        discard(world, h.arrow());
        discard(world, h.popupIcon());
        discard(world, h.popupText());
        Box box = Box.of(WheelRing.center(anchor), SWEEP * 2, SWEEP * 2, SWEEP * 2);
        for (Entity e : world.getOtherEntities(null, box, e -> e.getCommandTags().contains(TAG))) {
            e.discard();
        }
    }

    static void discard(ServerWorld world, @Nullable UUID id) {
        if (id != null && world.getEntity(id) instanceof Entity e) e.discard();
    }

    // ------------------------------------------------------------------ shared nbt builders (used by WheelRing too)

    /** Position + facing + billboard mode + the fields every wheel display shares (fullbright, no stutter). */
    static NbtCompound baseNbt(Vec3d pos, float faceYaw, String billboard) {
        NbtCompound n = new NbtCompound();
        n.put("Pos", doubles(pos.x, pos.y, pos.z));
        NbtList rot = new NbtList();
        rot.add(NbtFloat.of(faceYaw));
        rot.add(NbtFloat.of(0f));
        n.put("Rotation", rot);
        n.putString("billboard", billboard);
        n.putInt("interpolation_duration", 0);
        n.putInt("teleport_duration", 0);
        n.putInt("start_interpolation", 0);
        n.putFloat("view_range", 2.5f);   // a 20-block wheel is watched from far back — keep it rendered
        NbtCompound brightness = new NbtCompound(); // fullbright so the wheel films the same day or night
        brightness.putInt("block", 15);
        brightness.putInt("sky", 15);
        n.put("brightness", brightness);
        n.put("Tags", tagList());
        return n;
    }

    /** An identity transform at the given scale; callers overwrite {@code left_rotation} to orient it. */
    static NbtCompound transform(float sx, float sy, float sz) {
        NbtCompound t = new NbtCompound();
        t.put("translation", floats(0f, 0f, 0f));
        t.put("scale", floats(sx, sy, sz));
        t.put("left_rotation", floats(0f, 0f, 0f, 1f));
        t.put("right_rotation", floats(0f, 0f, 0f, 1f));
        return t;
    }

    /** Rotation about the display's local Z — the wheel's face normal, so this turns things IN the wheel plane. */
    static NbtList quatZ(double degrees) {
        double half = Math.toRadians(degrees) / 2.0;
        return floats(0f, 0f, (float) Math.sin(half), (float) Math.cos(half));
    }

    /** Rotation about the display's local Y — the popup icon's slow 3D turn. */
    static NbtList quatY(double degrees) {
        double half = Math.toRadians(degrees) / 2.0;
        return floats(0f, (float) Math.sin(half), 0f, (float) Math.cos(half));
    }

    /** The {@code item} compound an ITEM_DISPLAY renders. */
    static NbtCompound itemNbt(Item item) {
        NbtCompound stack = new NbtCompound();
        stack.putString("id", Registries.ITEM.getId(item).toString());
        stack.putInt("count", 1);
        return stack;
    }

    private static NbtList tagList() {
        NbtList tags = new NbtList();
        tags.add(NbtString.of(TAG));
        return tags;
    }

    private static NbtList doubles(double... values) {
        NbtList list = new NbtList();
        for (double v : values) list.add(NbtDouble.of(v));
        return list;
    }

    static NbtList floats(float... values) {
        NbtList list = new NbtList();
        for (float v : values) list.add(NbtFloat.of(v));
        return list;
    }
}
