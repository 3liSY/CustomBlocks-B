/**
 * SauceVisuals.java — Group 32 (Explosive Tomato) Phase D/E client-visual slice, SERVER side.
 *
 * The server half of the "full ground+screen sauce experience" (owner-locked 2026-07-17, the mesh stays the
 * only deferred piece). It spawns the custom {@link com.customblocks.particle.SauceParticles} — which the vanilla
 * particle packet forwards to every nearby client — and pushes the direct-hit screen overlay signal:
 *   • onBlast() — for each blast: red splat DECALS on the exposed side/ceiling faces of nearby blocks (walls,
 *     fences, stairs, slabs — D2), a DRIP or two running down each vertical face (D3), 3-5 tomato-chunk DEBRIS
 *     that fall and settle near the ground (D4), and a {@code SauceHitPayload} to every player caught in the
 *     blast so their client paints the screen-edge drip overlay (E8). The flat-ground puddle field itself is the
 *     real sauce BLOCK and stays in {@link SauceManager} — decals are only for the surfaces a block can't sit on.
 *   • the END_SERVER_TICK footprint sweep — a player who walks OFF a sauce puddle leaves a short red footprint
 *     trail that fades (E2): standing on sauce re-arms a step budget, and each step for a moment after leaving
 *     drops an alternating left/right splat mark.
 *
 * Server-only (never imports a client class — the particle TYPES are common; the drawing is client-side). No
 * interop with any other CB system.
 *
 * Depends on: SauceParticles, SauceRegistry, SauceHitPayload, Fabric ServerTickEvents + ServerPlayNetworking
 * Called by:  CustomBlocksMod.onInitialize (init), SauceManager.splatter (onBlast)
 */
package com.customblocks.tomato;

