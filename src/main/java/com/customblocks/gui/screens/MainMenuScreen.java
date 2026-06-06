/**
 * MainMenuScreen.java
 *
 * Responsibility: The top-level CustomBlocks GUI hub. Shows navigation buttons for
 * all major features. Buttons run /cb commands on the server so all existing
 * permission and validation logic is reused.
 * CLIENT-SIDE ONLY.
 *
 * Depends on: Screen, ButtonWidget, GuiEngine, GuiState
 * Called by: CustomBlocksClient (on OpenGuiPayload mode=MAIN_MENU)
 */
package com.customblocks.gui.screens;

import com.customblocks.gui.GuiEngine;
import com.customblocks.gui.GuiState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class MainMenuScreen extends Screen {

    private static final int BTN_W = 200;
    private static final int BTN_H = 20;

    public MainMenuScreen() {
        super(Text.literal("CustomBlocks"));
    }

    @Override
    protected void init() {
        int y  = height / 2 - 80;
        int gap = 24;

        addDrawableChild(btn("Block List",       y,          () -> cmd("/cb list")));
        addDrawableChild(btn("Categories",       y + gap,    () -> cmd("/cb categories")));
        addDrawableChild(btn("Templates",        y + gap*2,  () -> cmd("/cb template list")));
        addDrawableChild(btn("Macros",           y + gap*3,  () -> cmd("/cb gui macros")));
        addDrawableChild(btn("Arabic Letters",   y + gap*4,  () -> cmd("/cb gui arabic")));
        addDrawableChild(btn("Diagnostics",      y + gap*5,  () -> cmd("/cb diag")));
        addDrawableChild(btn("Close  [Esc]",     y + gap*6 + 8, this::close));
    }

    private ButtonWidget btn(String label, int y, Runnable action) {
        return ButtonWidget.builder(Text.literal(label), b -> action.run())
                .dimensions(width / 2 - BTN_W / 2, y, BTN_W, BTN_H)
                .build();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        GuiEngine.drawBackground(ctx, width, height);
        GuiEngine.drawHeader(ctx, width);
        GuiEngine.drawTitle(ctx, textRenderer, title, width, 8);
        GuiEngine.drawSeparator(ctx, 0, width, 30);
        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        GuiState.get().reset();
        super.close();
    }

    private void cmd(String command) {
        close();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.networkHandler.sendChatCommand(command.substring(1));
    }
}
