/**
 * ClientArabicRecolorPredictor.java — Group 13 / O11 (slow recolour fix). CLIENT-ONLY.
 *
 * Makes a colour-Square recolour of a placed auto-join Arabic letter feel INSTANT. The recolour is
 * server-authoritative (ShapeToolItem.recolorArabicLetter sets the BlockEntity colour + syncs) — the
 * delay the player saw was the network round-trip PLUS a tile rebuild for the new colour (the renderer
 * caches one tile per letter+form+colour, and the new colour was never warmed). This client listener
 * removes both: on the same tick as the click it (1) warms the target-colour tiles off-thread and
 * (2) paints the new colour onto the CLIENT BlockEntity. The server still does the real recolour;
 * because the prediction is the identical colour, the authoritative sync reconciles with no change.
 *
 * Mirrors ADR-009's ClientSwapPredictor (which left Arabic recolour "for later, if needed"). It never
 * cancels the interaction (always PASS) so the server round-trip is untouched — no "client-side skip
 * delay on tools" pitfall (CLAUDE.md §7). On a vanilla client there is no listener → round-trip as before.
 *
 * Depends on: item/ColorSwapTool, ArabicLetterBlock, ArabicLetterBlockEntity,
 *             client/render/ArabicLetterBlockEntityRenderer (tile prewarm).
 * Called by:  CustomBlocksClient.onInitializeClient (register()).
 *
 * See: docs/adr/ADR-009-instant-colour-swap-client-prediction.md
 */
package com.customblocks.client;

import com.customblocks.block.ArabicLetterBlock;
import com.customblocks.block.ArabicLetterBlockEntity;
import com.customblocks.client.render.ArabicLetterBlockEntityRenderer;
import com.customblocks.item.ColorSwapTool;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class ClientArabicRecolorPredictor {

    private ClientArabicRecolorPredictor() {}

    /** Hook the predictor into right-click-on-block. Registered once from the client entrypoint. */
    public static void register() {
        UseBlockCallback.EVENT.register(ClientArabicRecolorPredictor::onUse);
    }

    private static ActionResult onUse(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        // Visual prediction only — always PASS so vanilla still sends the use to the server.
        if (!world.isClient) return ActionResult.PASS;
        ItemStack stack = player.getStackInHand(hand);
        if (!(stack.getItem() instanceof ColorSwapTool tool)) return ActionResult.PASS;
        String colour = tool.swapColourKey(stack);
        if (colour == null) return ActionResult.PASS; // a Triangle / colourless tool — nothing to predict

        BlockPos pos = hit.getBlockPos();
        if (!(world.getBlockState(pos).getBlock() instanceof ArabicLetterBlock)) return ActionResult.PASS;
        if (!(world.getBlockEntity(pos) instanceof ArabicLetterBlockEntity be) || be.letter() == 0) return ActionResult.PASS;
        if (colour.equals(be.color())) return ActionResult.PASS; // already that colour — server says "Already…"

        // Warm the target-colour tiles (own glyph + the back-face partner) so the swap shows with no rebuild
        // stall, then paint the colour on the client BE. The BlockEntityRenderer redraws every frame, so it
        // picks up the new colour next frame; the server's authoritative recolour syncs the same colour back.
        ArabicLetterBlockEntityRenderer.prewarm(be.letter(), colour);
        if (be.backLetter() != 0) ArabicLetterBlockEntityRenderer.prewarm(be.backLetter(), colour);
        be.setColor(colour);
        return ActionResult.PASS;
    }
}
