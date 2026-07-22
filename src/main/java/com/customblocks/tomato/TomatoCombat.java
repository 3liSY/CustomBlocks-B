/**
 * TomatoCombat.java — Group 32 (Explosive Tomato) Phase E ("Combat & Entities"), server side.
 *
 * The blast's effects ON entities (everything that is not client-only rendering). Slipping itself is free
 * from {@link SauceBlock}'s slipperiness; footprints, the shield tint and the screen overlay are client
 * visuals and live elsewhere. What lives here is server-authoritative and owner-locked:
 *   • E9/E10 — {@link #explosionDamageSource}: the blast damages with ONE of 30 custom death-message damage
 *     types (data/customblocks/damage_type/tomato_0..29 + lang), picked at random, crediting the thrower.
 *     A dispenser tomato has no owner → the generic no-thrower line, nobody credited. A SELF-kill (the
 *     thrower dying to their own tomato, riding included) is routed to a distinct 30-line self-deprecating
 *     pool by {@link TomatoDamageSource} (owner-locked 2026-07-17). Falls back to the vanilla explosion
 *     source if the datapack type is somehow missing, so a blast can never crash on it.
 *   • E5 — pets (tamed wolf/cat/parrot) take ZERO explosion damage (ALLOW_DAMAGE veto), yet still get
 *     sauced and slip (that comes from the sauce block, untouched here).
 *   • E3 — hostile mobs are hard-stunned ~3s (Slowness 255 + AI disabled) — a rare edge-of-blast survivor's
 *     free-hit window (the blast usually 1-shots them, owner-locked E3: that's fine); the sweep re-enables AI.
 *   • E4 — a hit villager gets a trade-price bump on the thrower that CUSTOM-DECAYS linearly to zero over
 *     ~90s and REFRESHES to a full 90s on a re-hit (owner-locked 2026-07-17 — vanilla gossip decay is far too
 *     slow to observe in a test session). Angry particles + a brief scurry come with the hit.
 *
 * In-memory only (the stun list + the anger tracker clear on server stop), like {@link SauceManager}. No
 * interop with other CB systems.
 *
 * Depends on: TomatoEntity, TomatoDamageSource, CustomBlocksMod (MOD_ID), Fabric ServerLivingEntityEvents + Server(Tick/Lifecycle)Events
 * Called by:  CustomBlocksMod.onInitialize (init), TomatoEntity.detonate (explosionDamageSource, applyBlastEffects)
 */
package com.customblocks.tomato;

