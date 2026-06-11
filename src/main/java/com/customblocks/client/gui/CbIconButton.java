/**
 * CbIconButton.java
 *
 * Responsibility: A standard gray Minecraft button that also renders a small item icon
 * (Command Block) at its left edge — used for the ESC-menu CustomBlocks buttons.
 * CLIENT-SIDE ONLY.
 *
 * Depends on: ButtonWidget, DrawContext
 * Called by: EscMenuButtons
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class CbIconButton extends ButtonWidget {

    private final ItemStack icon;

    public CbIconButton(Item icon, String label, int x, int y, int w, int h, PressAction onPress) {
        super(x, y, w, h, Text.literal(label), onPress, textSupplier -> textSupplier.get());
        this.icon = new ItemStack(icon);
    }

    @Override
    public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderWidget(ctx, mouseX, mouseY, delta);
        // Item icon, vertically centered near the left edge.
        int iy = getY() + (getHeight() - 16) / 2;
        ctx.drawItem(icon, getX() + 4, iy);
    }
}
