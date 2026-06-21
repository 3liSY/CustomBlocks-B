/**
 * ArabicPrewarm.java — Group 13 / Pass 4 (O10 lag fix).
 *
 * Builds the four contextual-form tiles of every Arabic letter item the player is CARRYING (hotbar +
 * offhand) ahead of placement, so the first frame a placed letter is shown its tile is already cached
 * — no synchronous build queued behind the placement, no slow letter-by-letter fill. Idempotent:
 * ArabicLetterBlockEntityRenderer.prewarm routes through textureFor, which skips keys already built or
 * in flight, so running this every client tick is cheap (a handful of cache-map lookups) once warm.
 *
 * Depends on: ArabicLetterBlockEntityRenderer.prewarm, ArabicLetterRegistry.ITEM, ArabicLetterBlock
 * Called by:  CustomBlocksClient (ClientTickEvents.END_CLIENT_TICK)
 */
package com.customblocks.client.render;

import com.customblocks.block.ArabicLetterBlock;
import com.customblocks.block.ArabicLetterBlockEntity;
import com.customblocks.block.ArabicLetterRegistry;
import com.customblocks.item.ColorSwapTool;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;

public final class ArabicPrewarm {

    private ArabicPrewarm() {}

    /** Warm the tiles of any Arabic letter item in the player's hotbar or offhand. Client thread. */
    public static void tick(MinecraftClient client) {
        if (client.player == null) return;
        for (int i = 0; i < 9; i++) warm(client.player.getInventory().getStack(i)); // hotbar
        warm(client.player.getOffHandStack());                                       // offhand
        warmRecolorTarget(client);                                                   // O11: looked-at letter
    }

    private static void warm(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isOf(ArabicLetterRegistry.ITEM)) return;
        ArabicLetterBlockEntityRenderer.prewarm(ArabicLetterBlock.letterOf(stack),
                ArabicLetterBlock.colorOf(stack));
    }

    /**
     * O11 recolour fix: when the player holds a colour Square and looks at a placed auto-join letter, warm
     * that letter's tiles (own glyph + back-face partner) in the Square's colour BEFORE the click — so the
     * recolour shows with no tile rebuild stall. Pairs with ClientArabicRecolorPredictor, which paints the
     * colour on click; this just makes sure the tile is already cached so the paint is instant.
     */
    private static void warmRecolorTarget(MinecraftClient client) {
        if (client.world == null) return;
        ItemStack held = client.player.getMainHandStack();
        if (!(held.getItem() instanceof ColorSwapTool tool)) return;
        String colour = tool.swapColourKey(held);
        if (colour == null) return; // a Triangle / colourless tool — nothing to warm
        if (!(client.crosshairTarget instanceof BlockHitResult bhr)) return;
        if (!(client.world.getBlockState(bhr.getBlockPos()).getBlock() instanceof ArabicLetterBlock)) return;
        if (!(client.world.getBlockEntity(bhr.getBlockPos()) instanceof ArabicLetterBlockEntity be)
                || be.letter() == 0) return;
        ArabicLetterBlockEntityRenderer.prewarm(be.letter(), colour);
        if (be.backLetter() != 0) ArabicLetterBlockEntityRenderer.prewarm(be.backLetter(), colour);
    }
}
