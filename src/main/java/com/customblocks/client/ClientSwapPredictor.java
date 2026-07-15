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
 * Source of truth — dual, by session (G06-6):
 *   • Singleplayer / LAN HOST: the in-process SlotManager registry (the SAME static maps the
 *     server's swapPlaced reads). A variant created a moment ago by a Triangle is visible to the
 *     prediction immediately. This is the old project's approach (ColorSquareItem read SlotManager
 *     directly) ported here.
 *   • Remote DEDICATED / LAN-guest client: the client JVM loads its OWN slots.json (proven in C1
 *     step 4), so SlotManager is STALE — predicting off it painted an old/deleted block for the
 *     round-trip (the G05-4 flicker). So on a remote session we read the server-synced
 *     ClientSlotCache instead, which is kept FRESH after every create/delete/variant by the
 *     HudSync.broadcast added in C1 (G05-1). Result: instant AND correct — no flicker, no stale
 *     guess. (This SUPERSEDES the earlier "bail on remote" stopgap, which traded instant for safe.)
 *
 * The block REGISTRY (SlotManager.blockAt) is identical on every client, so only the METADATA
 * source (current id / target index / glow) switches between the two paths.
 *
 *   - shares the server's own id math (ColorVariantService.variantId/stripColourSuffix) → the
 *     client can never drift from server truth;
 *   - mirrors the LIGHT (glow) blockstate property (clamped 0..15) → no relight flash;
 *   - predicts HITS only: if the target variant does not exist it paints nothing and lets the
 *     server speak ("make it with the Triangle first") — no wrong guess, no flicker;
 *   - never cancels the interaction (returns PASS) so the server round-trip is untouched — this
 *     does NOT reintroduce the "client-side skip delay on tools" pitfall (CLAUDE.md §7).
 *
 * Depends on: SlotManager (registry + glow, SP), ClientSlotCache (synced metadata, remote),
 *             SlotBlock.LIGHT/CLIENT_REMOTE_SESSION, ColorVariantService (shared id math), ColorSwapTool.
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

        // Pick the metadata source by session (see class header). Remote = the fresh server-synced
        // ClientSlotCache; SP / LAN host = the in-process SlotManager. The block registry is shared.
        if (SlotBlock.CLIENT_REMOTE_SESSION) predictFromCache(world, pos, slot, key);
        else                                  predictFromRegistry(world, pos, slot, key);
        return ActionResult.PASS;
    }

    /** Remote client: mirror swapPlaced off the server-synced ClientSlotCache (G06-6 — fresh, not the stale local list). */
    private static void predictFromCache(World world, BlockPos pos, SlotBlock slot, String key) {
        ClientSlotCache.Entry cur = ClientSlotCache.getEntry(slot.getSlotIndex());
        if (cur == null || cur.id() == null || cur.id().isEmpty()) return;
        String targetId = ColorVariantService.variantId(cur.id(), key);
        Integer targetIdx = ClientSlotCache.indexForId(targetId);
        if (targetIdx == null && "black".equals(key)) // Black Square → base block fallback (server mirror)
            targetIdx = ClientSlotCache.indexForId(ColorVariantService.stripColourSuffix(cur.id()));
        if (targetIdx == null || targetIdx == slot.getSlotIndex()) return; // miss / already that colour → defer
        ClientSlotCache.Entry t = ClientSlotCache.getEntry(targetIdx);
        paint(world, pos, targetIdx, t == null ? 0 : t.glow());
    }

    /** Singleplayer / LAN host: mirror swapPlaced off the in-process SlotManager (the live truth there). */
    private static void predictFromRegistry(World world, BlockPos pos, SlotBlock slot, String key) {
        SlotData cur = SlotManager.getBySlot(slot.getSlotKey());
        if (cur == null) return;
        SlotData target = SlotManager.getById(ColorVariantService.variantId(cur.customId(), key));
        if (target == null && "black".equals(key)) // Black Square falls back to the base block (server mirror)
            target = SlotManager.getById(ColorVariantService.stripColourSuffix(cur.customId()));
        if (target == null || target.index() == slot.getSlotIndex()) return; // miss / already that colour → defer
        paint(world, pos, target.index(), SlotManager.glowFor(target.index()));
    }

    /** Paint the predicted target block (carrying its glow) at {@code pos}; no-op if it isn't registered. */
    private static void paint(World world, BlockPos pos, int index, int glow) {
        SlotBlock targetBlock = SlotManager.blockAt(index);
        if (targetBlock == null) return; // registry hole shouldn't happen — never let a click crash
        int g = Math.max(0, Math.min(15, glow)); // LIGHT is IntProperty 0..15
        BlockState predicted = targetBlock.getDefaultState().with(SlotBlock.LIGHT, g); // same glow swapPlaced rides in
        // NOTIFY_LISTENERS | FORCE_STATE: redraw without neighbour churn, no client/server fighting.
        world.setBlockState(pos, predicted, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
    }
}
