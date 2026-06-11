/**
 * DiagnosticsCommands.java
 *
 * Responsibility: /cb diag (system snapshot) + /cb incidents (incident log).
 * Stays under 400 lines (§9.3).
 *
 * Depends on: DiagnosticsHelper, IncidentRecorder, Chat
 * Called by: CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.DiagnosticsHelper;
import com.customblocks.core.IncidentRecorder;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public final class DiagnosticsCommands {

    private DiagnosticsCommands() {}

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("diag")
                .executes(DiagnosticsCommands::diag));

        root.then(CommandManager.literal("incidents")
                .executes(DiagnosticsCommands::incidents)
                .then(CommandManager.literal("clear")
                        .executes(DiagnosticsCommands::clearIncidents)));
    }

    private static int diag(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.DIAG));
            return 1;
        }
        List<String> lines = DiagnosticsHelper.collect(src.getServer());
        for (String line : lines) src.sendFeedback(() -> Text.literal(line), false);
        return 1;
    }

    private static int incidents(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        List<String> lines = IncidentRecorder.list();
        src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§eIncident log:"), false);
        for (String line : lines) src.sendFeedback(() -> Text.literal(line), false);
        return 1;
    }

    private static int clearIncidents(CommandContext<ServerCommandSource> ctx) {
        IncidentRecorder.clear();
        Chat.success(ctx.getSource(), "Incident log cleared.");
        return 1;
    }
}
