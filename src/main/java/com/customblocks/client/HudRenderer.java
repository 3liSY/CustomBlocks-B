/**
 * HudRenderer.java
 *
 * Responsibility: Render the CustomBlocks HUD overlay showing the ID and display name
 * of the custom block the player is currently looking at.
 * Uses ClientSlotCache (populated from HudSyncPayload on join) so it works on
 * both integrated and dedicated servers.
 * CLIENT-SIDE ONLY.
 *
 * Depends on: ClientSlotCache, HudConfig, SlotBlock (for instanceof check)
 * Called by: HudRenderMixin (after InGameHud.render)
 */
package com.customblocks.client;

import com.customblocks.block.SlotBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class HudRenderer {

    private HudRenderer() {}

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

        String id   = data[0];
        String name = data[1];

        int x = HudConfig.x;
        int y = HudConfig.y;
        ctx.drawTextWithShadow(client.textRenderer,
                Text.literal("§e" + id + " §7\"" + name + "\""), x, y, 0xFFFFFFFF);
    }
}
