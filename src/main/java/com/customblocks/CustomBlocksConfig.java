/**
 * CustomBlocksConfig.java
 *
 * Responsibility: Load/save mod configuration to config/customblocks/config.json.
 * Grows per-phase (Bible §7 "start minimal, expand per-phase") — only fields whose
 * feature is being built are added.
 *
 * All writes are atomic (temp file + ATOMIC_MOVE) so a mid-save crash cannot corrupt
 * the config (NFR-13, design rule #9).
 */
package com.customblocks;

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

public final class CustomBlocksConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("CustomBlocks");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "config/customblocks";
    private static final String CONFIG_FILE = "config.json";

    // ── Phase 1 ──────────────────────────────────────────────────────────────

    /** Number of slot blocks to pre-register at startup. Requires a restart to change. */
    public static volatile int maxSlots = 100;

    /** Port for the embedded resource-pack HTTP server. */
    public static volatile int httpPort = 8123;

    /** Block texture size in pixels (power of two: 16/32/64/128). */
    public static volatile int textureSize = 64;

    /** Host the resource-pack URL is served from (127.0.0.1 for local; set to server IP for LAN). */
    public static volatile String httpHost = "127.0.0.1";

    // ── Phase 6 ──────────────────────────────────────────────────────────────

    /** Max undo/redo steps per stack (RAM-only). */
    public static volatile int maxUndoDepth = 25;

    /** "global" = server-wide history; "per_player" = isolated per player. */
    public static volatile String undoMode = "global";

    // ── Phase 11 — HUD ───────────────────────────────────────────────────────

    /** Whether the block-info HUD overlay is shown when looking at a custom block. */
    public static volatile boolean hudEnabled = true;

    // ── Phase 13 — AI ────────────────────────────────────────────────────────

    /** API key for AI texture/command features (leave empty to disable). */
    public static volatile String aiApiKey = "";

    /** Whether AI texture generation is enabled (requires aiApiKey). */
    public static volatile boolean aiTextureEnabled = false;

    // ── Phase 14 — Cloud + Discord ───────────────────────────────────────────

    /** Cloudflare Block Vault endpoint URL (leave empty to disable). */
    public static volatile String vaultEndpoint = "";

    /** Discord webhook URL for block-event notifications (leave empty to disable). */
    public static volatile String discordWebhookUrl = "";

    private CustomBlocksConfig() {} // static-only

    /** Load config from disk, writing defaults if the file is missing. */
    public static void load() {
        Path dir = Path.of(CONFIG_DIR);
        Path file = dir.resolve(CONFIG_FILE);
        try {
            Files.createDirectories(dir);
            if (!Files.exists(file)) {
                save();
                LOGGER.info("[CustomBlocks] Created default config at {}", file);
                return;
            }
            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            maxSlots        = clamp(getInt(root, "maxSlots", maxSlots), 1, 8192);
            httpPort        = clamp(getInt(root, "httpPort", httpPort), 1, 65535);
            textureSize     = clamp(getInt(root, "textureSize", textureSize), 16, 256);
            httpHost        = getString(root, "httpHost", httpHost);
            maxUndoDepth    = clamp(getInt(root, "maxUndoDepth", maxUndoDepth), 1, 1000);
            String m        = getString(root, "undoMode", undoMode);
            undoMode        = ("per_player".equals(m) || "global".equals(m)) ? m : "global";
            hudEnabled      = getBool(root, "hudEnabled", hudEnabled);
            aiApiKey        = getString(root, "aiApiKey", aiApiKey);
            aiTextureEnabled = getBool(root, "aiTextureEnabled", aiTextureEnabled);
            vaultEndpoint   = getString(root, "vaultEndpoint", vaultEndpoint);
            discordWebhookUrl = getString(root, "discordWebhookUrl", discordWebhookUrl);
            LOGGER.info("[CustomBlocks] Config loaded: maxSlots={}, httpPort={}, textureSize={}, hudEnabled={}",
                    maxSlots, httpPort, textureSize, hudEnabled);
        } catch (Exception e) {
            LOGGER.error("[CustomBlocks] Failed to load config, using defaults", e);
        }
    }

    /** Save current config to disk via an atomic temp-file + move. */
    public static void save() {
        Path dir = Path.of(CONFIG_DIR);
        Path file = dir.resolve(CONFIG_FILE);
        try {
            Files.createDirectories(dir);
            JsonObject root = new JsonObject();
            root.addProperty("maxSlots",          maxSlots);
            root.addProperty("httpPort",           httpPort);
            root.addProperty("textureSize",        textureSize);
            root.addProperty("httpHost",           httpHost);
            root.addProperty("maxUndoDepth",       maxUndoDepth);
            root.addProperty("undoMode",           undoMode);
            root.addProperty("hudEnabled",         hudEnabled);
            root.addProperty("aiApiKey",           aiApiKey);
            root.addProperty("aiTextureEnabled",   aiTextureEnabled);
            root.addProperty("vaultEndpoint",      vaultEndpoint);
            root.addProperty("discordWebhookUrl",  discordWebhookUrl);
            Path tmp = dir.resolve(CONFIG_FILE + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOGGER.error("[CustomBlocks] Failed to save config", e);
        }
    }

    private static int    getInt   (JsonObject o, String k, int    d) { return o.has(k) ? o.get(k).getAsInt()     : d; }
    private static String getString(JsonObject o, String k, String d) { return o.has(k) ? o.get(k).getAsString()  : d; }
    private static boolean getBool (JsonObject o, String k, boolean d){ return o.has(k) ? o.get(k).getAsBoolean() : d; }
    private static int    clamp    (int v, int min, int max)          { return Math.max(min, Math.min(max, v)); }
}
