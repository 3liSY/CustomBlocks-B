/**
 * FavoritesManager.java
 *
 * Responsibility: Per-player block bookmarks. Each player has their own set of
 * favorited block IDs. Persists to config/customblocks/favorites.json as a map
 * of UUID string → array of block IDs (atomic write, NFR-13).
 *
 * Depends on: (none — standalone)
 * Called by:  ManagementCommands
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

public final class FavoritesManager {

    private static final String FILE = "config/customblocks/favorites.json";
    private static final Gson GSON = new Gson();
    // UUID string → ordered set of block IDs
    private static final Map<String, LinkedHashSet<String>> FAVS = new LinkedHashMap<>();

    static { load(); }

    private FavoritesManager() {}

    public static synchronized boolean isFavorite(UUID player, String id) {
        Set<String> set = FAVS.get(player.toString());
        return set != null && set.contains(id);
    }

    /** Add a block to the player's favorites. Returns false if already present. */
    public static synchronized boolean add(UUID player, String id) {
        if (FAVS.computeIfAbsent(player.toString(), k -> new LinkedHashSet<>()).add(id)) {
            save();
            return true;
        }
        return false;
    }

    /** Remove a block from the player's favorites. Returns false if it wasn't there. */
    public static synchronized boolean remove(UUID player, String id) {
        Set<String> set = FAVS.get(player.toString());
        if (set != null && set.remove(id)) {
            if (set.isEmpty()) FAVS.remove(player.toString());
            save();
            return true;
        }
        return false;
    }

    /** All favorites for a player, in insertion order. */
    public static synchronized List<String> list(UUID player) {
        Set<String> set = FAVS.get(player.toString());
        return set == null ? Collections.emptyList() : new ArrayList<>(set);
    }

    /**
     * Move a block id from {@code oldId} to {@code newId} in EVERY player's favorites (for /cb reid).
     * Order is preserved (the id is swapped in place). No-op for players who hadn't favorited it.
     */
    public static synchronized void renameId(String oldId, String newId) {
        boolean changed = false;
        for (var e : FAVS.entrySet()) {
            LinkedHashSet<String> set = e.getValue();
            if (set.contains(oldId)) {
                LinkedHashSet<String> rebuilt = new LinkedHashSet<>();
                for (String id : set) rebuilt.add(id.equals(oldId) ? newId : id);
                e.setValue(rebuilt);
                changed = true;
            }
        }
        if (changed) save();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject o = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null) return;
            for (var entry : o.entrySet()) {
                LinkedHashSet<String> ids = new LinkedHashSet<>();
                for (var el : entry.getValue().getAsJsonArray()) ids.add(el.getAsString());
                FAVS.put(entry.getKey(), ids);
            }
        } catch (Exception ignored) {}
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject o = new JsonObject();
            for (var entry : FAVS.entrySet()) {
                JsonArray arr = new JsonArray();
                entry.getValue().forEach(arr::add);
                o.add(entry.getKey(), arr);
            }
            Path tmp = file.resolveSibling("favorites.json.tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
