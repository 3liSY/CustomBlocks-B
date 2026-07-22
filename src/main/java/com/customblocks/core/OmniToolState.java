/**
 * OmniToolState.java
 *
 * Per-player state for the Omni-Tool (Group 06): the active mode (one of five, cycled in-hand)
 * plus a per-player in-memory Copy buffer. The mode persists to
 * config/customblocks/data/omni_tool.json so a player keeps their preference across restarts;
 * the Copy buffer is a session-only feel-stats clipboard that lives while Copy mode is active.
 *
 * Depends on: Gson, SlotData
 * Called by:  OmniToolItem (cycle mode / copy / paste), ToolCommands (give in current mode)
 */
package com.customblocks.core;

import com.customblocks.command.CbFmt;
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
     * The Omni-Tool's five modes, listed IN CYCLE ORDER (sneak+right-click steps through them,
     * wrapping via {@link Mode#next()}): Glow → Hardness → Face → Copy → Delete → back to Glow.
     * Delete is intentionally last (§F fixed order); the standalone Brush/Chisel are merged in.
     */
    public enum Mode {
        GLOW("Glow", CbFmt.VALUE),
        HARDNESS("Hardness", CbFmt.DIM),
        FACE("Face", CbFmt.VALUE),
        COPY("Copy", CbFmt.VALUE),
        DELETE("Delete", CbFmt.BAD);

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

    /**
     * The four feel-stats Copy mode grabs and pastes (glow, hardness, sound, walk-through) — NOT look,
     * shape, name, or category (§I5). A per-player, session-only clipboard; it need not persist, since
     * Copy is a live copy→paste loop that ends the moment the player leaves Copy mode.
     */
    public record Feel(int glow, float hardness, String soundType, boolean noCollision) {}

    private static final Map<UUID, Feel> COPY_BUF = new ConcurrentHashMap<>();

    public static Mode getMode(UUID player) {
        ensureLoaded();
        return MODE.getOrDefault(player, Mode.GLOW);
    }

    public static void setMode(UUID player, Mode mode) {
        ensureLoaded();
        MODE.put(player, mode);
        // §I3: the paste buffer/prompt lives only while Copy mode is active — leaving it drops the clipboard.
        if (mode != Mode.COPY) clearCopy(player);
        save();
    }

    // ── Copy-mode clipboard (session-only, per player) ────────────────────────
    public static void setCopy(UUID player, Feel feel) { COPY_BUF.put(player, feel); }
    public static Feel getCopy(UUID player)            { return COPY_BUF.get(player); }
    public static void clearCopy(UUID player)          { COPY_BUF.remove(player); }

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
