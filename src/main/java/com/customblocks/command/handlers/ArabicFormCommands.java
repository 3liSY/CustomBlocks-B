/**
 * ArabicFormCommands.java — Group 13 / O6.
 *
 * Responsibility: /cb config arabicforms — view / set / reset the three live Arabic join form labels
 * (initial / medial / final). Split into its own handler (like MirrorCommands) so ConfigCommands
 * stays under the 400-line gate; Brigadier merges this "config" literal into the existing /cb config
 * tree. Setting a label persists it (CustomBlocksConfig), updates the live ArabicLabels, and pushes
 * ArabicLabelsPayload to every online client so names re-label instantly with no resource-pack reload.
 *
 * Depends on: CustomBlocksConfig, ArabicLabels, ArabicLabelsPayload, Chat
 * Called by:  CommandRegistrar.register()
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.arabic.ArabicLabels;
import com.customblocks.command.Chat;
import com.customblocks.network.payloads.ArabicLabelsPayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Locale;

public final class ArabicFormCommands {

    private ArabicFormCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("config")
                .then(CommandManager.literal("arabicforms")
                        .executes(ArabicFormCommands::show)
                        .then(CommandManager.literal("reset").executes(ArabicFormCommands::reset))
                        .then(slot("ini"))
                        .then(slot("mid"))
                        .then(slot("fin"))));
    }

    /** A "ini|mid|fin <label>" branch. */
    private static LiteralArgumentBuilder<ServerCommandSource> slot(String which) {
        return CommandManager.literal(which)
                .then(CommandManager.argument("label", StringArgumentType.word())
                        .executes(ctx -> set(ctx, which, StringArgumentType.getString(ctx, "label"))));
    }

    private static int show(CommandContext<ServerCommandSource> ctx) {
        Chat.info(ctx.getSource(), "Arabic join form labels — initial: §f" + ArabicLabels.ini()
                + "§7 · medial: §f" + ArabicLabels.mid() + "§7 · final: §f" + ArabicLabels.fin()
                + "§8  (/cb config arabicforms ini|mid|fin <label> · reset)");
        return 1;
    }

    private static int reset(CommandContext<ServerCommandSource> ctx) {
        return apply(ctx, ArabicLabels.DEFAULT_INI, ArabicLabels.DEFAULT_MID, ArabicLabels.DEFAULT_FIN,
                "Arabic form labels reset to §fIni / Mid / Fin§a.");
    }

    private static int set(CommandContext<ServerCommandSource> ctx, String which, String label) {
        String ini = ArabicLabels.ini(), mid = ArabicLabels.mid(), fin = ArabicLabels.fin();
        switch (which.toLowerCase(Locale.ROOT)) {
            case "ini" -> ini = label;
            case "mid" -> mid = label;
            case "fin" -> fin = label;
        }
        return apply(ctx, ini, mid, fin,
                "Arabic " + which + " label → §f" + label + "§a. Every block re-labels instantly.");
    }

    /** Persist, update the live labels, broadcast to clients, then confirm. */
    private static int apply(CommandContext<ServerCommandSource> ctx,
                             String ini, String mid, String fin, String msg) {
        CustomBlocksConfig.arabicFormIni = ini;
        CustomBlocksConfig.arabicFormMid = mid;
        CustomBlocksConfig.arabicFormFin = fin;
        CustomBlocksConfig.save();
        ArabicLabels.set(ini, mid, fin);
        MinecraftServer s = ctx.getSource().getServer();
        if (s != null) {
            for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(p, new ArabicLabelsPayload(ini, mid, fin));
            }
        }
        Chat.success(ctx.getSource(), msg);
        return 1;
    }
}
