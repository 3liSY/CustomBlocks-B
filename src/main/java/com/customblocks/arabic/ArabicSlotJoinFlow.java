/**
 * ArabicSlotJoinFlow.java — Group 13 / G13-25 CP2+CP3(+CP3b): the auto-join re-flow for REAL
 * letter slots.
 *
 * DUAL-SIDE port of the proven block/ArabicJoinFlow (the old live-render system's updater), with
 * ONE mechanical difference: the old flow changed a form FIELD on its BlockEntity; this flow SWAPS
 * the placed block to the sibling form-slot's registered block — the exact live-swap mechanism
 * ColorVariantService.swapPlaced already ships for Square colour-swaps (world.setBlockState with
 * the target's default state + LIGHT). Everything else is the same walk:
 *
 *   membership — a placed SlotBlock whose SlotData carries a LETTER ArabicMeta (numbers have no
 *                letter char and never join) and whose BlockEntity facing matches the run's.
 *   axis       — the word runs along the horizontal axis PERPENDICULAR to the glyph facing;
 *                rightDir = facing.getOpposite().rotateYClockwise() (reader looks along -facing).
 *   run walk   — whole-run, capped at MAX_RUN, stepping over a single air gap (MAX_GAP) so the
 *                back-face mirror survives breaking a middle letter (owner-confirmed behaviour).
 *   back face  — block k's BACK draws the tile of block N-1-k (mirror partner) so the word reads
 *                correctly from behind; stored as a slot index on the BlockEntity (arabicBackSlot).
 *
 * Facing lives on the shared AnimSlotBlockEntity NBT — NOT a blockstate property (the FACING
 * property is exactly what forced the reverted CP1 partition; see GROUP_13_ARABIC.md §G13-25).
 *
 * CP3b — CLIENT PREDICTION (the MP fix for "not instant / dim / back missing", 2026-07-03): the
 * SAME flow also runs on the client the tick a letter is placed/broken, exactly like the old
 * system (ArabicLetterBlock ran its flow on BOTH sides) and like ClientSwapPredictor (ADR-009).
 * The server stays authoritative; the client paints the identical result ahead of the round-trip
 * (swaps with NOTIFY_LISTENERS|FORCE_STATE, BE stamps without sync), so the authoritative packets
 * reconcile with no visible change. Metadata source per session (the G05 lesson): singleplayer /
 * LAN host read the live in-process SlotManager; a REMOTE client's SlotManager is stale, so it
 * reads the server-synced ClientSlotCache through the {@link ClientView} seam (common code cannot
 * import a client class — the client entrypoint installs the adapter, ADR-009 pattern).
 *
 * Re-entrancy: the flow's own setBlockState swaps re-trigger SlotBlock.onStateReplaced; the
 * IN_FLOW gate makes those nested hook calls no-ops. Per-THREAD (ThreadLocal): in singleplayer
 * the server thread and the predicting client thread each run their own independent flow.
 *
 * Missing sibling (a colour-variant form-slot not created yet): the swap is skipped and the block
 * keeps its current form — degrade, never crash. Checkpoint 4's sibling-linked colour creation
 * makes this unreachable for coloured letters; base black always exists (ArabicSlotBootstrap).
 *
 * Depends on: SlotManager/SlotData/ArabicMeta (identity), ArabicSlotBootstrap.slotId (sibling ids),
 *             ArabicJoining (form brain), ArabicGlyphs (glyph char), AnimSlotBlockEntity (facing +
 *             back-partner stamps), SlotBlock (LIGHT, slot index)
 * Called by:  SlotBlock.onPlaced / onStateReplaced (data-gated, guarded)
 */
package com.customblocks.arabic;

