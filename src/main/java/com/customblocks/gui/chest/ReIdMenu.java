/**
 * ReIdMenu.java — the "change a block's id" GUI (reid slice B).
 *
 * Two ways in, one shared anvil:
 *   - build(player, page): a paginated block-picker (clone of BlockListMenu); clicking a block
 *     opens the anvil for that block's id.
 *   - openAnvil(player, id, onCancel): the shared anvil prompt, pre-filled with the current id.
 *     On submit it DELEGATES to the tested `/cb reid <id> <newId>` command (so the lock check,
 *     id-key migration, undo recording and chat feedback all behave exactly as the command does).
 *     On cancel it runs onCancel (reopen the source menu, or nothing from the bare command).
 *
 * Called by: ReIdCommands (no-arg picker, single-arg anvil), EditorMenu ("Change ID" button).
 */
package com.customblocks.gui.chest;

import com.customblocks.block.SlotBlock;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ReIdMenu {

    private ReIdMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, int page) {
        List<SlotData> all = new ArrayList<>(SlotManager.assignedSlots());
        all.sort(Comparator.comparingInt(SlotData::index));

        int per = Layout.PER_PAGE;
        int maxPage = all.isEmpty() ? 0 : (all.size() - 1) / per;
        int p = Math.max(0, Math.min(page, maxPage));

        ChestMenu m = new ChestMenu("Re-ID - pick a block", 6);
        if (all.isEmpty()) {
            m.set(22, Icons.of(Items.PAPER, "§7No blocks yet", "§8Create one with /cb create <id>"));
        }

        int start = p * per;
        for (int i = 0; i < per; i++) {
            int gi = start + i;
            if (gi >= all.size()) break;
            SlotData d = all.get(gi);
            m.set(i, icon(d), (pl, b, a) -> openAnvil(pl, d.customId(),
                    () -> GuiRouter.openFresh(pl, MenuKey.of(Dest.REID))));
        }

        Layout.pagedFooter(m, p, maxPage, Dest.REID, "", all.size());
        return m;
    }

    /**
     * Open the anvil to type a new id for {@code id}; submit runs the tested /cb reid command.
     * {@code onCancel} runs if the player closes the anvil without taking the result.
     */
    public static void openAnvil(ServerPlayerEntity player, String id, Runnable onCancel) {
        AnvilPrompt.open(player, "New id for " + id, new ItemStack(Items.NAME_TAG), id,
                newId -> {
                    MinecraftServer s = player.getServer();
                    if (s == null) return;
                    s.getCommandManager().executeWithPrefix(
                            player.getCommandSource(), "cb reid " + id + " " + newId.trim());
                },
                onCancel == null ? () -> {} : onCancel);
    }

    private static ItemStack icon(SlotData d) {
        SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
        Item display = item != null ? item : Items.STONE;
        return Icons.of(display, "§f" + d.displayName(),
                "§7id: §f" + d.customId(),
                "§7slot: §f" + d.index(),
                "§7category: §f" + (d.category().isEmpty() ? "none" : d.category()),
                "§eClick to change its id");
    }
}
