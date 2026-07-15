/**
 * CbOverlay.java — the CustomBlocks overlay layer. CLIENT-SIDE ONLY.
 *
 * ONE layer, owned by us, drawn from HudRenderMixin after vanilla finishes. Two groups render into it:
 *
 *   • GROUP 03 — the 3 HUD widgets (HudWidgetRenderer).
 *   • GROUP 04 — the chat flourishes: the confirm flash, and the progress/countdown readout that was
 *     rejected as a "live-updating chat line" on technical grounds. Vanilla gives no API to edit a chat
 *     message once it has been sent, so a line that rewrites itself is not a thing that can exist. It
 *     lives here instead, where we control every pixel.
 *
 * THE HARD RULE (locked 2026-07-14): never rewrite or re-render vanilla chat. Vanilla chat is
 * load-bearing — wrapping, scrollback, history, other mods' hooks — and is the single thing most likely
 * to break later. Everything cool happens in THIS layer, next to chat, never inside it.
 *
 * This layer makes no sound. The incidents ping and its blip were cut on 2026-07-14 (ADR-017):
 * IncidentRecorder still records everything, and /cb incidents is how you read it.
 *
 * Depends on: HudWidgetRenderer, HudRenderer (look-at context), CbFlash.
 * Called by:  HudRenderMixin (the one client HUD injection we already own).
 */
package com.customblocks.client.hud;

import com.customblocks.client.HudRenderer;
import com.customblocks.client.hud.widget.HudWidgetRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public final class CbOverlay {

    private CbOverlay() {} // static-only

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.player == null) return;

        // Reuse Group 27's look-at resolver rather than raycasting a second time — the padlock needs the
        // same "what is the crosshair on" answer the text bricks already computed this frame.
        HudFieldType.Ctx look = HudRenderer.buildContext(mc);

        HudWidgetRenderer.render(ctx, mc, look);
        CbFlash.render(ctx, mc);
    }
}
