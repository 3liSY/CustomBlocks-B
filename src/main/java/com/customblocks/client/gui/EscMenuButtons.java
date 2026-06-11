/**
 * EscMenuButtons.java
 *
 * Responsibility: Inject two CustomBlocks buttons ("CustomBlocks Menu" and "HUD Editor")
 * into the vanilla pause/ESC menu, positioned evenly below the bottom vanilla button.
 * Triggered from ScreenEvents.AFTER_INIT (Fabric) and added via ScreenInvoker so they
 * render + receive clicks exactly like vanilla buttons. CLIENT-SIDE ONLY.
 *
 * Depends on: Fabric Screen API (Screens), ScreenInvoker, CbIconButton, HudEditorScreen
 * Called by: CustomBlocksClient (ScreenEvents.AFTER_INIT)
 */
package com.customblocks.client.gui;

import com.customblocks.mixin.ScreenInvoker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.Items;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class EscMenuButtons {

    private EscMenuButtons() {}

    private static final int BTN_H = 20;
    private static final int GAP   = 4;

    public static void onScreenInit(MinecraftClient client, Screen screen) {
        if (!(screen instanceof GameMenuScreen)) return;

        // Anchor on the lowest existing vanilla button so we sit just below "Leave Game".
        List<ClickableWidget> existing = Screens.getButtons(screen);
        ClickableWidget anchor = null;
        for (ClickableWidget b : existing) {
            if (anchor == null || b.getY() > anchor.getY()) anchor = b;
        }
        int bw = anchor != null ? anchor.getWidth() : 204;
        int bx = anchor != null ? anchor.getX()     : screen.width / 2 - bw / 2;
        int baseY = anchor != null ? anchor.getY()  : screen.height / 2;

        int y1 = baseY + BTN_H + GAP;
        int y2 = y1 + BTN_H + GAP;

        // Keep on-screen even on short windows.
        int maxY = screen.height - BTN_H - 2;
        if (y2 > maxY) { y2 = maxY; y1 = y2 - BTN_H - GAP; }

        ScreenInvoker inv = (ScreenInvoker) screen;
        inv.customblocks$addDrawableChild(new CbIconButton(
                Items.COMMAND_BLOCK, "CustomBlocks Menu", bx, y1, bw, BTN_H,
                b -> openDashboard(client)));
        inv.customblocks$addDrawableChild(new CbIconButton(
                Items.COMMAND_BLOCK, "HUD Editor", bx, y2, bw, BTN_H,
                b -> client.setScreen(new HudEditorScreen())));
    }

    private static void openDashboard(MinecraftClient client) {
        if (client.player == null) return;
        client.setScreen(null);                              // close the pause menu
        client.player.networkHandler.sendChatCommand("cb");  // server opens the dashboard chest GUI
    }
}
