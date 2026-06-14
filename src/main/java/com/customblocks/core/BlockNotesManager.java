/**
 * BlockNotesManager.java
 *
 * Responsibility: Store and retrieve free-text notes attached to block IDs.
 * Notes are server-wide (not per-player) — they're annotations on the block definition
 * itself, visible to all. Persists to config/customblocks/notes.json (atomic write, NFR-13).
 *
 * Depends on: (none — standalone)
 * Called by:  ManagementCommands
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public final class BlockNotesManager {

    private static final String FILE = "config/customblocks/notes.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, String> NOTES = new LinkedHashMap<>();

    static { load(); }

    private BlockNotesManager() {}

    /** Returns the note text, or null if no note is set for this block. */
    public static synchronized String getNote(String id) {
        return NOTES.get(id);
    }

    public static synchronized boolean hasNote(String id) {
        return NOTES.containsKey(id);
    }

    /** Set or overwrite a note. */
    public static synchronized void setNote(String id, String text) {
        NOTES.put(id, text);
        save();
    }

    /** Remove a note. Returns true if there was a note to remove. */
    public static synchronized boolean clearNote(String id) {
        if (NOTES.remove(id) != null) { save(); return true; }
        return false;
    }

    /** Remove the note for a block when the block itself is deleted (clean-up). */
    public static synchronized void onBlockDeleted(String id) {
        if (NOTES.remove(id) != null) save();
    }

    /** Move a note from {@code oldId} to {@code newId} (for /cb reid). No-op if oldId had no note. */
    public static synchronized void renameId(String oldId, String newId) {
        String note = NOTES.remove(oldId);
        if (note != null) { NOTES.put(newId, note); save(); }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject o = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null) return;
            for (var entry : o.entrySet()) NOTES.put(entry.getKey(), entry.getValue().getAsString());
        } catch (Exception ignored) {}
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject o = new JsonObject();
            NOTES.forEach(o::addProperty);
            Path tmp = file.resolveSibling("notes.json.tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
