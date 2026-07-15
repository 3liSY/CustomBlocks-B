/**
 * TomatoCommands.java — Group 32 (Explosive Tomato) Phase A ("Fly").
 *
 * /cb tomato <targets> [amount]   → hand out tomatoes using vanilla entity selectors (@s, @a, @p, a name)
 * /cb tomato all [amount]         → every online player
 *
 * Amount defaults to 1. Gated on vanilla OP level 2: Fabric has no permission system, so the Bukkit-era
 * `customblocks.tomato.give` node from the original request does not exist and is not emulated (G32 lock).
 *
 * Overflow is reported, never dropped on the ground — same contract as /cb give (GiveCommands).
 *
 * Depends on: Chat, TomatoRegistry
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.tomato.TomatoRegistry;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.List;

public final class TomatoCommands {

    private TomatoCommands() {} // static-only

    /** Same ceiling as /cb give — 100 full stacks. */
    private static final int MAX_AMOUNT = 6400;

    private static final SuggestionProvider<ServerCommandSource> AMOUNTS = (c, b) -> {
        b.suggest(1); b.suggest(16); b.suggest(32); b.suggest(64);
        return b.buildFuture();
    };

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("tomato")
                .requires(src -> src.hasPermissionLevel(2)) // A7 — OP level 2, refused for everyone else
                .then(CommandManager.literal("all")
                        .executes(ctx -> give(ctx, everyone(ctx), 1))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, MAX_AMOUNT))
                                .suggests(AMOUNTS)
                                .executes(ctx -> give(ctx, everyone(ctx), amount(ctx)))))
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                        .executes(ctx -> give(ctx, targets(ctx), 1))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, MAX_AMOUNT))
                                .suggests(AMOUNTS)
                                .executes(ctx -> give(ctx, targets(ctx), amount(ctx))))));
    }

    private static int amount(CommandContext<ServerCommandSource> ctx) {
        return IntegerArgumentType.getInteger(ctx, "amount");
    }

    private static Collection<ServerPlayerEntity> targets(CommandContext<ServerCommandSource> ctx)
            throws CommandSyntaxException {
        return EntityArgumentType.getPlayers(ctx, "targets");
    }

    /** Every online player — the `all` literal, so admins don't have to remember the @a selector. */
    private static Collection<ServerPlayerEntity> everyone(CommandContext<ServerCommandSource> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        return server == null ? List.of() : List.copyOf(server.getPlayerManager().getPlayerList());
    }

    /** Hand {@code amount} tomatoes to each target, reporting anything that didn't fit. */
    private static int give(CommandContext<ServerCommandSource> ctx, Collection<ServerPlayerEntity> targets, int amount) {
        ServerCommandSource src = ctx.getSource();
        if (targets.isEmpty()) {
            Chat.error(src, "No players matched — nobody got a tomato.");
            return 0;
        }

        int served = 0;
        int shortfall = 0;
        for (ServerPlayerEntity target : targets) {
            int notFit = insert(target, amount);
            int gave = amount - notFit;
            shortfall += notFit;
            if (gave <= 0) continue;
            served++;
            Chat.toPlayer(target, "You received " + gave + " x Explosive Tomato. Sneak + throw to ride it.");
        }

        if (served == 0) {
            Chat.error(src, "Nobody had room — every target's inventory is full.");
            return 0;
        }

        String who = (targets.size() == 1)
                ? targets.iterator().next().getName().getString()
                : served + " player(s)";
        Chat.success(src, "Gave " + amount + " x Explosive Tomato to " + who
                + (shortfall > 0 ? " (" + shortfall + " didn't fit — inventory full)" : "") + ".");
        return served;
    }

    /**
     * Insert {@code amount} tomatoes in 64-stacks. Returns how many did NOT fit (inventory full);
     * the caller reports that, so nothing is silently dumped on the floor.
     */
    private static int insert(ServerPlayerEntity player, int amount) {
        int max = new ItemStack(TomatoRegistry.ITEM).getMaxCount();
        int remaining = amount;
        while (remaining > 0) {
            int batch = Math.min(remaining, max);
            ItemStack stack = new ItemStack(TomatoRegistry.ITEM, batch);
            player.getInventory().insertStack(stack);
            int inserted = batch - stack.getCount(); // insertStack leaves the leftover in stack
            remaining -= inserted;
            if (inserted < batch) break; // nothing more fits
        }
        return remaining;
    }
}
