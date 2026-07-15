/**
 * GuiCommands.java
 *
 * Responsibility: /cb edithud — open the drag-to-reposition HUD editor by sending an
 * OpenGuiPayload (mode = HUD_EDITOR) to the requesting player's client. HUD on/off
 * toggling now lives in `/cb config hud` (see ConfigCommands), so this command is no
 * longer a toggle stub. Stays under 400 lines (§9.3).
 *
 * Depends on: OpenGuiPayload, GuiMode, ServerPlayNetworking, Chat, SlotManager
 * Called by: CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.core.SoundFx;

import com.customblocks.command.Chat;
import com.customblocks.core.SlotManager;
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

public final class GuiCommands {

    private GuiCommands() {}

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // /cb gui, /cb admingui and the chest menus are handled by ChestGuiCommands.
        // /cb edithud — open the drag-to-reposition HUD editor (client-side overlay).
        root.then(CommandManager.literal("edithud")
                .executes(GuiCommands::editHud));

        // /cb edit <id> — what the ✎ Edit chip runs. It opens NOTHING yet, on purpose (owner 2026-07-14).
        //
        // It used to open BlockEditorScreen. That screen answered "edit" by closing itself and typing half
        // a command into your chat box for eight of its eleven buttons, which is the very split between chat
        // and screens that G04-4 exists to kill; the chest menu behind /cb editor is no better. Both were
        // rejected as the chip's target, and the replacement screen is not designed yet — so rather than
        // send the player somewhere bad, this says so honestly and stops.
        //
        // The BLOCK_EDITOR payload + the client's OpenScreen case are LEFT WIRED (openGui still speaks the
        // mode) so the real screen only has to change this one executes() to land.
        root.then(CommandManager.literal("edit")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> editComingLater(ctx, StringArgumentType.getString(ctx, "id")))));
    }

    /** /cb edit <id> — honest placeholder until the real editor screen exists (see register()). */
    private static int editComingLater(CommandContext<ServerCommandSource> ctx, String id)
            throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        src.getPlayerOrThrow(); // chips are player-only, same as the screen it will become

        // Still validate the id: "no such block" is the more useful answer, and it keeps the chip honest
        // about a block that has since been deleted (the staleness contract in Chat's chip banner).
        if (SlotManager.getById(id) == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        Chat.info(src, "The block editor screen is coming later. For now, edit \"" + id
                + "\" with the /cb commands (/cb setglow, /cb rename, /cb retexture …).");
        return 1;
    }

    private static int openGui(CommandContext<ServerCommandSource> ctx,
                                GuiMode mode, String data) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();

        // For BLOCK_EDITOR: validate the id before opening.
        if (mode == GuiMode.BLOCK_EDITOR && !data.isEmpty() && SlotManager.getById(data) == null) {
            Chat.error(src, "There's no block called \"" + data + "\". Check /cb list for the right id.");
            return 0;
        }

        // G04-4 cue: Edit and View are audibly different, because they are different actions.
        SoundFx.chip(player, mode == GuiMode.BLOCK_EDITOR ? SoundFx.Chip.EDIT : SoundFx.Chip.VIEW);
        ServerPlayNetworking.send(player, new OpenGuiPayload(mode.id, data));
        return 1;
    }

    private static int editHud(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        // The editor is a client-side overlay; tell the player's client to open it.
        return openGui(ctx, GuiMode.HUD_EDITOR, "");
    }
}
