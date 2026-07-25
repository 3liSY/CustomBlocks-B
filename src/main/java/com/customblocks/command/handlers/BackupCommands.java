/**
 * BackupCommands.java — Group 09, Slices 1 + 2.
 *
 *   /cb backup                 — (no args) open the advanced backup GUI (BackupMenu). Console → chat usage.
 *   /cb backup save [name]     — snapshot current data into backups/<name>/ (Slice 1, read-only).
 *   /cb backup list            — list saved backups, newest first (Slice 1).
 *   /cb backup load <name>     — replace live data with a backup. Requires /cb confirm. (Slice 2)
 *   /cb backup delete <name>   — remove a backup folder (never touches live data). (Slice 2)
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

import com.customblocks.command.CbFmt;
import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.BackupManager;
import com.customblocks.core.BackupArchive;
import com.customblocks.core.BackupIntegrity;
import com.customblocks.core.BackupRestore;
import com.customblocks.core.BackupRetention;
import com.customblocks.core.BackupView;
import com.customblocks.core.IncidentRecorder;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.GuiMode;
import com.customblocks.gui.chest.BackupSelection;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import com.customblocks.network.ResourcePackServer;
import com.customblocks.network.payloads.BackupActionPayload;
import com.customblocks.network.payloads.OpenGuiPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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

    /** Tab-complete the top-level entries INSIDE the already-typed backup (for granular load). */
    private static final SuggestionProvider<ServerCommandSource> ENTRIES = (ctx, b) -> {
        try {
            String n = StringArgumentType.getString(ctx, "name");
            for (String e : BackupRestore.contents(n)) b.suggest(e);
        } catch (Exception ignored) { /* name not parsed yet */ }
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
                .then(CommandManager.literal("load")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .suggests(NAMES)
                                .executes(ctx -> requestRestore(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))
                                // Granular (P5): /cb backup load <name> <entry> restores ONE store only.
                                .then(CommandManager.argument("entry", StringArgumentType.word())
                                        .suggests(ENTRIES)
                                        .executes(ctx -> requestRestoreEntry(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "entry"))))))
                .then(CommandManager.literal("contents")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .suggests(NAMES)
                                .executes(ctx -> BackupScreenCommands.contents(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))))
                .then(CommandManager.literal("delete")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .suggests(NAMES)
                                .executes(ctx -> delete(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))))
                .then(CommandManager.literal("preview")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .suggests(NAMES)
                                .executes(ctx -> BackupScreenCommands.preview(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))))
                .then(CommandManager.literal("verify")
                        .executes(ctx -> BackupScreenCommands.verifyAll(ctx.getSource()))
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .suggests(NAMES)
                                .executes(ctx -> BackupScreenCommands.verifyOne(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"))))));

        // /cb backupgui — open the advanced backup GUI directly (matches /cb bulkgui …).
        root.then(CommandManager.literal("backupgui").executes(ctx -> openOrUsage(ctx.getSource())));
    }

    /** Bare /cb backup: open the Backup Screen (G09-A4) as a player; console gets the chat usage list. */
    private static int openOrUsage(ServerCommandSource src) {
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            BackupScreenCommands.openScreen(p);
            return 1;
        }
        return usage(src);
    }

    private static int usage(ServerCommandSource src) {
        Chat.info(src, "Backup commands (run " + CbFmt.VALUE + "/cb backup" + CbFmt.DIM + " in-game for the GUI):");
        Chat.raw(src, Text.literal("  " + CbFmt.VALUE + "/cb backup save [name] " + CbFmt.DIM + "- save a point-in-time backup"));
        Chat.raw(src, Text.literal("  " + CbFmt.VALUE + "/cb backup list " + CbFmt.DIM + "- list saved backups"));
        Chat.raw(src, Text.literal("  " + CbFmt.VALUE + "/cb backup load <name> " + CbFmt.DIM + "- load one back (asks to confirm)"));
        Chat.raw(src, Text.literal("  " + CbFmt.VALUE + "/cb backup contents <name> " + CbFmt.DIM + "- list what a backup holds"));
        Chat.raw(src, Text.literal("  " + CbFmt.VALUE + "/cb backup load <name> <item> " + CbFmt.DIM + "- restore just one part (e.g. notes.json)"));
        Chat.raw(src, Text.literal("  " + CbFmt.VALUE + "/cb backup preview <name> " + CbFmt.DIM + "- show what a restore would change (no change)"));
        Chat.raw(src, Text.literal("  " + CbFmt.VALUE + "/cb backup verify [name] " + CbFmt.DIM + "- check a backup (or all) for corruption"));
        Chat.raw(src, Text.literal("  " + CbFmt.VALUE + "/cb backup delete <name> " + CbFmt.DIM + "- remove a backup"));
        return 1;
    }

    // ── Save / list (Slice 1) ────────────────────────────────────────────────

    private static int save(ServerCommandSource src, String nameArg) {
        MinecraftServer server = src.getServer();
        if (server == null) return 0;
        boolean userTyped = nameArg != null && !nameArg.isBlank();
        String name = userTyped ? nameArg.trim() : BackupManager.generatedName(BackupManager.Kind.MANUAL);
        if (!validateNewName(src, name, userTyped)) return 0;
        startSave(src, server, name, null);
        return 1;
    }

    /** Validate a proposed NEW backup name typed by a player (runs the reserved-prefix guard). */
    static boolean validateNewName(ServerCommandSource src, String name) {
        return validateNewName(src, name, true);
    }

    /**
     * Validate a proposed NEW backup name, messaging {@code src} on failure. When {@code userTyped} is
     * false the name came from the mod itself (generatedName/timestampName), so the reserved-prefix guard
     * is skipped — otherwise the auto-name "manual_…" would refuse itself for starting with "manual_".
     */
    static boolean validateNewName(ServerCommandSource src, String name, boolean userTyped) {
        if (!BackupManager.isValidName(name)) {
            Chat.error(src, "Bad backup name \"" + name + "\". Use letters, numbers, - or _ (max 48 chars).");
            return false;
        }
        if (userTyped && BackupManager.isReservedName(name)) {
            Chat.error(src, "\"" + name + "\" starts with a reserved prefix (manual_/auto_/safety_/pre-). "
                    + "Pick a different name — those are used by automatic backups.");
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
    static void startSave(ServerCommandSource src, MinecraftServer server, String name, Runnable onDone) {
        SlotManager.saveAll();
        int blocks = SlotManager.assignedSlots().size();
        Chat.info(src, "Saving backup \"" + name + "\"…");
        Thread worker = new Thread(() -> {
            try {
                BackupManager.save(name, blocks, BackupManager.Kind.MANUAL, "manual", null);
                server.execute(() -> {
                    Chat.success(src, "Backup \"" + name + "\" saved. (" + blocks + " block(s), config, textures)");
                    if (onDone != null) onDone.run();
                });
            } catch (Exception e) {
                String code = IncidentRecorder.record("Backup save failed for \"" + name + "\"", null, src.getName(), e);
                server.execute(() -> Chat.incidentError(src, "Couldn't save the backup.", code));
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
        boolean userTyped = nameArg != null && !nameArg.isBlank();
        String name = userTyped ? nameArg.trim() : BackupManager.generatedName(BackupManager.Kind.MANUAL);
        if (!validateNewName(src, name, userTyped)) { reopenList(player); return; }
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
        // A player gets the Backup Screen (G09-A4); console/command-block keeps the chat list.
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            BackupScreenCommands.openScreen(p);
            return 1;
        }
        List<BackupManager.BackupInfo> backups = BackupManager.list();
        if (backups.isEmpty()) {
            Chat.info(src, "No backups yet. Make one with /cb backup save [name].");
            return 1;
        }
        Chat.info(src, "Backups (" + backups.size() + ", newest first):");
        for (BackupManager.BackupInfo b : backups) {
            String label = BackupView.displayLabel(b);
            String blocks = b.blocks() >= 0 ? (b.blocks() + " block(s)") : "?";
            if (label.equals(b.name())) {
                // Manual save: the name is both the label and the restore-by id; show time separately.
                Chat.raw(src, Text.literal("  " + CbFmt.VALUE + label + " " + CbFmt.DIM + "- " + BackupView.friendlyTime(b)
                        + " " + CbFmt.FAINT + "· " + CbFmt.DIM + blocks));
            } else {
                // Auto: friendly label already carries the time; raw restore-by id shown dim in parens.
                Chat.raw(src, Text.literal("  " + CbFmt.VALUE + label + " " + CbFmt.FAINT + "(" + b.name() + ") " + CbFmt.FAINT + "· " + CbFmt.DIM + blocks));
            }
        }
        return 1;
    }

    // ── Restore / delete (Slice 2) ────────────────────────────────────────────

    /** Arm a confirm-gated restore (the spec requires /cb confirm for restore, always). */
    private static int requestRestore(ServerCommandSource src, String name) {
        if (!BackupManager.isValidBackup(name)) {
            Chat.error(src, "Backup \"" + name + "\" is missing or unreadable. Run /cb backup list to see what's there.");
            return 0;
        }
        BulkConfirm.request(src, () -> doRestore(src, name), "restore backup " + name);
        Chat.info(src, "About to RESTORE \"" + name + "\" — this replaces your current blocks. A safety copy of "
                + "the current state is saved first. Type " + CbFmt.OK + "/cb confirm" + CbFmt.DIM + " to proceed, or " + CbFmt.BAD + "/cb cancel" + CbFmt.DIM + ".");
        return 1;
    }

    /** Arm a confirm-gated granular restore of ONE entry from a backup (P5). */
    private static int requestRestoreEntry(ServerCommandSource src, String name, String entry) {
        if (!BackupManager.isValidBackup(name)) {
            Chat.error(src, "Backup \"" + name + "\" is missing or unreadable.");
            return 0;
        }
        if (!BackupRestore.contents(name).contains(entry)) {
            Chat.error(src, "Backup \"" + name + "\" has no \"" + entry + "\". Run /cb backup contents " + name + ".");
            return 0;
        }
        BulkConfirm.request(src, () -> doRestoreEntry(src, name, entry), "restore " + entry + " from " + name);
        Chat.info(src, "About to restore ONLY " + CbFmt.VALUE + entry + CbFmt.DIM + " from \"" + name + "\" (a safety copy of "
                + "the current " + entry + " is saved first). Type " + CbFmt.OK + "/cb confirm" + CbFmt.DIM + " or " + CbFmt.BAD + "/cb cancel" + CbFmt.DIM + ".");
        return 1;
    }

    /** Perform a single-entry restore on the server thread: pause pack, restore the one entry, reload. */
    private static void doRestoreEntry(ServerCommandSource src, String name, String entry) {
        MinecraftServer server = src.getServer();
        if (server == null) return;
        int current = SlotManager.assignedSlots().size();
        ResourcePackServer.pause();
        String safety;
        try {
            SlotManager.saveAll();
            safety = BackupRestore.restoreEntry(name, entry, current);
            BackupRetention.pruneSafety(CustomBlocksConfig.safetyKeepCount);
            CustomBlocksConfig.load();
            SlotManager.reload();
        } catch (Exception e) {
            ResourcePackServer.resume();
            String code = IncidentRecorder.record("Granular restore failed (" + entry + " from " + name + ")", null, src.getName(), e);
            Chat.incidentError(src, "Restore failed — your data was left as it was.", code);
            return;
        }
        ResourcePackServer.resume();
        ResourcePackServer.syncToAll();
        Chat.success(src, "Restored " + CbFmt.VALUE + entry + CbFmt.OK + " from \"" + name + "\". Old " + entry
                + " saved as \"" + safety + "\" (undo with /cb backup load " + safety + " " + entry + ").");
    }


    static int delete(ServerCommandSource src, String name) {
        if (!BackupManager.exists(name)) {
            Chat.error(src, "There's no backup named \"" + name + "\". Run /cb backup list.");
            return 0;
        }
        boolean ok = BackupManager.delete(name);
        if (ok) Chat.success(src, "Deleted backup \"" + name + "\".");
        else Chat.error(src, "Couldn't delete backup \"" + name + "\".");
        return ok ? 1 : 0;
    }

    /**
     * Perform the restore on the server thread: pause the pack, flush, safe-swap the files (a safety
     * backup is taken first), then reload config + slots and rebuild/push the pack. On failure the
     * data is left as it was (BackupManager rolls the safety copy back) and the pack resumes.
     */
    static void doRestore(ServerCommandSource src, String name) {
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
            safety = BackupRestore.restore(name, current);
            BackupRetention.pruneSafety(CustomBlocksConfig.safetyKeepCount); // bound the pre-restore copies (P0 fix)
            CustomBlocksConfig.load();
            SlotManager.reload();
        } catch (Exception e) {
            ResourcePackServer.resume();
            String code = IncidentRecorder.record("Backup restore failed for \"" + name + "\"", null, src.getName(), e);
            Chat.incidentError(src, "Restore failed — your data was left as it was.", code);
            return;
        }
        ResourcePackServer.resume();    // rebuilds the pack
        ResourcePackServer.syncToAll(); // push the restored pack to clients
        int now = SlotManager.assignedSlots().size();
        Chat.success(src, "Restored from \"" + name + "\" — " + now + " block(s) now. Old state saved as \""
                + safety + "\" (undo with /cb backup load " + safety + ").");
    }
}
