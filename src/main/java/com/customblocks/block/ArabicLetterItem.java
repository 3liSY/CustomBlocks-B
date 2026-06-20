/**
 * ArabicLetterItem.java — Group 13 / O6. The BlockItem for the single joinable letter block.
 *
 * Exists only to COMPUTE the stack's display name at render time from its NBT (letter, colour, form)
 * via {@link ArabicNaming}, instead of reading a baked CUSTOM_NAME (ADR-006). Because the name is
 * computed, editing a form label (/cb config arabicforms) re-labels every held / stored stack
 * instantly, with no resource-pack reload. A held auto-join stack has no neighbours yet, so it shows
 * its isolated name ("Jeem Black"); a fixed-form decoration variant shows its locked form's name.
 *
 * Depends on: ArabicLetterBlock (NBT readers), ArabicNaming, ArabicJoining
 * Called by:  ArabicLetterRegistry (registers this instead of a plain BlockItem)
 */
package com.customblocks.block;

import com.customblocks.arabic.ArabicJoining;
import com.customblocks.arabic.ArabicNaming;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class ArabicLetterItem extends BlockItem {

    public ArabicLetterItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        char letter = ArabicLetterBlock.letterOf(stack);
        if (letter == 0) return super.getName(stack); // not stamped yet — fall back to the block name
        int locked = ArabicLetterBlock.lockedFormOf(stack);
        int form = (locked >= 0) ? locked : ArabicJoining.ISOLATED; // unplaced auto-join shows isolated
        return Text.literal(ArabicNaming.displayName(letter, ArabicLetterBlock.colorOf(stack), form));
    }
}
