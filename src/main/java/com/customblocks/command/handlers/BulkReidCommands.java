/**
 * BulkReidCommands.java
 *
 * Bulk re-id (Group 07 §D): change the custom id of many blocks at once by a pattern transform —
 * the id-counterpart of /cb bulkrename (which transforms the display name). Each matched block keeps
 * its slot index (so textures + placed blocks are untouched, no pack rebuild), exactly like the
 * single /cb reid. Modes mirror bulkrename:
 *
 *   /cb bulkreid <filter> prefix <text>          newId = text + oldId
 *   /cb bulkreid <filter> suffix <text>          newId = oldId + text
 *   /cb bulkreid <filter> replace <old> <new>    newId = oldId.replace(old, new)
 *
 * Per block we SKIP (and report): locked blocks, no-op transforms (newId == oldId), invalid ids
 * (must be the same charset as /cb create), and collisions — a newId already taken by a block, or
 * already claimed by an earlier block in this same batch (this also rules out unsafe id swaps).
 * The whole batch records ONE undo entry (REID children), so a single /cb undo re-ids them all back.
 * Big/"all" batches are held for /cb confirm, like the other bulk ops.
 *
 * No-arg /cb bulkreid opens the Bulk Workbench on its Re-ID op (§G07-3) — the first GUI front-end this
 * handler has ever had. The Screen's preview reproduces the skip rules above before Apply.
 *
 * Depends on: BulkScope, SlotManager (reId/hasId), LockManager, UndoManager, BulkConfirm, BulkChat,
 *             HudSync, Chat
 * Called by:  CommandRegistrar, BulkApply (the Screen calls applyReid directly)
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class BulkReidCommands {

    private BulkReidCommands() {} // static-only

    /** Same id charset as /cb create / /cb reid (Brigadier word()): letters, digits, _ - . + */
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9_.+-]+");

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("bulkreid")
                .executes(ctx -> BulkCommands.openOp(ctx.getSource(), "reid"))
                .then(CommandManager.argument("args", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.RENAME_ARGS)
                        .executes(ctx -> bulkReid(ctx.getSource(), StringArgumentType.getString(ctx, "args")))));
    }

    private static int bulkReid(ServerCommandSource src, String args) {
        String[] t = args.trim().split("\\s+");
        // Same grammar as /cb bulkrename: the id list runs until the mode keyword (Group 04 §A7).
        int m = BulkCommands.modeIndex(t);
        if (m < 1 || t.length < m + 2) { usage(src); return 0; }
        String filter = String.join(" ", Arrays.copyOf(t, m));
        String mode = t[m].toLowerCase(Locale.ROOT);
        String a = "";
        String b = "";
        switch (mode) {
            case "prefix", "suffix" -> a = String.join(" ", Arrays.copyOfRange(t, m + 1, t.length));
            case "replace" -> {
                if (t.length < m + 3) { usage(src); return 0; }
                a = t[m + 1];
                b = t[m + 2];
            }
            default -> { usage(src); return 0; }
        }

        List<SlotData> blocks = BulkScope.resolve(filter, BulkConfirm.actor(src));
        if (blocks.isEmpty()) { Chat.error(src, "No blocks matched: " + filter); return 0; }

        int threshold = Math.max(1, CustomBlocksConfig.bulkConfirmThreshold);
        boolean needConfirm = BulkScope.isAll(filter) || blocks.size() > threshold;

        final String fmode = mode, fa = a, fb = b;
        Runnable action = () -> applyReid(src, blocks, fmode, fa, fb);
        if (needConfirm) {
            BulkConfirm.request(src, action, "re-id " + blocks.size() + " block(s)");
            String hoverList = CbFmt.DIM + reidWhat(mode, a, b) + " on:\n" + CbFmt.BODY + BulkChat.columns(BulkChat.ids(blocks));
            BulkChat.confirm(src, CbFmt.BODY + "Re-id ", CbFmt.BODY + " (" + reidWhat(mode, a, b) + ")?  ", blocks.size(), hoverList,
                    CbFmt.OK + CbFmt.BOLD + "[✔ Confirm]", CbFmt.BAD + CbFmt.BOLD + "[✖ Cancel]");
            return 1;
        }
        action.run();
        return 1;
    }

    /** Apply the id transform to every eligible matched block, as one undo batch. */
    static void applyReid(ServerCommandSource src, List<SlotData> blocks, String mode, String a, String b) {
        // Start from every currently-assigned id; a newId is a collision if it's taken by anything
        // other than the block being moved (this also blocks swaps within the batch).
        Set<String> taken = new HashSet<>();
        for (SlotData d : SlotManager.assignedSlots()) taken.add(d.customId());

        List<UndoManager.Op> children = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        int locked = 0, collided = 0, invalid = 0, unchanged = 0;

        for (SlotData target : blocks) {
            String oldId = target.customId();
            if (LockManager.isLocked(oldId)) { locked++; continue; }
            String newId = switch (mode) {
                case "prefix"  -> a + oldId;
                case "suffix"  -> oldId + a;
                case "replace" -> oldId.replace(a, b);
                default        -> oldId;
            };
            if (newId.equals(oldId)) { unchanged++; continue; }
            if (!VALID_ID.matcher(newId).matches()) { invalid++; continue; }
            if (taken.contains(newId)) { collided++; continue; }

            SlotData before = SlotManager.getById(oldId);
            if (before == null) continue;
            SlotData after = SlotManager.reId(oldId, newId);
            if (after == null) continue;
            children.add(new UndoManager.Op(UndoManager.Kind.REID, before, after, null, "bulk reid"));
            changed.add(oldId + " " + CbFmt.DIM + "→" + CbFmt.BODY + " " + newId);
            taken.remove(oldId);
            taken.add(newId);
        }

        if (changed.isEmpty()) {
            BulkResult.record(src, "No ids changed" + skipSuffix(locked, collided, invalid, unchanged) + "."); // X3
            Chat.error(src, "No ids changed" + skipSuffix(locked, collided, invalid, unchanged) + ".");
            return;
        }
        UndoManager.recordBatch(BulkConfirm.actor(src), children, "bulk-reid (" + changed.size() + ")");

        String hoverList = CbFmt.DIM + "Re-id'd " + changed.size() + " block(s):\n" + CbFmt.BODY + BulkChat.columns(changed)
                + skipHover(locked, collided, invalid, unchanged);
        MutableText msg = Text.literal(CbFmt.OK + "Re-id'd ")
                .append(Chat.hover(CbFmt.VALUE + CbFmt.UNDER + changed.size() + " block" + (changed.size() == 1 ? "" : "s") + CbFmt.RESET, hoverList))
                .append(Text.literal("  "))
                // G04-UNDO-DIALECT: runButton prepends its own ▶, so a "[↩ Undo]" label rendered "▶ [↩ Undo]".
                .append(Chat.undoButton())
                .append(Text.literal(" " + CbFmt.OK + "✔"));
        int skipped = locked + collided + invalid + unchanged;
        BulkResult.record(src, "Re-id'd " + changed.size() + " block(s)" + (skipped > 0 ? " · " + skipped + " skipped" : "")); // X3
        if (skipped > 0) msg.append(Text.literal("  " + CbFmt.FAINT + skipped + " skipped"));
        Chat.line(src, msg);
        HudSync.broadcast(src.getServer()); // NO-REJOIN: bulk id change shows live for ALL players (was actor-only)
    }

    /**
     * Apply an EXPLICIT per-block re-id map from the Hub's B9 editor. Each pair is {oldId, newId}; the same
     * skip rules as {@link #applyReid} apply, re-checked here at apply time: locked, unchanged (new == old),
     * invalid id, a new id already taken by another block, and a batch-clash (two pairs to the same new id).
     * One undo entry for the whole batch.
     */
    public static void applyReidExplicit(ServerCommandSource src, List<String[]> pairs) {
        List<UndoManager.Op> children = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        int locked = 0, collided = 0, invalid = 0, unchanged = 0, missing = 0;
        Set<String> seenNew = new HashSet<>();

        for (String[] pair : pairs) {
            String oldId = pair[0], newId = pair[1];
            SlotData before = SlotManager.getById(oldId);
            if (before == null) { missing++; continue; }
            if (LockManager.isLocked(oldId)) { locked++; continue; }
            if (newId.equals(oldId)) { unchanged++; continue; }
            if (!VALID_ID.matcher(newId).matches()) { invalid++; continue; }
            if (!seenNew.add(newId)) { collided++; continue; }        // same new id twice in this batch (clash)
            SlotData after = SlotManager.reId(oldId, newId);
            if (after == null) { collided++; continue; }              // new id already taken by an existing block
            children.add(new UndoManager.Op(UndoManager.Kind.REID, before, after, null, "bulk reid"));
            changed.add(oldId + " " + CbFmt.DIM + "→" + CbFmt.BODY + " " + newId);
        }

        if (changed.isEmpty()) {
            BulkResult.record(src, "No ids changed" + skipSuffix(locked, collided, invalid, unchanged)
                    + (missing > 0 ? ", " + missing + " gone" : "") + "."); // X3
            Chat.error(src, "No ids changed" + skipSuffix(locked, collided, invalid, unchanged)
                    + (missing > 0 ? (skipSuffix(locked, collided, invalid, unchanged).isEmpty() ? " — " : ", ") + missing + " gone" : "") + ".");
            return;
        }
        UndoManager.recordBatch(BulkConfirm.actor(src), children, "bulk-reid (" + changed.size() + ")");

        String hoverList = CbFmt.DIM + "Re-id'd " + changed.size() + " block(s):\n" + CbFmt.BODY + BulkChat.columns(changed)
                + skipHover(locked, collided, invalid, unchanged);
        MutableText msg = Text.literal(CbFmt.OK + "Re-id'd ")
                .append(Chat.hover(CbFmt.VALUE + CbFmt.UNDER + changed.size() + " block" + (changed.size() == 1 ? "" : "s") + CbFmt.RESET, hoverList))
                .append(Text.literal("  "))
                // G04-UNDO-DIALECT: runButton prepends its own ▶, so a "[↩ Undo]" label rendered "▶ [↩ Undo]".
                .append(Chat.undoButton())
                .append(Text.literal(" " + CbFmt.OK + "✔"));
        int skipped = locked + collided + invalid + unchanged + missing;
        BulkResult.record(src, "Re-id'd " + changed.size() + " block(s)" + (skipped > 0 ? " · " + skipped + " skipped" : "")); // X3
        if (skipped > 0) msg.append(Text.literal("  " + CbFmt.FAINT + skipped + " skipped"));
        Chat.line(src, msg);
        HudSync.broadcast(src.getServer()); // NO-REJOIN: live for ALL players
    }

    private static String reidWhat(String mode, String a, String b) {
        return switch (mode) {
            case "prefix" -> "add id-prefix \"" + a + "\"";
            case "suffix" -> "add id-suffix \"" + a + "\"";
            default       -> "replace \"" + a + "\" → \"" + b + "\" in ids";
        };
    }

    /** Short reason tail for the "nothing changed" error. */
    private static String skipSuffix(int locked, int collided, int invalid, int unchanged) {
        List<String> parts = new ArrayList<>();
        if (locked > 0)    parts.add(locked + " locked");
        if (collided > 0)  parts.add(collided + " would collide");
        if (invalid > 0)   parts.add(invalid + " invalid id");
        if (unchanged > 0) parts.add(unchanged + " unchanged");
        return parts.isEmpty() ? "" : " — " + String.join(", ", parts);
    }

    /** Hover footnote listing what was skipped, when at least one block did change. */
    private static String skipHover(int locked, int collided, int invalid, int unchanged) {
        String tail = skipSuffix(locked, collided, invalid, unchanged);
        return tail.isEmpty() ? "" : "\n\n" + CbFmt.BAD + tail.substring(3) + " — skipped";
    }

    static void usage(ServerCommandSource src) {
        Chat.error(src, "Usage: /cb bulkreid <ids...> prefix <text> | suffix <text> | replace <old> <new>");
        Chat.info(src, "Changes many block IDs by a pattern (keeps slots).");
        Chat.info(src, BulkChat.SCOPE_HELP);
    }
}
