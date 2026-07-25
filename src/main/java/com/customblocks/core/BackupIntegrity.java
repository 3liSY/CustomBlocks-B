/**
 * BackupIntegrity.java — Group 09 (split out of BackupManager, 2026-07-23 §9.3 no-monolith rule).
 *
 * Checking that backups are trustworthy (P3): {@link #verify} confirms a backup's files are present and,
 * when {@code deep}, still hash to their recorded checksum (catches bit-rot). The boot-guard trio detects
 * a live-data catastrophe — slots.json missing/corrupt while good backups exist — so callers can warn
 * loudly and auto-backup can refuse to snapshot the empty state over that history. Never touches live data.
 */
package com.customblocks.core;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BackupIntegrity {

    private BackupIntegrity() {} // static-only

    /** Result of an integrity check: {@code ok} plus how many files were checked and any problems found. */
    public record VerifyResult(String name, boolean ok, int checked, List<String> problems) {}

    /**
     * Integrity-check a backup. For a pooled (v3) backup: every recorded file's blob must exist and, when
     * {@code deep}, its content must still hash to the recorded sha. For older formats: slots.json must be
     * present and parse. Deep checks re-read every blob, so they're for one backup on demand.
     */
    public static synchronized VerifyResult verify(String name, boolean deep) {
        List<String> problems = new ArrayList<>();
        if (!BackupManager.exists(name)) return new VerifyResult(name, false, 0, List.of("Backup folder is missing."));
        Path dir = BackupManager.BACKUPS_DIR.resolve(name);
        JsonObject m = BackupManager.readManifest(dir);
        if (m == null) return new VerifyResult(name, false, 0, List.of("manifest.json is missing or unreadable."));
        int checked = 0;
        if (BackupManager.formatOf(m) >= BackupManager.FORMAT_POOLED) {
            List<BackupManager.FileRef> refs = BackupManager.fileRefs(m);
            if (refs.isEmpty()) problems.add("Manifest lists no files.");
            boolean sawSlots = false;
            for (BackupManager.FileRef fr : refs) {
                if (fr.path().equals("slots.json")) sawSlots = true;
                Path blob = BackupPool.blob(fr.sha());
                if (!Files.isRegularFile(blob)) { problems.add("Missing pooled data for " + fr.path()); continue; }
                if (deep) {
                    try {
                        if (!BackupPool.sha256(blob).equals(fr.sha())) problems.add("Corrupt (hash mismatch) for " + fr.path());
                    } catch (IOException e) { problems.add("Unreadable pooled data for " + fr.path()); }
                }
                checked++;
            }
            if (!sawSlots) problems.add("No slots.json in the backup.");
        } else {
            Path slots = dir.resolve("slots.json");
            if (!Files.isRegularFile(slots)) problems.add("No slots.json in the backup.");
            else {
                try { BackupManager.GSON.fromJson(Files.readString(slots, StandardCharsets.UTF_8), JsonObject.class); checked++; }
                catch (Exception e) { problems.add("slots.json does not parse."); }
            }
        }
        return new VerifyResult(name, problems.isEmpty(), checked, problems);
    }

    /** True if the live slots.json exists and parses — the primary "data is present" signal. */
    public static boolean liveSlotsPresentAndValid() {
        Path slots = BackupManager.LIVE_DIR.resolve("slots.json");
        if (!Files.isRegularFile(slots)) return false;
        try {
            BackupManager.GSON.fromJson(Files.readString(slots, StandardCharsets.UTF_8), JsonObject.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** True if at least one restorable backup exists on disk (used by the boot guard). */
    public static boolean hasAnyValidBackup() {
        for (BackupManager.BackupInfo b : BackupManager.list()) if (BackupManager.isValidBackup(b.name())) return true;
        return false;
    }

    /**
     * The boot guard: live slots.json is missing/corrupt WHILE restorable backups exist — i.e. the data
     * vanished but recovery is possible. Callers warn loudly and must NOT let auto-backup snapshot the
     * empty state over that good history.
     */
    public static boolean dataLossSuspected() {
        return !liveSlotsPresentAndValid() && hasAnyValidBackup();
    }
}
