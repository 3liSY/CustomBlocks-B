/**
 * TomatoSounds.java — Group 32 (Explosive Tomato) Phase B ("Boom").
 *
 * Registers the three CC0 splat sounds shipped in assets/customblocks/sounds/. One is picked at random per
 * blast (owner-locked) and layered over the vanilla TNT boom the explosion already plays. The fuse tell uses
 * vanilla ENTITY_TNT_PRIMED directly, so it is not registered here.
 *
 * Depends on: CustomBlocksMod (MOD_ID), sounds.json (assets/customblocks/sounds.json)
 * Called by:  CustomBlocksMod.onInitialize (register), TomatoEntity.detonate (randomSplat)
 */
package com.customblocks.tomato;

import com.customblocks.CustomBlocksMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

public final class TomatoSounds {

    private TomatoSounds() {} // static-only

    public static SoundEvent SPLAT_IMPACT;
    public static SoundEvent SPLAT_SQUISH;
    public static SoundEvent SPLAT_POP;

    /** The three splats, for the uniform random pick per blast. */
    private static SoundEvent[] SPLATS;

    /** Register the three splat SoundEvents. Call once, from onInitialize (before any tomato can detonate). */
    public static void register() {
        SPLAT_IMPACT = reg("tomato_squishsplat_impact");
        SPLAT_SQUISH = reg("tomato_squish_03");
        SPLAT_POP    = reg("tomato_squishpop");
        SPLATS = new SoundEvent[]{ SPLAT_IMPACT, SPLAT_SQUISH, SPLAT_POP };
    }

    private static SoundEvent reg(String path) {
        Identifier id = Identifier.of(CustomBlocksMod.MOD_ID, path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    /** One of the three splats, chosen uniformly at random per blast (owner-locked). */
    public static SoundEvent randomSplat(Random random) {
        return SPLATS[random.nextInt(SPLATS.length)];
    }
}
