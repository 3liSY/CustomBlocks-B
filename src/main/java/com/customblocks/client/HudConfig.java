/**
 * HudConfig.java — GROUP 27 §G27.4 (Lego HUD Builder). CLIENT-SIDE ONLY.
 *
 * Responsibility: live client state for the brick-based HUD — box-level globals (master
 * visible, snap on/off, master scale, global default background, recent colours, hover-sound
 * settings) plus the ordered list of HudField "bricks". Disk I/O + migration of the old flat
 * format live in HudConfigStore (keeps this file under the 300-line *Config gate); per-brick
 * (de)serialisation lives in HudField.
 *
 * Scope note: although the file name ends in "-server", the HUD is a client render concern.
 * In single-player / integrated-server the run directory is shared, so the path matches.
 *
 * Depends on: CustomBlocksConfig (initial enabled default), HudField, HudFieldType.
 * Called by: HudRenderer (reads), HudEditorScreen + inspector (edit + save), HudConfigStore
 *            (load/save), CustomBlocksClient (load on init), HudStatePayload receiver (toggle).
 */
package com.customblocks.client;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.client.hud.HudField;
import com.customblocks.client.hud.HudFieldType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class HudConfig {

    private HudConfig() {}

    // ── Defaults ─────────────────────────────────────────────────────────────
    public static final boolean DEF_VISIBLE      = true;
    public static final boolean DEF_SNAP         = true;
    public static final int     DEF_BG_COLOR     = 0x000000;
    public static final float   DEF_BG_OPACITY   = 0.4f;
    public static final float   DEF_MASTER_SCALE = 1.0f;
    public static final int     DEF_HOVER_TRIGGER= 0;        // 0 = none, 1 = custom only, 2 = any
    public static final String  DEF_HOVER_SOUND  = "none";
    public static final int     DEF_HOVER_VOLUME = 70;       // 0..100
    public static final int     RECENT_MAX       = 10;

    // ── Box-level globals ────────────────────────────────────────────────────
    /** Master HUD on/off (mirrors CustomBlocksConfig.hudEnabled / /cb config hud). */
    public static boolean visible      = DEF_VISIBLE;
    /** Magnetic snap enabled in the editor. */
    public static boolean snapEnabled  = DEF_SNAP;
    /** Global default background colour (0xRRGGBB) behind un-overridden bricks. */
    public static int     bgColor      = DEF_BG_COLOR;
    /** Global default background opacity (0..1; 0 = none). */
    public static float   bgOpacity    = DEF_BG_OPACITY;
    /** Master scale multiplier applied on top of each brick's own size. */
    public static float   masterScale  = DEF_MASTER_SCALE;
    /** Hover-sound trigger (0 none / 1 custom only / 2 any block). */
    public static int     hoverTrigger = DEF_HOVER_TRIGGER;
    /** Hover-sound key (see HudHoverSound). */
    public static String  hoverSound   = DEF_HOVER_SOUND;
    /** Hover-sound volume (0..100). */
    public static int     hoverVolume  = DEF_HOVER_VOLUME;
    /** Recently picked colours (most-recent first, capped at RECENT_MAX). */
    public static final List<Integer>  recentColors = new ArrayList<>();
    /** Ordered brick list; index 0 draws first (lowest z-order). */
    public static final List<HudField> fields       = new ArrayList<>(defaultFields());

    // ── Default fresh layout (spec: Name larger on top, ID smaller beneath, top-left) ─
    public static List<HudField> defaultFields() {
        List<HudField> list = new ArrayList<>();
        HudField name = new HudField(HudFieldType.DISPLAY_NAME, 6, 6, HudField.Anchor.TL, 1.4f);
        name.bold = true;
        list.add(name);
        HudField id = new HudField(HudFieldType.BLOCK_ID, 6, 26, HudField.Anchor.TL, 0.8f);
        id.color = 0xAAAAAA;
        list.add(id);
        return list;
    }

    /** [Reset] — restore the default brick list + background globals (does not persist). */
    public static void resetDefaults() {
        snapEnabled = DEF_SNAP;
        bgColor     = DEF_BG_COLOR;
        bgOpacity   = DEF_BG_OPACITY;
        masterScale = DEF_MASTER_SCALE;
        setFields(defaultFields());
    }

    /** Replace the brick list with deep copies of the given fields. */
    public static void setFields(List<HudField> newFields) {
        fields.clear();
        for (HudField f : newFields) fields.add(f.copy());
    }

    /** Deep-copied snapshot of the current brick list (for undo / preview). */
    public static List<HudField> snapshotFields() {
        List<HudField> out = new ArrayList<>(fields.size());
        for (HudField f : fields) out.add(f.copy());
        return out;
    }

    /** Record a freshly picked colour at the front of the recents list. */
    public static void pushRecent(int rgb) {
        int c = rgb & 0xFFFFFF;
        recentColors.remove(Integer.valueOf(c));
        recentColors.add(0, c);
        while (recentColors.size() > RECENT_MAX) recentColors.remove(recentColors.size() - 1);
    }

    /** Seed the visible flag from the server config (first-run fallback only). */
    public static void syncFromConfig() { visible = CustomBlocksConfig.hudEnabled; }

    // ── Persistence (delegated to HudConfigStore) ────────────────────────────
    public static void load() { HudConfigStore.load(); }
    public static void save() { HudConfigStore.save(); }

    // ── Shared helpers ───────────────────────────────────────────────────────
    public static float  clampScale(float s) { return Math.max(0.5f, Math.min(3.0f, s)); }
    public static float  clamp01(float v)     { return Math.max(0.0f, Math.min(1.0f, v)); }
    public static String toHex(int rgb)       { return String.format("#%06X", rgb & 0xFFFFFF); }

    public static int parseHex(String hex, int fallback) {
        try {
            String h = hex.trim();
            if (h.startsWith("#")) h = h.substring(1);
            return Integer.parseInt(h, 16) & 0xFFFFFF;
        } catch (Exception e) { return fallback; }
    }
}
