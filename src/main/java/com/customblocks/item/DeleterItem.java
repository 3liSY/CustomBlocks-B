/**
 * DeleterItem.java
 *
 * Responsibility: Right-click a placed custom block to delete it. Routes through the shared
 * {@link CbBlock} contract (G13-19), so it works the SAME on both families:
 *   - a slot block  → wipes its definition (texture/name/settings); the placed copy stays as a
 *     broken/empty block. Undoable via /cb undo.
 *   - an Arabic auto-join letter → removes that placed block and re-flows its neighbours.
 * Each block decides how to delete itself ({@code CbBlock.cbDelete}) and messages the player; this
 * item only resolves the target and runs the work server-side.
 *
 * Pitfall guard (Bible §9.6 "client-side skip"): we do NOT early-return on world.isClient — the
 * client path returns SUCCESS (the arm swings instantly) and only the server branch deletes.
 *
 * Depends on: CbBlock
 * Called by:  game on right-click with the tool item.
 */
package com.customblocks.item;

import com.customblocks.command.CbFmt;
import com.customblocks.block.CbBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class DeleterItem extends Item {

    public DeleterItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        if (!(ctx.getWorld().getBlockState(ctx.getBlockPos()).getBlock() instanceof CbBlock cb)) {
            return ActionResult.PASS; // not one of our blocks — behave like an empty hand
        }
        // Client returns SUCCESS so the arm swings with no delay; the server does the work.
        if (!(ctx.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        cb.cbDelete(player, ctx.getWorld(), ctx.getBlockPos());
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, java.util.List<Text> tooltip,
                              net.minecraft.item.tooltip.TooltipType type) {
        tooltip.add(Text.literal(CbFmt.DIM + "Right-click a custom block to erase its design —").styled(s -> s.withItalic(false)));
        tooltip.add(Text.literal(CbFmt.DIM + "texture, name and settings, all at once.").styled(s -> s.withItalic(false)));
        tooltip.add(Text.literal(CbFmt.FAINT + "Had second thoughts? " + CbFmt.DIM + "/cb undo " + CbFmt.FAINT + "restores everything.").styled(s -> s.withItalic(false)));
    }
}
