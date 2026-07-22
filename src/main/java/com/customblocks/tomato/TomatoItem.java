/**
 * TomatoItem.java — Group 32 (Explosive Tomato) Phase A ("Fly").
 *
 * The throwable item. Right-click throws (A2), sprint-jump adds your momentum (A3), sneak+throw mounts
 * you to the tomato (A4), and a dispenser fires it as a cannon (A6, via ProjectileItem).
 *
 * Locked behaviour, so nobody "fixes" it later:
 *   • No cooldown — spamming is allowed on purpose.
 *   • The stack is consumed, and free in creative (decrementUnlessCreative).
 *   • The tomato is INERT as an item: it is a plain Item with no block/BE hooks, which is exactly why a
 *     chest full of them just drops on break (A5) instead of detonating. There is no code for that — the
 *     absence of code IS the feature. Do not give it a use-on/entity hook without re-reading A5.
 *
 * Depends on: TomatoEntity, TomatoRegistry
 * Called by:  the game (right-click), the vanilla dispenser (ProjectileItem), TomatoCommands / creative tab
 */
package com.customblocks.tomato;

import com.customblocks.command.Chat;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ProjectileItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TomatoItem extends Item implements ProjectileItem {

    /** Snowball-grade throw speed. */
    private static final float THROW_SPEED = 1.5F;

    /** The ride is meant to GO somewhere — faster, and dead straight (see divergence below). */
    private static final float RIDE_SPEED = 2.2F;

    /** Vanilla snowball spread. A ridden tomato uses 0: you steer it by aiming, so it must not wobble. */
    private static final float THROW_DIVERGENCE = 1.0F;

    /** Dispenser cannon: hotter than a hand throw (snowball power is 1.5). */
    private static final float DISPENSER_POWER = 2.5F;

    private static final float DISPENSER_UNCERTAINTY = 1.0F;

    // Item.Settings, spelled out: TomatoItem inherits BOTH Item.Settings and ProjectileItem.Settings,
    // so a bare "Settings" is ambiguous and will not compile.
    public TomatoItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        boolean ride = user.isSneaking(); // A4 — sneak + throw = kamikaze ride

        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW,
                SoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!world.isClient) {
            TomatoEntity tomato = new TomatoEntity(world, user);
            applyCustomName(tomato, stack); // B7 name hovers in flight · B8 "nuke" → power 12 (both live on the entity)
            tomato.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F,
                    ride ? RIDE_SPEED : THROW_SPEED, ride ? 0.0F : THROW_DIVERGENCE);

            // A3 — inherit the thrower's momentum, so a sprint-jump throw flies further. This is the bow's
            // behaviour, NOT the snowball's: vanilla snowballs ignore the thrower's velocity entirely.
            // The y term is dropped while grounded, or a running throw would nose-dive into the floor.
            Vec3d thrower = user.getVelocity();
            tomato.setVelocity(tomato.getVelocity()
                    .add(thrower.x, user.isOnGround() ? 0.0 : thrower.y, thrower.z));

            world.spawnEntity(tomato);
            // force=true: the tomato is a projectile, so it is not a "normal" mount and would refuse a rider.
            if (ride) {
                user.startRiding(tomato, true);
                tomato.beginRide(); // A4: arm "sneak-again-to-dismount" so the throw-sneak doesn't eject the rider
                // One-shot hotbar cue. It fades on vanilla's own timer and is never re-sent, so it can't stick.
                if (user instanceof ServerPlayerEntity rider) Chat.tool(rider, "Riding the tomato — sneak to hop off");
            }
        }

        user.incrementStat(Stats.USED.getOrCreateStat(this));
        stack.decrementUnlessCreative(1, user); // free in creative
        return TypedActionResult.success(stack, world.isClient());
    }

    // ── Dispenser (A6) ───────────────────────────────────────────────────────────────────────────────
    // Ownerless on purpose: a dispenser kill credits nobody (Phase E death messages depend on this).

    @Override
    public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
        TomatoEntity tomato = new TomatoEntity(world, pos.getX(), pos.getY(), pos.getZ());
        applyCustomName(tomato, stack); // a dispenser can fire a named / "nuke" tomato too
        return tomato;
    }

    /**
     * Copy an anvil-given custom name from the thrown STACK onto the ENTITY, and make it hover.
     * The thrown-item entity does NOT inherit the stack's name on its own, so without this a named tomato
     * shows nothing mid-air (B7) and a "nuke"-named tomato never triggers TomatoEntity.isNuke() (B8). We copy
     * only a real custom name — never the default "Explosive Tomato" — so an unnamed throw stays label-free.
     */
    private static void applyCustomName(TomatoEntity tomato, ItemStack stack) {
        Text name = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (name != null) {
            tomato.setCustomName(name);
            tomato.setCustomNameVisible(true);
        }
    }

    @Override
    public ProjectileItem.Settings getProjectileSettings() {
        return ProjectileItem.Settings.builder()
                .uncertainty(DISPENSER_UNCERTAINTY)
                .power(DISPENSER_POWER)
                .build();
    }
}
