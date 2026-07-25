/**
 * SlotDataStore.java
 *
 * Responsibility: The ONLY class that reads/writes slot assignments to disk
 * (design rule #4, FR-01). Persists all assigned SlotData to
 * config/customblocks/slots.json using an atomic temp-file + move (NFR-13).
 *
 * Format: { "blocks": [ { "index", "customId", "displayName", "glow", "hardness", "sound", "noCollision", "category" }, ... ] }
 * Fields grow per-phase alongside SlotData (older files missing a field default safely).
 * Optional objects: "anim" (Group 14, animated only) and "arabic" (G13-25, Arabic slots only).
 *
 * Depends on: SlotData
 * Called by:  SlotManager (saveAll / loadAll) only.
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SlotDataStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("CustomBlocks");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIR = "config/customblocks";
    private static final String FILE = "slots.json";

    private SlotDataStore() {} // static-only

    /** Atomically write all assigned slot data to disk. */
    public static void save(Collection<SlotData> all) {
        Path dir = Path.of(DIR);
        Path file = dir.resolve(FILE);
        try {
            Files.createDirectories(dir);
            JsonArray arr = new JsonArray();
            for (SlotData d : all) arr.add(toJsonObject(d));
            JsonObject root = new JsonObject();
            root.add("blocks", arr);
            Path tmp = dir.resolve(FILE + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOGGER.error("[CustomBlocks] Failed to save slot data", e);
        }
    }

    /** Load all slot data from disk; returns an empty list if missing or corrupt. */
    public static List<SlotData> load() {
        List<SlotData> out = new ArrayList<>();
        Path file = Path.of(DIR).resolve(FILE);
        if (!Files.exists(file)) return out;
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("blocks")) return out;
            for (JsonElement el : root.getAsJsonArray("blocks")) {
                JsonObject o = el.getAsJsonObject();
                int index = o.get("index").getAsInt();
                String customId = o.get("customId").getAsString();
                String displayName = o.has("displayName") ? o.get("displayName").getAsString() : customId;
                int glow = o.has("glow") ? o.get("glow").getAsInt() : 0;
                float hardness = o.has("hardness") ? o.get("hardness").getAsFloat() : SlotData.DEFAULT_HARDNESS;
                String sound = o.has("sound") ? o.get("sound").getAsString() : SlotData.DEFAULT_SOUND;
                boolean noCollision = o.has("noCollision") && o.get("noCollision").getAsBoolean();
                String category = o.has("category") ? o.get("category").getAsString() : SlotData.DEFAULT_CATEGORY;
                String shape = o.has("shape") ? o.get("shape").getAsString() : SlotData.DEFAULT_SHAPE;
                AnimData anim = o.has("anim") && o.get("anim").isJsonObject()
                        ? animFromJson(o.getAsJsonObject("anim")) : AnimData.NONE;
                ArabicMeta arabic = o.has("arabic") && o.get("arabic").isJsonObject()
                        ? arabicFromJson(o.getAsJsonObject("arabic")) : null; // absent = normal block (G13-25)
                // absent = a block from before G10 §C → the default black fill, which is what it was baked as
                String background = o.has("background") ? o.get("background").getAsString() : BackgroundValue.DEFAULT;
                out.add(new SlotData(index, customId, displayName, glow, hardness, sound, noCollision, category, shape, anim, arabic, background));
            }
        } catch (Exception e) {
            LOGGER.error("[CustomBlocks] Failed to load slot data (starting empty)", e);
        }
        return out;
    }

    /**
     * Serialize ONE slot to its on-disk JSON shape (index, customId, displayName, glow, hardness,
     * sound, optional noCollision/category/shape/anim). Shared by {@link #save} and the Group 20
     * vault codec so the share payload is byte-for-byte the same format as slots.json. The "index"
     * is the source slot's number — importers (vault download) allocate a fresh slot and ignore it.
     */
    public static JsonObject toJsonObject(SlotData d) {
        JsonObject o = new JsonObject();
        o.addProperty("index", d.index());
        o.addProperty("customId", d.customId());
        o.addProperty("displayName", d.displayName());
        o.addProperty("glow", d.glow());
        o.addProperty("hardness", d.hardness());
        o.addProperty("sound", d.soundType());
        if (d.noCollision()) o.addProperty("noCollision", true); // omit the common default
        if (!d.category().isEmpty()) o.addProperty("category", d.category()); // omit when uncategorized
        if (!d.shape().equals(SlotData.DEFAULT_SHAPE)) o.addProperty("shape", d.shape()); // omit "full"
        if (!BackgroundValue.DEFAULT.equals(d.background())) o.addProperty("background", d.background()); // omit "black" (G10 §C)
        if (d.isAnimated()) o.add("anim", animToJson(d.anim())); // Group 14 — only present for animated blocks
        if (d.isArabic()) o.add("arabic", arabicToJson(d.arabic())); // G13-25 — only present for Arabic slots
        return o;
    }

    // ── G13-25 — Arabic meta (de)serialization (same optional-object pattern as anim) ──

    /** Serialize ArabicMeta ({glyphId, form, colorKey}) — only ever written for Arabic slots. */
    private static JsonObject arabicToJson(ArabicMeta m) {
        JsonObject o = new JsonObject();
        o.addProperty("glyphId", m.glyphId());
        o.addProperty("form", m.form());
        o.addProperty("colorKey", m.colorKey());
        return o;
    }

    /** Read ArabicMeta back; missing fields default safely; a blank glyphId means "not Arabic" (null). */
    private static ArabicMeta arabicFromJson(JsonObject o) {
        String glyphId  = o.has("glyphId")  ? o.get("glyphId").getAsString()  : "";
        int form        = o.has("form")     ? o.get("form").getAsInt()        : 0;
        String colorKey = o.has("colorKey") ? o.get("colorKey").getAsString() : "black";
        ArabicMeta m = new ArabicMeta(glyphId, form, colorKey);
        return m.isValid() ? m : null;
    }

    // ── Group 14 — animation (de)serialization (plain numbers only, never a baked mcmeta) ──

    /** Serialize AnimData to JSON. The original per-frame times are written as a plain int array. */
    private static JsonObject animToJson(AnimData a) {
        JsonObject o = new JsonObject();
        o.addProperty("frameCount", a.frameCount());
        o.addProperty("uniformTicks", a.uniformTicks()); // 0 = play at the original per-frame timing
        o.addProperty("loopMode", a.loopMode());
        o.addProperty("interpolate", a.interpolate());
        o.addProperty("trimStart", a.trimStart());
        o.addProperty("trimEnd", a.trimEnd());
        if (a.transparency()) o.addProperty("transparency", true); // omit the common default
        if (!a.frameTimes().isEmpty()) {
            JsonArray ft = new JsonArray();
            for (int t : a.frameTimes()) ft.add(t);
            o.add("frameTimes", ft);
        }
        if (!a.frameMs().isEmpty()) { // ADR-014: real per-frame ms for the off-atlas real-clock renderer
            JsonArray fm = new JsonArray();
            for (int t : a.frameMs()) fm.add(t);
            o.add("frameMs", fm);
        }
        return o;
    }

    /** Read AnimData back; any missing field falls back to the AnimData defaults. Public so the
     *  Group 20 vault codec can restore animation from a downloaded block's JSON. */
    public static AnimData animFromJson(JsonObject o) {
        int frameCount = o.has("frameCount") ? o.get("frameCount").getAsInt() : 0;
        String loop    = o.has("loopMode")   ? o.get("loopMode").getAsString() : AnimData.LOOP;
        boolean interp = o.has("interpolate") ? o.get("interpolate").getAsBoolean() : AnimData.DEFAULT_INTERPOLATE;
        int trimStart  = o.has("trimStart")  ? o.get("trimStart").getAsInt() : 0;
        int trimEnd    = o.has("trimEnd")    ? o.get("trimEnd").getAsInt()   : frameCount - 1;
        boolean transp = o.has("transparency") && o.get("transparency").getAsBoolean();
        List<Integer> frameTimes = new ArrayList<>();
        if (o.has("frameTimes") && o.get("frameTimes").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("frameTimes")) frameTimes.add(e.getAsInt());
        }
        List<Integer> frameMs = new ArrayList<>(); // absent (legacy) → renderer falls back to ticks × 50
        if (o.has("frameMs") && o.get("frameMs").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("frameMs")) frameMs.add(e.getAsInt());
        }
        // uniformTicks (new) is canonical. Back-compat with the first Group 14 build, which stored a
        // "frametime" + an empty frameTimes for a uniform clip (per-frame clips stored frameTimes):
        // old uniform → uniformTicks = that frametime; old per-frame → uniformTicks = 0 (original).
        int uniformTicks;
        if (o.has("uniformTicks")) uniformTicks = o.get("uniformTicks").getAsInt();
        else if (o.has("frametime")) uniformTicks = frameTimes.isEmpty() ? o.get("frametime").getAsInt() : 0;
        else uniformTicks = 0;
        return new AnimData(frameCount, uniformTicks, loop, interp, trimStart, trimEnd, transp, frameTimes, frameMs);
    }
}
