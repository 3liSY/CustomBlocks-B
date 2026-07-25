/**
 * CbPaths.java — Group 09 (P1, 2026-07-23): the single source of truth for where CustomBlocks keeps
 * its files on disk. Everything lives under {@link #ROOT} = config/customblocks/. Code that needs a
 * storage location should resolve it from here rather than hardcoding a "config/customblocks/…" literal,
 * so the backup system (which snapshots the WHOLE root) and future path changes stay consistent.
 *
 * BACKUP SCOPE: {@link #backupExcludes()} lists the top-level entries a snapshot must SKIP —
 *   • backups/    — the backup store itself (never back up backups, that recurses/explodes size);
 *   • updates/    — downloaded mod jars (JarUpdater), not mod data;
 *   • the generated resource pack zip(s) — regenerated from slots/textures on demand, huge, pointless to store.
 * A snapshot copies every OTHER top-level entry verbatim (slots.json, config.json, textures/, sources/,
 * data/, trash/, notes.json, locks.json, categories, markers, fonts, arabic files, …), so a restore
 * returns the whole mod state, not just four files (the pre-P1 §G data-loss hole).
 *
 * Depends on: (nothing — constants only). Called by: BackupManager and, over time, the storage classes.
 */
package com.customblocks.core;

import java.nio.file.Path;
import java.util.Set;

public final class CbPaths {

    private CbPaths() {} // constants only

    /** The one root every CustomBlocks file lives under. */
    public static final Path ROOT = Path.of("config", "customblocks");

    /** Runtime data sub-root (category meta, display blocks, overlays, screen prefs, vault codes, …). */
    public static final Path DATA     = ROOT.resolve("data");
    /** Point-in-time backup store (Group 09) — EXCLUDED from snapshots. */
    public static final Path BACKUPS  = ROOT.resolve("backups");
    /** Deleted-block trash store (Group 09 / G06). Included in snapshots. */
    public static final Path TRASH    = ROOT.resolve("trash");
    /** Baked per-slot texture PNGs. */
    public static final Path TEXTURES = ROOT.resolve("textures");
    /** Original source images used to (re)render textures. */
    public static final Path SOURCES  = ROOT.resolve("sources");
    /** Downloaded mod-update jars (JarUpdater) — EXCLUDED from snapshots. */
    public static final Path UPDATES  = ROOT.resolve("updates");

    /** The generated resource-pack zip filename (served by ResourcePackServer) — EXCLUDED from snapshots. */
    public static final String PACK_ZIP        = "customblocks_pack.zip";
    /** A legacy/diagnostics pack zip name also excluded, so an old file can't bloat a backup. */
    public static final String LEGACY_PACK_ZIP = "pack.zip";

    /**
     * Top-level names inside {@link #ROOT} a backup snapshot must NOT copy. Names only (single path
     * segments), matched against {@code root.list()} entries. Anything not listed here is backed up.
     */
    public static Set<String> backupExcludes() {
        return Set.of(
                BACKUPS.getFileName().toString(),  // backups/
                UPDATES.getFileName().toString(),  // updates/
                PACK_ZIP,                          // customblocks_pack.zip
                LEGACY_PACK_ZIP                    // pack.zip
        );
    }

    /** True if {@code name} (a single top-level entry name) should be skipped by a snapshot. Also skips
     *  any *.tmp scratch entry (in-progress backup/store writes). */
    public static boolean isExcludedFromBackup(String name) {
        return name == null || name.endsWith(".tmp") || backupExcludes().contains(name);
    }
}
