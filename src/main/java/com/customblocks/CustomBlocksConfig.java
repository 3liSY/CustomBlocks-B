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

public final class CustomBlocksConfig {

    // ── Phase 1 ──────────────────────────────────────────────────────────────

    /** Number of slot blocks to pre-register at startup. Requires a restart to change.
     *  G13-25 raised the default 800 → 1448: indices 800..1447 are the dedicated Arabic pool
     *  (see core/SlotPools). Boot enforces 1448 as a raise-only floor for older config files. */
    public static volatile int maxSlots = 1448;

    /** Port for the embedded resource-pack HTTP server. */
    public static volatile int httpPort = 8123;

    /** Block texture size (px), a power of two 16..{@link #MAX_TEXTURE_SIZE}. Set via /cb config texturesize.
     *  Default 512: every block now renders OFF the atlas (ADR-008 own-texture renderer), so full 512px is
     *  carried crisp — the old 256 default was an atlas-era cap and made blocks soft up close. */
    public static volatile int textureSize = 512;

    /** Hard ceiling for {@link #textureSize}. The own-texture renderer (ADR-008) carries full resolution with
     *  no atlas, so 512px renders crisp; only the legacy atlas paths (ADR-007) softened above 256px. */
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

    // ── Group 14 / Phase 1c — off-atlas background ───────────────────────────

    /** Off-atlas blocks: when false (default) transparent/letterbox pixels render solid BLACK; when true
     *  they stay see-through. Client-side visual; pushed to clients on join + on /cb config transparent. */
    public static volatile boolean transparentBackground = false;

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

    // ── Group 20 / S1 — cloud block share ────────────────────────────────────
    /** Master switch for cloud block sharing (D9). When false, every /cb vault command refuses
     *  gracefully and nothing hits the network. Default on (D11 — owner ships their vault enabled). */
    public static volatile boolean cloudShareEnabled = true;

    // ── Group 20 / §K — auto-update (AU6) ─────────────────────────────────────
    /** Server-side master switch for client auto-update (§CS3 AU6). When true (default), a client
     *  joining with an older jar is offered the auto-download+swap flow; when false, a version
     *  mismatch only shows a warning toast — no download. Pushed to the client on join inside the
     *  version-handshake packet, so the client honours THIS server's choice. */
    public static volatile boolean autoUpdateEnabled = true;

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

    /**
     * A bulk operation affecting MORE than this many blocks asks for /cb confirm first (chat path) or is
     * gated by the Hub's Deploy modal. Default is 2 (§G07-4, locked 2026-07-11): almost every batch confirms,
     * so a stray Delete/Re-ID over more than one block can never run un-gated.
     */
    public static volatile int bulkConfirmThreshold = 2;

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

    // ── Group 30 — Guess Mode ─────────────────────────────────────────────────
    // (v2 redesign: the blank name is hardcoded "???" and the disguise look the bundled "?" — no config.)

    // ── Group 16 / Slice 4 — per-category particle effects ───────────────────
    /** The FX category keys, shared by the particle toggles (slice 4) and the sound toggles (slice 5). */
    public static final String[] FX_CATEGORIES = {
        "success", "error", "gui", "selection", "bulk_complete", "achievement"
    };

    /** Per-category particle-effect enable flags (default all on). Toggle via /cb particles <cat> on|off. */
    public static final java.util.Map<String, Boolean> particlesEnabled = new java.util.concurrent.ConcurrentHashMap<>();
    static {
        for (String c : FX_CATEGORIES) particlesEnabled.put(c, true);
    }

    /** True if particle FX for {@code category} are enabled (unknown category → false). */
    public static boolean particlesOn(String category) {
        return category != null && Boolean.TRUE.equals(particlesEnabled.get(category));
    }

    // ── Group 16 / Slice 5 — per-category event sounds (merged with particles) ──
    /** Per-category event-sound enable flags (default all on). Toggle via /cb sounds <cat> on|off. */
    public static final java.util.Map<String, Boolean> soundsEnabled = new java.util.concurrent.ConcurrentHashMap<>();
    static {
        for (String c : FX_CATEGORIES) soundsEnabled.put(c, true);
    }

    /** True if the event sound for {@code category} is enabled (unknown category → false). */
    public static boolean soundsOn(String category) {
        return category != null && Boolean.TRUE.equals(soundsEnabled.get(category));
    }

    /** Whether {@code c} is one of {@link #FX_CATEGORIES}. */
    public static boolean isFxCategory(String c) {
        if (c == null) return false;
        for (String k : FX_CATEGORIES) if (k.equals(c)) return true;
        return false;
    }

    // ── Group 32 — Explosive Tomato blast (owner-locked 2026-07-15) ────────────
    //
    // A REAL vanilla explosion drives crater + damage + knockback off ONE power, exactly like TNT: point-blank
    // with no armour is LETHAL, full iron survives (the explosion runs damage through armour for free), and the
    // crater is destruction-ON but restores after tomatoRestoreSeconds (anti-grief). A tomato named "nuke"
    // overrides the power to TomatoEntity.NUKE_POWER. The old decoupled radius/damage keys are gone — one
    // power owns all three, which is why the hand-rolled falloff loop was dropped.

    /** Explosion power. TNT is 4.0; the tomato is hotter at 6.0 (a "nuke"-named tomato jumps to 12.0). */
    public static volatile double tomatoBlastPower = 6.0;

    /**
     * Knockback STRENGTH as a tunable float (owner-locked 8.0, 2026-07-17 — was a boolean on/off). The blast
     * still throws entities off a real vanilla explosion; this rescales that imparted push. 6.0 = the normal
     * blast power = an unscaled "vanilla" TNT-style shove, so 8.0 is punchier than a same-power vanilla blast;
     * 0.0 cancels knockback entirely. Clamped 0–64 on load.
     */
    public static volatile double tomatoBlastKnockback = 8.0;

    /** Whether the blast sets fire. Off: the tomato leaves a crater and a bad mood, not a wildfire. */
    public static volatile boolean tomatoBlastFire = false;

    /** How long (seconds) a crater waits before it restores to the original terrain (in-memory anti-grief). */
    public static volatile int tomatoRestoreSeconds = 5;

    /**
     * How long (seconds) a sauce puddle lasts before it dries out and fades (Phase D, config-tunable). Default
     * dropped 15 → 5 (owner-locked 2026-07-17) so the ENTIRE splash field dries in the same window the crater
     * restores — a puddle can no longer outlive the block it is sitting on and block that block's restore.
     */
    public static volatile int tomatoSauceDecaySeconds = 5;

    /** Max sauce puddles kept per chunk; past this the OLDEST is evicted first (Phase D, strict FIFO). */
    public static volatile int tomatoSauceCapPerChunk = 50;

    // ── Group 31 — BuzzerGame timer stand ─────────────────────────────────────
    /** Spawn size a freshly placed timer stand starts at (×). Shipped ×1.3 (2026-07-19 lock); the owner
     *  changes it in-game with {@code /cb buzzergame size <n> setdefault}. Clamped 0.1–5.0 on load. */
    public static volatile float timerDefaultScale = 1.3f;

    private CustomBlocksConfig() {} // static-only

    /** Load config from disk (delegates to {@link CustomBlocksConfigStore}). */
    public static void load() { CustomBlocksConfigStore.load(); }

    /** Save config to disk (delegates to {@link CustomBlocksConfigStore}). */
    public static void save() { CustomBlocksConfigStore.save(); }

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
}
