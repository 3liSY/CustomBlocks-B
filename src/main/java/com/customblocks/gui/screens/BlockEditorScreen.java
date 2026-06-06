/**
 * BlockEditorScreen.java
 *
 * Responsibility: Show all attributes of a custom block and provide action buttons.
 * Buttons either run a command directly (give, delete) or pre-fill the chat box
 * with the command so the player can add a value (retexture, setglow, etc.).
 * The block ID is carried from the server in OpenGuiPayload.data.
 * CLIENT-SIDE ONLY.
 *
 * Depends on: Screen, ButtonWidget, GuiEngine, GuiState
 * Called by: CustomBlocksClient (on OpenGuiPayload mode=BLOCK_EDITOR)
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
public class BlockEditorScreen extends Screen {

    private static final int BTN_W = 155;
    private static final int BTN_H = 18;

    private final String id;

    public BlockEditorScreen(String blockId) {
        super(Text.literal("Block Editor — " + blockId));
        this.id = blockId;
    }

    @Override
    protected void init() {
        int left  = width / 2 - BTN_W - 4;
        int right = width / 2 + 4;
        int y = height / 2 - 52;
        int gap = 22;

        // Left column
        addDrawableChild(bCmd("Retexture...",   left,  y,         "/cb retexture "   + id + " "));
        addDrawableChild(bCmd("Set Glow...",    left,  y + gap,   "/cb setglow "     + id + " "));
        addDrawableChild(bCmd("Set Hardness...",left,  y + gap*2, "/cb sethardness " + id + " "));
        addDrawableChild(bCmd("Set Sound...",   left,  y + gap*3, "/cb setsound "    + id + " "));

        // Right column
        addDrawableChild(bCmd("Set Collision...",right, y,         "/cb setcollision "+ id + " "));
        addDrawableChild(bCmd("Set Category...", right, y + gap,   "/cb setcategory " + id + " "));
        addDrawableChild(bCmd("Add Note...",     right, y + gap*2, "/cb note "        + id + " "));
        addDrawableChild(bRun("Give to Me",      right, y + gap*3, "/cb give "        + id));

        // Bottom row
        addDrawableChild(bCmd("Rename...",       left,  y + gap*4 + 6, "/cb rename " + id + " "));
        addDrawableChild(bRun("Delete Block",    right, y + gap*4 + 6, "/cb delete " + id));
        addDrawableChild(bRun("← Back",     width/2 - BTN_W/2, y + gap*5 + 12, null));
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        GuiEngine.drawBackground(ctx, width, height);
        GuiEngine.drawHeader(ctx, width);
        GuiEngine.drawTitle(ctx, textRenderer, title, width, 8);
        GuiEngine.drawSeparator(ctx, 0, width, 30);
        GuiEngine.drawLabel(ctx, textRenderer, "Click a button, or Esc to close", 6, height - 14);
        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        GuiState.get().reset();
        super.close();
    }

    /** Button that pre-fills the chat box with a command prefix. */
    private ButtonWidget bCmd(String label, int x, int y, String prefix) {
        return ButtonWidget.builder(Text.literal(label), b -> suggest(prefix))
                .dimensions(x, y, BTN_W, BTN_H).build();
    }

    /** Button that runs a command directly (pass null to close without command). */
    private ButtonWidget bRun(String label, int x, int y, String command) {
        return ButtonWidget.builder(Text.literal(label), b -> {
            if (command == null) close();
            else run(command);
        }).dimensions(x, y, BTN_W, BTN_H).build();
    }

    private void suggest(String cmd) {
        close();
        MinecraftClient.getInstance().setScreen(new ChatScreen(cmd));
    }

    private void run(String cmd) {
        close();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.networkHandler.sendChatCommand(cmd.substring(1));
    }
}
