/**
 * ImportReport.java — what one folder-import run did, kept so it can be shown again (Group 12 §B/§C).
 *
 * ONE report object, written once when a run commits, rendered by both the live result and the
 * {@code /cb importfolder last} recall (rule 21). §C is explicitly "the same report kept, not a second
 * reporting system", so there is no second shape of this anywhere.
 *
 * It survives a restart: the run is written to {@code data/last_import.json} straight after it commits
 * (owner's call, 2026-07-30), so a recall after a crash still answers.
 *
 * WHY IT STORES NO "undone" FLAG: a run is reversed by the ordinary {@code /cb undo}, and a single block
 * can also be deleted from the report itself, so a stored flag would go stale the moment either happened
 * without this class watching. Instead the renderer asks {@link SlotManager} whether each created id is
 * still there at the moment of reading (see {@link #stillLive}) — so an undone run reports itself undone
 * because the blocks are genuinely gone, never because a boolean claimed so (TG12 C5). That also keeps
 * G12 out of the undo engine, which it has no business hooking.
 *
 * Depends on: CbPaths, SlotManager, Gson
 * Called by:  ImportService (record), ImportChat (render), ImportFolderCommands (recall)
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class ImportReport {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "last_import.json";

    /** One block the run created: the id it claimed, its display name, and the file it came from. */
    public record Made(String id, String name, String file) {}

    /** One file the run did not import, and the plain-language reason. */
    public record Missed(String file, String reason) {}

    private final long whenMs;
    private final String actor;
    private final List<Made> made;
    private final List<Missed> missed;
    private final List<String> leftover;

    /** The last run in memory. Loaded from disk on the first read after a restart. */
    private static volatile ImportReport last;
    private static volatile boolean loaded;

    public ImportReport(long whenMs, String actor, List<Made> made, List<Missed> missed, List<String> leftover) {
        this.whenMs = whenMs;
        this.actor = actor == null ? "" : actor;
        this.made = List.copyOf(made);
        this.missed = List.copyOf(missed);
        this.leftover = List.copyOf(leftover);
    }

    public long whenMs()             { return whenMs; }
    public String actor()            { return actor; }
    public List<Made> made()         { return made; }
    public List<Missed> missed()     { return missed; }
    public List<String> leftover()   { return leftover; }

    /** The created blocks that are STILL in the world right now — read live, never cached. */
    public List<Made> stillLive() {
        List<Made> live = new ArrayList<>();
        for (Made m : made) if (SlotManager.hasId(m.id())) live.add(m);
        return live;
    }

    /**
     * True when the run created blocks and not one of them is left — i.e. it has been undone (or every
     * block was deleted since). The recall says exactly that instead of listing blocks that are gone.
     */
    public boolean fullyReversed() {
        return !made.isEmpty() && stillLive().isEmpty();
    }

    // ── the one kept report ──────────────────────────────────────────────────

    /**
     * Record this run as the last one and write it to disk. Returns false when the write failed, so the
     * caller can SAY the report will not survive a restart instead of letting a later recall answer "no
     * import has ever been run" — §C's rule is that a missing report is stated, never papered over.
     */
    public boolean keep() {
        last = this;
        loaded = true;
        return save();
    }

    /** The last run, or null when none has happened since the mod was installed (TG12 C4). */
    public static ImportReport last() {
        if (!loaded) {
            synchronized (ImportReport.class) {
                if (!loaded) { last = load(); loaded = true; }
            }
        }
        return last;
    }

    private static Path file() {
        return CbPaths.DATA.resolve(FILE_NAME);
    }

    private boolean save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("whenMs", whenMs);
            root.addProperty("actor", actor);
            JsonArray createdArr = new JsonArray();
            for (Made m : made) {
                JsonObject o = new JsonObject();
                o.addProperty("id", m.id());
                o.addProperty("name", m.name());
                o.addProperty("file", m.file());
                createdArr.add(o);
            }
            root.add("created", createdArr);
            JsonArray missedArr = new JsonArray();
            for (Missed s : missed) {
                JsonObject o = new JsonObject();
                o.addProperty("file", s.file());
                o.addProperty("reason", s.reason());
                missedArr.add(o);
            }
            root.add("skipped", missedArr);
            JsonArray leftArr = new JsonArray();
            for (String l : leftover) leftArr.add(l);
            root.add("leftover", leftArr);

            Path target = file();
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(FILE_NAME + ".tmp"); // atomic temp-rename (NFR-13)
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            // A report is a convenience, never the blocks themselves — a failed write must not fail a run.
            IncidentRecorder.record("Could not save the last import report", e);
            return false;
        }
    }

    private static ImportReport load() {
        try {
            Path p = file();
            if (!Files.isRegularFile(p)) return null;
            JsonObject root = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) return null;
            List<Made> made = new ArrayList<>();
            if (root.has("created")) {
                for (JsonElement el : root.getAsJsonArray("created")) {
                    JsonObject o = el.getAsJsonObject();
                    made.add(new Made(str(o, "id"), str(o, "name"), str(o, "file")));
                }
            }
            List<Missed> missed = new ArrayList<>();
            if (root.has("skipped")) {
                for (JsonElement el : root.getAsJsonArray("skipped")) {
                    JsonObject o = el.getAsJsonObject();
                    missed.add(new Missed(str(o, "file"), str(o, "reason")));
                }
            }
            List<String> leftover = new ArrayList<>();
            if (root.has("leftover")) {
                for (JsonElement el : root.getAsJsonArray("leftover")) leftover.add(el.getAsString());
            }
            long when = root.has("whenMs") ? root.get("whenMs").getAsLong() : 0L;
            return new ImportReport(when, str(root, "actor"), made, missed, leftover);
        } catch (Exception e) {
            return null; // an unreadable file means "no report", never a wrong one
        }
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }
}
