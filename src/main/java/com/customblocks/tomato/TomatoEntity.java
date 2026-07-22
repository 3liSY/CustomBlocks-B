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
 * sneak still isn't trapped.
 *
 * ── The blast (Phase B) — NO FUSE (owner-locked 2026-07-15, B1/B2) ────────────────────────────────
 * There is NO fuse: no ~0.5s tell, no TNT hiss, no white flash/swell, no bail window. The tomato
 * detonates INSTANTLY on ANY contact (block, entity, ground), and airbursts INSTANTLY at the 20s mark if
 * it hits nothing. The detonation is a REAL vanilla explosion (ExplosionSourceType.TNT): ONE power drives
 * crater + damage-through-armour + knockback, exactly like TNT. Point-blank with no armour is lethal; full
 * iron survives. Power is tomatoBlastPower (a tomato named "nuke" jumps to NUKE_POWER). Griefing is ON, but
 * the affected terrain is snapshotted BEFORE it craters and restored after tomatoRestoreSeconds — see
 * TomatoCraterManager (restore, first-snapshot-wins, anti-dupe) and TomatoBlastBehavior (CB blocks +
 * containers never break).
 *
 * B2 root cause (fixed 2026-07-15): vanilla ThrownItemEntity.onCollision DISCARDS the entity (and sends the
 * item-break status byte) the instant it hits anything. The old code called super.onCollision FIRST and only
 * then lit a fuse — but the discard had already killed the entity, so the blast never ran → zero damage, no
 * crater. The fix is to {@link #detonate()} DIRECTLY inside onCollision and NOT call super at all: no discard
 * race, no stray item-break puff. This single change delivers both B1 (instant contact blast) and B2 (damage).
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

import java.util.HashMap;
import java.util.Map;

public class TomatoEntity extends ThrownItemEntity {

    /**
     * Fly time before the tomato gives up and airbursts (20s, owner-locked 2026-07-15, was 10s). A tomato that
     * never hits anything detonates in mid-air here instead of flying on into unloaded chunks — instant, no fuse.
     */
    public static final int LIFETIME_TICKS = 400;

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

    /** Power of a tomato named "nuke" (case-insensitive), overriding tomatoBlastPower. */
    public static final double NUKE_POWER = 20.0;

    /**
     * Knockback reference: the imparted explosion push is scaled by {@code tomatoBlastKnockback / this}. 6.0 is
     * the normal blast power, so knockback == 6 reproduces a vanilla same-power shove and the locked 8 is punchier.
     */
    private static final double KNOCKBACK_VANILLA_REF = 6.0;

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
        super.tick(); // moves the tomato and, on any contact, fires onCollision → detonate() (instant, no fuse)
        if (getWorld().isClient || isRemoved()) return;
        armRideDismount(); // A4: keep the rider on until they release the throw-sneak (see header note)

        checkInterception(); // Phase C: a projectile hit / another tomato mid-air detonates it (flak)
        if (isRemoved()) return;

        // The 20s airburst: a tomato that never hits anything detonates in mid-air INSTANTLY (no fuse) rather
        // than flying on into unloaded chunks. A rider still aboard dies in the blast (owner-locked 2026-07-15).
        if (age >= LIFETIME_TICKS) detonate();
    }

    /**
     * Phase C interception (C1/C2). Scans a tight box around the tomato each server tick:
     *   • another {@link TomatoEntity} touching it → BOTH detonate (C2), each at its own spot; or
     *   • ANY other projectile (arrow, snowball, …) touching it → it detonates like flak (C1).
     * A deliberate mid-air trigger, separate from {@link #onCollision} (which fires on block/entity contact).
     * C3 (punch reflection/deflection) was removed entirely (owner-locked 2026-07-17): a melee punch now has
     * no special effect — only projectile/tomato overlaps intercept.
     */
    private void checkInterception() {
        for (Entity e : getWorld().getOtherEntities(this, getBoundingBox().expand(0.35))) {
            if (e instanceof TomatoEntity other && !other.isRemoved()) { // C2 — two tomatoes both explode
                other.detonate();
                detonate();
                return;
            }
            if (e instanceof ProjectileEntity) { // C1 — any incoming projectile sets it off (flak cannon)
                detonate();
                return;
            }
        }
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
     * Contact = instant blast (owner-locked 2026-07-15, B1/B2). We detonate DIRECTLY here and deliberately do
     * NOT call super.onCollision: vanilla would discard the entity (killing the blast before it runs — the B2
     * bug) and send an item-break status byte (a stray poof the stealthy tomato must not make). detonate()
     * does its own discard.
     */
    @Override
    protected void onCollision(HitResult hitResult) {
        if (getWorld().isClient) return;
        detonate();
    }

    /**
     * The blast: a real vanilla explosion (TNT source) at {@link #blastPower()} — one power drives crater,
     * damage (run through armour for free) and knockback, like TNT. Griefing is ON, but the affected terrain
     * is snapshotted BEFORE it craters and handed to {@link TomatoCraterManager} for a timed restore, and the
     * blast's own drops are cleared so a restored crater can't dupe items. CB blocks + containers never break
     * ({@link TomatoBlastBehavior}). A null damage source gives the vanilla explosion death line (Phase E adds
     * the 30 custom messages). Idempotent: a tomato already gone (e.g. contact + same-tick airburst) never
     * double-blasts.
     */
    private void detonate() {
        if (getWorld().isClient || isRemoved()) return;
        ServerWorld world = (ServerWorld) getWorld();

        float power = (float) blastPower();
        boolean fire = CustomBlocksConfig.tomatoBlastFire;
        double x = getX(), y = getY(), z = getZ();
        BlockPos centre = BlockPos.ofFloored(x, y, z);
        boolean restoring = CustomBlocksConfig.tomatoRestoreSeconds > 0;

        // Snapshot the terrain around the blast BEFORE it craters, so the destroyed blocks can be restored.
        int snapRadius = (int) Math.ceil(power * 1.5) + 1;
        Map<BlockPos, BlockState> pre = restoring ? TomatoCraterManager.snapshotBox(world, centre, snapRadius) : null;

        // Knockback is a tunable strength (owner-locked float=8, 2026-07-17): snapshot nearby entities' pre-blast
        // velocities, let the real explosion apply its shove, then rescale that delta by knockback / vanilla-ref.
        // knockback==6 is an unscaled vanilla shove, 8 is punchier, 0 cancels it. Skipped when the scale is ~1
        // (exact vanilla behaviour) so velocities are never touched needlessly.
        double kbScale = CustomBlocksConfig.tomatoBlastKnockback / KNOCKBACK_VANILLA_REF;
        Map<Entity, Vec3d> preVel = null;
        if (Math.abs(kbScale - 1.0) > 1.0e-4) {
            preVel = new HashMap<>();
            for (Entity e : world.getOtherEntities(this, new Box(centre).expand(power * 2.0))) preVel.put(e, e.getVelocity());
        }

        Explosion explosion = world.createExplosion(this, TomatoCombat.explosionDamageSource(world, this),
                TomatoBlastBehavior.INSTANCE, x, y, z, power, fire, World.ExplosionSourceType.TNT);

        if (preVel != null) {
            for (Map.Entry<Entity, Vec3d> en : preVel.entrySet()) {
                Entity e = en.getKey();
                Vec3d before = en.getValue();
                Vec3d delta = e.getVelocity().subtract(before); // the shove the explosion just imparted
                e.setVelocity(before.add(delta.multiply(kbScale)));
                e.velocityModified = true;
            }
        }

        // Restore only the blocks the blast actually destroyed (their pre-states); the manager applies
        // first-snapshot-wins. Then clear the blast's fresh drops so the restored crater can't dupe items.
        if (pre != null && !explosion.getAffectedBlocks().isEmpty()) {
            Map<BlockPos, BlockState> originals = new HashMap<>();
            for (BlockPos p : explosion.getAffectedBlocks()) {
                BlockState orig = pre.get(p);
                // E11 (owner-locked 2026-07-17, reversal): crops now RESTORE with the crater like any other block —
                // the full BlockState snapshot already carries the exact growth stage, so no crop special-casing.
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

        // Phase D ("Mess"): lay the sauce field — deep at the centre, thin at the edge (or floating clumps underwater).
        SauceManager.splatter(world, centre, power);

        // Phase E ("Combat"): stun hostiles (free-hit window), anger hit villagers (gossip), protect tamed pets.
        TomatoCombat.applyBlastEffects(world, this, centre, power);

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
