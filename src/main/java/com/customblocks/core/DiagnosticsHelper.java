/**
 * DiagnosticsHelper.java
 *
 * Responsibility: Read-only snapshot of CustomBlocks system state for /cb diag.
 * Never mutates any manager's state. Safe to call at any time from any thread.
 *
 * Depends on: SlotManager, CustomBlocksConfig, standard Java
 * Called by: DiagnosticsCommands
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksConfig;
import net.minecraft.server.MinecraftServer;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class DiagnosticsHelper {

    private DiagnosticsHelper() {}

    /** Collect system state as formatted display lines for /cb diag. */
    public static List<String> collect(MinecraftServer server) {
        List<String> lines = new ArrayList<>();
        lines.add("§6=== CustomBlocks Diagnostics ===");

        int used = SlotManager.usedSlots();
        int max  = CustomBlocksConfig.maxSlots;
        lines.add("§fSlots used: §e" + used + " / " + max);
        lines.add("§fUndo mode: §e" + CustomBlocksConfig.undoMode);
        lines.add("§fTexture size: §e" + CustomBlocksConfig.textureSize + "px");
        lines.add("§fHTTP: §e" + CustomBlocksConfig.httpHost + ":" + CustomBlocksConfig.httpPort);
        lines.add("§fHUD enabled: §e" + CustomBlocksConfig.hudEnabled);

        long packBytes = fileSize(Path.of("config/customblocks/pack.zip"));
        lines.add("§fResource pack: §e" + formatBytes(packBytes));

        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        long usedMb = mem.getHeapMemoryUsage().getUsed()  / (1024 * 1024);
        long maxMb  = mem.getHeapMemoryUsage().getMax()   / (1024 * 1024);
        lines.add("§fHeap: §e" + usedMb + " MB / " + maxMb + " MB");

        if (server != null) {
            float tps = Math.min(20f, 1000f / Math.max(1, (float) server.getAverageTickTime()));
            lines.add("§fTPS: §e" + String.format("%.1f", tps));
            lines.add("§fPlayers online: §e" + server.getCurrentPlayerCount());
        }
        lines.add("§7Snapshot: " + Instant.now());
        return lines;
    }

    private static long fileSize(Path p) {
        try { return Files.size(p); } catch (Exception e) { return 0; }
    }

    private static String formatBytes(long b) {
        if (b <= 0) return "n/a";
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return (b / 1024) + " KB";
        return String.format("%.1f MB", b / (1024.0 * 1024));
    }
}
