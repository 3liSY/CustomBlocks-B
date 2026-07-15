/**
 * BlueprintCommands.java
 *
 * Responsibility: the local Blueprint share path (Group 12).
 *   /cb exportblock <id>   — give the player a Blueprint item carrying that block's recipe.
 *   /cb importblock        — recreate the block from a Blueprint held in the main hand.
 *   /cb importblock <code> — import by vault share code.
 *
 * Recipe (de)serialisation reuses BlockExporter so the Blueprint, the single-block JSON and the
 * category ZIP all speak the same schema. Stays well under the 400-line handler gate (§9.3).
 *
 * Depends on: Blueprint, BlockExporter, SlotManager, ResourcePackServer, Chat, CloudCommands
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.BlockExporter;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.item.Blueprint;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class BlueprintCommands {

    private BlueprintCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("exportblock")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> exportBlock(ctx, StringArgumentType.getString(ctx, "id")))));

        root.then(CommandManager.literal("importblock")
                .executes(BlueprintCommands::importFromHand)
                .then(CommandManager.argument("code", StringArgumentType.word())
                        .executes(ctx -> importByCode(ctx, StringArgumentType.getString(ctx, "code")))));
    }

    /** /cb exportblock <id> — give the player a Blueprint of that block. */
    private static int exportBlock(CommandContext<ServerCommandSource> ctx, String id) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.getById(id);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        ServerPlayerEntity player = src.getPlayerOrThrow();
        player.getInventory().insertStack(Blueprint.create(d));
        Chat.success(src, "Made a Blueprint of " + CbFmt.VALUE + id + CbFmt.RESET + " — it's in your inventory. "
                + "Hand it to a friend; they run " + CbFmt.DIM + "/cb importblock" + CbFmt.RESET + " while holding it.");
        return 1;
    }

    /** /cb importblock — recreate the block from a Blueprint held in the main hand. */
    private static int importFromHand(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        ItemStack held = player.getMainHandStack();
        String json = Blueprint.json(held);
        if (json == null) {
            Chat.error(src, "Hold a Blueprint in your main hand first, then run /cb importblock.");
            return 0;
        }
        BlockExporter.ImportResult r = BlockExporter.importJson(json);
        if (!r.created().isEmpty()) {
            ResourcePackServer.updatePack();
            Chat.success(src, "Imported " + CbFmt.VALUE + String.join(", ", r.created()) + CbFmt.RESET + " from the Blueprint.");
            return 1;
        }
        if (!r.skipped().isEmpty()) {
            Chat.info(src, "Block \"" + String.join(", ", r.skipped()) + "\" already exists — "
                    + "delete or rename it first, then import again.");
            return 1;
        }
        Chat.error(src, "Couldn't read that Blueprint: " + String.join("; ", r.failed()));
        return 0;
    }

    /** /cb importblock <code> — vault share-code import. */
    private static int importByCode(CommandContext<ServerCommandSource> ctx, String code) {
        return CloudCommands.downloadBlock(ctx.getSource(), code);
    }
}
