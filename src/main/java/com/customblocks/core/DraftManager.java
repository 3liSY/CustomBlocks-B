/**
 * DraftManager.java
 *
 * Responsibility: Track which block IDs are marked as drafts (work-in-progress, not yet
 * "published"). Draft blocks are fully functional — the draft flag is organizational only.
 * Publishing removes the flag. Persists to config/customblocks/drafts.json (atomic, NFR-13).
 *
 * Depends on: (none — standalone)
 * Called by:  ManagementCommands, UtilityCommands (for status tags)
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

public final class DraftManager {

    private static final String FILE = "config/customblocks/drafts.json";
    private static final Gson GSON = new Gson();
    private static final Set<String> DRAFTS = new HashSet<>();

    static { load(); }

    private DraftManager() {}

    public static synchronized boolean isDraft(String id) {
        return DRAFTS.contains(id);
    }

    /** Mark a block as a draft. Returns false if it was already marked. */
    public static synchronized boolean markDraft(String id) {
        if (DRAFTS.add(id)) { save(); return true; }
        return false;
    }

    /** Remove the draft mark. Returns false if it was not a draft. */
    public static synchronized boolean publish(String id) {
        if (DRAFTS.remove(id)) { save(); return true; }
        return false;
    }

    /** Move draft state from {@code oldId} to {@code newId} (for /cb reid). No-op if oldId wasn't a draft. */
    public static synchronized void renameId(String oldId, String newId) {
        if (DRAFTS.remove(oldId)) { DRAFTS.add(newId); save(); }
    }

    /** All current draft IDs, sorted alphabetically. */
    public static synchronized List<String> list() {
        List<String> ids = new ArrayList<>(DRAFTS);
        Collections.sort(ids);
        return ids;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject o = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null || !o.has("drafts")) return;
            for (var e : o.getAsJsonArray("drafts")) DRAFTS.add(e.getAsString());
        } catch (Exception ignored) {}
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject o = new JsonObject();
            JsonArray arr = new JsonArray();
            list().forEach(arr::add);
            o.add("drafts", arr);
            Path tmp = file.resolveSibling("drafts.json.tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
