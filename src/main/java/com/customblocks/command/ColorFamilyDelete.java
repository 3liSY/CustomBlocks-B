/**
 * ColorFamilyDelete.java — G10 §F3: /cb colorvariants delete &lt;id&gt; (alias /cb variants delete).
 *
 * Removes a whole colour family in one step — the base block plus whichever of &lt;id&gt;_red /
 * _green / _yellow actually exist — instead of making the player run /cb delete once per member.
 *
 * Reuse, do NOT rewrite: membership comes from {@link ColorFamilyOps#takenTargets} (the same list
 * the create/overwrite forms clash-check against, so "the family" means one thing everywhere); the
 * deletion itself is {@link DeletionService#deleteCore} — the ONE shared delete rail, so a family
 * delete frees slots and swaps placed copies to "Deleted: &lt;name&gt;" markers exactly like
 * /cb delete and bulk delete do (no purple blocks left in the world). The batch-shared steps (one
 * pack rebuild, one undo entry, one HUD resync) follow BulkCommands.applyDelete.
 *
 * Held behind /cb confirm via BulkConfirm because it is destructive and multi-block; one /cb undo
 * puts the whole family back.
 *
 * Lives in its own class rather than inside ColorFamilyOps only because that file is already at its
 * §9.3 size cap.
 *
 * Depends on: ColorFamilyOps, BulkConfirm, DeletionService, SlotManager, LockManager, UndoManager,
 *             ResourcePackServer, HudSync, Chat.
 * Called by:  ColorVariantCommands (the Brigadier tree).
 */
package com.customblocks.command;

import com.customblocks.command.handlers.BulkConfirm;
import com.customblocks.core.DeletionService;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.UndoManager;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ColorFamilyDelete {

    private ColorFamilyDelete() {} // static-only

    /** /cb colorvariants delete &lt;id&gt; — ask first; the actual removal runs on /cb confirm. */
    public static int deleteFamily(ServerCommandSource src, String id) {
        MinecraftServer server = src.getServer();
        if (server == null) return 0;

        List<String> members = ColorFamilyOps.takenTargets(id);
        if (members.isEmpty()) {
            Chat.error(src, "There's no colour family called " + CbFmt.BODY + id + CbFmt.BAD + " — neither it nor "
                    + "its colours exist. Nothing was deleted.");
            return 0;
        }

        // Locked members are never deleted; say so up front so the confirm prompt is honest about
        // what will actually go, rather than reporting the skip only after the fact.
        List<String> deletable = new ArrayList<>();
        List<String> locked = new ArrayList<>();
        for (String member : members) {
            if (LockManager.isLocked(member)) locked.add(member); else deletable.add(member);
        }
        if (deletable.isEmpty()) {
            Chat.error(src, "Every block in the " + CbFmt.BODY + id + CbFmt.BAD + " family is locked ("
                    + String.join(", ", locked) + "). Nothing was deleted.");
            return 0;
        }

        BulkConfirm.request(src, () -> run(src, server, id, deletable), "delete the " + id + " colour family");
        Chat.info(src, "This will delete " + CbFmt.BODY + deletable.size() + " block"
                + (deletable.size() == 1 ? "" : "s") + CbFmt.DIM + " — " + CbFmt.BODY + String.join(", ", deletable) + CbFmt.DIM + "."
                + (locked.isEmpty() ? "" : " " + CbFmt.BAD + locked.size() + " locked will be kept: " + String.join(", ", locked) + CbFmt.DIM + ".")
                + " Type " + CbFmt.BODY + "/cb confirm" + CbFmt.DIM + " to delete, " + CbFmt.BODY + "/cb cancel" + CbFmt.DIM + " to stop.");
        return 1;
    }

    /** Confirmed: delete every listed member through the shared rail, as ONE undo entry. */
    private static void run(ServerCommandSource src, MinecraftServer server, String id, List<String> members) {
        UUID who = BulkConfirm.actor(src);
        List<UndoManager.Op> children = new ArrayList<>();
        List<String> deleted = new ArrayList<>();

        for (String member : members) {
            // Re-check on the way in — a member could have been deleted or locked during the confirm window.
            SlotData before = SlotManager.getById(member);
            if (before == null || LockManager.isLocked(member)) continue;
            byte[] texture = DeletionService.deleteCore(server, before);
            children.add(new UndoManager.Op(UndoManager.Kind.DELETE, before, null, texture, "delete"));
            deleted.add(member);
        }

        if (deleted.isEmpty()) {
            Chat.error(src, "Nothing was deleted — the " + CbFmt.BODY + id + CbFmt.BAD + " family changed while waiting "
                    + "for the confirmation. Run the command again.");
            return;
        }

        ResourcePackServer.updatePack(); // ONE debounced rebuild frees every deleted slot's texture
        UndoManager.recordBatch(who, children, "colorvariants delete " + id + " (" + deleted.size() + ")");
        HudSync.broadcast(server);       // clear the deleted identities live for everyone (G05-1)

        MutableText msg = Text.literal(CbFmt.BAD + "Deleted the " + CbFmt.BODY + id + CbFmt.BAD + " colour family — ")
                .append(Chat.hover(CbFmt.VALUE + CbFmt.UNDER + deleted.size() + " block" + (deleted.size() == 1 ? "" : "s") + CbFmt.RESET,
                        CbFmt.DIM + "Deleted:\n" + CbFmt.BODY + String.join("\n", deleted)))
                .append(Text.literal("  "))
                .append(Chat.undoButton());
        Chat.line(src, msg);
    }
}
