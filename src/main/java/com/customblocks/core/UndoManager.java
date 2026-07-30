/**
 * UndoManager.java
 *
 * Responsibility: Per-player undo/redo history for block edits (FR-06). Each player has
 * an isolated pair of capped stacks, so one player's undo never touches another player's
 * blocks (the Phase 6 milestone). This is pure history: it stores immutable SlotData
 * snapshots and hands them back; the actual state restore is performed by the caller
 * (HistoryCommands) through SlotManager, keeping this class free of Minecraft/server types.
 *
 * Clean-room note: the old UndoManager was ~1,170 lines of disk-snapshot delta machinery.
 * This is the minimal version covering the edits we actually have (create / delete / rename
 * / dupe / glow / hardness / sound). Texture-level (retexture) undo is a later slice.
 *
 * Depends on: SlotData, CustomBlocksConfig (maxUndoDepth)
 * Called by:  command handlers (record*), HistoryCommands (undo/redo)
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UndoManager {

    private UndoManager() {} // static-only

    /** What kind of change an Op represents (drives how undo/redo reverses it). */
    public enum Kind { CREATE, DELETE, MODIFY, BATCH, REID, SHAPE, TEXTURE, RETEXTURE, FLAG, FACE_ROTATE,
                       CATEGORY, CATEGORY_RECORD }

    /**
     * A lock / favorite flip, the one edit that lives OUTSIDE SlotData.
     *
     * Lock state is a flat set in LockManager and favorites are a per-player map in FavoritesManager —
     * neither is a field on the SlotData snapshot every other Kind restores. So a flag op carries its own
     * payload instead of a snapshot pair, and HistoryCommands flips it back through those two managers.
     *
     * @param id    the block id the flag applies to.
     * @param which "lock" or "favorite".
     * @param on    what this op SET the flag to (undo restores !on, redo re-applies on).
     * @param owner whose favorites list — favorites are per-player, so undo must hit the ORIGINAL actor's
     *              list, not the clicker's (they differ under undoMode=global). Null for "lock", which is
     *              a server-wide fact.
     */
    public record Flag(String id, String which, boolean on, UUID owner) {}

    /**
     * A per-face quarter-turn flip (G06 §G Face mode) — the one edit, like {@link Flag}, that lives
     * OUTSIDE SlotData. Face rotations are a flat store in {@link FaceRotations} keyed by slot index +
     * face, not a field on the SlotData snapshot. So a FACE_ROTATE op carries this payload instead of a
     * snapshot pair; HistoryCommands restores it through FaceRotations and rebuilds the model.
     *
     * @param index the slot index the rotation applies to.
     * @param face  the face name ("down".."east").
     * @param oldQ  the quarter-turn BEFORE the rotate (undo restores this).
     * @param newQ  the quarter-turn AFTER the rotate (redo re-applies this).
     */
    public record FaceRot(int index, String face, int oldQ, int newQ) {}

    /**
     * A change to which categories a block belongs to (G11) — the third edit, like {@link Flag} and
     * {@link FaceRot}, that lives OUTSIDE SlotData.
     *
     * Membership is a SET in {@link CategoryMembershipStore}, not a field on the snapshot, so a
     * MODIFY op carrying before/after SlotData could only ever restore the derived one-word display
     * shadow — it would look like the undo worked while the real memberships stayed changed. This
     * payload carries the whole set on each side instead, so undo puts a block back in exactly the
     * categories it was in.
     *
     * @param id     the block id whose memberships changed.
     * @param before every category key it held BEFORE (undo restores this).
     * @param after  every category key it held AFTER (redo re-applies this).
     */
    public record Membership(String id, List<String> before, List<String> after) {}

    /**
     * A category RECORD appearing or disappearing (G11) — the twin of {@link Membership}, which
     * only ever moves blocks between categories.
     *
     * Deleting a category drops its colour tag, icon, description and sort order along with it, so
     * an undo built from membership ops alone would hand back a category stripped of everything
     * that made it recognizable. This payload carries the whole record, serialized by
     * {@link CategoryMetadataStore#snapshot}, on each side.
     *
     * @param key    the normalized category key.
     * @param before the record as it was BEFORE (undo restores this); "" = there was none.
     * @param after  the record as it was AFTER (redo re-applies this); "" = deleted.
     */
    public record CategoryRecord(String key, String before, String after) {}

    /**
     * One reversible edit.
     *
     * @param kind         CREATE (before == null), DELETE (after == null), MODIFY (both set),
     *                     TEXTURE (pixels changed; before == after == the slot, texture/textureAfter
     *                     carry the bytes), BATCH (children set; before/after/texture null), or
     *                     FLAG (flag set; everything else null).
     * @param before       slot state before the edit (null for CREATE / BATCH / FLAG; the slot for TEXTURE).
     * @param after        slot state after the edit (null for DELETE / BATCH / FLAG; the slot for TEXTURE).
     * @param texture      for DELETE: the texture that existed before deletion; for TEXTURE: the
     *                     PRE-edit bytes restored on undo (may be null).
     * @param textureAfter for TEXTURE: the POST-edit bytes re-applied on redo; null otherwise.
     * @param label        human-readable verb shown in chat ("create", "rename", "glow", "dress", …).
     * @param children     for BATCH: the child ops reverted/re-applied together as one step (null otherwise).
     * @param flag         for FLAG: the lock/favorite payload (null otherwise).
     * @param faceRot      for FACE_ROTATE: the per-face quarter-turn payload (null otherwise).
     */
    public record Op(Kind kind, SlotData before, SlotData after,
                     byte[] texture, byte[] textureAfter, String label, List<Op> children, Flag flag, FaceRot faceRot,
                     Membership membership, CategoryRecord categoryRecord) {
        /** Pre-CATEGORY_RECORD 10-arg shape (CATEGORY era) — categoryRecord null. */
        public Op(Kind kind, SlotData before, SlotData after,
                  byte[] texture, byte[] textureAfter, String label, List<Op> children, Flag flag, FaceRot faceRot,
                  Membership membership) {
            this(kind, before, after, texture, textureAfter, label, children, flag, faceRot, membership, null);
        }

        /** Pre-CATEGORY 9-arg shape (FACE_ROTATE era) — membership null. */
        public Op(Kind kind, SlotData before, SlotData after,
                  byte[] texture, byte[] textureAfter, String label, List<Op> children, Flag flag, FaceRot faceRot) {
            this(kind, before, after, texture, textureAfter, label, children, flag, faceRot, null, null);
        }

        /** Pre-FACE_ROTATE 8-arg shape (FLAG era) — faceRot null. */
        public Op(Kind kind, SlotData before, SlotData after,
                  byte[] texture, byte[] textureAfter, String label, List<Op> children, Flag flag) {
            this(kind, before, after, texture, textureAfter, label, children, flag, null, null, null);
        }

        /** Pre-FLAG 7-arg shape — every existing caller still compiles unchanged. */
        public Op(Kind kind, SlotData before, SlotData after,
                  byte[] texture, byte[] textureAfter, String label, List<Op> children) {
            this(kind, before, after, texture, textureAfter, label, children, null, null, null, null);
        }

        /** Convenience constructor for a single (non-batch) op — textureAfter + children null. */
        public Op(Kind kind, SlotData before, SlotData after, byte[] texture, String label) {
            this(kind, before, after, texture, null, label, null, null, null, null, null);
        }
    }

    private static final Map<UUID, Deque<Op>> UNDO = new ConcurrentHashMap<>();
    private static final Map<UUID, Deque<Op>> REDO = new ConcurrentHashMap<>();

    /** Sentinel key for the single shared stack used in server-wide ("global") mode. */
    private static final UUID GLOBAL_KEY = new UUID(0L, 0L);

    /**
     * Stack key for an actor under the current undo mode. Returns the shared GLOBAL_KEY in
     * server-wide mode (so any player's undo hits one history), or the player's own UUID in
     * per-player mode. Null = not recordable (per-player mode with no player, e.g. console).
     */
    private static UUID keyFor(UUID player) {
        return "per_player".equals(CustomBlocksConfig.undoMode) ? player : GLOBAL_KEY;
    }

    // ── Recording (called by command handlers after a successful mutation) ────

    public static void recordCreate(UUID player, SlotData after) {
        if (after == null) return;
        push(player, new Op(Kind.CREATE, null, after, null, "create"));
    }

    public static void recordDelete(UUID player, SlotData before, byte[] texture) {
        if (before == null) return;
        push(player, new Op(Kind.DELETE, before, null, texture, "delete"));
    }

    public static void recordModify(UUID player, SlotData before, SlotData after, String label) {
        if (before == null || after == null) return;
        push(player, new Op(Kind.MODIFY, before, after, null, label));
    }

    /**
     * Record an id change (/cb reid): {@code before} carries the OLD id, {@code after} the NEW id.
     * Reversed by re-id-ing back (see HistoryCommands) — reId is its own inverse, so no snapshot
     * restore is needed. Distinct from MODIFY because the map key itself moved.
     */
    public static void recordReid(UUID player, SlotData before, SlotData after) {
        if (before == null || after == null) return;
        push(player, new Op(Kind.REID, before, after, null, "reid"));
    }

    /**
     * Record a shape change (/cb setshape, /cb clearshape). Like MODIFY but flagged distinct so
     * undo/redo also rebuilds the resource pack — the block's MODEL changes, not just live state.
     */
    public static void recordShape(UUID player, SlotData before, SlotData after) {
        if (before == null || after == null) return;
        push(player, new Op(Kind.SHAPE, before, after, null, "shape"));
    }

    /**
     * Record a per-face rotation (Omni-Tool Face mode, G06 §G). Like FLAG, it carries no SlotData
     * snapshot — the quarter-turn lives in {@link FaceRotations} — so it stores a {@link FaceRot}
     * payload. HistoryCommands restores it and rebuilds the model (the block's MODEL changes).
     */
    public static void recordFaceRotate(UUID player, int index, String face, int oldQ, int newQ) {
        push(player, new Op(Kind.FACE_ROTATE, null, null, null, null, "rotate", null, null,
                new FaceRot(index, face, oldQ, newQ)));
    }

    /**
     * Build a FLAG child op (for a bulk batch). Not pushed — hand it to {@link #recordBatch}.
     *
     * @param on what the caller SET the flag to, so undo can restore the opposite.
     */
    public static Op flagOp(Flag flag, String label) {
        return new Op(Kind.FLAG, null, null, null, null, label, null, flag);
    }

    /**
     * Record a single lock / unlock / favorite / unfavorite as one undo step (G04 §5, G07-BULK-UNDO).
     *
     * Before this, flag flips recorded NOTHING — /cb undo silently skipped straight past them to an
     * older, unrelated edit. The chip on a lock line now means the same thing it means everywhere else.
     */
    public static void recordFlag(UUID player, Flag flag, String label) {
        if (flag == null) return;
        push(player, flagOp(flag, label));
    }

    /**
     * Build a CATEGORY child op (for a bulk batch). Not pushed — hand it to {@link #recordBatch}.
     * Returns null when the sets match, so a no-op assignment doesn't earn an undo step.
     */
    public static Op membershipOp(String blockId, java.util.Collection<String> before,
                                  java.util.Collection<String> after, String label) {
        if (blockId == null || before == null || after == null) return null;
        List<String> b = List.copyOf(before), a = List.copyOf(after);
        if (b.equals(a)) return null;
        return new Op(Kind.CATEGORY, null, null, null, null, label, null, null, null,
                new Membership(blockId, b, a));
    }

    /**
     * Build a CATEGORY_RECORD child op (for a batch). Not pushed — hand it to {@link #recordBatch}.
     * Returns null when the two snapshots match, so a no-op earns no undo step.
     *
     * @param before {@link CategoryMetadataStore#snapshot} before the change ("" = no record).
     * @param after  the snapshot after it ("" = the record was deleted).
     */
    public static Op categoryRecordOp(String key, String before, String after, String label) {
        if (key == null || key.isEmpty()) return null;
        String b = before == null ? "" : before, a = after == null ? "" : after;
        if (b.equals(a)) return null;
        return new Op(Kind.CATEGORY_RECORD, null, null, null, null, label, null, null, null, null,
                new CategoryRecord(key, b, a));
    }

    /** Record one block's category-membership change as a single undo step (G11). */
    public static void recordMembership(UUID player, String blockId, java.util.Collection<String> before,
                                        java.util.Collection<String> after, String label) {
        Op op = membershipOp(blockId, before, after, label);
        if (op != null) push(player, op);
    }

    /**
     * Record several edits as ONE undo step (Group 07): a whole bulk operation reverts in a
     * single /cb undo. Children are applied/reverted together, newest-batch-first like any op.
     */
    public static void recordBatch(UUID player, List<Op> children, String label) {
        if (children == null || children.isEmpty()) return;
        push(player, new Op(Kind.BATCH, null, null, null, null, label, List.copyOf(children)));
    }

    /**
     * Record a texture replacement (/cb dress, and reusable by future pixel ops). {@code slot}
     * is the unchanged metadata snapshot (carries the index + id); {@code before}/{@code after}
     * are the PNG bytes so undo restores the old pixels and redo re-applies the new ones. The
     * caller does the actual TextureStore.save + pack rebuild (HistoryCommands), keeping this
     * class free of image/server types.
     */
    public static void recordTexture(UUID player, SlotData slot, byte[] before, byte[] after, String label) {
        if (slot == null || before == null || after == null) return;
        push(player, new Op(Kind.TEXTURE, slot, slot, before, after, label, null));
    }

    /**
     * Record a /cb retexture (Group 14 / ADR-014 Step 2). Unlike {@link #recordTexture} the metadata can
     * change too (animated↔static), so {@code before}/{@code after} are DISTINCT slot snapshots — undo
     * restores the whole {@code before} slot (incl. its AnimData) + the {@code beforeTex} pixels, and redo
     * restores {@code after} + {@code afterTex}. Pixel byte arrays may be null (a slot with no prior bake);
     * HistoryCommands then just restores the snapshot. The caller does the TextureStore.save + pack rebuild.
     */
    public static void recordRetexture(UUID player, SlotData before, SlotData after, byte[] beforeTex, byte[] afterTex) {
        if (before == null || after == null) return;
        push(player, new Op(Kind.RETEXTURE, before, after, beforeTex, afterTex, "retexture", null));
    }

    private static synchronized void push(UUID player, Op op) {
        // Audit hook (Group 02): record every recorded edit to the persistent history log.
        String mlId;
        if (op.kind() == Kind.BATCH) {
            mlId = "×" + (op.children() == null ? 0 : op.children().size());
        } else if (op.kind() == Kind.FLAG) {
            mlId = op.flag() != null ? op.flag().id() : "?"; // FLAG carries no snapshot — the id lives on the payload
        } else if (op.kind() == Kind.FACE_ROTATE) {
            mlId = op.faceRot() != null ? "slot " + op.faceRot().index() : "?"; // FACE_ROTATE: id is the slot index
        } else if (op.kind() == Kind.CATEGORY_RECORD) {
            mlId = op.categoryRecord() != null ? op.categoryRecord().key() : "?"; // no block — the id IS the category
        } else {
            mlId = op.after() != null ? op.after().customId()
                    : (op.before() != null ? op.before().customId() : "?");
        }
        MutationLog.record(player, op.label(), mlId);
        UUID key = keyFor(player);
        if (key == null) return; // per-player mode with no player → not undoable
        Deque<Op> u = UNDO.computeIfAbsent(key, k -> new ArrayDeque<>());
        u.addFirst(op);
        int cap = Math.max(1, CustomBlocksConfig.maxUndoDepth);
        while (u.size() > cap) u.removeLast();
        // A fresh edit invalidates the redo timeline.
        Deque<Op> r = REDO.get(key);
        if (r != null) r.clear();
    }

    // ── Undo / Redo (move an Op between stacks; the caller applies the change) ──

    /** Remove + return the next op to undo (moved onto the redo stack). Null if none. */
    public static synchronized Op undo(UUID player) {
        UUID key = keyFor(player);
        if (key == null) return null;
        Deque<Op> u = UNDO.get(key);
        if (u == null || u.isEmpty()) return null;
        Op op = u.removeFirst();
        REDO.computeIfAbsent(key, k -> new ArrayDeque<>()).addFirst(op);
        return op;
    }

    /** Remove + return the next op to redo (moved back onto the undo stack). Null if none. */
    public static synchronized Op redo(UUID player) {
        UUID key = keyFor(player);
        if (key == null) return null;
        Deque<Op> r = REDO.get(key);
        if (r == null || r.isEmpty()) return null;
        Op op = r.removeFirst();
        UNDO.computeIfAbsent(key, k -> new ArrayDeque<>()).addFirst(op);
        return op;
    }

    public static synchronized int undoSize(UUID player) {
        UUID key = keyFor(player);
        Deque<Op> u = key == null ? null : UNDO.get(key);
        return u == null ? 0 : u.size();
    }

    public static synchronized int redoSize(UUID player) {
        UUID key = keyFor(player);
        Deque<Op> r = key == null ? null : REDO.get(key);
        return r == null ? 0 : r.size();
    }

    /** Snapshot of the player's undo stack, most-recent first (for the visual undo menu). */
    public static synchronized List<Op> undoStack(UUID player) {
        UUID key = keyFor(player);
        Deque<Op> u = key == null ? null : UNDO.get(key);
        return u == null ? new ArrayList<>() : new ArrayList<>(u);
    }

    /** Snapshot of the player's redo stack, most-recent first (for the visual redo menu). */
    public static synchronized List<Op> redoStack(UUID player) {
        UUID key = keyFor(player);
        Deque<Op> r = key == null ? null : REDO.get(key);
        return r == null ? new ArrayList<>() : new ArrayList<>(r);
    }

    /** Drop a player's history (call on disconnect / reload). */
    public static synchronized void clearPlayer(UUID player) {
        UNDO.remove(player);
        REDO.remove(player);
    }

    /**
     * Clear this actor's undo AND redo history under the current undo mode (`/cb undo clear`).
     * Unlike clearPlayer it honours keyFor (so it works in server-wide mode too). Returns the
     * number of undo steps that were removed — for the "Cleared N undo steps" report.
     */
    public static synchronized int clearHistory(UUID player) {
        UUID key = keyFor(player);
        if (key == null) return 0;
        Deque<Op> u = UNDO.get(key);
        int removed = u == null ? 0 : u.size();
        if (u != null) u.clear();
        Deque<Op> r = REDO.get(key);
        if (r != null) r.clear();
        return removed;
    }

    /** Drop all history for everyone (call on /cb reload). */
    public static synchronized void clearAll() {
        UNDO.clear();
        REDO.clear();
    }
}
