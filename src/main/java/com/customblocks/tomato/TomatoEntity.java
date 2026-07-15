/**
 * TomatoEntity.java — Group 32 (Explosive Tomato). Phase A ("Fly") + Phase B ("Boom").
 *
 * The thrown tomato itself: a snowball-shaped arc (ThrownItemEntity, gravity 0.03) with two twists —
 * it can be RIDDEN, and it leaves no particle trail (the tomato is meant to be stealthy).
 *
 * ── G32-RIDE-EJECT (A4, 2026-07-15): "sneak AGAIN to dismount" ───────────────────────────────────
 * You THROW the tomato while sneaking (that is the mount trigger), and vanilla dismounts any sneaking
 * passenger — so the throw-sneak, which lingers a few ticks, was ejecting the rider almost immediately.
 * The fix is a short ARMING window: while the ride is arming, the vehicle's tick() (which runs before the
 * passenger's tickRiding(), so it beats shouldDismount()) clears the rider's sneak flag, keeping them on.
 * The window closes the instant the rider lets go of sneak — from then on a FRESH sneak drops them, free
 * and unconditional, and the tomato flies on without them. A hard 1s cap means a rider who never releases
 * sneak still isn't trapped. This is the middle ground between the old "full kamikaze, no dismount" clear
 * (owner-rejected) and deleting the clear outright (which brought back the instant eject).
 *
 * ── The blast (Phase B) ─────────────────────────────────────────────────────────────────────────
 * A REAL vanilla explosion (ExplosionSourceType.TNT): ONE power drives crater + damage-through-armour +
 * knockback, exactly like TNT. Point-blank with no armour is lethal; full iron survives. Power is
 * tomatoBlastPower (a tomato named "nuke" jumps to NUKE_POWER). Griefing is ON, but the affected terrain is
 * snapshotted BEFORE it craters and restored after tomatoRestoreSeconds — see TomatoCraterManager (restore,
 * first-snapshot-wins, anti-dupe) and TomatoBlastBehavior (CB blocks + containers never break).
 *
 * Every detonation is preceded by a ~0.5s FUSE (owner-locked): a vanilla TNT hiss + a white flash & swell
 * (the client reads the synced FUSE tracker), which doubles as the rider's bail window — a sneak-off in that
 * gap dismounts and saves them (B3). The fuse fires on impact, the 10s airburst, and a ride-into-wall.
 *
 * Depends on: TomatoRegistry, CustomBlocksConfig (blast keys), TomatoBlastBehavior, TomatoCraterManager, TomatoSounds
 * Called by:  TomatoItem (throw / ride), the dispenser behaviour, the game (tick / collision)
 */
package com.customblocks.tomato;

import com.customblocks.CustomBlocksConfig;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

import java.util.HashMap;
import java.util.Map;

public class TomatoEntity extends ThrownItemEntity {

    /** Fly time before the tomato gives up (10s). Phase B turns this into the airburst detonation. */
    public static final int LIFETIME_TICKS = 200;

    /** Snowball gravity — the locked "snowball-like arc". */
    private static final double GRAVITY = 0.03;

    /** A ridden tomato falls slower, so the ride is a ride and not a two-block hop. */
    private static final double RIDDEN_GRAVITY = 0.012;

    /** A4 arming window: the throw-sneak is suppressed for at most this long (1s) if never released. */
    private static final int ARM_TIMEOUT_TICKS = 20;

    /** False until the rider lets go of the sneak they threw with (or the 1s cap hits); see the header note. */
    private boolean rideArmed = false;
    /** The entity age the current ride began at, or -1 when nobody is riding. */
    private int mountAge = -1;

    /** ~0.5s fuse before every blast — the tell, and the rider's bail window (owner-locked 2026-07-15). */
    public static final int FUSE_TICKS = 10;

    /** Power of a tomato named "nuke" (case-insensitive), overriding tomatoBlastPower. */
    public static final double NUKE_POWER = 12.0;

