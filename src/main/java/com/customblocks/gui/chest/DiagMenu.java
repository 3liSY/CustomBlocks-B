/**
 * DiagMenu.java — system diagnostics GUI (finale fix; mirrors the old stats/maintenance
 * dashboard). Shows live gauges for slots, history and the texture server, plus the full
 * read-only snapshot from DiagnosticsHelper.collect (heap, TPS, pack size). A refresh
 * button rebuilds the menu in place; no system state is ever mutated.
 */
package com.customblocks.gui.chest;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.core.DiagnosticsHelper;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public final class DiagMenu {

    private DiagMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player) {
        ChestMenu m = new ChestMenu("Diagnostics", 3).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 18; i < 27; i++) m.set(i, Icons.accent());

        m.set(4, Icons.glint(Items.COMPARATOR, "§b§lSystem Diagnostics",
                "§7A live read-only snapshot of CustomBlocks."));

        int used = SlotManager.usedSlots();
        int max = CustomBlocksConfig.maxSlots;
        m.set(10, Icons.of(Items.CHEST, "§e§lBlock Slots",
                "§7Used: §f" + used + " §7/ §f" + max,
                "§7Free: §a" + Math.max(0, max - used)));

        m.set(12, Icons.of(Items.REPEATER, "§e§lHistory",
                "§7Mode: §f" + ("per_player".equals(CustomBlocksConfig.undoMode) ? "per-player" : "server-wide"),
                "§7Depth: §f" + CustomBlocksConfig.maxUndoDepth + " steps"));

        m.set(14, Icons.of(Items.BEACON, "§e§lTexture Server",
                "§7Address: §f" + CustomBlocksConfig.httpHost + ":" + CustomBlocksConfig.httpPort,
                "§7Texture size: §f" + CustomBlocksConfig.textureSize + "px"));

        // Full report from the shared helper (heap, TPS, pack size, …); drop the header line.
        List<String> lines = DiagnosticsHelper.collect(player.getServer());
        String[] lore = lines.stream().filter(l -> !l.contains("===")).toArray(String[]::new);
        m.set(16, Icons.of(Items.KNOWLEDGE_BOOK, "§b§lFull Report", lore));

        m.set(18, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(22, Icons.glint(Items.CLOCK, "§a§lRefresh", "§7Re-read the latest snapshot"),
                (p, b, a) -> GuiRouter.repage(p, MenuKey.of(Dest.DIAG)));
        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }
}
