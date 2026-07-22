/**
 * LowResPlayers.java — GROUP 05 §F (server-authoritative per-player pack resolution). SERVER-SIDE.
 *
 * Responsibility: hold + persist which texture resolution (128 / 256 / full 512) the server sends
 * each player, keyed by UUID, with a "pending" table for names set while offline. The SERVER is the
 * single authority — the client no longer downscales anything (the old client-only LowResState /
 * LowResScaler were retired 2026-07-19). On join {@link #bindOnJoin} folds a pending name-override
 * into the real UUID record (case-insensitive), so an owner can pre-set a friend before they log in.
 *
 * File: config/customblocks/data/lowres_players.json — written atomically (temp + ATOMIC_MOVE) so an
 * interrupted save leaves the previous valid mapping readable (§I5). Malformed rows are skipped with
 * a specific warning and the rest of the map still loads (§I4).
 *
 *   {
 *     "players": { "<uuid>": { "ign": "Name", "size": 128 } },
 *     "pending": { "name":  { "ign": "Name", "size": 256 } }   // key = lower-cased IGN
 *   }
 *
 * Depends on: Gson.
 * Called by:  LowResCommands (/cblowres set/read), PackSyncService (sizeFor at sync time),
 *             CustomBlocksMod JOIN (bindOnJoin).
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LowResPlayers {

    private LowResPlayers() {} // static-only

    /** Sentinel "off"/base size — the pack's real texture size; nothing above this is shrunk. */
    public static final int FULL = 512;
    /** The only resolutions a player may be forced to (plus {@link #FULL} = off). */
    public static final int[] LOW_SIZES = {256, 128};

    private static final Path FILE = Path.of("config/customblocks/data", "lowres_players.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** One stored choice: the last-known display name + the selected size. */
    private static final class Entry {
        String ign;
        int size;
        Entry(String ign, int size) { this.ign = ign; this.size = size; }
    }

    /** uuid -> choice (a bound, known player). */
    private static final Map<UUID, Entry> BY_UUID = new ConcurrentHashMap<>();
    /** lower-cased IGN -> choice set while the player was offline (folded into BY_UUID on join). */
    private static final Map<String, Entry> PENDING = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    // ── size parsing ────────────────────────────────────────────────────────────
    /** Parse a /cblowres size token: 128 / 256 / 512 / off / full. Returns -1 for anything else (§F18). */
    public static int parseSize(String token) {
        if (token == null) return -1;
        String t = token.trim().toLowerCase(Locale.ROOT);
        return switch (t) {
            case "off", "full", "512" -> FULL;
            case "256" -> 256;
            case "128" -> 128;
            default -> -1;
        };
    }

    /** Human label for a stored size. */
    public static String label(int size) {
        return size >= FULL ? "off (full " + FULL + "px)" : size + "px";
    }

    // ── lookups ─────────────────────────────────────────────────────────────────
    /** The resolution the server should send this player right now (FULL when unset). */
    public static int sizeFor(UUID uuid) {
        ensureLoaded();
        Entry e = uuid == null ? null : BY_UUID.get(uuid);
        return e == null ? FULL : e.size;
    }

    // ── mutations ───────────────────────────────────────────────────────────────
    /** Set (or clear, when size == FULL) a known player's resolution and persist. */
    public static void setUuid(UUID uuid, String ign, int size) {
        if (uuid == null) return;
        ensureLoaded();
        if (size >= FULL) BY_UUID.remove(uuid);
        else              BY_UUID.put(uuid, new Entry(ign, size));
        save();
    }

    /** Set (or clear) a resolution for an offline IGN; folded into the UUID record when they next join. */
    public static void setPending(String ign, int size) {
        if (ign == null || ign.isEmpty()) return;
        ensureLoaded();
        String key = ign.toLowerCase(Locale.ROOT);
        if (size >= FULL) PENDING.remove(key);
        else              PENDING.put(key, new Entry(ign, size));
        save();
    }

    /**
     * On join, reconcile identity into ONE deterministic record (§F5 / §F17): a matching pending
     * name-override (set while offline) wins and is folded into the UUID record with the current
     * display name; otherwise an existing UUID record just refreshes its stored name. Case-insensitive.
     */
    public static void bindOnJoin(UUID uuid, String ign) {
        if (uuid == null || ign == null) return;
        ensureLoaded();
        boolean changed = false;
        Entry pend = PENDING.remove(ign.toLowerCase(Locale.ROOT));
        if (pend != null) {
            if (pend.size >= FULL) BY_UUID.remove(uuid);
            else                   BY_UUID.put(uuid, new Entry(ign, pend.size));
            changed = true;
        } else {
            Entry cur = BY_UUID.get(uuid);
            if (cur != null && !ign.equals(cur.ign)) { cur.ign = ign; changed = true; } // rename → refresh label
        }
        if (changed) save();
    }

    // ── persistence ─────────────────────────────────────────────────────────────
    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            if (!Files.exists(FILE)) return;
            JsonElement parsed = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                CustomBlocksMod.LOGGER.warn("[CustomBlocks] lowres_players.json is not an object — ignoring.");
                return;
            }
            JsonObject root = parsed.getAsJsonObject();
            loadPlayers(root);
            loadPending(root);
        } catch (Exception e) {
            // A whole-file failure keeps whatever loaded and starts the rest fresh — never throws (§I5).
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Could not load lowres_players.json: {}", e.toString());
        }
    }

    private static void loadPlayers(JsonObject root) {
        if (!root.has("players") || !root.get("players").isJsonObject()) return;
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("players").entrySet()) {
            try {
                UUID id = UUID.fromString(e.getKey());
                Entry entry = readEntry(e.getValue());
                if (entry == null || entry.size >= FULL) continue; // FULL rows carry no override — drop them
                BY_UUID.put(id, entry);
            } catch (IllegalArgumentException bad) {
                CustomBlocksMod.LOGGER.warn("[CustomBlocks] lowres_players.json: skipping bad player row \"{}\".", e.getKey());
            }
        }
    }

    private static void loadPending(JsonObject root) {
        if (!root.has("pending") || !root.get("pending").isJsonObject()) return;
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("pending").entrySet()) {
            Entry entry = readEntry(e.getValue());
            if (entry == null || entry.size >= FULL) continue;
            PENDING.put(e.getKey().toLowerCase(Locale.ROOT), entry);
        }
    }

    /** Parse one {"ign","size"} object, tolerating a missing name; null (with a warning) if unusable. */
    private static Entry readEntry(JsonElement el) {
        if (el == null || !el.isJsonObject()) return null;
        JsonObject o = el.getAsJsonObject();
        if (!o.has("size") || o.get("size").isJsonNull()) return null;
        int size;
        try { size = o.get("size").getAsInt(); }
        catch (Exception bad) { return null; }
        if (size != 128 && size != 256 && size != FULL) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] lowres_players.json: ignoring out-of-range size {}.", size);
            return null;
        }
        String ign = o.has("ign") && !o.get("ign").isJsonNull() ? o.get("ign").getAsString() : "";
        return new Entry(ign, size);
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject root = new JsonObject();
            JsonObject players = new JsonObject();
            for (Map.Entry<UUID, Entry> e : BY_UUID.entrySet()) players.add(e.getKey().toString(), entryJson(e.getValue()));
            JsonObject pending = new JsonObject();
            for (Map.Entry<String, Entry> e : PENDING.entrySet()) pending.add(e.getKey(), entryJson(e.getValue()));
            root.add("players", players);
            root.add("pending", pending);
            Path tmp = FILE.resolveSibling("lowres_players.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Could not save lowres_players.json: {}", e.toString());
        }
    }

    private static JsonObject entryJson(Entry e) {
        JsonObject o = new JsonObject();
        o.addProperty("ign", e.ign == null ? "" : e.ign);
        o.addProperty("size", e.size);
        return o;
    }
}
