/**
 * HudConfig.java
 *
 * Responsibility: Client-side configuration + persistence for the CustomBlocks HUD
 * overlay — visibility, position, scale, text color, background opacity, and which
 * fields to show. Persisted to config/customblocks/data/hud-config-server.json with an
 * atomic write (temp file + ATOMIC_MOVE), matching the project's NFR-13 file pattern.
 *
 * Scope note: although the file name ends in "-server", the HUD is a client render
 * concern, so this client class owns the file. In single-player / integrated-server
 * (how the mod is tested) the client and server share the run directory, so the path
 * resolves to the same place a dedicated server would use.
 *
 * Depends on: CustomBlocksConfig (initial enabled default), Gson
 * Called by: HudRenderer (reads), HudEditorScreen (edits + saves), CustomBlocksClient
 *            (loads on init), ConfigCommands / HudStatePayload receiver (toggle).
 */
package com.customblocks.client;

import com.customblocks.CustomBlocksConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Environment(EnvType.CLIENT)
public final class HudConfig {

    private HudConfig() {}

    private static final String FILE = "config/customblocks/data/hud-config-server.json";
    private static final Gson GSON = new Gson();

    // ── Defaults (spec GROUP_03) ───────────────────────────────────────
    public static final int     DEF_X         = 5;
    public static final int     DEF_Y         = 5;
    public static final float   DEF_SCALE     = 1.0f;
    public static final int     DEF_COLOR     = 0xFFFFFF;
    public static final float   DEF_BG        = 0.4f;
    public static final boolean DEF_SHOW_ID   = true;
    public static final boolean DEF_SHOW_NAME = true;

    // ── Live state ─────────────────────────────────────────────
    /** Whether the HUD overlay is currently visible (mirrors CustomBlocksConfig.hudEnabled). */
    public static boolean visible   = true;
    /** X position (pixels from left edge). */
    public static int     x         = DEF_X;
    /** Y position (pixels from top edge). */
    public static int     y         = DEF_Y;
    /** Text scale multiplier (0.5 .. 3.0). */
    public static float   scale     = DEF_SCALE;
    /** Text color as 0xRRGGBB. */
    public static int     color     = DEF_COLOR;
    /** Background rectangle opacity (0.0 .. 1.0; 0 = no background). */
    public static float   bgOpacity = DEF_BG;
    /** Show the block ID line. */
    public static boolean showId    = DEF_SHOW_ID;
    /** Show the display-name line. */
    public static boolean showName  = DEF_SHOW_NAME;

    // ── Preset color palette (for the editor's "Color" cycle button) ────────────
    public static final int[]    PALETTE       = {
            0xFFFFFF, 0xFFFF55, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFAA00, 0xAAAAAA
    };
    public static final String[] PALETTE_NAMES = {
            "White", "Yellow", "Green", "Aqua", "Red", "Pink", "Gold", "Gray"
    };

    /** Human-readable name of the current color (falls back to hex). */
    public static String colorName() {
        for (int i = 0; i < PALETTE.length; i++) if (PALETTE[i] == (color & 0xFFFFFF)) return PALETTE_NAMES[i];
        return toHex(color);
    }

    /** Advance the color to the next palette entry. */
    public static void cycleColor() {
        int idx = -1;
        for (int i = 0; i < PALETTE.length; i++) if (PALETTE[i] == (color & 0xFFFFFF)) { idx = i; break; }
        color = PALETTE[(idx + 1) % PALETTE.length];
    }

    /** Restore every field to its spec default (does not persist; call save() to commit). */
    public static void resetDefaults() {
        x = DEF_X; y = DEF_Y; scale = DEF_SCALE; color = DEF_COLOR;
        bgOpacity = DEF_BG; showId = DEF_SHOW_ID; showName = DEF_SHOW_NAME;
    }

    /** Seed the visible flag from the server config (first-run fallback only). */
    public static void syncFromConfig() {
        visible = CustomBlocksConfig.hudEnabled;
    }

    // ── Persistence ──────────────────────────────────────────

    /**
     * Load saved settings from disk. If the file is missing/unreadable, seed the visible
     * flag from CustomBlocksConfig and leave the rest at defaults.
     */
    public static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) { syncFromConfig(); return; }
            JsonObject o = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null) { syncFromConfig(); return; }
            visible   = getBool (o, "hudEnabled",   CustomBlocksConfig.hudEnabled);
            x         = getInt  (o, "hudX",         DEF_X);
            y         = getInt  (o, "hudY",         DEF_Y);
            scale     = clampScale(getFloat(o, "hudScale", DEF_SCALE));
            color     = parseHex(getString(o, "hudColor", toHex(DEF_COLOR)), DEF_COLOR);
            bgOpacity = clamp01(getFloat(o, "hudBgOpacity", DEF_BG));
            showId    = getBool (o, "hudShowId",    DEF_SHOW_ID);
            showName  = getBool (o, "hudShowName",  DEF_SHOW_NAME);
        } catch (Exception ignored) {
            syncFromConfig();
        }
    }

    /** Persist current settings with an atomic write (temp file + ATOMIC_MOVE). */
    public static void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject o = new JsonObject();
            o.addProperty("hudEnabled",   visible);
            o.addProperty("hudX",         x);
            o.addProperty("hudY",         y);
            o.addProperty("hudScale",     scale);
            o.addProperty("hudColor",     toHex(color));
            o.addProperty("hudBgOpacity", bgOpacity);
            o.addProperty("hudShowId",    showId);
            o.addProperty("hudShowName",  showName);
            Path tmp = file.resolveSibling("hud-config-server.json.tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }

    // ── Helpers ───────────────────────────────────────────────

    public static float clampScale(float s) { return Math.max(0.5f, Math.min(3.0f, s)); }
    public static float clamp01(float v)     { return Math.max(0.0f, Math.min(1.0f, v)); }

    public static String toHex(int rgb) { return String.format("#%06X", rgb & 0xFFFFFF); }

    public static int parseHex(String hex, int fallback) {
        try {
            String h = hex.trim();
            if (h.startsWith("#")) h = h.substring(1);
            return Integer.parseInt(h, 16) & 0xFFFFFF;
        } catch (Exception e) { return fallback; }
    }

    private static boolean getBool(JsonObject o, String k, boolean def) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsBoolean() : def;
    }
    private static int getInt(JsonObject o, String k, int def) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : def;
    }
    private static float getFloat(JsonObject o, String k, float def) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsFloat() : def;
    }
    private static String getString(JsonObject o, String k, String def) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : def;
    }
}
