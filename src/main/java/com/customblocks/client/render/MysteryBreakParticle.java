/**
 * MysteryBreakParticle.java — Group 30 (Guess Mode) · G30 §R Total Blindness (R2). CLIENT-ONLY.
 *
 * The "?"-textured debris particle (type {@link com.customblocks.particle.MysteryParticles#MYSTERY_BREAK}).
 * A flagged holder must not read the answer off the crack/debris a disguised block throws — those sample the
 * REAL texture. So the §R particle mixin cancels the vanilla burst on the holder's client and spawns THESE
 * instead: they fall + settle like normal block debris (motion mirrors vanilla {@code BlockDustParticle}) but
 * carry the "?" sprite, so breaking feels responsive and nothing about the real block leaks.
 *
 * A WATCHER (not flagged) is never touched — the mixin only fires for the local holder, so everyone else
 * still sees the real particles.
 *
 * Depends on: SpriteBillboardParticle, the mystery_break particle sprite (particles/mystery_break.json).
 * Called by:  ParticleDisguiseMixin (spawnBreak/spawnDig), CustomBlocksClient (Factory registration).
 */
package com.customblocks.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
public class MysteryBreakParticle extends SpriteBillboardParticle {

    protected MysteryBreakParticle(ClientWorld world, double x, double y, double z,
                                   double vx, double vy, double vz, SpriteProvider sprites) {
        super(world, x, y, z, vx, vy, vz);
        // Mirror BlockDustParticle: keep a little of the spawn spread, then bias to the passed velocity.
        this.velocityX = this.velocityX * 0.1 + vx;
        this.velocityY = this.velocityY * 0.1 + vy;
        this.velocityZ = this.velocityZ * 0.1 + vz;
        this.gravityStrength = 1.0f;
        this.scale *= 0.6f + this.random.nextFloat() * 0.4f;
        this.maxAge = (int) (4.0 / (this.random.nextDouble() * 0.9 + 0.1));
        this.collidesWithWorld = true;
        this.setSprite(sprites.getSprite(this.random));
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
    }

    /** Full-bright is fine — these are "?" markers, not lit debris; keeps them readable in any light. */
    @Override
    public int getBrightness(float tint) {
        return LightmapTextureManager.MAX_LIGHT_COORDINATE;
    }

    // ── Spawn helpers (called from the §R particle mixin) ───────────────────────────────────────────────

    /** The break burst: a small cloud of "?" debris spread through the block cell. */
    public static void spawnBreak(ClientWorld world, BlockPos pos) {
        for (int i = 0; i < 12; i++) {
            double px = pos.getX() + world.random.nextDouble();
            double py = pos.getY() + world.random.nextDouble();
            double pz = pos.getZ() + world.random.nextDouble();
            double vx = (world.random.nextDouble() - 0.5) * 0.2;
            double vy = world.random.nextDouble() * 0.2;
            double vz = (world.random.nextDouble() - 0.5) * 0.2;
            world.addParticle(com.customblocks.particle.MysteryParticles.MYSTERY_BREAK, px, py, pz, vx, vy, vz);
        }
    }

    /** One mining chip at the struck face, nudged just outside it. */
    public static void spawnDig(ClientWorld world, BlockPos pos, Direction face) {
        double px = pos.getX() + 0.5 + face.getOffsetX() * 0.55 + (world.random.nextDouble() - 0.5) * 0.3;
        double py = pos.getY() + 0.5 + face.getOffsetY() * 0.55 + (world.random.nextDouble() - 0.5) * 0.3;
        double pz = pos.getZ() + 0.5 + face.getOffsetZ() * 0.55 + (world.random.nextDouble() - 0.5) * 0.3;
        world.addParticle(com.customblocks.particle.MysteryParticles.MYSTERY_BREAK, px, py, pz, 0.0, 0.0, 0.0);
    }

    /** Client factory — bound to the mystery_break sprite set at registration. */
    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientWorld world, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new MysteryBreakParticle(world, x, y, z, vx, vy, vz, sprites);
        }
    }
}
