/**
 * ToolCommands.java
 *
 * Responsibility: Tool-give shortcuts (Group 06). The Brush + Chisel were merged into the
 * Omni-Tool, so `/cb brush` and `/cb chisel` now give the Omni-Tool with the matching mode
 * pre-selected; `/cb omni` gives it in its current mode. `/cb deleter` still gives the
 * separate Deleter (unchanged by request).
 *
 * The Squares / Triangles and their shortcuts are intentionally NOT added here.
 *
 * Depends on: ToolItems, OmniToolItem/OmniToolState, Chat
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.OmniToolState;
import com.customblocks.core.OmniToolState.Mode;
import com.customblocks.item.OmniToolItem;
import com.customblocks.item.ToolItems;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ToolCommands {

    private ToolCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("omni")
                .executes(ctx -> giveOmni(ctx, null)));
        root.then(CommandManager.literal("brush")
                .executes(ctx -> giveOmni(ctx, Mode.GLOW)));
        root.then(CommandManager.literal("chisel")
                .executes(ctx -> giveOmni(ctx, Mode.HARDNESS)));
        root.then(CommandManager.literal("rectangle")
                .executes(ToolCommands::giveRectangle));
        root.then(CommandManager.literal("deleter")
                .executes(ToolCommands::giveDeleter));
    }

    /** Give the Omni-Tool. If mode != null, also pre-select that mode for the player. */
    private static int giveOmni(CommandContext<ServerCommandSource> ctx, Mode mode) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            Chat.error(src, "Run this as a player — tools go into your inventory.");
            return 0;
        }
        if (ToolItems.OMNI_TOOL == null) {
            Chat.error(src, "The Omni-Tool isn't available right now. Try /cb reload.");
            return 0;
        }
        Mode active = mode != null ? mode : OmniToolState.getMode(player.getUuid());
        if (mode != null) OmniToolState.setMode(player.getUuid(), mode);
        ItemStack stack = new ItemStack(ToolItems.OMNI_TOOL);
        OmniToolItem.applyName(stack, active);
        player.getInventory().insertStack(stack);
        Chat.success(src, "Gave you the Omni-Tool in " + active.label + " mode. Right-click a "
                + "custom block to use it; sneak + right-click to switch modes.");
        return 1;
    }

    private static int giveRectangle(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            Chat.error(src, "Run this as a player — tools go into your inventory.");
            return 0;
        }
        if (ToolItems.RAINBOW_RECTANGLE == null) {
            Chat.error(src, "The Rainbow Rectangle isn't available right now. Try /cb reload.");
            return 0;
        }
        player.getInventory().insertStack(new ItemStack(ToolItems.RAINBOW_RECTANGLE));
        Chat.success(src, "Gave you the Rainbow Rectangle. Right-click two custom blocks to mark an "
                + "area's corners.");
        return 1;
    }

    private static int giveDeleter(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            Chat.error(src, "Run this as a player — tools go into your inventory.");
            return 0;
        }
        if (ToolItems.DELETER == null) {
            Chat.error(src, "The Deleter isn't available right now. Try /cb reload.");
            return 0;
        }
        player.getInventory().insertStack(new ItemStack(ToolItems.DELETER));
        Chat.success(src, "Gave you the Deleter. Right-click a placed custom block to delete its "
                + "definition (undoable with /cb undo).");
        return 1;
    }
}
