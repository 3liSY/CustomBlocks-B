/**
 * GuiState.java
 *
 * Responsibility: Per-session navigational state for the CustomBlocks GUI.
 * Holds the back-stack (ArrayDeque of GuiMode) and any cross-screen data
 * (selected block ID, server-sent data string).
 * Reset when the GUI is fully closed.
 * CLIENT-SIDE ONLY.
 *
 * Depends on: GuiMode
 * Called by: CustomBlocksClient (creates on packet), screen classes (navigate)
 */
package com.customblocks.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayDeque;
import java.util.Deque;

@Environment(EnvType.CLIENT)
public final class GuiState {

    private static final GuiState INSTANCE = new GuiState();
    public static GuiState get() { return INSTANCE; }

    private final Deque<GuiMode> backStack = new ArrayDeque<>();
    private String selectedBlockId = null;
    private String serverData      = null;

    private GuiState() {}

    /** Push the current mode before navigating forward. */
    public void push(GuiMode mode) { backStack.push(mode); }

    /** Pop and return the previous screen, or null if the stack is empty. */
    public GuiMode pop() { return backStack.isEmpty() ? null : backStack.pop(); }

    /** Clear stack and cross-screen data (call when the GUI is fully closed). */
    public void reset() {
        backStack.clear();
        selectedBlockId = null;
        serverData      = null;
    }

    public String getSelectedBlockId()        { return selectedBlockId; }
    public void   setSelectedBlockId(String v){ selectedBlockId = v; }

    public String getServerData()             { return serverData; }
    public void   setServerData(String v)     { serverData = v; }
}
