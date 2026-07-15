/**
 * LockManager.java
 *
 * Responsibility: Track which block IDs are locked against modification.
 * Locked blocks cannot be deleted, renamed, retextured, or have attributes changed
 * until explicitly unlocked. Persists the locked set to config/customblocks/locks.json
 * via atomic write (NFR-13). In-memory cache avoids disk I/O on every command.
 *
 * Depends on: (none — standalone)
 * Called by:  ManagementCommands, CreationCommands, AttributeCommands, TemplateCommands, DeleterItem
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public final class LockManager {

    private static final String FILE = "config/customblocks/locks.json";
    private static final Gson GSON = new Gson();
    private static final Set<String> LOCKED = new HashSet<>();

    static { load(); }

    private LockManager() {}

    /**
     * Canonical lock key. Block ids resolve case-insensitively everywhere (SlotManager.getById,
     * the "G25-family fix"), so the lock set must too — otherwise `/cb lock baldiii` stores
     * "baldiii" while a tool holding the stored id "Baldiii" checks isLocked("Baldiii") and the
     * lock silently bypasses. Normalising to lowercase keeps every lock check case-agnostic.
     */
    private static String key(String id) {
        return id == null ? null : id.toLowerCase(java.util.Locale.ROOT);
    }

    public static synchronized boolean isLocked(String id) {
        return LOCKED.contains(key(id));
    }

    /** Lock a block. Returns false if it was already locked. */
    public static synchronized boolean lock(String id) {
        if (LOCKED.add(key(id))) { save(); return true; }
        return false;
    }

    /** Unlock a block. Returns false if it was not locked. */
    public static synchronized boolean unlock(String id) {
        if (LOCKED.remove(key(id))) { save(); return true; }
        return false;
    }

    /** Move lock state from {@code oldId} to {@code newId} (for /cb reid). No-op if oldId wasn't locked. */
    public static synchronized void renameId(String oldId, String newId) {
        if (LOCKED.remove(key(oldId))) { LOCKED.add(key(newId)); save(); }
    }

    /** All currently locked IDs, sorted alphabetically. */
    public static synchronized List<String> list() {
        List<String> ids = new ArrayList<>(LOCKED);
        Collections.sort(ids);
        return ids;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject o = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null || !o.has("locked")) return;
            for (var e : o.getAsJsonArray("locked")) LOCKED.add(key(e.getAsString())); // normalise legacy mixed-case entries
        } catch (Exception ignored) {}
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject o = new JsonObject();
            JsonArray arr = new JsonArray();
            list().forEach(arr::add);
            o.add("locked", arr);
            Path tmp = file.resolveSibling("locks.json.tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
