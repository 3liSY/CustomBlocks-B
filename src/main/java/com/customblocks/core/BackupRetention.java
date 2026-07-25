/**
 * BackupRetention.java — Group 09 (split out of BackupManager, 2026-07-23 §9.3 no-monolith rule).
 *
 * Bounding how many backups pile up. Retention prunes by MANIFEST kind (not folder name), so a manual
 * save literally named "auto_x" is never swept — the fix for the old prune bug. AUTO backups prune by
 * count and, optionally, by a disk budget over the dedup pool; SAFETY copies (pre-restore/pre-setall/…)
 * prune by count but never below 1, so the latest undo point always survives; MANUAL and pinned backups
 * are never pruned. Deletion + pool GC are BackupManager's package-private helpers.
 */
package com.customblocks.core;

import java.util.ArrayList;
import java.util.List;

public final class BackupRetention {

    private BackupRetention() {} // static-only

    /** Keep the {@code keep} newest AUTO backups, delete the rest (by manifest kind). Returns # removed. */
    public static synchronized int pruneAuto(int keep) {
        return pruneKind(BackupManager.Kind.AUTO, keep);
    }

    /** Keep the {@code keep} newest SAFETY backups (min 1 so the latest undo point survives). # removed. */
    public static synchronized int pruneSafety(int keep) {
        return pruneKind(BackupManager.Kind.SAFETY, Math.max(1, keep));
    }

    /**
     * Trim AUTO backups by disk budget (P4): while the shared pool's on-disk footprint exceeds
     * {@code budgetMB}, delete the OLDEST unprotected auto-backup — but never the last one. Runs after the
     * count-based prune. {@code budgetMB} ≤ 0 disables it. Returns how many auto-backups were removed.
     */
    public static synchronized int pruneAutoToBudgetMB(int budgetMB) {
        if (budgetMB <= 0) return 0;
        long budgetBytes = (long) budgetMB * 1024L * 1024L;
        int removed = 0;
        while (BackupPool.totalSize() > budgetBytes) {
            String oldest = null; // oldest unprotected auto = last in the newest-first list
            int autos = 0;
            for (BackupManager.BackupInfo b : BackupManager.list())
                if (b.kind() == BackupManager.Kind.AUTO && !b.protectedFromPrune()) { autos++; oldest = b.name(); }
            if (autos <= 1 || oldest == null) break; // keep at least one auto
            if (!BackupManager.deleteFolder(oldest)) break;
            BackupManager.gcPool();
            removed++;
        }
        return removed;
    }

    /** Shared newest-first retention for one kind: keep the {@code keep} newest unpinned, delete older. */
    private static synchronized int pruneKind(BackupManager.Kind kind, int keep) {
        int k = Math.max(0, keep);
        List<BackupManager.BackupInfo> matches = new ArrayList<>();
        for (BackupManager.BackupInfo b : BackupManager.list()) { // list() is already newest-first
            if (b.kind() == kind && !b.protectedFromPrune()) matches.add(b);
        }
        int removed = 0;
        for (int i = k; i < matches.size(); i++) {
            if (BackupManager.deleteFolder(matches.get(i).name())) removed++;
        }
        if (removed > 0) BackupManager.gcPool(); // one sweep after the batch, not per delete
        return removed;
    }
}
