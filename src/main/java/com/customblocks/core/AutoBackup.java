/**
 * AutoBackup.java — Group 09, Slice 3 (timed automatic backups + prune).
 *
 * A daemon scheduler that, every {@code autoBackupInterval} minutes, snapshots the live data into an
 * "auto-…" backup and then prunes "auto-" backups beyond {@code autoBackupKeepCount}. It runs SILENTLY
 * (no chat) — only a log line per save — and is path-agnostic: it reuses the exact same, tested
 * {@link BackupManager#save} / {@link BackupManager#pruneAuto} as the manual commands.
 *
 * THREADING (matches the manual-save idiom in BackupCommands):
 *   • the scheduler thread wakes up and asks the SERVER thread to flush slots ({@code SlotManager.saveAll})
 *     and read the block count — slot state is only ever touched on the server thread;
 *   • the heavy file copy then runs on a separate single-thread IO worker, so it never hitches the tick.
 *
 * The interval is re-read from config every cycle (self-rescheduling), so editing autoBackupInterval and
 * reloading config takes effect on the next cycle without a restart. interval ≤ 0 disables it (re-checked
 * every 60s in case it's switched back on).
 *
 * Depends on: CustomBlocksConfig, BackupManager, SlotManager, IncidentRecorder, CustomBlocksMod (LOGGER).
 * Called by:  CustomBlocksMod (SERVER_STARTED → start, SERVER_STOPPING → stop).
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.CustomBlocksMod;
import net.minecraft.server.MinecraftServer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class AutoBackup {

    private AutoBackup() {} // static-only

    /** How often to re-check the config while auto-backup is disabled (interval ≤ 0). */
    private static final long DISABLED_RECHECK_SECONDS = 60L;

    private static ScheduledExecutorService scheduler;
    private static ExecutorService ioWorker;
    private static MinecraftServer server;
    private static volatile boolean running;
    /** Bumped on every (re)schedule; a fired tick whose gen is stale no-ops, so only the newest
     *  chain ever runs. Lets {@link #applyConfigChange} swap the interval with no cancel/race. */
    private static volatile int generation;

    /** Start the timer for {@code srv}. Safe to call again — it restarts cleanly. */
    public static synchronized void start(MinecraftServer srv) {
        stop();
        server = srv;
        running = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> daemon(r, "CustomBlocks-AutoBackup"));
        ioWorker  = Executors.newSingleThreadExecutor(r -> daemon(r, "CustomBlocks-AutoBackup-IO"));
        scheduleNext();
        CustomBlocksMod.LOGGER.info("[CustomBlocks] Auto-backup started ({}).",
                describeSchedule());
        // A changed jar version is exactly when the owner wants a pre-update restore point. Take one
        // once per new version, off-thread, best-effort — never blocks or fails server start.
        final ExecutorService w = ioWorker;
        if (w != null) w.submit(AutoBackup::maybeVersionChangeBackup);
    }

    /** A short human description of the active schedule for the startup log. */
    private static String describeSchedule() {
        LocalTime daily = parseTime(CustomBlocksConfig.autoBackupTime);
        if (daily != null) return "daily at " + CustomBlocksConfig.autoBackupTime + ", keep=" + CustomBlocksConfig.autoBackupKeepCount;
        if (CustomBlocksConfig.autoBackupInterval > 0)
            return "every " + CustomBlocksConfig.autoBackupInterval + " min, keep=" + CustomBlocksConfig.autoBackupKeepCount;
        return "disabled";
    }

    /**
     * Take a one-time SAFETY backup when the mod's version has changed since the last run (stored in
     * data/last_version.txt). Best-effort: any failure is logged and swallowed. Skips when live data is
     * missing/corrupt (nothing worth snapshotting, and the boot guard already warned).
     */
    private static void maybeVersionChangeBackup() {
        try {
            String current = net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getModContainer(CustomBlocksMod.MOD_ID)
                    .map(c -> c.getMetadata().getVersion().getFriendlyString())
                    .orElse(null);
            if (current == null) return;
            java.nio.file.Path marker = CbPaths.DATA.resolve("last_version.txt");
            String previous = null;
            if (java.nio.file.Files.isRegularFile(marker)) {
                previous = java.nio.file.Files.readString(marker, java.nio.charset.StandardCharsets.UTF_8).trim();
            }
            if (current.equals(previous)) return; // same version — nothing to do
            if (previous != null && BackupIntegrity.liveSlotsPresentAndValid()) {
                // only back up on an actual UPGRADE from a known previous version, not first-ever boot
                String name = BackupManager.generatedName(BackupManager.Kind.SAFETY);
                int blocks = 0;
                MinecraftServer srv = server;
                if (srv != null) blocks = SlotManager.assignedSlots().size();
                BackupManager.save(name, blocks, BackupManager.Kind.SAFETY, "pre-update " + previous + "→" + current, null);
                BackupRetention.pruneSafety(CustomBlocksConfig.safetyKeepCount);
                CustomBlocksMod.LOGGER.info("[CustomBlocks] Version change {} → {}: saved pre-update backup \"{}\".",
                        previous, current, name);
            }
            java.nio.file.Files.createDirectories(CbPaths.DATA);
            java.nio.file.Files.writeString(marker, current, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            IncidentRecorder.record("Version-change backup check failed", e);
        }
    }

    /** Stop the timer and release the server reference. Safe to call when not running. */
    public static synchronized void stop() {
        running = false;
        if (scheduler != null) { scheduler.shutdownNow(); scheduler = null; }
        if (ioWorker != null)  { ioWorker.shutdownNow();  ioWorker = null; }
        server = null;
    }

    private static synchronized void scheduleNext() {
        if (!running || scheduler == null) return;
        final int gen = ++generation;
        scheduler.schedule(() -> tick(gen), secondsUntilNext(), TimeUnit.SECONDS);
    }

    /** True if auto-backup is on in either mode (daily wall-clock, or every-N-minutes interval). */
    private static boolean isEnabled() {
        return parseTime(CustomBlocksConfig.autoBackupTime) != null || CustomBlocksConfig.autoBackupInterval > 0;
    }

    /** Seconds until the next backup should fire. Daily mode wins when a valid time is set; otherwise the
     *  minute interval; when neither is on, re-check periodically in case it's switched on. */
    private static long secondsUntilNext() {
        LocalTime daily = parseTime(CustomBlocksConfig.autoBackupTime);
        if (daily != null) {
            // Owner's zone: "04:00" must mean 4 AM for the owner, not 4 AM on a UTC host.
            LocalDateTime now = LocalDateTime.now(CustomBlocksConfig.OWNER_ZONE);
            LocalDateTime next = now.toLocalDate().atTime(daily);
            if (!next.isAfter(now)) next = next.plusDays(1); // already passed today → tomorrow
            return Math.max(1L, Duration.between(now, next).getSeconds());
        }
        int min = CustomBlocksConfig.autoBackupInterval;
        return (min <= 0) ? DISABLED_RECHECK_SECONDS : (long) min * 60L;
    }

    /** Parse "HH:mm" to a LocalTime, or null if empty/invalid (→ interval mode). */
    private static LocalTime parseTime(String s) {
        if (s == null) return null;
        s = s.trim();
        if (!s.matches("([01]\\d|2[0-3]):[0-5]\\d")) return null;
        try { return LocalTime.parse(s); } catch (Exception e) { return null; }
    }

    /** Apply a runtime interval change now: bumping the generation supersedes the pending wait, so the
     *  new interval starts counting immediately. No-op when stopped. */
    public static synchronized void applyConfigChange() {
        if (running) scheduleNext();
    }

    /** One scheduler wake-up: take a backup if enabled, then always reschedule the next cycle. A stale
     *  tick (its generation was superseded by a reschedule) returns without running or rescheduling. */
    private static void tick(int gen) {
        if (gen != generation) return;
        try {
            if (running && isEnabled()) takeBackup();
        } catch (Throwable t) {
            IncidentRecorder.record("Auto-backup tick failed", t);
        } finally {
            scheduleNext();
        }
    }

    private static void takeBackup() {
        MinecraftServer srv = server;
        if (srv == null) return;
        // Flush slot state + read the count on the server thread, then copy off-thread.
        srv.execute(() -> {
            SlotManager.saveAll();
            int blocks = SlotManager.assignedSlots().size();
            ExecutorService w = ioWorker;
            if (w == null || w.isShutdown()) return;
            w.submit(() -> copy(blocks));
        });
    }

    private static void copy(int blocks) {
        // P3 boot guard: if the live data looks lost/corrupt while good backups exist, do NOT snapshot the
        // empty state over that history — that would destroy the very restore point the owner needs.
        if (BackupIntegrity.dataLossSuspected()) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Auto-backup SKIPPED — live slots.json is missing/corrupt but "
                    + "backups exist. Not overwriting good history. Restore with /cb backup load <name>.");
            return;
        }
        String name = BackupManager.generatedName(BackupManager.Kind.AUTO);
        try {
            if (BackupManager.exists(name)) return; // same-second collision — skip silently (never happens at 1+ min)
            BackupManager.save(name, blocks, BackupManager.Kind.AUTO, "scheduled", null);
            int pruned = BackupRetention.pruneAuto(CustomBlocksConfig.autoBackupKeepCount);
            // Then trim by disk budget (dedup pool footprint), if the owner set one.
            pruned += BackupRetention.pruneAutoToBudgetMB(CustomBlocksConfig.autoBackupBudgetMB);
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Auto-backup \"{}\" saved ({} block(s)); pruned {} old auto-backup(s).",
                    name, blocks, pruned);
        } catch (Exception e) {
            IncidentRecorder.record("Auto-backup save failed for \"" + name + "\"", e);
        }
    }

    private static Thread daemon(Runnable r, String name) {
        Thread t = new Thread(r, name);
        t.setDaemon(true);
        return t;
    }
}
