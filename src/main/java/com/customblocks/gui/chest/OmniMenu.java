/**
 * OmniMenu.java — the Omni-Tool config GUI (Group 06), opened by sneak + right-click with
 * the Omni-Tool on a custom block. Three mode buttons (Glow / Hardness / Eyedrop); the
 * active one glows. Clicking a mode switches it for this player, renames the held Omni-Tool
 * to match, and refreshes the menu in place.
 */
package com.customblocks.gui.chest;

import com.customblocks.core.OmniToolState;
import com.customblocks.core.OmniToolState.Mode;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import com.customblocks.item.OmniToolItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class OmniMenu {

    private OmniMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player) {
        Mode current = OmniToolState.getMode(player.getUuid());
        ChestMenu m = new ChestMenu("Omni-Tool", 3).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 18; i < 27; i++) m.set(i, Icons.accent());

        m.set(4, Icons.glint(Items.BLAZE_ROD, "§b§lOmni-Tool",
                "§7Active mode: " + current.color + current.label,
                "§7Right-click a block to use it.",
                "§7Sneak + right-click to reopen this menu."));

        m.set(11, modeIcon(Items.GLOWSTONE, Mode.GLOW, current,
                "§7Right-click cycles a block's glow", "§70 → 4 → 8 → 12 → 15 → 0"),
                (p, b, a) -> switchMode(p, Mode.GLOW));
        m.set(13, modeIcon(Items.IRON_INGOT, Mode.HARDNESS, current,
                "§7Right-click cycles a block's hardness", "§7instant → soft → stone → … → unbreakable"),
                (p, b, a) -> switchMode(p, Mode.HARDNESS));
        m.set(15, modeIcon(Items.PRISMARINE_SHARD, Mode.AREA, current,
                "§7Right-click two blocks to mark a box", "§7(the Rainbow Rectangle's area select)"),
                (p, b, a) -> switchMode(p, Mode.AREA));

        m.set(22, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    private static ItemStack modeIcon(net.minecraft.item.Item item, Mode mode, Mode current,
                                      String line1, String line2) {
        boolean active = mode == current;
        String name = mode.color + "§l" + mode.label + " Mode" + (active ? " §a(active)" : "");
        return active
                ? Icons.glint(item, name, line1, line2, "§8Currently selected")
                : Icons.of(item, name, line1, line2, "§aClick §7→ switch to " + mode.label);
    }

    private static void switchMode(ServerPlayerEntity player, Mode mode) {
        OmniToolState.setMode(player.getUuid(), mode);
        renameHeldOmniTool(player, mode);
        GuiRouter.openFresh(player, MenuKey.of(Dest.OMNI)); // refresh in place
    }

    /** Update the name of any Omni-Tool in the player's hands so it shows the new mode. */
    private static void renameHeldOmniTool(ServerPlayerEntity player, Mode mode) {
        ItemStack main = player.getMainHandStack();
        ItemStack off = player.getOffHandStack();
        if (main.getItem() instanceof OmniToolItem) OmniToolItem.applyName(main, mode);
        if (off.getItem() instanceof OmniToolItem) OmniToolItem.applyName(off, mode);
    }
}
