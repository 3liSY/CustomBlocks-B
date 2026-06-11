/**
 * ChestMenu.java
 *
 * A server-side chest menu: a SimpleInventory of decorated items plus a per-slot click
 * handler map. Rendered to vanilla clients as a generic container via CbChestHandler, so
 * no client code is required (Group 02 design). Read-only — clicks never move items.
 */
package com.customblocks.gui.chest;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public final class ChestMenu {

    /** Click callback for a menu slot. button: 0 = left, 1 = right. */
    public interface Click {
        void run(ServerPlayerEntity player, int button, SlotActionType action);
    }

    private final String title;
    private final int rows;
    private final SimpleInventory inventory;
    private final Map<Integer, Click> actions = new HashMap<>();
    private java.util.function.Consumer<ServerPlayerEntity> onClose;

    public ChestMenu(String title, int rows) {
        this.title = title;
        this.rows = Math.max(1, Math.min(6, rows));
        this.inventory = new SimpleInventory(this.rows * 9);
    }

    public int rows() { return rows; }
    public int size() { return rows * 9; }
    public String title() { return title; }
    SimpleInventory inventory() { return inventory; }

    /** Replace this menu's click actions with another menu's (used for in-place refresh). */
    void adoptActions(ChestMenu other) {
        this.actions.clear();
        this.actions.putAll(other.actions);
        this.onClose = other.onClose;
    }

    /** Run something when the screen showing this menu closes (any reason: button, ESC, Yes). */
    public ChestMenu onClose(java.util.function.Consumer<ServerPlayerEntity> callback) {
        this.onClose = callback;
        return this;
    }

    /** Invoked once by the handler when the screen is closed. */
    void closed(ServerPlayerEntity player) {
        if (onClose != null) onClose.accept(player);
    }

    /** Fill every slot with a blank gray pane (call first, then place content). */
    public ChestMenu fill() {
        for (int i = 0; i < size(); i++) inventory.setStack(i, Icons.filler());
        return this;
    }

    public ChestMenu set(int slot, ItemStack stack) {
        if (slot >= 0 && slot < size() && stack != null) inventory.setStack(slot, stack);
        return this;
    }

    public ChestMenu set(int slot, ItemStack stack, Click click) {
        set(slot, stack);
        if (click != null && slot >= 0 && slot < size()) actions.put(slot, click);
        return this;
    }

    /** Invoked by the handler when a container slot is clicked. */
    void click(ServerPlayerEntity player, int slot, int button, SlotActionType action) {
        Click c = actions.get(slot);
        if (c != null) c.run(player, button, action);
    }

    /** Open this menu for the player (call on the server thread). */
    public void open(ServerPlayerEntity player) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, p) -> new CbChestHandler(syncId, playerInventory, this),
                Text.literal(title)));
    }
}
