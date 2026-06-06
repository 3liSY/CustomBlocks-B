/**
 * SlotLighting.java
 *
 * Responsibility: Apply a glow change to blocks that are ALREADY placed in the world.
 * New placements pick up the configured glow via SlotBlock.getPlacementState; this handles
 * the blocks a player has already put down by rewriting their LIGHT state property, which
 * triggers Minecraft's normal light + client update.
 *
 * Bounded by design (avoids the "scan the whole world" performance pitfall): only loaded
 * chunks within a small radius of online players, and only non-empty chunk sections.
 *
 * Depends on: SlotBlock
 * Called by:  AttributeCommands.setglow (after SlotManager updates the data).
 */
package com.customblocks.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.Set;

public final class SlotLighting {

    /** Chunk radius around each player to refresh. Keeps the scan bounded. */
    private static final int CHUNK_RADIUS = 4;

    private SlotLighting() {} // static-only

    /** Rewrite the LIGHT state of every loaded, placed instance of {@code slotIndex} to {@code level}. */
    public static void applyToPlaced(MinecraftServer server, int slotIndex, int level) {
        if (server == null) return;
        final String slotKey = "slot_" + slotIndex;
        final int clamped = Math.max(0, Math.min(15, level));
        server.execute(() -> {
            Set<Long> seen = new HashSet<>();
            for (ServerWorld world : server.getWorlds()) {
                for (ServerPlayerEntity p : world.getPlayers()) {
                    int pcx = p.getChunkPos().x;
                    int pcz = p.getChunkPos().z;
                    for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                        for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                            int cx = pcx + dx;
                            int cz = pcz + dz;
                            if (!seen.add(ChunkPos.toLong(cx, cz))) continue;
                            WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                            if (chunk != null) scanChunk(world, chunk, slotKey, clamped);
                        }
                    }
                }
            }
        });
    }

    private static void scanChunk(ServerWorld world, WorldChunk chunk, String slotKey, int level) {
        ChunkSection[] sections = chunk.getSectionArray();
        int baseX = chunk.getPos().getStartX();
        int baseZ = chunk.getPos().getStartZ();
        int bottomY = world.getBottomY();
        for (int si = 0; si < sections.length; si++) {
            ChunkSection sec = sections[si];
            if (sec == null || sec.isEmpty()) continue;
            int baseY = bottomY + (si << 4);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState st = sec.getBlockState(x, y, z);
                        if (st.getBlock() instanceof SlotBlock sb
                                && sb.getSlotKey().equals(slotKey)
                                && st.get(SlotBlock.LIGHT) != level) {
                            BlockPos pos = new BlockPos(baseX + x, baseY + y, baseZ + z);
                            world.setBlockState(pos, st.with(SlotBlock.LIGHT, level), Block.NOTIFY_ALL);
                        }
                    }
                }
            }
        }
    }
}
