/**
 * SlotBlock.java
 *
 * Responsibility: The generic Minecraft Block that backs one of the pre-registered
 * slots. It holds only its slot index/key; its name (and later its texture, shape,
 * attributes) come from the SlotData assigned to it in SlotManager.
 *
 * Phase 1 (foundation): minimal — registration + display name only. Sounds, shapes,
 * glow, drops, and Arabic joining are added in their phases. Keep this file small.
 *
 * Depends on: SlotData, SlotManager
 * Called by:  SlotManager.registerAll() (registration), the game (rendering/naming)
 */
package com.customblocks.block;

import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class SlotBlock extends Block {

    private final int slotIndex;
    private final String slotKey;

    public SlotBlock(int slotIndex, Settings settings) {
        super(settings);
        this.slotIndex = slotIndex;
        this.slotKey = "slot_" + slotIndex;
    }

    public int getSlotIndex() { return slotIndex; }
    public String getSlotKey() { return slotKey; }

    @Override
    public MutableText getName() {
        SlotData d = SlotManager.getBySlot(slotKey);
        String name = (d != null) ? d.displayName() : null;
        return Text.literal(name != null ? name : "Custom Block " + slotIndex);
    }

    /** The matching BlockItem for a slot, named from the same SlotData. */
    public static class SlotItem extends BlockItem {
        private final String slotKey;

        public SlotItem(SlotBlock block, Item.Settings settings) {
            super(block, settings);
            this.slotKey = block.getSlotKey();
        }

        @Override
        public Text getName(ItemStack stack) {
            SlotData d = SlotManager.getBySlot(slotKey);
            String name = (d != null) ? d.displayName() : null;
            return Text.literal(name != null ? name : "Custom Block");
        }
    }
}
