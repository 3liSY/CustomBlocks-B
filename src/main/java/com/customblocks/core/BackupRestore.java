/**
 * BackupRestore.java — Group 09 (split out of BackupManager, 2026-07-23 §9.3 no-monolith rule).
 *
 * The restore side of the backup system: replacing live data from a saved backup (whole snapshot or a
 * single store), plus the read-only preview that shows what a restore WOULD change. Reconstruction is
 * format-aware — pooled (v3) backups rehydrate each file from {@link BackupPool}; older (v1/v2) backups
 * copy their real folder files. Every restore first moves the live state aside into a fresh SAFETY
 * backup (its own undo point) and rolls that back on any failure, so a restore can never lose data.
 *
 * All the shared plumbing (naming, manifests, live/backup entry listing, safe move/copy) lives in
 * BackupManager as package-private helpers this class calls. Run on the server thread.
 */
package com.customblocks.core;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class BackupRestore {

    private BackupRestore() {} // static-only

    /**
     * Restore live data from backups/&lt;name&gt;/ with a SAFE SWAP:
     *   1. verify the chosen backup parses (else abort, live untouched);
     *   2. MOVE the current live files into a fresh SAFETY backup — a fast rename that both clears the
     *      live state AND leaves the old state as a recoverable snapshot;
     *   3. reconstruct the chosen backup into the now-cleared live tree.
     * Returns the safety backup's name. On a copy failure it best-effort rolls the safety copy back into
     * place, then rethrows. The CALLER must pause the pack first and, afterwards, reload config +
     * SlotManager and rebuild the pack. Run on the server thread (no concurrent edits).
     */
    public static synchronized String restore(String name, int currentBlocks) throws IOException {
        if (!BackupManager.isValidBackup(name)) throw new IOException("Backup \"" + name + "\" is missing or unreadable.");
        Path backupDir = BackupManager.BACKUPS_DIR.resolve(name);

        String safety = BackupManager.generatedName(BackupManager.Kind.SAFETY);
        Path safetyDir = BackupManager.BACKUPS_DIR.resolve(safety);
        Files.createDirectories(safetyDir);
        List<String> movedAside = BackupManager.liveEntries();
        for (String entry : movedAside) BackupManager.moveIfExists(BackupManager.LIVE_DIR.resolve(entry), safetyDir.resolve(entry));
        // The safety copy is real move-aside files, so it is a FORMAT-2 backup (not pooled) — fast, and
        // the most critical undo point stays a plain folder that needs no pool to restore.
        BackupManager.writeLegacyManifest(safetyDir, safety, currentBlocks, BackupManager.Kind.SAFETY, "pre-restore", movedAside);

        try {
            restoreInto(backupDir, BackupManager.LIVE_DIR, null); // null = whole snapshot
        } catch (IOException e) {
            rollback(safetyDir); // put the old state back so a failed restore can't lose data
            throw e;
        }
        return safety;
    }

    /**
     * Granular restore (P5): restore ONE top-level entry (e.g. "notes.json", "textures", "data") from a
     * backup, leaving everything else live untouched. A SAFETY copy of just that live entry is taken
     * first (so the partial restore is itself undoable). Returns that safety backup's name.
     */
    public static synchronized String restoreEntry(String name, String entry, int currentBlocks) throws IOException {
        if (!BackupManager.isValidBackup(name)) throw new IOException("Backup \"" + name + "\" is missing or unreadable.");
        if (CbPaths.isExcludedFromBackup(entry)) throw new IOException("\"" + entry + "\" is not a restorable entry.");
        if (!contents(name).contains(entry)) throw new IOException("Backup \"" + name + "\" has no \"" + entry + "\".");
        Path backupDir = BackupManager.BACKUPS_DIR.resolve(name);

        String safety = BackupManager.generatedName(BackupManager.Kind.SAFETY);
        Path safetyDir = BackupManager.BACKUPS_DIR.resolve(safety);
        Files.createDirectories(safetyDir);
        Path liveEntry = BackupManager.LIVE_DIR.resolve(entry);
        boolean hadLive = Files.exists(liveEntry);
        if (hadLive) BackupManager.moveIfExists(liveEntry, safetyDir.resolve(entry));
        BackupManager.writeLegacyManifest(safetyDir, safety, currentBlocks, BackupManager.Kind.SAFETY,
                "pre-restore-entry:" + entry, hadLive ? List.of(entry) : List.of());

        try {
            restoreInto(backupDir, BackupManager.LIVE_DIR, entry);
        } catch (IOException e) {
            BackupManager.deleteRecursively(liveEntry); // roll the single entry back
            if (hadLive) BackupManager.moveIfExists(safetyDir.resolve(entry), liveEntry);
            throw e;
        }
        return safety;
    }

    /** The top-level entries a backup contains (read-only browse, P5). Empty if missing/unreadable. */
    public static synchronized List<String> contents(String name) {
        if (!BackupManager.exists(name)) return List.of();
        try { return BackupManager.topLevelEntries(BackupManager.BACKUPS_DIR.resolve(name)); }
        catch (IOException e) { return List.of(); }
    }

    /**
     * Describe what a restore of {@code name} WOULD do, without changing anything (dry-run / preview).
     * Read-only. Returns null if the backup is missing/unreadable.
     */
    public static synchronized RestorePlan restorePlan(String name) {
        if (!BackupManager.isValidBackup(name)) return null;
        Path backupDir = BackupManager.BACKUPS_DIR.resolve(name);
        List<String> live;
        List<String> inBackup;
        try {
            live = BackupManager.liveEntries();
            inBackup = BackupManager.topLevelEntries(backupDir);
        } catch (IOException e) {
            return null;
        }
        List<String> added = new ArrayList<>();     // in backup, not live now → appears after restore
        List<String> replaced = new ArrayList<>();  // in both → live copy overwritten by the backup's
        for (String e : inBackup) (live.contains(e) ? replaced : added).add(e);
        List<String> removed = new ArrayList<>();   // live now, not in backup → moved aside, gone after
        for (String e : live) if (!inBackup.contains(e)) removed.add(e);
        added.sort(String::compareTo); replaced.sort(String::compareTo); removed.sort(String::compareTo);
        return new RestorePlan(name, added, replaced, removed);
    }

    /** A read-only preview of a restore's effect on the live tree (see {@link #restorePlan}). */
    public record RestorePlan(String backup, List<String> added, List<String> replaced, List<String> removed) {}

    /** Reconstruct a backup's data into {@code destRoot} (live during restore). Format-aware. When
     *  {@code onlyEntry} is non-null, restore ONLY that top-level entry (granular restore, P5). */
    private static void restoreInto(Path backupDir, Path destRoot, String onlyEntry) throws IOException {
        JsonObject m = BackupManager.readManifest(backupDir);
        if (m != null && BackupManager.formatOf(m) >= BackupManager.FORMAT_POOLED) {
            for (BackupManager.FileRef fr : BackupManager.fileRefs(m)) {
                if (onlyEntry != null && !topOf(fr.path()).equals(onlyEntry)) continue;
                BackupPool.rehydrate(fr.sha(), destRoot.resolve(fr.path()));
            }
            return;
        }
        // Legacy v1/v2: real files sit in the backup folder next to the manifest.
        for (String entry : BackupManager.backupDataEntries(backupDir)) {
            if (onlyEntry != null && !entry.equals(onlyEntry)) continue;
            Path src = backupDir.resolve(entry);
            if (Files.isDirectory(src))       BackupManager.copyDir(src, destRoot.resolve(entry));
            else if (Files.isRegularFile(src)) Files.copy(src, destRoot.resolve(entry),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    /** First path segment of a '/'-joined relative path. */
    private static String topOf(String path) {
        int slash = path.indexOf('/');
        return slash < 0 ? path : path.substring(0, slash);
    }

    /**
     * Best-effort: after a failed restore, delete any partially-copied live entries and move the safety
     * copy's items back into the live location, so the old state is whole again. Excluded entries were
     * never touched, so they need no rollback.
     */
    private static void rollback(Path safetyDir) {
        try {
            for (String entry : BackupManager.liveEntries()) BackupManager.deleteRecursively(BackupManager.LIVE_DIR.resolve(entry));
        } catch (IOException ignored) {}
        try {
            for (String entry : BackupManager.backupDataEntries(safetyDir)) {
                Path s = safetyDir.resolve(entry);
                BackupManager.deleteRecursively(BackupManager.LIVE_DIR.resolve(entry));
                BackupManager.moveIfExists(s, BackupManager.LIVE_DIR.resolve(entry));
            }
        } catch (IOException ignored) { /* live may be partial; the safety backup folder still holds it */ }
    }
}
