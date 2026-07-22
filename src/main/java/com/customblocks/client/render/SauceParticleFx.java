/**
 * SauceParticleFx.java — Group 32 (Explosive Tomato) Phase D/E visual slice. CLIENT-ONLY.
 *
 * The three sauce particles ({@link com.customblocks.particle.SauceParticles}), each a small
 * {@link SpriteBillboardParticle}, plus their factories. The SERVER spawns them by position/velocity
 * (see {@code SauceVisuals}); this only draws them:
 *   • {@link Splat}  — a translucent red decal blob that clings where it is spawned (a wall/fence/stair face,
 *                      or a footstep), no gravity, holding for ~5s then fading out. Doubles as the footprint mark.
 *   • {@link Drip}   — a red droplet with light gravity that runs down a wall until it hits a block (D3).
 *   • {@link Debris} — a tomato chunk that falls, slides and settles (world-colliding, no bounce) (D4).
 *
 * Depends on: SpriteBillboardParticle, the three sauce particle sprites (particles/sauce_*.json).
 * Called by:  SauceVisuals (server spawn → vanilla particle packet), CustomBlocksClient (factory registration).
 */
package com.customblocks.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

@Environment(EnvType.CLIENT)
public final class SauceParticleFx {

    private SauceParticleFx() {}

    // ── Splat decal / footprint (D2 / E2) ────────────────────────────────────────────────────────
    @Environment(EnvType.CLIENT)
    public static class Splat extends SpriteBillboardParticle {
        Splat(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
            super(world, x, y, z, 0, 0, 0);
            this.velocityX = this.velocityY = this.velocityZ = 0.0; // clings where the blast put it
            this.gravityStrength = 0.0f;
            this.collidesWithWorld = false;
            // vx carries a size hint (server packs the intended scale there); fall back to a small blob.
            float size = vx > 0 ? (float) vx : 0.5f;
            this.scale = size * (0.85f + this.random.nextFloat() * 0.3f);
            this.maxAge = 90 + this.random.nextInt(30);
            this.setColor(1.0f, 1.0f, 1.0f);
            this.setSprite(sprites.getSprite(this.random));
        }

        @Override
        public void tick() {
            super.tick();
            // hold opaque, then fade out over the last ~25% of life so it doesn't just pop away
            float life = (float) this.age / this.maxAge;
            this.alpha = life < 0.75f ? 1.0f : Math.max(0.0f, 1.0f - (life - 0.75f) / 0.25f);
        }

        @Override
        public ParticleTextureSheet getType() { return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT; }
    }

    // ── Drip running down a wall (D3) ────────────────────────────────────────────────────────────
    @Environment(EnvType.CLIENT)
    public static class Drip extends SpriteBillboardParticle {
        Drip(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
            super(world, x, y, z, 0, 0, 0);
            this.velocityX = vx;
            this.velocityY = vy;              // starts near zero; gravity pulls it down the wall
            this.velocityZ = vz;
            this.gravityStrength = 0.04f;     // slower than water, faster than lava (owner-locked look)
            this.collidesWithWorld = true;    // runs down, then STOPS at the floor and pools there (D3 bottom puddle)
            this.scale = 0.24f + this.random.nextFloat() * 0.16f; // thin trail
            // D3 (owner-locked 2026-07-17): the streak + its bottom puddle fully decay after ~30s.
            this.maxAge = 560 + this.random.nextInt(120);
            this.setColor(1.0f, 1.0f, 1.0f);
            this.setSprite(sprites.getSprite(this.random));
        }

        @Override
        public void tick() {
            super.tick();
            this.velocityX *= 0.6; // keep it hugging the wall (kill lateral drift), let gravity own Y
            this.velocityZ *= 0.6;
            float life = (float) this.age / this.maxAge;
            this.alpha = life < 0.75f ? 1.0f : Math.max(0.0f, 1.0f - (life - 0.75f) / 0.25f);
        }

        @Override
        public ParticleTextureSheet getType() { return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT; }
    }

    // ── Tomato-chunk debris (D4) ─────────────────────────────────────────────────────────────────
    @Environment(EnvType.CLIENT)
    public static class Debris extends SpriteBillboardParticle {
        Debris(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
            super(world, x, y, z, vx, vy, vz);
            this.velocityX = vx;
            this.velocityY = vy;
            this.velocityZ = vz;
            this.gravityStrength = 0.9f;
            this.collidesWithWorld = true; // rolls + settles, no bounce (owner-locked D4)
            this.scale = 0.35f + this.random.nextFloat() * 0.25f;
            this.maxAge = 30 + this.random.nextInt(30);
            this.setColor(1.0f, 1.0f, 1.0f);
            this.setSprite(sprites.getSprite(this.random));
        }

        @Override
        public ParticleTextureSheet getType() { return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE; }
    }

    // ── Factories ────────────────────────────────────────────────────────────────────────────────

    @Environment(EnvType.CLIENT)
    public static class SplatFactory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider sprites;
        public SplatFactory(SpriteProvider sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientWorld world, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new Splat(world, x, y, z, vx, vy, vz, sprites);
        }
    }

    @Environment(EnvType.CLIENT)
    public static class DripFactory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider sprites;
        public DripFactory(SpriteProvider sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientWorld world, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new Drip(world, x, y, z, vx, vy, vz, sprites);
        }
    }

    @Environment(EnvType.CLIENT)
    public static class DebrisFactory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider sprites;
        public DebrisFactory(SpriteProvider sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientWorld world, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new Debris(world, x, y, z, vx, vy, vz, sprites);
        }
    }
}
