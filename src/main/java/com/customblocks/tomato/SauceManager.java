/**
 * SauceManager.java — Group 32 (Explosive Tomato) Phase D ("Mess").
 *
 * The server-side sauce lifecycle. {@link SauceBlock} is stateless per-block; everything with a timer or a limit
 * lives here, on the server thread (blast placement + an END_SERVER_TICK sweep), so plain collections are safe:
 *   • splatter() lays a real 8-layer puddle field from a land blast — deep at the centre, thin at the splash edge
 *     — then makes ONE small bounded downhill spread of thin 1-layer patches (D13) and STOPS. There is no ongoing
 *     spreading work. All placement is hard-bounded per blast (root safety, owner-locked 2026-07-17).
 *   • Underwater/seabed blasts place NO sauce at all (D5 reversal, owner-locked 2026-07-17): they show only a
 *     small hard-capped burst of red approved-colour particles + bubbles that dissolves underwater. The old
 *     underwater clump + self-heal loop is GONE — no continuous re-placement that could lag/crash the server.
 *   • A per-chunk deque tracks puddles OLDEST-first; past {@code tomatoSauceCapPerChunk} the oldest is evicted
 *     first, strict FIFO, regardless of how dry it is (owner-locked — no "prefer near-dry" logic) (D11).
 *   • The sweep dries a puddle after {@code tomatoSauceDecaySeconds} and burns it off instantly with steam if it
 *     touches lava. Rain-washing was REMOVED entirely (D7, owner-locked 2026-07-17) — weather has no effect.
 *
 * In-memory only (cleared on server stop). Fully independent of every other CB system — no interop with slots,
 * craters, or CB tools (owner-locked) — with ONE allowed exception: the crater restore may
 * {@link #clearSauceForRestore} a puddle that is blocking a block's refill (D1).
 *
 * Depends on: SauceBlock, SauceRegistry, SauceVisuals, CustomBlocksConfig, Fabric ServerTickEvents + ServerLifecycleEvents
 * Called by:  CustomBlocksMod.onInitialize (init), TomatoEntity.detonate (splatter), TomatoCraterManager (clearSauceForRestore)
 */
package com.customblocks.tomato;

