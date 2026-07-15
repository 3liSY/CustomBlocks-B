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

import com.customblocks.command.CbFmt;
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
     * {@code code} is the short, pasteable handle shown in chat (G04-4) — e.g. {@code E-45}.
     * Incidents written before the code registry existed have {@code code == null}.
     */
    public record Incident(String time, String context, String error,
                           String block, String player, Severity severity, String url, String code) {}

    private IncidentRecorder() {}

    /**
     * Record an incident and return its short code (e.g. {@code "E-45"}).
     *
     * G04-4: the code is the whole point of the code→incident registry. A player sees a plain-English
     * sentence plus this code, and a clickable link that runs {@code /cb incidents <code>} to jump
     * straight to the entry — so the raw exception NEVER has to be shown in chat to stay diagnosable.
     * The full throwable is still captured here, silently.
     *
     * blockId / actor / url may be null (→ "—" / "System" / no auto-fix). Severity is derived from the
     * throwable + context, never passed in. Thread-safe. Returns null only if the write itself failed.
     */
    public static synchronized String record(String context, String blockId, String actor,
                                             String url, Throwable ex) {
        try {
            String error = ex == null ? null : ex.getClass().getSimpleName() + ": " + ex.getMessage();
            Severity sev = deriveSeverity(context, error);
            JsonObject root = loadRoot();
            JsonArray arr = root.has("incidents") ? root.getAsJsonArray("incidents") : new JsonArray();

            int next = root.has("nextCode") ? root.get("nextCode").getAsInt() : 1;
            String code = codePrefix(sev) + "-" + next;

            JsonObject entry = new JsonObject();
            entry.addProperty("time",    Instant.now().toString());
            entry.addProperty("code",    code);
            entry.addProperty("context", context);
            if (error != null)               entry.addProperty("error",    error);
            if (blockId != null && !blockId.isBlank()) entry.addProperty("block", blockId);
            entry.addProperty("player",   (actor == null || actor.isBlank()) ? "System" : actor);
            entry.addProperty("severity", sev.name());
            if (url != null && !url.isBlank()) entry.addProperty("url", url);
            arr.add(entry);
            while (arr.size() > MAX_INCIDENTS) arr.remove(0);

            root = new JsonObject();
            root.add("incidents", arr);
            root.addProperty("nextCode", next + 1);
            Files.createDirectories(DIR);
            Path tmp = DIR.resolve("incidents.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return code;
        } catch (Exception e) {
            LOG.error("[CustomBlocks] Failed to write incident", e);
            return null;
        }
    }

    /** Record with block + actor in scope, but no re-download URL. Returns the short code. */
    public static String record(String context, String blockId, String actor, Throwable ex) {
        return record(context, blockId, actor, null, ex);
    }

    /** Record an incident with context + throwable (no block / player in scope → "—" / "System"). */
    public static String record(String context, Throwable ex) { return record(context, null, null, null, ex); }

    /** Convenience: incident without a throwable. */
    public static String record(String context) { return record(context, null, null, null, null); }

    /**
     * The code→incident registry lookup (G04-4). Case-insensitive so a player can paste "e-45".
     * Returns null if the code is unknown — it may simply have aged out of the last {@value #MAX_INCIDENTS}.
     */
    public static Incident byCode(String code) {
        if (code == null || code.isBlank()) return null;
        String want = code.trim().toUpperCase();
        for (Incident in : recent()) {
            if (in.code() != null && in.code().equalsIgnoreCase(want)) return in;
        }
        return null;
    }

    /** E- for an error, W- for a warning, I- for info. Keeps the code short and self-describing. */
    private static String codePrefix(Severity sev) {
        return switch (sev) { case ERROR -> "E"; case WARN -> "W"; case INFO -> "I"; };
    }

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
                // Written before the G04-4 code registry existed → no code. Degrade, don't crash.
                String code = e.has("code") ? e.get("code").getAsString() : null;
                out.add(new Incident(time, ctx, error, block, pl, sev, url, code));
            }
        } catch (Exception e) {
            LOG.error("[CustomBlocks] Failed to read incidents", e);
        }
        return out;
    }

    /** Return all incidents as formatted display lines (newest first) — console / text fallback. */
    public static List<String> list() {
        List<Incident> all = recent();
        if (all.isEmpty()) return List.of(CbFmt.DIM + "No incidents recorded.");
        List<String> lines = new ArrayList<>();
        for (Incident in : all) {
            String time = in.time().length() >= 19 ? in.time().substring(0, 19).replace('T', ' ') : in.time();
            String col  = switch (in.severity()) { case ERROR -> CbFmt.BAD; case WARN -> CbFmt.VALUE; default -> CbFmt.OK; };
            String err  = in.error() == null ? "" : " " + CbFmt.BAD + in.error();
            lines.add(CbFmt.DIM + "[" + time + "] " + col + in.context()
                    + " " + CbFmt.FAINT + "(" + in.player() + (in.block() == null ? "" : " · " + in.block()) + ")" + err);
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
        JsonObject root = loadRoot();
        return root.has("incidents") ? root.getAsJsonArray("incidents") : new JsonArray();
    }

    /** The whole file — incidents array plus the {@code nextCode} counter that backs the code registry. */
    private static JsonObject loadRoot() {
        try {
            if (!Files.exists(FILE)) return new JsonObject();
            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }
}