import com.customblocks.block.AnimSlotBlockEntity;
import com.customblocks.block.SlotBlock;
import com.customblocks.core.ArabicMeta;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ArabicSlotJoinFlow {

    private ArabicSlotJoinFlow() {} // static-only

    /** Safety cap on a single word run (a 64-letter word is already absurd). */
    private static final int MAX_RUN = 64;
    /** Max consecutive EMPTY cells the span walk bridges (back-mirror only) — one broken letter. */
    private static final int MAX_GAP = 1;

    /** True while this flow is swapping blocks — nested SlotBlock hooks must no-op. Per-THREAD:
     *  in singleplayer the server thread and the predicting client thread (CP3b) each run their
     *  own flow, so a shared plain flag would let one side's run gate the other's out. */
    private static final ThreadLocal<Boolean> IN_FLOW = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static boolean inFlow() { return IN_FLOW.get(); }

    /**
     * CP3b — client-only metadata view for REMOTE sessions (dedicated server / LAN guest), where
     * this JVM's SlotManager holds its OWN stale slots.json (the G05 lesson): the client
     * entrypoint installs an adapter over the server-synced ClientSlotCache (HudSync "ar" tuple).
     * Null on a server JVM; never consulted in singleplayer / on a LAN host.
     */
    public interface ClientView {
        @Nullable ArabicMeta arabicMeta(int slotIndex);
        @Nullable Integer indexForId(String customId);
        int glowFor(int slotIndex);
    }
    public static volatile ClientView CLIENT_VIEW = null;

    /** Server always runs (authoritative). The client runs the same flow as an instant visual
     *  prediction (CP3b) — only when its metadata source is trustworthy: the in-process
     *  SlotManager (singleplayer / LAN host) or the installed synced-cache view (remote). */
    private static boolean canRun(World world) {
        return !world.isClient() || !SlotBlock.CLIENT_REMOTE_SESSION || CLIENT_VIEW != null;
    }

    // ── entry points (SlotBlock hooks) ─────────────────────────────────────────

    /** A letter slot was placed: stamp its facing (join a neighbour's word axis, else face the
     *  placer — the proven ArabicLetterBlock placement logic), then re-flow its whole run.
     *  Runs on BOTH sides (CP3b): the server authoritatively, the client as instant prediction
     *  (vanilla calls onPlaced for the locally predicted block, same as the old system). */
    public static void onPlaced(World world, BlockPos pos, @Nullable LivingEntity placer) {
        if (inFlow() || !canRun(world)) return;
        ArabicMeta meta = letterMetaAt(world, pos);
        if (meta == null) return; // number / normal block — no facing, no joining
        Direction player = (placer != null) ? placer.getHorizontalFacing() : Direction.SOUTH;
        Direction facing = placementFacing(world, pos, player);
        if (world.getBlockEntity(pos) instanceof AnimSlotBlockEntity be) {
            if (be.setArabicFacing(facing)) be.sync();
        }
        IN_FLOW.set(true);
        try {
            recomputeRun(world, collectRun(world, pos, facing), facing);
        } finally {
            IN_FLOW.set(false);
        }
    }

    /** A letter slot was removed (break / Deleter sweep / any block change): re-flow the runs its
     *  neighbours belong to. All four horizontal sides are checked — a superset of the old in-axis
     *  pair; a perpendicular neighbour's run recomputes to itself (no-op). Runs on BOTH sides
     *  (CP3b): the client predicts the re-flow the tick the break happens, mirroring onPlaced. */
    public static void onBreak(World world, BlockPos pos) {
        if (inFlow() || !canRun(world)) return;
        IN_FLOW.set(true);
        try {
            for (Direction d : Direction.Type.HORIZONTAL) {
                BlockPos n = pos.offset(d);
                Direction f = facingAt(world, n);
                if (f != null && isJoinAt(world, n, f)) {
                    recomputeRun(world, collectRun(world, n, f), f);
                }
            }
        } finally {
            IN_FLOW.set(false);
        }
    }

    /** G13-25 CP4: re-flow the run {@code pos} belongs to after an in-place block swap (Square
     *  colour swap) re-stamped its facing. The swap replaced the BlockEntity, so the nested
     *  onStateReplaced re-flow ran while this letter briefly had no facing (out of its word);
     *  one clean whole-run recompute puts every form/mirror right again. Server-side caller. */
    public static void reflowAfterSwap(World world, BlockPos pos) {
        if (inFlow() || !canRun(world)) return;
        Direction f = facingAt(world, pos);
        if (f == null || letterMetaAt(world, pos) == null) return;
        IN_FLOW.set(true);
        try {
            recomputeRun(world, collectRun(world, pos, f), f);
        } finally {
            IN_FLOW.set(false);
        }
    }

    // ── placement facing (port of ArabicLetterBlock.joinFacing, confirmed TG §F) ──

    /** Facing so an adjacent letter ends up on our word axis: copy a neighbour already on its word
     *  line, else make the touch direction the word axis; no letter neighbour → furnace convention
     *  (glyph faces the placer). */
    private static Direction placementFacing(World world, BlockPos pos, Direction playerFacing) {
        for (Direction d : Direction.Type.HORIZONTAL) {
            BlockPos np = pos.offset(d);
            if (letterMetaAt(world, np) == null) continue;
            Direction nf = facingAt(world, np);
            if (nf != null && nf.getAxis() != d.getAxis()) return nf; // already on its word line — match it
            return perpendicularToward(d, playerFacing);              // re-orient so d becomes our word axis
        }
        return playerFacing.getOpposite();
    }

    /** A horizontal facing perpendicular to {@code d}, preferring the one that faces the placer. */
    private static Direction perpendicularToward(Direction d, Direction playerFacing) {
        Direction a = (d.getAxis() == Direction.Axis.X) ? Direction.NORTH : Direction.EAST;
        Direction b = a.getOpposite();
        Direction want = playerFacing.getOpposite();
        return (want == a || want == b) ? want : a;
    }

    // ── run walk (port of ArabicJoinFlow.collectRun / letterBeyondGap) ─────────

    /** The span of same-facing letters {@code pos} belongs to, index 0 = START (reader's right) →
     *  N-1 = END (reader's left); bridged single air gaps are {@code null} entries so N-1-i stays
     *  the true mirror position across a broken middle letter. */
    private static List<BlockPos> collectRun(World world, BlockPos pos, Direction facing) {
        Direction right = rightDir(facing);
        Direction left = right.getOpposite();
        BlockPos start = pos;
        for (int guard = 0; guard < MAX_RUN; guard++) {
            BlockPos n = start.offset(right);
            if (isJoinAt(world, n, facing)) { start = n; continue; }
            BlockPos beyond = letterBeyondGap(world, n, facing, right);
            if (beyond == null) break;
            start = beyond;
        }
        List<BlockPos> run = new ArrayList<>();
        BlockPos cur = start;
        for (int guard = 0; guard < MAX_RUN; guard++) {
            if (isJoinAt(world, cur, facing)) {
                run.add(cur);
                cur = cur.offset(left);
                continue;
            }
            BlockPos beyond = letterBeyondGap(world, cur, facing, left);
            if (beyond == null) break;
            for (BlockPos hole = cur; !hole.equals(beyond); hole = hole.offset(left)) run.add(null);
            cur = beyond;
        }
        return run;
    }

    /** If ≤ MAX_GAP air cells from {@code from} (not a letter) toward {@code dir} end at a letter
     *  sharing {@code facing}, that letter's pos; else null. Only AIR bridges — a solid block ends
     *  the span (it is not a broken-letter hole). */
    @Nullable
    private static BlockPos letterBeyondGap(World world, BlockPos from, Direction facing, Direction dir) {
        BlockPos cur = from;
        for (int gap = 0; gap < MAX_GAP; gap++) {
            if (!world.getBlockState(cur).isAir()) return null;
            BlockPos next = cur.offset(dir);
            if (isJoinAt(world, next, facing)) return next;
            cur = next;
        }
        return null;
    }

    // ── the re-flow itself (form brain unchanged; form CHANGE = sibling-slot swap) ──

    /**
     * Recompute every block in the run: swap each to its correct form's sibling slot (pass 1), then
     * stamp facing + back-face mirror partner on every BlockEntity and sync what changed (pass 2).
     * Two passes because a block's BACK partner must reference the partner's FINAL (post-swap) slot.
     */
    private static void recomputeRun(World world, List<BlockPos> run, Direction facing) {
        int n = run.size();
        if (n == 0) return;
        char[] letters = new char[n];
        ArabicMeta[] metas = new ArabicMeta[n];
        for (int i = 0; i < n; i++) {
            BlockPos p = run.get(i);
            metas[i] = (p == null) ? null : letterMetaAt(world, p);
            letters[i] = (metas[i] == null) ? 0 : charOf(metas[i]);
        }
        int[] forms = new int[n];
        for (int i = 0; i < n; i++) {
            if (letters[i] == 0) { forms[i] = 0; continue; } // hole — nothing to shape
            char right = (i > 0)     ? letters[i - 1] : 0;   // toward START (reader's right)
            char left  = (i < n - 1) ? letters[i + 1] : 0;   // toward END   (reader's left)
            forms[i] = ArabicJoining.form(letters[i], right, left);
        }
        // Pass 1 — swap wrong forms; record every block's final slot index for the mirror stamps.
        int[] finalSlot = new int[n];
        for (int i = 0; i < n; i++) {
            finalSlot[i] = -1;
            BlockPos p = run.get(i);
            if (p == null || metas[i] == null) continue;
            if (metas[i].form() != forms[i]) {
                int targetIdx = siblingIndex(world, siblingId(metas[i], forms[i]));
                SlotBlock tb = (targetIdx < 0) ? null : SlotManager.blockAt(targetIdx);
                if (tb != null) {
                    // The proven swapPlaced pattern: default state + the target's configured glow
                    // (clamped 0..15 — LIGHT is an IntProperty; mirrors ClientSwapPredictor.paint).
                    int glow = Math.max(0, Math.min(15, glowAt(world, targetIdx)));
                    BlockState st = tb.getDefaultState().with(SlotBlock.LIGHT, glow);
                    if (world.isClient()) {
                        // CP3b prediction: redraw only, no neighbour churn, no client/server fight
                        // — the server's authoritative swap reconciles to this exact state.
                        world.setBlockState(p, st, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                    } else {
                        world.setBlockState(p, st);
                    }
                    finalSlot[i] = targetIdx;
                }
            }
            if (finalSlot[i] < 0) finalSlot[i] = slotIndexAt(world, p);
        }
        // Pass 2 — facing + mirror partner on every survivor's (possibly fresh) BlockEntity.
        for (int i = 0; i < n; i++) {
            BlockPos p = run.get(i);
            if (p == null) continue;
            if (!(world.getBlockEntity(p) instanceof AnimSlotBlockEntity be)) continue;
            int mirror = n - 1 - i;
            int backIdx = (mirror == i) ? -1 : finalSlot[mirror]; // own centre / hole partner → own tile
            if (backIdx == finalSlot[i]) backIdx = -1;
            boolean changed = be.setArabicFacing(facing);
            changed |= be.setArabicBackSlot(backIdx);
            if (changed) be.sync();
        }
    }

    /** The sibling form-slot's custom id: same glyph + colour, {@code form}'s fixed token. */
    private static String siblingId(ArabicMeta meta, int form) {
        String base = ArabicSlotBootstrap.slotId(meta.glyphId(), form);
        String colour = meta.colorKey().toLowerCase(Locale.ROOT);
        return "black".equals(colour) ? base : base + "_" + colour;
    }

    // ── membership / lookups ───────────────────────────────────────────────────

    /** The LETTER ArabicMeta at pos, or null (not a slot block / not Arabic / a number). */
    @Nullable
    private static ArabicMeta letterMetaAt(World world, BlockPos pos) {
        if (!(world.getBlockState(pos).getBlock() instanceof SlotBlock sb)) return null;
        ArabicMeta meta;
        if (remoteClient(world)) {
            ClientView v = CLIENT_VIEW;
            meta = (v == null) ? null : v.arabicMeta(sb.getSlotIndex());
        } else {
            SlotData d = SlotManager.getBySlot(sb.getSlotKey());
            meta = (d == null || !d.isArabic()) ? null : d.arabic();
        }
        if (meta == null) return null;
        return (charOf(meta) == 0) ? null : meta; // numbers have no letter char — never join
    }

    /** True when metadata must come from the synced cache view, not this JVM's SlotManager. */
    private static boolean remoteClient(World world) {
        return world.isClient() && SlotBlock.CLIENT_REMOTE_SESSION;
    }

    /** True when the slot carries ArabicMeta (letters AND numbers) — the SlotBlock hooks' cheap
     *  data-gate. Session-aware (CP3b): a remote client's own SlotManager is stale, so it must
     *  consult the synced-cache view or the gate would never open and no prediction would run. */
    public static boolean isArabicSlot(World world, int slotIndex, String slotKey) {
        if (remoteClient(world)) {
            ClientView v = CLIENT_VIEW;
            return v != null && v.arabicMeta(slotIndex) != null;
        }
        SlotData d = SlotManager.getBySlot(slotKey);
        return d != null && d.isArabic();
    }

    /** The sibling form-slot's index for {@code customId}, or -1 (session-aware source). */
    private static int siblingIndex(World world, String customId) {
        if (remoteClient(world)) {
            ClientView v = CLIENT_VIEW;
            Integer idx = (v == null) ? null : v.indexForId(customId);
            return (idx == null) ? -1 : idx;
        }
        SlotData d = SlotManager.getById(customId);
        return (d == null) ? -1 : d.index();
    }

    /** The configured glow for a slot (session-aware source). */
    private static int glowAt(World world, int slotIndex) {
        if (remoteClient(world)) {
            ClientView v = CLIENT_VIEW;
            return (v == null) ? 0 : v.glowFor(slotIndex);
        }
        return SlotManager.glowFor(slotIndex);
    }

    /** The glyph's letter char, or 0 (numbers / unknown art base). */
    private static char charOf(ArabicMeta meta) {
        return ArabicGlyphs.charForName(meta.glyphId()).orElse((char) 0);
    }

    /** True when pos holds a letter slot sharing {@code facing}. */
    private static boolean isJoinAt(World world, BlockPos pos, Direction facing) {
        return letterMetaAt(world, pos) != null && facingAt(world, pos) == facing;
    }

    /** The stamped glyph facing at pos, or null. */
    @Nullable
    private static Direction facingAt(World world, BlockPos pos) {
        return (world.getBlockEntity(pos) instanceof AnimSlotBlockEntity be) ? be.arabicFacing() : null;
    }

    /** The registered slot index at pos (-1 if not a slot block). */
    private static int slotIndexAt(World world, BlockPos pos) {
        return (world.getBlockState(pos).getBlock() instanceof SlotBlock sb) ? sb.getSlotIndex() : -1;
    }

    /** Neighbour toward the word START (reader's right). */
    private static Direction rightDir(Direction facing) {
        return facing.getOpposite().rotateYClockwise();
    }
}
