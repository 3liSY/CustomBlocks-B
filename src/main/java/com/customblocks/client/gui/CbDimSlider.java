/**
 * CbDimSlider.java — shared backdrop-dim slider for Group 27 cube screens. CLIENT-ONLY.
 *
 * G27.7 §A2: a small slider docked top-right of the title bar that drives the persisted backdrop dim
 * (CbScreenPrefs). Subclasses vanilla SliderWidget so the Screen widget system handles click + drag for
 * free. Clear (left) → solid black (right); the value persists per CbScreenPrefs so it sticks next open.
 *
 * Depends on: SliderWidget, CbScreenPrefs.
 * Called by: ArabicPreviewScreen, RecolorSliderScreen, ShapeEditorScreen (added in init()).
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class CbDimSlider extends SliderWidget {

    public CbDimSlider(int x, int y, int w, int h) {
        super(x, y, w, h, Text.empty(), CbScreenPrefs.get().dim01());
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Text.literal("§7Dim " + (int) Math.round(value * 100) + "%"));
    }

    @Override
    protected void applyValue() {
        CbScreenPrefs.get().setDim01(value);
    }
}
