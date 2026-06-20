/**
 * HudAnchors.java — GROUP 27 §G27.4 (Lego HUD Builder). CLIENT-SIDE ONLY.
 *
 * Responsibility: anchor ↔ screen-position geometry shared by the editor. Converts a brick's
 * screen-space top-left back into offset terms for its current anchor (place), and re-anchors a
 * brick to the nearest screen corner so its offset stays small + holds across resolutions.
 * Pure math (matches HudRenderer's forward resolvePos).
 *
 * Depends on: HudField.
 * Called by: HudEditorScreen (drag / nudge / snap).
 */
package com.customblocks.client.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class HudAnchors {

    private HudAnchors() {}

    /** Store a brick's screen position (top-left) back into offset terms for its current anchor. */
    public static void place(HudField f, int x, int y, int w, int h, int screenW, int screenH) {
        switch (f.anchor) {
            case TR     -> { f.offsetX = screenW - x - w; f.offsetY = y; }
            case BL     -> { f.offsetX = x;               f.offsetY = screenH - y - h; }
            case BR     -> { f.offsetX = screenW - x - w; f.offsetY = screenH - y - h; }
            case CENTER -> { f.offsetX = x + w / 2 - screenW / 2; f.offsetY = y + h / 2 - screenH / 2; }
            default     -> { f.offsetX = x;               f.offsetY = y; }
        }
    }

    /**
     * Re-anchor to the nearest corner (by the brick's centre) and store the offset.
     *
     * §G27.7 §E5: a variable-width text brick (e.g. the display-name, whose width changes per block) that
     * the player centred used to take a left-edge corner anchor, so a longer name grew rightward and
     * "drifted right." If such a brick is dropped near the horizontal centre we give it the CENTER anchor
     * instead — resolvePos then re-centres it on its stored centre point every frame, so it grows
     * symmetrically and stays put as the name's width changes. Dividers (fixed width) keep corner anchors.
     */
    public static void reanchor(HudField f, int x, int y, int w, int h, int screenW, int screenH) {
        int cx = x + w / 2;
        if (!f.type.isDivider() && Math.abs(cx - screenW / 2) < screenW * 0.10) {
            f.anchor = HudField.Anchor.CENTER;
            place(f, x, y, w, h, screenW, screenH);
            return;
        }
        boolean right  = cx > screenW / 2;
        boolean bottom = (y + h / 2) > screenH / 2;
        f.anchor = right ? (bottom ? HudField.Anchor.BR : HudField.Anchor.TR)
                         : (bottom ? HudField.Anchor.BL : HudField.Anchor.TL);
        place(f, x, y, w, h, screenW, screenH);
    }
}
