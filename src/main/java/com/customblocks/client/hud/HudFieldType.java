/**
 * HudFieldType.java — GROUP 27 §G27.4 (Lego HUD Builder). CLIENT-SIDE ONLY.
 *
 * Responsibility: the brick catalogue. Each enum constant is one kind of HUD info line
 * ("brick"): its stable JSON key, its editor label, its visibility family (block-info vs
 * world), whether it needs synced SlotData, a suggested default prefix, and a value
 * resolver that turns the live look-at context into the string the brick displays.
 *
 * Visibility families (spec §"Brick visibility"):
 *   - BLOCK_INFO bricks render only while the crosshair is on a custom block.
 *   - WORLD bricks render always (their value may resolve null when nothing is aimed at).
 *
 * Depends on: nothing (pure catalogue + Ctx record).
 * Called by: HudRenderer (resolve per frame), HudField (type ↔ key), the editor palette.
 */
package com.customblocks.client.hud;

import java.util.Locale;

public enum HudFieldType {

    // key            label                family               needsSync  suggestedPrefix
    BLOCK_ID    ("id",        "Block ID",          Family.BLOCK_INFO, false, "ID: "),
    DISPLAY_NAME("name",      "Name",              Family.BLOCK_INFO, false, ""),
    SLOT        ("slot",      "Slot #",            Family.BLOCK_INFO, false, "Slot "),
    COORDS      ("coords",    "Coordinates",       Family.WORLD,      false, ""),
    LIGHT       ("light",     "World light",       Family.WORLD,      false, "Light "),
    DISTANCE    ("distance",  "Distance",          Family.WORLD,      false, ""),
    FACING      ("facing",    "Facing side",       Family.WORLD,      false, "Facing "),
    CUSTOM_TEXT ("text",      "Custom text",       Family.WORLD,      false, ""),
    HEADER      ("header",    "Header",            Family.WORLD,      false, ""),
    DIVIDER     ("divider",   "Divider",           Family.WORLD,      false, ""),
    CATEGORY    ("category",  "Category",          Family.BLOCK_INFO, true,  "Cat: "),
    GLOW        ("glow",      "Light value",       Family.BLOCK_INFO, true,  "Glow "),
    HARDNESS    ("hardness",  "Hardness",          Family.BLOCK_INFO, true,  "Hardness "),
    SOUND       ("sound",     "Sound type",        Family.BLOCK_INFO, true,  "Sound "),
    SHAPE       ("shape",     "Shape",             Family.BLOCK_INFO, true,  "Shape "),
    SOLID       ("solid",     "Solid / passable",  Family.BLOCK_INFO, true,  "");

    /** Visibility family — controls when a brick is allowed to render. */
    public enum Family { BLOCK_INFO, WORLD }

    private final String key;
    private final String label;
    private final Family family;
    private final boolean needsSync;
    private final String suggestedPrefix;

    HudFieldType(String key, String label, Family family, boolean needsSync, String suggestedPrefix) {
        this.key = key;
        this.label = label;
        this.family = family;
        this.needsSync = needsSync;
        this.suggestedPrefix = suggestedPrefix;
    }

    public String key()            { return key; }
    public String label()          { return label; }
    public Family family()         { return family; }
    public boolean needsSync()     { return needsSync; }
    public String suggestedPrefix(){ return suggestedPrefix; }

    /** True for the static, user-authored bricks whose text comes from the brick itself. */
    public boolean isText()    { return this == CUSTOM_TEXT || this == HEADER; }
    /** True for the rule-line brick that the renderer draws as a divider, not text. */
    public boolean isDivider() { return this == DIVIDER; }

    public static HudFieldType fromKey(String key) {
        for (HudFieldType t : values()) if (t.key.equals(key)) return t;
        return BLOCK_ID;
    }

    /**
     * Resolve the brick's display value from the current look-at context.
     * Returns null when this brick has nothing to show right now (renderer skips it).
     * {@code text} is the brick's own stored string (used by CUSTOM_TEXT / HEADER).
     */
    public String resolve(Ctx c, String text) {
        return switch (this) {
            case BLOCK_ID     -> emptyToNull(c.id());
            case DISPLAY_NAME -> emptyToNull(c.name());
            case SLOT         -> c.hasBlock() && c.slot() >= 0 ? String.valueOf(c.slot()) : null;
            case COORDS       -> c.hasPos() ? c.bx() + " " + c.by() + " " + c.bz() : null;
            case LIGHT        -> c.hasPos() ? String.valueOf(c.light()) : null;
            case DISTANCE     -> c.hasPos() ? String.format(Locale.ROOT, "%.1f", c.distance()) : null;
            case FACING       -> c.hasPos() ? c.facing() : null;
            case CUSTOM_TEXT  -> emptyToNull(text);
            case HEADER       -> emptyToNull(text);
            case DIVIDER      -> "";   // rendered as a rule line, not text
            case CATEGORY     -> isBlank(c.category()) ? "Uncategorized" : c.category();
            case GLOW         -> String.valueOf(c.glow());
            case HARDNESS     -> hardnessLabel(c.hardness());
            case SOUND        -> isBlank(c.sound()) ? "stone" : c.sound();
            case SHAPE        -> isBlank(c.shape()) ? "full" : c.shape();
            case SOLID        -> c.passable() ? "Passable" : "Solid";
        };
    }

    private static String emptyToNull(String s) { return (s == null || s.isEmpty()) ? null : s; }
    private static boolean isBlank(String s)     { return s == null || s.isBlank(); }

    private static String hardnessLabel(float h) {
        if (h < 0) return "Unbreakable";
        String named =
                h == 0f   ? "Instant" :
                h <= 0.5f ? "Soft"   :
                h <= 2.0f ? "Wood"   :
                h <= 3.0f ? "Stone"  :
                h <= 5.0f ? "Iron"   : "Hard";
        return String.format(Locale.ROOT, "%.1f (%s)", h, named);
    }

    /**
     * Immutable snapshot of everything the resolvers may read for one frame.
     * {@code hasBlock} = crosshair on a custom block; {@code hasPos} = crosshair on any block.
     */
    public record Ctx(
            boolean hasBlock, String id, String name, int slot,
            String category, int glow, float hardness, String sound, String shape, boolean passable,
            boolean hasPos, int bx, int by, int bz, int light, double distance, String facing) {

        /** An empty context: nothing aimed at (world bricks with no pos resolve null). */
        public static Ctx empty() {
            return new Ctx(false, "", "", -1, "", 0, 0f, "stone", "full", false,
                    false, 0, 0, 0, 0, 0d, "—");
        }
    }
}
