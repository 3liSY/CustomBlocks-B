/**
 * WheelBlockEntity.java - Group 34 (Wheel of Fortune) v2 items A-E.
 *
 * The logic behind a placed wheel. The block itself is an INVISIBLE anchor; everything the players see is the
 * display entities this class owns ({@link WheelDisplayVisual.Handles}), persisted by UUID so a reload
 * re-attaches to the wheel already standing in the world.
 *
 * The spin (design lock 2026-07-23, replacing v1's pre-picked fake):
 *   1. every spin re-rolls {@link WheelRing#SLICES} prizes onto the ring from the cached pool;
 *   2. a target SLICE is drawn uniformly at random, and the arrow is given a real easing curve — several fast
 *      turns, then a long decelerating coast — that ends exactly on that slice's centre angle;
 *   3. on landing the winner is READ BACK OUT of the arrow's final angle ({@link WheelRing#sliceAt}), so the
 *      announced item is by construction the icon the arrow is pointing at. There is no separate "winner"
 *      variable that the animation could disagree with.
 *
 * A spin in progress is deliberately NOT persisted: a mid-spin reload simply ends it, which is harmless for a
 * referee-run show. The last RESULT is persisted, so the popup a reload finds is the one it left.
 *
 * Depends on: WheelRing (geometry + reroll), WheelDisplayVisual (entities), WheelFx (audio), WheelPool
 * Called by:  WheelBlock (place / right-click / break / ticker), WheelBlockRegistry (singleton claim)
 */
