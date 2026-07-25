/**
 * BackupScreenCommands.java — Group 09 (split out of BackupCommands, 2026-07-23 §9.3 no-monolith rule).
 *
 * The Backup Screen server bridge plus the secondary /cb backup subcommands: the Screen push/action
 * handler (create/restore/delete/rename/protect), rename + protect helpers, and the read-only preview +
 * contents browse, and verify. The core lifecycle (register, save, list, restore, delete) stays in
 * {@link BackupCommands}; this class calls its package-private startSave/doRestore/delete/validateNewName
 * so there is one save/restore rail. Backups are LOCAL-ONLY — no cloud upload/pull (dropped 2026-07-23,
 * see TG9 §F: R2 needs a billing card the owner declined). Category/note sharing keeps its own cloud path.
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.BackupArchive;
import com.customblocks.core.BackupIntegrity;
import com.customblocks.core.BackupManager;
import com.customblocks.core.BackupRestore;
import com.customblocks.core.IncidentRecorder;
import com.customblocks.gui.GuiMode;
import com.customblocks.network.payloads.BackupActionPayload;
import com.customblocks.network.payloads.OpenGuiPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public final class BackupScreenCommands {

    private BackupScreenCommands() {} // static-only

    // ── Backup Screen bridge (G09-A4) ─────────────────────────────────────────

    /** Send the Backup Screen to a player with the current backup list (opens fresh or refreshes). */
    public static void openScreen(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new OpenGuiPayload(GuiMode.BACKUP_SCREEN.id, com.customblocks.core.BackupView.screenJson()));
    }

    /**
     * Perform a Backup Screen action server-side (authoritative), then re-push the refreshed screen. The
     * screen's opaque confirm modal is the confirmation, so restore/delete run immediately here — the
     * /cb confirm gate only guards the chat path. Runs on the server thread.
     */
    public static void handleScreenAction(ServerPlayerEntity player, String action, String name, String arg) {
        ServerCommandSource src = player.getCommandSource();
        switch (action == null ? "" : action) {
            case BackupActionPayload.ACTION_CREATE -> {
                MinecraftServer server = player.getServer();
                if (server == null) return;
                String n = (name == null || name.isBlank()) ? BackupManager.generatedName(BackupManager.Kind.MANUAL) : name.trim();
                if (!BackupCommands.validateNewName(src, n)) { openScreen(player); return; }
                BackupCommands.startSave(src, server, n, () -> openScreen(player)); // onDone refreshes once the save lands
            }
            case BackupActionPayload.ACTION_RESTORE -> { BackupCommands.doRestore(src, name); openScreen(player); }
            case BackupActionPayload.ACTION_DELETE  -> { BackupCommands.delete(src, name);    openScreen(player); }
            case BackupActionPayload.ACTION_RENAME  -> { renameBackup(src, name, arg); openScreen(player); }
            case BackupActionPayload.ACTION_PROTECT -> { toggleProtect(src, name);     openScreen(player); }
            default -> openScreen(player);
        }
    }

    private static void renameBackup(ServerCommandSource src, String oldName, String newName) {
        if (newName == null || newName.isBlank()) { Chat.error(src, "Enter a new name for the backup."); return; }
        try {
            boolean ok = BackupArchive.rename(oldName, newName.trim());
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
        if (BackupArchive.setProtected(name, nowProtected)) {
            Chat.info(src, nowProtected ? "Protected \"" + name + "\" — it won't be auto-pruned."
                                        : "Unprotected \"" + name + "\".");
        } else {
            Chat.error(src, "Couldn't update protection for \"" + name + "\".");
        }
    }

    // ── Read-only / maintenance subcommands ───────────────────────────────────

    /** Dry-run: show exactly what a restore of {@code name} would change, without touching live data. */
    static int preview(ServerCommandSource src, String name) {
        BackupRestore.RestorePlan plan = BackupRestore.restorePlan(name);
        if (plan == null) {
            Chat.error(src, "Backup \"" + name + "\" is missing or unreadable. Run /cb backup list to see what's there.");
            return 0;
        }
        Chat.info(src, "If you restore " + CbFmt.VALUE + name + CbFmt.DIM + ", this is what changes (nothing has changed yet):");
        Chat.raw(src, Text.literal("  " + CbFmt.OK + "replace " + CbFmt.DIM + "(overwritten by the backup): "
                + (plan.replaced().isEmpty() ? "none" : String.join(", ", plan.replaced()))));
        Chat.raw(src, Text.literal("  " + CbFmt.OK + "add " + CbFmt.DIM + "(restored, not present now): "
                + (plan.added().isEmpty() ? "none" : String.join(", ", plan.added()))));
        Chat.raw(src, Text.literal("  " + CbFmt.BAD + "remove " + CbFmt.DIM + "(moved to the safety copy, gone after): "
                + (plan.removed().isEmpty() ? "none" : String.join(", ", plan.removed()))));
        Chat.info(src, "Run " + CbFmt.OK + "/cb backup load " + name + CbFmt.DIM + " to apply it (a safety copy is saved first).");
        return 1;
    }

    /** Read-only browse (P5): list what a backup contains, without changing anything. */
    static int contents(ServerCommandSource src, String name) {
        if (!BackupManager.exists(name)) {
            Chat.error(src, "There's no backup named \"" + name + "\". Run /cb backup list.");
            return 0;
        }
        List<String> entries = BackupRestore.contents(name);
        if (entries.isEmpty()) { Chat.info(src, "Backup \"" + name + "\" is empty or unreadable."); return 1; }
        Chat.info(src, "Backup " + CbFmt.VALUE + name + CbFmt.DIM + " contains " + entries.size() + " item(s):");
        Chat.raw(src, Text.literal("  " + CbFmt.DIM + String.join(", ", entries)));
        Chat.info(src, "Restore just one with " + CbFmt.OK + "/cb backup load " + name + " <item>" + CbFmt.DIM
                + ", or all with " + CbFmt.OK + "/cb backup load " + name + CbFmt.DIM + ".");
        return 1;
    }

    /** Deep-verify one backup: every pooled file exists and still hashes to its recorded checksum. */
    static int verifyOne(ServerCommandSource src, String name) {
        BackupIntegrity.VerifyResult r = BackupIntegrity.verify(name, true);
        if (r.ok()) {
            Chat.success(src, "Backup \"" + name + "\" is healthy — " + r.checked() + " file(s) verified, no problems.");
        } else {
            Chat.error(src, "Backup \"" + name + "\" has " + r.problems().size() + " problem(s):");
            int shown = 0;
            for (String p : r.problems()) {
                if (shown++ >= 8) { Chat.raw(src, Text.literal("  " + CbFmt.DIM + "…and " + (r.problems().size() - 8) + " more.")); break; }
                Chat.raw(src, Text.literal("  " + CbFmt.BAD + "• " + CbFmt.DIM + p));
            }
        }
        return r.ok() ? 1 : 0;
    }

    /** Shallow-verify every backup (blob existence only, no re-hash) and summarize healthy vs. broken. */
    static int verifyAll(ServerCommandSource src) {
        List<BackupManager.BackupInfo> all = BackupManager.list();
        if (all.isEmpty()) { Chat.info(src, "No backups to verify yet."); return 1; }
        int healthy = 0;
        List<String> broken = new java.util.ArrayList<>();
        for (BackupManager.BackupInfo b : all) {
            if (BackupIntegrity.verify(b.name(), false).ok()) healthy++;
            else broken.add(b.name());
        }
        Chat.info(src, "Verified " + all.size() + " backup(s): " + CbFmt.OK + healthy + " healthy"
                + CbFmt.DIM + (broken.isEmpty() ? "." : ", " + CbFmt.BAD + broken.size() + " with problems" + CbFmt.DIM + "."));
        for (String n : broken) Chat.raw(src, Text.literal("  " + CbFmt.BAD + "• " + CbFmt.DIM + n
                + " — run /cb backup verify " + n + " for detail."));
        return 1;
    }
}
