/**
 * HudField.java — GROUP 27 §G27.4 (Lego HUD Builder). CLIENT-SIDE ONLY.
 *
 * Responsibility: one HUD "brick" instance — its type plus all per-brick state the editor
 * can change: free-floating position (offset + anchor), visibility, size, colour, bold,
 * shadow, prefix label, alignment, optional cosmetic effect, and an optional background
 * override. Owns its own JSON (de)serialisation so HudConfig / HudConfigStore stay lean.
 *
 * Mutable by design: this is a client-side editor model (not server SlotData), edited live
 * in HudEditorScreen. The immutability rule (CLAUDE.md §5.2) is about SlotData, not this.
 *
 * Depends on: HudFieldType, Gson.
 * Called by: HudConfig (brick list), HudRenderer (draw), HudEditorScreen / inspector (edit).
 */
package com.customblocks.client.hud;

import com.google.gson.JsonObject;

public final class HudField {

    /** Which screen corner the brick's offset is measured from. */
    public enum Anchor { TL, TR, BL, BR, CENTER }
    /** Horizontal text alignment within the brick's own pad. */
    public enum Align { LEFT, CENTER, RIGHT }
    /** Optional cosmetic effect; NONE renders a plain solid-colour line. */
    public enum Effect { NONE, RAINBOW, PULSE, GRADIENT }

    public HudFieldType type;
    public int     offsetX = 0;
    public int     offsetY = 0;
    public Anchor  anchor  = Anchor.TL;

    public boolean visible = true;       // 👁 show/hide toggle
    public float   size    = 1.0f;       // own scale, 0.5 .. 3.0
    public int     color   = 0xFFFFFF;   // 0xRRGGBB
    public boolean bold    = false;
    public boolean shadow  = true;
    public String  prefix  = "";         // e.g. "ID: "
    public Align   align   = Align.LEFT;
    public Effect  effect  = Effect.NONE;

    // Per-brick background override (else the global default is used).
    public boolean bgOverride = false;   // true → use this brick's bg settings below
    public boolean bgOff      = false;   // true (with bgOverride) → no background at all
    public int     bgColor    = 0x000000;
    public float   bgOpacity  = 0.4f;

    // Text payload for CUSTOM_TEXT / HEADER bricks.
    public String  text = "";

    public HudField() {}

    public HudField(HudFieldType type) {
        this.type = type;
        this.prefix = type.suggestedPrefix();
        if (type == HudFieldType.HEADER) { this.bold = true; this.text = "Header"; }
        if (type == HudFieldType.CUSTOM_TEXT) this.text = "Text";
    }

    public HudField(HudFieldType type, int offsetX, int offsetY, Anchor anchor, float size) {
        this(type);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.anchor = anchor;
        this.size = size;
    }

    /** Deep copy (used for undo snapshots + preset application). */
    public HudField copy() {
        HudField f = new HudField();
        f.type = type;
        f.offsetX = offsetX; f.offsetY = offsetY; f.anchor = anchor;
        f.visible = visible; f.size = size; f.color = color; f.bold = bold; f.shadow = shadow;
        f.prefix = prefix; f.align = align; f.effect = effect;
        f.bgOverride = bgOverride; f.bgOff = bgOff; f.bgColor = bgColor; f.bgOpacity = bgOpacity;
        f.text = text;
        return f;
    }

    // ── JSON ────────────────────────────────────────────────────────────────

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("type",    type.key());
        o.addProperty("x",       offsetX);
        o.addProperty("y",       offsetY);
        o.addProperty("anchor",  anchor.name());
        o.addProperty("visible", visible);
        o.addProperty("size",    size);
        o.addProperty("color",   color & 0xFFFFFF);
        o.addProperty("bold",    bold);
        o.addProperty("shadow",  shadow);
        o.addProperty("prefix",  prefix);
        o.addProperty("align",   align.name());
        o.addProperty("effect",  effect.name());
        o.addProperty("bgOver",  bgOverride);
        o.addProperty("bgOff",   bgOff);
        o.addProperty("bgColor", bgColor & 0xFFFFFF);
        o.addProperty("bgOpac",  bgOpacity);
        o.addProperty("text",    text);
        return o;
    }

    public static HudField fromJson(JsonObject o) {
        HudField f = new HudField();
        f.type      = HudFieldType.fromKey(str(o, "type", "id"));
        f.offsetX   = num(o, "x", 0);
        f.offsetY   = num(o, "y", 0);
        f.anchor    = anchorOf(str(o, "anchor", "TL"));
        f.visible   = bool(o, "visible", true);
        f.size      = clampSize((float) dbl(o, "size", 1.0));
        f.color     = num(o, "color", 0xFFFFFF) & 0xFFFFFF;
        f.bold      = bool(o, "bold", false);
        f.shadow    = bool(o, "shadow", true);
        f.prefix    = str(o, "prefix", "");
        f.align     = alignOf(str(o, "align", "LEFT"));
        f.effect    = effectOf(str(o, "effect", "NONE"));
        f.bgOverride= bool(o, "bgOver", false);
        f.bgOff     = bool(o, "bgOff", false);
        f.bgColor   = num(o, "bgColor", 0x000000) & 0xFFFFFF;
        f.bgOpacity = clamp01((float) dbl(o, "bgOpac", 0.4));
        f.text      = str(o, "text", "");
        return f;
    }

    public static float clampSize(float s) { return Math.max(0.5f, Math.min(3.0f, s)); }
    public static float clamp01(float v)    { return Math.max(0f, Math.min(1f, v)); }

    private static Anchor anchorOf(String s) { try { return Anchor.valueOf(s); } catch (Exception e) { return Anchor.TL; } }
    private static Align  alignOf(String s)  { try { return Align.valueOf(s);  } catch (Exception e) { return Align.LEFT; } }
    private static Effect effectOf(String s) { try { return Effect.valueOf(s); } catch (Exception e) { return Effect.NONE; } }

    private static String  str (JsonObject o, String k, String def)  { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString()  : def; }
    private static int     num (JsonObject o, String k, int def)     { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt()     : def; }
    private static double  dbl (JsonObject o, String k, double def)  { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble()  : def; }
    private static boolean bool(JsonObject o, String k, boolean def) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsBoolean() : def; }
}