import com.customblocks.CustomBlocksMod;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.village.VillageGossipType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TomatoCombat {

    private TomatoCombat() {} // static-only

    /** How many custom death-message damage types exist (tomato_0 .. tomato_29). Keep in step with the datapack + lang. */
    public static final int DEATH_MESSAGE_COUNT = 30;

    /** Hostile-stun length: ~3s of Slowness 255 + disabled AI (owner-locked). */
    private static final int STUN_TICKS = 60;

    /** E4: full anger lifetime — the price bump decays linearly to zero over ~90s (owner-locked 2026-07-17). */
    private static final int ANGER_DURATION_TICKS = 90 * 20;

    /** E4: peak MINOR_NEGATIVE gossip applied on a hit — a noticeable trade-price rise that then decays off. */
    private static final int ANGER_PEAK = 30;

    /** One stunned mob and the server tick its AI is switched back on. */
    private record Stun(MobEntity mob, long until) {}

    /** Mobs currently AI-disabled by a blast, so the sweep can un-freeze them. Cleared on server stop. */
    private static final List<Stun> STUNNED = new ArrayList<>();

    /** One thrower's decaying anger on one villager: when we applied it, and how much gossip WE still hold in. */
    private static final class Anger { long start; int applied; }

    /** world → villager uuid → thrower uuid → anger. Server thread only; cleared on server stop (E4). */
    private static final Map<RegistryKey<World>, Map<UUID, Map<UUID, Anger>>> VILLAGER_ANGER = new HashMap<>();

    /** Re-entrancy guard so the guaranteed-kill blow (B2) can't recurse through its own AFTER_DAMAGE callback. */
    private static final ThreadLocal<Boolean> GUARANTEEING = ThreadLocal.withInitial(() -> false);

    /** Wire the pet-immunity veto, the B2 guaranteed-kill, the stun-release + anger-decay sweep, and the server-stop clear. Call once from onInitialize. */
    public static void init() {
        // E5: a tamed pet takes ZERO damage from a tomato blast (it still gets sauced + slips — that is the block).
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (source.getSource() instanceof TomatoEntity
                    && entity instanceof TameableEntity pet && pet.isTamed()) return false;
            return true;
        });
        // B2 guaranteed infinite damage (owner-locked 2026-07-17): anything the tomato blast DAMAGES but does not
        // outright kill — an armour/resistance survivor — is finished off. AFTER_DAMAGE fires only when the hit did
        // NOT kill, so a point-blank kill needs nothing here; a survivor gets a bypassing lethal blow next tick.
        // Exempt: a shield BLOCKER (blocked==true, E6), tamed pets (E5), and creative/spectator/invulnerable. This
        // never changes the blast radius, terrain, out-of-range zero damage, or knockback — it only guarantees the
        // kill inside the area the explosion already damaged.
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (GUARANTEEING.get() || blocked) return;
            if (!(source.getSource() instanceof TomatoEntity)) return;
            if (isBlastExempt(entity) || !entity.isAlive()) return;
            if (entity.getWorld() instanceof ServerWorld sw) sw.getServer().execute(() -> guaranteeKill(entity, source));
        });
        // Re-enable a stunned mob's AI once its window passes (or immediately if it despawned). Leak-safe.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            releaseStuns(server);
            decayVillagerAnger(server); // E4: linear ~90s villager-anger decay
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { STUNNED.clear(); VILLAGER_ANGER.clear(); });
    }

    private static void releaseStuns(MinecraftServer server) {
        if (STUNNED.isEmpty()) return;
        long now = server.getTicks();
        Iterator<Stun> it = STUNNED.iterator();
        while (it.hasNext()) {
            Stun s = it.next();
            if (s.mob().isRemoved() || now >= s.until()) {
                if (!s.mob().isRemoved()) s.mob().setAiDisabled(false);
                it.remove();
            }
        }
    }

    // ── B2 — guaranteed infinite damage ───────────────────────────────────────────────────────────

    /**
     * A target the tomato blast must never kill (owner-locked 2026-07-17): a tamed pet (E5), a creative/spectator
     * player, or anything otherwise invulnerable. Shield blockers are excluded upstream via the AFTER_DAMAGE
     * {@code blocked} flag, not here.
     */
    private static boolean isBlastExempt(LivingEntity e) {
        if (e instanceof TameableEntity pet && pet.isTamed()) return true;
        if (e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator())) return true;
        return e.isInvulnerable();
    }

    /**
     * Finish off a blast survivor with one lethal, bypassing blow through the SAME tomato damage source (so the
     * credited/self death message still fires). The tomato_N damage types carry {@code bypasses_armor} +
     * {@code bypasses_effects} tags, so this ignores armour, resistance, and protection; a stubborn modded boss
     * that still lives falls through to {@code kill()}. The {@link #GUARANTEEING} guard stops the blow from
     * re-triggering this via its own AFTER_DAMAGE callback.
     */
    private static void guaranteeKill(LivingEntity e, DamageSource source) {
        if (!e.isAlive() || isBlastExempt(e)) return;
        GUARANTEEING.set(true);
        try {
            e.damage(source, 1.0e9f);
            if (e.isAlive()) e.kill();
        } finally {
            GUARANTEEING.set(false);
        }
    }

    // ── E9/E10 — random death-message damage source ───────────────────────────────────────────────

    /**
     * The DamageSource the blast should deal: a random one of the 30 custom death-message types, with the
     * thrower as attacker (so its {@code .player} line names them). A dispenser tomato has a null owner →
     * the generic no-thrower line, credit to nobody. A self-kill is caught per-victim by
     * {@link TomatoDamageSource#getDeathMessage} and routed to the self-deprecating pool. Falls back to the
     * vanilla explosion source if the custom type can't be resolved, so a blast never throws on a missing entry.
     */
    public static DamageSource explosionDamageSource(ServerWorld world, TomatoEntity tomato) {
        int n = world.getRandom().nextInt(DEATH_MESSAGE_COUNT);
        RegistryKey<DamageType> key = RegistryKey.of(RegistryKeys.DAMAGE_TYPE,
                Identifier.of(CustomBlocksMod.MOD_ID, "tomato_" + n));
        return world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).getEntry(key)
                .<DamageSource>map(type -> new TomatoDamageSource(
                        (RegistryEntry<DamageType>) type, tomato, tomato.getOwner(), n))
                .orElseGet(() -> world.getDamageSources().explosion(tomato, tomato.getOwner()));
    }

    // ── E3/E4/E5 — per-entity blast effects (run after the explosion) ─────────────────────────────

    /** Stun hostiles (free-hit window), anger hit villagers (custom decay), leave tamed pets untouched. */
    public static void applyBlastEffects(ServerWorld world, TomatoEntity tomato, BlockPos centre, double power) {
        long until = world.getServer().getTicks() + STUN_TICKS;
        Box area = new Box(centre).expand(power * 2.0);
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (e instanceof TameableEntity pet && pet.isTamed()) continue;          // E5 — no stun, just sauced/slip
            if (e instanceof VillagerEntity villager) { angerVillager(world, villager, tomato); continue; } // E4
            if (e instanceof MobEntity mob && e instanceof Monster) stun(mob, until);   // E3 — hostiles only
        }
    }

    /** E3: freeze a hostile ~3s — Slowness 255 pins it, AI-disable opens the free-hit window; the sweep un-freezes. */
    private static void stun(MobEntity mob, long until) {
        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, STUN_TICKS, 255, false, false));
        mob.setAiDisabled(true);
        STUNNED.removeIf(s -> s.mob() == mob); // a re-hit refreshes, never double-tracks
        STUNNED.add(new Stun(mob, until));
    }

    /**
     * E4: raise the thrower's trade prices with the villager and start (or REFRESH) our own ~90s linear decay.
     * We inject a MINOR_NEGATIVE gossip bump ourselves and shave it back down in {@link #decayVillagerAnger},
     * so the anger is observably temporary instead of riding vanilla's day-cycle-slow decay. A re-hit strips
     * whatever we still hold and re-applies the full peak, resetting the clock to a fresh 90s (owner-locked —
     * no stacking, one shared timer per villager+thrower pair). Angry clouds + a brief scurry come with the hit.
     */
    private static void angerVillager(ServerWorld world, VillagerEntity villager, TomatoEntity tomato) {
        Entity owner = tomato.getOwner();
        if (owner != null) { // no thrower (dispenser) → nobody to blame, so no reputation hit
            UUID thrower = owner.getUuid();
            Anger a = VILLAGER_ANGER.computeIfAbsent(world.getRegistryKey(), k -> new HashMap<>())
                    .computeIfAbsent(villager.getUuid(), k -> new HashMap<>())
                    .computeIfAbsent(thrower, k -> new Anger());
            if (a.applied > 0) villager.getGossip().removeGossip(thrower, VillageGossipType.MINOR_NEGATIVE, a.applied);
            villager.getGossip().startGossip(thrower, VillageGossipType.MINOR_NEGATIVE, ANGER_PEAK);
            a.start = world.getServer().getTicks();
            a.applied = ANGER_PEAK;
        }
        villager.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 1, false, false));
        world.spawnParticles(ParticleTypes.ANGRY_VILLAGER,
                villager.getX(), villager.getBodyY(1.0) + 0.5, villager.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
    }

    /**
     * E4 sweep: ease every tracked anger's injected gossip down toward its elapsed linear target, removing the
     * entry (and any residual gossip) once ~90s elapses. A villager that is unloaded is held untouched until it
     * returns; one that stays gone past the full duration is dropped so the map can't leak.
     */
    private static void decayVillagerAnger(MinecraftServer server) {
        if (VILLAGER_ANGER.isEmpty()) return;
        long now = server.getTicks();
        Iterator<Map.Entry<RegistryKey<World>, Map<UUID, Map<UUID, Anger>>>> wIt = VILLAGER_ANGER.entrySet().iterator();
        while (wIt.hasNext()) {
            Map.Entry<RegistryKey<World>, Map<UUID, Map<UUID, Anger>>> we = wIt.next();
            ServerWorld world = server.getWorld(we.getKey());
            Iterator<Map.Entry<UUID, Map<UUID, Anger>>> vIt = we.getValue().entrySet().iterator();
            while (vIt.hasNext()) {
                Map.Entry<UUID, Map<UUID, Anger>> ve = vIt.next();
                VillagerEntity villager = (world != null && world.getEntity(ve.getKey()) instanceof VillagerEntity v) ? v : null;
                Iterator<Map.Entry<UUID, Anger>> tIt = ve.getValue().entrySet().iterator();
                while (tIt.hasNext()) {
                    Map.Entry<UUID, Anger> te = tIt.next();
                    Anger a = te.getValue();
                    long elapsed = now - a.start;
                    if (elapsed >= ANGER_DURATION_TICKS) { // fully decayed — strip our residual and drop it
                        if (villager != null && a.applied > 0)
                            villager.getGossip().removeGossip(te.getKey(), VillageGossipType.MINOR_NEGATIVE, a.applied);
                        tIt.remove();
                        continue;
                    }
                    if (villager == null) continue; // unloaded — hold this pair's state until it comes back
                    int target = (int) Math.round(ANGER_PEAK * (1.0 - (double) elapsed / ANGER_DURATION_TICKS));
                    if (target < a.applied) {
                        villager.getGossip().removeGossip(te.getKey(), VillageGossipType.MINOR_NEGATIVE, a.applied - target);
                        a.applied = target;
                    }
                }
                if (ve.getValue().isEmpty()) vIt.remove();
            }
            if (we.getValue().isEmpty()) wIt.remove();
        }
    }
}
