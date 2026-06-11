/**
 * CbChestHandler.java
 *
 * Server-side screen handler backing every CustomBlocks chest menu. Renders as a vanilla
 * generic container (no client mod needed) but is fully read-only: every container-slot
 * click is routed to the ChestMenu's action map and NEVER moves items. We never call
 * super.onSlotClick and quickMove returns EMPTY, so menu items can't be extracted or duped.
 *
 * Modeled on the proven handler pattern from the previous CustomBlocks build.
 */
package com.customblocks.gui.chest;

import com.customblocks.CustomBlocksMod;
import com.customblocks.command.Chat;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class CbChestHandler extends GenericContainerScreenHandler {

    private final ChestMenu menu;
    private boolean disposed = false;

    public CbChestHandler(int syncId, PlayerInventory playerInventory, ChestMenu menu) {
        super(typeFor(menu.rows()), syncId, playerInventory, menu.inventory(), menu.rows());
        this.menu = menu;
    }

    /** True once the player has closed this screen (so the router won't refresh a dead handler). */
    public boolean isDisposed() { return disposed; }

    /** Row count of the menu currently backing this handler. */
    public int menuRows() { return menu.rows(); }

    /** Title of the menu currently backing this handler. */
    public String menuTitle() { return menu.title(); }

    /**
     * Update the visible contents IN PLACE (slots + click actions) and sync to the client,
     * WITHOUT reopening the screen. Reopening is what snaps the mouse cursor back to the
     * centre of the screen, so same-menu updates (toggles, paging) go through here instead.
     */
    public void refreshWith(ChestMenu newMenu) {
        SimpleInventory inv = menu.inventory();
        SimpleInventory src = newMenu.inventory();
        int count = Math.min(inv.size(), src.size());
        for (int i = 0; i < count; i++) {
            inv.setStack(i, src.getStack(i));
        }
        menu.adoptActions(newMenu);
        this.syncState();
    }

    private static ScreenHandlerType<GenericContainerScreenHandler> typeFor(int rows) {
        return switch (rows) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            default -> ScreenHandlerType.GENERIC_9X6;
        };
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < menu.size() && player instanceof ServerPlayerEntity sp) {
            try {
                menu.click(sp, slotIndex, button, actionType);
            } catch (Exception e) {
                CustomBlocksMod.LOGGER.error("[CustomBlocks] Chest GUI click error in slot {}", slotIndex, e);
                sp.sendMessage(Text.literal(Chat.PREFIX + "§cSomething went wrong. The action was not applied."), true);
                sp.closeHandledScreen();
                return;
            }
        }
        // Never call super: the whole menu (and the player inventory while open) is read-only.
        this.syncState();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        boolean first = !disposed;
        disposed = true;
        if (first && player instanceof ServerPlayerEntity sp) {
            menu.closed(sp);
            // A held pack push (deferred reload safety) can go out now the menu is gone.
            com.customblocks.network.ResourcePackServer.onGuiClosed(sp);
        }
    }
}
