/**
 * HudRenderer.java
 *
 * Responsibility: Render the CustomBlocks HUD overlay showing the ID and display name
 * of the custom block the player is currently looking at. Honors the configured
 * position, scale, text color, background opacity, and show-ID / show-name flags.
 * Uses ClientSlotCache (populated from HudSyncPayload on join) so it works on both
 * integrated and dedicated servers. CLIENT-SIDE ONLY.
 *
 * Depends on: ClientSlotCache, HudConfig, SlotBlock (for instanceof check)
 * Called by: HudRenderMixin (live, after InGameHud.render), HudEditorScreen (preview)
 */
package com.customblocks.client;

import com.customblocks.block.SlotBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class HudRenderer {

    private HudRenderer() {}

    private static final int PAD = 3;

    /** Live HUD: drawn every frame by HudRenderMixin after InGameHud.render. */
    public static void render(DrawContext ctx) {
        if (!HudConfig.visible) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHit)) return;
        if (hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = client.world.getBlockState(pos);
        if (!(state.getBlock() instanceof SlotBlock slot)) return;

        String[] data = ClientSlotCache.get(slot.getSlotIndex());
        if (data == null || data.length < 2) return;

        draw(ctx, client, data[0], data[1]);
    }

    /**
     * Shared draw routine used by both the live HUD and the editor preview.
     * Honors HudConfig position, scale, color, background opacity, and show flags.
     */
    public static void draw(DrawContext ctx, MinecraftClient client, String id, String name) {
        TextRenderer tr = client.textRenderer;

        List<String> lines = new ArrayList<>(2);
        if (HudConfig.showId   && id   != null && !id.isEmpty())   lines.add(id);
        if (HudConfig.showName && name != null && !name.isEmpty()) lines.add(name);
        if (lines.isEmpty()) return;

        int textW = 0;
        for (String l : lines) textW = Math.max(textW, tr.getWidth(l));
        int lineH = tr.fontHeight + 2;
        int textH = lines.size() * lineH;

        float scale = HudConfig.clampScale(HudConfig.scale);
        int col = 0xFF000000 | (HudConfig.color & 0xFFFFFF);

        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(HudConfig.x, HudConfig.y, 0);
        m.scale(scale, scale, 1.0f);

        if (HudConfig.bgOpacity > 0f) {
            int alpha = Math.round(HudConfig.clamp01(HudConfig.bgOpacity) * 255f);
            ctx.fill(-PAD, -PAD, textW + PAD, textH + PAD - 2, alpha << 24);
        }

        int yy = 0;
        for (String l : lines) {
            ctx.drawTextWithShadow(tr, Text.literal(l), 0, yy, col);
            yy += lineH;
        }
        m.pop();
    }

    /** Pixel size {w,h} of the HUD box at current settings (editor hit-testing). */
    public static int[] boxSize(MinecraftClient client, String id, String name) {
        TextRenderer tr = client.textRenderer;
        int lines = 0, textW = 0;
        if (HudConfig.showId   && id   != null && !id.isEmpty())   { lines++; textW = Math.max(textW, tr.getWidth(id)); }
        if (HudConfig.showName && name != null && !name.isEmpty()) { lines++; textW = Math.max(textW, tr.getWidth(name)); }
        int lineH = tr.fontHeight + 2;
        float scale = HudConfig.clampScale(HudConfig.scale);
        int w = Math.round((textW + PAD * 2) * scale);
        int h = Math.round((lines * lineH + PAD * 2 - 2) * scale);
        return new int[]{ Math.max(w, 8), Math.max(h, 8) };
    }
}
