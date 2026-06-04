/**
 * CreationCommands.java
 *
 * Responsibility: The block lifecycle subcommands — create, delete, rename, dupe.
 * Registered into the /cb tree by CommandRegistrar. All mutations go through
 * SlotManager (the single source of truth). Stays under 400 lines (§9.3).
 *
 * Phase 2: text-only management (no textures yet). retexture arrives in Phase 4.
 */
package com.customblocks.command.handlers;

import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public final class CreationCommands {

    private CreationCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("create")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .executes(ctx -> create(ctx, id(ctx), null))
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> create(ctx, id(ctx), StringArgumentType.getString(ctx, "name"))))));

        root.then(CommandManager.literal("delete")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .executes(ctx -> delete(ctx, id(ctx)))));

        root.then(CommandManager.literal("rename")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> rename(ctx, id(ctx), StringArgumentType.getString(ctx, "name"))))));

        root.then(CommandManager.literal("dupe")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .then(CommandManager.argument("newId", StringArgumentType.word())
                                .executes(ctx -> dupe(ctx, id(ctx), StringArgumentType.getString(ctx, "newId"))))));
    }

    private static String id(CommandContext<ServerCommandSource> ctx) {
        return StringArgumentType.getString(ctx, "id");
    }

    private static int create(CommandContext<ServerCommandSource> ctx, String id, String name) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.create(id, name);
        if (d == null) {
            src.sendError(Text.literal("Could not create '" + id + "' — id already exists or no free slots."));
            return 0;
        }
        src.sendFeedback(() -> Text.literal(
                "§aCreated §f" + id + " §7(slot " + d.index() + "). Use §f/cb give " + id + "§7 to get it."), false);
        return 1;
    }

    private static int delete(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.delete(id);
        if (d == null) {
            src.sendError(Text.literal("No block with id '" + id + "'."));
            return 0;
        }
        src.sendFeedback(() -> Text.literal("§cDeleted §f" + id + "§c."), false);
        return 1;
    }

    private static int rename(CommandContext<ServerCommandSource> ctx, String id, String name) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.rename(id, name);
        if (d == null) {
            src.sendError(Text.literal("No block with id '" + id + "'."));
            return 0;
        }
        src.sendFeedback(() -> Text.literal("§aRenamed §f" + id + " §7→ §f\"" + name + "\""), false);
        return 1;
    }

    private static int dupe(CommandContext<ServerCommandSource> ctx, String id, String newId) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.dupe(id, newId);
        if (d == null) {
            src.sendError(Text.literal("Could not duplicate — check the ids and that a free slot exists."));
            return 0;
        }
        src.sendFeedback(() -> Text.literal(
                "§aDuplicated §f" + id + " §7→ §f" + newId + " §7(slot " + d.index() + ")"), false);
        return 1;
    }
}
