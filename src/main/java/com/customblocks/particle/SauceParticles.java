/**
 * SauceParticles.java — Group 32 (Explosive Tomato) Phase D/E visual slice. COMMON registration.
 *
 * The three custom particles the sauce splatter uses. They are spawned from the SERVER (see
 * {@link com.customblocks.tomato.SauceVisuals}) via {@code ServerWorld.spawnParticles}, which forwards them to
 * every nearby client through the vanilla particle packet — so registering the type here (common) plus a client
 * factory (in {@code CustomBlocksClient}, see {@code SauceParticleFx}) is all that is needed; no bespoke packet.
 *   • {@code SAUCE_SPLAT}   — a translucent red blob decal that clings to a hit surface (D2) and also draws the
 *                             fading footprint trail (E2).
 *   • {@code SAUCE_DRIP}    — a red droplet that runs down a sauced wall (D3).
 *   • {@code SAUCE_DEBRIS}  — a small tomato chunk that falls, rolls and squishes near a blast (D4).
 *
 * The particle CLASSES + client factories are client-only; this class only owns the shared types so common code
 * (the registration call in {@code CustomBlocksMod}, the spawns in {@code SauceVisuals}) never imports a client
 * class (ADR-009, same pattern as {@link MysteryParticles}).
 *
 * Depends on: Fabric particle API, Minecraft registries.
 * Called by:  CustomBlocksMod.onInitialize (register), SauceVisuals (spawn), CustomBlocksClient (factories).
 */
package com.customblocks.particle;

import com.customblocks.CustomBlocksMod;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class SauceParticles {

    private SauceParticles() {} // static-only

    /** Wall/surface decal blob (D2) + footprint mark (E2). */
    public static final SimpleParticleType SAUCE_SPLAT = FabricParticleTypes.simple();
    /** Droplet running down a sauced wall (D3). */
    public static final SimpleParticleType SAUCE_DRIP = FabricParticleTypes.simple();
    /** Tomato-chunk debris near a blast (D4). */
    public static final SimpleParticleType SAUCE_DEBRIS = FabricParticleTypes.simple();

    /** Register the three particle types. Called once from {@link CustomBlocksMod#onInitialize()}. */
    public static void register() {
        reg("sauce_splat", SAUCE_SPLAT);
        reg("sauce_drip", SAUCE_DRIP);
        reg("sauce_debris", SAUCE_DEBRIS);
    }

    private static void reg(String path, SimpleParticleType type) {
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of(CustomBlocksMod.MOD_ID, path), type);
    }
}
