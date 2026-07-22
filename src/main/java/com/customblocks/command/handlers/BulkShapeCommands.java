/**
 * BulkShapeCommands.java
 *
 * Bulk shape (Group 08 §E): apply ONE block shape (slab, stairs, pillar, cross, …) to many blocks at
 * once — the shape-counterpart of /cb bulkproperty, using the same Group 07 bulk rail (scope → confirm
 * for big/"all" batches → one undo batch). The shape is the LAST token; everything before it is the
 * id list, so a multi-id scope still works:
 *
 *   /cb bulkshape <ids...> <shape>     e.g. /cb bulkshape doorframe pillar_a slab_bottom
 *
 * Per block we SKIP (and report): locked blocks, and blocks already wearing that shape (no-op). Because
 * a shape change swaps the block's MODEL, the whole batch triggers ONE debounced pack rebuild, and each
 * child is recorded as a SHAPE op so a single /cb undo reverts the batch AND rebuilds the pack (the same
 * rail /cb setshape uses). No-arg /cb bulkshape prints usage (the Bulk Workbench has no shape op).
 *
 * Depends on: BlockShapes, BulkScope, SlotManager (setShape), LockManager, UndoManager, BulkConfirm,
 *             BulkChat, HudSync, FeedbackFx, Chat
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.CustomBlocksConfig;
import com.customblocks.block.BlockShapes;
import com.customblocks.command.Chat;
import com.customblocks.core.BulkScope;
import com.customblocks.core.FeedbackFx;
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

public final class BulkShapeCommands {

    private BulkShapeCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("bulkshape")
                .executes(ctx -> { usage(ctx.getSource()); return 0; })
                .then(CommandManager.argument("args", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.SHAPE_ARGS)
                        .executes(ctx -> bulkShape(ctx.getSource(), StringArgumentType.getString(ctx, "args")))));
    }

    /** /cb bulkshape &lt;filter&gt; &lt;shape&gt; — shape is the LAST token, the rest is the filter. */
    private static int bulkShape(ServerCommandSource src, String args) {
        String[] parts = args.trim().split("\\s+");
        if (parts.length < 2) { usage(src); return 0; }
        String shape = parts[parts.length - 1].toLowerCase(Locale.ROOT);
        String scope = String.join(" ", Arrays.copyOf(parts, parts.length - 1));

        // Validate the shape ONCE up front — fail before touching any block (mirrors bulkProperty).
        if (!BlockShapes.isValid(shape)) {
            Chat.error(src, "Unknown shape \"" + shape + "\". See /cb shapelist for the choices.");
            return 0;
        }

        List<SlotData> blocks = BulkScope.resolve(scope, BulkConfirm.actor(src));
        if (blocks.isEmpty()) { Chat.error(src, "No blocks matched: " + scope); return 0; }

        int threshold = Math.max(1, CustomBlocksConfig.bulkConfirmThreshold);
        boolean needConfirm = BulkScope.isAll(scope) || blocks.size() > threshold;

        Runnable action = () -> applyShape(src, blocks, shape);
        if (needConfirm) {
            BulkConfirm.request(src, action, "bulkshape " + shape + " on " + blocks.size() + " block(s)");
            String hoverList = CbFmt.DIM + "Would set shape " + CbFmt.VALUE + shape + CbFmt.DIM + " on:\n" + CbFmt.BODY + BulkChat.columns(BulkChat.ids(blocks));
            BulkChat.confirm(src, CbFmt.BODY + "Set shape " + CbFmt.VALUE + shape + CbFmt.BODY + " on ", CbFmt.BODY + "?  ", blocks.size(), hoverList,
                    CbFmt.OK + CbFmt.BOLD + "[✔ Confirm]", CbFmt.BAD + CbFmt.BOLD + "[✖ Cancel]");
            return 1;
        }
        action.run();
        return 1;
    }

    /** Apply one shape to every (non-locked) matched block, as ONE undo batch + ONE pack rebuild. */
    static void applyShape(ServerCommandSource src, List<SlotData> blocks, String shape) {
        List<UndoManager.Op> children = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        int locked = 0, already = 0;
        for (SlotData target : blocks) {
            String id = target.customId();
            if (LockManager.isLocked(id)) { locked++; continue; }
            SlotData before = SlotManager.getById(id);
            if (before == null) continue;
            if (before.shape().equals(shape)) { already++; continue; } // already that shape — nothing to do
            SlotData after = SlotManager.setShape(id, shape);
            if (after == null) continue;
            // SHAPE (not MODIFY) so a later /cb undo also rebuilds the pack — the MODEL changed, not just state.
            children.add(new UndoManager.Op(UndoManager.Kind.SHAPE, before, after, null, "bulk shape"));
            changed.add(id);
        }
        if (changed.isEmpty()) {
            String why = locked > 0 ? " — " + locked + " locked" : "";
            if (already > 0) why += (why.isEmpty() ? " — " : ", ") + already + " already " + shape;
            Chat.error(src, "No shapes changed" + why + ".");
            return;
        }
        // G08 §B — no pack rebuild: a static slot's pack output no longer depends on its shape, so even a
        // bulk shape change is data-only. The HudSync push below re-meshes every client once.
        UndoManager.recordBatch(BulkConfirm.actor(src), children, "bulk-shape " + shape + " (" + changed.size() + ")");
        HudSync.broadcast(src.getServer()); // NO-REJOIN: HUD shape value updates live for all players (one push)

        String hoverList = CbFmt.DIM + "Set shape on " + changed.size() + " block(s):\n" + CbFmt.BODY + BulkChat.columns(changed)
                + (locked > 0 ? "\n\n" + CbFmt.BAD + locked + " locked — skipped" : "")
                + (already > 0 ? "\n" + CbFmt.FAINT + already + " already " + shape : "");
        MutableText msg = Text.literal(CbFmt.OK + "Set shape " + CbFmt.VALUE + shape + CbFmt.OK + " on ")
                .append(Chat.hover(CbFmt.VALUE + CbFmt.UNDER + changed.size() + " block" + (changed.size() == 1 ? "" : "s") + CbFmt.RESET, hoverList))
                .append(Text.literal("  "))
                // G04-UNDO-DIALECT: runButton prepends its own ▶, so a "[↩ Undo]" label rendered "▶ [↩ Undo]".
                .append(Chat.undoButton())
                .append(Text.literal(" " + CbFmt.OK + "✔"));
        if (locked > 0) msg.append(Text.literal("  " + CbFmt.FAINT + locked + " locked"));
        Chat.line(src, msg);
        FeedbackFx.fire(src, "bulk_complete");
    }

    private static void usage(ServerCommandSource src) {
        Chat.error(src, "Usage: /cb bulkshape <ids...> <shape>");
        Chat.info(src, "Shapes: " + String.join(" · ", BlockShapes.names()) + "  (see /cb shapelist)");
        Chat.info(src, BulkChat.SCOPE_HELP);
    }
}
