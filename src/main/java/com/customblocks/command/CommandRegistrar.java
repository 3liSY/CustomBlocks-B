/**
 * CommandRegistrar.java
 *
 * Responsibility: Build and register the /customblock command tree (alias /cb), then
 * delegate each subcommand group to a focused handler in command/handlers/.
 *
 * Design rule #7: NO command monolith. Each handler stays under 400 lines (§9.3).
 * LANG1 is avoided by construction — we use a clean Brigadier literal tree, so there
 * is no stray "unknown_cb_tail" argument to leak into the action bar.
 *
 * Depends on: CreationCommands, AttributeCommands, UtilityCommands (handlers)
 * Called by:  CustomBlocksMod.onInitialize()
 */
package com.customblocks.command;

import com.customblocks.command.handlers.AiCommands;
import com.customblocks.command.handlers.AnimCommands;
import com.customblocks.command.handlers.ArabicCommands;
import com.customblocks.command.handlers.ArabicFormCommands;
import com.customblocks.command.handlers.AttributeCommands;
import com.customblocks.command.handlers.BackupCommands;
import com.customblocks.command.handlers.BlueprintCommands;
import com.customblocks.command.handlers.BulkCategoryCommands;
import com.customblocks.command.handlers.BulkCommands;
import com.customblocks.command.handlers.BulkDuplicateCommands;
import com.customblocks.command.handlers.BulkExportCommands;
import com.customblocks.command.handlers.BulkFlagCommands;
import com.customblocks.command.handlers.BulkReidCommands;
import com.customblocks.command.handlers.CategoryCommands;
import com.customblocks.command.handlers.ChestGuiCommands;
import com.customblocks.command.handlers.CloudCommands;
import com.customblocks.command.handlers.ColorImageCommands;
import com.customblocks.command.handlers.ConfigCommands;
import com.customblocks.command.handlers.CreationCommands;
import com.customblocks.command.handlers.DiagnosticsCommands;
import com.customblocks.command.handlers.FaceCommands;
import com.customblocks.command.handlers.GuiCommands;
import com.customblocks.command.handlers.HelpCommands;
import com.customblocks.command.handlers.HexCommands;
import com.customblocks.command.handlers.HistoryCommands;
import com.customblocks.command.handlers.PaletteCommands;
import com.customblocks.command.handlers.ImageToolCommands;
import com.customblocks.command.handlers.MacroCommands;
import com.customblocks.command.handlers.ManagementCommands;
import com.customblocks.command.handlers.MirrorCommands;
import com.customblocks.command.handlers.ReIdCommands;
import com.customblocks.command.handlers.SafetyCommands;
import com.customblocks.command.handlers.ShapeCommands;
import com.customblocks.command.handlers.TemplateCommands;
import com.customblocks.command.handlers.ToolCommands;
import com.customblocks.command.handlers.TrashCommands;
import com.customblocks.command.handlers.UtilityCommands;
import com.customblocks.command.handlers.VideoCommands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public final class CommandRegistrar {

    private CommandRegistrar() {} // static-only

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> {
            LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("customblock");
            CreationCommands.register(root);
            AiCommands.register(root); // Group 15 — /cb ai [prompt] opens the studio's AI tab
            AnimCommands.register(root); // Group 14 — /cb anim <id> ticks|fps|loop|smoothing|trim
            ReIdCommands.register(root);
            ShapeCommands.register(root);
            FaceCommands.register(root);
            BackupCommands.register(root);
            TrashCommands.register(root);
            SafetyCommands.register(root);
            AttributeCommands.register(root);
            BulkCommands.register(root);
            BulkFlagCommands.register(root);
            BulkCategoryCommands.register(root);
            BulkDuplicateCommands.register(root);
            BulkExportCommands.register(root);
            BulkReidCommands.register(root);
            HistoryCommands.register(root);
            ConfigCommands.register(root);
            MirrorCommands.register(root); // Group 26 Part C — /cb config mirrornames (own handler, §5 split)
            ArabicFormCommands.register(root); // Group 13 / O6 — /cb config arabicforms (own handler, §5 split)
            ColorImageCommands.register(root);
            ImageToolCommands.register(root);
            PaletteCommands.register(root);
            HexCommands.register(root);
            ManagementCommands.register(root);
            CategoryCommands.register(root);
            TemplateCommands.register(root);
            UtilityCommands.register(root);
            BlueprintCommands.register(root);
            MacroCommands.register(root);
            ArabicCommands.register(root);
            DiagnosticsCommands.register(root);
            CloudCommands.register(root);
            GuiCommands.register(root);
            ChestGuiCommands.register(root);
            VideoCommands.register(root);
            HelpCommands.register(root);
            ToolCommands.register(root);
            // DidYouMean's greedy catch-all MUST be appended after every real literal —
            // Brigadier prefers literals, so this only fires for unknown subcommands.
            DidYouMean.appendFallback(root);
            LiteralCommandNode<ServerCommandSource> node = dispatcher.register(root);
            // /cb alias → forwards to the same tree, AND executes the dashboard when run bare.
            // A plain redirect does NOT inherit the target's executes, so "/cb" with no
            // subcommand would otherwise fail as an unknown command (only "/customblock" worked).
            dispatcher.register(CommandManager.literal("cb").redirect(node)
                    .executes(ChestGuiCommands::openDashboard));
        });
    }
}
