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

import com.customblocks.image.BackgroundRemover;
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
    public static volatile int maxSlots = 800;

    /** Port for the embedded resource-pack HTTP server. */
    public static volatile int httpPort = 8123;

    /** Block texture size (px), a power of two 16..{@link #MAX_TEXTURE_SIZE}. Set via /cb config texturesize. */
    public static volatile int textureSize = 256;

    /** Hard ceiling for {@link #textureSize}. Atlas-rendered blocks muffle above 256px (ADR-007); the
     *  own-texture renderer (ADR-008) carries full resolution with no atlas, so 512px is allowed for it. */
    public static final int MAX_TEXTURE_SIZE = 512;

    /** Snap a requested size to a power of two, 16..{@link #MAX_TEXTURE_SIZE} (512→512, 300→256, 200→128). */
    public static int sanitizeTextureSize(int px) {
        return Integer.highestOneBit(Math.max(16, Math.min(MAX_TEXTURE_SIZE, px)));
    }

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

    // ── Group 05 — silent resource pack ──────────────────────────────────────

    /** Auto-accept the resource-pack prompt so textures apply with no dialog (default true; false shows
     *  the vanilla "download pack?" dialog). Server-forced packs always prompt — a Minecraft limitation. */
    public static volatile boolean silentPack = true;

    // ── Group 04 — chat & command communication ──────────────────────────────

    /**
     * Typo correction for unknown /cb subcommands:
     * "smart" (default) = suggest only when confident · "always" = suggest the closest
     * match even on weaker hits · "off" = plain unknown-command message, no suggestion.
     */
    public static volatile String didYouMean = "smart";

    // ── Phase 13 — AI (legacy key fields; removal deferred to G15.7) ──────────
    /** API key for AI texture/command features (leave empty to disable). */
    public static volatile String aiApiKey = "";
    /** Whether AI texture generation is enabled (requires aiApiKey). */
    public static volatile boolean aiTextureEnabled = false;

    // ── Group 15 — AI textures (keyless Pollinations.ai) ─────────────────────
    /** Appended to every AI prompt so results read as block textures. Default "pixel_art". */
    public static volatile String aiTextureStyle = "pixel_art";

    // ── Phase 14 — Cloud + Discord ───────────────────────────────────────────
    /** Cloudflare Block Vault endpoint URL (leave empty to disable). */
    public static volatile String vaultEndpoint = "";
    /** Discord webhook URL for block-event notifications (leave empty to disable). */
    public static volatile String discordWebhookUrl = "";

    // ── Group 06 / M1 — background remover ────────────────────────────────────

    /**
     * Background removal applied when a block is (re)textured: "none" (off, default),
     * "edges" (remove the edge-connected background), or "closed" (also remove enclosed
     * areas matching the background colour). Removed pixels become opaque black.
     */
    public static volatile String backgroundMode = "none";

    /**
     * Background-removal strength, 0-100 (mapped internally to a CIE-LAB ΔE distance). 0 = off.
     * Higher = more shades count as background. Set via /cb tolerance.
     */
    public static volatile int backgroundTolerance = 30;

    // ── Group 06 / M2+M3 — colour-variant hexes ────────────────────────────────

    /** Shipped defaults for the four colour hexes (item art is re-tinted only when changed). */
    public static final String TRIANGLE_RED_DEFAULT    = "#EE3333";
    public static final String TRIANGLE_YELLOW_DEFAULT = "#F0C814";
    public static final String TRIANGLE_GREEN_DEFAULT  = "#1E8C1E";
    public static final String TRIANGLE_BLACK_DEFAULT  = "#0A0A0A";

    /** Fill colour used when a Red Triangle recolours a block's background (#RRGGBB). */
    public static volatile String triangleRedHex = TRIANGLE_RED_DEFAULT;

    /** Fill colour used when a Yellow Triangle recolours a block's background (#RRGGBB). */
    public static volatile String triangleYellowHex = TRIANGLE_YELLOW_DEFAULT;

    /** Fill colour used when a Green Triangle recolours a block's background (#RRGGBB). */
    public static volatile String triangleGreenHex = TRIANGLE_GREEN_DEFAULT;

    /** Fill colour used when a Black Triangle recolours a block's background (#RRGGBB). */
    public static volatile String triangleBlackHex = TRIANGLE_BLACK_DEFAULT;

    // ── Group 07 — bulk operations ────────────────────────────────────────────

    /** A bulk operation affecting MORE than this many blocks asks for /cb confirm first. */
    public static volatile int bulkConfirmThreshold = 10;

    // ── Group 09 / Slice 3 — auto-backup ──────────────────────────────────────

    /** Minutes between automatic backups. 0 (or less) disables auto-backup. Default 30. */
    public static volatile int autoBackupInterval = 30;

    /** How many of the most recent auto-backups to keep; older "auto-…" ones are pruned. Default 10. */
    public static volatile int autoBackupKeepCount = 10;

    // ── Group 09 / Slice 4 — deleted-block trash ──────────────────────────────

    /** Days a deleted block stays in the trash before auto-pruning. 0 = keep forever. Pinned never prune. */
    public static volatile int trashRetentionDays = 30;

    // ── Group 11 — categories ─────────────────────────────────────────────────

    /** When true, /cb create suggests a category from the block name (a clickable hint, not auto-applied). */
    public static volatile boolean autoCategorizeEnabled = true;

    // ── Group 26 / Part C — named-texture mirror ──────────────────────────────

    /** When true, keep a human-readable, write-only copy of each texture under textures_names/ named by
     *  display name (never read back; slot_N.png stays canonical). Off by default. See TextureNameMirror. */
    public static volatile boolean mirrorNamedTextures = false;

    // ── Group 13 / Area 2 — Arabic Studio colour defaults ─────────────────────

    /** Default BACKGROUND colour the Arabic word Color Studio starts on (#RRGGBB). */
    public static volatile String arabicDefaultBgHex = "#0A0A0A";

    /** Default LETTER colour the Arabic word Color Studio starts on (#RRGGBB); outline stays black. */
    public static volatile String arabicDefaultLetterHex = "#FFFFFF";

    // ── Group 13 / O6 — live Arabic join form labels (mirror into ArabicLabels, synced to clients) ──
    public static volatile String arabicFormIni = "Ini";
    public static volatile String arabicFormMid = "Mid";
    public static volatile String arabicFormFin = "Fin";

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
            textureSize     = sanitizeTextureSize(getInt(root, "textureSize", textureSize)); // pow2, ≤256 (atlas mipmap safety)
            httpHost        = getString(root, "httpHost", httpHost);
            maxUndoDepth    = clamp(getInt(root, "maxUndoDepth", maxUndoDepth), 1, 1000);
            String m        = getString(root, "undoMode", undoMode);
            undoMode        = ("per_player".equals(m) || "global".equals(m)) ? m : "global";
            hudEnabled      = getBool(root, "hudEnabled", hudEnabled);
            silentPack      = getBool(root, "silentPack", silentPack);
            didYouMean      = normalizeDidYouMean(getString(root, "didYouMean", didYouMean));
            aiApiKey        = getString(root, "aiApiKey", aiApiKey);
            aiTextureEnabled = getBool(root, "aiTextureEnabled", aiTextureEnabled);
            aiTextureStyle  = getString(root, "aiTextureStyle", aiTextureStyle);
            vaultEndpoint   = getString(root, "vaultEndpoint", vaultEndpoint);
            discordWebhookUrl = getString(root, "discordWebhookUrl", discordWebhookUrl);
            backgroundMode  = BackgroundRemover.normalize(getString(root, "backgroundMode", backgroundMode));
            backgroundTolerance = clamp(getInt(root, "backgroundTolerance", backgroundTolerance), 0, 100);
            triangleRedHex    = normalizeHexColor(getString(root, "triangleRedHex",    triangleRedHex),    triangleRedHex);
            triangleYellowHex = normalizeHexColor(getString(root, "triangleYellowHex", triangleYellowHex), triangleYellowHex);
            triangleGreenHex  = normalizeHexColor(getString(root, "triangleGreenHex",  triangleGreenHex),  triangleGreenHex);
            triangleBlackHex  = normalizeHexColor(getString(root, "triangleBlackHex",  triangleBlackHex),  triangleBlackHex);
            bulkConfirmThreshold = clamp(getInt(root, "bulkConfirmThreshold", bulkConfirmThreshold), 1, 100000);
            autoBackupInterval  = clamp(getInt(root, "autoBackupInterval", autoBackupInterval), 0, 10080); // 0..1 week
            autoBackupKeepCount = clamp(getInt(root, "autoBackupKeepCount", autoBackupKeepCount), 0, 1000);
            trashRetentionDays  = clamp(getInt(root, "trashRetentionDays", trashRetentionDays), 0, 3650);
            autoCategorizeEnabled = getBool(root, "autoCategorizeEnabled", autoCategorizeEnabled);
            mirrorNamedTextures = getBool(root, "mirrorNamedTextures", mirrorNamedTextures);
            arabicDefaultBgHex     = normalizeHexColor(getString(root, "arabicDefaultBgHex",     arabicDefaultBgHex),     arabicDefaultBgHex);
            arabicDefaultLetterHex = normalizeHexColor(getString(root, "arabicDefaultLetterHex", arabicDefaultLetterHex), arabicDefaultLetterHex);
            arabicFormIni = getString(root, "arabicFormIni", arabicFormIni);
            arabicFormMid = getString(root, "arabicFormMid", arabicFormMid);
            arabicFormFin = getString(root, "arabicFormFin", arabicFormFin);
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
            root.addProperty("silentPack",         silentPack);
            root.addProperty("didYouMean",         didYouMean);
            root.addProperty("aiApiKey",           aiApiKey);
            root.addProperty("aiTextureEnabled",   aiTextureEnabled);
            root.addProperty("aiTextureStyle",     aiTextureStyle);
            root.addProperty("vaultEndpoint",      vaultEndpoint);
            root.addProperty("discordWebhookUrl",  discordWebhookUrl);
            root.addProperty("backgroundMode",     backgroundMode);
            root.addProperty("backgroundTolerance", backgroundTolerance);
            root.addProperty("triangleRedHex",     triangleRedHex);
            root.addProperty("triangleYellowHex",  triangleYellowHex);
            root.addProperty("triangleGreenHex",   triangleGreenHex);
            root.addProperty("triangleBlackHex",   triangleBlackHex);
            root.addProperty("bulkConfirmThreshold", bulkConfirmThreshold);
            root.addProperty("autoBackupInterval",  autoBackupInterval);
            root.addProperty("autoBackupKeepCount", autoBackupKeepCount);
            root.addProperty("trashRetentionDays",  trashRetentionDays);
            root.addProperty("autoCategorizeEnabled", autoCategorizeEnabled);
            root.addProperty("mirrorNamedTextures", mirrorNamedTextures);
            root.addProperty("arabicDefaultBgHex",     arabicDefaultBgHex);
            root.addProperty("arabicDefaultLetterHex", arabicDefaultLetterHex);
            root.addProperty("arabicFormIni", arabicFormIni);
            root.addProperty("arabicFormMid", arabicFormMid);
            root.addProperty("arabicFormFin", arabicFormFin);
            Path tmp = dir.resolve(CONFIG_FILE + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOGGER.error("[CustomBlocks] Failed to save config", e);
        }
    }

    /**
     * Validate a "#RRGGBB" hex colour (a missing "#" is tolerated and added). Returns the
     * canonical "#RRGGBB" upper-case form, or {@code fallback} when the value is unparseable.
     */
    public static String normalizeHexColor(String raw, String fallback) {
        if (raw == null) return fallback;
        String v = raw.trim();
        if (v.startsWith("#")) v = v.substring(1);
        if (!v.matches("[0-9a-fA-F]{6}")) return fallback;
        return "#" + v.toUpperCase(java.util.Locale.ROOT);
    }

    /** Clamp a didYouMean value to one of: smart / always / off (unknown → smart). */
    public static String normalizeDidYouMean(String raw) {
        if (raw == null) return "smart";
        String v = raw.trim().toLowerCase(java.util.Locale.ROOT);
        return ("always".equals(v) || "off".equals(v)) ? v : "smart";
    }

    private static int    getInt   (JsonObject o, String k, int    d) { return o.has(k) ? o.get(k).getAsInt()     : d; }
    private static String getString(JsonObject o, String k, String d) { return o.has(k) ? o.get(k).getAsString()  : d; }
    private static boolean getBool (JsonObject o, String k, boolean d){ return o.has(k) ? o.get(k).getAsBoolean() : d; }
    private static int    clamp    (int v, int min, int max)          { return Math.max(min, Math.min(max, v)); }
}
