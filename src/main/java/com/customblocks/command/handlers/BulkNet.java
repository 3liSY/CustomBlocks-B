/**
 * BulkNet.java — Group 07 §G07-3 (Bulk Workbench Screen — server side).
 *
 * Receives {@link BulkActionPayload} from the Workbench, validates it, hops to the server thread, and calls
 * {@link BulkApply} for that op with STRUCTURED arguments (never a re-parsed command string), then pushes a
 * fresh snapshot back so the open Screen refreshes in place. Modelled on BuzzerPanelNet.
 *
 * Trust model: this accepts a packet from any client, so nothing here trusts the payload. The op must be a
 * known code, the scope is re-resolved server-side by BulkScope, and locked blocks are skipped inside the
 * handlers regardless of what the client's preview claimed. No permission gate: /cb bulk… commands carry no
 * `requires()` today, so gating the Screen would make it stricter than the command it replaces.
 *
 * Depends on: BulkActionPayload, BulkApply, BulkSnapshot, CreationStudioBridge, Chat
 * Called by:  CustomBlocksMod.onInitialize (registers this as the payload receiver)
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.SlotManager;
import com.customblocks.core.UndoManager;
import com.customblocks.gui.chest.BulkSession;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.ListSelection;
import com.customblocks.gui.chest.Nav;
import com.customblocks.network.payloads.BulkActionPayload;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public final class BulkNet {

    private BulkNet() {} // static-only

    /** Payload receiver body: hop to the server thread, then apply. */
    public static void handle(BulkActionPayload payload, ServerPlayerEntity player) {
        var server = player.getServer();
        if (server != null) server.execute(() -> apply(payload, player));
    }

    private static void apply(BulkActionPayload p, ServerPlayerEntity player) {
        ServerCommandSource src = player.getCommandSource();
        switch (p.op()) {
            // The client asked for a specific tab, so honour it rather than the "keep current tab" push.
            case BulkActionPayload.REFRESH -> {
                BulkSnapshot.open(player, p.p1());
                return;
            }

            // Leaves the Workbench: the studio's edit-load payload replaces the screen. No re-push.
            case BulkActionPayload.OPEN_EDITOR -> {
                CreationStudioBridge.openStudioEdit(src, p.p1());
                return;
            }

            // Group 12 hand-off: the player ticked blocks in "pick" mode. Park the ids where the chest
            // Export Dashboard reads them, then re-open it on its format phase. Leaves the Workbench.
            case BulkActionPayload.PICK_DONE -> {
                pickDone(player, p.scope());
                return;
            }

            case BulkActionPayload.PROPERTY  -> BulkApply.property(src, p.scope(), p.p1(), p.p2());
            case BulkActionPayload.RENAME    -> BulkApply.rename(src, p.scope(), p.p1(), p.p2(), p.p3());
            case BulkActionPayload.CATEGORY  -> BulkApply.category(src, p.scope(), p.p1());
            case BulkActionPayload.DUPLICATE -> BulkApply.duplicate(src, p.scope());
            case BulkActionPayload.EXPORT    -> BulkApply.export(src, p.scope(), p.p1());
            case BulkActionPayload.FLAG      -> BulkApply.flag(src, p.p1(), p.scope());
            case BulkActionPayload.REID      -> BulkApply.reid(src, p.scope(), p.p1(), p.p2(), p.p3());
            case BulkActionPayload.REID_MAP  -> BulkApply.reidMap(src, p.scope());
            case BulkActionPayload.DELETE    -> BulkApply.delete(src, p.scope());
            case BulkActionPayload.RECOLOR   -> BulkApply.recolor(src, p.scope(), p.p1());

            // In-screen Undo/Redo buttons + the history panel's jump-back-N (§G07-4): drive the SAME per-player
            // UndoManager stack as /cb undo. p1 = how many steps (blank/1 = one). Records a bottom-right toast for
            // the actor, then falls through to pushAll so the open Hub (and every player's) refreshes in place.
            case BulkActionPayload.UNDO -> doHistory(player, src, count(p.p1()), true);
            case BulkActionPayload.REDO -> doHistory(player, src, count(p.p1()), false);

            default -> {
                Chat.error(src, "Unknown bulk action.");
                return;
            }
        }
        // The op ran (or reported its own failure). Refresh EVERY player who has the Hub open, not just the
        // actor (§G07-4 MP ruling) — a favorite/lock/delete by player A must show live on player B's Hub. Blank
        // tab keeps each viewer on their own tab; closed clients ignore the push.
        BulkSnapshot.pushAll(player.getServer());
    }

    /** Parse the jump-count from the payload's p1 (blank/garbage → 1, floored at 1). */
    private static int count(String p1) {
        try { return Math.max(1, Integer.parseInt(p1.trim())); } catch (Exception e) { return 1; }
    }

    /** Undo (or redo) up to {@code n} steps; toast the actor with what happened; report to chat. */
    private static void doHistory(ServerPlayerEntity player, ServerCommandSource src, int n, boolean undo) {
        // Grab the label of the FIRST step (top of the relevant stack) before we move it, for the toast.
        var stack = undo ? UndoManager.undoStack(player.getUuid()) : UndoManager.redoStack(player.getUuid());
        String first = stack.isEmpty() ? null : prettyLabel(stack.get(0));
        int done = 0;
        for (int i = 0; i < n; i++) {
            boolean ok = undo ? HistoryCommands.undoOnce(player) : HistoryCommands.redoOnce(player);
            if (!ok) break;
            done++;
        }
        if (done == 0) { Chat.info(src, undo ? "Nothing to undo." : "Nothing to redo."); return; }
        String verb = undo ? "Undid" : "Redid";
        BulkToast.record(player, verb + " " + ((done == 1 && first != null) ? first : done + " changes"));
        Chat.success(src, verb + " " + (done == 1 ? "the last change" : done + " changes") + ".");
    }

    /** Human-friendly op label for the toast + history panel: strips the "bulk-" prefix and capitalises. */
    static String prettyLabel(UndoManager.Op op) {
        String l = op == null || op.label() == null ? "change" : op.label();
        if (l.startsWith("bulk-")) l = l.substring(5);
        return l.isEmpty() ? "change" : Character.toUpperCase(l.charAt(0)) + l.substring(1);
    }

    /**
     * Hand the Workbench's ticked ids back to Group 12's chest Export Dashboard.
     *
     * Nothing here trusts the packet: the pick only lands if the player actually started one from the
     * dashboard (listPickForExport), and every id is re-checked against SlotManager before it is stored, so a
     * crafted payload can neither open the dashboard unprompted nor smuggle an unknown id into the export set.
     */
    private static void pickDone(ServerPlayerEntity player, String scope) {
        BulkSession session = BulkSession.get(player.getUuid());
        if (!session.listPickForExport) return;

        List<String> picked = new ArrayList<>();
        for (String raw : scope.split(",")) {
            String id = raw.trim();
            if (!id.isEmpty() && SlotManager.getById(id) != null) picked.add(id);
        }
        if (picked.isEmpty()) {
            Chat.error(player.getCommandSource(), "Nothing was picked — tick some blocks first.");
            return;
        }
        ListSelection.clear(player.getUuid());
        ListSelection.addAll(player.getUuid(), picked);
        session.listPickForExport = false;
        GuiRouter.openFresh(player, Nav.MenuKey.of(Nav.Dest.EXPORT_DASHBOARD, "selection"));
    }
}