    /** Fuse countdown, synced to the client so the renderer can flash + swell the tomato (0 = not fusing). */
    private static final TrackedData<Integer> FUSE =
            DataTracker.registerData(TomatoEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public TomatoEntity(EntityType<? extends TomatoEntity> type, World world) {
        super(type, world);
    }

    /** Thrown by a player (A2/A3/A4). */
    public TomatoEntity(World world, LivingEntity thrower) {
        super(TomatoRegistry.TOMATO, thrower, world);
    }

    /** Fired by a dispenser (A6) — deliberately ownerless, so Phase B credits nobody for the kill. */
    public TomatoEntity(World world, double x, double y, double z) {
        super(TomatoRegistry.TOMATO, x, y, z, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(FUSE, 0);
    }

    @Override
    protected Item getDefaultItem() {
        return TomatoRegistry.ITEM;
    }

    @Override
    protected double getGravity() {
        return hasPassengers() ? RIDDEN_GRAVITY : GRAVITY;
    }

    /** One rider, and only one. */
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengerList().isEmpty();
    }

    /**
     * Water does not shake the rider loose (LivingEntity.baseTick would otherwise dismount).
     *
     * This is NOT the dismount block — that was the sneak force-clear, and it is deleted (G32-DISMOUNT).
     * Sneak gets you off, in water or out. This override only means a ride survives crossing a lake.
     */
    @Override
    public boolean shouldDismountUnderwater() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        armRideDismount(); // A4: keep the rider on until they release the throw-sneak (see header note)

        int fuse = dataTracker.get(FUSE);
        if (fuse > 0) {
            // Sit still and count down. The swell/flash play on the client; a rider still has this window to
            // sneak off and live (B3). At zero, it blows.
            setVelocity(Vec3d.ZERO);
            velocityModified = true;
            dataTracker.set(FUSE, --fuse);
            if (fuse <= 0) detonate();
            return;
        }
        // The 10s airburst: a tomato that never hits anything lights its fuse in mid-air rather than flying
        // on into unloaded chunks. Same fuse, same blast as an impact.
        if (age >= LIFETIME_TICKS) startFuse();
    }

    /** Begin the arming window for a fresh ride. Called by {@link TomatoItem} right after {@code startRiding}. */
    public void beginRide() {
        this.rideArmed = false;
        this.mountAge = this.age;
    }

    /**
     * A4 (G32-RIDE-EJECT): suppress the sneak that THREW the tomato so it doesn't instantly dismount the rider.
     * While arming, clear the rider's sneak flag (this runs before the passenger's tickRiding(), so it beats
     * vanilla's shouldDismount()). The window ends the moment the rider releases sneak — after that a fresh
     * sneak drops them normally — and hard-caps at 1s so a held-sneak rider is never trapped.
     */
    private void armRideDismount() {
        if (rideArmed || mountAge < 0) return;
        if (!(getFirstPassenger() instanceof PlayerEntity rider)) { rideArmed = true; mountAge = -1; return; }
        if (!rider.isSneaking() || age - mountAge >= ARM_TIMEOUT_TICKS) { rideArmed = true; return; }
        rider.setSneaking(false); // still holding the throw-sneak → keep them mounted this tick
    }

