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
        CustomBlocksMod.LOGGER.info("[CustomBlocks] Auto-backup started (interval={} min, keep={}).",
                CustomBlocksConfig.autoBackupInterval, CustomBlocksConfig.autoBackupKeepCount);
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
        int min = CustomBlocksConfig.autoBackupInterval;
        long seconds = (min <= 0) ? DISABLED_RECHECK_SECONDS : (long) min * 60L;
        scheduler.schedule(() -> tick(gen), seconds, TimeUnit.SECONDS);
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
            if (running && CustomBlocksConfig.autoBackupInterval > 0) takeBackup();
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
        String name = BackupManager.timestampName("auto");
        try {
            if (BackupManager.exists(name)) return; // same-second collision — skip silently (never happens at 1+ min)
            BackupManager.save(name, blocks, true);
            int pruned = BackupManager.pruneAuto(CustomBlocksConfig.autoBackupKeepCount);
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
