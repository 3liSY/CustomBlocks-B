/**
 * BackupArchive.java — Group 09 (split out of BackupManager, 2026-07-23 §9.3 no-monolith rule).
 *
 * The two backup-management ops that rewrite a backup's identity: rename, and protect-from-prune. The
 * portable-ZIP export/import that lived here was for cloud sync (Group 20 §C / TG9 §F) — dropped
 * 2026-07-23 when cloud backup was cut (R2 needs a billing card the owner declined). Backups are
 * local-only now; category/note sharing keeps its own cloud path in CloudVaultClient.
 *
 * Shared plumbing (naming, manifests, entry listing, safe delete) lives in BackupManager.
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksMod;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class BackupArchive {

    private BackupArchive() {} // static-only

    /**
     * Flip a backup's protect-from-prune flag by rewriting its manifest. Protected backups are kept even
     * past the retention counts. Returns false if the backup is missing.
     */
    public static synchronized boolean setProtected(String name, boolean value) {
        if (!BackupManager.exists(name)) return false;
        Path manifest = BackupManager.BACKUPS_DIR.resolve(name).resolve(BackupManager.MANIFEST);
        JsonObject o = null;
        try {
            if (Files.isRegularFile(manifest)) {
                o = BackupManager.GSON.fromJson(Files.readString(manifest, StandardCharsets.UTF_8), JsonObject.class);
            }
        } catch (Exception ignored) { /* rebuild a minimal manifest below */ }
        if (o == null) o = new JsonObject();
        o.addProperty("protected", value);
        try {
            Files.writeString(manifest, BackupManager.GSON.toJson(o), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Failed to set protected on backup \"{}\"", name, e);
            return false;
        }
    }

    /**
     * Rename a backup folder (and its stored manifest name). Throws if {@code newName} is invalid or
     * already taken; returns false only if {@code oldName} doesn't exist. Never touches live data.
     */
    public static synchronized boolean rename(String oldName, String newName) throws IOException {
        if (!BackupManager.exists(oldName)) return false;
        if (!BackupManager.isValidName(newName)) throw new IOException("Invalid backup name: " + newName);
        if (oldName.equals(newName)) return true;
        if (BackupManager.exists(newName)) throw new IOException("A backup named \"" + newName + "\" already exists.");
        Path from = BackupManager.BACKUPS_DIR.resolve(oldName);
        Path to   = BackupManager.BACKUPS_DIR.resolve(newName);
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to);
        }
        Path manifest = to.resolve(BackupManager.MANIFEST);
        if (Files.isRegularFile(manifest)) {
            try {
                JsonObject o = BackupManager.GSON.fromJson(Files.readString(manifest, StandardCharsets.UTF_8), JsonObject.class);
                if (o != null) {
                    o.addProperty("name", newName);
                    Files.writeString(manifest, BackupManager.GSON.toJson(o), StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) { /* folder rename already succeeded; name field is cosmetic */ }
        }
        return true;
    }
}
