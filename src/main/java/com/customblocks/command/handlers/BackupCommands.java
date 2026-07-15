/**
 * BackupCommands.java — Group 09, Slices 1 + 2.
 *
 *   /cb backup                 — (no args) open the advanced backup GUI (BackupMenu). Console → chat usage.
 *   /cb backup save [name]     — snapshot current data into backups/<name>/ (Slice 1, read-only).
 *   /cb backup list            — list saved backups, newest first (Slice 1).
 *   /cb backup load <name>     — replace live data with a backup. Requires /cb confirm. (Slice 2)
 *                                ("restore" is kept as a hidden alias for load.)
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
import com.customblocks.cloud.CloudVaultClient;
import com.customblocks.cloud.VaultHistory;
import com.customblocks.command.Chat;
import com.customblocks.core.BackupManager;
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
                                        StringArgumentType.getString(ctx, "name"))))));

        // /cb backupgui — open the advanced backup GUI directly (matches /cb bulkgui …).
        root.then(CommandManager.literal("backupgui").executes(ctx -> openOrUsage(ctx.getSource())));
    }

    /** Bare /cb backup: open the Backup Screen (G09-A4) as a player; console gets the chat usage list. */
    private static int openOrUsage(ServerCommandSource src) {
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            openScreen(p);
            return 1;
        }
        return usage(src);
    }

    private static int usage(ServerCommandSource src) {
        Chat.info(src, "Backup commands (run " + CbFmt.VALUE + "/cb backup" + CbFmt.DIM + " in-game for the GUI):");
        Chat.raw(src, Text.literal("  " + CbFmt.VALUE + "/cb backup save [name] " + CbFmt.DIM + "- save a point-in-time backup"));
        Chat.raw(src, Text.literal("  " + CbFmt.VALUE + "/cb backup list " + CbFmt.DIM + "- list saved backups"));
        Chat.raw(src, Text.literal("  " + CbFmt.VALUE + "/cb backup load <name> " + CbFmt.DIM + "- load one back (asks to confirm)"));
        Chat.raw(src, Text.literal("  " + CbFmt.VALUE + "/cb backup delete <name> " + CbFmt.DIM + "- remove a backup"));
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
                maybeCloudSync(src, server, name); // best-effort cloud push (gated); never fails the save
            } catch (Exception e) {
                String code = IncidentRecorder.record("Backup save failed for \"" + name + "\"", null, src.getName(), e);
                server.execute(() -> Chat.incidentError(src, "Couldn't save the backup.", code));
            }
        }, "CustomBlocks-Backup");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Best-effort cloud sync (Group 20 §C): if cloud sharing is ON and a vaultEndpoint is set, zip the
     * just-saved backup and push it to the vault, recording the returned code in /cb vault codes. The
     * local backup already succeeded, so any failure here is reported but NEVER fails the save. Runs on
     * the backup worker thread (network I/O); user-facing messages hop back to the server thread.
     */
    private static void maybeCloudSync(ServerCommandSource src, MinecraftServer server, String name) {
        if (!CustomBlocksConfig.cloudShareEnabled || !CloudVaultClient.isConfigured()) return;
        byte[] zip = BackupManager.zip(name);
        if (zip == null || zip.length == 0) {
            server.execute(() -> Chat.error(src, "Cloud sync skipped — couldn't package backup \"" + name + "\"."));
            return;
        }
        server.execute(() -> Chat.info(src, "Syncing backup \"" + name + "\" to the cloud…"));
        String code = CloudVaultClient.uploadBackup(name, zip);
        server.execute(() -> {
            if (code == null) {
                Chat.error(src, "Cloud sync failed — the backup is saved locally. Check vaultEndpoint and the worker's /backup route.");
            } else {
                VaultHistory.record("backup", code, name, src);
                Chat.success(src, "☁ Backup synced — code " + CbFmt.VALUE + code + CbFmt.OK + ". Find it again with " + CbFmt.BODY + "/cb vault codes" + CbFmt.OK + ".");
            }
        });
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
        // A player gets the Backup Screen (G09-A4); console/command-block keeps the chat list.
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            openScreen(p);
            return 1;
        }
        List<BackupManager.BackupInfo> backups = BackupManager.list();
        if (backups.isEmpty()) {
            Chat.info(src, "No backups yet. Make one with /cb backup save [name].");
            return 1;
        }
        Chat.info(src, "Backups (" + backups.size() + ", newest first):");
        for (BackupManager.BackupInfo b : backups) {
            String label = BackupManager.displayLabel(b);
            String blocks = b.blocks() >= 0 ? (b.blocks() + " block(s)") : "?";
            if (label.equals(b.name())) {
                // Manual save: the name is both the label and the restore-by id; show time separately.
                Chat.raw(src, Text.literal("  " + CbFmt.VALUE + label + " " + CbFmt.DIM + "- " + BackupManager.friendlyTime(b)
                        + " " + CbFmt.FAINT + "· " + CbFmt.DIM + blocks));
            } else {
                // Auto: friendly label already carries the time; raw restore-by id shown dim in parens.
                Chat.raw(src, Text.literal("  " + CbFmt.VALUE + label + " " + CbFmt.FAINT + "(" + b.name() + ") " + CbFmt.FAINT + "· " + CbFmt.DIM + blocks));
            }
        }
        return 1;
    }

    // ── Backup Screen bridge (G09-A4) ─────────────────────────────────────────

    /** Send the Backup Screen to a player with the current backup list (opens fresh or refreshes). */
    public static void openScreen(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new OpenGuiPayload(GuiMode.BACKUP_SCREEN.id, BackupManager.screenJson()));
    }

    /**
     * Perform a Backup Screen action server-side (authoritative), then re-push the refreshed screen.
     * The screen's opaque confirm modal is the confirmation, so restore/delete run immediately here —
     * the /cb confirm gate only guards the chat path. Runs on the server thread.
     */
    public static void handleScreenAction(ServerPlayerEntity player, String action, String name, String arg) {
        ServerCommandSource src = player.getCommandSource();
        switch (action == null ? "" : action) {
            case BackupActionPayload.ACTION_CREATE -> {
                MinecraftServer server = player.getServer();
                if (server == null) return;
                String n = (name == null || name.isBlank()) ? BackupManager.timestampName("backup") : name.trim();
                if (!validateNewName(src, n)) { openScreen(player); return; }
                startSave(src, server, n, () -> openScreen(player)); // onDone refreshes once the save lands
            }
            case BackupActionPayload.ACTION_RESTORE -> { doRestore(src, name); openScreen(player); }
            case BackupActionPayload.ACTION_DELETE  -> { delete(src, name);    openScreen(player); }
            case BackupActionPayload.ACTION_RENAME  -> { renameBackup(src, name, arg); openScreen(player); }
            case BackupActionPayload.ACTION_PROTECT -> { toggleProtect(src, name);     openScreen(player); }
            default -> openScreen(player);
        }
    }

    private static void renameBackup(ServerCommandSource src, String oldName, String newName) {
        if (newName == null || newName.isBlank()) { Chat.error(src, "Enter a new name for the backup."); return; }
        try {
            boolean ok = BackupManager.rename(oldName, newName.trim());
            if (ok) Chat.success(src, "Renamed backup \"" + oldName + "\" → \"" + newName.trim() + "\".");
            else Chat.error(src, "There's no backup named \"" + oldName + "\".");
        } catch (Exception e) {
            String code = IncidentRecorder.record("Backup rename failed for \"" + oldName + "\"", null, src.getName(), e);
            Chat.incidentError(src, "Couldn't rename the backup.", code);
        }
    }

    private static void toggleProtect(ServerCommandSource src, String name) {
        boolean nowProtected = false;
        boolean found = false;
        for (BackupManager.BackupInfo b : BackupManager.list()) {
            if (b.name().equals(name)) { nowProtected = !b.protectedFromPrune(); found = true; break; }
        }
        if (!found) { Chat.error(src, "There's no backup named \"" + name + "\"."); return; }
        if (BackupManager.setProtected(name, nowProtected)) {
            Chat.info(src, nowProtected ? "Protected \"" + name + "\" — it won't be auto-pruned."
                                        : "Unprotected \"" + name + "\".");
        } else {
            Chat.error(src, "Couldn't update protection for \"" + name + "\".");
        }
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
