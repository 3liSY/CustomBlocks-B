/**
 * GuiMode.java
 *
 * Responsibility: Enum of all CustomBlocks in-game GUI screens. The int id is
 * what travels in OpenGuiPayload so the client knows which screen to open.
 *
 * Depends on: (none)
 * Called by: GuiCommands (chooses mode), CustomBlocksClient (dispatches to screen)
 */
package com.customblocks.gui;

public enum GuiMode {
    MAIN_MENU(0),
    BLOCK_EDITOR(1),
    CONFIG(2),
    TEMPLATE_LIST(3),
    ARABIC_BROWSER(4),
    MACRO_LIST(5),
    HUD_EDITOR(6);

    public final int id;

    GuiMode(int id) { this.id = id; }

    public static GuiMode fromId(int id) {
        for (GuiMode m : values()) if (m.id == id) return m;
        return MAIN_MENU;
    }
}
