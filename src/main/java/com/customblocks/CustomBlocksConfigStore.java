/**
 * CustomBlocksConfigStore.java
 *
 * Responsibility: Disk I/O for {@link CustomBlocksConfig} — read config/customblocks/config.json into the
 * config fields, and write them back atomically (temp file + ATOMIC_MOVE, NFR-13). Split out of
 * CustomBlocksConfig so that class stays under the 300-line config limit (§9.3), mirroring the
 * HudConfig / HudConfigStore split. CustomBlocksConfig.load()/save() delegate here, so callers are unchanged.
 *
 * Depends on: CustomBlocksConfig (the fields), BackgroundRemover (mode normalize), Gson
 * Called by:  CustomBlocksConfig.load() / .save() (delegators)
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

public final class CustomBlocksConfigStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("CustomBlocks");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "config/customblocks";
    private static final String CONFIG_FILE = "config.json";

    private CustomBlocksConfigStore() {} // static-only

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
            CustomBlocksConfig.maxSlots        = clamp(getInt(root, "maxSlots", CustomBlocksConfig.maxSlots), 1, 8192);
            CustomBlocksConfig.httpPort        = clamp(getInt(root, "httpPort", CustomBlocksConfig.httpPort), 1, 65535);
            CustomBlocksConfig.textureSize     = CustomBlocksConfig.sanitizeTextureSize(getInt(root, "textureSize", CustomBlocksConfig.textureSize)); // pow2, 16..512 (off-atlas full-res; ADR-008)
            CustomBlocksConfig.httpHost        = getString(root, "httpHost", CustomBlocksConfig.httpHost);
            CustomBlocksConfig.maxUndoDepth    = clamp(getInt(root, "maxUndoDepth", CustomBlocksConfig.maxUndoDepth), 1, 1000);
            String m        = getString(root, "undoMode", CustomBlocksConfig.undoMode);
            CustomBlocksConfig.undoMode        = ("per_player".equals(m) || "global".equals(m)) ? m : "global";
            CustomBlocksConfig.hudEnabled      = getBool(root, "hudEnabled", CustomBlocksConfig.hudEnabled);
            CustomBlocksConfig.silentPack      = getBool(root, "silentPack", CustomBlocksConfig.silentPack);
            CustomBlocksConfig.transparentBackground = getBool(root, "transparentBackground", CustomBlocksConfig.transparentBackground);
            CustomBlocksConfig.aiApiKey        = getString(root, "aiApiKey", CustomBlocksConfig.aiApiKey);
            CustomBlocksConfig.aiTextureEnabled = getBool(root, "aiTextureEnabled", CustomBlocksConfig.aiTextureEnabled);
            CustomBlocksConfig.aiTextureStyle  = getString(root, "aiTextureStyle", CustomBlocksConfig.aiTextureStyle);
            CustomBlocksConfig.vaultEndpoint   = getString(root, "vaultEndpoint", CustomBlocksConfig.vaultEndpoint);
            CustomBlocksConfig.discordWebhookUrl = getString(root, "discordWebhookUrl", CustomBlocksConfig.discordWebhookUrl);
            CustomBlocksConfig.cloudShareEnabled = getBool(root, "cloudShareEnabled", CustomBlocksConfig.cloudShareEnabled);
            CustomBlocksConfig.autoUpdateEnabled = getBool(root, "autoUpdateEnabled", CustomBlocksConfig.autoUpdateEnabled);
            CustomBlocksConfig.backgroundMode  = BackgroundRemover.normalize(getString(root, "backgroundMode", CustomBlocksConfig.backgroundMode));
            CustomBlocksConfig.backgroundTolerance = clamp(getInt(root, "backgroundTolerance", CustomBlocksConfig.backgroundTolerance), 0, 100);
            CustomBlocksConfig.triangleRedHex    = CustomBlocksConfig.normalizeHexColor(getString(root, "triangleRedHex",    CustomBlocksConfig.triangleRedHex),    CustomBlocksConfig.triangleRedHex);
            CustomBlocksConfig.triangleYellowHex = CustomBlocksConfig.normalizeHexColor(getString(root, "triangleYellowHex", CustomBlocksConfig.triangleYellowHex), CustomBlocksConfig.triangleYellowHex);
            CustomBlocksConfig.triangleGreenHex  = CustomBlocksConfig.normalizeHexColor(getString(root, "triangleGreenHex",  CustomBlocksConfig.triangleGreenHex),  CustomBlocksConfig.triangleGreenHex);
            CustomBlocksConfig.triangleBlackHex  = CustomBlocksConfig.normalizeHexColor(getString(root, "triangleBlackHex",  CustomBlocksConfig.triangleBlackHex),  CustomBlocksConfig.triangleBlackHex);
            CustomBlocksConfig.bulkConfirmThreshold = clamp(getInt(root, "bulkConfirmThreshold", CustomBlocksConfig.bulkConfirmThreshold), 1, 100000);
            CustomBlocksConfig.autoBackupInterval  = clamp(getInt(root, "autoBackupInterval", CustomBlocksConfig.autoBackupInterval), 0, 10080); // 0..1 week
            CustomBlocksConfig.autoBackupKeepCount = clamp(getInt(root, "autoBackupKeepCount", CustomBlocksConfig.autoBackupKeepCount), 0, 1000);
            CustomBlocksConfig.safetyKeepCount     = clamp(getInt(root, "safetyKeepCount", CustomBlocksConfig.safetyKeepCount), 1, 1000);
            CustomBlocksConfig.autoBackupTime      = sanitizeTime(getString(root, "autoBackupTime", CustomBlocksConfig.autoBackupTime));
            CustomBlocksConfig.autoBackupBudgetMB  = clamp(getInt(root, "autoBackupBudgetMB", CustomBlocksConfig.autoBackupBudgetMB), 0, 1_000_000);
            CustomBlocksConfig.trashRetentionDays  = clamp(getInt(root, "trashRetentionDays", CustomBlocksConfig.trashRetentionDays), 0, 3650);
            CustomBlocksConfig.autoCategorizeEnabled = getBool(root, "autoCategorizeEnabled", CustomBlocksConfig.autoCategorizeEnabled);
            CustomBlocksConfig.mirrorNamedTextures = getBool(root, "mirrorNamedTextures", CustomBlocksConfig.mirrorNamedTextures);
            CustomBlocksConfig.arabicDefaultBgHex     = CustomBlocksConfig.normalizeHexColor(getString(root, "arabicDefaultBgHex",     CustomBlocksConfig.arabicDefaultBgHex),     CustomBlocksConfig.arabicDefaultBgHex);
            CustomBlocksConfig.arabicDefaultLetterHex = CustomBlocksConfig.normalizeHexColor(getString(root, "arabicDefaultLetterHex", CustomBlocksConfig.arabicDefaultLetterHex), CustomBlocksConfig.arabicDefaultLetterHex);
            CustomBlocksConfig.arabicFormIni = getString(root, "arabicFormIni", CustomBlocksConfig.arabicFormIni);
            CustomBlocksConfig.arabicFormMid = getString(root, "arabicFormMid", CustomBlocksConfig.arabicFormMid);
            CustomBlocksConfig.arabicFormFin = getString(root, "arabicFormFin", CustomBlocksConfig.arabicFormFin);
            for (String c : CustomBlocksConfig.FX_CATEGORIES) {
                CustomBlocksConfig.particlesEnabled.put(c,
                        getBool(root, "particlesEnabled_" + c, CustomBlocksConfig.particlesOn(c)));
            }
            for (String c : CustomBlocksConfig.FX_CATEGORIES) {
                CustomBlocksConfig.soundsEnabled.put(c,
                        getBool(root, "soundsEnabled_" + c, CustomBlocksConfig.soundsOn(c)));
            }
            // Group 32 — Explosive Tomato blast. Power is clamped so a bad config file can't turn the tomato
            // into a world-eater; restore seconds is clamped to a sane window (0 = crater never restores).
            CustomBlocksConfig.tomatoBlastPower     = clampD(getDouble(root, "tomatoBlastPower", CustomBlocksConfig.tomatoBlastPower), 0.0, 32.0);
            CustomBlocksConfig.tomatoBlastKnockback = clampD(getDouble(root, "tomatoBlastKnockback", CustomBlocksConfig.tomatoBlastKnockback), 0.0, 64.0);
            CustomBlocksConfig.tomatoBlastFire      = getBool(root, "tomatoBlastFire", CustomBlocksConfig.tomatoBlastFire);
            CustomBlocksConfig.tomatoRestoreSeconds = (int) clampD(getDouble(root, "tomatoRestoreSeconds", CustomBlocksConfig.tomatoRestoreSeconds), 0, 600);
            CustomBlocksConfig.tomatoSauceDecaySeconds = (int) clampD(getDouble(root, "tomatoSauceDecaySeconds", CustomBlocksConfig.tomatoSauceDecaySeconds), 1, 600);
            CustomBlocksConfig.tomatoSauceCapPerChunk  = (int) clampD(getDouble(root, "tomatoSauceCapPerChunk", CustomBlocksConfig.tomatoSauceCapPerChunk), 1, 512);
            CustomBlocksConfig.timerDefaultScale = (float) clampD(getDouble(root, "timerDefaultScale", CustomBlocksConfig.timerDefaultScale), 0.1, 5.0);
            LOGGER.info("[CustomBlocks] Config loaded: maxSlots={}, httpPort={}, textureSize={}, hudEnabled={}",
                    CustomBlocksConfig.maxSlots, CustomBlocksConfig.httpPort, CustomBlocksConfig.textureSize, CustomBlocksConfig.hudEnabled);
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
            root.addProperty("maxSlots",          CustomBlocksConfig.maxSlots);
            root.addProperty("httpPort",           CustomBlocksConfig.httpPort);
            root.addProperty("textureSize",        CustomBlocksConfig.textureSize);
            root.addProperty("httpHost",           CustomBlocksConfig.httpHost);
            root.addProperty("maxUndoDepth",       CustomBlocksConfig.maxUndoDepth);
            root.addProperty("undoMode",           CustomBlocksConfig.undoMode);
            root.addProperty("hudEnabled",         CustomBlocksConfig.hudEnabled);
            root.addProperty("silentPack",         CustomBlocksConfig.silentPack);
            root.addProperty("transparentBackground", CustomBlocksConfig.transparentBackground);
            root.addProperty("aiApiKey",           CustomBlocksConfig.aiApiKey);
            root.addProperty("aiTextureEnabled",   CustomBlocksConfig.aiTextureEnabled);
            root.addProperty("aiTextureStyle",     CustomBlocksConfig.aiTextureStyle);
            root.addProperty("vaultEndpoint",      CustomBlocksConfig.vaultEndpoint);
            root.addProperty("discordWebhookUrl",  CustomBlocksConfig.discordWebhookUrl);
            root.addProperty("cloudShareEnabled",  CustomBlocksConfig.cloudShareEnabled);
            root.addProperty("autoUpdateEnabled",  CustomBlocksConfig.autoUpdateEnabled);
            root.addProperty("backgroundMode",     CustomBlocksConfig.backgroundMode);
            root.addProperty("backgroundTolerance", CustomBlocksConfig.backgroundTolerance);
            root.addProperty("triangleRedHex",     CustomBlocksConfig.triangleRedHex);
            root.addProperty("triangleYellowHex",  CustomBlocksConfig.triangleYellowHex);
            root.addProperty("triangleGreenHex",   CustomBlocksConfig.triangleGreenHex);
            root.addProperty("triangleBlackHex",   CustomBlocksConfig.triangleBlackHex);
            root.addProperty("bulkConfirmThreshold", CustomBlocksConfig.bulkConfirmThreshold);
            root.addProperty("autoBackupInterval",  CustomBlocksConfig.autoBackupInterval);
            root.addProperty("autoBackupKeepCount", CustomBlocksConfig.autoBackupKeepCount);
            root.addProperty("safetyKeepCount",     CustomBlocksConfig.safetyKeepCount);
            root.addProperty("autoBackupTime",      CustomBlocksConfig.autoBackupTime);
            root.addProperty("autoBackupBudgetMB",  CustomBlocksConfig.autoBackupBudgetMB);
            root.addProperty("trashRetentionDays",  CustomBlocksConfig.trashRetentionDays);
            root.addProperty("autoCategorizeEnabled", CustomBlocksConfig.autoCategorizeEnabled);
            root.addProperty("mirrorNamedTextures", CustomBlocksConfig.mirrorNamedTextures);
            root.addProperty("arabicDefaultBgHex",     CustomBlocksConfig.arabicDefaultBgHex);
            root.addProperty("arabicDefaultLetterHex", CustomBlocksConfig.arabicDefaultLetterHex);
            root.addProperty("arabicFormIni", CustomBlocksConfig.arabicFormIni);
            root.addProperty("arabicFormMid", CustomBlocksConfig.arabicFormMid);
            root.addProperty("arabicFormFin", CustomBlocksConfig.arabicFormFin);
            root.addProperty("tomatoBlastPower",     CustomBlocksConfig.tomatoBlastPower);
            root.addProperty("tomatoBlastKnockback", CustomBlocksConfig.tomatoBlastKnockback);
            root.addProperty("tomatoBlastFire",      CustomBlocksConfig.tomatoBlastFire);
            root.addProperty("tomatoRestoreSeconds", CustomBlocksConfig.tomatoRestoreSeconds);
            root.addProperty("tomatoSauceDecaySeconds", CustomBlocksConfig.tomatoSauceDecaySeconds);
            root.addProperty("tomatoSauceCapPerChunk",  CustomBlocksConfig.tomatoSauceCapPerChunk);
            root.addProperty("timerDefaultScale", CustomBlocksConfig.timerDefaultScale);
            for (String c : CustomBlocksConfig.FX_CATEGORIES) {
                root.addProperty("particlesEnabled_" + c, CustomBlocksConfig.particlesOn(c));
            }
            for (String c : CustomBlocksConfig.FX_CATEGORIES) {
                root.addProperty("soundsEnabled_" + c, CustomBlocksConfig.soundsOn(c));
            }
            Path tmp = dir.resolve(CONFIG_FILE + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOGGER.error("[CustomBlocks] Failed to save config", e);
        }
    }

    private static int     getInt   (JsonObject o, String k, int     d) { return o.has(k) ? o.get(k).getAsInt()     : d; }
    private static String  getString(JsonObject o, String k, String  d) { return o.has(k) ? o.get(k).getAsString()  : d; }
    private static boolean getBool  (JsonObject o, String k, boolean d) { return o.has(k) ? o.get(k).getAsBoolean() : d; }
    private static double  getDouble(JsonObject o, String k, double  d) { return o.has(k) ? o.get(k).getAsDouble()  : d; }
    private static int     clamp    (int v, int min, int max)           { return Math.max(min, Math.min(max, v)); }
    /** Accept a valid "HH:mm" (00:00–23:59); anything else (incl. blank) becomes "" = interval mode. */
    private static String  sanitizeTime(String s) {
        if (s == null) return "";
        s = s.trim();
        if (!s.matches("([01]\\d|2[0-3]):[0-5]\\d")) return "";
        return s;
    }
    private static double  clampD   (double v, double min, double max)  { return Math.max(min, Math.min(max, v)); }
}
