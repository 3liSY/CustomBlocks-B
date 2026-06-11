/**
 * AreaSelection.java
 *
 * Per-player two-corner block selection (Group 06) shared by the Rainbow Rectangle tool
 * and the Omni-Tool's Area mode. Right-clicking marks corner 1, then corner 2; a third
 * mark starts a new selection. In-memory only — selections don't persist across restarts.
 *
 * Depends on: Minecraft BlockPos
 * Called by:  RainbowRectangleItem, OmniToolItem (Area mode)
 */
package com.customblocks.core;

import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AreaSelection {

    private AreaSelection() {} // static-only

    /** Outcome of a mark: either the first corner, or the completed box with its volume. */
    public record Result(boolean firstCorner, BlockPos a, BlockPos b, int volume) {}

    private static final Map<UUID, BlockPos> CORNER_A = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> CORNER_B = new ConcurrentHashMap<>();

    /** Mark a corner for this player and report what happened. */
    public static Result mark(UUID player, BlockPos pos) {
        BlockPos a = CORNER_A.get(player);
        boolean haveBoth = a != null && CORNER_B.get(player) != null;
        if (a == null || haveBoth) {
            CORNER_A.put(player, pos);
            CORNER_B.remove(player);
            return new Result(true, pos, null, 0);
        }
        CORNER_B.put(player, pos);
        int dx = Math.abs(pos.getX() - a.getX()) + 1;
        int dy = Math.abs(pos.getY() - a.getY()) + 1;
        int dz = Math.abs(pos.getZ() - a.getZ()) + 1;
        return new Result(false, a, pos, dx * dy * dz);
    }

    public static void clear(UUID player) {
        CORNER_A.remove(player);
        CORNER_B.remove(player);
    }
}
