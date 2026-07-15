/**
 * VaultHistory.java — server-wide log of share codes created via the Cloud Vault (Group 20 §S3).
 *
 * Every successful upload (block / category / note / backup) appends one entry here so codes aren't
 * lost. Shown by /cb vault codes, newest first. One shared server-wide list — all operators see every
 * code. Records on upload only (never on download). Persists to config/customblocks/data/vault_codes.json
 * via atomic write (NFR-13).
 *
 * The Entry schema is intentionally open (kind + label) so new share kinds can log here without
 * changing the file format.
 *
 * Depends on: (Gson, bundled with Minecraft)
 * Called by: CloudCommands (block + listing), CategoryCommands, NoteCommands, BackupCommands
 */
package com.customblocks.cloud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VaultHistory {

    private static final String FILE = "config/customblocks/data/vault_codes.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX = 500; // keep only the newest 500 codes on record

    /** One recorded share code. Open-ended on purpose (kind + label) so new share types can log here. */
    public static final class Entry {
        String kind  = "";   // "block" | "category" | "note" | "backup" | …
        String code  = "";   // the share code
        String label = "";   // what was shared (block id / category name / note id / backup name)
        String by    = "";   // uploader display name ("" = console/unknown)
        String byId  = "";   // uploader UUID string ("" = console/unknown)
        long   at    = 0L;   // epoch millis

        private Entry() {}

        public String kind()  { return kind; }
        public String code()  { return code; }
        public String label() { return label; }
        public String by()    { return by; }
        public String byId()  { return byId; }
        public long   at()    { return at; }
    }

    private static final List<Entry> DATA = new ArrayList<>(); // insertion order = oldest → newest

    static { load(); }

    private VaultHistory() {} // static-only

    /** Record a successful upload, pulling the uploader's name/uuid from the command source. */
    public static synchronized void record(String kind, String code, String label, ServerCommandSource src) {
        String name = "";
        String id = "";
        if (src != null) {
            name = src.getName();
            Entity e = src.getEntity();
            if (e != null) id = e.getUuidAsString();
        }
        record(kind, code, label, name, id);
    }

    /** Record a successful upload (raw name/uuid form). No-op on a blank code; dedupes by code. */
    public static synchronized void record(String kind, String code, String label, String byName, String byId) {
        if (code == null || code.isBlank()) return;
        String c = code.trim();
        for (Entry e : DATA) if (e.code.equals(c)) return; // already logged this code
        Entry e = new Entry();
        e.kind  = kind   == null ? "" : kind;
        e.code  = c;
        e.label = label  == null ? "" : label;
        e.by    = byName == null ? "" : byName;
        e.byId  = byId   == null ? "" : byId;
        e.at    = System.currentTimeMillis();
        DATA.add(e);
        while (DATA.size() > MAX) DATA.remove(0); // drop oldest
        save();
    }

    /** All recorded codes, newest first. */
    public static synchronized List<Entry> all() {
        List<Entry> out = new ArrayList<>(DATA);
        Collections.reverse(out);
        return out;
    }

    /** How many codes are on record. */
    public static synchronized int size() {
        return DATA.size();
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonArray arr = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonArray.class);
            if (arr == null) return;
            for (JsonElement el : arr) {
                try {
                    JsonObject o = el.getAsJsonObject();
                    Entry e = new Entry();
                    if (o.has("kind"))  e.kind  = o.get("kind").getAsString();
                    if (o.has("code"))  e.code  = o.get("code").getAsString();
                    if (o.has("label")) e.label = o.get("label").getAsString();
                    if (o.has("by"))    e.by    = o.get("by").getAsString();
                    if (o.has("byId"))  e.byId  = o.get("byId").getAsString();
                    if (o.has("at"))    e.at    = o.get("at").getAsLong();
                    if (!e.code.isEmpty()) DATA.add(e);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonArray arr = new JsonArray();
            for (Entry e : DATA) {
                JsonObject o = new JsonObject();
                o.addProperty("kind", e.kind);
                o.addProperty("code", e.code);
                o.addProperty("label", e.label);
                o.addProperty("by", e.by);
                o.addProperty("byId", e.byId);
                o.addProperty("at", e.at);
                arr.add(o);
            }
            Path tmp = file.resolveSibling("vault_codes.json.tmp");
            Files.writeString(tmp, GSON.toJson(arr), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
