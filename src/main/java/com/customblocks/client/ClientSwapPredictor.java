/**
 * ClientSwapPredictor.java
 *
 * Responsibility: make colour-Square swaps feel INSTANT. A swap is server-authoritative (the
 * Square's useOnBlock runs on the server, see ColorVariantService.swapPlaced) — the only delay
 * the player sees is the network round-trip: click → packet → server setBlockState → block
 * update packet back → re-render. This client-only listener removes that wait by painting the
 * predicted target block on the CLIENT world the same tick as the click. The server still does
 * the real swap; because the prediction mirrors the server exactly (same target block + same
 * glow), the authoritative packet reconciles with no visible change.
 *
 * Source of truth: the LIVE SlotManager registry — the SAME static maps the server's
 * swapPlaced reads. In singleplayer / a LAN host that registry is shared in-process, so a
 * variant created a moment ago by a Triangle is visible to the prediction immediately. This is
 * the old project's approach (CustomBlocks/.../ColorSquareItem read SlotManager directly) ported
 * to the current structure. The earlier version read ClientSlotCache (the HUD sync cache), which
 * LAGS a freshly-created variant — createVariant does not re-sync it — so the prediction missed
 * every just-made colour and the swap fell back to the slow round-trip. SlotManager never lags.
 *
 * On a remote DEDICATED server the client's SlotManager is empty (slot data lives on the server),
 * so getBySlot returns null and we cleanly PASS → the swap still works, just without prediction —
 * exactly as the old project behaved. No wrong guess, no flicker.
 *
 *   - shares the server's own id math (ColorVariantService.variantId/stripColourSuffix) → the
 *     client can never drift from server truth;
 *   - mirrors the LIGHT (glow) blockstate property via SlotManager.glowFor → no relight flash;
 *   - predicts HITS only: if the target variant does not exist it paints nothing and lets the
 *     server speak ("make it with the Triangle first") — no wrong guess, no flicker;
 *   - never cancels the interaction (returns PASS) so the server round-trip is untouched — this
 *     does NOT reintroduce the "client-side skip delay on tools" pitfall (CLAUDE.md §7).
 *
 * Depends on: SlotManager (live registry + glow), SlotBlock.LIGHT, ColorVariantService (shared
 *             id math), item/ColorSwapTool.
 * Called by:  CustomBlocksClient.onInitializeClient (register()).
 *
 * See: docs/adr/ADR-009-instant-colour-swap-client-prediction.md
 */
package com.customblocks.client;

import com.customblocks.block.SlotBlock;
import com.customblocks.core.ColorVariantService;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.item.ColorSwapTool;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class ClientSwapPredictor {

    private ClientSwapPredictor() {}

    /** Hook the predictor into right-click-on-block. Registered once from the client entrypoint. */
    public static void register() {
        UseBlockCallback.EVENT.register(ClientSwapPredictor::onUse);
    }

    private static ActionResult onUse(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        // Visual prediction only — always PASS so vanilla still sends the use to the server.
        if (!world.isClient) return ActionResult.PASS;
        ItemStack stack = player.getStackInHand(hand);
        if (!(stack.getItem() instanceof ColorSwapTool tool)) return ActionResult.PASS;
        String key = tool.swapColourKey(stack);
        if (key == null) return ActionResult.PASS; // a Triangle, or a colourless tool — nothing to predict

        BlockPos pos = hit.getBlockPos();
        if (!(world.getBlockState(pos).getBlock() instanceof SlotBlock slot)) return ActionResult.PASS;

        // Mirror ColorVariantService.swapPlaced exactly, off the LIVE registry (shared in
        // singleplayer). getBySlot is null on a remote dedicated client → PASS, no prediction.
        SlotData cur = SlotManager.getBySlot(slot.getSlotKey());
        if (cur == null) return ActionResult.PASS;

        SlotData target = SlotManager.getById(ColorVariantService.variantId(cur.customId(), key));
        if (target == null && "black".equals(key)) // Black Square falls back to the base block (server mirror)
            target = SlotManager.getById(ColorVariantService.stripColourSuffix(cur.customId()));
        if (target == null || target.index() == slot.getSlotIndex()) return ActionResult.PASS; // miss / already that colour → defer

        SlotBlock targetBlock = SlotManager.blockAt(target.index());
        if (targetBlock == null) return ActionResult.PASS;
        BlockState predicted = targetBlock.getDefaultState()
                .with(SlotBlock.LIGHT, SlotManager.glowFor(target.index())); // same glow swapPlaced rides in
        // NOTIFY_LISTENERS | FORCE_STATE: redraw without neighbour churn, no client/server fighting.
        world.setBlockState(pos, predicted, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        return ActionResult.PASS;
    }
}
