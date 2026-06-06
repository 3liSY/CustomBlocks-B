/**
 * ArabicBrowserScreen.java
 *
 * Responsibility: In-game GUI for the Arabic letter/word system.
 * Buttons import letters, create word blocks, and browse existing Arabic blocks.
 * Opens from MainMenuScreen → "Arabic Letters", or via GuiMode.ARABIC_BROWSER.
 * CLIENT-SIDE ONLY.
 *
 * Depends on: Screen, ButtonWidget, GuiEngine, GuiState
 * Called by: CustomBlocksClient (on OpenGuiPayload mode=ARABIC_BROWSER)
 */
package com.customblocks.gui.screens;

import com.customblocks.gui.GuiEngine;
import com.customblocks.gui.GuiState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class ArabicBrowserScreen extends Screen {

    private static final int BTN_W = 200;
    private static final int BTN_H = 20;

    public ArabicBrowserScreen() {
        super(Text.literal("Arabic Letters"));
    }

    @Override
    protected void init() {
        int y   = height / 2 - 60;
        int gap = 24;

        addDrawableChild(bRun("Import All 28 Letters", y,          "/cb arabic import"));
        addDrawableChild(bCmd("Import Single Letter...",y + gap,    "/cb arabic letter "));
        addDrawableChild(bCmd("Create Word Block...",   y + gap*2,  "/cb arabic word "));
        addDrawableChild(bRun("List Arabic Blocks",     y + gap*3,  "/cb arabic list"));
        addDrawableChild(bRun("Close  [Esc]",           y + gap*4 + 8, null));
    }

    private ButtonWidget bRun(String label, int y, String command) {
        return ButtonWidget.builder(Text.literal(label), b -> {
            if (command == null) close();
            else run(command);
        }).dimensions(width / 2 - BTN_W / 2, y, BTN_W, BTN_H).build();
    }

    private ButtonWidget bCmd(String label, int y, String prefix) {
        return ButtonWidget.builder(Text.literal(label), b -> suggest(prefix))
                .dimensions(width / 2 - BTN_W / 2, y, BTN_W, BTN_H).build();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        GuiEngine.drawBackground(ctx, width, height);
        GuiEngine.drawHeader(ctx, width);
        GuiEngine.drawTitle(ctx, textRenderer, title, width, 8);
        GuiEngine.drawSeparator(ctx, 0, width, 30);
        GuiEngine.drawLabel(ctx, textRenderer,
                "Tip: place arabtype.ttf in config/customblocks/ for full shaping", 6, height - 14);
        super.render(ctx, mx, my, delta);
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void close() {
        GuiState.get().reset();
        super.close();
    }

    private void suggest(String prefix) {
        close();
        MinecraftClient.getInstance().setScreen(new ChatScreen(prefix));
    }

    private void run(String cmd) {
        close();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.networkHandler.sendChatCommand(cmd.substring(1));
    }
}
