/**
 * ArabicJoinFlow.java — Group 13 / Pass 4 step 4c: the auto-join re-flow updater.
 *
 * When a joinable letter is placed or broken, re-evaluates the contextual form of every letter in
 * the affected word RUN and pushes the change. Originally a bounded ±2-neighbour updater; widened
 * (issue #6, C-full readable back) to a capped WHOLE-RUN walk because the back face of a letter must
 * show its mirror partner's glyph, which needs whole-word knowledge. Still no recursion and no world
 * scan — it walks the one contiguous run along the word axis, capped at {@link #MAX_RUN} letters.
 *
 * Axis (4d): a letter's word runs along the horizontal axis PERPENDICULAR to its FACING. Reading is
 * right-to-left from the reader's view, so the neighbour toward the word START is on the reader's
 * RIGHT and the neighbour toward the END is on the LEFT:
 *     rightDir = FACING.getOpposite().rotateYClockwise()   (reader looks back along -FACING)
 * Only blocks that share the SAME FACING join, so two words pointing different ways never cross-join.
 *
 * C-full back face (issue #6): the run is indexed 0..N-1 from START (reader's right) to END (left).
 * The back of block k is read from behind, where left/right swap, so it must show the letter+form of
 * block N-1-k (the mirror partner). Because it is the same word read from the same kind of viewpoint,
 * the partner's BACK form equals the partner's FRONT form — one run-walk computes both faces. The
 * renderer draws that partner tile U-flipped (un-mirrors the 180° back-face quad).
 *
 * Depends on: ArabicLetterBlock (FACING), ArabicLetterBlockEntity, ArabicJoining (form brain)
 * Called by:  ArabicLetterBlock.onPlaced / onStateReplaced
 */
package com.customblocks.block;

import com.customblocks.arabic.ArabicJoining;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public final class ArabicJoinFlow {

    private ArabicJoinFlow() {}

    /** Safety cap on a single word run (a 64-letter word is already absurd). Past this the back-face
     *  mirror mapping is left as-is rather than walking unbounded. */
    private static final int MAX_RUN = 64;

    /** Re-flow the whole run a freshly placed block belongs to (or, if it doesn't join, its neighbours). */
    public static void onPlace(World world, BlockPos pos) {
        Direction facing = facingAt(world, pos);
        if (facing == null) return;
        if (isJoinAt(world, pos, facing)) {
            recomputeRun(world, collectRun(world, pos, facing));
        } else {
            // pos doesn't join (fixed-form decoration / non-letter): it may have split the neighbour runs.
            recomputeNeighbourRuns(world, pos, facing);
        }
    }

    /** Re-flow the run(s) on each side of a block that was just broken (pos is already empty). */
    public static void onBreak(World world, BlockPos pos, Direction oldFacing) {
        if (oldFacing == null) return;
        recomputeNeighbourRuns(world, pos, oldFacing);
    }

    /** Recompute the run reachable from each in-axis neighbour of {@code pos} (used on break / split). */
    private static void recomputeNeighbourRuns(World world, BlockPos pos, Direction facing) {
        for (Direction d : new Direction[]{rightDir(facing), leftDir(facing)}) {
            BlockPos n = pos.offset(d);
            if (isJoinAt(world, n, facing)) recomputeRun(world, collectRun(world, n, facing));
        }
    }

    /**
     * Collect the contiguous run of auto-join letters (same facing) that {@code pos} belongs to, ordered
     * index 0 = START (reader's right) → N-1 = END (reader's left). Capped at {@link #MAX_RUN}.
     */
    private static List<BlockPos> collectRun(World world, BlockPos pos, Direction facing) {
        Direction right = rightDir(facing);
        Direction left = leftDir(facing);
        // Walk toward START to find the run's first block.
        BlockPos start = pos;
        for (int guard = 0; guard < MAX_RUN; guard++) {
            BlockPos n = start.offset(right);
            if (!isJoinAt(world, n, facing)) break;
            start = n;
        }
        // Walk from START toward END, collecting.
        List<BlockPos> run = new ArrayList<>();
        BlockPos cur = start;
        for (int guard = 0; guard < MAX_RUN && isJoinAt(world, cur, facing); guard++) {
            run.add(cur);
            cur = cur.offset(left);
        }
        return run;
    }

    /**
     * Recompute every block in the run: its own (front) form, attached flag, and its back-face mirror
     * partner letter+form. Syncs only the blocks that actually changed.
     */
    private static void recomputeRun(World world, List<BlockPos> run) {
        int n = run.size();
        if (n == 0) return;
        char[] letters = new char[n];
        for (int i = 0; i < n; i++) letters[i] = letterAt(world, run.get(i));
        int[] forms = new int[n];
        for (int i = 0; i < n; i++) {
            char right = (i > 0)     ? letters[i - 1] : 0; // toward START (reader's right)
            char left  = (i < n - 1) ? letters[i + 1] : 0; // toward END   (reader's left)
            forms[i] = ArabicJoining.form(letters[i], right, left);
        }
        boolean attached = n > 1;
        for (int i = 0; i < n; i++) {
            if (!(world.getBlockEntity(run.get(i)) instanceof ArabicLetterBlockEntity be)) continue;
            int mirror = n - 1 - i; // the partner whose FRONT this block's BACK shows
            boolean changed = be.setForm(forms[i]);
            changed |= be.setAttached(attached);
            changed |= be.setBackLetter(letters[mirror]);
            changed |= be.setBackForm(forms[mirror]);
            if (changed) be.sync();
        }
    }

    /** Letter of an auto-join block at pos (0 if not a valid run member). */
    private static char letterAt(World world, BlockPos pos) {
        return (world.getBlockEntity(pos) instanceof ArabicLetterBlockEntity be) ? be.letter() : 0;
    }

    /** True when pos holds an auto-join letter block sharing {@code facing} (not a fixed-form variant). */
    private static boolean isJoinAt(World world, BlockPos pos, Direction facing) {
        if (!(world.getBlockEntity(pos) instanceof ArabicLetterBlockEntity be)) return false;
        if (be.letter() == 0) return false;
        if (be.lockedForm() >= 0) return false; // fixed-form decoration never joins
        return facingAt(world, pos) == facing;
    }

    private static Direction facingAt(World world, BlockPos pos) {
        BlockState st = world.getBlockState(pos);
        return st.contains(ArabicLetterBlock.FACING) ? st.get(ArabicLetterBlock.FACING) : null;
    }

    /** Neighbour toward the word START (reader's right). */
    private static Direction rightDir(Direction facing) {
        return facing.getOpposite().rotateYClockwise();
    }

    /** Neighbour toward the word END (reader's left). */
    private static Direction leftDir(Direction facing) {
        return rightDir(facing).getOpposite();
    }
}
