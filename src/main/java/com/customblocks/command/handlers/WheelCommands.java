/**
 * WheelCommands.java - Group 34 (Wheel of Fortune) v2 item F (commands).
 *
 * The {@code /cb wheel ...} handler, registered from {@link com.customblocks.command.CommandRegistrar} like
 * every other {@code *Commands.java}:
 *
 *   /cb wheel        -> put a wheel item in your inventory (place it to build the wheel)
 *   /cb wheel list   -> how many prizes are in the pool, plus a few examples
 *
 * There is no {@code place} or {@code spin} leaf any more (v2, 2026-07-23): the wheel is placed like a normal
 * block so it can face the placer, and it is spun by right-clicking it. The command backup is deferred scope.
 *
 * Depends on: WheelBlockRegistry, WheelPool, Chat
 * Called by:  CommandRegistrar.register()
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.wheel.WheelBlockRegistry;
import com.customblocks.wheel.WheelPool;
import com.customblocks.wheel.WheelRing;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public final class WheelCommands {

    private WheelCommands() {} // static-only

    /** How many pool examples {@code list} prints. */
    private static final int EXAMPLES = 8;

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("wheel")
                .executes(WheelCommands::give)
                .then(CommandManager.literal("give").executes(WheelCommands::give))
                .then(CommandManager.literal("list").executes(WheelCommands::list)));
    }

    // ------------------------------------------------------------------ give

    /** /cb wheel - hand over the wheel item. Placing it is what builds the wheel, facing the placer. */
    private static int give(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        ItemStack stack = new ItemStack(WheelBlockRegistry.ITEM);
        if (!player.getInventory().insertStack(stack)) {
            Chat.error(src, "No room in your inventory for the wheel item.");
            return 0;
        }
        Chat.success(src, "Wheel item added. Place it in a clear area about "
                + (int) (WheelRing.OUTER_R * 2) + " blocks wide, facing the way you want the wheel to face.");
        return 1;
    }

    // ------------------------------------------------------------------ list

    /** /cb wheel list - report how many prizes the ring draws from, with a few examples. */
    private static int list(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        List<Item> pool = WheelPool.items();
        Chat.info(src, "Wheel of Fortune - " + pool.size() + " possible prizes (survival vanilla items); "
                + WheelRing.SLICES + " of them are re-rolled onto the ring every spin.");
        int examples = Math.min(EXAMPLES, pool.size());
        StringBuilder sb = new StringBuilder("Examples: ");
        for (int i = 0; i < examples; i++) {
            if (i > 0) sb.append(", ");
            sb.append(pool.get(i).getName().getString());
        }
        if (pool.size() > examples) sb.append(", ...");
        Chat.raw(src, sb.toString());
        return 1;
    }
}
