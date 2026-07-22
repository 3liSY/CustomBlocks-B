/**
 * RainbowRectangleItem.java
 *
 * Rainbow Rectangle (Group 06) — the per-face paint tool (M4).
 * Right-click one FACE of a custom block → chat opens pre-filled with
 * "/cb setface <id> <face> " so the player only pastes an image URL — only that face
 * changes.
 *
 * G06-18 (2026-07-16): the old sneak+right-click Area/corner-select was a misread of this tool's
 * job (owner: "it should repaint faces not area") — removed entirely. This is a single-block,
 * per-face paint tool only; there is no corner-marking / bulk-area behaviour.
 *
 * Depends on: SlotBlock/SlotData/SlotManager, ChatPrefillPayload, Chat
 */
package com.customblocks.item;

import com.customblocks.command.CbFmt;
import com.customblocks.block.SlotBlock;
import com.customblocks.command.Chat;
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
        // M4 — paint the clicked face: pre-fill the command, the player pastes the URL.
        SlotData d = SlotManager.getBySlot(slot.getSlotKey());
        if (d == null) {
            return ActionResult.SUCCESS; // unassigned slot block; nothing to paint
        }
        String face = ctx.getSide().getName(); // down/up/north/south/west/east
        Chat.tool(player, "Painting the " + face + " face of " + d.customId()
                + " — paste the image URL and press Enter.");
        ServerPlayNetworking.send(player, new ChatPrefillPayload(
                "/cb setface " + d.customId() + " " + face + " "));
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip,
                              net.minecraft.item.tooltip.TooltipType type) {
        tooltip.add(Text.literal(CbFmt.DIM + "Paints ONE face of a custom block.").styled(s -> s.withItalic(false)));
        tooltip.add(Text.literal(CbFmt.DIM + "Right-click a face → paste an image URL in chat;").styled(s -> s.withItalic(false)));
        tooltip.add(Text.literal(CbFmt.DIM + "only that face changes (" + CbFmt.BODY + "/cb clearface " + CbFmt.DIM + "undoes it).").styled(s -> s.withItalic(false)));
    }
}
