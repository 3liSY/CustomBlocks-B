/**
 * AchievementStore.java
 *
 * Responsibility: The ONLY reader/writer of per-player achievement state. Holds, for each
 * player UUID, their progress counters and their unlocked achievements (key -> unlock epoch
 * millis). Persists to config/customblocks/data/achievements.json with an atomic write
 * (temp + ATOMIC_MOVE), mirroring OnboardingManager's pattern.
 *
 * Depends on: Gson, Achievement (Counter enum).
 * Called by:  AchievementManager only. Nothing else mutates achievement state directly.
 */
package com.customblocks.core.onboarding;

import com.customblocks.core.onboarding.Achievement.Counter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AchievementStore {

    private static final Logger LOG  = LoggerFactory.getLogger("CustomBlocks");
    private static final Gson   GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path   DIR  = Path.of("config/customblocks/data");
    private static final Path   FILE = DIR.resolve("achievements.json");

    /** uuid -> that player's record. Guarded by LOCK. */
    private static final Map<String, Record> PLAYERS = new HashMap<>();
    private static final Object LOCK = new Object();

    static { load(); }

    private AchievementStore() {}

    /** One player's mutable state: progress counters + unlocked keys with their unlock time. */
    private static final class Record {
        final Map<String, Integer> counters = new HashMap<>(); // Counter.name() -> value
        final Map<String, Long>    unlocked = new HashMap<>(); // achievement key -> epoch millis
    }

    private static Record recordFor(UUID uuid) {
        return PLAYERS.computeIfAbsent(uuid.toString(), k -> new Record());
    }

    /** Current value of a progress counter for a player (0 if never touched). */
    public static int counter(UUID uuid, Counter c) {
        synchronized (LOCK) {
            return recordFor(uuid).counters.getOrDefault(c.name(), 0);
        }
    }

    /** Add {@code delta} to a counter, persist, and return the new value. */
    public static int bumpCounter(UUID uuid, Counter c, int delta) {
        synchronized (LOCK) {
            Record r = recordFor(uuid);
            int next = r.counters.getOrDefault(c.name(), 0) + delta;
            r.counters.put(c.name(), next);
            save();
            return next;
        }
    }

    /** Whether the player has unlocked the given achievement key. */
    public static boolean isUnlocked(UUID uuid, String key) {
        synchronized (LOCK) {
            return recordFor(uuid).unlocked.containsKey(key);
        }
    }

    /** Unlock epoch millis, or 0 if the player has not unlocked this key. */
    public static long unlockedAt(UUID uuid, String key) {
        synchronized (LOCK) {
            return recordFor(uuid).unlocked.getOrDefault(key, 0L);
        }
    }

    /**
     * Record an unlock at {@code epochMs}. Returns true if this was NEW (so the caller should
     * notify), false if the player already had it (idempotent, no-op).
     */
    public static boolean markUnlocked(UUID uuid, String key, long epochMs) {
        synchronized (LOCK) {
            Record r = recordFor(uuid);
            if (r.unlocked.containsKey(key)) return false;
            r.unlocked.put(key, epochMs);
            save();
            return true;
        }
    }

    /** How many achievements this player has unlocked (the numerator for "X of Y"). */
    public static int unlockedCount(UUID uuid) {
        synchronized (LOCK) {
            return recordFor(uuid).unlocked.size();
        }
    }

    /** A snapshot copy of this player's unlocked keys -> unlock millis (for the gallery to read). */
    public static Map<String, Long> unlockedSnapshot(UUID uuid) {
        synchronized (LOCK) {
            return new HashMap<>(recordFor(uuid).unlocked);
        }
    }

    // ---- persistence ----------------------------------------------------------------

    private static void load() {
        synchronized (LOCK) {
            try {
                if (!Files.exists(FILE)) return;
                String json = Files.readString(FILE, StandardCharsets.UTF_8);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                if (!root.has("players")) return;
                JsonObject players = root.getAsJsonObject("players");
                for (String uuid : players.keySet()) {
                    JsonObject pj = players.getAsJsonObject(uuid);
                    Record r = new Record();
                    if (pj.has("counters")) {
                        JsonObject cj = pj.getAsJsonObject("counters");
                        for (String k : cj.keySet()) r.counters.put(k, cj.get(k).getAsInt());
                    }
                    if (pj.has("unlocked")) {
                        JsonObject uj = pj.getAsJsonObject("unlocked");
                        for (String k : uj.keySet()) r.unlocked.put(k, uj.get(k).getAsLong());
                    }
                    PLAYERS.put(uuid, r);
                }
            } catch (Exception e) {
                LOG.warn("[CustomBlocks] Could not load achievements.json: {}", e.getMessage());
            }
        }
    }

    /** Atomic write of the whole map. Called under LOCK by the mutators above. */
    private static void save() {
        try {
            Files.createDirectories(DIR);
            JsonObject root = new JsonObject();
            JsonObject players = new JsonObject();
            for (Map.Entry<String, Record> e : PLAYERS.entrySet()) {
                Record r = e.getValue();
                JsonObject pj = new JsonObject();
                JsonObject cj = new JsonObject();
                for (Map.Entry<String, Integer> c : r.counters.entrySet()) cj.addProperty(c.getKey(), c.getValue());
                JsonObject uj = new JsonObject();
                for (Map.Entry<String, Long> u : r.unlocked.entrySet()) uj.addProperty(u.getKey(), u.getValue());
                pj.add("counters", cj);
                pj.add("unlocked", uj);
                players.add(e.getKey(), pj);
            }
            root.add("players", players);
            Path tmp = DIR.resolve("achievements.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOG.error("[CustomBlocks] Failed to save achievements.json", e);
        }
    }
}
