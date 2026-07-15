/**
 * BlockNotesManager.java
 *
 * Responsibility: Store and retrieve the Lore attached to block IDs (Group 18 REVAMP v2). A block's lore
 * is a list of short lines plus an on/off switch, held as an immutable {@link NoteData}. Lore is
 * server-wide (an annotation on the block definition, visible to all). Persists to
 * config/customblocks/notes.json with an atomic write (NFR-13).
 *
 * Backward compatible — auto-migrates on first load, no data loss except the removed To-Do:
 *   • legacy flat {@code {id:"text"}}      → one line, enabled.
 *   • old 3-field {lore, tooltip, todo, …} → old tooltip line + old lore (split on newlines) become the
 *                                            lines; enabled = old tooltipEnabled OR the block had lore;
 *                                            the old To-Do list is dropped (To-Do was cut in the revamp).
 *
 * Depends on: NoteData
 * Called by:  NoteCommands, NotesMenu, HudSync (activeLore), SlotManager (renameId), delete paths
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlockNotesManager {

    private static final String FILE = "config/customblocks/notes.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, NoteData> NOTES = new LinkedHashMap<>();

    static { load(); }

    private BlockNotesManager() {}

    // ── Reads ───────────────────────────────────────────────────────────────

    /** The full lore for a block, or {@link NoteData#EMPTY} when none is set (never null). */
    public static synchronized NoteData get(String id) {
        return NOTES.getOrDefault(id, NoteData.EMPTY);
    }

    /** True when this block has any lore content. */
    public static synchronized boolean hasNote(String id) {
        NoteData d = NOTES.get(id);
        return d != null && !d.isEmpty();
    }

    /** The lore lines to show on the item, or an empty list when disabled/empty (used by on-item hover). */
    public static synchronized List<String> activeLore(String id) {
        NoteData d = NOTES.get(id);
        if (d == null || !d.enabled() || d.lines().isEmpty()) return List.of();
        return d.lines();
    }

    // ── Writes ──────────────────────────────────────────────────────────────

    /** Replace the whole lore. Empty lore is dropped so notes.json stays tidy. */
    public static synchronized void update(String id, NoteData data) {
        if (data == null || data.isEmpty()) NOTES.remove(id);
        else NOTES.put(id, data);
        save();
    }

    /** Add one line. Enables the lore automatically when it was the first line (so it shows by default). */
    public static synchronized void addLine(String id, String text) {
        NoteData d = get(id);
        boolean wasEmpty = d.lines().isEmpty();
        NoteData updated = d.addLine(text);
        if (wasEmpty && !updated.lines().isEmpty()) updated = updated.withEnabled(true);
        update(id, updated);
    }

    public static synchronized void editLine(String id, int index, String text) {
        update(id, disableIfNoLines(get(id).editLine(index, text)));
    }

    public static synchronized void removeLine(String id, int index) {
        update(id, disableIfNoLines(get(id).removeLine(index)));
    }

    public static synchronized void setEnabled(String id, boolean on) {
        update(id, get(id).withEnabled(on));
    }

    /** A note with no lines should not linger as "enabled" — that keeps update()'s tidy-drop working. */
    private static NoteData disableIfNoLines(NoteData d) {
        return d.lines().isEmpty() ? d.withEnabled(false) : d;
    }

    /** Clear all lore on a block. Returns true if there was anything to clear. */
    public static synchronized boolean clearNote(String id) {
        if (NOTES.remove(id) != null) { save(); return true; }
        return false;
    }

    /** Remove the lore for a block when the block itself is deleted (clean-up). */
    public static synchronized void onBlockDeleted(String id) {
        if (NOTES.remove(id) != null) save();
    }

    /** Move lore from {@code oldId} to {@code newId} (for /cb reid). No-op if oldId had none. */
    public static synchronized void renameId(String oldId, String newId) {
        NoteData d = NOTES.remove(oldId);
        if (d != null) { NOTES.put(newId, d); save(); }
    }

    // ── Share (Group 18 / R-S4) ───────────────────────────────────────────────

    /** Serialize a block's lore to the same JSON shape stored on disk (for vault sharing). */
    public static synchronized String exportJson(String id) {
        return GSON.toJson(toJson(get(id)));
    }

    /** Parse shared lore JSON back into a NoteData (auto-migrates legacy shapes). EMPTY on bad input. */
    public static NoteData importJson(String json) {
        try { return parse(JsonParser.parseString(json)); }
        catch (Exception e) { return NoteData.EMPTY; }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject o = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null) return;
            for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                NoteData d = parse(e.getValue());
                if (!d.isEmpty()) NOTES.put(e.getKey(), d);
            }
        } catch (Exception ignored) {}
    }

    /** Parse one entry: new {lines,enabled} as-is; old flat string and old 3-field shape auto-migrate. */
    private static NoteData parse(JsonElement el) {
        if (el == null) return NoteData.EMPTY;
        if (el.isJsonPrimitive()) return migrate("", el.getAsString(), false); // legacy {id:"text"}
        if (!el.isJsonObject()) return NoteData.EMPTY;
        JsonObject o = el.getAsJsonObject();

        // New format (Group 18 REVAMP v2): { "lines": [...], "enabled": bool }
        if (o.has("lines")) {
            List<String> lines = new ArrayList<>();
            if (o.get("lines").isJsonArray()) {
                for (JsonElement le : o.getAsJsonArray("lines"))
                    if (le.isJsonPrimitive()) { String s = le.getAsString(); if (!s.isEmpty()) lines.add(s); }
            }
            boolean enabled = o.has("enabled") && o.get("enabled").getAsBoolean();
            return new NoteData(lines, enabled);
        }

        // Old 3-field format → migrate (tooltip + lore become lines; To-Do dropped).
        String lore = o.has("lore") ? o.get("lore").getAsString() : "";
        String tooltip = o.has("tooltip") ? o.get("tooltip").getAsString() : "";
        boolean ttOn = o.has("tooltipEnabled") && o.get("tooltipEnabled").getAsBoolean();
        return migrate(tooltip, lore, ttOn);
    }

    /** Build the new line list from an old tooltip + lore; enabled = ttOn OR the block had lore. */
    private static NoteData migrate(String tooltip, String lore, boolean ttOn) {
        List<String> lines = new ArrayList<>();
        if (tooltip != null && !tooltip.isEmpty()) lines.add(tooltip);
        boolean hadLore = lore != null && !lore.isEmpty();
        if (hadLore) {
            for (String ln : lore.split("\n", -1)) if (!ln.isEmpty()) lines.add(ln);
        }
        return new NoteData(lines, ttOn || hadLore);
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            for (Map.Entry<String, NoteData> e : NOTES.entrySet()) root.add(e.getKey(), toJson(e.getValue()));
            Path tmp = file.resolveSibling("notes.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }

    private static JsonObject toJson(NoteData d) {
        JsonObject o = new JsonObject();
        JsonArray arr = new JsonArray();
        for (String line : d.lines()) arr.add(line);
        o.add("lines", arr);
        o.addProperty("enabled", d.enabled());
        return o;
    }
}
