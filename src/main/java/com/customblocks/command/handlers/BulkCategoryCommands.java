/**
 * BulkCategoryCommands.java
 *
 * Bulk "move to category" (Group 07): reassign the category of every matched block at once, the
 * batch counterpart of /cb setcategory. Kept in its own handler so BulkCommands stays under the
 * 400-line gate (§9.3); shares the bulk plumbing (BulkScope filters, BulkConfirm guard for big/all
 * batches, BulkChat hover list, one UndoManager batch entry).
 *
 *   /cb bulkcategory <filter> <category>   — set the category (or "none" to clear)
 *   /cb bulkcategory                        — opens the Bulk Workbench on its Move op (§G07-3)
 *
 * Category is metadata only — no texture or resource-pack rebuild (mirrors the single setter).
 * Locked blocks are skipped. Mutation goes through SlotManager (the single source of truth).
 *
 * Depends on: BulkScope, BulkConfirm, BulkChat, SlotManager, LockManager, UndoManager, Chat
 * Called by:  CommandRegistrar, BulkApply (the Screen calls applyCategory directly)
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.BulkScope;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.UndoManager;
import com.customblocks.network.HudSync;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class BulkCategoryCommands {

    private BulkCategoryCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("bulkcategory")
                .executes(ctx -> BulkCommands.openOp(ctx.getSource(), "category"))
                .then(CommandManager.argument("args", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.CATEGORY_ARGS)
                        .executes(ctx -> bulkCategory(ctx.getSource(), StringArgumentType.getString(ctx, "args")))));
    }

    // ── /cb bulkcategory <filter> <category> ──────────────────────────────────

    private static int bulkCategory(ServerCommandSource src, String args) {
        String[] parts = args.trim().split("\\s+");
        if (parts.length < 2) { usage(src); return 0; }
        String catRaw = parts[parts.length - 1];
        String scope  = String.join(" ", Arrays.copyOf(parts, parts.length - 1));
        String cat    = normalize(catRaw);

        List<SlotData> blocks = BulkScope.resolve(scope, BulkConfirm.actor(src));
        if (blocks.isEmpty()) { Chat.error(src, "No blocks matched: " + scope); return 0; }

        int threshold = Math.max(1, CustomBlocksConfig.bulkConfirmThreshold);
        boolean needConfirm = BulkScope.isAll(scope) || blocks.size() > threshold;

        Runnable action = () -> applyCategory(src, blocks, cat);
        if (needConfirm) {
            String what = cat.isEmpty() ? "clear the category of" : "move to \"" + cat + "\"";
            BulkConfirm.request(src, action, "move " + blocks.size() + " block(s)");
            String hoverList = CbFmt.DIM + "Will " + what + ":\n" + CbFmt.BODY + BulkChat.columns(BulkChat.ids(blocks));
            BulkChat.confirm(src, CbFmt.BODY + "Move ", CbFmt.BODY + " (" + what + ")?  ", blocks.size(), hoverList,
                    CbFmt.OK + CbFmt.BOLD + "[✔ Confirm]", CbFmt.BAD + CbFmt.BOLD + "[✖ Cancel]");
            return 1;
        }
        action.run();
        return 1;
    }

    /** Set the category on every (non-locked) matched block, as one undo batch. */
    static void applyCategory(ServerCommandSource src, List<SlotData> blocks, String cat) {
        List<UndoManager.Op> children = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        int locked = 0;
        for (SlotData target : blocks) {
            String id = target.customId();
            if (LockManager.isLocked(id)) { locked++; continue; }
            SlotData before = SlotManager.getById(id);
            if (before == null) continue;
            SlotData after = SlotManager.setCategory(id, cat);
            if (after == null) continue;
            children.add(new UndoManager.Op(UndoManager.Kind.MODIFY, before, after, null, "bulk category"));
            changed.add(id);
        }
        if (changed.isEmpty()) {
            BulkResult.record(src, "No blocks moved" + (locked > 0 ? " — all " + locked + " locked" : "") + "."); // X3
            Chat.error(src, "No blocks moved" + (locked > 0 ? " — all " + locked + " matched are locked" : "") + ".");
            return;
        }
        UndoManager.recordBatch(BulkConfirm.actor(src), children, "bulk-category (" + changed.size() + ")");
        BulkResult.record(src, (cat.isEmpty() ? "Cleared category on " : "Moved to \"" + cat + "\": ") + changed.size()
                + " block(s)" + (locked > 0 ? " · " + locked + " locked skipped" : "")); // X3

        String verb = cat.isEmpty() ? "Cleared the category of " : "Moved ";
        String tail = cat.isEmpty() ? "" : " " + CbFmt.OK + "to " + CbFmt.VALUE + cat;
        String hoverList = CbFmt.DIM + (cat.isEmpty() ? "Cleared" : "Moved to " + cat) + " for "
                + changed.size() + " block(s):\n" + CbFmt.BODY + BulkChat.columns(changed)
                + (locked > 0 ? "\n\n" + CbFmt.BAD + locked + " locked — skipped" : "");
        MutableText msg = Text.literal(CbFmt.OK + verb)
                .append(Chat.hover(CbFmt.VALUE + CbFmt.UNDER + changed.size() + " block" + (changed.size() == 1 ? "" : "s") + CbFmt.RESET, hoverList))
                .append(Text.literal(tail))
                .append(Text.literal("  "))
                // G04-UNDO-DIALECT: runButton prepends its own ▶, so a "[↩ Undo]" label rendered "▶ [↩ Undo]".
                .append(Chat.undoButton())
                .append(Text.literal(" " + CbFmt.OK + "✔"));
        if (locked > 0) msg.append(Text.literal("  " + CbFmt.FAINT + locked + " locked"));
        Chat.line(src, msg);
        HudSync.broadcast(src.getServer()); // NO-REJOIN: bulk category change shows live for all players (one push)
    }

    /** "none"/"clear"/"uncategorized" → "" (clear); otherwise lowercase (mirrors /cb setcategory). */
    static String normalize(String raw) {
        String c = raw.trim();
        if (c.equalsIgnoreCase("none") || c.equalsIgnoreCase("clear") || c.equalsIgnoreCase("uncategorized")) {
            return "";
        }
        return c.toLowerCase(Locale.ROOT);
    }

    private static void usage(ServerCommandSource src) {
        Chat.error(src, "Usage: /cb bulkcategory <ids...> <category>   (use 'none' to clear)");
        Chat.info(src, BulkChat.SCOPE_HELP);
    }
}
