/**
 * ConfigScreen.java
 *
 * Responsibility: Display current CustomBlocks config values and provide
 * quick-fill buttons for the most common settings.
 * CLIENT-SIDE ONLY.
 *
 * Depends on: Screen, ButtonWidget, GuiEngine, GuiState, CustomBlocksConfig
 * Called by: CustomBlocksClient (on OpenGuiPayload mode=CONFIG)
 */
package com.customblocks.gui.screens;

import com.customblocks.CustomBlocksConfig;
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
public class ConfigScreen extends Screen {

    private static final int BTN_W = 180;
    private static final int BTN_H = 18;

    public ConfigScreen() {
        super(Text.literal("CustomBlocks Config"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y  = height / 2 - 70;
        int gap = 22;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Undo Mode: " + CustomBlocksConfig.undoMode),
                b -> cmd("/cb config undomode")
        ).dimensions(cx - BTN_W / 2, y, BTN_W, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("HUD: " + (CustomBlocksConfig.hudEnabled ? "ON" : "OFF")),
                b -> cmd("/cb config hud " + (CustomBlocksConfig.hudEnabled ? "off" : "on"))
        ).dimensions(cx - BTN_W / 2, y + gap, BTN_W, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Texture Size: " + CustomBlocksConfig.textureSize + "px"),
                b -> suggest("/cb config texturesize ")
        ).dimensions(cx - BTN_W / 2, y + gap * 2, BTN_W, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Max Undo Depth: " + CustomBlocksConfig.maxUndoDepth),
                b -> suggest("/cb config maxundodepth ")
        ).dimensions(cx - BTN_W / 2, y + gap * 3, BTN_W, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("HTTP Host: " + CustomBlocksConfig.httpHost),
                b -> suggest("/cb config httphost ")
        ).dimensions(cx - BTN_W / 2, y + gap * 4, BTN_W, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Show /cb config"),
                b -> cmd("/cb config")
        ).dimensions(cx - BTN_W / 2, y + gap * 5, BTN_W, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Close  [Esc]"),
                b -> close()
        ).dimensions(cx - BTN_W / 2, y + gap * 6 + 8, BTN_W, BTN_H).build());
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

    private void suggest(String prefix) {
        close();
        MinecraftClient.getInstance().setScreen(new ChatScreen(prefix));
    }
}
