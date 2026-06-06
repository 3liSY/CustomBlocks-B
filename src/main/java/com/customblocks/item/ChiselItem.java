/**
 * ChiselItem.java
 *
 * Responsibility: Right-click a custom block to cycle its break hardness through named
 * presets (instant → soft → stone → hard → tough → unbreakable → instant …); sneak +
 * right-click cycles backward. Reuses /cb sethardness, so it persists and is undoable.
 *
 * (Shape editing — the chisel's other old-project role — waits for the shape system.)
 *
 * Depends on: CustomToolItem, SlotManager, UndoManager, Chat
 */
package com.customblocks.item;

import com.customblocks.command.Chat;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.UndoManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ChiselItem extends CustomToolItem {

    /** Ordered hardness presets the chisel cycles through (-1 = unbreakable). */
    private static final float[] STEPS = {0f, 0.5f, 1.5f, 5f, 20f, -1f};
    private static final String[] LABELS = {"instant", "soft", "stone", "hard", "tough", "unbreakable"};

    public ChiselItem(Settings settings) {
        super(settings);
    }

    @Override
    protected void act(ServerPlayerEntity player, World world, BlockPos pos, SlotData d) {
        int i = indexOf(d.hardness());
        int n = player.isSneaking() ? (i - 1 + STEPS.length) % STEPS.length : (i + 1) % STEPS.length;
        SlotData updated = SlotManager.setHardness(d.customId(), STEPS[n]);
        if (updated == null) return;
        UndoManager.recordModify(player.getUuid(), d, updated, "hardness");
        Chat.tool(player, d.customId() + " hardness → " + LABELS[n]);
    }

    /** Index of the preset matching the current hardness, defaulting to "stone". */
    private static int indexOf(float h) {
        for (int i = 0; i < STEPS.length; i++) if (Float.compare(STEPS[i], h) == 0) return i;
        return 2; // "stone" — a sensible starting point for an off-preset value
    }
}
