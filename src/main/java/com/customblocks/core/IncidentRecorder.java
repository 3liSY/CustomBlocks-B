/**
 * IncidentRecorder.java
 *
 * Responsibility: Append critical runtime incidents to config/customblocks/incidents.json
 * for post-mortem analysis and the IT Chest dashboard (Group 16). Each entry carries a
 * timestamp, context, optional error, the affected block id and the acting player, plus an
 * auto-derived severity (error / warn / info). Keeps the last MAX_INCIDENTS entries; older
 * ones are pruned. Atomic write (temp+move) so a mid-save crash cannot corrupt the file.
 * Old entries that predate the structured schema degrade gracefully (missing fields → "—").
 *
 * Depends on: Gson, standard Java
 * Called by: any catch block via IncidentRecorder.record(...)
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

    /** Auto-derived from the error/context — never passed by a call site. */
    public enum Severity { ERROR, WARN, INFO }

    /**
     * One structured incident, newest-first when returned from {@link #recent()}.
     * {@code url} is the source URL when the incident is a re-downloadable texture failure
     * (slice 2 auto-fix), else null.
     */
    public record Incident(String time, String context, String error,
                           String block, String player, Severity severity, String url) {}

    private IncidentRecorder() {}

    /**
     * Record an incident. blockId / actor / url may be null (→ "—" / "System" / no auto-fix).
     * Severity is derived from the throwable + context, never passed in. Thread-safe.
     */
    public static synchronized void record(String context, String blockId, String actor,
                                           String url, Throwable ex) {
        try {
            String error = ex == null ? null : ex.getClass().getSimpleName() + ": " + ex.getMessage();
            Severity sev = deriveSeverity(context, error);
            JsonArray arr = loadArray();
            JsonObject entry = new JsonObject();
            entry.addProperty("time",    Instant.now().toString());
            entry.addProperty("context", context);
            if (error != null)               entry.addProperty("error",    error);
            if (blockId != null && !blockId.isBlank()) entry.addProperty("block", blockId);
            entry.addProperty("player",   (actor == null || actor.isBlank()) ? "System" : actor);
            entry.addProperty("severity", sev.name());
            if (url != null && !url.isBlank()) entry.addProperty("url", url);
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

    /** Record with block + actor in scope, but no re-download URL. */
    public static void record(String context, String blockId, String actor, Throwable ex) {
        record(context, blockId, actor, null, ex);
    }

    /** Record an incident with context + throwable (no block / player in scope → "—" / "System"). */
    public static void record(String context, Throwable ex) { record(context, null, null, null, ex); }

    /** Convenience: incident without a throwable. */
    public static void record(String context) { record(context, null, null, null, null); }

    /** Newest-first structured snapshot of all recorded incidents (for the IT Chest dashboard). */
    public static synchronized List<Incident> recent() {
        List<Incident> out = new ArrayList<>();
        try {
            JsonArray arr = loadArray();
            for (int i = arr.size() - 1; i >= 0; i--) {
                JsonObject e = arr.get(i).getAsJsonObject();
                String time  = e.has("time")    ? e.get("time").getAsString()    : "?";
                String ctx   = e.has("context") ? e.get("context").getAsString() : "?";
                String error = e.has("error")   ? e.get("error").getAsString()   : null;
                String block = e.has("block")   ? e.get("block").getAsString()   : null;
                String pl    = e.has("player")  ? e.get("player").getAsString()  : "—";
                Severity sev = e.has("severity")
                        ? parseSeverity(e.get("severity").getAsString())
                        : deriveSeverity(ctx, error);   // old entries: derive on read
                String url = e.has("url") ? e.get("url").getAsString() : null;
                out.add(new Incident(time, ctx, error, block, pl, sev, url));
            }
        } catch (Exception e) {
            LOG.error("[CustomBlocks] Failed to read incidents", e);
        }
        return out;
    }

    /** Return all incidents as formatted display lines (newest first) — console / text fallback. */
    public static List<String> list() {
        List<Incident> all = recent();
        if (all.isEmpty()) return List.of("§7No incidents recorded.");
        List<String> lines = new ArrayList<>();
        for (Incident in : all) {
            String time = in.time().length() >= 19 ? in.time().substring(0, 19).replace('T', ' ') : in.time();
            String col  = switch (in.severity()) { case ERROR -> "§c"; case WARN -> "§e"; default -> "§a"; };
            String err  = in.error() == null ? "" : " §c" + in.error();
            lines.add("§7[" + time + "] " + col + in.context()
                    + " §8(" + in.player() + (in.block() == null ? "" : " · " + in.block()) + ")" + err);
        }
        return lines;
    }

    /** Clear all recorded incidents. */
    public static synchronized void clear() {
        try { Files.deleteIfExists(FILE); } catch (Exception ignored) {}
    }

    /** throwable / "fail|error" → ERROR · "skip|warn" → WARN · else INFO. */
    private static Severity deriveSeverity(String context, String error) {
        if (error != null && !error.isBlank()) return Severity.ERROR;
        String s = (context == null ? "" : context).toLowerCase();
        if (s.contains("fail") || s.contains("error")) return Severity.ERROR;
        if (s.contains("skip") || s.contains("warn"))  return Severity.WARN;
        return Severity.INFO;
    }

    private static Severity parseSeverity(String s) {
        try { return Severity.valueOf(s.toUpperCase()); }
        catch (Exception e) { return Severity.INFO; }
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
