/**
 * CbImmediateFill.java — GROUP 27 UI kit. Shared "opaque fill that can't be bled through" helper. CLIENT-ONLY.
 *
 * {@code ctx.fill}/{@code ctx.drawText} are DEFERRED (flushed at the end of the screen render), while
 * {@code ctx.drawTexture} renders IMMEDIATELY (see {@link PreviewCube} header) — two deferred draws
 * (an earlier background text, a later opaque scrim) can still land in the wrong visual order on the
 * final flush even with an intermediate {@code ctx.draw()} call (the F2/K7 bleed-through the CbHelpOverlay
 * ctx.draw() trick didn't fully fix). Drawing the scrim as an IMMEDIATE textured quad instead of a
 * deferred fill sidesteps the reordering entirely — it paints to the framebuffer right then and there,
 * after everything queued so far, guaranteed on top.
 *
 * Depends on: DrawContext, NativeImageBackedTexture, RenderSystem. Called by: CbHelpOverlay, CbPopupPicker.
 */
package com.customblocks.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class CbImmediateFill {

    private CbImmediateFill() {} // static-only

    private static NativeImageBackedTexture whiteTex;
    private static Identifier whiteTexId;

    private static void ensureWhite() {
        if (whiteTex != null) return;
        NativeImage img = new NativeImage(1, 1, false);
        img.setColor(0, 0, 0xFFFFFFFF); // opaque white, little-endian ABGR — tinted per-draw via shader color
        whiteTex = new NativeImageBackedTexture(img);
        whiteTexId = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("cb_immediate_fill", whiteTex);
    }

    /**
     * Fill [x0,y0)-[x1,y1) with an opaque/translucent colour, drawn IMMEDIATELY so nothing already
     * queued (background text included) can land on top of it. Flushes deferred draws issued so far
     * first, same as {@link PreviewCube}'s backdrop trick.
     */
    public static void fill(DrawContext ctx, int x0, int y0, int x1, int y1, int argb) {
        ensureWhite();
        ctx.draw(); // flush everything queued so far so this quad paints on top of it, not before it
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderColor(r, g, b, a);
        ctx.drawTexture(whiteTexId, x0, y0, x1 - x0, y1 - y0, 0f, 0f, 1, 1, 1, 1);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
    }
}
