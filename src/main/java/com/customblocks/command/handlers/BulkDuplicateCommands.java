/**
 * BulkDuplicateCommands.java
 *
 * Bulk duplicate (Group 07): copy every matched block into a new block, texture + attributes and
 * all (SlotManager.dupe clones everything). Each copy gets a unique id derived from the source —
 * "<id>_copy", then "_copy2", "_copy3"… so a batch never collides. The whole batch records ONE
 * UndoManager entry (CREATE children), so a single /cb undo removes every copy; ONE pack rebuild
 * runs after the batch. Big/all batches confirm in chat first.
 *
 *   /cb bulkduplicate <filter>     /cb bulkduplicate     (no args → Bulk Workbench, Duplicate op)
 *
 * Locked sources are fine to copy (duplicating reads, never edits, the original) — so, unlike Edit/Rename/
 * Move/Re-ID/Delete, this op does NOT skip them. Slot exhaustion is reported as a skipped count. Kept in its
 * own handler to stay under the 400-line gate (§9.3).
 *
 * Depends on: BulkScope, BulkConfirm, BulkChat, SlotManager, UndoManager, ResourcePackServer, Chat
 * Called by:  CommandRegistrar, BulkApply (the Screen calls applyDuplicate directly)
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.BulkScope;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.UndoManager;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class BulkDuplicateCommands {

    private BulkDuplicateCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("bulkduplicate")
                .executes(ctx -> BulkCommands.openOp(ctx.getSource(), "duplicate"))
                .then(CommandManager.argument("filter", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.FILTER_ONLY)
                        .executes(ctx -> bulkDuplicate(ctx.getSource(), StringArgumentType.getString(ctx, "filter")))));
    }

    private static int bulkDuplicate(ServerCommandSource src, String filter) {
        List<SlotData> blocks = BulkScope.resolve(filter, BulkConfirm.actor(src));
        if (blocks.isEmpty()) { Chat.error(src, "No blocks matched: " + filter); return 0; }

        int threshold = Math.max(1, CustomBlocksConfig.bulkConfirmThreshold);
        boolean needConfirm = BulkScope.isAll(filter) || blocks.size() > threshold;

        Runnable action = () -> applyDuplicate(src, blocks);
        if (needConfirm) {
            BulkConfirm.request(src, action, "duplicate " + blocks.size() + " block(s)");
            String hoverList = CbFmt.DIM + "Will copy " + blocks.size() + " block(s):\n" + CbFmt.BODY
                    + BulkChat.columns(BulkChat.ids(blocks));
            BulkChat.confirm(src, CbFmt.BODY + "Duplicate ", CbFmt.BODY + "?  ", blocks.size(), hoverList,
                    CbFmt.OK + CbFmt.BOLD + "[✔ Confirm]", CbFmt.BAD + CbFmt.BOLD + "[✖ Cancel]");
            return 1;
        }
        action.run();
        return 1;
    }

    static void applyDuplicate(ServerCommandSource src, List<SlotData> blocks) {
        List<UndoManager.Op> children = new ArrayList<>();
        List<String> made = new ArrayList<>();
        int failed = 0;
        for (SlotData target : blocks) {
            // Duplicating a copy bumps the number instead of stacking suffixes: stone_copy → stone_copy2,
            // never stone_copy_copy (§G07-4 edge ruling). copyBase strips a trailing _copy / _copyN first.
            String newId = uniqueId(copyBase(target.customId()) + "_copy");
            if (newId == null) { failed++; continue; }
            SlotData d = SlotManager.dupe(target.customId(), newId);
            if (d == null) { failed++; continue; } // no free slot / id race
            children.add(new UndoManager.Op(UndoManager.Kind.CREATE, null, d, null, "duplicate"));
            made.add(newId);
        }
        if (made.isEmpty()) {
            BulkResult.record(src, "Nothing duplicated" + (failed > 0 ? " — no free slots for " + failed + " block(s)" : "") + "."); // X3
            Chat.error(src, "Nothing duplicated" + (failed > 0 ? " — no free slots for " + failed + " block(s)" : "") + ".");
            return;
        }
        ResourcePackServer.updatePack(); // ONE rebuild — the copies' textures were just written
        HudSync.broadcast(src.getServer()); // NO-REJOIN: push the new copies' name/slot data so the HUD + held item show live (was missing → copies looked blank until rejoin)
        UndoManager.recordBatch(BulkConfirm.actor(src), children, "bulk-duplicate (" + made.size() + ")");
        BulkResult.record(src, "Duplicated " + made.size() + " block(s)"
                + (failed > 0 ? " · " + failed + " skipped (no free slot)" : "")); // X3

        String hoverList = CbFmt.DIM + "Created " + made.size() + " copy(ies):\n" + CbFmt.BODY + BulkChat.columns(made)
                + (failed > 0 ? "\n\n" + CbFmt.BAD + failed + " skipped — no free slot" : "");
        MutableText msg = Text.literal(CbFmt.OK + "Duplicated into ")
                .append(Chat.hover(CbFmt.VALUE + CbFmt.UNDER + made.size() + " new block" + (made.size() == 1 ? "" : "s") + CbFmt.RESET, hoverList))
                .append(Text.literal("  "))
                // G04-UNDO-DIALECT: runButton prepends its own ▶, so a "[↩ Undo]" label rendered "▶ [↩ Undo]".
                .append(Chat.undoButton())
                .append(Text.literal(" " + CbFmt.OK + "✔"));
        if (failed > 0) msg.append(Text.literal("  " + CbFmt.FAINT + failed + " skipped"));
        Chat.line(src, msg);
    }

    /** A trailing "_copy" or "_copy<number>" — stripped so a copy-of-a-copy bumps the number, not the suffix. */
    private static final java.util.regex.Pattern COPY_SUFFIX = java.util.regex.Pattern.compile("_copy\\d*$");

    /** "stone" → "stone"; "stone_copy" → "stone"; "stone_copy2" → "stone". The base a fresh "_copy" hangs off. */
    private static String copyBase(String id) {
        return COPY_SUFFIX.matcher(id).replaceFirst("");
    }

    /** "<base>", then "<base>2", "<base>3"… — the first id not already in use, or null if none free. */
    private static String uniqueId(String base) {
        if (!SlotManager.hasId(base)) return base;
        for (int i = 2; i <= 999; i++) {
            String candidate = base + i;
            if (!SlotManager.hasId(candidate)) return candidate;
        }
        return null;
    }
}
