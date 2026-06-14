/**
 * BlockToleranceStore.java — Group 10 (Coloring), per-block background-removal tolerance overrides.
 *
 * Responsibility: remember a background-removal strength (0-100) for individual blocks, separate
 * from the server-wide default (CustomBlocksConfig.backgroundTolerance). A block with no entry here
 * simply uses the global default. Set via "/cb tolerance <value> <id>" or by Apply in the Background
 * Studio. Persists to config/customblocks/block_tolerances.json via atomic write (NFR-13); the
 * in-memory cache avoids disk I/O on every lookup. Mirrors the LockManager pattern.
 *
 * Depends on: (none — standalone, Gson is bundled with Minecraft).
 * Called by:  command/handlers/ImageToolCommands, gui/chest/{BgStudioSession, BgStudioMenu}, ReIdCommands.
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public final class BlockToleranceStore {

    private static final String FILE = "config/customblocks/block_tolerances.json";
    private static final Gson GSON = new Gson();
    private static final Map<String, Integer> OVERRIDES = new HashMap<>();

    static { load(); }

    private BlockToleranceStore() {} // static-only

    /** The block's tolerance override (0-100), or null if it uses the global default. */
    public static synchronized Integer get(String id) {
        return OVERRIDES.get(id);
    }

    public static synchronized boolean has(String id) {
        return OVERRIDES.containsKey(id);
    }

    /**
     * The tolerance to use for {@code id}: its override if set, else {@code globalDefault}.
     * Convenience so callers don't repeat the null check.
     */
    public static synchronized int effective(String id, int globalDefault) {
        Integer v = OVERRIDES.get(id);
        return v != null ? v : globalDefault;
    }

    /** Set (or replace) a block's tolerance override, clamped to 0-100. */
    public static synchronized void set(String id, int value) {
        OVERRIDES.put(id, Math.max(0, Math.min(100, value)));
        save();
    }

    /** Remove a block's override so it falls back to the global default. Returns false if none. */
    public static synchronized boolean clear(String id) {
        if (OVERRIDES.remove(id) != null) { save(); return true; }
        return false;
    }

    /** Move an override from {@code oldId} to {@code newId} (for /cb reid). No-op if none. */
    public static synchronized void renameId(String oldId, String newId) {
        Integer v = OVERRIDES.remove(oldId);
        if (v != null) { OVERRIDES.put(newId, v); save(); }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject o = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null) return;
            for (var e : o.entrySet()) {
                try { OVERRIDES.put(e.getKey(), e.getValue().getAsInt()); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject o = new JsonObject();
            for (var e : OVERRIDES.entrySet()) o.addProperty(e.getKey(), e.getValue());
            Path tmp = file.resolveSibling("block_tolerances.json.tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