package com.customblocks.wheel;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class WheelBlockEntity extends BlockEntity {

    /** Spin length in ticks — the locked ~8 second feel. */
    private static final int SPIN_TICKS = 160;
    /** Full turns the arrow makes before settling (random per spin, so no two spins look alike). */
    private static final int TURNS_MIN = 8;
    private static final int TURNS_MAX = 12;
    /** Deceleration curve: {@code 1-(1-p)^POWER}. 3 opens at ~67°/tick and tails off to nothing — fast loops,
     *  then a coast, then a settle. Anything under 180°/tick also keeps the client's rotation smoothing honest. */
    private static final double EASE_POWER = 3.0;
    /** Peg boundaries the clacks fire on. Early on the arrow crosses several per tick (capped at one clack a
     *  tick, which reads as a rattle); as it slows the crossings — and so the clacks — space out on their own. */
    private static final int PEGS = 20;
    /** Whir re-trigger interval while spinning. */
    private static final int WHIR_EVERY = 4;
    /** Pop-in length for the win popup, and the overshoot that gives it its bounce. */
    private static final int POPUP_IN_TICKS = 12;
    private static final double POPUP_OVERSHOOT = 1.9;
    /** Degrees the giant popup icon turns per tick (pushed every other tick, interpolated between). */
    private static final double POPUP_SPIN_PER_TICK = 1.4;
    /** How far the landing announcement reaches. */
    private static final double ANNOUNCE_RANGE = 48.0;

    /** Which way the wheel's face is turned — the display yaw, set from the placer once and then persisted. */
    private float faceYaw = 0f;
    /** Every display entity this wheel owns. */
    private WheelDisplayVisual.Handles handles = WheelDisplayVisual.Handles.empty();
    /** The prizes currently drawn on the ring (re-rolled every spin). */
    private List<Item> ringItems = List.of();
    /** The item the arrow last landed on; the popup holds it until the next spin. */
    private @Nullable Item result;
    /** The arrow's angle, kept across spins so the next one starts where the last stopped. */
    private double angle = 0.0;

    // Spin state — never persisted (a reload ends the spin).
    private boolean spinning = false;
    private int spinTick = 0;
    private double startAngle = 0.0;
    private double deltaAngle = 0.0;
    private int lastPeg = Integer.MIN_VALUE;

    // Popup animation state — cheap to rebuild, so also not persisted.
    private int popupTick = POPUP_IN_TICKS;
    private double popupSpin = 0.0;
    private boolean claimed = false;

    public WheelBlockEntity(BlockPos pos, BlockState state) {
        super(WheelBlockRegistry.BLOCK_ENTITY, pos, state);
    }

    public boolean isSpinning() { return spinning; }

    // ------------------------------------------------------------------ place / break lifecycle (item A)

    /** Turn the wheel's face toward the player who placed it. Call BEFORE {@link #ensureBuilt}. */
    public void faceTowards(float placerYaw) {
        faceYaw = placerYaw + 180f; // the display's front is its local +Z; +180 turns it back at the placer
        markDirty();
    }

    /**
     * Build the whole wheel around the anchor: wedge ring, first prize-icon ring, centre arrow, hidden popup,
     * and the click surfaces. A partially-built wheel (something was {@code /kill}ed) is torn down and rebuilt
     * so the handle lists always stay in step with what is actually standing.
     */
    public void ensureBuilt(ServerWorld world) {
        if (handles.complete()) return;
        WheelDisplayVisual.despawnAll(world, handles, pos);
        if (ringItems.size() != WheelRing.SLICES) ringItems = WheelRing.sample(world.getRandom(), WheelRing.SLICES);
        Item shown = result != null ? result : Items.CLOCK;
        UUID[] popup = WheelDisplayVisual.spawnPopup(world, pos, faceYaw, shown);
        handles = new WheelDisplayVisual.Handles(
                WheelRing.spawnWedges(world, pos, faceYaw),
                WheelRing.spawnIcons(world, pos, faceYaw, ringItems),
                WheelDisplayVisual.spawnArrow(world, pos, faceYaw, angle),
                popup[0], popup[1],
                WheelDisplayVisual.spawnHits(world, pos, faceYaw));
        WheelBlockRegistry.claim(world, pos);
        claimed = true;
        markDirty();
    }

    /** Remove every display entity this wheel owns (on break). */
    public void despawnAndClear(ServerWorld world) {
        WheelDisplayVisual.despawnAll(world, handles, pos);
        handles = WheelDisplayVisual.Handles.empty();
        spinning = false;
        result = null;
        WheelBlockRegistry.release(world, pos);
    }

    // ------------------------------------------------------------------ spin (items B + C)

    /**
     * Start a spin. Re-rolls the ring, clears the old popup, and lays down an easing curve that ends on a
     * uniformly-random slice. Returns false if a spin is already running (right-clicks mid-spin are ignored).
     */
    public boolean startSpin(ServerWorld world) {
        if (spinning) return false;
        if (WheelPool.size() == 0) return false;
        ensureBuilt(world);

        ringItems = WheelRing.sample(world.getRandom(), WheelRing.SLICES);
        WheelRing.pushIcons(world, handles.icons(), pos, faceYaw, ringItems);
        result = null;
        WheelDisplayVisual.pushPopup(world, handles.popupIcon(), handles.popupText(), pos, faceYaw,
                Items.CLOCK, 0f, 0.0); // scale 0 = the old winner is gone the moment the wheel moves

        int target = world.getRandom().nextInt(WheelRing.SLICES); // the honest, uniform landing draw
        int turns = TURNS_MIN + world.getRandom().nextInt(TURNS_MAX - TURNS_MIN + 1);
        double toTarget = WheelRing.sliceCenterDeg(target) - angle;
        toTarget = ((toTarget % 360.0) + 360.0) % 360.0;
        startAngle = angle;
        deltaAngle = turns * 360.0 + toTarget;
        spinning = true;
        spinTick = 0;
        lastPeg = Integer.MIN_VALUE;
        WheelFx.spinStart(world, WheelRing.center(pos));
        markDirty();
        return true;
    }

    /** Server ticker (wired by {@link WheelBlock#getTicker}): drive the spin, then the resting popup. */
    public static void serverTick(World world, BlockPos pos, BlockState state, WheelBlockEntity be) {
        if (!(world instanceof ServerWorld sw)) return;
        if (!be.claimed) { // re-claim the singleton after a restart, before anyone can place a second wheel
            WheelBlockRegistry.claim(sw, pos);
            be.claimed = true;
        }
        if (be.spinning) be.tickSpin(sw);
        else if (be.result != null) be.tickPopup(sw);
    }

    /** One frame of the spin: advance the eased angle, re-aim the arrow, and ride the audio down with it. */
    private void tickSpin(ServerWorld world) {
        spinTick++;
        if (spinTick >= SPIN_TICKS) {
            angle = startAngle + deltaAngle;
            WheelDisplayVisual.pushArrow(world, handles.arrow(), pos, faceYaw, angle, 0); // snap exactly on slice
            land(world);
            return;
        }
        double p = spinTick / (double) SPIN_TICKS;
        double remaining = 1.0 - p;
        angle = startAngle + deltaAngle * (1.0 - Math.pow(remaining, EASE_POWER));
        WheelDisplayVisual.pushArrow(world, handles.arrow(), pos, faceYaw, angle, 1);

        // Speed straight off the derivative of the same curve, normalised to its value at t=0 → 1..0.
        double speed = Math.pow(remaining, EASE_POWER - 1.0);
        Vec3d at = WheelRing.center(pos);
        if (spinTick % WHIR_EVERY == 0) WheelFx.whir(world, at, speed);
        int peg = (int) Math.floor((angle - startAngle) / (360.0 / PEGS));
        if (peg != lastPeg) {
            lastPeg = peg;
            WheelFx.clack(world, at, speed); // at most one a tick — a rattle when fast, single clacks when slow
        }
    }

    /** The arrow has stopped: read the winner OUT of its final angle, then celebrate (items C + D + E). */
    private void land(ServerWorld world) {
        spinning = false;
        int slice = WheelRing.sliceAt(angle);
        Item won = slice < ringItems.size() ? ringItems.get(slice) : Items.CLOCK;
        result = won;
        popupTick = 0;
        popupSpin = 0.0;
        WheelDisplayVisual.pushPopup(world, handles.popupIcon(), handles.popupText(), pos, faceYaw, won, 0f, 0.0);
        WheelFx.win(world, pos, faceYaw);
        announce(world, won);
        markDirty();
    }

    /** The resting popup: pop-in bounce for the first ticks, then a slow endless 3D turn. Holds until the next spin. */
    private void tickPopup(ServerWorld world) {
        if (result == null) return;
        popupSpin += POPUP_SPIN_PER_TICK;
        if (popupTick <= POPUP_IN_TICKS) popupTick++;
        if (popupTick % 2 != 0) return; // pushed every other tick; interpolation_duration=2 fills the gap
        WheelDisplayVisual.pushPopup(world, handles.popupIcon(), handles.popupText(), pos, faceYaw,
                result, popScale(popupTick), popupSpin);
    }

    /** Back-out easing: overshoots past full size then settles, so the popup lands with a bounce (item D2). */
    private static float popScale(int tick) {
        if (tick >= POPUP_IN_TICKS) return 1f;
        double x = tick / (double) POPUP_IN_TICKS - 1.0;
        return (float) (1.0 + (POPUP_OVERSHOOT + 1.0) * x * x * x + POPUP_OVERSHOOT * x * x);
    }

    /** Tell everyone near the wheel what it landed on. Show-only — the mod never hands the item out (item F). */
    private void announce(ServerWorld world, Item won) {
        String name = won.getName().getString();
        String line = Chat.PREFIX + CbFmt.BODY + "Wheel of Fortune landed on: "
                + CbFmt.VALUE + name + " " + CbFmt.OK + "✔";
        Vec3d at = WheelRing.center(pos);
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (p.squaredDistanceTo(at) <= ANNOUNCE_RANGE * ANNOUNCE_RANGE) Chat.toPlayer(p, line);
        }
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putFloat("faceYaw", faceYaw);
        nbt.putDouble("angle", angle);
        nbt.put("wedges", uuidList(handles.wedges()));
        nbt.put("icons", uuidList(handles.icons()));
        nbt.put("hits", uuidList(handles.hits()));
        if (handles.arrow() != null) nbt.putUuid("arrow", handles.arrow());
        if (handles.popupIcon() != null) nbt.putUuid("popupIcon", handles.popupIcon());
        if (handles.popupText() != null) nbt.putUuid("popupText", handles.popupText());
        NbtList ring = new NbtList();
        for (Item item : ringItems) ring.add(NbtString.of(Registries.ITEM.getId(item).toString()));
        nbt.put("ring", ring);
        if (result != null) nbt.putString("result", Registries.ITEM.getId(result).toString());
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        faceYaw = nbt.getFloat("faceYaw");
        angle = nbt.getDouble("angle");
        handles = new WheelDisplayVisual.Handles(
                readUuids(nbt, "wedges"), readUuids(nbt, "icons"),
                nbt.containsUuid("arrow") ? nbt.getUuid("arrow") : null,
                nbt.containsUuid("popupIcon") ? nbt.getUuid("popupIcon") : null,
                nbt.containsUuid("popupText") ? nbt.getUuid("popupText") : null,
                readUuids(nbt, "hits"));
        List<Item> ring = new ArrayList<>();
        for (NbtElement e : nbt.getList("ring", NbtElement.STRING_TYPE)) {
            Item item = Registries.ITEM.get(Identifier.of(e.asString()));
            if (item != Items.AIR) ring.add(item);
        }
        ringItems = List.copyOf(ring);
        result = nbt.contains("result") ? itemOrNull(nbt.getString("result")) : null;
        spinning = false; // never resume a spin across a reload
        popupTick = POPUP_IN_TICKS;
        claimed = false;
    }

    private static @Nullable Item itemOrNull(String id) {
        Item item = Registries.ITEM.get(Identifier.of(id));
        return item == Items.AIR ? null : item;
    }

    private static NbtList uuidList(List<UUID> ids) {
        NbtList list = new NbtList();
        for (UUID id : ids) list.add(NbtString.of(id.toString()));
        return list;
    }

    private static List<UUID> readUuids(NbtCompound nbt, String key) {
        List<UUID> out = new ArrayList<>();
        for (NbtElement e : nbt.getList(key, NbtElement.STRING_TYPE)) {
            try {
                out.add(UUID.fromString(e.asString()));
            } catch (IllegalArgumentException ignored) { /* a corrupt handle — the rebuild path covers it */ }
        }
        return List.copyOf(out);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup lookup) {
        return createNbt(lookup);
    }
}
