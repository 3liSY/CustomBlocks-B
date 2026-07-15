/**
 * GuessShowcaseStore.java — Group 30 (Guess Mode) · G30-8b Showcase. SERVER source of truth.
 *
 * Responsibility: the ONE shared "showcase" tuning — the look/feel every placed Showcase display uses
 * (there's no per-display tuning; every showcase reads these, mirroring how the pose is one shared value —
 * see {@link GuessPoseStore}). Holds the core spin speed and overall size. (The old outer-layer orbit +
 * particles were removed with the outer layer — G30 §S: S1/S7; the glow toggle was scrapped — G30 §S: S6.)
 * Persisted atomically
 * to config/customblocks/guessshowcase.json and broadcast
 * to all clients by {@link com.customblocks.network.GuessSync} so a change takes effect live (NO-REJOIN).
 * The per-display state (position + which block's picture it shows) lives on the placed BlockEntity instead.
 *
 * Depends on: Gson.
 * Called by:  the GuessShowcasePayload receiver (mutations from the Showcase tab), GuessSync (read for broadcast).
 */
package com.customblocks.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class GuessShowcaseStore {

    private static final String FILE = "config/customblocks/guessshowcase.json";

    // Defaults — a gentle end-crystal feel: the core turntables slowly.
    public static final float DEF_INNER = 30f;    // core spin, degrees/second (turntable)
    public static final float DEF_SIZE  = 1.0f;   // overall scale multiplier

    // Slider ranges (shared with the Showcase tab).
    public static final float SIZE_MIN = 0.3f, SIZE_MAX = 3.0f;
    public static final float SPEED_MIN = -180f, SPEED_MAX = 180f;

    // Live values. One shared tuning applied to every placed Showcase.
    private static float inner = DEF_INNER, size = DEF_SIZE;

    static { load(); }

    private GuessShowcaseStore() {} // static-only

    public static synchronized float inner()     { return inner; }
    public static synchronized float size()      { return size;  }

    /** Set every tuning value at once (clamped) and persist in a single write. Used by the Showcase tab. */
    public static synchronized void setAll(float inner, float size) {
        GuessShowcaseStore.inner = clamp(inner, SPEED_MIN, SPEED_MAX);
        GuessShowcaseStore.size  = clamp(size,  SIZE_MIN,  SIZE_MAX);
        save();
    }

    /** Reset every knob to the shipped defaults and persist. */
    public static synchronized void reset() {
        inner = DEF_INNER; size = DEF_SIZE;
        save();
    }

    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }

    /** Serialize for the client feed (GuessSync). */
    public static synchronized JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("in", inner); o.addProperty("sz", size);
        return o;
    }

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
            inner = f(o, "in", DEF_INNER); size = f(o, "sz", DEF_SIZE);
        } catch (Exception ignored) {}
    }

    private static float f(JsonObject o, String k, float def) {
        return (o.has(k) && o.get(k).isJsonPrimitive()) ? o.get(k).getAsFloat() : def;
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("guessshowcase.json.tmp");
            Files.writeString(tmp, toJson().toString(), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
