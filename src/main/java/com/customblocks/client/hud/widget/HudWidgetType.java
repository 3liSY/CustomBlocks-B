/**
 * HudWidgetType.java — GROUP 03. CLIENT-SIDE ONLY.
 *
 * The catalogue of the 2 HUD widgets. This is a SIBLING of {@link com.customblocks.client.hud.HudFieldType},
 * deliberately NOT an extension of it (locked 2026-07-14):
 *
 *   HudFieldType is Group 27's catalogue of TEXT BRICKS — every entry resolves the look-at context to a
 *   String, and the renderer draws that string. The widgets here are GRAPHICAL and stateful: a padlock
 *   glyph beside the crosshair, a banner that pulses. Neither is "a string", so bolting them onto a
 *   string-resolver API would mean lying about what they are and dragging Group 27's shipped text HUD
 *   into every change. Two catalogues, one overlay layer.
 *
 * WIDGETS ARE DISPLAY-ONLY (locked 2026-07-14). They render state; they are never clicked. Minecraft grabs
 * the mouse while you play, so there is no cursor to click them with, and a "hold a key to free the cursor"
 * scheme was rejected outright ("no one is gonna remember to click a hotkey"). Every widget's ACTION is the
 * command it already maps to — see {@link #action()}, which exists to document that mapping, not to run it.
 *
 * Position and colour are FIXED here, not player-editable. The Widgets tab, the per-widget colour picker,
 * the layout store and the op-force pin were all cut on 2026-07-14 (see ADR-017), so the look of these
 * three lives in exactly one place: this enum. All three are pending a redesign pass.
 *
 * Depends on: nothing (pure catalogue).
 * Called by: HudWidgetRenderer (draw).
 */
package com.customblocks.client.hud.widget;

import com.customblocks.client.gui.CbTheme;
import com.customblocks.client.hud.HudField.Anchor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum HudWidgetType {

    // ACTIVE_TOOL is DELETED (G03-TOOLCHIP, owner-locked 2026-07-15). It rendered WidgetSync's "tool" property,
    // which carried the player's REMEMBERED Omni-Tool mode rather than "is holding the tool" — so setting a mode
    // once left the bottom-left chip on screen forever. Deleting the widget IS the fix; gating it would have been
    // more code for a widget the owner did not want. Two widgets remain.

    /**
     * Pulses red/white while `/cb macro record` is running. Loud on purpose — the whole failure mode
     * this widget exists to stop is finishing a build and only THEN noticing you were recording.
     * Action: /cb macro stop.
     */
    MACRO_BANNER("macro", "Macro-recording banner", Anchor.TL, 4, 4, CbTheme.ACCENT,
            "/cb macro stop"),

    /**
     * A SMALL padlock 10px BELOW the crosshair — never on it (locked 2026-07-15). The point is to tell you the
     * block is locked without ever obstructing your aim, so the offset is from screen centre and deliberately
     * non-zero: offsetX 0 (horizontally centred), offsetY 10 (clear of the reticle, below it).
     *
     * Drawn as a hand-built 7×9 sprite, NOT the 🔒 font glyph — see HudWidgetRenderer.drawPadlock.
     * Action: /cb unlock <id>.
     */
    LOCKED_PADLOCK("padlock", "Locked-block padlock", Anchor.CENTER, 0, 10, CbTheme.ACCENT,
            "/cb unlock <id>");

    private final String key;
    private final String label;
    private final Anchor anchor;
    private final int offsetX;
    private final int offsetY;
    private final int color;
    private final String action;

    HudWidgetType(String key, String label, Anchor anchor, int offsetX, int offsetY,
                  int color, String action) {
        this.key = key;
        this.label = label;
        this.anchor = anchor;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.color = color;
        this.action = action;
    }

    /** Stable key. */
    public String key() { return key; }

    /** Human label. */
    public String label() { return label; }

    public Anchor anchor() { return anchor; }
    public int offsetX() { return offsetX; }
    public int offsetY() { return offsetY; }

    /** The widget's colour, from {@link CbTheme} — the mod-wide red/black/lime the Screen GUIs already use. */
    public int color() { return color; }

    /** The existing command this widget's action maps to. Documentation only; never executed by the HUD. */
    public String action() { return action; }

    /** Look up by stable key, or null. */
    public static HudWidgetType byKey(String k) {
        for (HudWidgetType t : values()) {
            if (t.key.equalsIgnoreCase(k)) return t;
        }
        return null;
    }
}
