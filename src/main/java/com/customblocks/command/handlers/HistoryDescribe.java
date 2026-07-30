/**
 * HistoryDescribe.java — how one undo/redo step reads in chat.
 *
 * Pure formatting, split out of HistoryCommands so that file stays under the 400-line handler gate
 * (§9.3). Nothing here mutates: it turns an {@link UndoManager.Op} into the short phrase shown in
 * "Undid glow g11a 12→8", picking the right side of each pair for the direction being applied.
 *
 * Depends on: UndoManager, SlotData, CbFmt
 * Called by:  HistoryCommands
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.core.SlotData;
import com.customblocks.core.UndoManager;

public final class HistoryDescribe {

    private HistoryDescribe() {} // static-only

    /**
     * One step's human description. When {@code undo} is true the value diff reads
     * current→restored (e.g. glow 12→8); on redo it reads restored→reapplied (8→12).
     */
    static String describe(UndoManager.Op op, boolean undo) {
        if (op.kind() == UndoManager.Kind.BATCH) {
            // A label that already carries its own "(…)" is left exactly as written — the G11
            // block-wipe entry reads "Deleted 4 blocks (Arabic Numbers)", where the parenthesis is
            // the CATEGORY, not a count, and appending a second one would read as nonsense.
            if (op.label() != null && op.label().endsWith(")")) return op.label();
            // Count BLOCKS, not children: a G11 category delete batches one CATEGORY_RECORD op
            // alongside its per-block ops, and that one is not a block.
            int n = 0;
            if (op.children() != null) {
                for (UndoManager.Op child : op.children()) {
                    if (child.kind() != UndoManager.Kind.CATEGORY_RECORD) n++;
                }
            }
            return op.label() + " (" + n + " block" + (n == 1 ? "" : "s") + ")";
        }
        if (op.kind() == UndoManager.Kind.REID) {
            String from = undo ? op.after().customId() : op.before().customId();
            String to = undo ? op.before().customId() : op.after().customId();
            return "reid " + from + CbFmt.DIM + "→" + CbFmt.RESET + " " + to;
        }
        if (op.kind() == UndoManager.Kind.FLAG) { // carries no snapshot — the id is on the payload
            return op.label() + " " + (op.flag() == null ? "?" : op.flag().id());
        }
        if (op.kind() == UndoManager.Kind.CATEGORY) { // no snapshot — the id + both sets are on the payload
            UndoManager.Membership m = op.membership();
            if (m == null) return op.label() + " ?";
            java.util.List<String> to = undo ? m.before() : m.after();
            return op.label() + " " + m.id() + " " + CbFmt.DIM + "→" + CbFmt.RESET + " "
                    + (to.isEmpty() ? "uncategorized" : String.join(", ", to));
        }
        if (op.kind() == UndoManager.Kind.CATEGORY_RECORD) { // no block at all — the category itself
            UndoManager.CategoryRecord r = op.categoryRecord();
            return op.label() + " " + (r == null ? "?" : r.key());
        }
        if (op.kind() == UndoManager.Kind.FACE_ROTATE) { // no snapshot — index+face live on the payload
            UndoManager.FaceRot fr = op.faceRot();
            if (fr == null) return op.label() + " ?";
            return "rotate " + fr.face() + " " + ((undo ? fr.oldQ() : fr.newQ()) * 90) + "°";
        }
        SlotData ref = op.before() != null ? op.before() : op.after();
        String id = ref != null ? ref.customId() : "?";
        if ((op.kind() == UndoManager.Kind.MODIFY || op.kind() == UndoManager.Kind.SHAPE)
                && op.before() != null && op.after() != null) {
            String d = undo ? diff(op.after(), op.before()) : diff(op.before(), op.after());
            return op.label() + " " + id + (d.isEmpty() ? "" : " " + d);
        }
        return op.label() + " " + id;
    }

    /** Format the single attribute that differs between two snapshots as "from→to" (or "" if none). */
    private static String diff(SlotData a, SlotData b) {
        if (a.glow() != b.glow()) return a.glow() + CbFmt.DIM + "→" + CbFmt.RESET + b.glow();
        if (a.hardness() != b.hardness()) return fmtH(a.hardness()) + CbFmt.DIM + "→" + CbFmt.RESET + fmtH(b.hardness());
        if (!eq(a.soundType(), b.soundType())) return a.soundType() + CbFmt.DIM + "→" + CbFmt.RESET + b.soundType();
        if (a.noCollision() != b.noCollision()) return solid(a.noCollision()) + CbFmt.DIM + "→" + CbFmt.RESET + solid(b.noCollision());
        if (!eq(a.category(), b.category())) return cat(a.category()) + CbFmt.DIM + "→" + CbFmt.RESET + cat(b.category());
        if (!eq(a.shape(), b.shape())) return a.shape() + CbFmt.DIM + "→" + CbFmt.RESET + b.shape();
        if (!eq(a.displayName(), b.displayName())) return "\"" + a.displayName() + "\"" + CbFmt.DIM + "→" + CbFmt.RESET + "\"" + b.displayName() + "\"";
        return "";
    }

    private static boolean eq(String x, String y) {
        return x == null ? y == null : x.equals(y);
    }

    private static String fmtH(float h) {
        return h == Math.rint(h) ? String.valueOf((int) h) : String.valueOf(h);
    }

    private static String solid(boolean noCollision) {
        return noCollision ? "passable" : "solid";
    }

    private static String cat(String c) {
        return c == null || c.isBlank() ? "(none)" : c;
    }
}