import com.customblocks.CustomBlocksConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class SauceManager {

    private SauceManager() {} // static-only

    private static final int MAX = SauceBlock.MAX_LAYERS;
    private static final int SWEEP_INTERVAL = 10;         // ticks between lifecycle sweeps (~0.5s = "instant" enough)

    /** Root safety (owner-locked): a single blast never lays more than this many sauce blocks, nuke included. */
    private static final int MAX_SAUCE_PER_BLAST = 64;
    /** Widest the land puddle field ever reaches, so a nuke can't try to coat a 40-block disc in one tick. */
    private static final int MAX_FIELD_RADIUS = 6;
    /** D13: how many thin downhill patches the one-shot spread may add, then it STOPS. */
    private static final int SPREAD_ATTEMPTS = 10;
    /** D5: hard cap on the underwater particle burst (no blocks, no queue). */
    private static final int UNDERWATER_PARTICLE_CAP = 40;
    /** Approved deep tomato red for the underwater particle burst (D5). */
    private static final Vector3f SAUCE_PARTICLE_RGB = new Vector3f(0.70f, 0.09f, 0.07f);

    private static final Direction[] HORIZONTALS = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };

    /** One placed puddle: where it is and when it dries out. */
    private record Puddle(BlockPos pos, long expireTick) {}

    /** world → chunkPosLong → puddles in that chunk, OLDEST first (for strict FIFO eviction). */
    private static final Map<RegistryKey<World>, Map<Long, ArrayDeque<Puddle>>> BY_WORLD = new HashMap<>();

    /** Wire the sweep + the server-stop clear. Call once from onInitialize. */
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(SauceManager::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> BY_WORLD.clear());
    }

    // ── Blast entry point ───────────────────────────────────────────────────────────────────────

    /** Splatter sauce from a blast: a bounded 8-layer land field (+ one downhill spread), or an underwater burst. */
    public static void splatter(ServerWorld world, BlockPos centre, double power) {
        Random rnd = world.getRandom();
        // Client visuals (D2-D4 decals/drips/debris) + the direct-hit screen overlay (E8) ride every blast.
        SauceVisuals.onBlast(world, centre, power);

        if (isUnderwater(world, centre)) { underwaterFx(world, centre, power); return; } // D5 — particles only, no blocks

        int radius = MathHelper.clamp((int) Math.round(power), 2, MAX_FIELD_RADIUS);
        long expire = world.getServer().getTicks() + (long) CustomBlocksConfig.tomatoSauceDecaySeconds * 20L;
        List<BlockPos> placed = new ArrayList<>();
        BlockPos.Mutable col = new BlockPos.Mutable();
        for (int dx = -radius; dx <= radius && placed.size() < MAX_SAUCE_PER_BLAST; dx++) {
            for (int dz = -radius; dz <= radius && placed.size() < MAX_SAUCE_PER_BLAST; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius) continue;
                // depth by distance: 8 at the centre, 1 at the splash edge, with a little jitter for chaos
                int layers = MathHelper.clamp(
                        (int) Math.round(MAX * (1.0 - dist / (radius + 0.5))) + (rnd.nextInt(3) - 1), 1, MAX);
                col.set(centre.getX() + dx, centre.getY(), centre.getZ() + dz);
                BlockPos surface = findSurface(world, col);
                if (surface != null) { placeSauce(world, surface, layers, expire); placed.add(surface); }
            }
        }
        spreadDownhill(world, placed, expire, rnd); // D13: one bounded downhill spread, then STOP
        world.playSound(null, centre, SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.BLOCKS, 1.0f, 0.7f);
    }

    /**
     * D13 (owner-locked 2026-07-17): a single, hard-bounded downhill spread. For a few edge puddles we place one
     * thin 1-layer patch on the column one step OVER and DOWN — biasing the sauce downhill — then stop completely.
     * There is no per-tick spreading and no growing queue: this runs once, at placement, and never again.
     */
    private static void spreadDownhill(ServerWorld world, List<BlockPos> placed, long expire, Random rnd) {
        int made = 0;
        for (BlockPos p : placed) {
            if (made >= SPREAD_ATTEMPTS || placed.isEmpty()) break;
            if (rnd.nextFloat() > 0.5f) continue;
            Direction dir = HORIZONTALS[rnd.nextInt(HORIZONTALS.length)];
            BlockPos surface = findSurface(world, p.offset(dir).down()); // one step over, one down → downhill bias
            if (surface != null && surface.getY() < p.getY() && world.getBlockState(surface).isAir()) {
                placeSauce(world, surface, 1, expire);
                made++;
            }
        }
    }

    /** True when the blast centre (this y or the one above) sits in water — the D5 no-sauce underwater path. */
    private static boolean isUnderwater(ServerWorld world, BlockPos centre) {
        return world.getFluidState(centre).isIn(FluidTags.WATER)
                || world.getFluidState(centre.up()).isIn(FluidTags.WATER);
    }

    /**
     * D5 (owner-locked 2026-07-17): underwater/seabed blasts still crater + restore (that is Phase B), but lay NO
     * sauce. All they show is a small HARD-CAPPED burst of red approved-colour particles + bubbles that dissolves
     * underwater — no blocks, no surface clumps, no self-heal loop, nothing queued.
     */
    private static void underwaterFx(ServerWorld world, BlockPos centre, double power) {
        int n = Math.min(UNDERWATER_PARTICLE_CAP, 12 + (int) (power * 2));
        Vec3d c = Vec3d.ofCenter(centre);
        double spread = Math.min(power, 3.0) * 0.5;
        world.spawnParticles(new DustParticleEffect(SAUCE_PARTICLE_RGB, 1.6f), c.x, c.y, c.z, n, spread, spread, spread, 0.0);
        world.spawnParticles(ParticleTypes.BUBBLE, c.x, c.y, c.z, n, spread, spread, spread, 0.05);
        world.playSound(null, centre, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 0.8f, 0.8f);
    }

    /** The block position sitting on the topmost solid surface within the blast column, or null if none. */
    private static BlockPos findSurface(ServerWorld world, BlockPos col) {
        int cy = col.getY();
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int y = cy + 3; y >= cy - 4; y--) {
            m.set(col.getX(), y, col.getZ());
            BlockState here = world.getBlockState(m);
            boolean canSit = here.isAir() || here.isReplaceable() || here.isOf(SauceRegistry.SAUCE);
            if (canSit && world.getBlockState(m.down()).isSideSolidFullSquare(world, m.down(), Direction.UP))
                return m.toImmutable();
        }
        return null;
    }

    /** Place (or deepen) a sauce block and track it for the lifecycle sweep + the per-chunk cap. */
    private static void placeSauce(ServerWorld world, BlockPos pos, int layers, long expire) {
        BlockState existing = world.getBlockState(pos);
        int newLayers = existing.isOf(SauceRegistry.SAUCE) ? existing.get(SauceBlock.LAYERS) + layers : layers;
        int finalLayers = MathHelper.clamp(newLayers, 1, MAX);
        world.setBlockState(pos, SauceRegistry.SAUCE.getDefaultState().with(SauceBlock.LAYERS, finalLayers));
        track(world, pos, expire);
    }

    // ── Per-chunk tracking + FIFO cap ───────────────────────────────────────────────────────────

    private static void track(ServerWorld world, BlockPos pos, long expire) {
        Map<Long, ArrayDeque<Puddle>> byChunk = BY_WORLD.computeIfAbsent(world.getRegistryKey(), k -> new HashMap<>());
        long chunk = ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4);
        ArrayDeque<Puddle> dq = byChunk.computeIfAbsent(chunk, k -> new ArrayDeque<>());
        dq.removeIf(p -> p.pos().equals(pos)); // de-dupe: a re-splatter on the same spot refreshes its timer
        dq.addLast(new Puddle(pos.toImmutable(), expire));
        int cap = CustomBlocksConfig.tomatoSauceCapPerChunk;
        while (dq.size() > cap) {
            Puddle oldest = dq.pollFirst(); // strict oldest-first, dry state ignored (owner-locked D11)
            if (oldest != null && world.getBlockState(oldest.pos()).isOf(SauceRegistry.SAUCE))
                world.removeBlock(oldest.pos(), false);
        }
    }

    /**
     * D1 (owner-locked 2026-07-17): the crater restore's ONE allowed touch of sauce. Force-clear the puddle on
     * {@code pos} — properly untracking it from its chunk's FIFO deque (not just a bare world-removal, or the
     * deque would keep a dangling entry) — with a small splash fx, so the restore can then place the original
     * block back on a now-empty spot.
     */
    public static void clearSauceForRestore(ServerWorld world, BlockPos pos) {
        Map<Long, ArrayDeque<Puddle>> byChunk = BY_WORLD.get(world.getRegistryKey());
        if (byChunk != null) {
            ArrayDeque<Puddle> dq = byChunk.get(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4));
            if (dq != null) dq.removeIf(p -> p.pos().equals(pos));
        }
        if (world.getBlockState(pos).isOf(SauceRegistry.SAUCE)) world.removeBlock(pos, false);
        splashFx(world, pos);
    }

    // ── Lifecycle sweep ─────────────────────────────────────────────────────────────────────────

    private static void tick(MinecraftServer server) {
        if (BY_WORLD.isEmpty() || server.getTicks() % SWEEP_INTERVAL != 0) return;
        long now = server.getTicks();
        for (Map.Entry<RegistryKey<World>, Map<Long, ArrayDeque<Puddle>>> we : BY_WORLD.entrySet()) {
            ServerWorld world = server.getWorld(we.getKey());
            if (world == null) continue;
            Iterator<Map.Entry<Long, ArrayDeque<Puddle>>> cit = we.getValue().entrySet().iterator();
            while (cit.hasNext()) {
                ArrayDeque<Puddle> dq = cit.next().getValue();
                Iterator<Puddle> it = dq.iterator();
                while (it.hasNext()) {
                    Puddle p = it.next();
                    BlockPos pos = p.pos();
                    if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) continue; // untouched until loaded
                    if (!world.getBlockState(pos).isOf(SauceRegistry.SAUCE)) { it.remove(); continue; } // gone
                    if (touchingLava(world, pos)) { burnOff(world, pos); it.remove(); continue; }
                    if (now >= p.expireTick()) { world.removeBlock(pos, false); it.remove(); }              // dried out
                }
                if (dq.isEmpty()) cit.remove();
            }
        }
    }

    private static boolean touchingLava(ServerWorld world, BlockPos pos) {
        for (Direction d : Direction.values())
            if (world.getFluidState(pos.offset(d)).isIn(FluidTags.LAVA)) return true;
        return false;
    }

    /** Lava contact: burn off instantly with a puff of steam + a hiss (owner-locked — any contact, not just direct). */
    private static void burnOff(ServerWorld world, BlockPos pos) {
        world.removeBlock(pos, false);
        Vec3d c = Vec3d.ofCenter(pos);
        world.spawnParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 12, 0.2, 0.2, 0.2, 0.02);
        world.playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.6f, 1.2f);
    }

    /** The small splash burst used by the crater-restore force-clear. */
    private static void splashFx(ServerWorld world, BlockPos pos) {
        Vec3d c = Vec3d.ofCenter(pos);
        world.spawnParticles(ParticleTypes.SPLASH, c.x, c.y, c.z, 8, 0.25, 0.1, 0.25, 0.0);
    }
}
