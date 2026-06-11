/**
 * RainbowRectangleItem.java
 *
 * Rainbow Rectangle (Group 06) — the per-face paint tool (M4) + area selector.
 * Right-click one FACE of a custom block → chat opens pre-filled with
 * "/cb paintface <id> <face> " so the player only pastes an image URL — only that face
 * changes. Sneak + right-click keeps the corner-marking selector (corner 1 / corner 2),
 * which also powers the Omni-Tool's Area mode.
 *
 * Depends on: AreaSelection, SlotBlock/SlotData/SlotManager, ChatPrefillPayload, Chat
 */
package com.customblocks.item;

import com.customblocks.block.SlotBlock;
import com.customblocks.command.Chat;
import com.customblocks.core.AreaSelection;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.network.payloads.ChatPrefillPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class RainbowRectangleItem extends Item {

    public RainbowRectangleItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        if (!(ctx.getWorld().getBlockState(ctx.getBlockPos()).getBlock() instanceof SlotBlock slot)) {
            return ActionResult.PASS;
        }
        if (!(ctx.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS; // client swings instantly; server does the work
        }
        if (player.isSneaking()) { // the area selector lives on sneak now
            markArea(player, ctx.getBlockPos());
            return ActionResult.SUCCESS;
        }
        // M4 — paint the clicked face: pre-fill the command, the player pastes the URL.
        SlotData d = SlotManager.getBySlot(slot.getSlotKey());
        if (d == null) {
            return ActionResult.SUCCESS; // unassigned slot block; nothing to paint
        }
        String face = ctx.getSide().getName(); // down/up/north/south/west/east
        Chat.tool(player, "§7Painting the §f" + face + "§7 face of §f" + d.customId()
                + "§7 — paste the image URL and press Enter.");
        ServerPlayNetworking.send(player, new ChatPrefillPayload(
                "/cb paintface " + d.customId() + " " + face + " "));
        return ActionResult.SUCCESS;
    }

    /** Shared corner-marking feedback (also used by the Omni-Tool's Area mode). */
    public static void markArea(ServerPlayerEntity player, BlockPos pos) {
        AreaSelection.Result r = AreaSelection.mark(player.getUuid(), pos);
        if (r.firstCorner()) {
            Chat.tool(player, "§6Corner 1 set §7at " + fmt(pos) + " §8— right-click another block for corner 2");
        } else {
            Chat.tool(player, "§6Area selected §7" + fmt(r.a()) + " → " + fmt(r.b())
                    + " §8(" + r.volume() + " blocks)");
        }
    }

    private static String fmt(BlockPos p) {
        return "(" + p.getX() + ", " + p.getY() + ", " + p.getZ() + ")";
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip,
                              net.minecraft.item.tooltip.TooltipType type) {
        tooltip.add(Text.literal("§7Paints ONE face of a custom block.").styled(s -> s.withItalic(false)));
        tooltip.add(Text.literal("§7Right-click a face → paste an image URL in chat;").styled(s -> s.withItalic(false)));
        tooltip.add(Text.literal("§7only that face changes (§f/cb clearface §7undoes it).").styled(s -> s.withItalic(false)));
        tooltip.add(Text.literal("§8Sneak + right-click marks area corners (Omni Area mode).").styled(s -> s.withItalic(false)));
    }
}
