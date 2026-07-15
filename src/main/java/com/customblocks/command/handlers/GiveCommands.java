/**
 * GiveCommands.java
 *
 * Responsibility: /cb give — hand a custom block's item to a player.
 *   /cb give <id>                  → 1 to yourself (preserved old behaviour)
 *   /cb give <id> <amount>         → amount (1–6400) to yourself; overflow reported, not dropped
 *   /cb give <id> <amount> <player>→ to an online player (OP / permission-level-2 only)
 *
 * Split out of UtilityCommands (Group 17 slice 2) so that file stays under the 400-line
 * command-handler cap (§9.3). Inventory inserts go in 64-stacks; anything that doesn't fit is
 * counted and reported ("N didn't fit — inventory full"), never dropped on the ground.
 *
 * Depends on: SlotData, SlotManager, SlotBlock.SlotItem, Chat, BlockSuggestions
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.block.SlotBlock;
import com.customblocks.command.Chat;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.onboarding.FirstUseHints;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class GiveCommands {

    private GiveCommands() {} // static-only

    /** Largest amount a single /cb give may hand out (100 full stacks). */
    private static final int MAX_AMOUNT = 6400;

    /** Tab-complete samples for the <amount> argument. */
    private static final SuggestionProvider<ServerCommandSource> AMOUNTS = (c, b) -> {
        b.suggest(1); b.suggest(16); b.suggest(32); b.suggest(64);
        return b.buildFuture();
    };

    /** Tab-complete online player names for the <player> argument. */
    private static final SuggestionProvider<ServerCommandSource> ONLINE_PLAYERS = (c, b) -> {
        MinecraftServer s = c.getSource().getServer();
        if (s != null) {
            for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) b.suggest(p.getName().getString());
        }
        return b.buildFuture();
    };

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("give")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> giveSelf(ctx, id(ctx), 1))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, MAX_AMOUNT))
                                .suggests(AMOUNTS)
                                .executes(ctx -> giveSelf(ctx, id(ctx), amount(ctx)))
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .suggests(ONLINE_PLAYERS)
                                        .executes(ctx -> giveTo(ctx, id(ctx), amount(ctx),
                                                StringArgumentType.getString(ctx, "player")))))));
    }

    private static String id(CommandContext<ServerCommandSource> ctx) {
        return StringArgumentType.getString(ctx, "id");
    }

    private static int amount(CommandContext<ServerCommandSource> ctx) {
        return IntegerArgumentType.getInteger(ctx, "amount");
    }

    /** /cb give <id> [amount] — to the caller. */
    private static int giveSelf(CommandContext<ServerCommandSource> ctx, String id, int amount) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        SlotData d = block(src, id);
        if (d == null) return 0;
        Item item = SlotManager.itemAt(d.index());
        if (item == null) { itemMissing(src, id); return 0; }
        ServerPlayerEntity player = src.getPlayerOrThrow();
        int notFit = insert(player, item, amount);
        int gave = amount - notFit;
        if (gave <= 0) {
            Chat.error(src, "Couldn't give any " + d.displayName() + " — your inventory is full.");
            return 0;
        }
        Chat.success(src, notFit > 0
                ? "Gave " + gave + " × " + d.displayName() + " (" + notFit + " didn't fit — inventory full)."
                : "Gave you " + gave + " × " + d.displayName() + ".");
        FirstUseHints.onFirstGive(player); // Group 23: one-time offhand-preview hint
        return 1;
    }

    /** /cb give <id> <amount> <player> — to an online player (OP only). */
    private static int giveTo(CommandContext<ServerCommandSource> ctx, String id, int amount, String name) {
        ServerCommandSource src = ctx.getSource();
        if (!src.hasPermissionLevel(2)) {
            Chat.error(src, "Only operators can give blocks to other players. (/cb give " + id + " " + amount + " gives to you.)");
            return 0;
        }
        SlotData d = block(src, id);
        if (d == null) return 0;
        Item item = SlotManager.itemAt(d.index());
        if (item == null) { itemMissing(src, id); return 0; }
        MinecraftServer server = src.getServer();
        ServerPlayerEntity target = server == null ? null : server.getPlayerManager().getPlayer(name);
        if (target == null) {
            Chat.error(src, "No online player named \"" + name + "\". They must be online to receive blocks.");
            return 0;
        }
        int notFit = insert(target, item, amount);
        int gave = amount - notFit;
        if (gave <= 0) {
            Chat.error(src, target.getName().getString() + "'s inventory is full — nothing was given.");
            return 0;
        }
        String tName = target.getName().getString();
        Chat.success(src, "Gave " + gave + " × " + d.displayName() + " to " + tName
                + (notFit > 0 ? " (" + notFit + " didn't fit — their inventory is full)" : "") + ".");
        Chat.success(target.getCommandSource(), "You received " + gave + " × " + d.displayName() + " from " + src.getName() + ".");
        return 1;
    }

    /**
     * Insert {@code amount} of {@code item} into the player's inventory in 64-stacks. Returns how
     * many did NOT fit (inventory full). Overflow is reported by the caller, never dropped.
     */
    private static int insert(ServerPlayerEntity player, Item item, int amount) {
        int max = new ItemStack(item).getMaxCount();
        int remaining = amount;
        while (remaining > 0) {
            int batch = Math.min(remaining, max);
            ItemStack stack = new ItemStack(item, batch);
            player.getInventory().insertStack(stack);
            int inserted = batch - stack.getCount(); // insertStack leaves the leftover in stack
            remaining -= inserted;
            if (inserted < batch) break; // nothing more fits — inventory full
        }
        return remaining;
    }

    /** Resolve an id to its SlotData, or send the standard "no such block" error and return null. */
    private static SlotData block(ServerCommandSource src, String id) {
        SlotData d = SlotManager.getById(id);
        if (d == null) Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
        return d;
    }

    private static void itemMissing(ServerCommandSource src, String id) {
        Chat.error(src, "The item for \"" + id + "\" couldn't be found — try /cb reload, "
                + "and report this if it keeps happening.");
    }
}
