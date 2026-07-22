/**
 * ConfigRegistry.java
 *
 * Responsibility: The single source of truth for the Settings Book (Group 21). Lists EVERY server
 * setting once as a {@link ConfigField} -- value fields (bool/enum/int/string/hex), sub-chest openers
 * (group), action buttons (e.g. Edit HUD), and Coming-Soon placeholders for features with no backend
 * yet. The GUI, search, presets, reset and modified-markers all read this list, so no consumer ever
 * special-cases an individual setting (spec section 2).
 *
 * Human keys (D5) live here; they map to the existing CustomBlocksConfig static fields via the
 * getter/setter lambdas. This class adds NO behaviour -- it only describes what already exists, so it
 * is safe to land before any GUI or disk-key migration.
 *
 * Depends on: ConfigField, CustomBlocksConfig (the live fields).
 * Called by:  the Settings Book GUI + editors + search + presets (later phases).
 */
package com.customblocks.config;

import com.customblocks.CustomBlocksConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfigRegistry {

    private ConfigRegistry() {} // static-only

    /** Drawer ids -- one per category header in the book. Stable strings, used as ConfigField.category. */
    public static final class Cat {
        public static final String GENERAL     = "general";
        public static final String LOOK        = "look";
        public static final String VARIANT     = "variant";     // sub-chest of LOOK
        public static final String EFFECTS     = "effects";
        public static final String HUD         = "hud";
        public static final String NETWORK     = "network";
        public static final String BACKUP      = "backup";
        public static final String AI          = "ai";
        public static final String DISCORD     = "discord";
        public static final String ARABIC      = "arabic";
        public static final String ADVANCED    = "advanced";
        public static final String TOOLS       = "tools";
        public static final String PERMISSIONS = "permissions";
        private Cat() {}
    }

    private static final List<ConfigField> ALL = build();

    /** Every setting, in book order (unmodifiable). */
    public static List<ConfigField> all() { return ALL; }

    /** The one field with this human key, or null if none. */
    public static ConfigField byKey(String key) {
        if (key == null) return null;
        for (ConfigField f : ALL) if (f.key.equals(key)) return f;
        return null;
    }

    /** Every field in a drawer, in order. */
    public static List<ConfigField> inCategory(String cat) {
        List<ConfigField> out = new ArrayList<>();
        for (ConfigField f : ALL) if (f.category.equals(cat)) out.add(f);
        return out;
    }

    /** Every field on a page (1 or 2), in order. */
    public static List<ConfigField> onPage(int page) {
        List<ConfigField> out = new ArrayList<>();
        for (ConfigField f : ALL) if (f.page == page) out.add(f);
        return out;
    }

    /** Only fields that hold a readable value (excludes action/group/coming-soon). */
    public static List<ConfigField> live() {
        List<ConfigField> out = new ArrayList<>();
        for (ConfigField f : ALL) if (f.hasValue()) out.add(f);
        return out;
    }

    // ── the list ────────────────────────────────────────────────────────────────
    private static List<ConfigField> build() {
        List<ConfigField> f = new ArrayList<>();

        // ===== PAGE 1 -- Everyday =================================================

        // General
        f.add(ConfigField.intField("max_blocks", "Max blocks",
                "How many custom-block slots the server makes at startup. Higher uses more memory. Needs a restart.",
                Cat.GENERAL, 1,
                () -> CustomBlocksConfig.maxSlots, v -> CustomBlocksConfig.maxSlots = v,
                // G13-25: default 1448 (800 normal + 648 Arabic pool). Boot re-raises lower values.
                com.customblocks.core.SlotPools.REQUIRED_MAX_SLOTS, 1, 8192, 50, true, true));
        f.add(ConfigField.enumField("texture_quality", "Texture quality",
                "Block texture resolution in pixels. Bigger looks sharper but costs more memory.",
                Cat.GENERAL, 1,
                () -> String.valueOf(CustomBlocksConfig.textureSize),
                v -> { try { CustomBlocksConfig.textureSize = CustomBlocksConfig.sanitizeTextureSize(Integer.parseInt(v.trim())); } catch (Exception ignored) {} },
                "256", new String[]{"16", "32", "64", "128", "256", "512"}, false, false));
        f.add(ConfigField.bool("silent_pack", "Silent pack",
                "Apply the texture pack without showing players the 'download pack?' dialog.",
                Cat.GENERAL, 1,
                () -> CustomBlocksConfig.silentPack, v -> CustomBlocksConfig.silentPack = v,
                true, false, false));
        f.add(ConfigField.bool("auto_category", "Auto category",
                "When creating a block, suggest a category from its name (a clickable hint, never forced).",
                Cat.GENERAL, 1,
                () -> CustomBlocksConfig.autoCategorizeEnabled, v -> CustomBlocksConfig.autoCategorizeEnabled = v,
                true, false, false));

        // Look & Textures
        f.add(ConfigField.bool("transparent_background", "Transparent background",
                "Off-atlas blocks: keep see-through pixels see-through (on) or fill them solid black (off).",
                Cat.LOOK, 1,
                () -> CustomBlocksConfig.transparentBackground, v -> CustomBlocksConfig.transparentBackground = v,
                false, false, false));
        f.add(ConfigField.enumField("background_removal", "Background removal",
                "Strip a block image's background when it is applied: off, edges (outer background) or closed (also enclosed areas).",
                Cat.LOOK, 1,
                () -> CustomBlocksConfig.backgroundMode, v -> CustomBlocksConfig.backgroundMode = v,
                "none", new String[]{"none", "edges", "closed"}, false, false));
        f.add(ConfigField.intField("background_strength", "Background strength",
                "How aggressively background removal matches shades (0 = off, 100 = most). Only used when removal is on.",
                Cat.LOOK, 1,
                () -> CustomBlocksConfig.backgroundTolerance, v -> CustomBlocksConfig.backgroundTolerance = v,
                30, 0, 100, 5, false, false));
        f.add(ConfigField.group("variant_colours", "Variant colours",
                "The four triangle recolour swatches (red, yellow, green, black). Opens a colour chest.",
                Cat.LOOK, 1));
        f.add(ConfigField.bool("named_texture_mirror", "Named texture mirror",
                "Also save a human-named copy of each texture under textures_names/ (write-only; the slot file stays canonical).",
                Cat.LOOK, 1,
                () -> CustomBlocksConfig.mirrorNamedTextures, v -> CustomBlocksConfig.mirrorNamedTextures = v,
                false, false, false));

        // Variant colours -- sub-chest of Look
        f.add(ConfigField.hex("variant_red", "Red variant",
                "Fill colour a Red Triangle paints onto a block's background.",
                Cat.VARIANT, 1,
                () -> CustomBlocksConfig.triangleRedHex,
                v -> CustomBlocksConfig.triangleRedHex = CustomBlocksConfig.normalizeHexColor(v, CustomBlocksConfig.triangleRedHex),
                CustomBlocksConfig.TRIANGLE_RED_DEFAULT));
        f.add(ConfigField.hex("variant_yellow", "Yellow variant",
                "Fill colour a Yellow Triangle paints onto a block's background.",
                Cat.VARIANT, 1,
                () -> CustomBlocksConfig.triangleYellowHex,
                v -> CustomBlocksConfig.triangleYellowHex = CustomBlocksConfig.normalizeHexColor(v, CustomBlocksConfig.triangleYellowHex),
                CustomBlocksConfig.TRIANGLE_YELLOW_DEFAULT));
        f.add(ConfigField.hex("variant_green", "Green variant",
                "Fill colour a Green Triangle paints onto a block's background.",
                Cat.VARIANT, 1,
                () -> CustomBlocksConfig.triangleGreenHex,
                v -> CustomBlocksConfig.triangleGreenHex = CustomBlocksConfig.normalizeHexColor(v, CustomBlocksConfig.triangleGreenHex),
                CustomBlocksConfig.TRIANGLE_GREEN_DEFAULT));
        f.add(ConfigField.hex("variant_black", "Black variant",
                "Fill colour a Black Triangle paints onto a block's background.",
                Cat.VARIANT, 1,
                () -> CustomBlocksConfig.triangleBlackHex,
                v -> CustomBlocksConfig.triangleBlackHex = CustomBlocksConfig.normalizeHexColor(v, CustomBlocksConfig.triangleBlackHex),
                CustomBlocksConfig.TRIANGLE_BLACK_DEFAULT));

        // Edit HUD -- action button (D3: hudEnabled removed; on/off + layout live in HudEditorScreen)
        f.add(ConfigField.action("edit_hud", "Edit HUD",
                "Open the on-screen HUD editor to turn the block-info overlay on or off and move it around.",
                Cat.HUD, 1));

        // Effects -- sub-chest opener + per-category particle/sound toggles
        f.add(ConfigField.group("effects", "Effects",
                "Particle and sound feedback per event. Opens a chest: left-click a row toggles particles, right-click toggles sound.",
                Cat.EFFECTS, 1));
        for (String cat : CustomBlocksConfig.FX_CATEGORIES) {
            final String c = cat;
            String label = fxLabel(c);
            f.add(ConfigField.bool("particles_" + c, label + " particles",
                    "Show particle effects for " + label.toLowerCase() + " events.",
                    Cat.EFFECTS, 1,
                    () -> CustomBlocksConfig.particlesOn(c), v -> CustomBlocksConfig.particlesEnabled.put(c, v),
                    true, false, false));
            f.add(ConfigField.bool("sounds_" + c, label + " sound",
                    "Play a sound for " + label.toLowerCase() + " events.",
                    Cat.EFFECTS, 1,
                    () -> CustomBlocksConfig.soundsOn(c), v -> CustomBlocksConfig.soundsEnabled.put(c, v),
                    true, false, false));
        }

        // ===== PAGE 2 -- Advanced & Connections ===================================

        // Network
        f.add(ConfigField.intField("resource_pack_port", "Resource-pack port",
                "Network port the texture-pack web server listens on. Change only if the port is taken. Needs a restart.",
                Cat.NETWORK, 2,
                () -> CustomBlocksConfig.httpPort, v -> CustomBlocksConfig.httpPort = v,
                8123, 1, 65535, 1, true, true));
        f.add(ConfigField.string("server_ip", "Server IP",
                "Address players' clients use to fetch textures. 127.0.0.1 for a local test; your server's IP for a real server.",
                Cat.NETWORK, 2,
                () -> CustomBlocksConfig.httpHost, v -> CustomBlocksConfig.httpHost = v.trim(),
                "127.0.0.1", false));
        f.add(ConfigField.bool("cloud_sharing", "Cloud sharing",
                "Master switch for sharing blocks to your cloud vault. Off means no /cb vault command touches the network.",
                Cat.NETWORK, 2,
                () -> CustomBlocksConfig.cloudShareEnabled, v -> CustomBlocksConfig.cloudShareEnabled = v,
                true, false, false));
        f.add(ConfigField.string("cloud_url", "Cloud URL",
                "Web address of your cloud Block Vault. Leave empty to disable cloud sharing.",
                Cat.NETWORK, 2,
                () -> CustomBlocksConfig.vaultEndpoint, v -> CustomBlocksConfig.vaultEndpoint = v.trim(),
                "", false));
        f.add(ConfigField.comingSoon("cloud_secret", "Cloud secret",
                "A private key for your cloud vault. Not available yet.",
                Cat.NETWORK, 2));
        f.add(ConfigField.action("cloud_test", "Test cloud connection",
                "Check that the cloud URL responds.",
                Cat.NETWORK, 2));

        // Backup & History
        f.add(ConfigField.enumField("auto_backup_interval", "Auto-backup interval",
                "Minutes between automatic backups (off disables it).",
                Cat.BACKUP, 2,
                () -> String.valueOf(CustomBlocksConfig.autoBackupInterval),
                v -> { try { CustomBlocksConfig.autoBackupInterval = Integer.parseInt(v.trim()); } catch (Exception ignored) {} },
                "30", new String[]{"0", "5", "15", "30", "60", "120", "360"}, false, false));
        f.add(ConfigField.intField("auto_backup_keep", "Auto-backups kept",
                "How many of the newest automatic backups to keep before deleting older ones.",
                Cat.BACKUP, 2,
                () -> CustomBlocksConfig.autoBackupKeepCount, v -> CustomBlocksConfig.autoBackupKeepCount = v,
                10, 0, 1000, 1, false, false));
        f.add(ConfigField.intField("trash_retention_days", "Trash retention (days)",
                "Days a deleted block stays in the trash before it is purged (0 = keep forever; pinned blocks never purge).",
                Cat.BACKUP, 2,
                () -> CustomBlocksConfig.trashRetentionDays, v -> CustomBlocksConfig.trashRetentionDays = v,
                30, 0, 3650, 1, false, false));
        f.add(ConfigField.intField("undo_depth", "Undo depth",
                "How many undo/redo steps are remembered (in memory only).",
                Cat.BACKUP, 2,
                () -> CustomBlocksConfig.maxUndoDepth, v -> CustomBlocksConfig.maxUndoDepth = v,
                25, 1, 1000, 1, false, false));
        f.add(ConfigField.enumField("history_mode", "History mode",
                "Undo history is either shared server-wide or kept separately per player.",
                Cat.BACKUP, 2,
                () -> CustomBlocksConfig.undoMode,
                v -> CustomBlocksConfig.undoMode = ("per_player".equals(v) || "global".equals(v)) ? v : "global",
                "global", new String[]{"global", "per_player"}, false, false));

        // AI -- sub-chest
        f.add(ConfigField.group("ai", "AI textures",
                "Settings for AI-generated textures (keyless). Opens a chest.",
                Cat.AI, 2));
        f.add(ConfigField.string("ai_texture_style", "AI texture style",
                "Words added to every AI image request so results look like block textures (e.g. pixel_art).",
                Cat.AI, 2,
                () -> CustomBlocksConfig.aiTextureStyle, v -> CustomBlocksConfig.aiTextureStyle = v.trim(),
                "pixel_art", false));
        f.add(ConfigField.bool("ai_textures_enabled", "AI textures enabled",
                "Allow generating block textures from AI image prompts.",
                Cat.AI, 2,
                () -> CustomBlocksConfig.aiTextureEnabled, v -> CustomBlocksConfig.aiTextureEnabled = v,
                false, false, false));
        f.add(ConfigField.comingSoon("ai_variations", "AI variations",
                "Generate several texture options to pick from. Not available yet.",
                Cat.AI, 2));
        f.add(ConfigField.comingSoon("ai_provider", "AI provider",
                "Choose which AI image service to use. Not available yet.",
                Cat.AI, 2));

        // Discord -- sub-chest
        f.add(ConfigField.group("discord", "Discord",
                "Send block events to a Discord channel. Opens a chest.",
                Cat.DISCORD, 2));
        f.add(ConfigField.string("discord_webhook", "Discord webhook",
                "Discord webhook URL that block-event messages are posted to. Leave empty to disable.",
                Cat.DISCORD, 2,
                () -> CustomBlocksConfig.discordWebhookUrl, v -> CustomBlocksConfig.discordWebhookUrl = v.trim(),
                "", false));
        f.add(ConfigField.action("discord_test", "Send test message",
                "Post a test message to the Discord webhook to confirm it works.",
                Cat.DISCORD, 2));
        f.add(ConfigField.comingSoon("discord_per_event", "Per-event notifications",
                "Choose which events get posted to Discord. Not available yet.",
                Cat.DISCORD, 2));

        // Arabic -- sub-chest
        f.add(ConfigField.group("arabic", "Arabic studio",
                "Default colours and join labels for the Arabic word studio. Opens a chest.",
                Cat.ARABIC, 2));
        f.add(ConfigField.hex("arabic_default_bg", "Default background",
                "Background colour the Arabic word Color Studio starts on.",
                Cat.ARABIC, 2,
                () -> CustomBlocksConfig.arabicDefaultBgHex,
                v -> CustomBlocksConfig.arabicDefaultBgHex = CustomBlocksConfig.normalizeHexColor(v, CustomBlocksConfig.arabicDefaultBgHex),
                "#0A0A0A"));
        f.add(ConfigField.hex("arabic_default_letter", "Default letter colour",
                "Letter colour the Arabic word Color Studio starts on (the outline stays black).",
                Cat.ARABIC, 2,
                () -> CustomBlocksConfig.arabicDefaultLetterHex,
                v -> CustomBlocksConfig.arabicDefaultLetterHex = CustomBlocksConfig.normalizeHexColor(v, CustomBlocksConfig.arabicDefaultLetterHex),
                "#FFFFFF"));
        f.add(ConfigField.string("arabic_form_ini", "Join label: initial",
                "Short label shown for the initial joining form of an Arabic letter.",
                Cat.ARABIC, 2,
                () -> CustomBlocksConfig.arabicFormIni, v -> CustomBlocksConfig.arabicFormIni = v.trim(),
                "Ini", false));
        f.add(ConfigField.string("arabic_form_mid", "Join label: medial",
                "Short label shown for the medial joining form of an Arabic letter.",
                Cat.ARABIC, 2,
                () -> CustomBlocksConfig.arabicFormMid, v -> CustomBlocksConfig.arabicFormMid = v.trim(),
                "Mid", false));
        f.add(ConfigField.string("arabic_form_fin", "Join label: final",
                "Short label shown for the final joining form of an Arabic letter.",
                Cat.ARABIC, 2,
                () -> CustomBlocksConfig.arabicFormFin, v -> CustomBlocksConfig.arabicFormFin = v.trim(),
                "Fin", false));

        // Advanced -- sub-chest
        f.add(ConfigField.group("advanced", "Advanced",
                "Power-user thresholds. Opens a chest.",
                Cat.ADVANCED, 2));
        f.add(ConfigField.intField("bulk_confirm_threshold", "Bulk confirm threshold",
                "A bulk operation touching more than this many blocks asks for /cb confirm first.",
                Cat.ADVANCED, 2,
                () -> CustomBlocksConfig.bulkConfirmThreshold, v -> CustomBlocksConfig.bulkConfirmThreshold = v,
                2, 1, 100000, 1, false, false));
        f.add(ConfigField.comingSoon("payloads_per_tick", "Texture payloads / tick",
                "How many texture chunks to send each server tick. Not available yet.",
                Cat.ADVANCED, 2));

        // Tools / Permissions -- Coming Soon drawers (D4, D9)
        f.add(ConfigField.comingSoon("tools", "Tools",
                "Sorting and offhand display tools. Coming soon.",
                Cat.TOOLS, 2));
        f.add(ConfigField.comingSoon("permissions", "Permissions",
                "Three-tier use / edit / admin permissions. Coming with Group 22.",
                Cat.PERMISSIONS, 2));

        return Collections.unmodifiableList(f);
    }

    /** Pretty label for an FX category key (e.g. "bulk_complete" -> "Bulk complete"). */
    private static String fxLabel(String key) {
        switch (key) {
            case "success":       return "Success";
            case "error":         return "Error";
            case "gui":           return "Menus";
            case "selection":     return "Selection";
            case "bulk_complete": return "Bulk complete";
            case "achievement":   return "Achievement";
            default:              return key;
        }
    }
}
