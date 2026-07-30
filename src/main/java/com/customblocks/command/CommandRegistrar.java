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

import com.customblocks.command.handlers.AchievementCommands;
import com.customblocks.command.handlers.AiCommands;
import com.customblocks.command.handlers.AnimCommands;
import com.customblocks.command.handlers.BackgroundCommands;
import com.customblocks.command.handlers.ArabicCommands;
import com.customblocks.command.handlers.ArabicFormCommands;
import com.customblocks.command.handlers.AttributeCommands;
import com.customblocks.command.handlers.BackupCommands;
import com.customblocks.command.handlers.BulkCategoryCommands;
import com.customblocks.command.handlers.BulkCommands;
import com.customblocks.command.handlers.BulkDuplicateCommands;
import com.customblocks.command.handlers.BulkExportCommands;
import com.customblocks.command.handlers.BulkFlagCommands;
import com.customblocks.command.handlers.BulkReidCommands;
import com.customblocks.command.handlers.BulkShapeCommands;
import com.customblocks.command.handlers.SetAllCommands;
import com.customblocks.command.handlers.BuzzerGameCommands;
import com.customblocks.command.handlers.CategoryCommands;
import com.customblocks.command.handlers.ChestGuiCommands;
import com.customblocks.command.handlers.CloudCommands;
import com.customblocks.command.handlers.ColorImageCommands;
import com.customblocks.command.handlers.ColorVariantCommands;
import com.customblocks.command.handlers.ConfigCommands;
import com.customblocks.command.handlers.CreationCommands;
import com.customblocks.command.handlers.DebugCommands;
import com.customblocks.command.handlers.DeleteCommands;
import com.customblocks.command.handlers.DiagnosticsCommands;
import com.customblocks.command.handlers.FaceCommands;
import com.customblocks.command.handlers.GiveCommands;
import com.customblocks.command.handlers.GuessCommands;
import com.customblocks.command.handlers.GuiCommands;
import com.customblocks.command.handlers.HelpCommands;
import com.customblocks.command.handlers.HexCommands;
import com.customblocks.command.handlers.HistoryCommands;
import com.customblocks.command.handlers.LowResCommands;
import com.customblocks.command.handlers.PaletteCommands;
import com.customblocks.command.handlers.RecordOverlayCommands;
import com.customblocks.command.handlers.ImageToolCommands;
import com.customblocks.command.handlers.MacroCommands;
import com.customblocks.command.handlers.ManagementCommands;
import com.customblocks.command.handlers.MarkerTestCommands;
import com.customblocks.command.handlers.MirrorCommands;
import com.customblocks.command.handlers.NoteCommands;
import com.customblocks.command.handlers.ParticleCommands;
import com.customblocks.command.handlers.ReIdCommands;
import com.customblocks.command.handlers.RetextureAllCommands;
import com.customblocks.command.handlers.SafetyCommands;
import com.customblocks.command.handlers.FeedbackCommands;
import com.customblocks.command.handlers.ShapeCommands;
import com.customblocks.command.handlers.SoundCommands;
import com.customblocks.command.handlers.SourceListCommands;
import com.customblocks.command.handlers.SourceWallCommands;
import com.customblocks.command.handlers.TempRetextureCommands;
import com.customblocks.command.handlers.TemplateCommands;
import com.customblocks.command.handlers.TomatoCommands;
import com.customblocks.command.handlers.ToolCommands;
import com.customblocks.command.handlers.TrashCommands;
import com.customblocks.command.handlers.UtilityCommands;
import com.customblocks.command.handlers.WheelCommands;
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
            RetextureAllCommands.register(root); // /cb retextureall — split from CreationCommands (§9.3 gate)
            SourceListCommands.register(root); // Group 14 — /cb sourcelist: read-only "which blocks can be re-made sharper"
            SourceWallCommands.register(root); // Group 14 §5d TEMP — /cb sourcewall [clear]: review wall (REMOVE when re-source job done)
            TempRetextureCommands.register(root); // Group 14 §S TEMP — /cb tempretexture all|undo|clear: batch-apply sourced logos (REMOVE when re-source job done)
            DeleteCommands.register(root); // Group 17 slice 3 — /cb delete <id> + /cb delete #
            MarkerTestCommands.register(root); // G06-14 slice 1 TEMP — /cb spawnmarker <name> (remove in slice 2+)
            AiCommands.register(root); // Group 15 — /cb ai [prompt] opens the studio's AI tab
            AnimCommands.register(root); // Group 14 — /cb animation <id> ticks|fps|loop|smoothing|trim
            ReIdCommands.register(root);
            ShapeCommands.register(root);
            FaceCommands.register(root);
            BackupCommands.register(root);
            TrashCommands.register(root);
            SafetyCommands.register(root);
            AttributeCommands.register(root);
            BulkCommands.register(root);
            SetAllCommands.register(root); // Group 07 — /cb setall <setting> <value> (all blocks, auto-backup+prune-3)
            BulkFlagCommands.register(root);
            BulkCategoryCommands.register(root);
            BulkDuplicateCommands.register(root);
            BulkExportCommands.register(root);
            BulkReidCommands.register(root);
            BulkShapeCommands.register(root); // Group 08 §E — /cb bulkshape <filter> <shape>
            HistoryCommands.register(root);
            ConfigCommands.register(root);
            MirrorCommands.register(root); // Group 26 Part C — /cb config mirrornames (own handler, §5 split)
            ArabicFormCommands.register(root); // Group 13 / O6 — /cb config arabicforms (own handler, §5 split)
            ColorImageCommands.register(root);
            ColorVariantCommands.register(root); // G10-CV — /cb colorvariants + /cb variants (create form, Phase 1 slice 1)
            BackgroundCommands.register(root); // G10 §C — /cb setbg + /cb setbackground (stored background attribute)
            ImageToolCommands.register(root);
            PaletteCommands.register(root);
            HexCommands.register(root);
            ManagementCommands.register(root);
            NoteCommands.register(root); // Group 18 — /cb lore <id> opens the Lore GUI (alias /cb note; + quick-add/clear/import)
            CategoryCommands.register(root);
            TemplateCommands.register(root);
            UtilityCommands.register(root);
            GiveCommands.register(root); // Group 17 slice 2 — /cb give <id> [amount] [player]
            // BlueprintCommands is GONE (G12, 2026-07-30): /cb exportblock + /cb importblock and the
            // Blueprint item were removed, not maintained. G20 §A Vault share codes already move a block
            // between people and are confirmed working, so a same-server-only paper item added nothing.
            MacroCommands.register(root);
            ArabicCommands.register(root);
            DiagnosticsCommands.register(root);
            ParticleCommands.register(root); // Group 16 slice 4 — /cb particles <cat> on|off + board
            SoundCommands.register(root);    // Group 16 slice 5 — /cb sounds <cat> on|off
            FeedbackCommands.register(root); // Group 16 slice 5 — /cb feedback <cat> on|off (both)
            CloudCommands.register(root);
            GuiCommands.register(root);
            ChestGuiCommands.register(root);
            // /cb video removed 2026-06-22 (owner) — video import will return inside the studio (Phase 7).
            HelpCommands.register(root);
            AchievementCommands.register(root); // Group 23 — /cb achievements (text list; [View] link target)
            ToolCommands.register(root);
            RecordOverlayCommands.register(root); // Group 29 Build B - /cb recordoverlay
            GuessCommands.register(root); // Group 30 — /cb guess <player> ... (op-only party guess mode)
            BuzzerGameCommands.register(root); // Group 31 — /cb buzzergame ... (BuzzerGame minigame)
            TomatoCommands.register(root); // Group 32 — /cb tomato <targets> [amount] | all (OP level 2)
            WheelCommands.register(root); // Group 34 — /cb wheel place|spin|list (Wheel of Fortune)
            DebugCommands.register(root); // §G04-5 — hidden /cb debug kick <cause> (OP-only; kick-screen preview)
            // DidYouMean's greedy catch-all MUST be appended after every real literal —
            // Brigadier prefers literals, so this only fires for unknown subcommands.
            DidYouMean.appendFallback(root);
            LiteralCommandNode<ServerCommandSource> node = dispatcher.register(root);
            // G04-2: this node is the ONE authoritative list of what /cb can do. Snapshot its literal
            // children here and both "did you mean" and /cb help read from it — never from a
            // hand-maintained dictionary that can (and did) drift out of sync with reality.
            CommandTree.capture(node);
            // /cb alias → forwards to the same tree, AND executes the dashboard when run bare.
            // A plain redirect does NOT inherit the target's executes, so "/cb" with no
            // subcommand would otherwise fail as an unknown command (only "/customblock" worked).
            dispatcher.register(CommandManager.literal("cb").redirect(node)
                    .executes(ChestGuiCommands::openDashboard));
            // Group 05 §F — /cblowres lives on its OWN root (not under /cb): the old client-side command
            // owned this root; the server now owns it and chooses each player's pack resolution.
            LowResCommands.register(dispatcher);
        });
    }
}
