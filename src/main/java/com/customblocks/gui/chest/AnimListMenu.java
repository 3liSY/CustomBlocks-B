/**
 * AnimListMenu.java — Group 14 Phase 2: the animated-blocks-only picker behind /cb anim (no id).
 *
 * Lists ONLY animated blocks (SlotData.isAnimated()), paginated. Clicking one closes the chest and
 * opens the Block Creation Studio on that block's Animation tab in edit mode — the same
 * CreationStudioBridge.openStudioEdit rail /cb anim <id> uses. This replaces the old behaviour where
 * /cb anim opened the FULL block list and a click toggled a bulk ✔ / opened the chest editor (neither
 * was animated-only nor landed on the Animation tab — owner-reported 2026-06-19).
 *
 * Depends on: SlotManager, SlotData, CreationStudioBridge, Layout, Icons, ChestMenu, Nav.
 * Called by:  GuiRouter.build (Dest.ANIM_LIST), opened by AnimCommands.openList.
 */
package com.customblocks.gui.chest;

import com.customblocks.block.SlotBlock;
import com.customblocks.command.handlers.CreationStudioBridge;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AnimListMenu {

    private AnimListMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, int page) {
        List<SlotData> shown = new ArrayList<>();
        for (SlotData d : SlotManager.assignedSlots()) if (d.isAnimated()) shown.add(d);
        shown.sort(Comparator.comparingInt(SlotData::index));

        int per = Layout.PER_PAGE;
        int maxPage = shown.isEmpty() ? 0 : (shown.size() - 1) / per;
        int p = Math.max(0, Math.min(page, maxPage));

        ChestMenu m = new ChestMenu("Animated Blocks", 6);
        if (shown.isEmpty()) {
            m.set(22, Icons.of(Items.PAPER, "§7No animated blocks yet",
                    "§8Make one with /cb create <id> <name> <gif-url>",
                    "§8then it shows up here to edit."));
        }

        int start = p * per;
        for (int i = 0; i < per; i++) {
            int gi = start + i;
            if (gi >= shown.size()) break;
            SlotData d = shown.get(gi);
            m.set(i, icon(d), (pl, b, a) -> openEdit(pl, d.customId()));
        }

        Layout.pagedFooter(m, p, maxPage, Dest.ANIM_LIST, "", shown.size());
        return m;
    }

    /** Close the chest and open the studio on this block's Animation tab (edit mode). Deferred to the
     *  server thread so it never closes/opens a screen re-entrantly inside the slot-click handler. */
    private static void openEdit(ServerPlayerEntity player, String id) {
        MinecraftServer s = player.getServer();
        if (s == null) return;
        s.execute(() -> {
            player.closeHandledScreen();
            CreationStudioBridge.openStudioEdit(player.getCommandSource(), id);
        });
    }

    private static net.minecraft.item.ItemStack icon(SlotData d) {
        SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
        Item display = item != null ? item : Items.STONE;
        int frames = d.anim() != null ? d.anim().frameCount() : 0;
        return Icons.glint(display, "§f" + d.displayName(),
                "§7id: §f" + d.customId(),
                "§7slot: §f" + d.index(),
                "§7frames: §f" + frames,
                "§eClick §8to edit its animation");
    }
}
