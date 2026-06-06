/**
 * LuminaBrushItem.java
 *
 * Responsibility: Right-click a custom block to cycle its glow (light emission) through a
 * handful of useful levels; sneak + right-click cycles backward. Reuses the same path as
 * /cb setglow, so the change persists, refreshes nearby placed copies, and is undoable.
 *
 * Depends on: CustomToolItem, SlotManager, SlotLighting, UndoManager, Chat
 */
package com.customblocks.item;

import com.customblocks.block.SlotLighting;
import com.customblocks.command.Chat;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.UndoManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LuminaBrushItem extends CustomToolItem {

    /** Glow levels the brush steps through (0 = off … 15 = max). */
    private static final int[] STEPS = {0, 4, 8, 12, 15};

    public LuminaBrushItem(Settings settings) {
        super(settings);
    }

    @Override
    protected void act(ServerPlayerEntity player, World world, BlockPos pos, SlotData d) {
        int next = player.isSneaking() ? prevStep(d.glow()) : nextStep(d.glow());
        SlotData updated = SlotManager.setGlow(d.customId(), next);
        if (updated == null) return;
        UndoManager.recordModify(player.getUuid(), d, updated, "glow");
        SlotLighting.applyToPlaced(player.getServer(), d.index(), next); // refresh placed copies
        Chat.tool(player, d.customId() + " glow → " + next);
    }

    private static int nextStep(int cur) {
        for (int s : STEPS) if (s > cur) return s;
        return STEPS[0]; // wrap past 15 back to 0
    }

    private static int prevStep(int cur) {
        for (int i = STEPS.length - 1; i >= 0; i--) if (STEPS[i] < cur) return STEPS[i];
        return STEPS[STEPS.length - 1]; // wrap below 0 back to 15
    }
}
