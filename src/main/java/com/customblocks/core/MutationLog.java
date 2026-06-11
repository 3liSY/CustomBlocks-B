/**
 * MutationLog.java
 *
 * Append-only audit log of block mutations (Group 02, G02.8). Records are pushed from a
 * single hook inside UndoManager.push(), so every create/modify/delete that the commands
 * record for undo is also captured here with the acting player's UUID, the action label,
 * the affected block id and a timestamp. Persisted to config/customblocks/data/history.json.
 * The actor's display name is resolved at render time (see HistoryMenu).
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MutationLog {

    private MutationLog() {} // static-only

    /** One recorded mutation. actor is a UUID string, or "console". */
    public record Entry(long time, String actor, String action, String blockId) {}

    private static final int CAP = 500;
    private static final Path FILE = Path.of("config/customblocks/data", "history.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();
    private static volatile boolean loaded = false;

    /** Record a mutation (newest entries first). Called from UndoManager.push(). */
    public static synchronized void record(UUID actor, String action, String blockId) {
        ensureLoaded();
        Entry e = new Entry(System.currentTimeMillis(),
                actor == null ? "console" : actor.toString(),
                action == null ? "edit" : action,
                blockId == null ? "?" : blockId);
        ENTRIES.add(0, e);
        while (ENTRIES.size() > CAP) ENTRIES.remove(ENTRIES.size() - 1);
        save();
    }

    /** Newest-first snapshot of all recorded mutations. */
    public static synchronized List<Entry> recent() {
        ensureLoaded();
        return new ArrayList<>(ENTRIES);
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            if (Files.exists(FILE)) {
                String json = Files.readString(FILE, StandardCharsets.UTF_8);
                JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
                for (var el : arr) {
                    JsonObject o = el.getAsJsonObject();
                    ENTRIES.add(new Entry(
                            o.has("time") ? o.get("time").getAsLong() : 0L,
                            o.has("actor") ? o.get("actor").getAsString() : "console",
                            o.has("action") ? o.get("action").getAsString() : "edit",
                            o.has("blockId") ? o.get("blockId").getAsString() : "?"));
                }
            }
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Could not load history.json: {}", e.toString());
        }
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            JsonArray arr = new JsonArray();
            for (Entry e : ENTRIES) {
                JsonObject o = new JsonObject();
                o.addProperty("time", e.time());
                o.addProperty("actor", e.actor());
                o.addProperty("action", e.action());
                o.addProperty("blockId", e.blockId());
                arr.add(o);
            }
            Files.writeString(FILE, GSON.toJson(arr), StandardCharsets.UTF_8);
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Could not save history.json: {}", e.toString());
        }
    }
}