    /**
     * Impact lights the fuse — it does not detonate instantly (the ~0.5s tell is the bail window). No status
     * byte is sent (that would spawn a vanilla item-break puff): the tomato is stealthy; the fuse is the tell.
     */
    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!getWorld().isClient) startFuse();
    }

    /**
     * Light the ~0.5s fuse: freeze in place, play the vanilla TNT hiss, and start the countdown the renderer
     * reads for its flash + swell. Idempotent — a tomato already fusing (e.g. it keeps grazing the ground)
     * does not restart or stack it.
     */
    private void startFuse() {
        if (getWorld().isClient || dataTracker.get(FUSE) > 0) return;
        dataTracker.set(FUSE, FUSE_TICKS);
        setVelocity(Vec3d.ZERO);
        setNoGravity(true);
        velocityModified = true;
        getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_TNT_PRIMED,
                SoundCategory.NEUTRAL, 1.0F, 1.0F);
    }

    /** The current fuse value (0 = not fusing). Read by the client renderer for the flash + swell. */
    public int getFuse() {
        return dataTracker.get(FUSE);
    }

    /**
     * The blast: a real vanilla explosion (TNT source) at {@link #blastPower()} — one power drives crater,
     * damage (run through armour for free) and knockback, like TNT. Griefing is ON, but the affected terrain
     * is snapshotted BEFORE it craters and handed to {@link TomatoCraterManager} for a timed restore, and the
     * blast's own drops are cleared so a restored crater can't dupe items. CB blocks + containers never break
     * ({@link TomatoBlastBehavior}). A null damage source gives the vanilla explosion death line (Phase E adds
     * the 30 custom messages).
     */
    private void detonate() {
        if (getWorld().isClient) return;
        ServerWorld world = (ServerWorld) getWorld();

        float power = (float) blastPower();
        boolean fire = CustomBlocksConfig.tomatoBlastFire;
        double x = getX(), y = getY(), z = getZ();
        BlockPos centre = BlockPos.ofFloored(x, y, z);
        boolean restoring = CustomBlocksConfig.tomatoRestoreSeconds > 0;

        // Snapshot the terrain around the blast BEFORE it craters, so the destroyed blocks can be restored.
        int snapRadius = (int) Math.ceil(power * 1.5) + 1;
        Map<BlockPos, BlockState> pre = restoring ? TomatoCraterManager.snapshotBox(world, centre, snapRadius) : null;

        // Honour tomatoBlastKnockback=false (a real explosion always shoves) by restoring pre-blast velocities.
        Map<Entity, Vec3d> preVel = null;
        if (!CustomBlocksConfig.tomatoBlastKnockback) {
            preVel = new HashMap<>();
            for (Entity e : world.getOtherEntities(this, new Box(centre).expand(power * 2.0))) preVel.put(e, e.getVelocity());
        }

        Explosion explosion = world.createExplosion(this, null, TomatoBlastBehavior.INSTANCE,
                x, y, z, power, fire, World.ExplosionSourceType.TNT);

        if (preVel != null) {
            for (Map.Entry<Entity, Vec3d> en : preVel.entrySet()) { en.getKey().setVelocity(en.getValue()); en.getKey().velocityModified = true; }
        }

        // Restore only the blocks the blast actually destroyed (their pre-states); the manager applies
        // first-snapshot-wins. Then clear the blast's fresh drops so the restored crater can't dupe items.
        if (pre != null && !explosion.getAffectedBlocks().isEmpty()) {
            Map<BlockPos, BlockState> originals = new HashMap<>();
            for (BlockPos p : explosion.getAffectedBlocks()) {
                BlockState orig = pre.get(p);
                if (orig != null && !orig.isAir()) originals.put(p.toImmutable(), orig);
            }
            TomatoCraterManager.recordCrater(world, originals);
            for (ItemEntity drop : world.getEntitiesByClass(ItemEntity.class,
                    new Box(centre).expand(snapRadius + 1.0), e -> e.age <= 1)) {
                drop.discard();
            }
        }

        // A random splat (owner-locked) layered over the vanilla TNT boom the explosion already played.
        world.playSound(null, x, y, z, TomatoSounds.randomSplat(world.getRandom()),
                SoundCategory.NEUTRAL, 1.2F, 1.0F);

        discard();
    }

    /** Explosion power: {@link #NUKE_POWER} for a tomato named "nuke" (case-insensitive), else the config power. */
    private double blastPower() {
        return isNuke() ? NUKE_POWER : CustomBlocksConfig.tomatoBlastPower;
    }

    /** True when this tomato is named "nuke" (case-insensitive exact match) — the bigger-blast override. */
    private boolean isNuke() {
        return hasCustomName() && getCustomName() != null
                && "nuke".equalsIgnoreCase(getCustomName().getString());
    }
}
