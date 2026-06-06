/**
 * HudRenderMixin.java
 *
 * Responsibility: Inject into InGameHud.render to draw the CustomBlocks HUD overlay
 * after vanilla HUD rendering completes.
 * CLIENT-SIDE ONLY — listed in the "client" array of customblocks.mixins.json.
 *
 * Depends on: InGameHud, HudRenderer
 * Called by: Mixin framework on every render tick.
 */
package com.customblocks.mixin;

import com.customblocks.client.HudRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class HudRenderMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void cbRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        HudRenderer.render(context);
    }
}
