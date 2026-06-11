/**
 * OmniToolState.java
 *
 * Per-player state for the Omni-Tool (Group 06): the active mode (Glow / Hardness /
 * Eyedrop) and the Eyedrop clipboard. The mode persists to
 * config/customblocks/data/omni_tool.json so a player keeps their preference across
 * restarts; the clipboard is in-memory only.
 *
 * Depends on: Gson, SlotData
 * Called by:  OmniToolItem (read/cycle/copy), OmniMenu (switch mode)
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OmniToolState {

    private OmniToolState() {} // static-only

    /**
     * The Omni-Tool's modes — the three tools it merges: the Brush (Glow), the Chisel
     * (Hardness) and the Rainbow Rectangle (Area selection).
     */
    public enum Mode {
        GLOW("Glow", "§e"),
        HARDNESS("Hardness", "§7"),
        AREA("Area", "§6");

        public final String label;
        public final String color;
        Mode(String label, String color) { this.label = label; this.color = color; }

        /** Next mode in the cycle (wraps). */
        public Mode next() {
            Mode[] v = values();
            return v[(ordinal() + 1) % v.length];
        }

        public static Mode fromName(String s) {
            if (s == null) return GLOW;
            try { return valueOf(s); } catch (IllegalArgumentException e) { return GLOW; }
        }
    }

    private static final Path FILE = Path.of("config/customblocks/data", "omni_tool.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<UUID, Mode> MODE = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    public static Mode getMode(UUID player) {
        ensureLoaded();
        return MODE.getOrDefault(player, Mode.GLOW);
    }

    public static void setMode(UUID player, Mode mode) {
        ensureLoaded();
        MODE.put(player, mode);
        save();
    }

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            if (Files.exists(FILE)) {
                JsonObject o = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                for (String key : o.keySet()) {
                    try {
                        MODE.put(UUID.fromString(key), Mode.fromName(o.get(key).getAsString()));
                    } catch (IllegalArgumentException ignored) { /* skip bad uuid */ }
                }
            }
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Could not load omni_tool.json: {}", e.toString());
        }
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject o = new JsonObject();
            for (Map.Entry<UUID, Mode> e : MODE.entrySet()) o.addProperty(e.getKey().toString(), e.getValue().name());
            Files.writeString(FILE, GSON.toJson(o), StandardCharsets.UTF_8);
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Could not save omni_tool.json: {}", e.toString());
        }
    }
}
