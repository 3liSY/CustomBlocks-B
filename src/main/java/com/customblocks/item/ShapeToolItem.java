/**
 * ShapeToolItem.java
 *
 * The coloured Square / Triangle colour-variant tools (Group 06), rebuilt from scratch — only
 * the old textures are reused; this code is new. One shared class drives all eight variants
 * (Green / Yellow / Red / Black × Square / Triangle); each instance carries its own colour
 * and shape for naming, lore and the on-use behaviour.
 *
 * Behaviour (M2 + M3): TRIANGLES create a colour variant of the clicked custom block —
 * right-click makes it immediately, sneak+right-click opens a Yes/Info/No confirm first.
 * SQUARES swap the clicked PLACED block to that colour's EXISTING variant (never create);
 * the Black Square falls back to the base block when no "_black" variant exists.
 *
 * Depends on: SlotBlock/SlotData/SlotManager, ColorVariantService, GuiRouter/Nav
 */
package com.customblocks.item;

import com.customblocks.block.SlotBlock;
import com.customblocks.core.ColorVariantService;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.util.List;

public class ShapeToolItem extends Item {

    private final String colorName;  // "Green", "Yellow", "Red", "Black"
    private final String colorCode;  // §a / §e / §c / §8
    private final String shape;      // "Square" or "Triangle"

    public ShapeToolItem(Settings settings, String colorName, String colorCode, String shape) {
        super(settings);
        this.colorName = colorName;
        this.colorCode = colorCode;
        this.shape = shape;
    }

    /** Live name showing the configured hex, e.g. "Black Square [#0A0A0A]" (M3 hex). */
    @Override
    public Text getName(ItemStack stack) {
        return Text.literal(colorCode + colorName + " " + shape + " §8["
                + ColorVariantService.hexFor(colorName.toLowerCase(java.util.Locale.ROOT)) + "]");
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        if (!(ctx.getWorld().getBlockState(ctx.getBlockPos()).getBlock() instanceof SlotBlock slot)) {
            return ActionResult.PASS;
        }
        if (!(ctx.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS; // client swings instantly; server does the work
        }
        SlotData d = SlotManager.getBySlot(slot.getSlotKey());
        if (d == null) {
            return ActionResult.SUCCESS; // unassigned slot block; nothing to act on
        }
        String colourKey = colorName.toLowerCase(java.util.Locale.ROOT);
        if ("Triangle".equals(shape)) { // M2 — create the colour variant
            if (player.isSneaking()) { // confirm first
                GuiRouter.openFresh(player, Nav.MenuKey.of(Nav.Dest.RECOLOR_CONFIRM,
                        colourKey + ":" + d.customId()));
            } else {
                ColorVariantService.createVariant(player, d.customId(), colourKey);
            }
            return ActionResult.SUCCESS;
        }
        // Square (M3) — swap the placed block to this colour's existing variant.
        ColorVariantService.swapPlaced(player, ctx.getWorld(), ctx.getBlockPos(), d, colourKey);
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip,
                              net.minecraft.item.tooltip.TooltipType type) {
        if ("Triangle".equals(shape)) {
            tooltip.add(Text.literal("§7Right-click a custom block to create its "
                    + colorCode + colorName.toLowerCase() + " §7variant.").styled(s -> s.withItalic(false)));
            tooltip.add(Text.literal("§7The image's background is recoloured; the design stays.")
                    .styled(s -> s.withItalic(false)));
            tooltip.add(Text.literal("§8Sneak + right-click to see a confirm first.")
                    .styled(s -> s.withItalic(false)));
        } else {
            tooltip.add(Text.literal("§7Right-click a placed custom block to swap it to its "
                    + colorCode + colorName.toLowerCase() + " §7variant.").styled(s -> s.withItalic(false)));
            tooltip.add(Text.literal("§7Swaps only — create the variants with the Triangles.")
                    .styled(s -> s.withItalic(false)));
            if ("Black".equals(colorName)) {
                tooltip.add(Text.literal("§8No black variant? Swaps back to the original block.")
                        .styled(s -> s.withItalic(false)));
            }
        }
    }
}
