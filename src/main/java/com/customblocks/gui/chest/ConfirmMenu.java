/**
 * ConfirmMenu.java — reusable "Are you sure?" confirmation chest GUI (Group 02 finale fix).
 * Currently used to gate /cb config behind a Yes/No screen, mirroring the old version's
 * config-warning flow. "Yes" runs the underlying chat command; "No" returns to the previous
 * menu.
 */
package com.customblocks.gui.chest;

import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ConfirmMenu {

    private ConfirmMenu() {} // static-only

    /** Confirmation gate shown before opening the server config (mirrors the old version). */
    public static ChestMenu configWarning(ServerPlayerEntity player) {
        ChestMenu m = new ChestMenu("Open Server Config?", 3).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 18; i < 27; i++) m.set(i, Icons.accent());

        m.set(4, Icons.of(Items.LEVER, "§e§lServer Configuration",
                "§7You're about to open the live server config.",
                "§7Changes here can affect every player.",
                "§7Are you sure you want to continue?"));

        m.set(11, Icons.of(Items.RED_CONCRETE, "§c§lNo, go back", "§7Return to the dashboard"),
                (p, b, a) -> GuiRouter.back(p));

        m.set(15, Icons.of(Items.LIME_CONCRETE, "§a§lYes, continue", "§7Open the configuration"),
                (p, b, a) -> GuiRouter.runCommand(p, "config"));

        m.set(22, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }
}
