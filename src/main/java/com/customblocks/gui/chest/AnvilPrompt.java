/**
 * AnvilPrompt.java
 *
 * Responsibility: A reusable server-side anvil text prompt — the player "renames" a seed
 * item and takes the result to submit the typed text. Used wherever a chest menu needs
 * free-form input (hex colours, custom names) without any client-side mod. The algorithm
 * is recycled from the old project's proven AnvilPromptManager; this version hands the
 * result to a callback instead of a global input router.
 *
 * Submit  → onSubmit.accept(text) on the server thread (closing the anvil first).
 * Close/ESC without taking the output → onCancel.run() (skipped on disconnect).
 *
 * Depends on: vanilla AnvilScreenHandler only.
 * Called by:  HexColorsMenu (hex editing), CustomColorMenu (custom hex input).
 */
package com.customblocks.gui.chest;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public final class AnvilPrompt {

    private static final int MAX_LENGTH = 50;

    private AnvilPrompt() {} // static-only

    /** True when {@code handler} is one of our prompt anvils (pack pushes wait for those). */
    public static boolean isPrompt(net.minecraft.screen.ScreenHandler handler) {
        return handler instanceof PromptHandler;
    }

    /**
     * Open the prompt. {@code seedItem} is what the player "renames" (its look hints at the
     * field being edited); {@code initialText} pre-fills the name box.
     */
    public static void open(ServerPlayerEntity player, String title, ItemStack seedItem,
                            String initialText, Consumer<String> onSubmit, Runnable onCancel) {
        ItemStack seed = (seedItem == null || seedItem.isEmpty())
                ? new ItemStack(Items.NAME_TAG) : seedItem.copyWithCount(1);
        String text = initialText == null ? "" : initialText;
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new PromptHandler(syncId, inv, seed, text, onSubmit, onCancel),
                Text.literal(title)));
    }

    private static final class PromptHandler extends AnvilScreenHandler {
        private final ItemStack seedItem;
        private final Consumer<String> onSubmit;
        private final Runnable onCancel;
        private String currentText;
        private boolean done; // submit happened — onClosed must not also fire the cancel

        private PromptHandler(int syncId, PlayerInventory playerInventory, ItemStack seedItem,
                              String initialText, Consumer<String> onSubmit, Runnable onCancel) {
            super(syncId, playerInventory, ScreenHandlerContext.EMPTY);
            this.seedItem = seedItem.copyWithCount(1);
            this.onSubmit = onSubmit;
            this.onCancel = onCancel;
            this.currentText = clamp(initialText);
            this.input.setStack(0, named(this.seedItem, this.currentText));
            this.input.setStack(1, ItemStack.EMPTY);
            updateResult();
        }

        @Override
        public boolean setNewItemName(String newItemName) {
            this.currentText = clamp(newItemName);
            updateResult();
            return true;
        }

        @Override
        public void updateResult() {
            if (currentText.isBlank()) {
                this.output.setStack(0, ItemStack.EMPTY);
            } else {
                ItemStack result = seedItem.copyWithCount(1);
                result.set(DataComponentTypes.CUSTOM_NAME,
                        Text.literal(currentText).styled(s -> s.withItalic(false)));
                this.output.setStack(0, result);
            }
            sendContentUpdates();
        }

        @Override
        protected boolean canTakeOutput(PlayerEntity player, boolean present) {
            return present && !currentText.isBlank();
        }

        @Override
        protected void onTakeOutput(PlayerEntity player, ItemStack stack) {
            if (!(player instanceof ServerPlayerEntity sp)) return;
            String submitted = currentText.trim();
            if (submitted.isEmpty()) return;
            done = true;
            clearAll();
            setCursorStack(ItemStack.EMPTY);
            MinecraftServer server = sp.getServer();
            if (server == null) return;
            server.execute(() -> {
                sp.closeHandledScreen();
                onSubmit.accept(submitted);
            });
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
            if (slotIndex == 0 || slotIndex == 1) return; // the seed item stays put
            super.onSlotClick(slotIndex, button, actionType, player);
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
            clearAll(); // the prompt item is UI, never loot
            super.onClosed(player);
            if (player instanceof ServerPlayerEntity sp) {
                com.customblocks.network.ResourcePackServer.onGuiClosed(sp);
                if (!done && onCancel != null && !sp.isDisconnected()) {
                    done = true;
                    onCancel.run();
                }
            }
        }

        private void clearAll() {
            this.input.setStack(0, ItemStack.EMPTY);
            this.input.setStack(1, ItemStack.EMPTY);
            this.output.setStack(0, ItemStack.EMPTY);
        }

        private static ItemStack named(ItemStack base, String text) {
            ItemStack s = base.copyWithCount(1);
            s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(
                    text == null || text.isBlank() ? "Type here" : text)
                    .styled(st -> st.withItalic(false)));
            return s;
        }

        private static String clamp(String v) {
            if (v == null) return "";
            return v.length() > MAX_LENGTH ? v.substring(0, MAX_LENGTH) : v;
        }
    }
}
