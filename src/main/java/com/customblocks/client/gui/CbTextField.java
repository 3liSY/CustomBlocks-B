/**
 * CbTextField.java — GROUP 27 §G27.6.P (P13). CLIENT-SIDE ONLY.
 *
 * Red+black themed text field — draws its own dark box with a grey border that turns neon-red
 * (CbTheme.ACCENT) on focus, replacing vanilla's white stock box. Disables the stock background
 * and renders the box itself, then defers to TextFieldWidget for text/cursor/placeholder.
 *
 * Depends on: TextFieldWidget, CbTheme, DrawContext. Called by: BlockCreationStudioScreen.
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class CbTextField extends TextFieldWidget {

    public CbTextField(TextRenderer tr, int x, int y, int w, int h, Text msg) {
        super(tr, x, y, w, h, msg);
        setDrawsBackground(false); // we draw our own box in renderWidget
    }

    @Override
    public void renderWidget(DrawContext ctx, int mx, int my, float delta) {
        if (this.visible) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            int border = isFocused() ? CbTheme.ACCENT : 0xFF555555;
            ctx.fill(x - 3, y - 3, x + w + 3, y + h + 3, border);   // border / focus glow
            ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF0C0C0C); // inner dark fill
        }
        // §G27.20a — drawsBackground=false makes vanilla draw text/placeholder at the raw top-left
        // (x,y): no inset, no vertical centering, so hints ride ~4px high onto the label above.
        // Shift to the inset+centered spot vanilla uses WITH a background, so text sits IN the box.
        ctx.getMatrices().push();
        ctx.getMatrices().translate(4.0, (getHeight() - 8) / 2.0, 0.0);
        super.renderWidget(ctx, mx, my, delta); // text + placeholder + cursor (no stock box)
        ctx.getMatrices().pop();
    }

    // §G27.20a — match vanilla's clip width (box minus the 4px inset each side) so long text
    // can't run past the box edge.
    @Override
    public int getInnerWidth() {
        return getWidth() - 8;
    }
}
