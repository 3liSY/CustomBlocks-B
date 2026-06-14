/**
 * PlayerPaletteManager.java — Group 10 (per-player colour palettes).
 *
 * Responsibility: each player has a "working set" of colours they are collecting (from the colour
 * GUIs, the screen eyedrop, or /cb palette add) plus any number of named, saved palettes. Saving
 * snapshots the working set under a name; loading copies a saved palette back into the working set.
 * Persists to config/customblocks/palettes.json (atomic write, NFR-13): UUID → { working:[...],
 * saved:{ name:[...] } }. All colours are stored canonical "#RRGGBB".
 *
 * Depends on: Gson (bundled with Minecraft).
 * Called by:  command/handlers/PaletteCommands, gui/chest/PaletteMenu, the eyedrop result path.
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerPaletteManager {

    private PlayerPaletteManager() {} // static-only

    private static final String FILE = "config/customblocks/palettes.json";
    private static final Gson GSON = new Gson();
    /** Cap the working set so it fits the GUI and never grows without bound. */
    public static final int WORKING_MAX = 18;

    private static final Map<String, LinkedHashSet<String>> WORKING = new LinkedHashMap<>();
    private static final Map<String, LinkedHashMap<String, List<String>>> SAVED = new LinkedHashMap<>();

    static { load(); }

    // ── Working set ─────────────────────────────────────────────────────────────

    /** Add a colour to the working set (deduped, capped). Returns false if already present/full. */
    public static synchronized boolean workingAdd(UUID player, String hex) {
        LinkedHashSet<String> set = WORKING.computeIfAbsent(player.toString(), k -> new LinkedHashSet<>());
        if (set.contains(hex)) return false;
        if (set.size() >= WORKING_MAX) return false;
        set.add(hex);
        save();
        return true;
    }

    public static synchronized boolean workingRemove(UUID player, String hex) {
        LinkedHashSet<String> set = WORKING.get(player.toString());
        if (set != null && set.remove(hex)) { save(); return true; }
        return false;
    }

    public static synchronized void workingClear(UUID player) {
        if (WORKING.remove(player.toString()) != null) save();
    }

    public static synchronized List<String> working(UUID player) {
        LinkedHashSet<String> set = WORKING.get(player.toString());
        return set == null ? new ArrayList<>() : new ArrayList<>(set);
    }

    // ── Named saved palettes ──────────────────────────────────────────────────────

    /** Snapshot the current working set under {@code name}. Returns false if the working set is empty. */
    public static synchronized boolean save(UUID player, String name) {
        List<String> work = working(player);
        if (work.isEmpty()) return false;
        SAVED.computeIfAbsent(player.toString(), k -> new LinkedHashMap<>()).put(name, work);
        save();
        return true;
    }

    /** Copy saved palette {@code name} into the working set. Returns false if it doesn't exist. */
    public static synchronized boolean load(UUID player, String name) {
        Map<String, List<String>> map = SAVED.get(player.toString());
        if (map == null || !map.containsKey(name)) return false;
        WORKING.put(player.toString(), new LinkedHashSet<>(map.get(name)));
        save();
        return true;
    }

    public static synchronized boolean delete(UUID player, String name) {
        Map<String, List<String>> map = SAVED.get(player.toString());
        if (map != null && map.remove(name) != null) {
            if (map.isEmpty()) SAVED.remove(player.toString());
            save();
            return true;
        }
        return false;
    }

    public static synchronized List<String> names(UUID player) {
        Map<String, List<String>> map = SAVED.get(player.toString());
        return map == null ? new ArrayList<>() : new ArrayList<>(map.keySet());
    }

    public static synchronized List<String> get(UUID player, String name) {
        Map<String, List<String>> map = SAVED.get(player.toString());
        return map == null ? null : map.get(name);
    }

    // ─────────────────────────────────────────────────────────────────────────────

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject root = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) return;
            for (var entry : root.entrySet()) {
                JsonObject o = entry.getValue().getAsJsonObject();
                if (o.has("working")) {
                    LinkedHashSet<String> work = new LinkedHashSet<>();
                    for (var el : o.getAsJsonArray("working")) work.add(el.getAsString());
                    WORKING.put(entry.getKey(), work);
                }
                if (o.has("saved")) {
                    LinkedHashMap<String, List<String>> map = new LinkedHashMap<>();
                    for (var s : o.getAsJsonObject("saved").entrySet()) {
                        List<String> cols = new ArrayList<>();
                        for (var el : s.getValue().getAsJsonArray()) cols.add(el.getAsString());
                        map.put(s.getKey(), cols);
                    }
                    SAVED.put(entry.getKey(), map);
                }
            }
        } catch (Exception ignored) {}
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            java.util.Set<String> keys = new LinkedHashSet<>();
            keys.addAll(WORKING.keySet());
            keys.addAll(SAVED.keySet());
            for (String uuid : keys) {
                JsonObject o = new JsonObject();
                LinkedHashSet<String> work = WORKING.get(uuid);
                if (work != null && !work.isEmpty()) {
                    JsonArray arr = new JsonArray();
                    work.forEach(arr::add);
                    o.add("working", arr);
                }
                Map<String, List<String>> map = SAVED.get(uuid);
                if (map != null && !map.isEmpty()) {
                    JsonObject saved = new JsonObject();
                    for (var e : map.entrySet()) {
                        JsonArray arr = new JsonArray();
                        e.getValue().forEach(arr::add);
                        saved.add(e.getKey(), arr);
                    }
                    o.add("saved", saved);
                }
                root.add(uuid, o);
            }
            Path tmp = file.resolveSibling("palettes.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