import com.customblocks.particle.SauceParticles;
import com.customblocks.network.payloads.SauceHitPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SauceVisuals {

    private SauceVisuals() {} // static-only

    /** Cap on decals per blast so a big nuke can't flood a client with particles. */
    private static final int MAX_DECALS = 26;
    /** After leaving sauce, a player drops footprints for up to this many steps, then the trail dries. */
    private static final int FOOTPRINT_STEPS = 10;
    /** Minimum horizontal move between two footprints (blocks). */
    private static final double FOOTPRINT_STRIDE = 0.4;

    /** Per-player footprint state (server thread only; cleared on stop). */
    private static final class Feet { int stepsLeft; double lastX, lastZ; boolean left; }
    private static final Map<UUID, Feet> FEET = new HashMap<>();

    /** Wire the footprint sweep + the server-stop clear. Call once from onInitialize. */
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(SauceVisuals::tickFootprints);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> FEET.clear());
    }

    // ── Per-blast decals / drips / debris + the direct-hit screen signal ─────────────────────────

    public static void onBlast(ServerWorld world, BlockPos centre, double power) {
        Random rnd = world.getRandom();
        spawnSurfaceDecals(world, centre, power, rnd); // D2 + D3
        spawnDebris(world, centre, power, rnd);         // D4
        signalDirectHits(world, centre, power);         // E8
    }

    /** D2/D3: red splat decals on exposed side/ceiling faces near the blast, with drips down vertical faces. */
    private static void spawnSurfaceDecals(ServerWorld world, BlockPos centre, double power, Random rnd) {
        int vr = Math.min((int) Math.round(power), 4);
        int placed = 0;
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -vr; dx <= vr && placed < MAX_DECALS; dx++) {
            for (int dy = -vr; dy <= vr && placed < MAX_DECALS; dy++) {
                for (int dz = -vr; dz <= vr && placed < MAX_DECALS; dz++) {
                    if (Math.sqrt(dx * dx + dy * dy + dz * dz) > vr + 0.5) continue;
                    m.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    BlockState state = world.getBlockState(m);
                    if (state.isAir() || state.isReplaceable() || state.isOf(SauceRegistry.SAUCE)) continue;
                    for (Direction face : Direction.values()) {
                        if (face == Direction.UP) continue;               // flat top = the sauce BLOCK's job
                        if (!world.getBlockState(m.offset(face)).isAir()) continue; // face not exposed to air
                        if (rnd.nextFloat() < 0.18f) continue;            // ~82% coverage (D2 target 75-85%), hugging the face
                        double px = m.getX() + 0.5 + face.getOffsetX() * 0.52;
                        double py = m.getY() + 0.5 + face.getOffsetY() * 0.52;
                        double pz = m.getZ() + 0.5 + face.getOffsetZ() * 0.52;
                        float size = 0.4f + rnd.nextFloat() * 0.5f;
                        // count=0 → directional/param mode: the client reads dx as the decal size hint.
                        world.spawnParticles(SauceParticles.SAUCE_SPLAT, px, py, pz, 0, size, 0.0, 0.0, 1.0);
                        placed++;
                        if (face.getOffsetY() == 0) { // vertical wall face → run 1-2 drips down it (D3)
                            int drips = 1 + rnd.nextInt(2);
                            for (int i = 0; i < drips; i++) {
                                double dpy = m.getY() + rnd.nextFloat(); // start somewhere on the face
                                world.spawnParticles(SauceParticles.SAUCE_DRIP, px, dpy, pz, 0,
                                        (rnd.nextFloat() - 0.5f) * 0.01f, -0.02, (rnd.nextFloat() - 0.5f) * 0.01f, 1.0);
                            }
                        }
                        if (placed >= MAX_DECALS) break;
                    }
                }
            }
        }
    }

    /** D4: 3-5 tomato-chunk debris flung near the ground; each gets its own outward+up velocity (count=0). */
    private static void spawnDebris(ServerWorld world, BlockPos centre, double power, Random rnd) {
        int count = 3 + rnd.nextInt(3);
        for (int i = 0; i < count; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2.0;
            double spd = 0.18 + rnd.nextDouble() * 0.16;
            double vx = Math.cos(ang) * spd;
            double vz = Math.sin(ang) * spd;
            double vy = 0.18 + rnd.nextDouble() * 0.22;
            double px = centre.getX() + 0.5 + Math.cos(ang) * 0.4;
            double pz = centre.getZ() + 0.5 + Math.sin(ang) * 0.4;
            world.spawnParticles(SauceParticles.SAUCE_DEBRIS, px, centre.getY() + 0.4, pz, 0, vx, vy, vz, 1.0);
        }
    }

    /** E8: tell every player caught in the blast to paint the screen-edge drip overlay (stronger up close). */
    private static void signalDirectHits(ServerWorld world, BlockPos centre, double power) {
        double hitR = power * 1.1;
        Vec3d c = Vec3d.ofCenter(centre);
        for (ServerPlayerEntity player : world.getPlayers()) {
            double d = player.getPos().distanceTo(c);
            if (d <= hitR) {
                float intensity = MathHelper.clamp(1.0f - (float) (d / hitR), 0.15f, 1.0f);
                ServerPlayNetworking.send(player, new SauceHitPayload(intensity));
            }
        }
    }

    // ── E2 footprint trail ────────────────────────────────────────────────────────────────────────

    private static void tickFootprints(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                Feet st = FEET.computeIfAbsent(player.getUuid(), k -> new Feet());
                BlockPos feet = player.getBlockPos();
                boolean onSauce = world.getBlockState(feet).isOf(SauceRegistry.SAUCE)
                        || world.getBlockState(feet.down()).isOf(SauceRegistry.SAUCE);
                if (onSauce) {
                    st.stepsLeft = FOOTPRINT_STEPS; // re-arm the trail while still standing in it
                    st.lastX = player.getX();
                    st.lastZ = player.getZ();
                    continue;
                }
                if (st.stepsLeft <= 0 || !player.isOnGround()) continue;
                double moved = Math.hypot(player.getX() - st.lastX, player.getZ() - st.lastZ);
                if (moved < FOOTPRINT_STRIDE) continue;
                // E2 (owner-locked 2026-07-17): alternating recognizable left/right prints, each a randomized smear.
                // The perpendicular offset puts left/right on opposite sides of the walk line; per-print size jitter
                // (a little bigger on the leading foot) makes the two prints read as distinct rather than identical.
                Random rnd = world.getRandom();
                double yaw = Math.toRadians(player.getYaw());
                double side = st.left ? 0.18 : -0.18;
                double ox = Math.cos(yaw) * side;
                double oz = Math.sin(yaw) * side;
                float smear = (st.left ? 0.30f : 0.24f) + rnd.nextFloat() * 0.12f;
                world.spawnParticles(SauceParticles.SAUCE_SPLAT,
                        player.getX() + ox, player.getY() + 0.03, player.getZ() + oz, 0, smear, 0.0, 0.0, 1.0);
                st.left = !st.left;
                st.lastX = player.getX();
                st.lastZ = player.getZ();
                st.stepsLeft--;
            }
        }
    }
}
