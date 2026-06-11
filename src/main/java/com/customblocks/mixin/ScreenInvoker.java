/**
 * ScreenInvoker.java
 *
 * Responsibility: Mixin accessor exposing Screen's protected addDrawableChild() so the
 * ESC-menu injector (EscMenuButtons) can add real, rendered + clickable widgets to the
 * vanilla GameMenuScreen — the same call the screen uses internally. This is the Group 03
 * "Mixin on GameMenuScreen" requirement, implemented as a tiny invoker so the button
 * wiring stays in plain client code instead of brittle init-method patching.
 *
 * Depends on: SpongePowered Mixin, Minecraft Screen
 * Called by: EscMenuButtons
 */
package com.customblocks.mixin;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenInvoker {

    @Invoker("addDrawableChild")
    <T extends Element & Drawable & Selectable> T customblocks$addDrawableChild(T widget);
}
