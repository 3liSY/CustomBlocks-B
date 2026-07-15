/**
 * GuessModeStore.java — Group 30 (Guess Mode). v2 command redesign (2026-07-05).
 *
 * Responsibility: per-player "guess mode" flag, persisted by player UUID. A flagged player's OWN client
 * blinds the custom block(s) they're flagged for (name → "???", look → the bundled "?" mystery cube) and
 * everyone sees them hold a flagged block with a centered two-handed pose. This store is the SERVER's source
 * of truth; the set is synced to clients by {@link com.customblocks.network.GuessSync}. Mirrors
 * {@link FavoritesManager} (per-UUID map, atomic tmp→ATOMIC_MOVE write, {@code synchronized}, static self-load).
 *
 * A player's guess mode is now expressed as, per player:
 *   • all — blind EVERY custom block they see (the old "all" scope). Command: /cb guess &lt;p&gt; on all.
 *   • looks — a MAP of specific block ids to blind → an optional per-round disguise LOOK id (the block whose
 *     already-baked picture is shown instead). Each id stacks. Command: /cb guess &lt;p&gt; on &lt;blockid&gt;
 *     [look &lt;lookid&gt;]. A null look value means "flagged, no override" → fall back to the global default.
 * A player is "active" (in guess mode) when {@code all} is true OR {@code looks} is non-empty. There is no
 * separate enabled flag and no per-player blank text (hardcoded "???").
 *
 * v3 Phase 1 (2026-07-06): the disguise look is no longer hardcoded to the bundled "?". Resolution chain is
 * per-round override ({@code looks} value) → global {@link #defaultLook()} (/cb guess defaultblock) → bundled
 * "?" (client fallback). All look values are block IDS here; GuessSync resolves them to slot indices for the
 * client, and the bundled "?" fallback stays hardcoded on the client (never stored).
 *
 * Persisted (config/customblocks/guessmode.json):
 *   • "_default" — top-level string, the global default disguise-look block id (absent = none).
 *   • &lt;uuid&gt; → { all: bool, looks: { flaggedId: lookId-or-null } }. Old v2 files with { all, ids:[...] }
 *     are still read (each id migrates to a null-look entry) so a live upgrade keeps existing flags.
 * Only ACTIVE players are written; a bare /cb guess &lt;p&gt; off drops the player back to the default (absent).
 *
 * Depends on: (none — standalone, like FavoritesManager)
 * Called by:  GuessCommands (mutations), GuessSync (read for broadcast), SlotManager.renameId (id rename).
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GuessModeStore {

    private static final String FILE = "config/customblocks/guessmode.json";
    private static final Gson GSON = new Gson();

    /** Reserved JSON key (not a valid UUID) holding the global default look id. */
    private static final String DEFAULT_KEY = "_default";

    /** One player's guess-mode settings. Mutable holder. */
    public static final class Entry {
        public boolean all;                            // blind every custom block they see
        // flagged block id → optional per-round disguise-look id (null = use the global default). Insertion-
        // ordered so a status readout lists them in the order they were added.
        public final Map<String, String> looks = new LinkedHashMap<>();

        Entry() {}

        /** In guess mode iff all-mode is on or at least one specific id is flagged. */
        public boolean active() { return all || !looks.isEmpty(); }
    }

    // UUID string → settings.
    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    /** Global default disguise-look block id (the OP's saved go-to), or null when unset. */
    private static String defaultLook;

    static { load(); }

    private GuessModeStore() {} // static-only

    /** The record for a player, creating a fresh empty one on first touch (for a mutation). */
    private static Entry get(UUID player) {
        return ENTRIES.computeIfAbsent(player.toString(), k -> new Entry());
    }

    /** The record for a player without creating one (for a read), or null. */
    private static Entry peek(UUID player) {
        return ENTRIES.get(player.toString());
    }

    /** Whether a player is currently in guess mode (all-mode or at least one flagged id). */
    public static synchronized boolean isActive(UUID player) {
        Entry e = peek(player);
        return e != null && e.active();
    }

    /** All-mode flag for a player. */
    public static synchronized boolean isAll(UUID player) {
        Entry e = peek(player);
        return e != null && e.all;
    }

    /** A copy of the specific block ids flagged for a player (empty when none). */
    public static synchronized List<String> ids(UUID player) {
        Entry e = peek(player);
        return e == null ? List.of() : new ArrayList<>(e.looks.keySet());
    }

    /** The per-round disguise-look id set for a player's flagged block, or null when none (use default). */
    public static synchronized String lookFor(UUID player, String flaggedId) {
        Entry e = peek(player);
        return e == null ? null : e.looks.get(flaggedId);
    }

    /** The global default disguise-look block id, or null when unset. */
    public static synchronized String defaultLook() { return defaultLook; }

    /** /cb guess defaultblock &lt;id&gt; (null/empty clears) — set the global default disguise look. */
    public static synchronized void setDefaultLook(String id) {
        defaultLook = (id == null || id.isEmpty()) ? null : id;
        save();
    }

    /** /cb guess &lt;p&gt; on all — blind every custom block the player sees. */
    public static synchronized void setAll(UUID player, boolean on) {
        Entry e = on ? get(player) : peek(player);
        if (e == null) return;
        e.all = on;
        pruneIfInactive(player, e);
        save();
    }

    /** /cb guess &lt;p&gt; on &lt;id&gt; — add a specific block id to the blind set (keeps any existing look). */
    public static synchronized void addId(UUID player, String id) {
        if (id == null || id.isEmpty()) return;
        get(player).looks.putIfAbsent(id, null);
        save();
    }

    /** /cb guess &lt;p&gt; on &lt;id&gt; look &lt;lookId&gt; — flag the block AND set its per-round disguise look. */
    public static synchronized void setLook(UUID player, String id, String lookId) {
        if (id == null || id.isEmpty()) return;
        get(player).looks.put(id, (lookId == null || lookId.isEmpty()) ? null : lookId);
        save();
    }

    /** /cb guess &lt;p&gt; off &lt;id&gt; — stop blinding just that one block id. */
    public static synchronized void removeId(UUID player, String id) {
        Entry e = peek(player);
        if (e == null || id == null) return;
        e.looks.remove(id);
        pruneIfInactive(player, e);
        save();
    }

    /** Bare /cb guess &lt;p&gt; off — clear EVERYTHING (all-mode and every flagged id) for the player. */
    public static synchronized void clear(UUID player) {
        if (ENTRIES.remove(player.toString()) != null) save();
    }

    /** Drop a now-inactive record so the file only holds players actually in guess mode. */
    private static void pruneIfInactive(UUID player, Entry e) {
        if (!e.active()) ENTRIES.remove(player.toString());
    }

    /** A snapshot copy of every ACTIVE player's settings (for GuessSync broadcast). */
    public static synchronized Map<UUID, Entry> activeEntries() {
        Map<UUID, Entry> out = new LinkedHashMap<>();
        for (var e : ENTRIES.entrySet()) {
            Entry v = e.getValue();
            if (!v.active()) continue;
            try {
                Entry copy = new Entry();
                copy.all = v.all;
                copy.looks.putAll(v.looks);
                out.put(UUID.fromString(e.getKey()), copy);
            } catch (IllegalArgumentException ignored) {}
        }
        return out;
    }

    /**
     * Keep every player's flagged ids valid across a /cb reid (id → newId). Mirrors
     * FavoritesManager.renameId — called from SlotManager.renameId. No-op for players not flagged for
     * {@code oldId}.
     */
    public static synchronized void renameId(String oldId, String newId) {
        boolean changed = false;
        // The renamed block may be a FLAGGED block (a looks key) and/or a LOOK target (a looks value or the
        // global default) — rename it in every position so no reference dangles.
        if (oldId.equals(defaultLook)) { defaultLook = newId; changed = true; }
        for (Entry e : ENTRIES.values()) {
            if (e.looks.containsKey(oldId)) {              // renamed block is flagged for this player
                e.looks.put(newId, e.looks.remove(oldId)); // move the entry, keep its look override
                changed = true;
            }
            for (Map.Entry<String, String> le : e.looks.entrySet()) {
                if (oldId.equals(le.getValue())) { le.setValue(newId); changed = true; } // renamed block is a look
            }
        }
        if (changed) save();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject o = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null) return;
            if (o.has(DEFAULT_KEY) && o.get(DEFAULT_KEY).isJsonPrimitive())
                defaultLook = o.get(DEFAULT_KEY).getAsString();
            for (var entry : o.entrySet()) {
                if (DEFAULT_KEY.equals(entry.getKey()) || !entry.getValue().isJsonObject()) continue;
                JsonObject j = entry.getValue().getAsJsonObject();
                Entry e = new Entry();
                e.all = j.has("all") && j.get("all").getAsBoolean();
                if (j.has("looks") && j.get("looks").isJsonObject()) {           // v3 shape
                    for (var le : j.get("looks").getAsJsonObject().entrySet())
                        e.looks.put(le.getKey(), le.getValue().isJsonNull() ? null : le.getValue().getAsString());
                } else if (j.has("ids") && j.get("ids").isJsonArray()) {         // v2 shape → migrate (null looks)
                    for (var el : j.get("ids").getAsJsonArray())
                        if (el.isJsonPrimitive()) e.looks.put(el.getAsString(), null);
                }
                if (e.active()) ENTRIES.put(entry.getKey(), e);
            }
        } catch (Exception ignored) {}
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            if (defaultLook != null) root.addProperty(DEFAULT_KEY, defaultLook);
            for (var entry : ENTRIES.entrySet()) {
                Entry v = entry.getValue();
                if (!v.active()) continue;
                JsonObject j = new JsonObject();
                j.addProperty("all", v.all);
                JsonObject looks = new JsonObject();
                for (var le : v.looks.entrySet()) {
                    if (le.getValue() == null) looks.add(le.getKey(), JsonNull.INSTANCE);
                    else looks.addProperty(le.getKey(), le.getValue());
                }
                j.add("looks", looks);
                root.add(entry.getKey(), j);
            }
            Path tmp = file.resolveSibling("guessmode.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
