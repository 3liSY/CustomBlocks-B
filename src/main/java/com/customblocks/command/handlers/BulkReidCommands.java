/**
 * BulkReidCommands.java
 *
 * Bulk re-id (Group 07 §D): change the custom id of many blocks at once by a pattern transform —
 * the id-counterpart of /cb bulkrename (which transforms the display name). Each matched block keeps
 * its slot index (so textures + placed blocks are untouched, no pack rebuild), exactly like the
 * single /cb reid. Modes are prefix, suffix, and replace.
 *
 * SCREEN-ONLY SINCE 2026-07-26 (G07 locked decision). The chat argument forms
 * (/cb bulkreid <filter> prefix|suffix|replace …) are gone: renaming from chat is the wrong surface
 * for an operation that needs to show what it is about to touch. /cb bulkreid takes no arguments and
 * opens the Bulk Workbench on its Re-ID op, whose preview reproduces the skip rules before Apply.
 * Typing the old form is answered with where it went, not with a Brigadier syntax error.
 *
 * The transform itself is unchanged and still lives here — the Screen calls it. Per block we SKIP (and
 * report): locked blocks, no-op transforms (newId == oldId), invalid ids (must be the same charset as
 * /cb create), and collisions — a newId already taken by a block, or already claimed by an earlier
 * block in this same batch (this also rules out unsafe id swaps). The whole batch records ONE undo
 * entry (REID children), so a single /cb undo re-ids them all back.
 *
 * Depends on: SlotManager (reId/hasId), LockManager, UndoManager, BulkConfirm, BulkChat, HudSync, Chat
 * Called by:  CommandRegistrar, BulkApply (the Screen calls applyReid directly)
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class BulkReidCommands {

    private BulkReidCommands() {} // static-only

    /** Same id charset as /cb create / /cb reid (Brigadier word()): letters, digits, _ - . + */
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9_.+-]+");

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("bulkreid")
                .executes(ctx -> BulkCommands.openOp(ctx.getSource(), "reid"))
                // The argument forms are retired, but the grammar still ACCEPTS anything so an old habit
                // gets told where the operation went instead of a bare "Incorrect argument for command".
                .then(CommandManager.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> retired(ctx.getSource()))));
    }

    /** Answer the retired chat form: say it moved, and name the one command that replaces it. */
    private static int retired(ServerCommandSource src) {
        Chat.error(src, "Re-id from chat is gone.");
        Chat.info(src, "Run " + CbFmt.OK + "/cb bulkreid" + CbFmt.BODY
                + " with no arguments — the Workbench shows every id it is about to change before you apply.");
        return 0;
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

    /** The Screen sent a mode this handler does not implement — a bug on the Screen side, not a typo. */
    static void badMode(ServerCommandSource src) {
        Chat.error(src, "That re-id mode is not one this can apply. Pick prefix, suffix, or replace in the Workbench.");
    }
}
