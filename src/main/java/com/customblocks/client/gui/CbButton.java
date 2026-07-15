/**
 * CbButton.java — GROUP 27 §G27.6.P (P11/P12). CLIENT-SIDE ONLY.
 *
 * Red+black themed button drawn from CbTheme — replaces vanilla ButtonWidget on the Block
 * Creation Studio so the screen has no grey stock buttons. Four styles:
 *   PRIMARY — filled neon-red (Create / Save, "Yes, discard")
 *   GHOST   — outlined, transparent fill (Cancel / Keep editing)
 *   NORMAL  — dark card, red on hover (in-section actions: Load texture, +/-, …)
 *   TAB     — left section strip; carries a selected state
 *
 * Depends on: ButtonWidget, CbTheme, DrawContext, MinecraftClient. Called by: BlockCreationStudioScreen.
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class CbButton extends ButtonWidget {

    public enum Style { PRIMARY, GHOST, NORMAL, TAB }

    private final Style style;
    private final boolean selected; // TAB only

    private CbButton(Text msg, int x, int y, int w, int h, PressAction onPress, Style s, boolean selected, boolean enabled) {
        super(x, y, w, h, msg, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.style = s;
        this.selected = selected;
        this.active = enabled;
    }

    public static CbButton primary(Text msg, int x, int y, int w, int h, PressAction a) {
        return new CbButton(msg, x, y, w, h, a, Style.PRIMARY, false, true);
    }
    public static CbButton ghost(Text msg, int x, int y, int w, int h, PressAction a) {
        return new CbButton(msg, x, y, w, h, a, Style.GHOST, false, true);
    }
    public static CbButton normal(Text msg, int x, int y, int w, int h, PressAction a) {
        return new CbButton(msg, x, y, w, h, a, Style.NORMAL, false, true);
    }
    public static CbButton tab(Text msg, int x, int y, int w, int h, boolean selected, boolean enabled, PressAction a) {
        return new CbButton(msg, x, y, w, h, a, Style.TAB, selected, enabled);
    }

    @Override
    public void renderWidget(DrawContext ctx, int mx, int my, float delta) {
        boolean hov = isHovered();
        int bg, border, textCol;
        if (!active) {
            bg = 0xFF161616; border = 0xFF2A2A2A; textCol = 0xFF777777;
        } else switch (style) {
            case PRIMARY -> { bg = hov ? 0xFFFF3D63 : CbTheme.ACCENT; border = 0xFFFF8DA1; textCol = 0xFFFFFFFF; }
            case GHOST   -> { bg = hov ? 0x22FF1744 : 0x00000000;     border = hov ? CbTheme.ACCENT : 0xFF6A6A6A; textCol = hov ? 0xFFFFFFFF : 0xFFCCCCCC; }
            case TAB     -> { bg = selected ? 0x33FF1744 : (hov ? 0xFF241015 : 0xFF151515); border = selected ? CbTheme.ACCENT : 0xFF2A2A2A; textCol = 0xFFFFFFFF; }
            default      -> { bg = hov ? 0xFF2A1014 : 0xFF1A1A1A;     border = hov ? CbTheme.ACCENT : 0xFF3A3A3A; textCol = 0xFFFFFFFF; }
        }
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        ctx.fill(x, y, x + w, y + h, bg);
        ctx.fill(x, y, x + w, y + 1, border);
        ctx.fill(x, y + h - 1, x + w, y + h, border);
        ctx.fill(x, y, x + 1, y + h, border);
        ctx.fill(x + w - 1, y, x + w, y + h, border);
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawCenteredTextWithShadow(tr, getMessage(), x + w / 2, y + (h - 8) / 2, textCol);
    }
}
