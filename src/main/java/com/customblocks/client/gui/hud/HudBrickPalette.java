/**
 * HudBrickPalette.java — GROUP 27 §G27.4 (Lego HUD Builder). CLIENT-SIDE ONLY.
 *
 * Responsibility: the "+ Add brick" popup. Draws the list of every HudFieldType to the left of
 * the editor's right-dock panel and reports which type the player clicked (or closes when they
 * click away). Pure draw + hit-test; the editor owns adding the chosen brick.
 *
 * Depends on: HudFieldType.
 * Called by: HudEditorScreen.
 */
package com.customblocks.client.gui.hud;

import com.customblocks.client.hud.HudFieldType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class HudBrickPalette {

    private static final int GOLD = 0xFFFFAA00;
    private static final int PW = 150, ROW = 14;

    private boolean open = false;

    public boolean isOpen() { return open; }
    public void toggle()    { open = !open; }
    public void close()     { open = false; }

    private static int[] box(int screenW, int panelW, int barH) {
        int x = screenW - panelW - PW - 6;
        if (x < 4) x = 4;
        return new int[]{ x, barH + 8, PW, HudFieldType.values().length * ROW + 8 };
    }

    public void draw(DrawContext ctx, TextRenderer tr, int screenW, int panelW, int barH, double mx, double my) {
        HudFieldType[] types = HudFieldType.values();
        int[] b = box(screenW, panelW, barH);
        int x = b[0], y = b[1];
        ctx.fill(x - 1, y - 1, x + PW + 1, y + b[3] + 1, GOLD);
        ctx.fill(x, y, x + PW, y + b[3], 0xF0101010);
        for (int i = 0; i < types.length; i++) {
            int ry = y + 4 + i * ROW;
            boolean hover = mx >= x && mx <= x + PW && my >= ry && my < ry + ROW;
            if (hover) ctx.fill(x, ry, x + PW, ry + ROW, 0x44FFFFFF);
            ctx.drawText(tr, Text.literal((hover ? "§f" : "§7") + types[i].label()), x + 6, ry + 3, 0xFFFFFFFF, false);
        }
    }

    /** Returns the clicked brick type, or null. Always closes the palette (click-away dismisses). */
    public HudFieldType click(double mx, double my, int screenW, int panelW, int barH) {
        HudFieldType[] types = HudFieldType.values();
        int[] b = box(screenW, panelW, barH);
        int x = b[0], y = b[1];
        open = false;
        if (mx < x || mx > x + PW || my < y || my > y + b[3]) return null;
        for (int i = 0; i < types.length; i++) {
            int ry = y + 4 + i * ROW;
            if (my >= ry && my < ry + ROW) return types[i];
        }
        return null;
    }
}
