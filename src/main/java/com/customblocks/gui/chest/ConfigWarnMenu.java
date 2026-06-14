/**
 * ConfigWarnMenu.java — the Yes/No gate shown before the server-config GUI opens.
 *
 * Opening /cb config (or the dashboard Config button) lands here first: a short "are you sure"
 * screen, since the config screen changes live server behaviour. Yes swaps this screen for the
 * real ConfigMenu (so Back from config goes home, not back to this warning); No backs out.
 *
 * Depends on: ChestMenu, Icons, GuiRouter, GuiFx, Nav.
 * Called by:  GuiRouter.build (Dest.CONFIG_CONFIRM); reached from /cb config and MainMenu.
 */
package com.customblocks.gui.chest;

import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ConfigWarnMenu {

    private ConfigWarnMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player) {
        ChestMenu m = new ChestMenu("Open server config?", 3).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 18; i < 27; i++) m.set(i, Icons.accent());

        m.set(13, Icons.of(Items.LEVER, "§6§lServer Configuration",
                "§7You're about to open the live server settings.",
                "§7Changes here take effect immediately for everyone.",
                "§8Only the glowing entries are editable in the GUI.",
                "§eContinue?"));

        m.set(11, Icons.glint(Items.LIME_DYE, "§a§lYes — open config",
                        "§7Go to the configuration screen."),
                (p, b, a) -> { GuiFx.open(p); GuiRouter.repage(p, MenuKey.of(Dest.CONFIG)); });

        m.set(15, Icons.of(Items.RED_DYE, "§c§lNo — go back",
                        "§7Don't open the config screen."),
                (p, b, a) -> { GuiFx.click(p); GuiRouter.back(p); });

        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }
}
