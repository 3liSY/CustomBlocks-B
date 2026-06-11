/**
 * OmniToolItem.java
 *
 * The Omni-Tool (Group 06) — one item that merges the old Lumina Brush (Glow), Chisel
 * (Hardness) and the Diamond-Triangle wand (Eyedrop) into a single hot-swappable tool.
 *
 *   Right-click a custom block  → run the active mode's action.
 *   Sneak + right-click a block → open the Omni-Tool config GUI (switch mode).
 *
 * The active mode is stored per-player in OmniToolState. Because sneak+right-click opens
 * the GUI, the Omni-Tool's Glow/Hardness cycles are forward-only (the standalone Brush /
 * Chisel keep their sneak-to-reverse behaviour).
 *
 * Depends on: OmniToolState, SlotBlock/SlotData/SlotManager, SlotLighting, UndoManager,
 *             GuiRouter (open config GUI), Chat
 */
package com.customblocks.item;

import com.customblocks.block.SlotBlock;
import com.customblocks.block.SlotLighting;
import com.customblocks.command.Chat;
import com.customblocks.core.OmniToolState;
import com.customblocks.core.OmniToolState.Mode;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.UndoManager;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class OmniToolItem extends Item {

    private static final int[] GLOW_STEPS = {0, 4, 8, 12, 15};
    private static final float[] HARD_STEPS = {0f, 0.5f, 1.5f, 5f, 20f, -1f};
    private static final String[] HARD_LABELS = {"instant", "soft", "stone", "hard", "tough", "unbreakable"};

    public OmniToolItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        if (!(ctx.getWorld().getBlockState(ctx.getBlockPos()).getBlock() instanceof SlotBlock slot)) {
            return ActionResult.PASS; // not our block — act like an empty hand
        }
        // Client returns SUCCESS so the arm swings with no delay; the server does the work.
        if (!(ctx.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        SlotData d = SlotManager.getBySlot(slot.getSlotKey());
        if (d == null) {
            return ActionResult.SUCCESS; // unassigned slot block; nothing to edit
        }
        if (player.isSneaking()) {
            GuiRouter.openFresh(player, Nav.MenuKey.of(Nav.Dest.OMNI));
            return ActionResult.SUCCESS;
        }
        switch (OmniToolState.getMode(player.getUuid())) {
            case GLOW     -> cycleGlow(player, d);
            case HARDNESS -> cycleHardness(player, d);
            case AREA     -> RainbowRectangleItem.markArea(player, ctx.getBlockPos());
        }
        return ActionResult.SUCCESS;
    }

    /**
     * Group 06 fix: the sneak-click "switch mode" gesture must also work in the air (and on
     * any non-custom block), not only when aimed at a custom block. useOnBlock already
     * handles the sneak-click on a custom block (returns SUCCESS, so this never double-fires
     * there); this covers air + non-custom blocks. A plain (non-sneak) air-click has nothing
     * to act on, so it passes through.
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!user.isSneaking()) {
            return TypedActionResult.pass(stack); // normal air-click: no block to edit
        }
        // Client swings instantly; the server opens the config GUI (mirrors useOnBlock).
        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.success(stack);
        }
        GuiRouter.openFresh(player, Nav.MenuKey.of(Nav.Dest.OMNI));
        return TypedActionResult.success(stack);
    }

    // ── Mode actions ─────────────────────────────────────────────────────────

    private static void cycleGlow(ServerPlayerEntity player, SlotData d) {
        int next = GLOW_STEPS[0];
        for (int s : GLOW_STEPS) { if (s > d.glow()) { next = s; break; } }
        SlotData u = SlotManager.setGlow(d.customId(), next);
        if (u == null) return;
        UndoManager.recordModify(player.getUuid(), d, u, "glow");
        SlotLighting.applyToPlaced(player.getServer(), d.index(), next);
        Chat.tool(player, "§eGlow §f" + d.customId() + " → " + next);
    }

    private static void cycleHardness(ServerPlayerEntity player, SlotData d) {
        int i = hardIndex(d.hardness());
        int n = (i + 1) % HARD_STEPS.length;
        SlotData u = SlotManager.setHardness(d.customId(), HARD_STEPS[n]);
        if (u == null) return;
        UndoManager.recordModify(player.getUuid(), d, u, "hardness");
        Chat.tool(player, "§7Hardness §f" + d.customId() + " → " + HARD_LABELS[n]);
    }

    private static int hardIndex(float h) {
        for (int i = 0; i < HARD_STEPS.length; i++) if (Float.compare(HARD_STEPS[i], h) == 0) return i;
        return 2; // "stone" — sensible default for an off-preset value
    }

    // ── Display helpers (mode shown in the item's name) ──────────────────────

    /** Set the stack's name to reflect the given mode, e.g. "Omni-Tool [Glow Mode]". */
    public static void applyName(ItemStack stack, Mode mode) {
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§bOmni-Tool " + mode.color + "[" + mode.label + " Mode]")
                .styled(s -> s.withItalic(false)));
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, java.util.List<Text> tooltip,
                              net.minecraft.item.tooltip.TooltipType type) {
        tooltip.add(Text.literal("§7Three trusted tools, forged into one.").styled(s -> s.withItalic(false)));
        tooltip.add(Text.literal("§7Right-click a custom block to apply the current mode.").styled(s -> s.withItalic(false)));
        tooltip.add(Text.literal("§7Sneak + right-click anywhere to switch:").styled(s -> s.withItalic(false)));
        tooltip.add(Text.literal("§eGlow §8· §7Hardness §8· §6Area").styled(s -> s.withItalic(false)));
    }
}
