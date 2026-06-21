/**
 * ParticleFx.java
 *
 * Server-side particle feedback for the mod's events (Group 16, slice 4). The mod previously
 * emitted zero particles; this is the FX set. Each of the six FX categories
 * ({@link com.customblocks.CustomBlocksConfig#FX_CATEGORIES}) maps to a small particle burst
 * around the acting player. {@link #play} respects the per-category toggle
 * (config particlesEnabled_<category>); {@link #preview} ignores it (used by the GUI board so a
 * disabled category can still be sampled). Spawns are scheduled on the server thread, so callers
 * may invoke from anywhere (command feedback, GUI clicks, async completions).
 *
 * Depends on: CustomBlocksConfig (toggle map), vanilla ParticleTypes / ServerWorld
 * Called by:  Chat (success/error), GuiFx (gui/selection), ParticlesMenu (preview)
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksConfig;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class ParticleFx {

    private ParticleFx() {} // static-only

    /** Emit {@code category}'s effect for {@code p}, but only if that category is enabled. */
    public static void play(ServerPlayerEntity p, String category) {
        if (p == null || !CustomBlocksConfig.particlesOn(category)) return;
        emit(p, category);
    }

    /** Emit {@code category}'s effect for {@code p} regardless of its toggle (GUI sampling). */
    public static void preview(ServerPlayerEntity p, String category) {
        if (p == null) return;
        emit(p, category);
    }

    private static void emit(ServerPlayerEntity p, String category) {
        MinecraftServer s = p.getServer();
        if (s == null) return;
        s.execute(() -> {
            ServerWorld w = p.getServerWorld();
            if (w == null) return;
            spawn(w, p, category);
        });
    }

    private static void spawn(ServerWorld w, ServerPlayerEntity p, String category) {
        switch (category) {
            case "success"       -> ring(w, p, ParticleTypes.HAPPY_VILLAGER, 12, 0.85);
            case "error"         -> above(w, p, ParticleTypes.ANGRY_VILLAGER, 6);
            case "gui"           -> burst(w, p, ParticleTypes.ENCHANT, 6, 0.4, 0.4);
            case "selection"     -> burst(w, p, ParticleTypes.END_ROD, 8, 0.3, 0.02);
            case "bulk_complete" -> ring(w, p, ParticleTypes.TOTEM_OF_UNDYING, 28, 1.05);
            case "achievement"   -> column(w, p, ParticleTypes.FIREWORK, 22);
            default -> { /* unknown category — no-op */ }
        }
    }

    /** A flat ring of single particles at chest height around the player. */
    private static void ring(ServerWorld w, ServerPlayerEntity p, ParticleEffect fx, int count, double radius) {
        double cx = p.getX(), cy = p.getY() + 1.0, cz = p.getZ();
        for (int i = 0; i < count; i++) {
            double a = (Math.PI * 2 * i) / count;
            w.spawnParticles(fx, cx + Math.cos(a) * radius, cy, cz + Math.sin(a) * radius, 1, 0, 0, 0, 0.0);
        }
    }

    /** A small cloud just above the player's head. */
    private static void above(ServerWorld w, ServerPlayerEntity p, ParticleEffect fx, int count) {
        w.spawnParticles(fx, p.getX(), p.getY() + 2.2, p.getZ(), count, 0.25, 0.1, 0.25, 0.0);
    }

    /** A compact puff centred on the player. */
    private static void burst(ServerWorld w, ServerPlayerEntity p, ParticleEffect fx, int count, double spread, double speed) {
        w.spawnParticles(fx, p.getX(), p.getY() + 1.0, p.getZ(), count, spread, spread, spread, speed);
    }

    /** A vertical column rising from the player's feet. */
    private static void column(ServerWorld w, ServerPlayerEntity p, ParticleEffect fx, int count) {
        w.spawnParticles(fx, p.getX(), p.getY() + 1.0, p.getZ(), count, 0.15, 1.0, 0.15, 0.05);
    }
}
