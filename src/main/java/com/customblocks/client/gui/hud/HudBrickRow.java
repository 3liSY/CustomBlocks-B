/**
 * HudBrickRow.java — GROUP 27 §G27.4 (Lego HUD Builder). CLIENT-SIDE ONLY.
 *
 * Responsibility: draw one row of the editor's right-dock brick list (⠿ handle · 👁 eye ·
 * label · ⚙ inspector · ✕ delete) and report which control zone a click landed in. Keeps the
 * panel layout in one place so HudEditorScreen stays lean.
 *
 * Depends on: HudField.
 * Called by: HudEditorScreen (drawPanel + handlePanelClick).
 */
package com.customblocks.client.gui.hud;

import com.customblocks.client.hud.HudField;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class HudBrickRow {

    private HudBrickRow() {}

    /** Which control of a row a click hit. */
    public enum Zone { HANDLE, EYE, GEAR, DELETE, BODY, NONE }

    public static void draw(DrawContext ctx, TextRenderer tr, HudField f, int px, int ry, int rowH,
                            int screenW, boolean selected, boolean reorderTarget, int cyan) {
        if (selected)       ctx.fill(px - 4, ry - 1, screenW, ry + rowH - 1, 0x55FFAA00);
        if (reorderTarget)  ctx.fill(px - 4, ry, screenW, ry + 1, cyan);
        String eye = f.visible ? "§a●" : "§7○";
        ctx.drawText(tr, Text.literal("§8⠿ " + eye + " §f" + f.type.label()), px - 2, ry + 3, 0xFFFFFFFF, false);
        ctx.drawText(tr, Text.literal("§e⚙ §c✕"), screenW - 26, ry + 3, 0xFFFFFFFF, false);
    }

    /** Map an X within a row to its control zone (Y is the caller's row hit-test). */
    public static Zone zoneAt(double mx, int px, int screenW) {
        if (mx >= px - 4 && mx < px + 6)        return Zone.HANDLE;
        if (mx >= px + 6 && mx < px + 18)       return Zone.EYE;
        if (mx >= screenW - 28 && mx < screenW - 18) return Zone.GEAR;
        if (mx >= screenW - 18)                 return Zone.DELETE;
        if (mx >= px - 4 && mx < screenW)       return Zone.BODY;
        return Zone.NONE;
    }
}
