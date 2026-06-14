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
    public enum Kind { CREATE, DELETE, MODIFY, BATCH, REID, SHAPE, TEXTURE }

    /**
     * One reversible edit.
     *
     * @param kind         CREATE (before == null), DELETE (after == null), MODIFY (both set),
     *                     TEXTURE (pixels changed; before == after == the slot, texture/textureAfter
     *                     carry the bytes), or BATCH (children set; before/after/texture null).
     * @param before       slot state before the edit (null for CREATE / BATCH; the slot for TEXTURE).
     * @param after        slot state after the edit (null for DELETE / BATCH; the slot for TEXTURE).
     * @param texture      for DELETE: the texture that existed before deletion; for TEXTURE: the
     *                     PRE-edit bytes restored on undo (may be null).
     * @param textureAfter for TEXTURE: the POST-edit bytes re-applied on redo; null otherwise.
     * @param label        human-readable verb shown in chat ("create", "rename", "glow", "dress", …).
     * @param children     for BATCH: the child ops reverted/re-applied together as one step (null otherwise).
     */
    public record Op(Kind kind, SlotData before, SlotData after,
                     byte[] texture, byte[] textureAfter, String label, List<Op> children) {
        /** Convenience constructor for a single (non-batch) op — textureAfter + children null. */
        public Op(Kind kind, SlotData before, SlotData after, byte[] texture, String label) {
            this(kind, before, after, texture, null, label, null);
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

    private static synchronized void push(UUID player, Op op) {
        // Audit hook (Group 02): record every recorded edit to the persistent history log.
        String mlId;
        if (op.kind() == Kind.BATCH) {
            mlId = "×" + (op.children() == null ? 0 : op.children().size());
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

    /** Drop all history for everyone (call on /cb reload). */
    public static synchronized void clearAll() {
        UNDO.clear();
        REDO.clear();
    }
}
