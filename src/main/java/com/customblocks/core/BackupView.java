/**
 * BackupView.java — Group 09 (split out of BackupManager, 2026-07-23 §9.3 no-monolith rule).
 *
 * Presentation helpers for the backup list — the friendly wall-clock time, the primary display label
 * (kind-aware), a human byte size, and the JSON payload the Backup Screen renders. No disk mutation:
 * this is the read/format layer over {@link BackupManager#list()}.
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class BackupView {

    private BackupView() {} // static-only

    /** Friendly wall-clock label for lists, e.g. "Jul 12, 2:30 PM". */
    private static final DateTimeFormatter FRIENDLY = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.ENGLISH);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Friendly wall-clock time for a backup, e.g. "Jul 12, 2:30 PM". Derived from the creation epoch;
     * falls back to the stored human string (or "?") when the epoch is unknown.
     */
    public static String friendlyTime(BackupManager.BackupInfo b) {
        if (b.createdEpochMs() > 0L) {
            // Owner's zone, not the host's — the host may run UTC, which showed list times hours off.
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(b.createdEpochMs()), CustomBlocksConfig.OWNER_ZONE)
                    .format(FRIENDLY);
        }
        return b.created().isEmpty() ? "?" : b.created();
    }

    /**
     * Primary display label for lists: a timed auto/safety backup shows "auto · Jul 12, 2:30 PM" /
     * "safety · …" instead of its raw folder name; a manual save shows its own name. The raw name is
     * still the restore-by id and should be shown alongside (dim) so it stays copy-pasteable.
     */
    public static String displayLabel(BackupManager.BackupInfo b) {
        return switch (b.kind()) {
            case AUTO   -> "auto · " + friendlyTime(b);
            case SAFETY -> "safety · " + friendlyTime(b);
            case MANUAL -> b.name();
        };
    }

    /** Human-readable byte size, e.g. "12.3 KB". "?" when unknown (negative). */
    public static String humanSize(long bytes) {
        if (bytes < 0) return "?";
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.ENGLISH, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.ENGLISH, "%.1f MB", mb);
        return String.format(Locale.ENGLISH, "%.1f GB", mb / 1024.0);
    }

    /**
     * The backup list serialized for the Backup Screen (G09-A4). One object per backup, newest first:
     * {@code name} (raw restore-by id), {@code label} (friendly primary), {@code when}, {@code blocks},
     * {@code size} (human string), {@code kind} (manual/auto/safety → tab + icon), {@code auto} (kept for
     * older screen builds), {@code reason}, {@code note}, {@code prot}.
     */
    public static String screenJson() {
        JsonArray arr = new JsonArray();
        for (BackupManager.BackupInfo b : BackupManager.list()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", b.name());
            o.addProperty("label", displayLabel(b));
            o.addProperty("when", friendlyTime(b));
            o.addProperty("blocks", b.blocks());
            o.addProperty("size", humanSize(b.sizeBytes()));
            o.addProperty("kind", b.kind().id());
            o.addProperty("auto", b.auto());
            o.addProperty("reason", b.reason());
            o.addProperty("note", b.note());
            o.addProperty("prot", b.protectedFromPrune());
            arr.add(o);
        }
        JsonObject root = new JsonObject();
        root.add("backups", arr);
        return GSON.toJson(root);
    }
}
