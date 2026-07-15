/**
 * RecordOverlayCommands.java - GROUP 29 Build B command surface.
 *
 * Responsibility: register /cb recordoverlay and forward local recording actions to
 * the requesting client. Overlay settings are intentionally client-local.
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.gui.GuiMode;
import com.customblocks.network.payloads.OpenGuiPayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class RecordOverlayCommands {

    private RecordOverlayCommands() {}

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("recordoverlay")
                .executes(ctx -> action(ctx, "studio"))
                .then(CommandManager.literal("studio").executes(ctx -> action(ctx, "studio")))
                .then(CommandManager.literal("on").executes(ctx -> action(ctx, "on")))
                .then(CommandManager.literal("off").executes(ctx -> action(ctx, "off")))
                .then(CommandManager.literal("toggle").executes(ctx -> action(ctx, "toggle")))
                .then(CommandManager.literal("borderless").executes(ctx -> action(ctx, "borderless")))
                .then(CommandManager.literal("obs")
                        .executes(ctx -> action(ctx, "obs_toggle"))
                        .then(CommandManager.literal("on").executes(ctx -> action(ctx, "obs_on")))
                        .then(CommandManager.literal("off").executes(ctx -> action(ctx, "obs_off")))
                        .then(CommandManager.literal("toggle").executes(ctx -> action(ctx, "obs_toggle"))))
                .then(CommandManager.literal("layout")
                        .executes(ctx -> action(ctx, "layout_list"))
                        .then(CommandManager.literal("list").executes(ctx -> action(ctx, "layout_list")))
                        .then(CommandManager.literal("import").executes(ctx -> action(ctx, "layout_import")))
                        .then(CommandManager.literal("save")
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .executes(ctx -> action(ctx, "layout_save:" + arg(ctx, "name")))))
                        .then(CommandManager.literal("load")
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .executes(ctx -> action(ctx, "layout_load:" + arg(ctx, "name")))))
                        .then(CommandManager.literal("delete")
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .executes(ctx -> action(ctx, "layout_delete:" + arg(ctx, "name")))))
                        .then(CommandManager.literal("export")
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .executes(ctx -> action(ctx, "layout_export:" + arg(ctx, "name"))))))
                .then(CommandManager.literal("push")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("everyone").executes(ctx -> pushStub(ctx, "everyone")))
                        .then(CommandManager.literal("off").executes(ctx -> pushStub(ctx, "off")))));
    }

    private static int action(CommandContext<ServerCommandSource> ctx, String action) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        ServerPlayNetworking.send(player, new OpenGuiPayload(GuiMode.RECORD_OVERLAY.id, action));
        return 1;
    }

    private static int pushStub(CommandContext<ServerCommandSource> ctx, String mode) throws CommandSyntaxException {
        action(ctx, "push_" + mode);
        Chat.info(ctx.getSource(), "Record overlay push " + mode + " is reserved for the next multiplayer sync slice.");
        return 1;
    }

    private static String arg(CommandContext<ServerCommandSource> ctx, String name) {
        return StringArgumentType.getString(ctx, name);
    }
}
