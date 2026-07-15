/**
 * MysteryParticles.java — Group 30 (Guess Mode) · G30 §R Total Blindness (R2). COMMON registration.
 *
 * Registers the ONE custom particle the mod owns: {@code customblocks:mystery_break} — a "?"-textured
 * debris particle. When the flagged holder breaks / mines a disguised block, the real block-break particles
 * (which sample the REAL texture and would leak the answer) are suppressed on their client and replaced with
 * a burst of these "?" particles instead — so the break still feels responsive without revealing the block
 * (G30 §R: R2). The particle CLASS + client factory + spawn helpers are client-only
 * ({@link com.customblocks.client.render.MysteryBreakParticle}); this class only owns the shared type so
 * common code (the registration call in {@code CustomBlocksMod}) never imports a client class (ADR-009).
 *
 * The particle's sprite comes from {@code assets/customblocks/particles/mystery_break.json} →
 * {@code textures/particle/mystery_break.png} (a copy of the bundled questionmark art).
 *
 * Depends on: Fabric particle API, Minecraft registries.
 * Called by:  CustomBlocksMod.onInitialize (register), the §R particle mixin (spawn), CustomBlocksClient (factory).
 */
package com.customblocks.particle;

import com.customblocks.CustomBlocksMod;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class MysteryParticles {

    private MysteryParticles() {} // static-only

    /** "?"-textured break/dig debris shown to a flagged holder instead of the real (leaking) block particles. */
    public static final SimpleParticleType MYSTERY_BREAK = FabricParticleTypes.simple();

    /** Register the particle type. Called once from {@link CustomBlocksMod#onInitialize()}. */
    public static void register() {
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of(CustomBlocksMod.MOD_ID, "mystery_break"), MYSTERY_BREAK);
    }
}
