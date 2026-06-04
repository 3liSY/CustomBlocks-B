/**
 * CustomBlocksConfig.java
 *
 * Responsibility: Load/save mod configuration to config/customblocks/config.json.
 * Grows per-phase (Bible §7 "start minimal, expand per-phase") — only fields whose
 * feature is being built are added. Phase 1 has just maxSlots + httpPort.
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

    /**
     * Number of slot blocks to pre-register at startup. Requires a restart to change.
     * Default 800 (lighter/faster load); raise toward the 1028 cap if you need more.
     */
    public static volatile int maxSlots = 800;

    /** Port for the embedded resource-pack HTTP server (first used in Phase 4). */
    public static volatile int httpPort = 8123;

    private CustomBlocksConfig() {} // static-only

    /** Load config from disk, writing defaults if the file is missing. */
    public static void load() {
        Path dir = Path.of(CONFIG_DIR);
        Path file = dir.resolve(CONFIG_FILE);
        try {
            Files.createDirectories(dir);
            if (!Files.exists(file)) {
                save(); // write defaults
                LOGGER.info("[CustomBlocks] Created default config at {}", file);
                return;
            }
            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            maxSlots = clamp(getInt(root, "maxSlots", maxSlots), 1, 8192);
            httpPort = clamp(getInt(root, "httpPort", httpPort), 1, 65535);
            LOGGER.info("[CustomBlocks] Config loaded: maxSlots={}, httpPort={}", maxSlots, httpPort);
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
            root.addProperty("maxSlots", maxSlots);
            root.addProperty("httpPort", httpPort);
            Path tmp = dir.resolve(CONFIG_FILE + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOGGER.error("[CustomBlocks] Failed to save config", e);
        }
    }

    private static int getInt(JsonObject obj, String key, int def) {
        return obj.has(key) ? obj.get(key).getAsInt() : def;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
