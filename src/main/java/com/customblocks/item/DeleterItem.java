/**
 * DeleterItem.java
 *
 * Responsibility: Right-click a placed custom block to wipe its entire definition —
 * deletes the slot data + texture, pushes a resource-pack rebuild. The placed block in
 * the world stays but reverts to the unassigned-slot appearance. The operation is fully
 * undoable via /cb undo (which restores the definition + texture + rebuilds the pack).
 *
 * The physical block is NOT broken: the user may want to retexture or recreate and keep
 * the placed copy. Deleting the definition is the authoritative action, not breaking an
 * instance.
 *
 * Depends on: CustomToolItem, Chat, SlotManager, TextureStore, UndoManager, ResourcePackServer
 * Called by:  game on right-click with the tool item.
 */
package com.customblocks.item;

import com.customblocks.command.Chat;
import com.customblocks.core.BlockNotesManager;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.UndoManager;
import com.customblocks.network.ResourcePackServer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DeleterItem extends CustomToolItem {

    public DeleterItem(Settings settings) {
        super(settings);
    }

    @Override
    protected void act(ServerPlayerEntity player, World world, BlockPos pos, SlotData d) {
        if (LockManager.isLocked(d.customId())) {
            Chat.tool(player, "§c'" + d.customId() + "' is locked — /cb unlock " + d.customId() + " first");
            return;
        }
        // Load texture BEFORE delete() removes it from TextureStore so undo can restore it.
        byte[] texture = TextureStore.load(d.index());
        SlotManager.delete(d.customId());
        BlockNotesManager.onBlockDeleted(d.customId()); // clean up orphaned note if any
        ResourcePackServer.updatePack();
        UndoManager.recordDelete(player.getUuid(), d, texture);
        Chat.tool(player, "Deleted " + d.customId());
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, java.util.List<Text> tooltip,
                              net.minecraft.item.tooltip.TooltipType type) {
        tooltip.add(Text.literal("§7Right-click a custom block to erase its design —").styled(s -> s.withItalic(false)));
        tooltip.add(Text.literal("§7texture, name and settings, all at once.").styled(s -> s.withItalic(false)));
        tooltip.add(Text.literal("§8Had second thoughts? §7/cb undo §8restores everything.").styled(s -> s.withItalic(false)));
    }
}
