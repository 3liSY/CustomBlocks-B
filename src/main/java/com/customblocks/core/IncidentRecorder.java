/**
 * IncidentRecorder.java
 *
 * Responsibility: Append critical runtime incidents to config/customblocks/incidents.json
 * for post-mortem analysis. Keeps the last MAX_INCIDENTS entries; older ones are pruned.
 * Atomic write (temp+move) so a mid-save crash cannot corrupt the file.
 *
 * Depends on: Gson, standard Java
 * Called by: any catch block via IncidentRecorder.record(context, throwable)
 */
package com.customblocks.core;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class IncidentRecorder {

    private static final Logger LOG = LoggerFactory.getLogger("CustomBlocks/Incidents");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR  = Path.of("config/customblocks");
    private static final Path FILE = DIR.resolve("incidents.json");
    private static final int MAX_INCIDENTS = 100;

    private IncidentRecorder() {}

    /** Record an incident with context + optional throwable. Thread-safe. */
    public static synchronized void record(String context, Throwable ex) {
        try {
            JsonArray arr = loadArray();
            JsonObject entry = new JsonObject();
            entry.addProperty("time",    Instant.now().toString());
            entry.addProperty("context", context);
            if (ex != null) {
                entry.addProperty("error", ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
            arr.add(entry);
            while (arr.size() > MAX_INCIDENTS) arr.remove(0);
            JsonObject root = new JsonObject();
            root.add("incidents", arr);
            Files.createDirectories(DIR);
            Path tmp = DIR.resolve("incidents.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOG.error("[CustomBlocks] Failed to write incident", e);
        }
    }

    /** Convenience: incident without a throwable. */
    public static void record(String context) { record(context, null); }

    /** Return all incidents as formatted display lines (newest first). */
    public static synchronized List<String> list() {
        try {
            if (!Files.exists(FILE)) return List.of("§7No incidents recorded.");
            JsonArray arr = loadArray();
            List<String> lines = new ArrayList<>();
            for (int i = arr.size() - 1; i >= 0; i--) {
                JsonObject e = arr.get(i).getAsJsonObject();
                String time = e.has("time") ? e.get("time").getAsString().substring(0, 19) : "?";
                String ctx  = e.has("context") ? e.get("context").getAsString() : "?";
                String err  = e.has("error")   ? " §c" + e.get("error").getAsString() : "";
                lines.add("§7[" + time + "] §f" + ctx + err);
            }
            return lines.isEmpty() ? List.of("§7No incidents recorded.") : lines;
        } catch (Exception e) {
            return List.of("§cFailed to read incidents: " + e.getMessage());
        }
    }

    /** Clear all recorded incidents. */
    public static synchronized void clear() {
        try { Files.deleteIfExists(FILE); } catch (Exception ignored) {}
    }

    private static JsonArray loadArray() {
        try {
            if (!Files.exists(FILE)) return new JsonArray();
            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return root.has("incidents") ? root.getAsJsonArray("incidents") : new JsonArray();
        } catch (Exception e) {
            return new JsonArray();
        }
    }
}
