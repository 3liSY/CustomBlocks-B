/**
 * BackupCommands.java — Group 09, Slices 1 + 2.
 *
 *   /cb backup                 — (no args) open the advanced backup GUI (BackupMenu). Console → chat usage.
 *   /cb backup save [name]     — snapshot current data into backups/<name>/ (Slice 1, read-only).
 *   /cb backup list            — list saved backups, newest first (Slice 1).
 *   /cb backup load <name>     — replace live data with a backup. Requires /cb confirm. (Slice 2)
 *                                ("restore" is kept as a hidden alias for load.)
 *   /cb backup delete <name>   — remove a backup folder (never touches live data). (Slice 2)
 *   /cb backup panic           — EMERGENCY: restore the newest backup immediately, no confirm. (Slice 2)
 *   /cb recover                — restore the newest backup (with /cb confirm). (Slice 2)
 *
 * The GUI (BackupMenu / BackupConfirmMenu) calls guiCreate / guiLoad here so it reuses the exact same
 * tested save + restore orchestration as the chat commands — no backup logic is duplicated in the GUI.
 *
 * Restore safety: it always auto-saves the current state first (as "pre-restore-…"), pauses the pack
 * during the swap, then reloads config + slots and rebuilds/pushes the pack — so a restore is itself
 * undoable and never overwrites live data mid-write (the swap is move-aside-then-copy in BackupManager).
 *
 * Depends on: BackupManager, SlotManager, CustomBlocksConfig, ResourcePackServer, BulkConfirm,
 *             IncidentRecorder, Chat.
 * Called by:  CommandRegistrar.
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.BackupManager;
import com.customblocks.core.IncidentRecorder;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.BackupSelection;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public final class BackupCommands {

    private BackupCommands() {} // static-only

    /** Tab-complete existing backup names. */
    private static final SuggestionProvider<ServerCommandSource> NAMES = (ctx, b) -> {
        for (BackupManager.BackupInfo i : BackupManager.list()) b.suggest(i.name());
        return b.buildFuture();
    };

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("backup")
                // Bare /cb backup opens the advanced GUI for a player; console falls back to chat usage.
                .executes(ctx -> openOrUsage(ctx.getSource()))
                .then(CommandManager.literal("save")
                        .executes(ctx -> save(ctx.getSource(), null))
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .executes(ctx -> save(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(CommandManager.literal("list")
                        .executes(ctx -> list(ctx.getSource())))
                .then(CommandManager.literal("load") // primary verb (renamed from "restore")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .suggests(NAMES)
                                .executes(ctx -> requestRestore(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))))
                .then(CommandManager.literal("restore") // hidden alias — keeps old muscle memory working
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .suggests(NAMES)
                                .executes(ctx -> requestRestore(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))))
                .then(CommandManager.literal("delete")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .suggests(NAMES)
                                .executes(ctx -> delete(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))))
                .then(CommandManager.literal("panic")
                        .executes(ctx -> panic(ctx.getSource()))));

        // /cb backupgui — open the advanced backup GUI directly (matches /cb listgui, /cb bulkgui …).
        root.then(CommandManager.literal("backupgui").executes(ctx -> openOrUsage(ctx.getSource())));

        // /cb recover — restore the newest backup (with confirm). Top-level per the Group 09 spec.
        root.then(CommandManager.literal("recover").executes(ctx -> recover(ctx.getSource())));
    }

    /** Bare /cb backup: open the GUI as a player; console/command-block gets the chat usage list. */
    private static int openOrUsage(ServerCommandSource src) {
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            BackupSelection.clear(p.getUuid()); // start each GUI session with a clean selection
            GuiRouter.openFresh(p, MenuKey.of(Dest.BACKUP_LIST));
            return 1;
        }
        return usage(src);
    }

    private static int usage(ServerCommandSource src) {
        Chat.info(src, "Backup commands (run §e/cb backup§7 in-game for the GUI):");
        src.sendFeedback(() -> Text.literal("  §e/cb backup save [name] §7- save a point-in-time backup"), false);
        src.sendFeedback(() -> Text.literal("  §e/cb backup list §7- list saved backups"), false);
        src.sendFeedback(() -> Text.literal("  §e/cb backup load <name> §7- load one back (asks to confirm)"), false);
        src.sendFeedback(() -> Text.literal("  §e/cb backup delete <name> §7- remove a backup"), false);
        src.sendFeedback(() -> Text.literal("  §c/cb backup panic §7- emergency: load the newest now"), false);
        src.sendFeedback(() -> Text.literal("  §e/cb recover §7- load the newest back (asks to confirm)"), false);
        return 1;
    }

    // ── Save / list (Slice 1) ────────────────────────────────────────────────

    private static int save(ServerCommandSource src, String nameArg) {
        MinecraftServer server = src.getServer();
        if (server == null) return 0;
        String name = (nameArg == null || nameArg.isBlank())
                ? BackupManager.timestampName("backup") : nameArg.trim();
        if (!validateNewName(src, name)) return 0;
        startSave(src, server, name, null);
        return 1;
    }

    /** Validate a proposed NEW backup name, messaging {@code src} on failure. */
    private static boolean validateNewName(ServerCommandSource src, String name) {
        if (!BackupManager.isValidName(name)) {
            Chat.error(src, "Bad backup name \"" + name + "\". Use letters, numbers, - or _ (max 48 chars).");
            return false;
        }
        if (BackupManager.exists(name)) {
            Chat.error(src, "A backup named \"" + name + "\" already exists. Pick another name, or delete it first.");
            return false;
        }
        return true;
    }

    /**
     * Flush live data on the server thread, then copy the backup off-thread (heavy-I/O idiom). On
     * success {@code onDone} (if any) runs on the server thread — the GUI uses it to refresh the list.
     * Assumes {@code name} is already validated.
     */
    private static void startSave(ServerCommandSource src, MinecraftServer server, String name, Runnable onDone) {
        SlotManager.saveAll();
        int blocks = SlotManager.assignedSlots().size();
        Chat.info(src, "Saving backup \"" + name + "\"…");
        Thread worker = new Thread(() -> {
            try {
                BackupManager.save(name, blocks, false);
                server.execute(() -> {
                    Chat.success(src, "Backup \"" + name + "\" saved. (" + blocks + " block(s), config, textures)");
                    if (onDone != null) onDone.run();
                });
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                IncidentRecorder.record("Backup save failed for \"" + name + "\"", e);
                server.execute(() -> Chat.error(src, "Couldn't save the backup. " + msg));
            }
        }, "CustomBlocks-Backup");
        worker.setDaemon(true);
        worker.start();
    }

    // ── GUI bridge (BackupMenu / BackupConfirmMenu) ───────────────────────────

    /** Create a backup from the GUI's name prompt, then refresh the list when the save finishes. */
    public static void guiCreate(ServerPlayerEntity player, String nameArg) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ServerCommandSource src = player.getCommandSource();
        String name = (nameArg == null || nameArg.isBlank())
                ? BackupManager.timestampName("backup") : nameArg.trim();
        if (!validateNewName(src, name)) { reopenList(player); return; }
        startSave(src, server, name, () -> reopenList(player));
    }

    /** Load (restore) a backup from the GUI — the chest's Yes IS the confirm, so no /cb confirm needed. */
    public static void guiLoad(ServerPlayerEntity player, String name) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        server.execute(() -> {
            player.closeHandledScreen();        // close the chest before the pack pauses/resyncs
            doRestore(player.getCommandSource(), name);
        });
    }

    private static void reopenList(ServerPlayerEntity player) {
        GuiRouter.render(player, MenuKey.of(Dest.BACKUP_LIST));
    }

    private static int list(ServerCommandSource src) {
        List<BackupManager.BackupInfo> backups = BackupManager.list();
        if (backups.isEmpty()) {
            Chat.info(src, "No backups yet. Make one with /cb backup save [name].");
            return 1;
        }
        Chat.info(src, "Backups (" + backups.size() + ", newest first):");
        for (BackupManager.BackupInfo b : backups) {
            String when = b.created().isEmpty() ? "?" : b.created();
            String blocks = b.blocks() >= 0 ? (b.blocks() + " block(s)") : "?";
            String tag = b.auto() ? " §8(auto)" : "";
            src.sendFeedback(() -> Text.literal("  §e" + b.name() + " §7- " + when + " §8· §7" + blocks + tag), false);
        }
        return 1;
    }

    // ── Restore / delete / panic / recover (Slice 2) ──────────────────────────

    /** Arm a confirm-gated restore (the spec requires /cb confirm for restore, always). */
    private static int requestRestore(ServerCommandSource src, String name) {
        if (!BackupManager.isValidBackup(name)) {
            Chat.error(src, "Backup \"" + name + "\" is missing or unreadable. Run /cb backup list to see what's there.");
            return 0;
        }
        BulkConfirm.request(src, () -> doRestore(src, name), "restore backup " + name);
        Chat.info(src, "About to RESTORE \"" + name + "\" — this replaces your current blocks. A safety copy of "
                + "the current state is saved first. Type §a/cb confirm§7 to proceed, or §c/cb cancel§7.");
        return 1;
    }

    private static int delete(ServerCommandSource src, String name) {
        if (!BackupManager.exists(name)) {
            Chat.error(src, "There's no backup named \"" + name + "\". Run /cb backup list.");
            return 0;
        }
        boolean ok = BackupManager.delete(name);
        if (ok) Chat.success(src, "Deleted backup \"" + name + "\".");
        else Chat.error(src, "Couldn't delete backup \"" + name + "\".");
        return ok ? 1 : 0;
    }

    private static int panic(ServerCommandSource src) {
        String latest = BackupManager.latestName();
        if (latest == null) {
            Chat.error(src, "No backups to roll back to. (Nothing has been saved yet.)");
            return 0;
        }
        Chat.info(src, "§cPANIC §7— rolling back to the newest backup \"" + latest + "\" now…");
        doRestore(src, latest);
        return 1;
    }

    private static int recover(ServerCommandSource src) {
        String latest = BackupManager.latestName();
        if (latest == null) {
            Chat.error(src, "No backups to recover from. (Nothing has been saved yet.)");
            return 0;
        }
        return requestRestore(src, latest);
    }

    /**
     * Perform the restore on the server thread: pause the pack, flush, safe-swap the files (a safety
     * backup is taken first), then reload config + slots and rebuild/push the pack. On failure the
     * data is left as it was (BackupManager rolls the safety copy back) and the pack resumes.
     */
    private static void doRestore(ServerCommandSource src, String name) {
        MinecraftServer server = src.getServer();
        if (server == null) return;
        if (!BackupManager.isValidBackup(name)) {
            Chat.error(src, "Backup \"" + name + "\" is missing or unreadable.");
            return;
        }
        int current = SlotManager.assignedSlots().size();
        ResourcePackServer.pause();
        String safety;
        try {
            SlotManager.saveAll();
            safety = BackupManager.restore(name, current);
            CustomBlocksConfig.load();
            SlotManager.reload();
        } catch (Exception e) {
            ResourcePackServer.resume();
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            IncidentRecorder.record("Backup restore failed for \"" + name + "\"", e);
            Chat.error(src, "Restore failed — your data was left as it was. " + msg);
            return;
        }
        ResourcePackServer.resume();    // rebuilds the pack
        ResourcePackServer.syncToAll(); // push the restored pack to clients
        int now = SlotManager.assignedSlots().size();
        Chat.success(src, "Restored from \"" + name + "\" — " + now + " block(s) now. Old state saved as \""
                + safety + "\" (undo with /cb backup load " + safety + ").");
    }
}
