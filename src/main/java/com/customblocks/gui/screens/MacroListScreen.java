/**
 * MacroListScreen.java
 *
 * Responsibility: In-game GUI for macro management.
 * Shows action buttons that run /cb macro commands or pre-fill the chat box.
 * Opens from MainMenuScreen → "Macros", or via GuiMode.MACRO_LIST.
 * CLIENT-SIDE ONLY.
 *
 * Depends on: Screen, ButtonWidget, GuiEngine, GuiState
 * Called by: CustomBlocksClient (on OpenGuiPayload mode=MACRO_LIST)
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
public class MacroListScreen extends Screen {

    private static final int BTN_W = 200;
    private static final int BTN_H = 20;

    public MacroListScreen() {
        super(Text.literal("Macro Manager"));
    }

    @Override
    protected void init() {
        int y   = height / 2 - 75;
        int gap = 24;

        addDrawableChild(bRun("View All Macros",     y,          "/cb macro list"));
        addDrawableChild(bCmd("Record New Macro...", y + gap,    "/cb macro record "));
        addDrawableChild(bCmd("Play Macro...",       y + gap*2,  "/cb macro play "));
        addDrawableChild(bCmd("Delete Macro...",     y + gap*3,  "/cb macro delete "));
        addDrawableChild(bRun("Add Step (recording)",y + gap*4,  "/cb macro add "));
        addDrawableChild(bRun("Stop Recording",      y + gap*5,  "/cb macro stop"));
        addDrawableChild(bRun("Close  [Esc]",        y + gap*6 + 8, null));
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
        GuiEngine.drawLabel(ctx, textRenderer, "Results appear in chat", 6, height - 14);
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
