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

    // ---- Live health gauges for the IT Chest dashboard (Group 16, slice 1) -----------------

    /** Traffic-light status for a single gauge. The menu maps this to a colour. */
    public enum Health { GREEN, YELLOW, RED }

    /** A single health gauge: its status plus already-formatted gray hover lines. */
    public record Gauge(Health health, List<String> hover) {}

    /** TPS — green ≥18 / yellow 15–17 / red <15. */
    public static Gauge tps(MinecraftServer server) {
        if (server == null) return new Gauge(Health.YELLOW, List.of("§7TPS: §f— §8(no server)"));
        float tps = Math.min(20f, 1000f / Math.max(1, (float) server.getAverageTickTime()));
        Health h = tps >= 18f ? Health.GREEN : tps >= 15f ? Health.YELLOW : Health.RED;
        return new Gauge(h, List.of("§7TPS: §f" + String.format("%.1f", tps) + " §7/ 20.0"));
    }

    /** Block registry — green while used ≤ max, red if over capacity. */
    public static Gauge registry() {
        int used = SlotManager.usedSlots();
        int max  = SlotManager.getMaxSlots();
        Health h = used <= max ? Health.GREEN : Health.RED;
        return new Gauge(h, List.of(
                "§7Used: §f" + used + " §7/ §f" + max + " slots",
                "§7Free: §a" + Math.max(0, max - used)));
    }

    /** Network sync — best-effort: online count + pack SHA, no per-client lag. */
    public static Gauge networkSync(MinecraftServer server) {
        int online = server == null ? 0 : server.getCurrentPlayerCount();
        String hash = com.customblocks.network.ResourcePackServer.getHash();
        Health h = hash != null ? Health.GREEN : Health.YELLOW;
        return new Gauge(h, List.of(
                "§7Players online: §f" + online,
                "§7Pack SHA: §f" + shortHash(hash),
                "§8best-effort (no per-client lag)"));
    }

    /** Pack status — yellow rebuilding / red missing / green current. */
    public static Gauge packStatus() {
        java.io.File f = com.customblocks.network.ResourcePackServer.getPackFile();
        boolean rebuilding = com.customblocks.network.ResourcePackServer.isRebuilding();
        long size = (f != null && f.exists()) ? f.length() : 0L;
        Health h = rebuilding ? Health.YELLOW : (size <= 0 ? Health.RED : Health.GREEN);
        String when = (f != null && f.exists())
                ? new java.text.SimpleDateFormat("MMM d, HH:mm").format(new java.util.Date(f.lastModified()))
                : "—";
        return new Gauge(h, List.of(
                "§7Size: §f" + formatBytes(size),
                "§7SHA-1: §f" + shortHash(com.customblocks.network.ResourcePackServer.getHash()),
                "§7Last rebuild: §f" + when,
                rebuilding ? "§eRebuilding…" : ""));
    }

    /** Memory — green <70% / yellow 70–85% / red >85% of heap. */
    public static Gauge memory() {
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        long usedB = mem.getHeapMemoryUsage().getUsed();
        long maxB  = mem.getHeapMemoryUsage().getMax();
        long usedMb = usedB / (1024 * 1024);
        long maxMb  = maxB  / (1024 * 1024);
        int pct = maxB > 0 ? (int) (usedB * 100 / maxB) : 0;
        Health h = pct < 70 ? Health.GREEN : pct <= 85 ? Health.YELLOW : Health.RED;
        return new Gauge(h, List.of(
                "§7Heap: §f" + usedMb + " §7/ §f" + maxMb + " MB",
                "§7Usage: §f" + pct + "%"));
    }

    private static String shortHash(String hash) {
        if (hash == null || hash.isBlank()) return "—";
        return hash.length() > 8 ? hash.substring(0, 8) : hash;
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
