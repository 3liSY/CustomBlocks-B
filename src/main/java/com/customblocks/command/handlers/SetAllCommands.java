/**
 * SetAllCommands.java — Group 07 (Bulk Operations).
 *
 *   /cb setall <setting> <value…>
 *
 * A power alias over the bulk rail with the filter fixed to "all": it applies one setting to EVERY custom
 * block in one shot. Unlike /cb bulkproperty all … it does NOT ask for /cb confirm — instead it takes an
 * automatic point-in-time backup (named "setall-<stamp>") BEFORE every run and prunes those to the last 3,
 * so the safety net is the backup (plus the normal one-step /cb undo the underlying cores record).
 *
 * Covers every setting, routed to the same tested cores the chat bulk commands use:
 *   glow|light · hardness · sound · collision → {@link BulkCommands#applyProperty}
 *   category                                  → {@link BulkCategoryCommands#applyCategory}  ("none"/"clear" clears)
 *   shape                                     → {@link BulkShapeCommands#applyShape}
 *
 * The backup runs off the server thread (heavy I/O); the actual apply + prune run back ON the server thread
 * once the backup lands, so the snapshot always captures the pre-change state. If the backup fails the run
 * is aborted (no unsafe change).
 *
 * Depends on: BackupManager, BulkValues, BulkScope, BlockShapes, SlotManager, BulkConfirm, Chat.
 * Called by:  CommandRegistrar.
 */
package com.customblocks.command.handlers;

import com.customblocks.core.IncidentRecorder;

import com.customblocks.command.CbFmt;
import com.customblocks.block.BlockShapes;
import com.customblocks.command.Chat;
import com.customblocks.core.BackupManager;
import com.customblocks.core.BulkScope;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.GuiMode;
import com.customblocks.network.payloads.OpenGuiPayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class SetAllCommands {

    private SetAllCommands() {} // static-only

    private static final String BACKUP_PREFIX = "setall";
    private static final int    KEEP_BACKUPS  = 3;

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("setall")
                // Bare /cb setall opens the dedicated Set All Screen for a player; console prints usage.
                .executes(ctx -> { openOrUsage(ctx.getSource()); return 1; })
                .then(CommandManager.argument("args", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.SETALL_ARGS)
                        .executes(ctx -> setAll(ctx.getSource(), StringArgumentType.getString(ctx, "args")))));
    }

    /** Bare /cb setall: open the Screen for a player, or show usage from a non-player source. */
    private static void openOrUsage(ServerCommandSource src) {
        if (src.getEntity() instanceof ServerPlayerEntity p) openScreen(p);
        else usage(src);
    }

    /** Push the Set All Screen to a player (no data needed — it reads the block count from the client cache). */
    public static void openScreen(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new OpenGuiPayload(GuiMode.SETALL_SCREEN.id, "{}"));
    }

    /** Screen entry point (SetAllActionPayload): apply exactly like the chat command, from structured args. */
    public static void handleScreenAction(ServerPlayerEntity player, String setting, String value) {
        if (setting == null || value == null || setting.isBlank() || value.isBlank()) {
            Chat.error(player.getCommandSource(), "Pick a setting and a value first.");
            return;
        }
        run(player.getCommandSource(), setting.toLowerCase(Locale.ROOT), value.trim());
    }

    private static int setAll(ServerCommandSource src, String args) {
        String[] parts = args.trim().split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) { usage(src); return 0; }
        return run(src, parts[0].toLowerCase(Locale.ROOT), parts[1].trim());
    }

    /** The shared core: validate, auto-backup off-thread, then apply-to-all + prune on the server thread. */
    private static int run(ServerCommandSource src, String setting, String value) {
        // Validate the setting + value ONCE up front so a bad command never takes a pointless backup.
        Runnable apply = buildApply(src, setting, value);
        if (apply == null) return 0; // buildApply already messaged the reason

        MinecraftServer server = src.getServer();
        if (server == null) { Chat.error(src, "No server context."); return 0; }

        // Auto-backup BEFORE the change, off-thread, then run the apply + prune on the server thread.
        SlotManager.saveAll();
        int blocks = SlotManager.assignedSlots().size();
        String name = BackupManager.timestampName(BACKUP_PREFIX);
        Chat.info(src, "Backing up all " + blocks + " block(s) before applying " + CbFmt.VALUE + setting + CbFmt.DIM + "…");
        Thread worker = new Thread(() -> {
            try {
                BackupManager.save(name, blocks, false);
            } catch (Exception e) {
                String code = IncidentRecorder.record("Backup-before-setall failed (" + setting + ")", null, src.getName(), e);
                server.execute(() -> Chat.incidentError(src, "Backup failed — setall aborted, nothing changed.", code));
                return;
            }
            server.execute(() -> {
                apply.run();
                int removed = pruneSetAllBackups();
                Chat.info(src, "Safety backup saved as " + CbFmt.VALUE + name + CbFmt.DIM
                        + (removed > 0 ? " (pruned " + removed + " old setall backup(s))" : "")
                        + ". Undo with " + CbFmt.OK + "/cb undo" + CbFmt.DIM + " or " + CbFmt.OK + "/cb backup load " + name + CbFmt.DIM + ".");
            });
        }, "CustomBlocks-SetAll");
        worker.setDaemon(true);
        worker.start();
        return 1;
    }

    /**
     * Validate {@code value} for {@code setting} and return a Runnable that applies it to all blocks, or
     * null (having messaged the reason) if the setting/value is invalid or no blocks matched.
     */
    private static Runnable buildApply(ServerCommandSource src, String setting, String value) {
        List<SlotData> blocks = BulkScope.resolve("all", BulkConfirm.actor(src));
        if (blocks.isEmpty()) { Chat.error(src, "There are no custom blocks to change."); return null; }

        switch (setting) {
            case "glow", "light", "hardness", "sound", "collision" -> {
                String prop = setting.equals("light") ? "glow" : setting;
                BulkValues.Parsed pv = BulkValues.parse(src, prop, value);
                if (pv == null) return null; // parse messaged the reason
                return () -> BulkCommands.applyProperty(src, blocks, prop, pv);
            }
            case "shape" -> {
                String shape = value.toLowerCase(Locale.ROOT);
                if (!BlockShapes.isValid(shape)) {
                    Chat.error(src, "Unknown shape \"" + shape + "\". See /cb shapelist for the choices.");
                    return null;
                }
                return () -> BulkShapeCommands.applyShape(src, blocks, shape);
            }
            case "category" -> {
                String cat = (value.equalsIgnoreCase("none") || value.equalsIgnoreCase("clear")) ? "" : value;
                return () -> BulkCategoryCommands.applyCategory(src, blocks, cat);
            }
            default -> {
                Chat.error(src, "Unknown setting \"" + setting + "\".");
                usage(src);
                return null;
            }
        }
    }

    /** Keep the {@value #KEEP_BACKUPS} newest "setall-…" backups, delete the rest. Returns how many were removed. */
    private static int pruneSetAllBackups() {
        List<BackupManager.BackupInfo> all = BackupManager.list(); // newest-first
        int seen = 0, removed = 0;
        for (BackupManager.BackupInfo b : all) {
            if (!b.name().startsWith(BACKUP_PREFIX + "-")) continue;
            seen++;
            if (seen > KEEP_BACKUPS && BackupManager.delete(b.name())) removed++;
        }
        return removed;
    }

    private static void usage(ServerCommandSource src) {
        Chat.error(src, "Usage: /cb setall <glow|hardness|sound|collision|category|shape> <value>");
        Chat.info(src, "Applies to EVERY block at once (auto-backup taken first). Undo with /cb undo.");
    }
}
